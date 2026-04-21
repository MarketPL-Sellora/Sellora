package com.sellora.core.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
      .csrf(csrf -> csrf.disable()) // Вимикаємо для розробки
      .authorizeHttpRequests(auth -> auth
        .requestMatchers("/health", "/swagger-ui/**", "/v3/api-docs/**", "/api/v1/trigger-error").permitAll() // Дозволяємо ці шляхи без логіна
        .anyRequest().authenticated() // Все інше потребує авторизації
      )
      .formLogin(form -> form.permitAll()); // Залишаємо форму логіна, але вона не блокуватиме /health

    return http.build();
  }
}
