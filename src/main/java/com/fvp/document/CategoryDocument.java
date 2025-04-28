package com.fvp.document;

import lombok.Data;

@Data
public class CategoryDocument {
    private String id;
    private Integer tenantId;
    private String name;
    private String description;
} 