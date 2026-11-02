package com.seashare.m2.reservas.application.dto;

import java.util.List;

public record ReservationViewPage(List<ReservationView> items, long totalItems) {
}
