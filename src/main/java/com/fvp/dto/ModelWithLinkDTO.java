package com.fvp.dto;

import lombok.Data;

@Data
public class ModelWithLinkDTO {
    private Integer id;
    private String name;
    private String description;
    private String link;
    private String linkTitle;
    private String linkThumbnail;
    private String linkThumbPath;
    private Integer linkDuration;
    private Integer linkCount;
} 