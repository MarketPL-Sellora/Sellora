package com.sellora.core.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtService jwtService;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
    throws ServletException, IOException {

    String jwt = null;

    // 1. Спочатку шукаємо токен у 쿠ках (наш новий спосіб)
    if (request.getCookies() != null) {
      for (Cookie cookie : request.getCookies()) {
        if ("accessToken".equals(cookie.getName())) { // Назва кукі, яку ми бачили в DevTools
          jwt = cookie.getValue();
          break;
        }
      }
    }

    // 2. Якщо в куках немає, перевіряємо старий заголовок Authorization (на всяк випадок)
    if (jwt == null) {
      final String authHeader = request.getHeader("Authorization");
      if (authHeader != null && authHeader.startsWith("Bearer ")) {
        jwt = authHeader.substring(7);
      }
    }

    // 3. Якщо токена так і не знайшли - пропускаємо запит (Spring Security його потім заблокує)
    if (jwt == null) {
      filterChain.doFilter(request, response);
      return;
    }

    try {
      if (jwtService.isTokenValid(jwt)) {
        Long userId = jwtService.extractUserId(jwt);

        // Створюємо об'єкт аутентифікації для Spring Security
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
          userId, null, Collections.emptyList() // Зверни увагу: тут поки немає ролей (Collections.emptyList())
        );

        // Кладемо юзера в "контекст" — тепер Спрінг знає, хто це
        SecurityContextHolder.getContext().setAuthentication(authToken);
      }
    } catch (Exception e) {
      // Якщо токен битий — просто ігноруємо, SecurityConfig розбереться далі
    }

    filterChain.doFilter(request, response);
  }
}
