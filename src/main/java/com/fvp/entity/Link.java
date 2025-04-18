package com.fvp.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Entity
@Table(name = "links", schema = "fvp_test")
public class Link {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "tenant_id", nullable = false)
    private Integer tenantId;
    
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "json")
    private String category;
    
    @Column(nullable = false)
    private Integer duration;
    
    @Column(length = 500, nullable = false)
    private String thumbnail;
    
    @Column(length = 100, nullable = false)
    private String thumbPath;
    
    @Column(length = 100, nullable = false)
    private String sheetName;
    
    @Column(length = 500, nullable = false, unique = true)
    private String link;
    
    @Column(length = 100, nullable = false)
    private String source;
    
    @Column(columnDefinition = "json")
    private String star;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(length = 500)
    private String trailer;
    
    @OneToMany(mappedBy = "link", cascade = CascadeType.ALL)
    private Set<LinkCategory> linkCategories;
} 