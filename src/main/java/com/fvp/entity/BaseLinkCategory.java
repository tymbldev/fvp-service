package com.fvp.entity;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

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

  @Column(name = "hd")
  private Integer hd = 0;

  @ToString.Exclude
  @ManyToOne
  @JoinColumn(name = "link_id", insertable = false, updatable = false)
  private Link link;
} 