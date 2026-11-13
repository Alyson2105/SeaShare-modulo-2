package com.seashare.m2.reservas.domain.service;

import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.reservas.domain.model.CancellationSubtype;
import com.seashare.m2.shared.domain.ErrorCode;

import java.time.Duration;
import java.time.Instant;

public class CancellationClassifier { // clasifica la cancelacion del arrendatario segun la anticipacion

    private static final long FLEXIBLE_THRESHOLD_MINUTES = 72L * 60; // umbral de 72 horas en minutos

    private static final long MODERATE_THRESHOLD_MINUTES = 24L * 60; // umbral de 24 horas en minutos

    public Classification classifyForRenter(Instant now, Instant scheduledDeparture) { // calcula subtipo y minutos truncados
        if (!now.isBefore(scheduledDeparture)) { // no se permite cancelar en el zarpe ni despues
            throw new ReservationRuleException(ErrorCode.INVALID_RESERVATION_STATE,
                    "Cancellation is not allowed at or after scheduled departure"); // rechaza solicitud fuera de plazo
        }

        long rawMinutes = Duration.between(now, scheduledDeparture).toMinutes(); // minutos enteros truncados hacia abajo
        int anticipationMinutes = (int) Math.min(rawMinutes, Integer.MAX_VALUE); // limita el valor para el contrato

        CancellationSubtype subtype; // clasificacion resultante
        if (anticipationMinutes >= FLEXIBLE_THRESHOLD_MINUTES) { // 72 horas o mas
            subtype = CancellationSubtype.FLEXIBLE; // politica flexible
        } else if (anticipationMinutes >= MODERATE_THRESHOLD_MINUTES) { // entre 24 y menos de 72 horas
            subtype = CancellationSubtype.MODERATE; // politica moderada
        } else { // menos de 24 horas
            subtype = CancellationSubtype.LATE; // politica tardia
        }

        return new Classification(subtype, anticipationMinutes); // devuelve la clasificacion completa
    }

    public record Classification(CancellationSubtype subtype, int anticipationMinutes) { // resultado de la clasificacion
    }
}