package com.sellora.core.application.usecases;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.sellora.core.presentation.exceptions.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImageUploadServiceTest {

  @Mock
  private Cloudinary cloudinary;

  @Mock
  private Uploader uploader;

  @InjectMocks
  private ImageUploadService imageUploadService;

  @BeforeEach
  void setUp() {
    // Важливо: для тестів, що доходять до завантаження, uploader не має бути null
    lenient().when(cloudinary.uploader()).thenReturn(uploader);
  }

  @Test
  void uploadImage_InvalidFormat_ThrowsBadRequestException() {
    MockMultipartFile file = new MockMultipartFile(
      "file", "test.txt", "text/plain", "content".getBytes());

    BadRequestException ex = assertThrows(BadRequestException.class,
      () -> imageUploadService.uploadImage(file));
    assertTrue(ex.getMessage().contains("Недопустимий формат"));
  }

  @Test
  void uploadImage_FileTooLarge_ThrowsBadRequestException() {
    byte[] largeContent = new byte[11 * 1024 * 1024]; // 11MB
    MockMultipartFile file = new MockMultipartFile(
      "file", "large.jpg", "image/jpeg", largeContent);

    BadRequestException ex = assertThrows(BadRequestException.class,
      () -> imageUploadService.uploadImage(file));
    assertTrue(ex.getMessage().contains("занадто великий"));
  }

  @Test
  void uploadImage_Success_ReturnsUrl() throws IOException {
    MockMultipartFile file = new MockMultipartFile(
      "file", "image.jpg", "image/jpeg", "data".getBytes());

    when(uploader.upload(any(), anyMap())).thenReturn(Map.of("secure_url", "https://cloud.com/img.jpg"));

    String url = imageUploadService.uploadImage(file);

    assertEquals("https://cloud.com/img.jpg", url);
    verify(uploader).upload(any(), anyMap());
  }
}
