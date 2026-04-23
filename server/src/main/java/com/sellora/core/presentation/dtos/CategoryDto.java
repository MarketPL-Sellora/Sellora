package com.sellora.core.presentation.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDto {
  private Long id;
  private String name;
  private Long parentId;

  @Builder.Default
  private List<CategoryDto> children = new ArrayList<>();
}
