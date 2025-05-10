package com.fvp.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

public class PageDeserializer extends JsonDeserializer<Page<?>> {

    @Override
    public Page<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ObjectMapper mapper = (ObjectMapper) p.getCodec();
        JsonNode node = mapper.readTree(p);
        
        // Extract content
        JsonNode contentNode = node.get("content");
        List<Object> content = new ArrayList<>();
        if (contentNode.isArray()) {
            for (JsonNode item : contentNode) {
                content.add(mapper.treeToValue(item, Object.class));
            }
        }
        
        // Extract pageable information
        JsonNode pageableNode = node.get("pageable");
        int pageNumber = pageableNode.get("pageNumber").asInt();
        int pageSize = pageableNode.get("pageSize").asInt();
        
        // Extract sort information
        JsonNode sortNode = pageableNode.get("sort");
        Sort sort = Sort.unsorted();
        if (!sortNode.get("empty").asBoolean()) {
            List<Sort.Order> orders = new ArrayList<>();
            JsonNode ordersNode = sortNode.get("orders");
            if (ordersNode != null && ordersNode.isArray()) {
                for (JsonNode orderNode : ordersNode) {
                    String property = orderNode.get("property").asText();
                    String direction = orderNode.get("direction").asText();
                    orders.add(new Sort.Order(Sort.Direction.valueOf(direction), property));
                }
                sort = Sort.by(orders);
            }
        }
        
        // Create PageRequest
        PageRequest pageRequest = PageRequest.of(pageNumber, pageSize, sort);
        
        // Extract total elements
        long totalElements = node.get("totalElements").asLong();
        
        // Create and return PageImpl
        return new PageImpl<>(content, pageRequest, totalElements);
    }
} 