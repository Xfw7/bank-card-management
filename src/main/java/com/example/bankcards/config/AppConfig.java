package com.example.bankcards.config;

import com.example.bankcards.config.properties.AdminProperties;
import com.example.bankcards.config.properties.CorsProperties;
import com.example.bankcards.config.properties.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class, AdminProperties.class})
public class AppConfig {
}
