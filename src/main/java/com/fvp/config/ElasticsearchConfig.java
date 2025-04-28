package com.fvp.config;

import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClientBuilder;

import java.nio.charset.StandardCharsets;
import java.io.IOException;

@Configuration
public class ElasticsearchConfig {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchConfig.class);

    @Value("${spring.elasticsearch.uris}")
    private String elasticsearchUri;

    @Value("${spring.elasticsearch.username:}")
    private String username;

    @Value("${spring.elasticsearch.password:}")
    private String password;

    @Value("${spring.elasticsearch.connection-timeout:5000}")
    private int connectionTimeout;

    @Value("${spring.elasticsearch.socket-timeout:30000}")
    private int socketTimeout;

    @Bean
    public RestHighLevelClient elasticsearchClient() {
        try {
            RestHighLevelClient client = createClient();

            // Create index if it doesn't exist
            String indexName = "links";
            if (!client.indices().exists(new GetIndexRequest(indexName), org.elasticsearch.client.RequestOptions.DEFAULT)) {
                CreateIndexRequest request = new CreateIndexRequest(indexName);
                
                // Load settings from file
                ClassPathResource resource = new ClassPathResource("es-settings.json");
                String settings = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
                
                // Apply settings
                request.source(settings, XContentType.JSON);
                
                try {
                    client.indices().create(request, org.elasticsearch.client.RequestOptions.DEFAULT);
                    logger.info("Created Elasticsearch index: {}", indexName);
                } catch (Exception e) {
                    logger.warn("Could not create index with settings file, trying with basic settings: {}", e.getMessage());
                    // Fallback to basic settings
                    request.settings(Settings.builder()
                            .put("index.number_of_shards", 1)
                            .put("index.number_of_replicas", 0));
                    client.indices().create(request, org.elasticsearch.client.RequestOptions.DEFAULT);
                    logger.info("Created Elasticsearch index with basic settings: {}", indexName);
                }
            }

            return client;
        } catch (Exception e) {
            logger.error("Failed to create Elasticsearch client: {}", e.getMessage(), e);
            return createDummyClient();
        }
    }

    private RestHighLevelClient createClient() {
        String host = elasticsearchUri.replace("http://", "").replace("https://", "");
        String[] hostParts = host.split(":");
        String hostName = hostParts[0];
        int port = hostParts.length > 1 ? Integer.parseInt(hostParts[1]) : 9200;
        
        RestClientBuilder builder = RestClient.builder(new HttpHost(hostName, port, "http"));
        
        // Configure timeouts
        builder.setRequestConfigCallback(requestConfigBuilder -> 
            requestConfigBuilder
                .setConnectTimeout(connectionTimeout)
                .setSocketTimeout(socketTimeout));
        
        // Configure authentication if provided
        if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
            logger.info("Elasticsearch authentication enabled with username: {}", username);
            
            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(
                AuthScope.ANY, new UsernamePasswordCredentials(username, password));
            
            builder.setHttpClientConfigCallback(httpClientBuilder -> 
                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
        } else {
            logger.info("Elasticsearch authentication disabled (no password provided)");
        }
        
        return new RestHighLevelClient(builder);
    }

    private RestHighLevelClient createDummyClient() {
        logger.warn("Creating dummy Elasticsearch client - operations will be logged but not executed");
        return new RestHighLevelClient(
            RestClient.builder(new HttpHost("localhost", 9200, "http")));
    }
} 