package com.fvp.dto;

import lombok.Data;

@Data
public class CategoryWithLinkDTO {
    private Integer id;
    private String name;
    private Boolean homeThumb;
    private Boolean header;
    private Boolean homeSEO;
    private Integer homeCatOrder;
    private String description;
    private String link;
    private String linkTitle;
    private String linkThumbnail;
    private Integer linkDuration;
    private Long linkCount;  // Number of links in this category
} 