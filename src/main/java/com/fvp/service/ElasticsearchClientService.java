package com.fvp.service;

import com.fvp.document.CategoryDocument;
import com.fvp.document.LinkDocument;
import com.fvp.document.ModelDocument;
import com.fvp.util.LoggingUtil;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.text.SimpleDateFormat;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.xcontent.XContentType;
import java.io.IOException;
import com.fvp.dto.AutosuggestItem;

@Service
public class ElasticsearchClientService {

    private static final Logger logger = LoggingUtil.getLogger(ElasticsearchClientService.class);
    private static final String LINKS_INDEX = "links";
    private static final String CATEGORIES_INDEX = "categories";
    private static final String MODELS_INDEX = "models";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestHighLevelClient esClient;

    @Autowired
    public ElasticsearchClientService(RestHighLevelClient esClient) {
        this.esClient = esClient;
        try{
            ensureIndexExists(LINKS_INDEX);
            ensureIndexExists(CATEGORIES_INDEX);
            ensureIndexExists(MODELS_INDEX);
        }catch (Exception e){
            logger.error("Error ensuring indexes exist: {}", e.getMessage(), e);
        }
    }

    private void ensureIndexExists(String indexName) {
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
                logger.debug("Indexing document: {}", documentMap);
                
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

    public List<LinkDocument> findByCategoriesContaining(String category) {
        return LoggingUtil.logOperationTime(logger, "find documents by category", () -> {
            List<LinkDocument> results = new ArrayList<>();
            try {
                if (category == null || category.isEmpty()) {
                    logger.warn("Category is null or empty");
                    return results;
                }

                SearchRequest searchRequest = new SearchRequest(LINKS_INDEX);
                SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
                searchSourceBuilder.query(QueryBuilders.matchQuery("categories", category));
                searchRequest.source(searchSourceBuilder);

                SearchResponse response = esClient.search(searchRequest, RequestOptions.DEFAULT);
                
                for (SearchHit hit : response.getHits().getHits()) {
                    Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                    LinkDocument document = convertToLinkDocument(sourceAsMap);
                    document.setId(hit.getId());
                    results.add(document);
                }
                
                logger.info("Found {} documents with category {}", results.size(), category);
            } catch (Exception e) {
                logger.error("Error searching for documents with category {}: {}", category, e.getMessage(), e);
                throw new RuntimeException("Failed to search documents by category", e);
            }
            return results;
        });
    }

    public List<LinkDocument> searchByTitleOrText(String title, String searchableText) {
        return LoggingUtil.logOperationTime(logger, "search documents by title or text", () -> {
            List<LinkDocument> results = new ArrayList<>();
            try {
                SearchRequest searchRequest = new SearchRequest(LINKS_INDEX);
                SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
                
                if (title != null && !title.isEmpty()) {
                    searchSourceBuilder.query(QueryBuilders.matchQuery("title", title));
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

    public void deleteById(String id) {
        LoggingUtil.logOperationTime(logger, "delete document by id", () -> {
            try {
                if (id == null || id.isEmpty()) {
                    logger.error("Document ID is required for deletion");
                    return null;
                }

                DeleteRequest request = new DeleteRequest(LINKS_INDEX, id);
                DeleteResponse response = esClient.delete(request, RequestOptions.DEFAULT);
                logger.info("Deleted document with ID: {}", response.getId());
                return null;
            } catch (Exception e) {
                logger.error("Error deleting document with ID {}: {}", id, e.getMessage(), e);
                throw new RuntimeException("Failed to delete document", e);
            }
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
        map.put("sheetName", document.getSheetName());
        map.put("source", document.getSource());
        map.put("stars", document.getStars());
        
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
        document.setSheetName((String) map.get("sheetName"));
        document.setSource((String) map.get("source"));
        document.setStars((Integer) map.get("stars"));
        
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
        return document;
    }

    public void saveCategoryDocument(CategoryDocument document) {
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
        return map;
    }
    
    private Map<String, Object> convertToMap(ModelDocument document) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", document.getId());
        map.put("tenantId", document.getTenantId());
        map.put("name", document.getName());
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
    
    public List<AutosuggestItem> autosuggest(String query) {
        return LoggingUtil.logOperationTime(logger, "autosuggest across all indices", () -> {
            List<AutosuggestItem> results = new ArrayList<>();
            
            try {
                if (query == null || query.isEmpty()) {
                    return results;
                }
                
                // Search in links index
                SearchRequest linksRequest = new SearchRequest(LINKS_INDEX);
                SearchSourceBuilder linksSourceBuilder = new SearchSourceBuilder();
                linksSourceBuilder.query(
                    QueryBuilders.boolQuery()
                        .should(QueryBuilders.matchPhrasePrefixQuery("title", query))
                        .should(QueryBuilders.matchPhrasePrefixQuery("searchableText", query))
                );
                linksSourceBuilder.size(5); // Limit results
                linksRequest.source(linksSourceBuilder);
                
                SearchResponse linksResponse = esClient.search(linksRequest, RequestOptions.DEFAULT);
                for (SearchHit hit : linksResponse.getHits().getHits()) {
                    Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                    LinkDocument document = convertToLinkDocument(sourceAsMap);
                    document.setId(hit.getId());
                    results.add(new AutosuggestItem(
                        document.getId(),
                        document.getTitle(),
                        "link",
                        document
                    ));
                }
                
                // Search in categories index
                SearchRequest categoriesRequest = new SearchRequest(CATEGORIES_INDEX);
                SearchSourceBuilder categoriesSourceBuilder = new SearchSourceBuilder();
                categoriesSourceBuilder.query(QueryBuilders.matchPhrasePrefixQuery("name", query));
                categoriesSourceBuilder.size(5); // Limit results
                categoriesRequest.source(categoriesSourceBuilder);
                
                SearchResponse categoriesResponse = esClient.search(categoriesRequest, RequestOptions.DEFAULT);
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
                modelsSourceBuilder.size(5); // Limit results
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
                
                logger.info("Found total of {} items for autosuggest query '{}'", results.size(), query);
                
                // Sort results by relevance (for now, just mix them together)
                // In a real implementation, you might want to sort based on hit score
                
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
        return document;
    }
    
    private ModelDocument convertToModelDocument(Map<String, Object> map) {
        ModelDocument document = new ModelDocument();
        document.setId((String) map.get("id"));
        document.setTenantId((Integer) map.get("tenantId"));
        document.setName((String) map.get("name"));
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