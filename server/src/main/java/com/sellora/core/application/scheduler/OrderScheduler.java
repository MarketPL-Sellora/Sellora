package com.sellora.core.application.scheduler;

import com.sellora.core.application.usecases.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderScheduler {

  private final OrderService orderService;

  // cron = "0 */30 * * * *" означає запуск кожні 30 хвилин (на 0-вій та 30-тій хвилині години)
  @Scheduled(cron = "0 */30 * * * *")
  public void cancelUnpaidOrdersJob() {
    log.info("Запуск Job: Автоматичне скасування прострочених замовлень (старіше 24 год)...");
    try {
      int cancelledCount = orderService.cancelExpiredPendingOrders();
      if (cancelledCount > 0) {
        log.info("Job завершено успішно. Автоматично скасовано замовлень: {}", cancelledCount);
      } else {
        log.info("Job завершено. Прострочених замовлень не знайдено.");
      }
    } catch (Exception e) {
      log.error("Помилка під час виконання Job автоматичного скасування: ", e);
    }
  }
}
