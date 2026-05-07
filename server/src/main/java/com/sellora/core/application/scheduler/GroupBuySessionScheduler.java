package com.sellora.core.application.scheduler;

import com.sellora.core.application.usecases.RefundService;
import com.sellora.core.domain.entities.GroupBuySession;
import com.sellora.core.infrastructure.persistence.GroupBuySessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class GroupBuySessionScheduler {

  private final GroupBuySessionRepository sessionRepository;
  private final RefundService refundService;

  @Scheduled(fixedRate = 60000)
  public void checkExpiredSessions() {
    log.info("Запуск планувальника: перевірка протермінованих сесій...");

    // Шукаємо сесії, які ACTIVE, але час їх життя вже минув
    List<GroupBuySession> expiredSessions = sessionRepository.findByStatusAndExpiresAtBefore(
      "ACTIVE", LocalDateTime.now()
    );

    if (expiredSessions.isEmpty()) {
      return; // Немає протермінованих, просто виходимо
    }

    log.info("Знайдено протермінованих сесій: {}. Починаємо обробку.", expiredSessions.size());

    for (GroupBuySession session : expiredSessions) {
      try {
        // 1. Змінюємо статус на CANCELLED (або EXPIRED)
        session.setStatus("CANCELLED");
        sessionRepository.save(session);

        // 2. Викликаємо сервіс повернення коштів
        refundService.processRefundForSession(session.getId());

        log.info("Сесію ID {} успішно скасовано.", session.getId());
      } catch (Exception e) {
        log.error("Помилка при обробці сесії ID {}: {}", session.getId(), e.getMessage());
      }
    }
  }
}
