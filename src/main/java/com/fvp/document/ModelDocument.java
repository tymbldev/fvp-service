package com.fvp.document;

import lombok.Data;
import java.util.Date;

@Data
public class ModelDocument {
    private String id;
    private Integer tenantId;
    private String name;
    private Integer linkCount;
    private Date createdAt;
} 