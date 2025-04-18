package com.fvp.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "link_categories", schema = "fvp_test")
public class LinkCategory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "tenant_id", nullable = false)
    private Integer tenantId;
    
    @ManyToOne
    @JoinColumn(name = "link_id", nullable = false)
    private Link link;
    
    @Column(nullable = false)
    private String category;
    
    @CreationTimestamp
    @Column(name = "created_on")
    private LocalDateTime createdOn;
} 