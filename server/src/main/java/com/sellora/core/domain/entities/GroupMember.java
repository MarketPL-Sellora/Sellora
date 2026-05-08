package com.sellora.core.domain.entities;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "group_members")
@Data
public class GroupMember {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "session_id")
  private Long sessionId;

  @Column(name = "user_id")
  private Long userId;

  @Column(name = "joined_at", insertable = false, updatable = false)
  private LocalDateTime joinedAt;
}
