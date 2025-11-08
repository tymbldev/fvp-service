package com.fvp.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class CategoryWithLinkDTO {

  private Integer id;
  private String name;
  private String description;
  private String link;
  private String linkTitle;
  private String linkThumbnail;
  private String linkThumbPath;
  private String linkSource;
  private String linkTrailer;
  private Integer linkDuration;
  private Long linkCount;  // Number of links in this category
  private LocalDateTime createdOn;
} 