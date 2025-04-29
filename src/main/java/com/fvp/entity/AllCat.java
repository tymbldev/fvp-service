package com.fvp.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.io.Serializable;

@Data
@Entity
@Table(name = "all_cat")
@NoArgsConstructor
public class AllCat implements Serializable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "tenant_id", nullable = false)
    private Integer tenantId;
    
    @Column(unique = true, nullable = false)
    private String name;
    
    @Column(name = "home_thumb", nullable = false)
    private Boolean homeThumb = false;
    
    @Column(nullable = false)
    private Boolean header = false;
    
    @Column(nullable = false)
    private Boolean homeSEO = false;
    
    @Column(name = "home_cat_order", nullable = false)
    private Integer homeCatOrder = 0;
    
    @Column(name = "home", nullable = false)
    private Integer home = 0;
    
    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Boolean createdViaLink = false;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
} 