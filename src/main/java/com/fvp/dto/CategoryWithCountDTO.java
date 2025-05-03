package com.fvp.dto;

import lombok.Data;

@Data
public class CategoryWithCountDTO {

  private String name;
  private String description;
  private Long linkCount;
} 