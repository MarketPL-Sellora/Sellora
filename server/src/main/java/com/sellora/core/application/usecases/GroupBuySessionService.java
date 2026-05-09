package com.sellora.core.application.usecases;

import com.sellora.core.domain.entities.GroupBuySession;
import com.sellora.core.domain.entities.GroupMember;
import com.sellora.core.domain.entities.Product;
import com.sellora.core.infrastructure.persistence.GroupBuySessionRepository;
import com.sellora.core.infrastructure.persistence.GroupMemberRepository;
import com.sellora.core.infrastructure.persistence.ProductRepository;
import com.sellora.core.presentation.dtos.CreateGroupBuySessionDto;
import com.sellora.core.presentation.dtos.GroupBuySessionResponseDto;
import com.sellora.core.presentation.dtos.GroupMemberDto;
import com.sellora.core.presentation.exceptions.BadRequestException;
import com.sellora.core.presentation.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupBuySessionService {

  private final GroupBuySessionRepository sessionRepository;
  private final GroupMemberRepository memberRepository;
  private final ProductRepository productRepository;

  public GroupBuySessionResponseDto getSessionDetails(String uuid) {
    GroupBuySession session = sessionRepository.findByUuid(uuid)
      .orElseThrow(() -> new ResourceNotFoundException("Сесію за таким посиланням не знайдено"));

    Product product = productRepository.findById(session.getProductId())
      .orElseThrow(() -> new ResourceNotFoundException("Товар не знайдено"));

    int currentMembers = memberRepository.countBySessionId(session.getId());
    boolean isAvailable = "ACTIVE".equals(session.getStatus()) && currentMembers < session.getLockedTargetSize();

    String mainImage = (product.getImages() != null && !product.getImages().isEmpty())
      ? product.getImages().get(0)
      : null;

    // НОВЕ: Отримуємо масив учасників і мапимо його в DTO
    List<GroupMemberDto> memberDtos = memberRepository.findBySessionId(session.getId())
      .stream()
      .map(m -> new GroupMemberDto(m.getId(), m.getSessionId(), m.getUserId(), m.getJoinedAt()))
      .collect(Collectors.toList());

    return new GroupBuySessionResponseDto(
      session.getUuid(),
      product.getId(),
      product.getTitle(),
      mainImage,
      session.getLockedPrice(),
      session.getLockedTargetSize(),
      currentMembers,
      session.getStatus(),
      session.getExpiresAt(),
      isAvailable,
      LocalDateTime.now(),
      memberDtos // Передаємо масив у DTO
    );
  }

  @Transactional
  public void joinSession(String uuid, Long userId) {
    GroupBuySession session = sessionRepository.findByUuid(uuid)
      .orElseThrow(() -> new ResourceNotFoundException("Сесію не знайдено"));

    if (!"ACTIVE".equals(session.getStatus())) {
      throw new BadRequestException("Ця сесія вже неактивна");
    }

    if (memberRepository.existsBySessionIdAndUserId(session.getId(), userId)) {
      throw new BadRequestException("Ви вже є учасником цієї групової покупки");
    }

    int currentMembers = memberRepository.countBySessionId(session.getId());
    if (currentMembers >= session.getLockedTargetSize()) {
      throw new BadRequestException("У цій групі більше немає вільних місць");
    }

    GroupMember newMember = new GroupMember();
    newMember.setSessionId(session.getId());
    newMember.setUserId(userId);
    newMember.setJoinedAt(LocalDateTime.now());
    memberRepository.save(newMember);

    // НОВЕ: Якщо після приєднання група заповнилася, переводимо в COMPLETED
    if (currentMembers + 1 == session.getLockedTargetSize()) {
      session.setStatus("COMPLETED");
      sessionRepository.save(session);
    }
  }

  @Transactional
  public GroupBuySessionResponseDto createSession(CreateGroupBuySessionDto dto, Long initiatorId) {
    Product product = productRepository.findById(dto.productId())
      .orElseThrow(() -> new ResourceNotFoundException("Товар не знайдено"));

    // НОВЕ: Валідація. Якщо юзер вже в активній сесії для ЦЬОГО товару - відмовляємо.
    if (memberRepository.isUserInActiveSessionForProduct(initiatorId, product.getId())) {
      throw new BadRequestException("Ви вже берете участь в активній сесії для цього товару. Дочекайтеся її завершення.");
    }

    GroupBuySession session = new GroupBuySession();
    session.setUuid(UUID.randomUUID().toString());
    session.setProductId(product.getId());
    session.setInitiatorId(initiatorId);
    session.setStatus("ACTIVE");
    session.setExpiresAt(LocalDateTime.now().plusHours(24));

    BigDecimal price = product.getGroupPrice() != null
      ? product.getGroupPrice()
      : product.getStandardPrice();

    session.setLockedPrice(price);

    int targetSize = product.getGroupTargetSize() != null
      ? product.getGroupTargetSize()
      : 3;

    session.setLockedTargetSize(targetSize);
    session = sessionRepository.save(session);

    GroupMember initiator = new GroupMember();
    initiator.setSessionId(session.getId());
    initiator.setUserId(initiatorId);
    initiator.setJoinedAt(LocalDateTime.now());
    memberRepository.save(initiator);

    return getSessionDetails(session.getUuid());
  }

  // НОВЕ: Отримання історії сесій користувача
  public List<GroupBuySessionResponseDto> getUserSessions(Long userId, String status) {
    List<GroupBuySession> sessions;

    if (status != null && !status.trim().isEmpty()) {
      sessions = sessionRepository.findAllByUserIdAndStatus(userId, status.toUpperCase());
    } else {
      sessions = sessionRepository.findAllByUserId(userId);
    }

    // Використовуємо існуючий метод для формування красивої відповіді з масивом учасників і товарами
    return sessions.stream()
      .map(s -> getSessionDetails(s.getUuid()))
      .collect(Collectors.toList());
  }
}
