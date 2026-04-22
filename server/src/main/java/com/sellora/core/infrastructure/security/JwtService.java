package com.sellora.core.infrastructure.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

@Service
public class JwtService {

  // Секретний ключ (має бути довгим). Зазвичай його винесуть в application.properties
  private final String secretKey = "123123123123123123123123123123123";

  private Key getSigningKey() {
    return Keys.hmacShaKeyFor(secretKey.getBytes());
  }

  public String generateToken(Long userId) {
    return Jwts.builder()
      .setSubject(String.valueOf(userId)) // Зберігаємо ID замість email/username
      .setIssuedAt(new Date())
      .setExpiration(new Date(System.currentTimeMillis() + 86400000)) // 24 години
      .signWith(getSigningKey(), SignatureAlgorithm.HS256)
      .compact();
  }
}
