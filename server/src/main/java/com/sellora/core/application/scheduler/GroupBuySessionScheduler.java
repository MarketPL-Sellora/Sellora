package com.sellora.core.application.scheduler;

import com.sellora.core.application.usecases.OrderService;
import com.sellora.core.application.usecases.RefundService;
import com.sellora.core.domain.entities.GroupBuySession;
import com.sellora.core.domain.entities.Order;
import com.sellora.core.infrastructure.persistence.GroupBuySessionRepository;
import com.sellora.core.infrastructure.persistence.GroupMemberRepository;
import com.sellora.core.infrastructure.persistence.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class GroupBuySessionScheduler {

  private final GroupBuySessionRepository sessionRepository;
  private final GroupMemberRepository groupMemberRepository;
  private final OrderRepository orderRepository;
  private final OrderService orderService;
  private final RefundService refundService;

  // cron = "0 */15 * * * *" запускає задачу кожні 15 хвилин
  @Scheduled(cron = "0 */15 * * * *")
  @Transactional
  public void checkExpiredSessions() {
    log.info("Запуск планувальника: перевірка протермінованих сесій...");

    List<GroupBuySession> expiredSessions = sessionRepository.findByStatusAndExpiresAtBefore(
      "ACTIVE", LocalDateTime.now()
    );

    if (expiredSessions.isEmpty()) {
      return;
    }

    log.info("Знайдено протермінованих сесій: {}. Починаємо обробку.", expiredSessions.size());

    int completedCount = 0;
    int cancelledCount = 0;

    for (GroupBuySession session : expiredSessions) {
      try {
        int membersCount = groupMemberRepository.countBySessionId(session.getId());

        if (membersCount >= session.getLockedTargetSize()) {
          // Група зібралась в останній момент
          session.setStatus("COMPLETED");
          sessionRepository.save(session);
          completedCount++;
          log.info("Сесію ID {} успішно переведено в COMPLETED (учасників: {}).", session.getId(), membersCount);
        } else {
          // Група НЕ зібралась
          session.setStatus("CANCELLED");
          sessionRepository.save(session);

          // Скасовуємо всі пов'язані замовлення та повертаємо товари на склад
          List<Order> sessionOrders = orderRepository.findBySessionId(session.getId());
          for (Order order : sessionOrders) {
            if (!"CANCELLED".equalsIgnoreCase(order.getPaymentStatus())) {
              orderService.processOrderCancellationLogic(order);
            }
          }

          // Викликаємо сервіс повернення коштів (залишив твій існуючий виклик)
          refundService.processRefundForSession(session.getId());

          cancelledCount++;
          log.info("Сесію ID {} скасовано (недобір: {}/{}), замовлення анульовано.",
            session.getId(), membersCount, session.getLockedTargetSize());
        }
      } catch (Exception e) {
        log.error("Помилка при обробці сесії ID {}: {}", session.getId(), e.getMessage());
      }
    }

    log.info("Обробку завершено. Успішно зібрано: {}, Скасовано: {}", completedCount, cancelledCount);
  }
}
