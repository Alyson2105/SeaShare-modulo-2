package com.seashare.m2.disputa.infrastructure.adapter.in.web;

import com.seashare.m2.disputa.domain.exception.DisputeRuleException;
import com.seashare.m2.shared.domain.ErrorCode;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.UUID;

// Identidad temporal por headers, equivalente a la de reservas pero propia del contexto disputa.
@Component
public class DisputeCallerIdentityResolver implements HandlerMethodArgumentResolver { // resuelve la identidad del llamador

    @Override
    public boolean supportsParameter(MethodParameter parameter) { // solo resuelve DisputeCallerIdentity
        return parameter.getParameterType().equals(DisputeCallerIdentity.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) { // extrae y valida los headers
        String userIdHeader = webRequest.getHeader("X-User-Id"); // header de identificador
        String roleHeader = webRequest.getHeader("X-User-Role"); // header de rol

        if (userIdHeader == null || userIdHeader.isBlank() || roleHeader == null || roleHeader.isBlank()) {
            throw new DisputeRuleException(ErrorCode.UNAUTHENTICATED, "Missing required authentication headers");
        }

        UUID userId;
        try {
            userId = UUID.fromString(userIdHeader.trim()); // valida formato uuid
        } catch (IllegalArgumentException e) {
            throw new DisputeRuleException(ErrorCode.INVALID_UUID, "Invalid UUID format in X-User-Id header");
        }

        return new DisputeCallerIdentity(userId, roleHeader.trim());
    }
}
