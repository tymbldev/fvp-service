package com.fvp.document;

import java.util.Date;
import java.util.List;
import lombok.Data;

@Data
public class LinkDocument {

  private String id;
  private Integer tenantId;
  private String title;
  private List<String> categories;
  private Integer duration;
  private String thumbnail;
  private String thumbPath;
  private String sheetName;
  private String link;
  private String source;
  private Integer stars;
  private Date createdAt;
  private String trailer;
  private String searchableText;
} 