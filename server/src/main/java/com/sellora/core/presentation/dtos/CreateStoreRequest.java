package com.sellora.core.presentation.dtos;

import lombok.Data;

@Data
public class CreateStoreRequest {
  private String name;
  private String address;
  private String contactPhone;
  private String description;
  private String logoUrl;
}
