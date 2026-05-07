package com.sellora.core.presentation.controllers;

import com.sellora.core.application.usecases.GroupBuySessionService;
import com.sellora.core.presentation.dtos.GroupBuySessionResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/share") // Зверни увагу, без /api/v1, щоб посилання було коротшим
@RequiredArgsConstructor
@Tag(name = "Share Controller", description = "Ендпоінти для генерації OpenGraph тегів для месенджерів")
public class ShareController {

  private final GroupBuySessionService sessionService;

  @Operation(summary = "Генерація HTML з OpenGraph для Telegram/Viber")
  @GetMapping(value = "/group-buy/{uuid}", produces = MediaType.TEXT_HTML_VALUE)
  public ResponseEntity<String> getOpenGraphPreview(@PathVariable String uuid) {

    // Отримуємо дані про сесію (вже написаний нами метод!)
    GroupBuySessionResponseDto session = sessionService.getSessionDetails(uuid);

    // Формуємо красивий текст для опису
    String description = String.format("Приєднуйся до спільної покупки! Зафіксована ціна: %s грн. Залишилось зібрати людей: %d",
      session.price(),
      (session.targetSize() - session.currentMembersCount()));

    // URL вашого майбутнього фронтенду (поки можна захардкодити локалхост або тестовий домен)
    String frontendRedirectUrl = "http://localhost:3000/group-buy/" + uuid;

    // Використовуємо Java Text Blocks для створення HTML
    String html = """
                <!DOCTYPE html>
                <html lang="uk">
                <head>
                    <meta charset="UTF-8">
                    <title>Спільна покупка: %s</title>

                    <meta property="og:title" content="Спільна покупка: %s" />
                    <meta property="og:description" content="%s" />
                    <meta property="og:image" content="%s" />
                    <meta property="og:type" content="website" />

                    <script>
                        window.location.href = "%s";
                    </script>
                </head>
                <body>
                    <p>Перенаправлення на сторінку товару...</p>
                    <a href="%s">Натисніть тут, якщо перенаправлення не відбулося автоматично</a>
                </body>
                </html>
                """.formatted(
      session.productTitle(),
      session.productTitle(),
      description,
      session.productImage() != null ? session.productImage() : "https://via.placeholder.com/600x400?text=Sellora", // Фолбек картинка
      frontendRedirectUrl,
      frontendRedirectUrl
    );

    return ResponseEntity.ok(html);
  }
}
