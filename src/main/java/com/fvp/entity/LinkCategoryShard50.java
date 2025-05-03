package com.fvp.entity;

import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "link_category_shard_50")
@NoArgsConstructor
public class LinkCategoryShard50 extends BaseLinkCategory {

}