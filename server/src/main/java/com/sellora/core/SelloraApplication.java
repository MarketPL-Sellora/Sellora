package com.sellora.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SelloraApplication {
  public static void main(String[] args) {
    SpringApplication.run(SelloraApplication.class, args);
  }
}
