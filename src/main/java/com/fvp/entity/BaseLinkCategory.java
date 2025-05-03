package com.fvp.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@MappedSuperclass
@NoArgsConstructor
public abstract class BaseLinkCategory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "tenant_id")
    private Integer tenantId;
    
    @Column(name = "link_id")
    private Integer linkId;
    
    @Column(name = "category")
    private String category;
    
    @CreationTimestamp
    @Column(name = "created_on")
    private LocalDateTime createdOn;
    
    @Column(name = "random_order")
    private Integer randomOrder;
    
    @ToString.Exclude
    @ManyToOne
    @JoinColumn(name = "link_id", insertable = false, updatable = false)
    private Link link;
} 