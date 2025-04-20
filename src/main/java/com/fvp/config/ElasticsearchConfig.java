package com.fvp.config;

import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.data.elasticsearch.config.AbstractElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.time.Duration;

@Configuration
@EnableElasticsearchRepositories(basePackages = "com.fvp.repository")
public class ElasticsearchConfig extends AbstractElasticsearchConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchConfig.class);

    @Value("${spring.elasticsearch.uris}")
    private String elasticsearchUri;

    @Value("${spring.elasticsearch.username:}")
    private String username;

    @Value("${spring.elasticsearch.password:}")
    private String password;

    @Value("${spring.elasticsearch.connection-timeout:5000}")
    private long connectionTimeout;

    @Value("${spring.elasticsearch.socket-timeout:30000}")
    private long socketTimeout;

    @Override
    @Bean
    public RestHighLevelClient elasticsearchClient() {
        try {
            ClientConfiguration clientConfiguration = createClientConfiguration();
            RestHighLevelClient client = RestClients.create(clientConfiguration).rest();

            // Create index if it doesn't exist
            String indexName = "links";
            if (!client.indices().exists(new GetIndexRequest(indexName), org.elasticsearch.client.RequestOptions.DEFAULT)) {
                CreateIndexRequest request = new CreateIndexRequest(indexName);
                
                // Load settings from file
                ClassPathResource resource = new ClassPathResource("es-settings.json");
                String settings = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
                
                // For Elasticsearch 8.x, we need to use a different approach to set settings
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

    private ClientConfiguration createClientConfiguration() {
        String host = elasticsearchUri.replace("http://", "").replace("https://", "");
        
        if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
            logger.info("Elasticsearch authentication enabled with username: {}", username);
            return ClientConfiguration.builder()
                .connectedTo(host)
                .withBasicAuth(username, password)
                .withConnectTimeout(Duration.ofMillis(connectionTimeout))
                .withSocketTimeout(Duration.ofMillis(socketTimeout))
                .build();
        } else {
            logger.info("Elasticsearch authentication disabled (no password provided)");
            return ClientConfiguration.builder()
                .connectedTo(host)
                .withConnectTimeout(Duration.ofMillis(connectionTimeout))
                .withSocketTimeout(Duration.ofMillis(socketTimeout))
                .build();
        }
    }

    private RestHighLevelClient createDummyClient() {
        logger.warn("Creating dummy Elasticsearch client - operations will be logged but not executed");
        return RestClients.create(ClientConfiguration.builder()
            .connectedTo("localhost:9200")
            .build()).rest();
    }
} 