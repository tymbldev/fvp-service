package com.fvp.entity;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

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