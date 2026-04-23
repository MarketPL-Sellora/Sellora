package com.sellora.core.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {

  private final String secretKey = "123123123123123123123123123123123";

  private Key getSigningKey() {
    return Keys.hmacShaKeyFor(secretKey.getBytes());
  }

  public String generateToken(Long userId) {
    return Jwts.builder()
      .setSubject(String.valueOf(userId))
      .setIssuedAt(new Date())
      .setExpiration(new Date(System.currentTimeMillis() + 86400000)) // 24 години
      .signWith(getSigningKey(), SignatureAlgorithm.HS256)
      .compact();
  }

  // НОВІ МЕТОДИ ДЛЯ ПЕРЕВІРКИ

  public Long extractUserId(String token) {
    return Long.parseLong(extractClaim(token, Claims::getSubject));
  }

  public boolean isTokenValid(String token) {
    return !isTokenExpired(token);
  }

  private boolean isTokenExpired(String token) {
    return extractClaim(token, Claims::getExpiration).before(new Date());
  }

  private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = Jwts.parserBuilder()
      .setSigningKey(getSigningKey())
      .build()
      .parseClaimsJws(token)
      .getBody();
    return claimsResolver.apply(claims);
  }
}
