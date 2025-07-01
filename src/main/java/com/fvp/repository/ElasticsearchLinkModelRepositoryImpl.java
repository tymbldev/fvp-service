package com.fvp.repository;

import com.fvp.document.ModelDocument;
import com.fvp.document.LinkDocument;
import com.fvp.service.ElasticsearchClientService;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
public class ElasticsearchLinkModelRepositoryImpl implements ElasticsearchLinkModelRepository {
    private final ElasticsearchClientService elasticsearchClientService;

    @Autowired
    public ElasticsearchLinkModelRepositoryImpl(ElasticsearchClientService elasticsearchClientService) {
        this.elasticsearchClientService = elasticsearchClientService;
    }

    @Override
    public Optional<ModelDocument> findRandomModelByTenantId(Integer tenantId) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("tenantId", tenantId));
        FunctionScoreQueryBuilder functionScoreQuery = QueryBuilders.functionScoreQuery(
                boolQuery,
                new FunctionScoreQueryBuilder.FilterFunctionBuilder[]{
                        new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                                org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders.randomFunction()
                        )
                }
        );
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(functionScoreQuery);
        searchSourceBuilder.size(1);
        try {
            org.elasticsearch.action.search.SearchRequest searchRequest = new org.elasticsearch.action.search.SearchRequest("links");
            searchRequest.source(searchSourceBuilder);
            log.info("Elasticsearch query for findRandomModelByTenantId: {}", searchSourceBuilder.toString());
            org.elasticsearch.action.search.SearchResponse response =
                    elasticsearchClientService.getEsClient().search(searchRequest, org.elasticsearch.client.RequestOptions.DEFAULT);
            for (org.elasticsearch.search.SearchHit hit : response.getHits().getHits()) {
                ModelDocument document = elasticsearchClientService.convertToModelDocument(hit.getSourceAsMap());
                document.setId(hit.getId());
                return Optional.of(document);
            }
        } catch (Exception e) {
            log.error("Error in findRandomModelByTenantId", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<ModelDocument> findRandomModelByName(Integer tenantId, String modelName) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("tenantId", tenantId))
                .must(QueryBuilders.termQuery("models", modelName));
        FunctionScoreQueryBuilder functionScoreQuery = QueryBuilders.functionScoreQuery(
                boolQuery,
                new FunctionScoreQueryBuilder.FilterFunctionBuilder[]{
                        new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                                org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders.randomFunction()
                        )
                }
        );
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(functionScoreQuery);
        searchSourceBuilder.size(1);
        try {
            org.elasticsearch.action.search.SearchRequest searchRequest = new org.elasticsearch.action.search.SearchRequest("links");
            searchRequest.source(searchSourceBuilder);
            log.info("Elasticsearch query for findRandomModelByName: {}", searchSourceBuilder.toString());
            org.elasticsearch.action.search.SearchResponse response =
                    elasticsearchClientService.getEsClient().search(searchRequest, org.elasticsearch.client.RequestOptions.DEFAULT);
            for (org.elasticsearch.search.SearchHit hit : response.getHits().getHits()) {
                ModelDocument document = elasticsearchClientService.convertToModelDocument(hit.getSourceAsMap());
                document.setId(hit.getId());
                return Optional.of(document);
            }
        } catch (Exception e) {
            log.error("Error in findRandomModelByName", e);
        }
        return Optional.empty();
    }

    @Override
    public Long countByTenantId(Integer tenantId) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("tenantId", tenantId));
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQuery);
        searchSourceBuilder.size(0);
        searchSourceBuilder.trackTotalHits(true);
        try {
            org.elasticsearch.action.search.SearchRequest searchRequest = new org.elasticsearch.action.search.SearchRequest("links");
            searchRequest.source(searchSourceBuilder);
            log.info("Elasticsearch query for countByTenantId: {}", searchSourceBuilder.toString());
            org.elasticsearch.action.search.SearchResponse response =
                    elasticsearchClientService.getEsClient().search(searchRequest, org.elasticsearch.client.RequestOptions.DEFAULT);
            return response.getHits().getTotalHits().value;
        } catch (Exception e) {
            log.error("Error in countByTenantId", e);
        }
        return 0L;
    }

    @Override
    public Long countByTenantIdAndModel(Integer tenantId, String modelName) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("tenantId", tenantId))
                .must(QueryBuilders.termQuery("models", modelName));
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQuery);
        searchSourceBuilder.size(0);
        searchSourceBuilder.trackTotalHits(true);
        try {
            org.elasticsearch.action.search.SearchRequest searchRequest = new org.elasticsearch.action.search.SearchRequest("links");
            searchRequest.source(searchSourceBuilder);
            log.info("Elasticsearch query for countByTenantIdAndModel: {}", searchSourceBuilder.toString());
            org.elasticsearch.action.search.SearchResponse response =
                    elasticsearchClientService.getEsClient().search(searchRequest, org.elasticsearch.client.RequestOptions.DEFAULT);
            return response.getHits().getTotalHits().value;
        } catch (Exception e) {
            log.error("Error in countByTenantIdAndModel", e);
        }
        return 0L;
    }

    @Override
    public List<ModelDocument> findByTenantId(Integer tenantId) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("tenantId", tenantId));
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQuery);
        searchSourceBuilder.size(1000);
        List<ModelDocument> results = new ArrayList<>();
        try {
            org.elasticsearch.action.search.SearchRequest searchRequest = new org.elasticsearch.action.search.SearchRequest("links");
            searchRequest.source(searchSourceBuilder);
            log.info("Elasticsearch query for findByTenantId: {}", searchSourceBuilder.toString());
            org.elasticsearch.action.search.SearchResponse response =
                    elasticsearchClientService.getEsClient().search(searchRequest, org.elasticsearch.client.RequestOptions.DEFAULT);
            for (org.elasticsearch.search.SearchHit hit : response.getHits().getHits()) {
                ModelDocument document = elasticsearchClientService.convertToModelDocument(hit.getSourceAsMap());
                document.setId(hit.getId());
                results.add(document);
            }
        } catch (Exception e) {
            log.error("Error in findByTenantId", e);
        }
        return results;
    }

    @Override
    public List<ModelDocument> findByTenantIdAndModel(Integer tenantId, String modelName) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("tenantId", tenantId))
                .must(QueryBuilders.termQuery("models", modelName));
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQuery);
        searchSourceBuilder.size(1000);
        List<ModelDocument> results = new ArrayList<>();
        try {
            org.elasticsearch.action.search.SearchRequest searchRequest = new org.elasticsearch.action.search.SearchRequest("links");
            searchRequest.source(searchSourceBuilder);
            log.info("Elasticsearch query for findByTenantIdAndModel: {}", searchSourceBuilder.toString());
            org.elasticsearch.action.search.SearchResponse response =
                    elasticsearchClientService.getEsClient().search(searchRequest, org.elasticsearch.client.RequestOptions.DEFAULT);
            for (org.elasticsearch.search.SearchHit hit : response.getHits().getHits()) {
                ModelDocument document = elasticsearchClientService.convertToModelDocument(hit.getSourceAsMap());
                document.setId(hit.getId());
                results.add(document);
            }
        } catch (Exception e) {
            log.error("Error in findByTenantIdAndModel", e);
        }
        return results;
    }

    @Override
    public List<ModelDocument> findByModelName(String modelName) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("models", modelName));
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQuery);
        searchSourceBuilder.size(1000);
        List<ModelDocument> results = new ArrayList<>();
        try {
            org.elasticsearch.action.search.SearchRequest searchRequest = new org.elasticsearch.action.search.SearchRequest("links");
            searchRequest.source(searchSourceBuilder);
            log.info("Elasticsearch query for findByModelName: {}", searchSourceBuilder.toString());
            org.elasticsearch.action.search.SearchResponse response =
                    elasticsearchClientService.getEsClient().search(searchRequest, org.elasticsearch.client.RequestOptions.DEFAULT);
            for (org.elasticsearch.search.SearchHit hit : response.getHits().getHits()) {
                ModelDocument document = elasticsearchClientService.convertToModelDocument(hit.getSourceAsMap());
                document.setId(hit.getId());
                results.add(document);
            }
        } catch (Exception e) {
            log.error("Error in findByModelName", e);
        }
        return results;
    }

    @Override
    public List<LinkDocument> findByModelWithFiltersPageable(Integer tenantId, String modelName, Integer minAge, Integer maxAge, String country, int offset, int limit) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("tenantId", tenantId))
                .must(QueryBuilders.termQuery("models", modelName));
        if (minAge != null || maxAge != null) {
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("modelAge");
            if (minAge != null) rangeQuery.gte(minAge);
            if (maxAge != null) rangeQuery.lte(maxAge);
            boolQuery.must(rangeQuery);
        }
        if (country != null && !country.isEmpty()) {
            boolQuery.must(QueryBuilders.termQuery("modelCountry", country));
        }
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQuery);
        searchSourceBuilder.from(offset);
        searchSourceBuilder.size(limit);
        searchSourceBuilder.sort("hd", SortOrder.DESC);
        searchSourceBuilder.sort("trailerPresent", SortOrder.DESC);

        List<LinkDocument> results = new ArrayList<>();
        try {
            org.elasticsearch.action.search.SearchRequest searchRequest = new org.elasticsearch.action.search.SearchRequest("links");
            searchRequest.source(searchSourceBuilder);
            log.info("Elasticsearch query for findByModelWithFiltersPageable: {}", searchSourceBuilder.toString());
            org.elasticsearch.action.search.SearchResponse response =
                    elasticsearchClientService.getEsClient().search(searchRequest, org.elasticsearch.client.RequestOptions.DEFAULT);
            for (org.elasticsearch.search.SearchHit hit : response.getHits().getHits()) {
                LinkDocument document = elasticsearchClientService.convertToLinkDocument(hit.getSourceAsMap());
                document.setLinkId(hit.getId());
                results.add(document);
            }
        } catch (Exception e) {
            log.error("Error in findByModelWithFiltersPageable", e);
        }
        return results;
    }

    @Override
    public Long countByModelWithFilters(Integer tenantId, String modelName, Integer minAge, Integer maxAge, String country) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("tenantId", tenantId))
                .must(QueryBuilders.termQuery("models", modelName));
        if (minAge != null || maxAge != null) {
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("modelAge");
            if (minAge != null) rangeQuery.gte(minAge);
            if (maxAge != null) rangeQuery.lte(maxAge);
            boolQuery.must(rangeQuery);
        }
        if (country != null && !country.isEmpty()) {
            boolQuery.must(QueryBuilders.termQuery("modelCountry", country));
        }
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQuery);
        searchSourceBuilder.size(0);
        searchSourceBuilder.trackTotalHits(true);
        try {
            org.elasticsearch.action.search.SearchRequest searchRequest = new org.elasticsearch.action.search.SearchRequest("links");
            searchRequest.source(searchSourceBuilder);
            log.info("Elasticsearch query for countByModelWithFilters: {}", searchSourceBuilder.toString());
            org.elasticsearch.action.search.SearchResponse response =
                    elasticsearchClientService.getEsClient().search(searchRequest, org.elasticsearch.client.RequestOptions.DEFAULT);
            return response.getHits().getTotalHits().value;
        } catch (Exception e) {
            log.error("Error in countByModelWithFilters", e);
        }
        return 0L;
    }

    @Override
    public List<LinkDocument> findByModelWithFiltersExcludingModelPageable(Integer tenantId, String modelName, Integer minAge, Integer maxAge, String country, String excludeModelName, int offset, int limit) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("tenantId", tenantId))
                .must(QueryBuilders.termQuery("models", modelName));
        if (minAge != null || maxAge != null) {
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("modelAge");
            if (minAge != null) rangeQuery.gte(minAge);
            if (maxAge != null) rangeQuery.lte(maxAge);
            boolQuery.must(rangeQuery);
        }
        if (country != null && !country.isEmpty()) {
            boolQuery.must(QueryBuilders.termQuery("modelCountry", country));
        }
        if (excludeModelName != null && !excludeModelName.isEmpty()) {
            boolQuery.mustNot(QueryBuilders.termQuery("models", excludeModelName));
        }
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQuery);
        searchSourceBuilder.from(offset);
        searchSourceBuilder.size(limit);
        searchSourceBuilder.sort("hd", SortOrder.DESC);
        searchSourceBuilder.sort("trailerPresent", SortOrder.DESC);

        List<LinkDocument> results = new ArrayList<>();
        try {
            org.elasticsearch.action.search.SearchRequest searchRequest = new org.elasticsearch.action.search.SearchRequest("links");
            searchRequest.source(searchSourceBuilder);
            log.info("Elasticsearch query for findByModelWithFiltersExcludingModelPageable: {}", searchSourceBuilder.toString());
            org.elasticsearch.action.search.SearchResponse response =
                    elasticsearchClientService.getEsClient().search(searchRequest, org.elasticsearch.client.RequestOptions.DEFAULT);
            for (org.elasticsearch.search.SearchHit hit : response.getHits().getHits()) {
                LinkDocument document = elasticsearchClientService.convertToLinkDocument(hit.getSourceAsMap());
                document.setLinkId(hit.getId());
                results.add(document);
            }
        } catch (Exception e) {
            log.error("Error in findByModelWithFiltersExcludingModelPageable", e);
        }
        return results;
    }

    @Override
    public List<LinkDocument> findByModelWithFiltersExcludingLinkPageable(Integer tenantId, String modelName, Integer maxDuration, String quality, Integer excludeId, int offset, int limit) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("tenantId", tenantId))
                .must(QueryBuilders.termQuery("models", modelName));
        if (maxDuration != null) {
            boolQuery.must(QueryBuilders.rangeQuery("linkDuration").lte(maxDuration));
        }
        if (quality != null && !quality.isEmpty()) {
            boolQuery.must(QueryBuilders.termQuery("quality", quality));
        }
        if (excludeId != null) {
            boolQuery.mustNot(QueryBuilders.termQuery("linkId", excludeId.toString()));
        }
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQuery);
        searchSourceBuilder.from(offset);
        searchSourceBuilder.size(limit);
        searchSourceBuilder.sort("hd", SortOrder.DESC);
        searchSourceBuilder.sort("trailerPresent", SortOrder.DESC);

        List<LinkDocument> results = new ArrayList<>();
        try {
            org.elasticsearch.action.search.SearchRequest searchRequest = new org.elasticsearch.action.search.SearchRequest("links");
            searchRequest.source(searchSourceBuilder);
            log.info("Elasticsearch query for findByModelWithFiltersExcludingLinkPageable: {}", searchSourceBuilder.toString());
            org.elasticsearch.action.search.SearchResponse response =
                    elasticsearchClientService.getEsClient().search(searchRequest, org.elasticsearch.client.RequestOptions.DEFAULT);
            for (org.elasticsearch.search.SearchHit hit : response.getHits().getHits()) {
                LinkDocument document = elasticsearchClientService.convertToLinkDocument(hit.getSourceAsMap());
                document.setLinkId(hit.getId());
                results.add(document);
            }
        } catch (Exception e) {
            log.error("Error in findByModelWithFiltersExcludingLinkPageable", e);
        }
        return results;
    }

    @Override
    public List<LinkDocument> findByModelWithFiltersPageable(Integer tenantId, String modelName, Integer maxDuration, String quality, int offset, int limit) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("tenantId", tenantId))
                .must(QueryBuilders.termQuery("models", modelName));
        if (maxDuration != null) {
            boolQuery.must(QueryBuilders.rangeQuery("linkDuration").lte(maxDuration));
        }
        if (quality != null && !quality.isEmpty()) {
            boolQuery.must(QueryBuilders.termQuery("quality", quality));
        }
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQuery);
        searchSourceBuilder.from(offset);
        searchSourceBuilder.size(limit);
        searchSourceBuilder.sort("hd", SortOrder.DESC);
        searchSourceBuilder.sort("trailerPresent", SortOrder.DESC);

        List<LinkDocument> results = new ArrayList<>();
        try {
            org.elasticsearch.action.search.SearchRequest searchRequest = new org.elasticsearch.action.search.SearchRequest("links");
            searchRequest.source(searchSourceBuilder);
            log.info("Elasticsearch query for findByModelWithFiltersPageable (duration/quality): {}", searchSourceBuilder.toString());
            org.elasticsearch.action.search.SearchResponse response =
                    elasticsearchClientService.getEsClient().search(searchRequest, org.elasticsearch.client.RequestOptions.DEFAULT);
            for (org.elasticsearch.search.SearchHit hit : response.getHits().getHits()) {
                LinkDocument document = elasticsearchClientService.convertToLinkDocument(hit.getSourceAsMap());
                document.setLinkId(hit.getId());
                results.add(document);
            }
        } catch (Exception e) {
            log.error("Error in findByModelWithFiltersPageable", e);
        }
        return results;
    }

    @Override
    public Long countByModelWithFilters(Integer tenantId, String modelName, Integer maxDuration, String quality) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("tenantId", tenantId))
                .must(QueryBuilders.termQuery("models", modelName));
        if (maxDuration != null) {
            boolQuery.must(QueryBuilders.rangeQuery("linkDuration").lte(maxDuration));
        }
        if (quality != null && !quality.isEmpty()) {
            boolQuery.must(QueryBuilders.termQuery("quality", quality));
        }
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQuery);
        searchSourceBuilder.size(0);
        searchSourceBuilder.trackTotalHits(true);
        try {
            org.elasticsearch.action.search.SearchRequest searchRequest = new org.elasticsearch.action.search.SearchRequest("links");
            searchRequest.source(searchSourceBuilder);
            log.info("Elasticsearch query for countByModelWithFilters (duration/quality): {}", searchSourceBuilder.toString());
            org.elasticsearch.action.search.SearchResponse response =
                    elasticsearchClientService.getEsClient().search(searchRequest, org.elasticsearch.client.RequestOptions.DEFAULT);
            return response.getHits().getTotalHits().value;
        } catch (Exception e) {
            log.error("Error in countByModelWithFilters", e);
        }
        return 0L;
    }

    @Override
    public List<ModelDocument> findRandomModelsByNames(Integer tenantId, List<String> modelNames) {
        List<ModelDocument> result = new ArrayList<>();
        for (String modelName : modelNames) {
            findRandomModelByName(tenantId, modelName).ifPresent(result::add);
        }
        return result;
    }

    @Override
    public List<Object[]> countByTenantIdAndModels(Integer tenantId, List<String> modelNames) {
        List<Object[]> result = new ArrayList<>();
        for (String modelName : modelNames) {
            long count = countByTenantIdAndModel(tenantId, modelName);
            result.add(new Object[]{modelName, count});
        }
        return result;
    }

    @Override
    public List<String> findAllDistinctModels(Integer tenantId) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("tenantId", tenantId));
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQuery);
        searchSourceBuilder.size(0);
        searchSourceBuilder.aggregation(AggregationBuilders.terms("distinct_models")
                .field("models")
                .size(10000));
        List<String> models = new ArrayList<>();
        try {
            org.elasticsearch.action.search.SearchRequest searchRequest = new org.elasticsearch.action.search.SearchRequest("links");
            searchRequest.source(searchSourceBuilder);
            log.info("Elasticsearch query for findAllDistinctModels: {}", searchSourceBuilder.toString());
            org.elasticsearch.action.search.SearchResponse response =
                    elasticsearchClientService.getEsClient().search(searchRequest, org.elasticsearch.client.RequestOptions.DEFAULT);
            org.elasticsearch.search.aggregations.Aggregations aggs = response.getAggregations();
            if (aggs != null) {
                Terms terms = aggs.get("distinct_models");
                if (terms != null) {
                    for (Terms.Bucket bucket : terms.getBuckets()) {
                        models.add(bucket.getKeyAsString());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error in findAllDistinctModels", e);
        }
        return models;
    }

    @Override
    public void deleteByModelName(String modelName) {
        try {
            org.elasticsearch.action.delete.DeleteRequest deleteRequest = new org.elasticsearch.action.delete.DeleteRequest("links", modelName);
            elasticsearchClientService.getEsClient().delete(deleteRequest, org.elasticsearch.client.RequestOptions.DEFAULT);
        } catch (Exception e) {
            log.error("Error in deleteByModelName", e);
        }
    }

    @Override
    public Optional<LinkDocument> findRandomLinkByModel(Integer tenantId, String modelName) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("tenantId", tenantId))
                .must(QueryBuilders.termQuery("models", modelName));
        FunctionScoreQueryBuilder functionScoreQuery = QueryBuilders.functionScoreQuery(
                boolQuery,
                new FunctionScoreQueryBuilder.FilterFunctionBuilder[]{
                        new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                                org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders.randomFunction()
                        )
                }
        );
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(functionScoreQuery);
        searchSourceBuilder.size(1);
        try {
            org.elasticsearch.action.search.SearchRequest searchRequest = new org.elasticsearch.action.search.SearchRequest("links");
            searchRequest.source(searchSourceBuilder);
            log.info("Elasticsearch query for findRandomLinkByModel: {}", searchSourceBuilder.toString());
            org.elasticsearch.action.search.SearchResponse response =
                    elasticsearchClientService.getEsClient().search(searchRequest, org.elasticsearch.client.RequestOptions.DEFAULT);
            for (org.elasticsearch.search.SearchHit hit : response.getHits().getHits()) {
                LinkDocument document = elasticsearchClientService.convertToLinkDocument(hit.getSourceAsMap());
                document.setLinkId(hit.getId());
                return Optional.of(document);
            }
        } catch (Exception e) {
            log.error("Error in findRandomLinkByModel", e);
        }
        return Optional.empty();
    }
} 