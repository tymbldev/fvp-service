package com.fvp.document;

import java.util.Date;
import java.util.List;
import lombok.Data;

@Data
public class LinkDocument {

  private Integer tenantId;
  private List<String> categories;
  private String link;
  private List<String> models;
  private Date createdAt;
  private String searchableText;

  private String linkTitle;
  private String linkThumbnail;
  private String linkThumbPath;
  private Integer linkDuration;
  private String linkId;
  private String linkSource;
  private String linkTrailer;
} 