package com.fvp.document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.Data;

@Data
public class LinkDocument {

  private Integer tenantId;
  private List<String> categories = new ArrayList<>();
  private String link;
  private List<String> models = new ArrayList<>();
  private Date createdAt;
  private String searchableText;

  private String linkTitle;
  private String linkThumbnail;
  private String linkThumbPath;
  private Integer linkDuration;
  private String linkId;
  private String linkSource;
  private String linkTrailer;

  private String quality;
  private String sheetName;
  private Integer randomOrder;
  private Integer thumbPathProcessed;
  private Integer trailerPresent;
  private Integer hd;
  private Date createdOn;
} 