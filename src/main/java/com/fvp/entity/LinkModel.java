package com.fvp.entity;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "link_model")
public class LinkModel {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "link_id")
  private Integer linkId;

  @Column(name = "tenant_id")
  private Integer tenantId;

  @Column
  private String model;

  @Column(name = "created_on")
  private LocalDateTime createdOn;

  @Column(name = "random_order")
  private Double randomOrder;


  @Column(name = "hd")
  private Integer hd = 0;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Integer getLinkId() {
    return linkId;
  }

  public void setLinkId(Integer linkId) {
    this.linkId = linkId;
  }

  public Integer getTenantId() {
    return tenantId;
  }

  public void setTenantId(Integer tenantId) {
    this.tenantId = tenantId;
  }

  public String getModel() {
    return model;
  }

  public void setModel(String model) {
    this.model = model;
  }

  public LocalDateTime getCreatedOn() {
    return createdOn;
  }

  public void setCreatedOn(LocalDateTime createdOn) {
    this.createdOn = createdOn;
  }

  public Double getRandomOrder() {
    return randomOrder;
  }

  public void setRandomOrder(Double randomOrder) {
    this.randomOrder = randomOrder;
  }

  public Integer getHd() {
    return hd;
  }

  public void setHd(Integer hd) {
    this.hd = hd;
  }
}