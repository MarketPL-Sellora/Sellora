package com.sellora.core.application.usecases;

import com.sellora.core.domain.entities.MerchantRequisite;
import com.sellora.core.domain.entities.User;
import com.sellora.core.infrastructure.persistence.MerchantRequisiteRepository;
import com.sellora.core.infrastructure.persistence.UserRepository;
import com.sellora.core.presentation.dtos.MerchantRequisiteDto;
import com.sellora.core.presentation.exceptions.ConflictException;
import com.sellora.core.presentation.exceptions.ForbiddenException;
import com.sellora.core.presentation.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MerchantRequisiteService {
  private final MerchantRequisiteRepository requisiteRepository;
  private final UserRepository userRepository;

  private void verifyMerchant(Long userId) {
    User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    if (!"MERCHANT".equalsIgnoreCase(user.getRole()) && !"ADMIN".equalsIgnoreCase(user.getRole())) {
      throw new ForbiddenException("Тільки MERCHANT або ADMIN мають доступ до реквізитів");
    }
  }

  @Transactional
  public MerchantRequisiteDto create(MerchantRequisiteDto dto, Long userId) {
    verifyMerchant(userId);

    // Логіка is_primary
    if (dto.isPrimary()) {
      requisiteRepository.findByOwnerIdAndIsPrimaryTrue(userId).ifPresent(oldPrimary -> {
        oldPrimary.setIsPrimary(false);
        requisiteRepository.save(oldPrimary);
      });
    }

    MerchantRequisite req = new MerchantRequisite();
    req.setOwnerId(userId);
    req.setEdrpou(dto.edrpou());
    req.setIban(dto.iban());
    req.setBankName(dto.bankName());
    req.setIsPrimary(dto.isPrimary());

    MerchantRequisite saved = requisiteRepository.save(req);
    return mapToDto(saved);
  }

  @Transactional
  public MerchantRequisiteDto update(Long id, MerchantRequisiteDto dto, Long userId) {
    verifyMerchant(userId);
    MerchantRequisite req = requisiteRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Реквізити не знайдено"));

    if (!req.getOwnerId().equals(userId)) throw new ForbiddenException("Це не ваші реквізити");

    if (dto.isPrimary() && !req.getIsPrimary()) {
      requisiteRepository.findByOwnerIdAndIsPrimaryTrue(userId).ifPresent(oldPrimary -> {
        oldPrimary.setIsPrimary(false);
        requisiteRepository.save(oldPrimary);
      });
    }

    req.setEdrpou(dto.edrpou());
    req.setIban(dto.iban());
    req.setBankName(dto.bankName());
    req.setIsPrimary(dto.isPrimary());

    return mapToDto(requisiteRepository.save(req));
  }

  @Transactional
  public void delete(Long id, Long userId) {
    verifyMerchant(userId);
    MerchantRequisite req = requisiteRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Реквізити не знайдено"));

    if (!req.getOwnerId().equals(userId) && !"ADMIN".equalsIgnoreCase(userRepository.findById(userId).get().getRole())) {
      throw new ForbiddenException("Недостатньо прав");
    }

    if (req.getIsPrimary()) throw new ConflictException("Не можна видалити основні реквізити (is_primary = true)");
    if (requisiteRepository.countByOwnerId(req.getOwnerId()) <= 1) throw new ConflictException("Не можна видалити останні реквізити");

    requisiteRepository.delete(req);
  }

  public List<MerchantRequisiteDto> getAllByOwner(Long ownerId) {
    return requisiteRepository.findByOwnerId(ownerId).stream().map(this::mapToDto).toList();
  }

  private MerchantRequisiteDto mapToDto(MerchantRequisite r) {
    return new MerchantRequisiteDto(r.getId(), r.getOwnerId(), r.getEdrpou(), r.getIban(), r.getBankName(), r.getIsPrimary(), r.getCreatedAt());
  }
}
