package com.sellora.core.application.usecases;

import com.sellora.core.infrastructure.security.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.Key;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class JwtServiceTest {

  private JwtService jwtService;

  // Секретний ключ, ідентичний тому, що використовується в JwtService
  private final String SECRET_KEY = "123123123123123123123123123123123";

  @BeforeEach
  void setUp() {
    // Пряма ініціалізація без рефлексії
    jwtService = new JwtService();
  }

  @Test
  void generateToken_ReturnsValidJwtFormat() {
    // Arrange
    Long userId = 1L;

    // Act
    String token = jwtService.generateToken(userId);

    // Assert
    assertNotNull(token);
    String[] parts = token.split("\\.");
    assertEquals(3, parts.length);
  }

  @Test
  void extractUserId_ValidToken_ReturnsCorrectId() {
    // Arrange
    Long expectedUserId = 123L;
    String token = jwtService.generateToken(expectedUserId);

    // Act
    Long actualUserId = jwtService.extractUserId(token);

    // Assert
    assertEquals(expectedUserId, actualUserId);
  }

  @Test
  void parseToken_MalformedStructure_ThrowsMalformedJwtException() {
    // Arrange
    String invalidToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.invalid-payload";

    // Act & Assert
    assertThrows(MalformedJwtException.class, () -> {
      jwtService.extractUserId(invalidToken);
    });
  }

  @Test
  void parseToken_InvalidSignature_ThrowsSignatureException() {
    // Arrange
    Long userId = 1L;
    String validToken = jwtService.generateToken(userId);

    // Модифікація підпису (Tampering)
    String tamperedToken = validToken.substring(0, validToken.length() - 5) + "abcde";

    // Act & Assert
    assertThrows(SignatureException.class, () -> {
      jwtService.extractUserId(tamperedToken);
    });
  }

  @Test
  void parseToken_ExpiredTime_ThrowsExpiredJwtException() {
    // Arrange: Ручна генерація простроченого токена (Expiration у минулому)
    Key key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    String expiredToken = Jwts.builder()
      .setSubject("1")
      .setIssuedAt(new Date(System.currentTimeMillis() - 10000))
      .setExpiration(new Date(System.currentTimeMillis() - 1000))
      .signWith(key, SignatureAlgorithm.HS256)
      .compact();

    // Act & Assert
    assertThrows(ExpiredJwtException.class, () -> {
      jwtService.extractUserId(expiredToken);
    });
  }

  @Test
  void isTokenValid_ValidToken_ReturnsTrue() {
    // Arrange
    String token = jwtService.generateToken(1L);

    // Act
    boolean isValid = jwtService.isTokenValid(token);

    // Assert
    assertTrue(isValid);
  }
}
