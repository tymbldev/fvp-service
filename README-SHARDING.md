# LinkCategory Sharding Implementation

This document details the implementation of a sharding strategy for the `LinkCategory` table to handle high volumes of data.

## Overview

The LinkCategory table was sharded to address performance issues caused by a high number of records. The sharding strategy uses:

1. 10 physical shard tables (LinkCategoryShard1, LinkCategoryShard2, ..., LinkCategoryShard10)
2. A consistent hashing algorithm based on category name
3. A caching layer for shard mapping and query results
4. A migration service to move data from the original table to the appropriate shards

## Architecture

### Entities and Repositories

- `BaseLinkCategory`: An abstract base class for all shard entities
- `LinkCategoryShard1` - `LinkCategoryShard10`: Entity classes for each shard table
- `ShardedLinkCategoryRepository`: Common interface for all shard repositories
- `LinkCategoryShard1Repository` - `LinkCategoryShard10Repository`: Repository implementations for each shard

### Services

- `LinkCategoryShardingService`: Core service for shard routing based on category name
- `LinkCategoryService`: Facade service that replaces direct usage of `LinkCategoryRepository`
- `LinkCategoryMigrationService`: Service for migrating data from the original table to the shards

### Migration

The migration process is managed through REST endpoints:

- `POST /api/admin/migration/link-category/all`: Migrate all data
- `POST /api/admin/migration/link-category/tenant/{tenantId}`: Migrate data for a specific tenant

## How it Works

1. The system determines the appropriate shard for a category using a hash of its name:
   ```java
   int hash = Math.abs(category.hashCode());
   int shardNumber = (hash % TOTAL_SHARDS) + 1; // 1-based index
   ```

2. The result is cached using Spring's caching framework to avoid recalculating the shard for frequent operations.

3. When performing operations on a category, the system:
   - Determines the appropriate shard
   - Routes the operation to the correct repository
   - Converts between shard entities and the original `LinkCategory` entity as needed

4. For operations across all categories (like finding all categories for a tenant), the system:
   - Executes the query on all shards in parallel
   - Combines and de-duplicates the results

## Caching Strategy

Several caches are implemented:

- `categoryShardCache`: Maps category names to shard numbers
- `linkCategoryCache`: Caches query results for links by category
- `categoryCountCache`: Caches count query results
- `categoriesCache`: Caches lists of categories

## Migration Steps

To migrate data from the original table to the sharded tables:

1. Apply the database migration (`V1_10__create_link_category_shards.sql`) to create the shard tables
2. Use the `/api/admin/migration/link-category/all` endpoint to migrate all data, or migrate tenant by tenant
3. Verify the data migration
4. Update application code to use the `LinkCategoryService` instead of directly using `LinkCategoryRepository`

## Testing

The sharding logic is tested to ensure:
- Consistent hashing (same category always maps to the same shard)
- Even distribution of categories across shards
- Correct routing of operations to appropriate shards

## Implementation Notes

- The implementation maintains backward compatibility with existing code by providing a service facade
- The sharding is transparent to the rest of the application
- All queries are now automatically routed to the correct shard based on the category name
- The system can handle cross-shard queries by executing them on all shards and combining the results

## Monitoring

You can monitor the distribution of data across shards using SQL queries:

```sql
SELECT 'shard1', COUNT(*) FROM link_category_shard1 UNION ALL
SELECT 'shard2', COUNT(*) FROM link_category_shard2 UNION ALL
SELECT 'shard3', COUNT(*) FROM link_category_shard3 UNION ALL
SELECT 'shard4', COUNT(*) FROM link_category_shard4 UNION ALL
SELECT 'shard5', COUNT(*) FROM link_category_shard5 UNION ALL
SELECT 'shard6', COUNT(*) FROM link_category_shard6 UNION ALL
SELECT 'shard7', COUNT(*) FROM link_category_shard7 UNION ALL
SELECT 'shard8', COUNT(*) FROM link_category_shard8 UNION ALL
SELECT 'shard9', COUNT(*) FROM link_category_shard9 UNION ALL
SELECT 'shard10', COUNT(*) FROM link_category_shard10;
```

## Future Improvements

- Implement dynamic sharding to automatically adjust the number of shards
- Add read replicas for each shard to improve read performance
- Implement shard rebalancing for more evenly distributed data 