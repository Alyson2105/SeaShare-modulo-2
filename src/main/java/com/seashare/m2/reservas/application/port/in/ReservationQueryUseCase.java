package com.seashare.m2.reservas.application.port.in;

import com.seashare.m2.reservas.application.dto.ReservationView;
import com.seashare.m2.reservas.application.dto.ReservationViewPage;

import java.util.UUID;

public interface ReservationQueryUseCase { // puerto de entrada para consultas paginadas y detalle de reservas

    ReservationViewPage findReservations(UUID userId, String role, int page, int limit); // consulta reservas propias con paginacion

    ReservationView getReservationDetail(UUID reservationId, UUID userId, String role); // consulta detalle de reserva propia
}
