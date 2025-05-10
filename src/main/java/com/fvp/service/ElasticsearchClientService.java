package com.fvp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fvp.document.CategoryDocument;
import com.fvp.document.LinkDocument;
import com.fvp.document.ModelDocument;
import com.fvp.dto.AutosuggestItem;
import com.fvp.util.LoggingUtil;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;

@Service
public class ElasticsearchClientService {

  private static final Logger logger = LoggingUtil.getLogger(ElasticsearchClientService.class);
  private static final String LINKS_INDEX = "links";
  private static final String CATEGORIES_INDEX = "categories";
  private static final String MODELS_INDEX = "models";
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final RestHighLevelClient esClient;
  private final boolean elasticsearchEnabled;

  @Autowired
  public ElasticsearchClientService(
      RestHighLevelClient esClient,
      @Value("${elasticsearch.enabled:false}") boolean elasticsearchEnabled) {
    this.esClient = esClient;
    this.elasticsearchEnabled = elasticsearchEnabled;
    
    if (elasticsearchEnabled) {
    try {
      ensureIndexExists(LINKS_INDEX);
      ensureIndexExists(CATEGORIES_INDEX);
      ensureIndexExists(MODELS_INDEX);
    } catch (Exception e) {
      logger.error("Error ensuring indexes exist: {}", e.getMessage(), e);
      }
    } else {
      logger.info("Elasticsearch is disabled. Operations will be logged but not executed.");
    }
  }

  private void ensureIndexExists(String indexName) {
    if (!elasticsearchEnabled) {
      logger.debug("Elasticsearch disabled: skipping index check for {}", indexName);
      return;
    }
    
    LoggingUtil.logOperationTime(logger, "ensure elasticsearch index exists: " + indexName, () -> {
      try {
        if (!esClient.indices().exists(new GetIndexRequest(indexName), RequestOptions.DEFAULT)) {
          CreateIndexRequest request = new CreateIndexRequest(indexName);
          request.settings(Settings.builder()
              .put("index.number_of_shards", 1)
              .put("index.number_of_replicas", 0));
          esClient.indices().create(request, RequestOptions.DEFAULT);
          logger.info("Created Elasticsearch index: {}", indexName);
        }
        return null;
      } catch (Exception e) {
        logger.error("Failed to ensure index exists: {}", e.getMessage(), e);
        throw new RuntimeException("Failed to ensure index exists", e);
      }
    });
  }

  public void saveLinkDocument(LinkDocument document) {
    if (!elasticsearchEnabled) {
      logger.debug("Elasticsearch disabled: skipping document save for ID={}", 
          document != null ? document.getId() : "null");
      return;
    }
    
    LoggingUtil.logOperationTime(logger, "save link document to elasticsearch", () -> {
      try {
        if (document == null) {
          logger.error("Cannot index null document");
          return null;
        }

        if (document.getId() == null || document.getId().isEmpty()) {
          logger.error("Document ID is required for indexing");
          return null;
        }

        Map<String, Object> documentMap = convertToMap(document);
        logger.info("Indexing document: {}", documentMap);

        // Use low-level client directly
        try {
          org.elasticsearch.client.Request lowLevelRequest = new org.elasticsearch.client.Request(
              "PUT",
              "/" + LINKS_INDEX + "/_doc/" + document.getId()
          );

          // Convert map to JSON string
          String jsonBody = objectMapper.writeValueAsString(documentMap);
          lowLevelRequest.setJsonEntity(jsonBody);

          org.elasticsearch.client.Response lowLevelResponse =
              esClient.getLowLevelClient().performRequest(lowLevelRequest);

          int statusCode = lowLevelResponse.getStatusLine().getStatusCode();
          if (statusCode >= 200 && statusCode < 300) {
              logger.info("Document indexed successfully: ID={}", document.getId());
          } else {
            logger.warn("Indexing returned non-success status: {}", statusCode);
            throw new RuntimeException("Failed to index document: HTTP " + statusCode);
          }
        } catch (Exception e) {
          logger.error("Error indexing document: {}", e.getMessage(), e);
          throw new RuntimeException("Failed to index document", e);
        }

        return null;
      } catch (Exception e) {
        logger.error("Error in document indexing process: {}", e.getMessage(), e);
        throw new RuntimeException("Failed to complete document indexing", e);
      }
    });
  }


  public List<LinkDocument> searchByTitleOrText(String link, String searchableText) {
    if (!elasticsearchEnabled) {
      logger.debug("Elasticsearch disabled: skipping search for link={}, text={}", link, searchableText);
      return new ArrayList<>();
    }
    
    return LoggingUtil.logOperationTime(logger, "search documents by title or text", () -> {
      List<LinkDocument> results = new ArrayList<>();
      try {
        SearchRequest searchRequest = new SearchRequest(LINKS_INDEX);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        if (link != null && !link.isEmpty()) {
          searchSourceBuilder.query(QueryBuilders.matchQuery("title", link));
        } else if (searchableText != null && !searchableText.isEmpty()) {
          searchSourceBuilder.query(QueryBuilders.matchQuery("searchableText", searchableText));
        } else {
          logger.warn("Both title and searchableText are null or empty");
          return results;
        }

        searchRequest.source(searchSourceBuilder);

        SearchResponse response = esClient.search(searchRequest, RequestOptions.DEFAULT);

        for (SearchHit hit : response.getHits().getHits()) {
          Map<String, Object> sourceAsMap = hit.getSourceAsMap();
          LinkDocument document = convertToLinkDocument(sourceAsMap);
          document.setId(hit.getId());
          results.add(document);
        }

        logger.info("Found {} documents matching search criteria", results.size());
      } catch (Exception e) {
        logger.error("Error searching documents: {}", e.getMessage(), e);
        throw new RuntimeException("Failed to search documents", e);
      }
      return results;
    });
  }


  private Map<String, Object> convertToMap(LinkDocument document) {
    Map<String, Object> map = new HashMap<>();
    map.put("id", document.getId());
    map.put("tenantId", document.getTenantId());
    map.put("title", document.getTitle());
    map.put("link", document.getLink());
    map.put("thumbnail", document.getThumbnail());
    map.put("thumbPath", document.getThumbPath());
    map.put("duration", document.getDuration());
    map.put("source", document.getSource());

    // Format date for Elasticsearch using ISO-8601 format
    if (document.getCreatedAt() != null) {
      SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
      dateFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
      map.put("createdAt", dateFormat.format(document.getCreatedAt()));
    } else {
      map.put("createdAt", null);
    }

    map.put("trailer", document.getTrailer());
    map.put("searchableText", document.getSearchableText());
    map.put("categories", document.getCategories());
    map.put("models", document.getModels());
    return map;
  }

  private LinkDocument convertToLinkDocument(Map<String, Object> map) {
    LinkDocument document = new LinkDocument();
    document.setId((String) map.get("id"));
    document.setTenantId((Integer) map.get("tenantId"));
    document.setTitle((String) map.get("title"));
    document.setLink((String) map.get("link"));
    document.setThumbnail((String) map.get("thumbnail"));
    document.setThumbPath((String) map.get("thumbPath"));
    document.setDuration((Integer) map.get("duration"));
    document.setSource((String) map.get("source"));

    // Parse date from Elasticsearch format with more flexible parsing
    String createdAtStr = (String) map.get("createdAt");
    if (createdAtStr != null) {
      try {
        // Try multiple date formats
        String[] formats = {
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
        };

        for (String format : formats) {
          try {
            SimpleDateFormat dateFormat = new SimpleDateFormat(format);
            dateFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            document.setCreatedAt(dateFormat.parse(createdAtStr));
            break;
          } catch (Exception e) {
            // Continue to next format if this one fails
            continue;
          }
        }

        // If all formats failed, log warning
        if (document.getCreatedAt() == null) {
          logger.warn("Failed to parse date with any format: {}", createdAtStr);
        }
      } catch (Exception e) {
        logger.warn("Failed to parse date: {}", createdAtStr);
        document.setCreatedAt(null);
      }
    } else {
      document.setCreatedAt(null);
    }

    document.setTrailer((String) map.get("trailer"));
    document.setSearchableText((String) map.get("searchableText"));
    document.setCategories((List<String>) map.get("categories"));
    document.setModels((List<String>) map.get("models"));
    return document;
  }

  public void saveCategoryDocument(CategoryDocument document) {
    if (!elasticsearchEnabled) {
      logger.debug("Elasticsearch disabled: skipping category document save for ID={}", 
          document != null ? document.getId() : "null");
      return;
    }
    
    LoggingUtil.logOperationTime(logger, "save category document to elasticsearch", () -> {
      try {
        if (document == null) {
          logger.error("Cannot index null document");
          return null;
        }

        if (document.getId() == null || document.getId().isEmpty()) {
          logger.error("Document ID is required for indexing");
          return null;
        }

        Map<String, Object> documentMap = convertToMap(document);
        logger.debug("Indexing category document: {}", documentMap);

        try {
          org.elasticsearch.client.Request lowLevelRequest = new org.elasticsearch.client.Request(
              "PUT",
              "/" + CATEGORIES_INDEX + "/_doc/" + document.getId()
          );

          String jsonBody = objectMapper.writeValueAsString(documentMap);
          lowLevelRequest.setJsonEntity(jsonBody);

          org.elasticsearch.client.Response lowLevelResponse =
              esClient.getLowLevelClient().performRequest(lowLevelRequest);

          int statusCode = lowLevelResponse.getStatusLine().getStatusCode();
          if (statusCode >= 200 && statusCode < 300) {
            logger.info("Category document indexed successfully: ID={}", document.getId());
          } else {
            logger.warn("Category indexing returned non-success status: {}", statusCode);
            throw new RuntimeException("Failed to index category document: HTTP " + statusCode);
          }
        } catch (Exception e) {
          logger.error("Error indexing category document: {}", e.getMessage(), e);
          throw new RuntimeException("Failed to index category document", e);
        }

        return null;
      } catch (Exception e) {
        logger.error("Error in category document indexing process: {}", e.getMessage(), e);
        throw new RuntimeException("Failed to complete category document indexing", e);
      }
    });
  }

  public void saveModelDocument(ModelDocument document) {
    if (!elasticsearchEnabled) {
      logger.debug("Elasticsearch disabled: skipping model document save for ID={}", 
          document != null ? document.getId() : "null");
      return;
    }
    
    LoggingUtil.logOperationTime(logger, "save model document to elasticsearch", () -> {
      try {
        if (document == null) {
          logger.error("Cannot index null document");
          return null;
        }

        if (document.getId() == null || document.getId().isEmpty()) {
          logger.error("Document ID is required for indexing");
          return null;
        }

        Map<String, Object> documentMap = convertToMap(document);
        logger.debug("Indexing model document: {}", documentMap);

        try {
          org.elasticsearch.client.Request lowLevelRequest = new org.elasticsearch.client.Request(
              "PUT",
              "/" + MODELS_INDEX + "/_doc/" + document.getId()
          );

          String jsonBody = objectMapper.writeValueAsString(documentMap);
          lowLevelRequest.setJsonEntity(jsonBody);

          org.elasticsearch.client.Response lowLevelResponse =
              esClient.getLowLevelClient().performRequest(lowLevelRequest);

          int statusCode = lowLevelResponse.getStatusLine().getStatusCode();
          if (statusCode >= 200 && statusCode < 300) {
            logger.info("Model document indexed successfully: ID={}", document.getId());
          } else {
            logger.warn("Model indexing returned non-success status: {}", statusCode);
            throw new RuntimeException("Failed to index model document: HTTP " + statusCode);
          }
        } catch (Exception e) {
          logger.error("Error indexing model document: {}", e.getMessage(), e);
          throw new RuntimeException("Failed to index model document", e);
        }

        return null;
      } catch (Exception e) {
        logger.error("Error in model document indexing process: {}", e.getMessage(), e);
        throw new RuntimeException("Failed to complete model document indexing", e);
      }
    });
  }

  // Helper methods for converting documents to maps
  private Map<String, Object> convertToMap(CategoryDocument document) {
    Map<String, Object> map = new HashMap<>();
    map.put("id", document.getId());
    map.put("tenantId", document.getTenantId());
    map.put("name", document.getName());
    map.put("description", document.getDescription());
    map.put("homeThumb", document.getHomeThumb());
    map.put("header", document.getHeader());
    map.put("homeSEO", document.getHomeSEO());
    map.put("homeCatOrder", document.getHomeCatOrder());
    map.put("home", document.getHome());
    map.put("createdViaLink", document.getCreatedViaLink());
    map.put("linkCount", document.getLinkCount());

    // Format date for Elasticsearch
    if (document.getCreatedAt() != null) {
      SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
      dateFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
      map.put("createdAt", dateFormat.format(document.getCreatedAt()));
    } else {
      map.put("createdAt", null);
    }

    return map;
  }

  private Map<String, Object> convertToMap(ModelDocument document) {
    Map<String, Object> map = new HashMap<>();
    map.put("id", document.getId());
    map.put("tenantId", document.getTenantId());
    map.put("name", document.getName());
    map.put("description", document.getDescription());
    map.put("country", document.getCountry());
    map.put("thumbnail", document.getThumbnail());
    map.put("thumbPath", document.getThumbPath());
    map.put("age", document.getAge());
    map.put("linkCount", document.getLinkCount());

    // Format date for Elasticsearch
    if (document.getCreatedAt() != null) {
      SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
      dateFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
      map.put("createdAt", dateFormat.format(document.getCreatedAt()));
    } else {
      map.put("createdAt", null);
    }

    return map;
  }

  public List<CategoryDocument> findCategories(String name) {
    if (!elasticsearchEnabled) {
      logger.debug("Elasticsearch disabled: skipping category search for name={}", name);
      return new ArrayList<>();
    }
    
    return LoggingUtil.logOperationTime(logger, "find categories by name", () -> {
      List<CategoryDocument> results = new ArrayList<>();
      try {
        SearchRequest searchRequest = new SearchRequest(CATEGORIES_INDEX);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        if (name != null && !name.isEmpty()) {
          searchSourceBuilder.query(QueryBuilders.matchQuery("name", name));
        } else {
          searchSourceBuilder.query(QueryBuilders.matchAllQuery());
        }

        searchRequest.source(searchSourceBuilder);
        SearchResponse response = esClient.search(searchRequest, RequestOptions.DEFAULT);

        for (SearchHit hit : response.getHits().getHits()) {
          Map<String, Object> sourceAsMap = hit.getSourceAsMap();
          CategoryDocument document = convertToCategoryDocument(sourceAsMap);
          document.setId(hit.getId());
          results.add(document);
        }

        logger.info("Found {} categories matching name '{}'", results.size(), name);
      } catch (Exception e) {
        logger.error("Error searching for categories with name {}: {}", name, e.getMessage(), e);
        throw new RuntimeException("Failed to search categories", e);
      }
      return results;
    });
  }

  public List<ModelDocument> findModels(String name) {
    if (!elasticsearchEnabled) {
      logger.debug("Elasticsearch disabled: skipping model search for name={}", name);
      return new ArrayList<>();
    }
    
    return LoggingUtil.logOperationTime(logger, "find models by name", () -> {
      List<ModelDocument> results = new ArrayList<>();
      try {
        SearchRequest searchRequest = new SearchRequest(MODELS_INDEX);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        if (name != null && !name.isEmpty()) {
          searchSourceBuilder.query(QueryBuilders.matchQuery("name", name));
        } else {
          searchSourceBuilder.query(QueryBuilders.matchAllQuery());
        }

        searchRequest.source(searchSourceBuilder);
        SearchResponse response = esClient.search(searchRequest, RequestOptions.DEFAULT);

        for (SearchHit hit : response.getHits().getHits()) {
          Map<String, Object> sourceAsMap = hit.getSourceAsMap();
          ModelDocument document = convertToModelDocument(sourceAsMap);
          document.setId(hit.getId());
          results.add(document);
        }

        logger.info("Found {} models matching name '{}'", results.size(), name);
      } catch (Exception e) {
        logger.error("Error searching for models with name {}: {}", name, e.getMessage(), e);
        throw new RuntimeException("Failed to search models", e);
      }
      return results;
    });
  }

  public Page<LinkDocument> searchLinks(String query, Pageable pageable) {
    if (!elasticsearchEnabled) {
      logger.debug("Elasticsearch disabled: skipping link search for query={}", query);
      return new PageImpl<>(new ArrayList<>(), pageable, 0);
    }
    
    return LoggingUtil.logOperationTime(logger, "search links with pagination", () -> {
      try {
        SearchRequest searchRequest = new SearchRequest(LINKS_INDEX);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        if (query != null && !query.isEmpty()) {
          searchSourceBuilder.query(QueryBuilders.multiMatchQuery(query)
              .field("title")
              .field("searchableText")
              .field("categories")
              .field("models")
              .field("source")
              .field("sheetName")
              .type(MultiMatchQueryBuilder.Type.BEST_FIELDS));
        } else {
          searchSourceBuilder.query(QueryBuilders.matchAllQuery());
        }

        searchSourceBuilder.from((int) pageable.getOffset());
        searchSourceBuilder.size(pageable.getPageSize());

        if (pageable.getSort().isSorted()) {
          pageable.getSort().forEach(order -> {
            searchSourceBuilder.sort(order.getProperty(),
                order.getDirection() == Direction.ASC ?
                    SortOrder.ASC : SortOrder.DESC);
          });
        }

        searchRequest.source(searchSourceBuilder);
        SearchResponse response = esClient.search(searchRequest, RequestOptions.DEFAULT);

        List<LinkDocument> results = new ArrayList<>();
        for (SearchHit hit : response.getHits().getHits()) {
          Map<String, Object> sourceAsMap = hit.getSourceAsMap();
          LinkDocument document = convertToLinkDocument(sourceAsMap);
          document.setId(hit.getId());
          results.add(document);
        }

        long totalHits = response.getHits().getTotalHits().value;
        logger.info("Found {} links matching query '{}'", totalHits, query);

        return new PageImpl<>(results, pageable, totalHits);
      } catch (Exception e) {
        logger.error("Error searching links with query {}: {}", query, e.getMessage(), e);
        throw new RuntimeException("Failed to search links", e);
      }
    });
  }

  public List<AutosuggestItem> autosuggest(String query) {
    if (!elasticsearchEnabled) {
      logger.debug("Elasticsearch disabled: skipping autosuggest for query={}", query);
      return new ArrayList<>();
    }
    
    return LoggingUtil.logOperationTime(logger, "autosuggest across model and category indices",
        () -> {
          List<AutosuggestItem> results = new ArrayList<>();

          try {
            if (query == null || query.isEmpty()) {
              return results;
            }

            // Search in categories index
            SearchRequest categoriesRequest = new SearchRequest(CATEGORIES_INDEX);
            SearchSourceBuilder categoriesSourceBuilder = new SearchSourceBuilder();
            categoriesSourceBuilder.query(QueryBuilders.matchPhrasePrefixQuery("name", query));
            categoriesSourceBuilder.size(10); // Limit to 10 results
            categoriesRequest.source(categoriesSourceBuilder);

            SearchResponse categoriesResponse = esClient.search(categoriesRequest,
                RequestOptions.DEFAULT);
            for (SearchHit hit : categoriesResponse.getHits().getHits()) {
              Map<String, Object> sourceAsMap = hit.getSourceAsMap();
              CategoryDocument document = convertToCategoryDocument(sourceAsMap);
              document.setId(hit.getId());
              results.add(new AutosuggestItem(
                  document.getId(),
                  document.getName(),
                  "category",
                  document
              ));
            }

            // Search in models index
            SearchRequest modelsRequest = new SearchRequest(MODELS_INDEX);
            SearchSourceBuilder modelsSourceBuilder = new SearchSourceBuilder();
            modelsSourceBuilder.query(QueryBuilders.matchPhrasePrefixQuery("name", query));
            modelsSourceBuilder.size(10); // Limit to 10 results
            modelsRequest.source(modelsSourceBuilder);

            SearchResponse modelsResponse = esClient.search(modelsRequest, RequestOptions.DEFAULT);
            for (SearchHit hit : modelsResponse.getHits().getHits()) {
              Map<String, Object> sourceAsMap = hit.getSourceAsMap();
              ModelDocument document = convertToModelDocument(sourceAsMap);
              document.setId(hit.getId());
              results.add(new AutosuggestItem(
                  document.getId(),
                  document.getName(),
                  "model",
                  document
              ));
            }

            logger.info("Found total of {} items for autosuggest query '{}'", results.size(),
                query);

            return results;
          } catch (Exception e) {
            logger.error("Error in autosuggest for query {}: {}", query, e.getMessage(), e);
            throw new RuntimeException("Failed to perform autosuggest search", e);
          }
        });
  }

  private CategoryDocument convertToCategoryDocument(Map<String, Object> map) {
    CategoryDocument document = new CategoryDocument();
    document.setId((String) map.get("id"));
    document.setTenantId((Integer) map.get("tenantId"));
    document.setName((String) map.get("name"));
    document.setDescription((String) map.get("description"));
    document.setHomeThumb((Boolean) map.get("homeThumb"));
    document.setHeader((Boolean) map.get("header"));
    document.setHomeSEO((Boolean) map.get("homeSEO"));
    document.setHomeCatOrder((Integer) map.get("homeCatOrder"));
    document.setHome((Integer) map.get("home"));
    document.setCreatedViaLink((Boolean) map.get("createdViaLink"));
    document.setLinkCount((Long) map.get("linkCount"));

    // Parse date
    String createdAtStr = (String) map.get("createdAt");
    if (createdAtStr != null) {
      try {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        dateFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        document.setCreatedAt(dateFormat.parse(createdAtStr));
      } catch (Exception e) {
        logger.warn("Failed to parse date: {}", createdAtStr);
        document.setCreatedAt(null);
      }
    }

    return document;
  }

  private ModelDocument convertToModelDocument(Map<String, Object> map) {
    ModelDocument document = new ModelDocument();
    document.setId((String) map.get("id"));
    document.setTenantId((Integer) map.get("tenantId"));
    document.setName((String) map.get("name"));
    document.setDescription((String) map.get("description"));
    document.setCountry((String) map.get("country"));
    document.setThumbnail((String) map.get("thumbnail"));
    document.setThumbPath((String) map.get("thumbPath"));
    document.setAge((Integer) map.get("age"));
    document.setLinkCount((Integer) map.get("linkCount"));

    // Parse date
    String createdAtStr = (String) map.get("createdAt");
    if (createdAtStr != null) {
      try {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        dateFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        document.setCreatedAt(dateFormat.parse(createdAtStr));
      } catch (Exception e) {
        logger.warn("Failed to parse date: {}", createdAtStr);
        document.setCreatedAt(null);
      }
    }

    return document;
  }
} 