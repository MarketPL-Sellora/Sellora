package com.sellora.core.domain.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSettings {

  @Id
  @Column(name = "user_id")
  private Long userId;

  @Column(name = "notify_email_on_order_status")
  private Boolean notifyEmailOnOrderStatus = true; // Дефолтне значення
}
