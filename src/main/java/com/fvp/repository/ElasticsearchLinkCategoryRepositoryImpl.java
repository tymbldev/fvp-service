package com.fvp.repository;

import com.fvp.document.LinkDocument;
import com.fvp.service.ElasticsearchClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;

@Repository
public class ElasticsearchLinkCategoryRepositoryImpl implements ElasticsearchLinkCategoryRepository {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchLinkCategoryRepositoryImpl.class);
    private final ElasticsearchClientService elasticsearchClientService;

    @Autowired
    public ElasticsearchLinkCategoryRepositoryImpl(ElasticsearchClientService elasticsearchClientService) {
        this.elasticsearchClientService = elasticsearchClientService;
    }

    @Override
    public Optional<LinkDocument> findRandomRecentLinkByCategory(Integer tenantId, String category, Long recentDays) {
        // Calculate date threshold
        java.util.Date thresholdDate = java.util.Date.from(java.time.LocalDateTime.now()
                .minusDays(recentDays)
                .atZone(java.time.ZoneId.systemDefault())
                .toInstant());

        // Build ES query
        org.elasticsearch.index.query.BoolQueryBuilder boolQuery = org.elasticsearch.index.query.QueryBuilders.boolQuery()
                .must(org.elasticsearch.index.query.QueryBuilders.termQuery("tenantId", tenantId))
                .must(org.elasticsearch.index.query.QueryBuilders.termQuery("categories", category))
                .must(org.elasticsearch.index.query.QueryBuilders.rangeQuery("createdAt").gte(thresholdDate))
                .must(org.elasticsearch.index.query.QueryBuilders.existsQuery("thumbPath"))
                .mustNot(org.elasticsearch.index.query.QueryBuilders.boolQuery()
                        .should(org.elasticsearch.index.query.QueryBuilders.termQuery("thumbPath", ""))
                        .should(org.elasticsearch.index.query.QueryBuilders.termQuery("thumbPath", "NA"))
                        .should(org.elasticsearch.index.query.QueryBuilders.termQuery("thumbPath", "null")));

        // First, get the total count for this query
        org.elasticsearch.search.builder.SearchSourceBuilder countBuilder = new org.elasticsearch.search.builder.SearchSourceBuilder();
        countBuilder.query(boolQuery);
        countBuilder.size(0);
        countBuilder.trackTotalHits(true);
        
        try {
            org.elasticsearch.action.search.SearchRequest countRequest = new org.elasticsearch.action.search.SearchRequest("links");
            countRequest.source(countBuilder);
            org.elasticsearch.action.search.SearchResponse countResponse =
                    elasticsearchClientService.getEsClient().search(countRequest, org.elasticsearch.client.RequestOptions.DEFAULT);
            
            long totalCount = countResponse.getHits().getTotalHits().value;
            if (totalCount == 0) {
                return Optional.empty();
            }

            // Generate random offset
            int randomOffset = (int) (Math.random() * totalCount);

            // Use random offset for true randomness
            org.elasticsearch.search.builder.SearchSourceBuilder searchSourceBuilder = new org.elasticsearch.search.builder.SearchSourceBuilder();
            searchSourceBuilder.query(boolQuery);
            searchSourceBuilder.from(randomOffset);
            searchSourceBuilder.size(1);

            org.elasticsearch.action.search.SearchRequest searchRequest = new org.elasticsearch.action.search.SearchRequest("links");
            searchRequest.source(searchSourceBuilder);
            log.info("Elasticsearch query for findRandomRecentLinkByCategory: {}", searchSourceBuilder.toString());
            org.elasticsearch.action.search.SearchResponse response =
                    elasticsearchClientService.getEsClient().search(searchRequest, org.elasticsearch.client.RequestOptions.DEFAULT);
            for (org.elasticsearch.search.SearchHit hit : response.getHits().getHits()) {
                java.util.Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                LinkDocument document = elasticsearchClientService.convertToLinkDocument(sourceAsMap);
                document.setLinkId(hit.getId());
                return Optional.of(document);
            }
        } catch (Exception e) {
            log.error("Error in findRandomRecentLinkByCategory for tenantId: {}, category: {}, recentDays: {}", 
                    tenantId, category, recentDays, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<LinkDocument> findRandomLinkByCategory(Integer tenantId, String category) {
        // Build ES query
        org.elasticsearch.index.query.BoolQueryBuilder boolQuery = org.elasticsearch.index.query.QueryBuilders.boolQuery()
                .must(org.elasticsearch.index.query.QueryBuilders.termQuery("tenantId", tenantId))
                .must(org.elasticsearch.index.query.QueryBuilders.termQuery("categories", category))
                .must(org.elasticsearch.index.query.QueryBuilders.existsQuery("thumbPath"))
                .mustNot(org.elasticsearch.index.query.QueryBuilders.boolQuery()
                        .should(org.elasticsearch.index.query.QueryBuilders.termQuery("thumbPath", ""))
                        .should(org.elasticsearch.index.query.QueryBuilders.termQuery("thumbPath", "NA"))
                        .should(org.elasticsearch.index.query.QueryBuilders.termQuery("thumbPath", "null")));

        // First, get the total count for this query
        org.elasticsearch.search.builder.SearchSourceBuilder countBuilder = new org.elasticsearch.search.builder.SearchSourceBuilder();
        countBuilder.query(boolQuery);
        countBuilder.size(0);
        countBuilder.trackTotalHits(true);
        
        try {
            org.elasticsearch.action.search.SearchRequest countRequest = new org.elasticsearch.action.search.SearchRequest("links");
            countRequest.source(countBuilder);
            org.elasticsearch.action.search.SearchResponse countResponse =
                    elasticsearchClientService.getEsClient().search(countRequest, org.elasticsearch.client.RequestOptions.DEFAULT);
            
            long totalCount = countResponse.getHits().getTotalHits().value;
            if (totalCount == 0) {
                return Optional.empty();
            }

            // Generate random offset
            int randomOffset = (int) (Math.random() * totalCount);

            // Use random offset for true randomness
            org.elasticsearch.search.builder.SearchSourceBuilder searchSourceBuilder = new org.elasticsearch.search.builder.SearchSourceBuilder();
            searchSourceBuilder.query(boolQuery);
            searchSourceBuilder.from(randomOffset);
            searchSourceBuilder.size(1);

            org.elasticsearch.action.search.SearchRequest searchRequest = new org.elasticsearch.action.search.SearchRequest("links");
            searchRequest.source(searchSourceBuilder);
            log.info("Elasticsearch query for findRandomLinkByCategory: {}", searchSourceBuilder.toString());
            org.elasticsearch.action.search.SearchResponse response =
                    elasticsearchClientService.getEsClient().search(searchRequest, org.elasticsearch.client.RequestOptions.DEFAULT);
            for (org.elasticsearch.search.SearchHit hit : response.getHits().getHits()) {
                java.util.Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                LinkDocument document = elasticsearchClientService.convertToLinkDocument(sourceAsMap);
                document.setLinkId(hit.getId());
                return Optional.of(document);
            }
        } catch (Exception e) {
            log.error("Error in findRandomLinkByCategory for tenantId: {}, category: {}", tenantId, category, e);
        }
        return Optional.empty();
    }

    @Override
    public Long countByTenantIdAndCategory(Integer tenantId, String category) {
        org.elasticsearch.index.query.BoolQueryBuilder boolQuery = org.elasticsearch.index.query.QueryBuilders.boolQuery()
                .must(org.elasticsearch.index.query.QueryBuilders.termQuery("tenantId", tenantId))
                .must(org.elasticsearch.index.query.QueryBuilders.termQuery("categories", category))
                .must(org.elasticsearch.index.query.QueryBuilders.existsQuery("thumbPath"))
                .mustNot(org.elasticsearch.index.query.QueryBuilders.boolQuery()
                        .should(org.elasticsearch.index.query.QueryBuilders.termQuery("thumbPath", ""))
                        .should(org.elasticsearch.index.query.QueryBuilders.termQuery("thumbPath", "NA"))
                        .should(org.elasticsearch.index.query.QueryBuilders.termQuery("thumbPath", "null")));

        org.elasticsearch.search.builder.SearchSourceBuilder searchSourceBuilder = new org.elasticsearch.search.builder.SearchSourceBuilder();
        searchSourceBuilder.query(boolQuery);
        searchSourceBuilder.size(0);
        searchSourceBuilder.trackTotalHits(true);
        try {
            org.elasticsearch.action.search.SearchRequest searchRequest = new org.elasticsearch.action.search.SearchRequest("links");
            searchRequest.source(searchSourceBuilder);
            log.info("Elasticsearch query for countByTenantIdAndCategory: {}", searchSourceBuilder.toString());
            org.elasticsearch.action.search.SearchResponse response =
                    elasticsearchClientService.getEsClient().search(searchRequest, org.elasticsearch.client.RequestOptions.DEFAULT);
            return response.getHits().getTotalHits().value;
        } catch (Exception e) {
            log.error("Error in countByTenantIdAndCategory for tenantId: {}, category: {}", tenantId, category, e);
        }
        return 0L;
    }

    @Override
    public List<LinkDocument> findByTenantIdAndLinkId(Integer tenantId, Integer linkId) {
        org.elasticsearch.index.query.BoolQueryBuilder boolQuery = org.elasticsearch.index.query.QueryBuilders.boolQuery()
                .must(org.elasticsearch.index.query.QueryBuilders.termQuery("tenantId", tenantId))
                .must(org.elasticsearch.index.query.QueryBuilders.termQuery("linkId", linkId))
                .must(org.elasticsearch.index.query.QueryBuilders.existsQuery("thumbPath"))
                .mustNot(org.elasticsearch.index.query.QueryBuilders.boolQuery()
                        .should(org.elasticsearch.index.query.QueryBuilders.termQuery("thumbPath", ""))
                        .should(org.elasticsearch.index.query.QueryBuilders.termQuery("thumbPath", "NA"))
                        .should(org.elasticsearch.index.query.QueryBuilders.termQuery("thumbPath", "null")));

        org.elasticsearch.search.builder.SearchSourceBuilder searchSourceBuilder = new org.elasticsearch.search.builder.SearchSourceBuilder();
        searchSourceBuilder.query(boolQuery);
        searchSourceBuilder.size(100); // reasonable default page size

        List<LinkDocument> results = new java.util.ArrayList<>();
        try {
            org.elasticsearch.action.search.SearchRequest searchRequest = new org.elasticsearch.action.search.SearchRequest("links");
            searchRequest.source(searchSourceBuilder);
            log.info("Elasticsearch query for findByTenantIdAndLinkId: {}", searchSourceBuilder.toString());
            org.elasticsearch.action.search.SearchResponse response =
                    elasticsearchClientService.getEsClient().search(searchRequest, org.elasticsearch.client.RequestOptions.DEFAULT);
            for (org.elasticsearch.search.SearchHit hit : response.getHits().getHits()) {
                java.util.Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                LinkDocument document = elasticsearchClientService.convertToLinkDocument(sourceAsMap);
                document.setLinkId(hit.getId());
                results.add(document);
            }
        } catch (Exception e) {
            log.error("Error in findByTenantIdAndLinkId for tenantId: {}, linkId: {}", tenantId, linkId, e);
        }
        return results;
    }

    @Override
    public List<String> findAllDistinctCategories(Integer tenantId) {
        org.elasticsearch.index.query.BoolQueryBuilder boolQuery = org.elasticsearch.index.query.QueryBuilders.boolQuery()
                .must(org.elasticsearch.index.query.QueryBuilders.termQuery("tenantId", tenantId));

        org.elasticsearch.search.aggregations.AggregationBuilder aggregation =
                org.elasticsearch.search.aggregations.AggregationBuilders.terms("distinct_categories")
                        .field("categories.keyword")
                        .size(1000); // adjust as needed for expected cardinality

        org.elasticsearch.search.builder.SearchSourceBuilder searchSourceBuilder = new org.elasticsearch.search.builder.SearchSourceBuilder();
        searchSourceBuilder.query(boolQuery);
        searchSourceBuilder.aggregation(aggregation);
        searchSourceBuilder.size(0); // no hits, only aggregation

        List<String> categories = new java.util.ArrayList<>();
        try {
            org.elasticsearch.action.search.SearchRequest searchRequest = new org.elasticsearch.action.search.SearchRequest("links");
            searchRequest.source(searchSourceBuilder);
            log.info("Elasticsearch query for findAllDistinctCategories: {}", searchSourceBuilder.toString());
            org.elasticsearch.action.search.SearchResponse response =
                    elasticsearchClientService.getEsClient().search(searchRequest, org.elasticsearch.client.RequestOptions.DEFAULT);
            org.elasticsearch.search.aggregations.Aggregations aggs = response.getAggregations();
            if (aggs != null) {
                org.elasticsearch.search.aggregations.bucket.terms.Terms terms = aggs.get("distinct_categories");
                if (terms != null) {
                    for (org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket bucket : terms.getBuckets()) {
                        categories.add(bucket.getKeyAsString());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error in findAllDistinctCategories for tenantId: {}", tenantId, e);
        }
        return categories;
    }


    @Override
    public List<LinkDocument> findByTenantIdAndCategory(Integer tenantId, String category) {
        org.elasticsearch.index.query.BoolQueryBuilder boolQuery = org.elasticsearch.index.query.QueryBuilders.boolQuery()
                .must(org.elasticsearch.index.query.QueryBuilders.termQuery("tenantId", tenantId))
                .must(org.elasticsearch.index.query.QueryBuilders.termQuery("categories", category))
                .must(org.elasticsearch.index.query.QueryBuilders.existsQuery("thumbPath"))
                .mustNot(org.elasticsearch.index.query.QueryBuilders.boolQuery()
                        .should(org.elasticsearch.index.query.QueryBuilders.termQuery("thumbPath", ""))
                        .should(org.elasticsearch.index.query.QueryBuilders.termQuery("thumbPath", "NA"))
                        .should(org.elasticsearch.index.query.QueryBuilders.termQuery("thumbPath", "null")));

        org.elasticsearch.search.builder.SearchSourceBuilder searchSourceBuilder = new org.elasticsearch.search.builder.SearchSourceBuilder();
        searchSourceBuilder.query(boolQuery);
        searchSourceBuilder.size(1000); // reasonable default page size
        searchSourceBuilder.sort("hd", org.elasticsearch.search.sort.SortOrder.DESC);
        searchSourceBuilder.sort("trailerPresent", org.elasticsearch.search.sort.SortOrder.DESC);

        List<LinkDocument> results = new java.util.ArrayList<>();
        try {
            org.elasticsearch.action.search.SearchRequest searchRequest = new org.elasticsearch.action.search.SearchRequest("links");
            searchRequest.source(searchSourceBuilder);
            log.info("Elasticsearch query for findByTenantIdAndCategory: {}", searchSourceBuilder.toString());
            org.elasticsearch.action.search.SearchResponse response =
                    elasticsearchClientService.getEsClient().search(searchRequest, org.elasticsearch.client.RequestOptions.DEFAULT);
            for (org.elasticsearch.search.SearchHit hit : response.getHits().getHits()) {
                java.util.Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                LinkDocument document = elasticsearchClientService.convertToLinkDocument(sourceAsMap);
                document.setLinkId(hit.getId());
                results.add(document);
            }
        } catch (Exception e) {
            log.error("Error in findByTenantIdAndCategory for tenantId: {}, category: {}", tenantId, category, e);
        }
        return results;
    }

    @Override
    public List<LinkDocument> findByTenantIdAndCategoryOrderByRandomOrder(Integer tenantId, String category) {
        // No randomOrder in LinkDocument, so just return filtered list
        return findByTenantIdAndCategory(tenantId, category);
    }

    @Override
    public List<LinkDocument> findByCategoryWithFiltersPageable(Integer tenantId, String category, Integer minDuration, Integer maxDuration, String quality, int offset, int limit) {
        org.elasticsearch.index.query.BoolQueryBuilder boolQuery = org.elasticsearch.index.query.QueryBuilders.boolQuery()
                .must(org.elasticsearch.index.query.QueryBuilders.termQuery("tenantId", tenantId))
                .must(org.elasticsearch.index.query.QueryBuilders.termQuery("categories", category))
                .must(org.elasticsearch.index.query.QueryBuilders.existsQuery("thumbPath"))
                .mustNot(org.elasticsearch.index.query.QueryBuilders.boolQuery()
                        .should(org.elasticsearch.index.query.QueryBuilders.termQuery("thumbPath", ""))
                        .should(org.elasticsearch.index.query.QueryBuilders.termQuery("thumbPath", "NA"))
                        .should(org.elasticsearch.index.query.QueryBuilders.termQuery("thumbPath", "null")));
        if (minDuration != null || maxDuration != null) {
            org.elasticsearch.index.query.RangeQueryBuilder rangeQuery = org.elasticsearch.index.query.QueryBuilders.rangeQuery("duration");
            if (minDuration != null) rangeQuery.gte(minDuration);
            if (maxDuration != null) rangeQuery.lte(maxDuration);
            boolQuery.must(rangeQuery);
        }
        if (quality != null && !quality.isEmpty()) {
            boolQuery.must(org.elasticsearch.index.query.QueryBuilders.termQuery("quality", quality));
        }
        org.elasticsearch.search.builder.SearchSourceBuilder searchSourceBuilder = new org.elasticsearch.search.builder.SearchSourceBuilder();
        searchSourceBuilder.query(boolQuery);
        searchSourceBuilder.from(offset);
        searchSourceBuilder.size(limit);
        searchSourceBuilder.sort("hd", org.elasticsearch.search.sort.SortOrder.DESC);
        searchSourceBuilder.sort("trailerPresent", org.elasticsearch.search.sort.SortOrder.DESC);

        List<LinkDocument> results = new java.util.ArrayList<>();
        try {
            org.elasticsearch.action.search.SearchRequest searchRequest = new org.elasticsearch.action.search.SearchRequest("links");
            searchRequest.source(searchSourceBuilder);
            log.info("Elasticsearch query for findByCategoryWithFiltersPageable: {}", searchSourceBuilder.toString());
            org.elasticsearch.action.search.SearchResponse response =
                    elasticsearchClientService.getEsClient().search(searchRequest, org.elasticsearch.client.RequestOptions.DEFAULT);
            for (org.elasticsearch.search.SearchHit hit : response.getHits().getHits()) {
                java.util.Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                LinkDocument document = elasticsearchClientService.convertToLinkDocument(sourceAsMap);
                document.setLinkId(hit.getId());
                results.add(document);
            }
        } catch (Exception e) {
            log.error("Error in findByCategoryWithFiltersPageable for tenantId: {}, category: {}, minDuration: {}, maxDuration: {}, quality: {}, offset: {}, limit: {}", 
                    tenantId, category, minDuration, maxDuration, quality, offset, limit, e);
        }
        return results;
    }

    @Override
    public Long countByCategoryWithFilters(Integer tenantId, String category, Integer minDuration, Integer maxDuration, String quality) {
        org.elasticsearch.index.query.BoolQueryBuilder boolQuery = org.elasticsearch.index.query.QueryBuilders.boolQuery()
                .must(org.elasticsearch.index.query.QueryBuilders.termQuery("tenantId", tenantId))
                .must(org.elasticsearch.index.query.QueryBuilders.termQuery("categories", category))
                .must(org.elasticsearch.index.query.QueryBuilders.existsQuery("thumbPath"))
                .mustNot(org.elasticsearch.index.query.QueryBuilders.boolQuery()
                        .should(org.elasticsearch.index.query.QueryBuilders.termQuery("thumbPath", ""))
                        .should(org.elasticsearch.index.query.QueryBuilders.termQuery("thumbPath", "NA"))
                        .should(org.elasticsearch.index.query.QueryBuilders.termQuery("thumbPath", "null")));
        if (minDuration != null || maxDuration != null) {
            org.elasticsearch.index.query.RangeQueryBuilder rangeQuery = org.elasticsearch.index.query.QueryBuilders.rangeQuery("duration");
            if (minDuration != null) rangeQuery.gte(minDuration);
            if (maxDuration != null) rangeQuery.lte(maxDuration);
            boolQuery.must(rangeQuery);
        }
        if (quality != null && !quality.isEmpty()) {
            boolQuery.must(org.elasticsearch.index.query.QueryBuilders.termQuery("quality", quality));
        }
        org.elasticsearch.search.builder.SearchSourceBuilder searchSourceBuilder = new org.elasticsearch.search.builder.SearchSourceBuilder();
        searchSourceBuilder.query(boolQuery);
        searchSourceBuilder.size(0);
        searchSourceBuilder.trackTotalHits(true);
        try {
            org.elasticsearch.action.search.SearchRequest searchRequest = new org.elasticsearch.action.search.SearchRequest("links");
            searchRequest.source(searchSourceBuilder);
            log.info("Elasticsearch query for countByCategoryWithFilters: {}", searchSourceBuilder.toString());
            org.elasticsearch.action.search.SearchResponse response =
                    elasticsearchClientService.getEsClient().search(searchRequest, org.elasticsearch.client.RequestOptions.DEFAULT);
            return response.getHits().getTotalHits().value;
        } catch (Exception e) {
            log.error("Error in countByCategoryWithFilters for tenantId: {}, category: {}, minDuration: {}, maxDuration: {}, quality: {}", 
                    tenantId, category, minDuration, maxDuration, quality, e);
        }
        return 0L;
    }

    @Override
    public List<LinkDocument> findByCategoryWithFiltersExcludingLinkPageable(Integer tenantId, String category, Integer minDuration, Integer maxDuration, String quality, Integer excludeId, int offset, int limit) {
        org.elasticsearch.index.query.BoolQueryBuilder boolQuery = org.elasticsearch.index.query.QueryBuilders.boolQuery()
                .must(org.elasticsearch.index.query.QueryBuilders.termQuery("tenantId", tenantId))
                .must(org.elasticsearch.index.query.QueryBuilders.termQuery("categories", category))
                .must(org.elasticsearch.index.query.QueryBuilders.existsQuery("thumbPath"))
                .mustNot(org.elasticsearch.index.query.QueryBuilders.boolQuery()
                        .should(org.elasticsearch.index.query.QueryBuilders.termQuery("thumbPath", ""))
                        .should(org.elasticsearch.index.query.QueryBuilders.termQuery("thumbPath", "NA"))
                        .should(org.elasticsearch.index.query.QueryBuilders.termQuery("thumbPath", "null")));
        if (minDuration != null || maxDuration != null) {
            org.elasticsearch.index.query.RangeQueryBuilder rangeQuery = org.elasticsearch.index.query.QueryBuilders.rangeQuery("duration");
            if (minDuration != null) rangeQuery.gte(minDuration);
            if (maxDuration != null) rangeQuery.lte(maxDuration);
            boolQuery.must(rangeQuery);
        }
        if (quality != null && !quality.isEmpty()) {
            boolQuery.must(org.elasticsearch.index.query.QueryBuilders.termQuery("quality", quality));
        }
        if (excludeId != null) {
            boolQuery.mustNot(org.elasticsearch.index.query.QueryBuilders.termQuery("linkId", excludeId.toString()));
        }
        org.elasticsearch.search.builder.SearchSourceBuilder searchSourceBuilder = new org.elasticsearch.search.builder.SearchSourceBuilder();
        searchSourceBuilder.query(boolQuery);
        searchSourceBuilder.from(offset);
        searchSourceBuilder.size(limit);
        searchSourceBuilder.sort("hd", org.elasticsearch.search.sort.SortOrder.DESC);
        searchSourceBuilder.sort("trailerPresent", org.elasticsearch.search.sort.SortOrder.DESC);

        List<LinkDocument> results = new java.util.ArrayList<>();
        try {
            org.elasticsearch.action.search.SearchRequest searchRequest = new org.elasticsearch.action.search.SearchRequest("links");
            searchRequest.source(searchSourceBuilder);
            log.info("Elasticsearch query for findByCategoryWithFiltersExcludingLinkPageable: {}", searchSourceBuilder.toString());
            org.elasticsearch.action.search.SearchResponse response =
                    elasticsearchClientService.getEsClient().search(searchRequest, org.elasticsearch.client.RequestOptions.DEFAULT);
            for (org.elasticsearch.search.SearchHit hit : response.getHits().getHits()) {
                java.util.Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                LinkDocument document = elasticsearchClientService.convertToLinkDocument(sourceAsMap);
                document.setLinkId(hit.getId());
                results.add(document);
            }
        } catch (Exception e) {
            log.error("Error in findByCategoryWithFiltersExcludingLinkPageable for tenantId: {}, category: {}, minDuration: {}, maxDuration: {}, quality: {}, excludeId: {}, offset: {}, limit: {}", 
                    tenantId, category, minDuration, maxDuration, quality, excludeId, offset, limit, e);
        }
        return results;
    }


    @Override
    public List<Object[]> countByTenantIdAndCategories(Integer tenantId, List<String> categoryNames) {
        List<Object[]> result = new java.util.ArrayList<>();
        for (String category : categoryNames) {
            long count = countByTenantIdAndCategory(tenantId, category);
            result.add(new Object[]{category, count});
        }
        return result;
    }

    @Override
    public List<LinkDocument> findRandomLinksByCategoryNames(Integer tenantId, List<String> categoryNames) {
        List<LinkDocument> result = new java.util.ArrayList<>();
        for (String category : categoryNames) {
            findRandomLinkByCategory(tenantId, category).ifPresent(result::add);
        }
        return result;
    }

    @Override
    public void deleteByLinkId(Integer linkId) {
        try {
            org.elasticsearch.action.delete.DeleteRequest deleteRequest = new org.elasticsearch.action.delete.DeleteRequest("links", linkId.toString());
            elasticsearchClientService.getEsClient().delete(deleteRequest, org.elasticsearch.client.RequestOptions.DEFAULT);
        } catch (Exception e) {
            log.error("Error in deleteByLinkId for linkId: {}", linkId, e);
        }
    }

    @Override
    public List<LinkDocument> findByLinkIdAndCategory(Integer linkId, String category) {
        org.elasticsearch.index.query.BoolQueryBuilder boolQuery = org.elasticsearch.index.query.QueryBuilders.boolQuery()
                .must(org.elasticsearch.index.query.QueryBuilders.termQuery("linkId", linkId))
                .must(org.elasticsearch.index.query.QueryBuilders.termQuery("categories", category))
                .must(org.elasticsearch.index.query.QueryBuilders.existsQuery("thumbPath"))
                .mustNot(org.elasticsearch.index.query.QueryBuilders.boolQuery()
                        .should(org.elasticsearch.index.query.QueryBuilders.termQuery("thumbPath", ""))
                        .should(org.elasticsearch.index.query.QueryBuilders.termQuery("thumbPath", "NA"))
                        .should(org.elasticsearch.index.query.QueryBuilders.termQuery("thumbPath", "null")));
        org.elasticsearch.search.builder.SearchSourceBuilder searchSourceBuilder = new org.elasticsearch.search.builder.SearchSourceBuilder();
        searchSourceBuilder.query(boolQuery);
        searchSourceBuilder.size(100);
        List<LinkDocument> results = new java.util.ArrayList<>();
        try {
            org.elasticsearch.action.search.SearchRequest searchRequest = new org.elasticsearch.action.search.SearchRequest("links");
            searchRequest.source(searchSourceBuilder);
            log.info("Elasticsearch query for findByLinkIdAndCategory: {}", searchSourceBuilder.toString());
            org.elasticsearch.action.search.SearchResponse response =
                    elasticsearchClientService.getEsClient().search(searchRequest, org.elasticsearch.client.RequestOptions.DEFAULT);
            for (org.elasticsearch.search.SearchHit hit : response.getHits().getHits()) {
                java.util.Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                LinkDocument document = elasticsearchClientService.convertToLinkDocument(sourceAsMap);
                document.setLinkId(hit.getId());
                results.add(document);
            }
        } catch (Exception e) {
            log.error("Error in findByLinkIdAndCategory for linkId: {}, category: {}", linkId, category, e);
        }
        return results;
    }

    @Override
    public List<LinkDocument> findByCategoryAndTenantId(String category, Integer tenantId) {
        org.elasticsearch.index.query.BoolQueryBuilder boolQuery = org.elasticsearch.index.query.QueryBuilders.boolQuery()
                .must(org.elasticsearch.index.query.QueryBuilders.termQuery("categories", category))
                .must(org.elasticsearch.index.query.QueryBuilders.termQuery("tenantId", tenantId))
                .must(org.elasticsearch.index.query.QueryBuilders.existsQuery("thumbPath"))
                .mustNot(org.elasticsearch.index.query.QueryBuilders.boolQuery()
                        .should(org.elasticsearch.index.query.QueryBuilders.termQuery("thumbPath", ""))
                        .should(org.elasticsearch.index.query.QueryBuilders.termQuery("thumbPath", "NA"))
                        .should(org.elasticsearch.index.query.QueryBuilders.termQuery("thumbPath", "null")));
        org.elasticsearch.search.builder.SearchSourceBuilder searchSourceBuilder = new org.elasticsearch.search.builder.SearchSourceBuilder();
        searchSourceBuilder.query(boolQuery);
        searchSourceBuilder.size(1000);
        List<LinkDocument> results = new java.util.ArrayList<>();
        try {
            org.elasticsearch.action.search.SearchRequest searchRequest = new org.elasticsearch.action.search.SearchRequest("links");
            searchRequest.source(searchSourceBuilder);
            log.info("Elasticsearch query for findByCategoryAndTenantId: {}", searchSourceBuilder.toString());
            org.elasticsearch.action.search.SearchResponse response =
                    elasticsearchClientService.getEsClient().search(searchRequest, org.elasticsearch.client.RequestOptions.DEFAULT);
            for (org.elasticsearch.search.SearchHit hit : response.getHits().getHits()) {
                java.util.Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                LinkDocument document = elasticsearchClientService.convertToLinkDocument(sourceAsMap);
                document.setLinkId(hit.getId());
                results.add(document);
            }
        } catch (Exception e) {
            log.error("Error in findByCategoryAndTenantId for category: {}, tenantId: {}", category, tenantId, e);
        }
        return results;
    }

    @Override
    public List<LinkDocument> findByLinkId(Integer linkId) {
        org.elasticsearch.index.query.BoolQueryBuilder boolQuery = org.elasticsearch.index.query.QueryBuilders.boolQuery()
                .must(org.elasticsearch.index.query.QueryBuilders.termQuery("linkId", linkId))
                .must(org.elasticsearch.index.query.QueryBuilders.existsQuery("thumbPath"))
                .mustNot(org.elasticsearch.index.query.QueryBuilders.boolQuery()
                        .should(org.elasticsearch.index.query.QueryBuilders.termQuery("thumbPath", ""))
                        .should(org.elasticsearch.index.query.QueryBuilders.termQuery("thumbPath", "NA"))
                        .should(org.elasticsearch.index.query.QueryBuilders.termQuery("thumbPath", "null")));
        org.elasticsearch.search.builder.SearchSourceBuilder searchSourceBuilder = new org.elasticsearch.search.builder.SearchSourceBuilder();
        searchSourceBuilder.query(boolQuery);
        searchSourceBuilder.size(100);
        List<LinkDocument> results = new java.util.ArrayList<>();
        try {
            org.elasticsearch.action.search.SearchRequest searchRequest = new org.elasticsearch.action.search.SearchRequest("links");
            searchRequest.source(searchSourceBuilder);
            log.info("Elasticsearch query for findByLinkId: {}", searchSourceBuilder.toString());
            org.elasticsearch.action.search.SearchResponse response =
                    elasticsearchClientService.getEsClient().search(searchRequest, org.elasticsearch.client.RequestOptions.DEFAULT);
            for (org.elasticsearch.search.SearchHit hit : response.getHits().getHits()) {
                java.util.Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                LinkDocument document = elasticsearchClientService.convertToLinkDocument(sourceAsMap);
                document.setLinkId(hit.getId());
                results.add(document);
            }
        } catch (Exception e) {
            log.error("Error in findByLinkId for linkId: {}", linkId, e);
        }
        return results;
    }

    @Override
    public List<LinkDocument> findByTenantId(Integer tenantId) {
        org.elasticsearch.index.query.BoolQueryBuilder boolQuery = org.elasticsearch.index.query.QueryBuilders.boolQuery()
                .must(org.elasticsearch.index.query.QueryBuilders.termQuery("tenantId", tenantId))
                .must(org.elasticsearch.index.query.QueryBuilders.existsQuery("thumbPath"))
                .mustNot(org.elasticsearch.index.query.QueryBuilders.boolQuery()
                        .should(org.elasticsearch.index.query.QueryBuilders.termQuery("thumbPath", ""))
                        .should(org.elasticsearch.index.query.QueryBuilders.termQuery("thumbPath", "NA"))
                        .should(org.elasticsearch.index.query.QueryBuilders.termQuery("thumbPath", "null")));
        org.elasticsearch.search.builder.SearchSourceBuilder searchSourceBuilder = new org.elasticsearch.search.builder.SearchSourceBuilder();
        searchSourceBuilder.query(boolQuery);
        searchSourceBuilder.size(1000);
        List<LinkDocument> results = new java.util.ArrayList<>();
        try {
            org.elasticsearch.action.search.SearchRequest searchRequest = new org.elasticsearch.action.search.SearchRequest("links");
            searchRequest.source(searchSourceBuilder);
            log.info("Elasticsearch query for findByTenantId: {}", searchSourceBuilder.toString());
            org.elasticsearch.action.search.SearchResponse response =
                    elasticsearchClientService.getEsClient().search(searchRequest, org.elasticsearch.client.RequestOptions.DEFAULT);
            for (org.elasticsearch.search.SearchHit hit : response.getHits().getHits()) {
                java.util.Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                LinkDocument document = elasticsearchClientService.convertToLinkDocument(sourceAsMap);
                document.setLinkId(hit.getId());
                results.add(document);
            }
        } catch (Exception e) {
            log.error("Error in findByTenantId for tenantId: {}", tenantId, e);
        }
        return results;
    }

    /**
     * Alternative random method using random offset - more reliable for true randomness
     */
    public Optional<LinkDocument> findRandomLinkByCategoryWithOffset(Integer tenantId, String category) {
        // First, get the total count
        Long totalCount = countByTenantIdAndCategory(tenantId, category);
        if (totalCount == 0) {
            return Optional.empty();
        }

        // Generate random offset
        int randomOffset = (int) (Math.random() * totalCount);

        // Build ES query
        org.elasticsearch.index.query.BoolQueryBuilder boolQuery = org.elasticsearch.index.query.QueryBuilders.boolQuery()
                .must(org.elasticsearch.index.query.QueryBuilders.termQuery("tenantId", tenantId))
                .must(org.elasticsearch.index.query.QueryBuilders.termQuery("categories", category))
                .must(org.elasticsearch.index.query.QueryBuilders.existsQuery("thumbPath"))
                .mustNot(org.elasticsearch.index.query.QueryBuilders.boolQuery()
                        .should(org.elasticsearch.index.query.QueryBuilders.termQuery("thumbPath", ""))
                        .should(org.elasticsearch.index.query.QueryBuilders.termQuery("thumbPath", "NA"))
                        .should(org.elasticsearch.index.query.QueryBuilders.termQuery("thumbPath", "null")));

        org.elasticsearch.search.builder.SearchSourceBuilder searchSourceBuilder = new org.elasticsearch.search.builder.SearchSourceBuilder();
        searchSourceBuilder.query(boolQuery);
        searchSourceBuilder.from(randomOffset);
        searchSourceBuilder.size(1);

        try {
            org.elasticsearch.action.search.SearchRequest searchRequest = new org.elasticsearch.action.search.SearchRequest("links");
            searchRequest.source(searchSourceBuilder);
            log.info("Elasticsearch query for findRandomLinkByCategoryWithOffset: {}", searchSourceBuilder.toString());
            org.elasticsearch.action.search.SearchResponse response =
                    elasticsearchClientService.getEsClient().search(searchRequest, org.elasticsearch.client.RequestOptions.DEFAULT);
            for (org.elasticsearch.search.SearchHit hit : response.getHits().getHits()) {
                java.util.Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                LinkDocument document = elasticsearchClientService.convertToLinkDocument(sourceAsMap);
                document.setLinkId(hit.getId());
                return Optional.of(document);
            }
        } catch (Exception e) {
            log.error("Error in findRandomLinkByCategoryWithOffset for tenantId: {}, category: {}", tenantId, category, e);
        }
        return Optional.empty();
    }
} 