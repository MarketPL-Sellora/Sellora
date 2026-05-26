package com.sellora.core.application.usecases;

import com.sellora.core.domain.entities.UserSettings;
import com.sellora.core.infrastructure.persistence.UserSettingsRepository;
import com.sellora.core.presentation.dtos.UserSettingsDto;
import com.sellora.core.presentation.exceptions.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserSettingsService {

  private final UserSettingsRepository userSettingsRepository;

  private Long getCurrentUserId() {
    Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    if (principal.equals("anonymousUser")) throw new UnauthorizedException("Потрібна авторизація");
    return (Long) principal;
  }

  @Transactional
  public UserSettingsDto getUserSettings() {
    Long userId = getCurrentUserId();

    // Шукаємо налаштування. Якщо немає (старий юзер) - створюємо "на льоту" дефолтні
    UserSettings settings = userSettingsRepository.findById(userId).orElseGet(() -> {
      UserSettings newSettings = new UserSettings(userId, true);
      return userSettingsRepository.save(newSettings);
    });

    return new UserSettingsDto(settings.getNotifyEmailOnOrderStatus());
  }

  @Transactional
  public UserSettingsDto updateUserSettings(UserSettingsDto dto) {
    Long userId = getCurrentUserId();

    UserSettings settings = userSettingsRepository.findById(userId).orElse(new UserSettings(userId, true));

    // Жорстке оновлення (Hard Update)
    settings.setNotifyEmailOnOrderStatus(dto.notifyEmailOnOrderStatus());
    userSettingsRepository.save(settings);

    return new UserSettingsDto(settings.getNotifyEmailOnOrderStatus());
  }
}
