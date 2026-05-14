package com.sellora.core.presentation.controllers;

import com.sellora.core.application.usecases.ImageUploadService;
import com.sellora.core.presentation.exceptions.BadRequestException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/uploads")
@RequiredArgsConstructor
public class UploadController {

  private final ImageUploadService imageUploadService;

  @Operation(summary = "Завантаження картинки (до 3 МБ). Повертає URL.")
  @PostMapping(value = "/image", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE) // Вказуємо тип контенту
  public ResponseEntity<Map<String, String>> uploadImage(
    @Parameter(description = "Файл зображення для завантаження", content = @Content(mediaType = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE))
    @RequestParam("file") MultipartFile file) {

    // BUG-004 FIX: Валідація розміру (3 МБ = 3 * 1024 * 1024 байт)
    if (file.getSize() > 3 * 1024 * 1024) {
      throw new BadRequestException("Розмір файлу не може перевищувати 3 МБ");
    }

    // Завантажуємо в Cloudinary
    String url = imageUploadService.uploadImage(file);

    // Повертаємо JSON з посиланням: { "url": "https://..." }
    return ResponseEntity.ok(Map.of("url", url));
  }
}
