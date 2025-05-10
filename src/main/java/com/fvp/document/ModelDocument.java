package com.fvp.document;

import java.util.Date;
import lombok.Data;

@Data
public class ModelDocument {

  private String id;
  private Integer tenantId;
  private String name;
  private String description;
  private String country;
  private String thumbnail;
  private String thumbPath;
  private Integer age;
  private Date createdAt;
  private Integer linkCount;


} 