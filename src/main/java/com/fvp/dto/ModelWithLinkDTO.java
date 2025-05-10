package com.fvp.dto;

import java.io.Serializable;
import lombok.Data;

@Data
public class ModelWithLinkDTO implements Serializable {

  private Integer id;
  private Integer tenantId;
  private String name;
  private String description;
  private String country;
  private String thumbnail;
  private String thumbPath;
  private Integer age;
  
  // Link related fields
  private String link;
  private String linkTitle;
  private String linkThumbnail;
  private String linkThumbPath;
  private Integer linkDuration;
  private Long linkId;
  private String linkSource;
  private String linkTrailer;
} 