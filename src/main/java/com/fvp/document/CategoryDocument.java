package com.fvp.document;

import java.util.Date;
import lombok.Data;

@Data
public class CategoryDocument {

  private String id;
  private Integer tenantId;
  private String name;
  private String description;
  private Boolean homeThumb;
  private Boolean header;
  private Boolean homeSEO;
  private Integer homeCatOrder;
  private Integer home;
  private Boolean createdViaLink;
  private Date createdAt;
  private Long linkCount;
} 