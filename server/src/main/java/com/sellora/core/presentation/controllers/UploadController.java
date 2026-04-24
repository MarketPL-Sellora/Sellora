package com.sellora.core.presentation.controllers; // Твій пакет

import com.sellora.core.application.usecases.ImageUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
   import org.springframework.http.MediaType;


@RestController
@RequestMapping("/api/v1/upload")
@RequiredArgsConstructor
public class UploadController {

  private final ImageUploadService imageUploadService;

  @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
    String imageUrl = imageUploadService.uploadImage(file);

    // Повертаємо URL у вигляді JSON: { "url": "https://..." }
    return ResponseEntity.ok(Map.of("url", imageUrl));
  }
}
