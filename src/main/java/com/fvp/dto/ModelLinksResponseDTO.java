package com.fvp.dto;

import java.io.Serializable;
import java.util.List;
import lombok.Data;

@Data
public class ModelLinksResponseDTO implements Serializable {
    private Integer id;
    private Integer tenantId;
    private String name;
    private String description;
    private String country;
    private String thumbnail;
    private String thumbPath;
    private Integer age;
    private List<LinkDTO> links;
    private long totalElements;
    private int totalPages;
    private int number;
    private int size;
    private boolean first;
    private boolean last;
    private boolean empty;

    @Data
    public static class LinkDTO implements Serializable {
        private String link;
        private String linkTitle;
        private String linkThumbnail;
        private String linkThumbPath;
        private String linkSource;
        private String linkTrailer;
        private Integer linkDuration;
        private java.time.LocalDateTime createdOn;
    }
} 