package com.fvp.dto;

import java.io.Serializable;
import lombok.Data;

@Data
public class ModelWithoutLinkDTO implements Serializable {

  private Integer id;
  private Integer tenantId;
  private String name;
  private String description;
  private String country;
  private String thumbnail;
  private String thumbPath;
  private Integer age;

} 