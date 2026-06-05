package com.sellora.core.application.usecases;

import com.sellora.core.application.usecases.UserFavoriteService;
import com.sellora.core.presentation.controllers.FavoriteController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FavoriteControllerTest {

  @Mock
  private UserFavoriteService favoriteService;

  @InjectMocks
  private FavoriteController favoriteController;

  @Test
  void addFavorite_Success_ReturnsCreatedStatus() {
    Long productId = 1L;
    doNothing().when(favoriteService).addFavorite(productId);

    ResponseEntity<Void> response = favoriteController.addFavorite(productId);

    assertNotNull(response);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    verify(favoriteService, times(1)).addFavorite(productId);
  }

  @Test
  void removeFavorite_Success_ReturnsNoContentStatus() {
    Long productId = 1L;
    doNothing().when(favoriteService).removeFavorite(productId);

    ResponseEntity<Void> response = favoriteController.removeFavorite(productId);

    assertNotNull(response);
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    verify(favoriteService, times(1)).removeFavorite(productId);
  }
}
