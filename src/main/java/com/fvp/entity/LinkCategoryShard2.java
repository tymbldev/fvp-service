package com.fvp.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Table;

@Data
@Entity
@Table(name = "link_category_shard_2")
@NoArgsConstructor
public class LinkCategoryShard2 extends BaseLinkCategory {
} 