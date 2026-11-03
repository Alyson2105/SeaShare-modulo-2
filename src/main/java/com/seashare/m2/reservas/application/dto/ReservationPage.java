package com.seashare.m2.reservas.application.dto;

import com.seashare.m2.reservas.domain.model.Reservation;

import java.util.List;

public record ReservationPage(List<Reservation> items, long totalItems) { // pagina de reservas de dominio con total de elementos
}
