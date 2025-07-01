package com.fvp.repository;

import com.fvp.document.LinkDocument;
import com.fvp.document.ModelDocument;
import com.fvp.service.ElasticsearchClientService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
public class ElasticsearchLinkModelRepositoryImpl implements ElasticsearchLinkModelRepository {

  private final ElasticsearchClientService elasticsearchClientService;

  @Autowired
  public ElasticsearchLinkModelRepositoryImpl(
      ElasticsearchClientService elasticsearchClientService) {
    this.elasticsearchClientService = elasticsearchClientService;
  }

  @Override
  public Optional<ModelDocument> findRandomModelByName(Integer tenantId, String modelName) {
    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
        .must(QueryBuilders.termQuery("tenantId", tenantId))
        .must(QueryBuilders.termQuery("models", modelName));

    // First, get the total count for this query
    SearchSourceBuilder countBuilder = new SearchSourceBuilder();
    countBuilder.query(boolQuery);
    countBuilder.size(0);
    countBuilder.trackTotalHits(true);

    try {
      org.elasticsearch.action.search.SearchRequest countRequest = new org.elasticsearch.action.search.SearchRequest(
          "links");
      countRequest.source(countBuilder);
      org.elasticsearch.action.search.SearchResponse countResponse =
          elasticsearchClientService.getEsClient()
              .search(countRequest, org.elasticsearch.client.RequestOptions.DEFAULT);

      long totalCount = countResponse.getHits().getTotalHits().value;
      if (totalCount == 0) {
        return Optional.empty();
      }

      // Generate random offset
      int randomOffset = (int) (Math.random() * totalCount);

      // Use random offset for true randomness
      SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
      searchSourceBuilder.query(boolQuery);
      searchSourceBuilder.from(randomOffset);
      searchSourceBuilder.size(1);

      org.elasticsearch.action.search.SearchRequest searchRequest = new org.elasticsearch.action.search.SearchRequest(
          "links");
      searchRequest.source(searchSourceBuilder);
      log.info("Elasticsearch query for findRandomModelByName: {}", searchSourceBuilder.toString());
      org.elasticsearch.action.search.SearchResponse response =
          elasticsearchClientService.getEsClient()
              .search(searchRequest, org.elasticsearch.client.RequestOptions.DEFAULT);
      for (org.elasticsearch.search.SearchHit hit : response.getHits().getHits()) {
        ModelDocument document = elasticsearchClientService.convertToModelDocument(
            hit.getSourceAsMap());
        document.setId(hit.getId());
        return Optional.of(document);
      }
    } catch (Exception e) {
      log.error("Error in findRandomModelByName", e);
    }
    return Optional.empty();
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
      org.elasticsearch.action.search.SearchRequest searchRequest = new org.elasticsearch.action.search.SearchRequest(
          "links");
      searchRequest.source(searchSourceBuilder);
      log.info("Elasticsearch query for countByTenantIdAndModel: {}",
          searchSourceBuilder.toString());
      org.elasticsearch.action.search.SearchResponse response =
          elasticsearchClientService.getEsClient()
              .search(searchRequest, org.elasticsearch.client.RequestOptions.DEFAULT);
      return response.getHits().getTotalHits().value;
    } catch (Exception e) {
      log.error("Error in countByTenantIdAndModel", e);
    }
    return 0L;
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
      org.elasticsearch.action.search.SearchRequest searchRequest = new org.elasticsearch.action.search.SearchRequest(
          "links");
      searchRequest.source(searchSourceBuilder);
      log.info("Elasticsearch query for findByTenantIdAndModel: {}",
          searchSourceBuilder.toString());
      org.elasticsearch.action.search.SearchResponse response =
          elasticsearchClientService.getEsClient()
              .search(searchRequest, org.elasticsearch.client.RequestOptions.DEFAULT);
      for (org.elasticsearch.search.SearchHit hit : response.getHits().getHits()) {
        ModelDocument document = elasticsearchClientService.convertToModelDocument(
            hit.getSourceAsMap());
        document.setId(hit.getId());
        results.add(document);
      }
    } catch (Exception e) {
      log.error("Error in findByTenantIdAndModel", e);
    }
    return results;
  }

  @Override
  public List<LinkDocument> findByModelWithFiltersPageable(Integer tenantId, String modelName,
      Integer minAge, Integer maxAge, String country, int offset, int limit) {
    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
        .must(QueryBuilders.termQuery("tenantId", tenantId))
        .must(QueryBuilders.termQuery("models", modelName));
    if (minAge != null || maxAge != null) {
      RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("modelAge");
        if (minAge != null) {
            rangeQuery.gte(minAge);
        }
        if (maxAge != null) {
            rangeQuery.lte(maxAge);
        }
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
      org.elasticsearch.action.search.SearchRequest searchRequest = new org.elasticsearch.action.search.SearchRequest(
          "links");
      searchRequest.source(searchSourceBuilder);
      log.info("Elasticsearch query for findByModelWithFiltersPageable: {}",
          searchSourceBuilder.toString());
      org.elasticsearch.action.search.SearchResponse response =
          elasticsearchClientService.getEsClient()
              .search(searchRequest, org.elasticsearch.client.RequestOptions.DEFAULT);
      for (org.elasticsearch.search.SearchHit hit : response.getHits().getHits()) {
        LinkDocument document = elasticsearchClientService.convertToLinkDocument(
            hit.getSourceAsMap());
        document.setLinkId(hit.getId());
        results.add(document);
      }
    } catch (Exception e) {
      log.error("Error in findByModelWithFiltersPageable", e);
    }
    return results;
  }

  @Override
  public List<LinkDocument> findByModelWithFiltersExcludingLinkPageable(Integer tenantId,
      String modelName, Integer maxDuration, String quality, Integer excludeId, int offset,
      int limit) {
    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
        .must(QueryBuilders.termQuery("tenantId", tenantId))
        .must(QueryBuilders.termQuery("models", modelName));
    if (maxDuration != null) {
      boolQuery.must(QueryBuilders.rangeQuery("duration").lte(maxDuration));
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
      org.elasticsearch.action.search.SearchRequest searchRequest = new org.elasticsearch.action.search.SearchRequest(
          "links");
      searchRequest.source(searchSourceBuilder);
      log.info("Elasticsearch query for findByModelWithFiltersExcludingLinkPageable: {}",
          searchSourceBuilder.toString());
      org.elasticsearch.action.search.SearchResponse response =
          elasticsearchClientService.getEsClient()
              .search(searchRequest, org.elasticsearch.client.RequestOptions.DEFAULT);
      for (org.elasticsearch.search.SearchHit hit : response.getHits().getHits()) {
        LinkDocument document = elasticsearchClientService.convertToLinkDocument(
            hit.getSourceAsMap());
        document.setLinkId(hit.getId());
        results.add(document);
      }
    } catch (Exception e) {
      log.error("Error in findByModelWithFiltersExcludingLinkPageable", e);
    }
    return results;
  }

  @Override
  public List<LinkDocument> findByModelWithFiltersPageable(Integer tenantId, String modelName,
      Integer maxDuration, String quality, int offset, int limit) {
    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
        .must(QueryBuilders.termQuery("tenantId", tenantId))
        .must(QueryBuilders.termQuery("models", modelName));
    if (maxDuration != null) {
      boolQuery.must(QueryBuilders.rangeQuery("duration").lte(maxDuration));
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
      org.elasticsearch.action.search.SearchRequest searchRequest = new org.elasticsearch.action.search.SearchRequest(
          "links");
      searchRequest.source(searchSourceBuilder);
      log.info("Elasticsearch query for findByModelWithFiltersPageable (duration/quality): {}",
          searchSourceBuilder.toString());
      org.elasticsearch.action.search.SearchResponse response =
          elasticsearchClientService.getEsClient()
              .search(searchRequest, org.elasticsearch.client.RequestOptions.DEFAULT);
      for (org.elasticsearch.search.SearchHit hit : response.getHits().getHits()) {
        LinkDocument document = elasticsearchClientService.convertToLinkDocument(
            hit.getSourceAsMap());
        document.setLinkId(hit.getId());
        results.add(document);
      }
    } catch (Exception e) {
      log.error("Error in findByModelWithFiltersPageable", e);
    }
    return results;
  }

  @Override
  public Long countByModelWithFilters(Integer tenantId, String modelName, Integer maxDuration,
      String quality) {
    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
        .must(QueryBuilders.termQuery("tenantId", tenantId))
        .must(QueryBuilders.termQuery("models", modelName));
    if (maxDuration != null) {
      boolQuery.must(QueryBuilders.rangeQuery("duration").lte(maxDuration));
    }
    if (quality != null && !quality.isEmpty()) {
      boolQuery.must(QueryBuilders.termQuery("quality", quality));
    }
    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
    searchSourceBuilder.query(boolQuery);
    searchSourceBuilder.size(0);
    searchSourceBuilder.trackTotalHits(true);
    try {
      org.elasticsearch.action.search.SearchRequest searchRequest = new org.elasticsearch.action.search.SearchRequest(
          "links");
      searchRequest.source(searchSourceBuilder);
      log.info("Elasticsearch query for countByModelWithFilters (duration/quality): {}",
          searchSourceBuilder.toString());
      org.elasticsearch.action.search.SearchResponse response =
          elasticsearchClientService.getEsClient()
              .search(searchRequest, org.elasticsearch.client.RequestOptions.DEFAULT);
      return response.getHits().getTotalHits().value;
    } catch (Exception e) {
      log.error("Error in countByModelWithFilters", e);
    }
    return 0L;
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
      org.elasticsearch.action.search.SearchRequest searchRequest = new org.elasticsearch.action.search.SearchRequest(
          "links");
      searchRequest.source(searchSourceBuilder);
      log.info("Elasticsearch query for findAllDistinctModels: {}", searchSourceBuilder.toString());
      org.elasticsearch.action.search.SearchResponse response =
          elasticsearchClientService.getEsClient()
              .search(searchRequest, org.elasticsearch.client.RequestOptions.DEFAULT);
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
  public Optional<LinkDocument> findRandomLinkByModel(Integer tenantId, String modelName) {
    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
        .must(QueryBuilders.termQuery("tenantId", tenantId))
        .must(QueryBuilders.termQuery("models", modelName));

    // First, get the total count for this query
    SearchSourceBuilder countBuilder = new SearchSourceBuilder();
    countBuilder.query(boolQuery);
    countBuilder.size(0);
    countBuilder.trackTotalHits(true);

    try {
      org.elasticsearch.action.search.SearchRequest countRequest = new org.elasticsearch.action.search.SearchRequest(
          "links");
      countRequest.source(countBuilder);
      org.elasticsearch.action.search.SearchResponse countResponse =
          elasticsearchClientService.getEsClient()
              .search(countRequest, org.elasticsearch.client.RequestOptions.DEFAULT);

      long totalCount = countResponse.getHits().getTotalHits().value;
      if (totalCount == 0) {
        return Optional.empty();
      }

      // Generate random offset
      int randomOffset = (int) (Math.random() * totalCount);

      // Use random offset for true randomness
      SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
      searchSourceBuilder.query(boolQuery);
      searchSourceBuilder.from(randomOffset);
      searchSourceBuilder.size(1);

      org.elasticsearch.action.search.SearchRequest searchRequest = new org.elasticsearch.action.search.SearchRequest(
          "links");
      searchRequest.source(searchSourceBuilder);
      log.info("Elasticsearch query for findRandomLinkByModel: {}", searchSourceBuilder.toString());
      org.elasticsearch.action.search.SearchResponse response =
          elasticsearchClientService.getEsClient()
              .search(searchRequest, org.elasticsearch.client.RequestOptions.DEFAULT);
      for (org.elasticsearch.search.SearchHit hit : response.getHits().getHits()) {
        LinkDocument document = elasticsearchClientService.convertToLinkDocument(
            hit.getSourceAsMap());
        document.setLinkId(hit.getId());
        return Optional.of(document);
      }
    } catch (Exception e) {
      log.error("Error in findRandomLinkByModel", e);
    }
    return Optional.empty();
  }
} 