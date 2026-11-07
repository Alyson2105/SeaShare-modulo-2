package com.seashare.m2.reservas.infrastructure.adapter.in.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer { // configuracion de spring mvc para registrar resolvers

    private final CallerIdentityArgumentResolver callerIdentityArgumentResolver; // resolver de identidad temporal

    public WebMvcConfig(CallerIdentityArgumentResolver callerIdentityArgumentResolver) { // inyeccion del resolver
        this.callerIdentityArgumentResolver = callerIdentityArgumentResolver; // asigna resolver
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) { // registra resolvers personalizados
        resolvers.add(callerIdentityArgumentResolver); // agrega resolver de CallerIdentity
    }
}
