package com.seashare.m2.reservas.application.port.in;

public interface ExpireReservationsUseCase { // puerto de entrada para expirar reservas vencidas por TTL

    int execute(); // expira las reservas en INITIATED cuyo ttl_expires_at ya venció y retorna cuántas fueron procesadas
}
