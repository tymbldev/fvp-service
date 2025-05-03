package com.fvp.dto;

import lombok.Data;

@Data
public class AutosuggestItem {

  private String id;
  private String name;
  private String type; // "link", "category", or "model"
  private Object data; // Original document

  public AutosuggestItem(String id, String name, String type, Object data) {
    this.id = id;
    this.name = name;
    this.type = type;
    this.data = data;
  }
} 