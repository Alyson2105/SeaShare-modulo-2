package com.seashare.m2.reservas.infrastructure.adapter.in.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class CorsWebMvcConfig implements WebMvcConfigurer { // CORS para el frontend (B-01); sin Spring Security

    private final List<String> allowedOrigins; // origenes leidos de seashare.cors.allowed-origins

    public CorsWebMvcConfig(@Value("${seashare.cors.allowed-origins}") List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins; // lista separada por comas desde el yaml
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) { // registra CORS sobre las rutas de la API
        registry.addMapping("/api/**") // cubre reservas, flota y disputa
                .allowedOrigins(allowedOrigins.stream().map(String::trim).toArray(String[]::new)) // nada de origenes fijados en codigo
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS") // verbos usados por la API
                .allowedHeaders("X-User-Id", "X-User-Role", "Idempotency-Key", // identidad, idempotencia y trazas
                        "X-Correlation-Id", "Content-Type", "Accept")
                .exposedHeaders("X-Correlation-Id", "Location") // cabeceras que el navegador si puede leer
                .allowCredentials(false) // sin cookies: la identidad viaja en headers
                .maxAge(3600); // segundos que el navegador cachea el preflight
    }
}
