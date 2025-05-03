package com.fvp.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ModelWithLinkDTO {

  private Integer id;
  private Integer tenantId;
  private String name;
  private String description;
  private String country;
  private String thumbnail;
  private String thumbPath;
  private Integer age;
  private LocalDateTime createdAt;

  // Link related fields
  private String link;
  private String linkTitle;
  private String linkThumbnail;
  private String linkThumbPath;
  private Integer linkDuration;
  private Integer linkCount;
} 