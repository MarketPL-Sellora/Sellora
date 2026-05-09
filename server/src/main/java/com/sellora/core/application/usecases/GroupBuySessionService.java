package com.sellora.core.application.usecases;

import com.sellora.core.domain.entities.GroupBuySession;
import com.sellora.core.domain.entities.GroupMember;
import com.sellora.core.domain.entities.Product;
import com.sellora.core.infrastructure.persistence.GroupBuySessionRepository;
import com.sellora.core.infrastructure.persistence.GroupMemberRepository;
import com.sellora.core.infrastructure.persistence.ProductRepository;
import com.sellora.core.presentation.dtos.GroupBuySessionResponseDto;
import com.sellora.core.presentation.exceptions.BadRequestException;
import com.sellora.core.presentation.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import com.sellora.core.presentation.dtos.CreateGroupBuySessionDto;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class GroupBuySessionService {

  private final GroupBuySessionRepository sessionRepository;
  private final GroupMemberRepository memberRepository;
  private final ProductRepository productRepository;

  public GroupBuySessionResponseDto getSessionDetails(String uuid) {
    // 1. Шукаємо сесію. Якщо немає - кидаємо помилку 404
    GroupBuySession session = sessionRepository.findByUuid(uuid)
      .orElseThrow(() -> new ResourceNotFoundException("Сесію за таким посиланням не знайдено"));

    // 2. Дістаємо товар, щоб показати його назву і картинку
    Product product = productRepository.findById(session.getProductId())
      .orElseThrow(() -> new ResourceNotFoundException("Товар не знайдено"));

    // 3. Рахуємо, скільки людей вже приєдналося
    int currentMembers = memberRepository.countBySessionId(session.getId());

    // 4. ВАЛІДАЦІЯ: Перевіряємо, чи можна ще приєднатися
    boolean isAvailable = "ACTIVE".equals(session.getStatus()) && currentMembers < session.getLockedTargetSize();

    // 5. Беремо першу картинку товару для прев'ю
    String mainImage = (product.getImages() != null && !product.getImages().isEmpty())
      ? product.getImages().get(0)
      : null;

    // 6. Повертаємо сформований DTO
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
      LocalDateTime.now()
    );
  }

  public void joinSession(String uuid, Long userId) {
    // 1. Шукаємо сесію
    GroupBuySession session = sessionRepository.findByUuid(uuid)
      .orElseThrow(() -> new ResourceNotFoundException("Сесію не знайдено"));

    // 2. Перевіряємо, чи вона активна
    if (!"ACTIVE".equals(session.getStatus())) {
      throw new BadRequestException("Ця сесія вже неактивна");
    }

    // 3. Перевіряємо, чи юзер уже НЕ є учасником (щоб не було дублікатів у базі)
    if (memberRepository.existsBySessionIdAndUserId(session.getId(), userId)) {
      throw new BadRequestException("Ви вже є учасником цієї групової покупки");
    }

    // 4. Перевіряємо, чи є вільні місця
    int currentMembers = memberRepository.countBySessionId(session.getId());
    if (currentMembers >= session.getLockedTargetSize()) {
      throw new BadRequestException("У цій групі більше немає вільних місць");
    }

    // 5. Зберігаємо нового учасника
    GroupMember newMember = new GroupMember();
    newMember.setSessionId(session.getId());
    newMember.setUserId(userId);

    memberRepository.save(newMember);
  }

  @Transactional
  public GroupBuySessionResponseDto createSession(CreateGroupBuySessionDto dto, Long initiatorId) {
    // 1. Шукаємо товар
    Product product = productRepository.findById(dto.productId())
      .orElseThrow(() -> new ResourceNotFoundException("Товар не знайдено"));

    // 2. Створюємо нову сесію (AC #4: дедлайн 24 години, AC #5: генерація UUID)
    GroupBuySession session = new GroupBuySession();
    session.setUuid(UUID.randomUUID().toString()); // Генеруємо унікальний лінк
    session.setProductId(product.getId());
    session.setInitiatorId(initiatorId);
    session.setStatus("ACTIVE");
    session.setExpiresAt(LocalDateTime.now().plusHours(24)); // Дедлайн +24 години

    BigDecimal price = product.getGroupPrice() != null
      ? product.getGroupPrice()
      : product.getStandardPrice(); // фолбек, якщо групову ціну забули вказати

    session.setLockedPrice(price);

    // Цільова кількість людей (беремо з товару, дефолт - 3)
    int targetSize = product.getGroupTargetSize() != null
      ? product.getGroupTargetSize()
      : 3;

    session.setLockedTargetSize(targetSize);

    session = sessionRepository.save(session); // Зберігаємо сесію, щоб отримати її ID

    // 3. Ініціатор стає першим учасником (AC #3)
    GroupMember initiator = new GroupMember();
    initiator.setSessionId(session.getId());
    initiator.setUserId(initiatorId);
    memberRepository.save(initiator);

    // 4. Повертаємо деталі щойно створеної сесії (використовуємо наш старий метод!)
    return getSessionDetails(session.getUuid());
  }
}
