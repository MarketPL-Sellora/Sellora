package com.sellora.core.infrastructure.security;


import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthFilter; // Наш новий фільтр
  private final CustomAuthenticationEntryPoint authenticationEntryPoint;

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
      .cors(cors -> cors.configurationSource(corsConfigurationSource()))
      .csrf(csrf -> csrf.disable())
      .exceptionHandling(ex -> ex
        .authenticationEntryPoint(authenticationEntryPoint)
      )

      .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(auth -> auth
        // 1. Відкриті двері (реєстрація, логін, свагер) - сюди можна без токена
        .requestMatchers("/api/v1/auth/**",  "/swagger-ui/**","/api/v1/upload/**","/v3/api-docs/**", "/health").permitAll()
        // 2. Читати каталог товарів і категорій можуть всі (навіть неавторизовані гості сайту)
        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/categories/**", "/api/v1/products/**").permitAll()

        // 3. ЗАКРИТІ ДВЕРІ: Будь-який інший запит (створення товару, кошик, профіль) вимагає токен!
        .anyRequest().authenticated()
      )
      // ДОДАЄМО ФІЛЬТР ПЕРЕД СТАНДАРТНИМИ
      .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    // ДОЗВОЛЯЄМО ВСЕ ДЛЯ ЛОКАЛКИ (щоб пофіксити помилку зі скріншота)
    configuration.setAllowedOriginPatterns(List.of("*"));
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowCredentials(true);
    configuration.setAllowedHeaders(List.of("*"));

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
