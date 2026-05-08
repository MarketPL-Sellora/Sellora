package com.sellora.core.application.usecases;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RefundService {

  public void processRefundForSession(Long sessionId) {
    // У майбутньому тут буде API-запит до банку на скасування холда коштів
    log.info("[RefundService] Виконано повернення коштів для скасованої сесії з ID: {}", sessionId);
  }
}
