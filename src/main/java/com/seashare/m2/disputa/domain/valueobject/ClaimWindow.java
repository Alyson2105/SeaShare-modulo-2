package com.seashare.m2.disputa.domain.valueobject;

import java.time.Duration;
import java.time.Instant;

/**
 * Ventana fija de 24 horas para registrar el reclamo (CU-16 FR-005).
 *
 * <p>Semantica de borde, exigida por los specs: el reclamo se admite mientras
 * {@code now < end}; en el instante exacto {@code now == end} la ventana ya esta
 * cerrada y el job de cierre puede resolver la disputa.</p>
 */
public record ClaimWindow(Instant start, Instant end) {

    public static final Duration DURATION = Duration.ofHours(24); // ventana fija, no configurable

    public static ClaimWindow startingAt(Instant start) { // abre la ventana de 24 h en el instante dado
        return new ClaimWindow(start, start.plus(DURATION));
    }

    public boolean isOpenAt(Instant now) { // admite reclamo solo antes del fin exacto
        return now.isBefore(end);
    }

    public boolean isClosedAt(Instant now) { // en el fin exacto y despues la ventana esta cerrada
        return !isOpenAt(now);
    }
}
