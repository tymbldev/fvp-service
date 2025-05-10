package com.fvp.repository;

import com.fvp.entity.BaseLinkCategory;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import javax.persistence.TemporalType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Central class for building dynamic queries for LinkCategory entities across all shards.
 * This uses JPA Criteria API to build truly dynamic queries where conditions
 * are only included when parameters have values.
 */
@Component
public class LinkCategoryDynamicQueryBuilder {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Find a random recent link by category.
     * 
     * @param entityClass The entity class
     * @param tenantId The tenant ID
     * @param category The category name
     * @param recentDays The number of days to look back
     * @return An optional containing a random link from the recent days
     */
    public <T extends BaseLinkCategory> Optional<T> findRandomRecentLinkByCategory(
            Class<T> entityClass,
            Integer tenantId,
            String category,
            Long recentDays
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = cb.createQuery(entityClass);
        Root<T> root = query.from(entityClass);
        
        // Join with link table
        Join<T, Object> linkJoin = root.join("link", JoinType.INNER);
        
        // Calculate date threshold (current date minus recentDays)
        LocalDateTime thresholdDate = LocalDateTime.now().minus(recentDays, ChronoUnit.DAYS);
        
        // Build predicates
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("tenantId"), tenantId));
        predicates.add(cb.equal(root.get("category"), category));
        predicates.add(cb.greaterThanOrEqualTo(root.get("createdOn"), thresholdDate));
        predicates.add(cb.equal(linkJoin.get("thumbPathProcessed"), 1));
        
        // Apply predicates
        query.where(predicates.toArray(new Predicate[0]));
        
        // Order by random
        query.orderBy(cb.asc(cb.function("RAND", Double.class)));
        
        // Execute query and return first result if present
        TypedQuery<T> typedQuery = entityManager.createQuery(query);
        typedQuery.setMaxResults(1);
        List<T> results = typedQuery.getResultList();
        
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Find a random link by category.
     * 
     * @param entityClass The entity class
     * @param tenantId The tenant ID
     * @param category The category name
     * @return An optional containing a random link from the category
     */
    public <T extends BaseLinkCategory> Optional<T> findRandomLinkByCategory(
            Class<T> entityClass,
            Integer tenantId,
            String category
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = cb.createQuery(entityClass);
        Root<T> root = query.from(entityClass);
        
        // Join with link table
        Join<T, Object> linkJoin = root.join("link", JoinType.INNER);
        
        // Build predicates
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("tenantId"), tenantId));
        predicates.add(cb.equal(root.get("category"), category));
        predicates.add(cb.equal(linkJoin.get("thumbPathProcessed"), 1));
        
        // Apply predicates
        query.where(predicates.toArray(new Predicate[0]));
        
        // Order by random
        query.orderBy(cb.asc(cb.function("RAND", Double.class)));
        
        // Execute query and return first result if present
        TypedQuery<T> typedQuery = entityManager.createQuery(query);
        typedQuery.setMaxResults(1);
        List<T> results = typedQuery.getResultList();
        
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Count the number of links in a category.
     * 
     * @param entityClass The entity class
     * @param tenantId The tenant ID
     * @param category The category name
     * @return The count of links in the category
     */
    public <T extends BaseLinkCategory> Long countByTenantIdAndCategory(
            Class<T> entityClass,
            Integer tenantId,
            String category
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<T> root = query.from(entityClass);
        
        // Join with link table
        Join<T, Object> linkJoin = root.join("link", JoinType.INNER);
        
        // Build predicates
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("tenantId"), tenantId));
        predicates.add(cb.equal(root.get("category"), category));
        predicates.add(cb.equal(linkJoin.get("thumbPathProcessed"), 1));
        
        // Apply predicates
        query.where(predicates.toArray(new Predicate[0]));
        
        // Count query
        query.select(cb.count(root));
        
        return entityManager.createQuery(query).getSingleResult();
    }

    /**
     * Find links by tenant ID and link ID.
     * 
     * @param entityClass The entity class
     * @param tenantId The tenant ID
     * @param linkId The link ID
     * @return List of links matching the criteria
     */
    public <T extends BaseLinkCategory> List<T> findByTenantIdAndLinkId(
            Class<T> entityClass,
            Integer tenantId,
            Integer linkId
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = cb.createQuery(entityClass);
        Root<T> root = query.from(entityClass);
        
        // Join with link table
        Join<T, Object> linkJoin = root.join("link", JoinType.INNER);
        
        // Build predicates
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("tenantId"), tenantId));
        predicates.add(cb.equal(root.get("linkId"), linkId));
        predicates.add(cb.equal(linkJoin.get("thumbPathProcessed"), 1));
        
        // Apply predicates
        query.where(predicates.toArray(new Predicate[0]));
        
        return entityManager.createQuery(query).getResultList();
    }

    /**
     * Find all distinct categories for a tenant.
     * 
     * @param entityClass The entity class
     * @param tenantId The tenant ID
     * @return List of distinct category names
     */
    public <T extends BaseLinkCategory> List<String> findAllDistinctCategories(
            Class<T> entityClass,
            Integer tenantId
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<String> query = cb.createQuery(String.class);
        Root<T> root = query.from(entityClass);
        
        // Select distinct categories
        query.select(root.get("category")).distinct(true);
        
        // Add tenant ID predicate
        query.where(cb.equal(root.get("tenantId"), tenantId));
        
        return entityManager.createQuery(query).getResultList();
    }

    /**
     * Find links by tenant ID.
     * 
     * @param entityClass The entity class
     * @param tenantId The tenant ID
     * @return List of links for the tenant
     */
    public <T extends BaseLinkCategory> List<T> findByTenantId(
            Class<T> entityClass,
            Integer tenantId
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = cb.createQuery(entityClass);
        Root<T> root = query.from(entityClass);
        
        // Join with link table
        Join<T, Object> linkJoin = root.join("link", JoinType.INNER);
        
        // Add tenant ID predicate
        query.where(cb.equal(root.get("tenantId"), tenantId));
        
        return entityManager.createQuery(query).getResultList();
    }

    /**
     * Find links by tenant ID and category.
     * 
     * @param entityClass The entity class
     * @param tenantId The tenant ID
     * @param category The category name
     * @return List of links matching the criteria
     */
    public <T extends BaseLinkCategory> List<T> findByTenantIdAndCategory(
            Class<T> entityClass,
            Integer tenantId,
            String category
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = cb.createQuery(entityClass);
        Root<T> root = query.from(entityClass);
        
        // Join with link table
        Join<T, Object> linkJoin = root.join("link", JoinType.INNER);
        
        // Build predicates
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("tenantId"), tenantId));
        predicates.add(cb.equal(root.get("category"), category));
        predicates.add(cb.equal(linkJoin.get("thumbPathProcessed"), 1));
        
        // Apply predicates
        query.where(predicates.toArray(new Predicate[0]));
        
        return entityManager.createQuery(query).getResultList();
    }

    /**
     * Find links by tenant ID and category ordered by random order.
     * 
     * @param entityClass The entity class
     * @param tenantId The tenant ID
     * @param category The category name
     * @return List of links matching the criteria ordered by random order
     */
    public <T extends BaseLinkCategory> List<T> findByTenantIdAndCategoryOrderByRandomOrder(
            Class<T> entityClass,
            Integer tenantId,
            String category
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = cb.createQuery(entityClass);
        Root<T> root = query.from(entityClass);
        
        // Join with link table
        Join<T, Object> linkJoin = root.join("link", JoinType.INNER);
        
        // Build predicates
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("tenantId"), tenantId));
        predicates.add(cb.equal(root.get("category"), category));
        predicates.add(cb.equal(linkJoin.get("thumbPathProcessed"), 1));
        
        // Apply predicates
        query.where(predicates.toArray(new Predicate[0]));
        
        // Order by randomOrder
        query.orderBy(cb.asc(root.get("randomOrder")));
        
        return entityManager.createQuery(query).getResultList();
    }

    /**
     * Find links by link ID.
     * 
     * @param entityClass The entity class
     * @param linkId The link ID
     * @return List of links matching the criteria
     */
    public <T extends BaseLinkCategory> List<T> findByLinkId(
            Class<T> entityClass,
            Integer linkId
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = cb.createQuery(entityClass);
        Root<T> root = query.from(entityClass);
        
        // Add link ID predicate
        query.where(cb.equal(root.get("linkId"), linkId));
        
        return entityManager.createQuery(query).getResultList();
    }

    /**
     * Find links by category and tenant ID.
     * 
     * @param entityClass The entity class
     * @param category The category name
     * @param tenantId The tenant ID
     * @return List of links matching the criteria
     */
    public <T extends BaseLinkCategory> List<T> findByCategoryAndTenantId(
            Class<T> entityClass,
            String category,
            Integer tenantId
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = cb.createQuery(entityClass);
        Root<T> root = query.from(entityClass);
        
        // Build predicates
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("tenantId"), tenantId));
        predicates.add(cb.equal(root.get("category"), category));
        
        // Apply predicates
        query.where(predicates.toArray(new Predicate[0]));
        
        return entityManager.createQuery(query).getResultList();
    }

    /**
     * Delete links by link ID.
     * 
     * @param entityClass The entity class
     * @param linkId The link ID
     */
    public <T extends BaseLinkCategory> int deleteByLinkId(
            Class<T> entityClass,
            Integer linkId
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaDelete<T> delete = cb.createCriteriaDelete(entityClass);
        Root<T> root = delete.from(entityClass);
        
        // Add link ID predicate
        delete.where(cb.equal(root.get("linkId"), linkId));
        
        return entityManager.createQuery(delete).executeUpdate();
    }

    /**
     * Find random links by category names.
     * 
     * @param entityClass The entity class
     * @param tenantId The tenant ID
     * @param categoryNames List of category names
     * @return List of random links from the specified categories
     */
    public <T extends BaseLinkCategory> List<T> findRandomLinksByCategoryNames(
            Class<T> entityClass,
            Integer tenantId,
            List<String> categoryNames
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = cb.createQuery(entityClass);
        Root<T> root = query.from(entityClass);
        
        // Join with link table
        Join<T, Object> linkJoin = root.join("link", JoinType.INNER);
        
        // Build predicates
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("tenantId"), tenantId));
        predicates.add(root.get("category").in(categoryNames));
        predicates.add(cb.equal(linkJoin.get("thumbPathProcessed"), 1));
        
        // Apply predicates
        query.where(predicates.toArray(new Predicate[0]));
        
        // Group by category
        query.groupBy(root.get("category"));
        
        // Order by random
        query.orderBy(cb.asc(cb.function("RAND", Double.class)));
        
        return entityManager.createQuery(query).getResultList();
    }

    /**
     * Count links by tenant ID and categories.
     * 
     * @param entityClass The entity class
     * @param tenantId The tenant ID
     * @param categoryNames List of category names
     * @return List of category name and count pairs
     */
    public <T extends BaseLinkCategory> List<Object[]> countByTenantIdAndCategories(
            Class<T> entityClass,
            Integer tenantId,
            List<String> categoryNames
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        Root<T> root = query.from(entityClass);
        
        // Join with link table
        Join<T, Object> linkJoin = root.join("link", JoinType.INNER);
        
        // Build predicates
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("tenantId"), tenantId));
        predicates.add(root.get("category").in(categoryNames));
        predicates.add(cb.equal(linkJoin.get("thumbPathProcessed"), 1));
        
        // Apply predicates
        query.where(predicates.toArray(new Predicate[0]));
        
        // Select category and count
        query.multiselect(root.get("category"), cb.count(root));
        
        // Group by category
        query.groupBy(root.get("category"));
        
        return entityManager.createQuery(query).getResultList();
    }

    /**
     * Find links by category with dynamic filters.
     * Only applies filters for non-null parameters.
     *
     * @param entityClass  The entity class (LinkCategoryShard1, LinkCategoryShard2, etc.)
     * @param tenantId     The tenant ID
     * @param category     The category name
     * @param minDuration  Minimum duration (optional)
     * @param maxDuration  Maximum duration (optional)
     * @param quality      Quality filter (optional)
     * @param offset       Pagination offset
     * @param limit        Maximum number of results
     * @param <T>          The entity type
     * @return Filtered list of entities
     */
    public <T extends BaseLinkCategory> List<T> findByCategoryWithDynamicFilters(
            Class<T> entityClass,
            Integer tenantId,
            String category,
            Integer minDuration,
            Integer maxDuration,
            String quality,
            int offset,
            int limit
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = cb.createQuery(entityClass);
        Root<T> root = query.from(entityClass);
        
        // Join with link table
        Join<T, Object> linkJoin = root.join("link", JoinType.INNER);
        
        // Build predicates list
        List<Predicate> predicates = buildBasePredicates(cb, root, linkJoin, tenantId, category);
        
        // Add optional filters
        addDurationPredicates(cb, linkJoin, predicates, minDuration, maxDuration);
        addQualityPredicate(cb, linkJoin, predicates, quality);
        
        // Require thumb processed
        predicates.add(cb.equal(linkJoin.get("thumbPathProcessed"), 1));
        
        // Apply predicates to query
        query.where(predicates.toArray(new Predicate[0]));
        
        // Create and execute typed query with pagination
        TypedQuery<T> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult(offset);
        typedQuery.setMaxResults(limit);
        
        return typedQuery.getResultList();
    }
    
    /**
     * Count links by category with dynamic filters.
     * Only applies filters for non-null parameters.
     *
     * @param entityClass  The entity class (LinkCategoryShard1, LinkCategoryShard2, etc.)
     * @param tenantId     The tenant ID
     * @param category     The category name
     * @param minDuration  Minimum duration (optional)
     * @param maxDuration  Maximum duration (optional)
     * @param quality      Quality filter (optional)
     * @param <T>          The entity type
     * @return Count of matching entities
     */
    public <T extends BaseLinkCategory> Long countByCategoryWithDynamicFilters(
            Class<T> entityClass,
            Integer tenantId,
            String category,
            Integer minDuration,
            Integer maxDuration,
            String quality
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<T> root = query.from(entityClass);
        
        // Join with link table
        Join<T, Object> linkJoin = root.join("link", JoinType.INNER);
        
        // Build predicates list
        List<Predicate> predicates = buildBasePredicates(cb, root, linkJoin, tenantId, category);
        
        // Add optional filters
        addDurationPredicates(cb, linkJoin, predicates, minDuration, maxDuration);
        addQualityPredicate(cb, linkJoin, predicates, quality);
        
        // Require thumb processed
        predicates.add(cb.equal(linkJoin.get("thumbPathProcessed"), 1));
        
        // Apply predicates to query
        query.where(predicates.toArray(new Predicate[0]));
        
        // Count query
        query.select(cb.count(root));
        
        return entityManager.createQuery(query).getSingleResult();
    }
    
    /**
     * Find links by category with dynamic filters, excluding a specific link.
     * Only applies filters for non-null parameters.
     *
     * @param entityClass  The entity class (LinkCategoryShard1, LinkCategoryShard2, etc.)
     * @param tenantId     The tenant ID
     * @param category     The category name
     * @param minDuration  Minimum duration (optional)
     * @param maxDuration  Maximum duration (optional)
     * @param quality      Quality filter (optional)
     * @param excludeId    Link ID to exclude
     * @param offset       Pagination offset
     * @param limit        Maximum number of results
     * @param <T>          The entity type
     * @return Filtered list of entities
     */
    public <T extends BaseLinkCategory> List<T> findByCategoryWithDynamicFiltersExcludingLink(
            Class<T> entityClass,
            Integer tenantId,
            String category,
            Integer minDuration,
            Integer maxDuration,
            String quality,
            Integer excludeId,
            int offset,
            int limit
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = cb.createQuery(entityClass);
        Root<T> root = query.from(entityClass);
        
        // Join with link table
        Join<T, Object> linkJoin = root.join("link", JoinType.INNER);
        
        // Build predicates list
        List<Predicate> predicates = buildBasePredicates(cb, root, linkJoin, tenantId, category);
        
        // Add optional filters
        addDurationPredicates(cb, linkJoin, predicates, minDuration, maxDuration);
        addQualityPredicate(cb, linkJoin, predicates, quality);
        
        // Exclude specific link ID
        predicates.add(cb.notEqual(linkJoin.get("id"), excludeId));
        
        // Require thumb processed
        predicates.add(cb.equal(linkJoin.get("thumbPathProcessed"), 1));
        
        // Apply predicates to query
        query.where(predicates.toArray(new Predicate[0]));
        
        // Order by random_order
        query.orderBy(cb.asc(root.get("randomOrder")));
        
        // Create and execute typed query with pagination
        TypedQuery<T> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult(offset);
        typedQuery.setMaxResults(limit);
        
        return typedQuery.getResultList();
    }
    
    // Helper methods to build query predicates
    
    private <T extends BaseLinkCategory> List<Predicate> buildBasePredicates(
            CriteriaBuilder cb,
            Root<T> root,
            Join<T, Object> linkJoin,
            Integer tenantId,
            String category
    ) {
        List<Predicate> predicates = new ArrayList<>();
        
        // Required filters (always applied)
        predicates.add(cb.equal(root.get("tenantId"), tenantId));
        if(category!=null && !category.isEmpty()) {
            predicates.add(cb.equal(root.get("category"), category));
        }
        
        return predicates;
    }
    
    private void addDurationPredicates(
            CriteriaBuilder cb,
            Join<?, Object> linkJoin,
            List<Predicate> predicates,
            Integer minDuration,
            Integer maxDuration
    ) {
        // Apply min duration filter only if parameter is provided
        if (minDuration != null) {
            predicates.add(cb.greaterThanOrEqualTo(linkJoin.get("duration"), minDuration));
        }
        
        // Apply max duration filter only if parameter is provided
        if (maxDuration != null) {
            predicates.add(cb.lessThanOrEqualTo(linkJoin.get("duration"), maxDuration));
        }
    }
    
    private void addQualityPredicate(
            CriteriaBuilder cb,
            Join<?, Object> linkJoin,
            List<Predicate> predicates,
            String quality
    ) {
        // Apply quality filter only if parameter is provided and not empty
        if (quality != null && !quality.isEmpty()) {
            predicates.add(cb.equal(linkJoin.get("quality"), quality));
        }
    }
} 