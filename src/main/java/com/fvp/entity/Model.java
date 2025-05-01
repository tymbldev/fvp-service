package com.fvp.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "model")
@NoArgsConstructor
public class Model {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "tenant_id")
    private Integer tenantId;
    
    @Column(name = "name")
    private String name;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "country")
    private String country;
    
    @Column(nullable = false)
    private String thumbnail;
    
    @Column(length = 100, nullable = false)
    private String thumbPath;
    
    @Column(nullable = false)
    private Integer age = 0;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
} 