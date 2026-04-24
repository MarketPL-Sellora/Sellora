package com.sellora.core.application.usecases; // Твій пакет

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ImageUploadService {

  private final Cloudinary cloudinary;

  public String uploadImage(MultipartFile file) {
    try {
      // Відправляємо файл у Cloudinary
      Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());

      // Витягуємо безпечне посилання (https) на завантажену картинку
      return uploadResult.get("secure_url").toString();

    } catch (IOException e) {
      throw new RuntimeException("Помилка завантаження картинки", e);
    }
  }
}
