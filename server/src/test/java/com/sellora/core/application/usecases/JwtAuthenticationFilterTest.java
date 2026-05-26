package com.sellora.core.application.usecases;

import com.sellora.core.domain.entities.User;
import com.sellora.core.infrastructure.persistence.UserRepository;
import com.sellora.core.infrastructure.security.JwtAuthenticationFilter;
import com.sellora.core.infrastructure.security.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JwtAuthenticationFilterTest {

  @Mock
  private JwtService jwtService;

  @Mock
  private UserRepository userRepository;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @Mock
  private FilterChain filterChain;

  @InjectMocks
  private JwtAuthenticationFilter jwtAuthenticationFilter;

  @BeforeEach
  void setUp() {
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void doFilterInternal_ValidTokenInCookie_SetsAuthentication() throws ServletException, IOException {
    // Arrange
    String token = "valid.jwt.token";
    Long userId = 1L;
    Cookie authCookie = new Cookie("accessToken", token);

    User user = new User();
    user.setId(userId);
    user.setRole("BUYER");

    when(request.getCookies()).thenReturn(new Cookie[]{authCookie});
    when(jwtService.isTokenValid(token)).thenReturn(true);
    when(jwtService.extractUserId(token)).thenReturn(userId);
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    // Act
    jwtAuthenticationFilter.doFilter(request, response, filterChain);

    // Assert
    assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    assertEquals(userId, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities()
      .stream().anyMatch(a -> a.getAuthority().equals("ROLE_BUYER")));
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void doFilterInternal_ValidTokenInHeader_SetsAuthentication() throws ServletException, IOException {
    // Arrange
    String token = "valid.jwt.token";
    Long userId = 1L;

    User user = new User();
    user.setId(userId);
    user.setRole("MERCHANT");

    when(request.getCookies()).thenReturn(null);
    when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
    when(jwtService.isTokenValid(token)).thenReturn(true);
    when(jwtService.extractUserId(token)).thenReturn(userId);
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    // Act
    jwtAuthenticationFilter.doFilter(request, response, filterChain);

    // Assert
    assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    assertEquals(userId, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void doFilterInternal_InvalidToken_DoesNotSetAuthentication() throws ServletException, IOException {
    // Arrange
    String token = "invalid.token";
    when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
    when(jwtService.isTokenValid(token)).thenReturn(false);

    // Act
    jwtAuthenticationFilter.doFilter(request, response, filterChain);

    // Assert
    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void doFilterInternal_NoToken_ProceedsToNextFilter() throws ServletException, IOException {
    // Arrange
    when(request.getCookies()).thenReturn(null);
    when(request.getHeader("Authorization")).thenReturn(null);

    // Act
    jwtAuthenticationFilter.doFilter(request, response, filterChain);

    // Assert
    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void doFilterInternal_UserNotFoundInDb_DoesNotSetAuthentication() throws ServletException, IOException {
    // Arrange
    String token = "valid.token";
    Long userId = 1L;

    when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
    when(jwtService.isTokenValid(token)).thenReturn(true);
    when(jwtService.extractUserId(token)).thenReturn(userId);
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    // Act
    jwtAuthenticationFilter.doFilter(request, response, filterChain);

    // Assert
    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(filterChain).doFilter(request, response);
  }
}
