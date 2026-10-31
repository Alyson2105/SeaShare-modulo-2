package com.seashare.m2.reservas.infrastructure.adapter.in.web;

import java.util.UUID;

public record CallerIdentity(
        UUID userId, // identificador del usuario que realiza la llamada
        String role // rol del usuario que realiza la llamada
) { // representacion inmutable de la identidad del llamador
}
