package com.sellora.core.infrastructure.security;

import com.sellora.core.domain.entities.User;
import com.sellora.core.infrastructure.persistence.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtService jwtService;
  private final UserRepository userRepository; // НОВЕ: Інжектимо репозиторій юзерів

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
    throws ServletException, IOException {

    String jwt = null;

    if (request.getCookies() != null) {
      for (Cookie cookie : request.getCookies()) {
        if ("accessToken".equals(cookie.getName())) {
          jwt = cookie.getValue();
          break;
        }
      }
    }

    if (jwt == null) {
      final String authHeader = request.getHeader("Authorization");
      if (authHeader != null && authHeader.startsWith("Bearer ")) {
        jwt = authHeader.substring(7);
      }
    }

    if (jwt == null) {
      filterChain.doFilter(request, response);
      return;
    }

    try {
      if (jwtService.isTokenValid(jwt)) {
        Long userId = jwtService.extractUserId(jwt);

        // НОВЕ: Шукаємо користувача в БД
        User user = userRepository.findById(userId).orElse(null);

        if (user != null) {
          // Додаємо префікс "ROLE_", оскільки Spring Security очікує саме такий формат
          var authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()));

          UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
            userId, null, authorities
          );

          SecurityContextHolder.getContext().setAuthentication(authToken);
        }
      }
    } catch (Exception e) {
      // Якщо токен битий — ігноруємо
    }

    filterChain.doFilter(request, response);
  }
}
