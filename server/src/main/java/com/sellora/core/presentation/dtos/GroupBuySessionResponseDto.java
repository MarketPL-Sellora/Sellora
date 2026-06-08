package com.sellora.core.presentation.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record GroupBuySessionResponseDto(
  String uuid,
  Long productId,
  String productTitle,
  String productImage,
  BigDecimal price,
  Integer targetSize,
  Integer currentMembersCount,
  String status,
  LocalDateTime expiresAt,
  boolean isAvailable,
  LocalDateTime serverTime,
  List<GroupMemberDto> members,

  @JsonProperty("payment_url")
  String paymentUrl
) {}
