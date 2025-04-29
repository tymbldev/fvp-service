package com.fvp.entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

@Data
@Entity
@Table(name = "link")
@NoArgsConstructor
public class Link implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
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
    
    @ToString.Exclude
    @OneToMany(mappedBy = "link", cascade = CascadeType.ALL)
    private Set<LinkCategory> linkCategories = new HashSet<>();

    @Column(name = "quality")
    private String quality;

    @Column(name = "created_on")
    private LocalDateTime createdOn;

    @Column(name = "random_order")
    private Integer randomOrder;
    
    @Column(name = "thumb_path_processed")
    private Integer thumbPathProcessed = 0;
} 