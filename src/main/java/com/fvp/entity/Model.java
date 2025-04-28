package com.fvp.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "models")
@NoArgsConstructor
public class Model {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "tenant_id", nullable = false)
    private Integer tenantId;
    
    @Column(unique = true, nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String thumbnail;
    
    @Column(length = 100, nullable = false)
    private String thumbPath;
    
    @Column(nullable = false)
    private Integer age = 0;
    
    @Column(length = 100)
    private String country;
    
    @Column(length = 1000)
    private String description;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
} 