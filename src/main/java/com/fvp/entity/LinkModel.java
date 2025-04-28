package com.fvp.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "link_model")
@Data
@NoArgsConstructor
public class LinkModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "tenant_id")
    private Integer tenantId;

    @Column(name = "link_id")
    private Integer linkId;

    @Column(name = "model")
    private String model;

    @Column(name = "created_on")
    private LocalDateTime createdOn;

    @Column(name = "random_order")
    private Integer randomOrder;

    @ManyToOne
    @JoinColumn(name = "link_id", insertable = false, updatable = false)
    private Link link;
} 