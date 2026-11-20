package com.seashare.m2.reservas.infrastructure.adapter.in.web;

import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.shared.domain.ErrorCode;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.UUID;

// identidad temporal por headers (sin Spring Security); se reemplaza cuando exista autenticacion real.
@Component
public class CallerIdentityArgumentResolver implements HandlerMethodArgumentResolver { // resuelve la identidad del usuario a partir de headers http

    @Override
    public boolean supportsParameter(MethodParameter parameter) { // verifica si el argumento coincide con CallerIdentity
        return parameter.getParameterType().equals(CallerIdentity.class); // confirma coincidencia de tipo
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) { // extrae y valida los encabezados de identidad
        String userIdHeader = webRequest.getHeader("X-User-Id"); // obtiene encabezado de id de usuario
        String roleHeader = webRequest.getHeader("X-User-Role"); // obtiene encabezado de rol

        if (userIdHeader == null || userIdHeader.isBlank() || roleHeader == null || roleHeader.isBlank()) { // verifica presencia obligatoria
            throw new ReservationRuleException(ErrorCode.UNAUTHENTICATED, "Missing required authentication headers"); // rechaza peticion sin identidad
        }

        UUID userId;
        try {
            userId = UUID.fromString(userIdHeader.trim()); // parsea identificador uuid
        } catch (IllegalArgumentException e) {
            throw new ReservationRuleException(ErrorCode.INVALID_UUID, "Invalid UUID format in X-User-Id header"); // rechaza uuid con formato invalido
        }

        return new CallerIdentity(userId, roleHeader.trim()); // construye objeto de identidad autenticada
    }
}
