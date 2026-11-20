package com.seashare.m2.reservas.application.port.in;

import com.seashare.m2.reservas.application.dto.ReservationDetailView;
import com.seashare.m2.reservas.application.dto.ReservationListFilter;
import com.seashare.m2.reservas.application.dto.ReservationView;
import com.seashare.m2.reservas.application.dto.ReservationViewPage;

import java.util.UUID;

public interface ReservationQueryUseCase { // puerto de entrada para consultas paginadas y detalle de reservas

    ReservationViewPage findReservations(UUID userId, String role, int page, int limit); // consulta reservas propias sin filtro explicito

    ReservationViewPage findReservations(UUID userId, String role, int page, int limit, ReservationListFilter filter); // consulta con filtro de pestana

    ReservationView getReservationDetail(UUID reservationId, UUID userId, String role); // consulta detalle de reserva propia

    ReservationDetailView getReservationDetailView(UUID reservationId, UUID userId, String role); // detalle completo del contrato CU-21
}
