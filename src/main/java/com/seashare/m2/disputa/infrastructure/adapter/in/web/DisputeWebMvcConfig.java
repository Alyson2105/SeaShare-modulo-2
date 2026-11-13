package com.seashare.m2.disputa.infrastructure.adapter.in.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class DisputeWebMvcConfig implements WebMvcConfigurer { // registra el resolver de identidad de disputa

    private final DisputeCallerIdentityResolver identityResolver; // resolver propio del contexto

    public DisputeWebMvcConfig(DisputeCallerIdentityResolver identityResolver) {
        this.identityResolver = identityResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(identityResolver);
    }
}
