package com.seashare.m2.reservas.domain.service;

import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.reservas.domain.model.CancellationSubtype;
import com.seashare.m2.shared.domain.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CancellationClassifierTest { // pruebas puras de clasificacion de CU-04

    private static final Instant ZARPE = Instant.parse("2027-04-10T14:00:00Z"); // zarpe fijo de referencia

    private final CancellationClassifier classifier = new CancellationClassifier(); // clase bajo prueba

    @Test
    void exactly72HoursIsFlexible() { // borde exacto de 72 horas
        CancellationClassifier.Classification result = classifier.classifyForRenter(ZARPE.minus(Duration.ofHours(72)), ZARPE);

        assertThat(result.subtype()).isEqualTo(CancellationSubtype.FLEXIBLE); // politica flexible
        assertThat(result.anticipationMinutes()).isEqualTo(4320); // 72 horas en minutos
    }

    @Test
    void oneSecondLessThan72HoursIsModerate() { // un segundo por debajo del borde flexible
        CancellationClassifier.Classification result =
                classifier.classifyForRenter(ZARPE.minus(Duration.ofHours(72)).plusSeconds(1), ZARPE);

        assertThat(result.subtype()).isEqualTo(CancellationSubtype.MODERATE); // politica moderada
        assertThat(result.anticipationMinutes()).isEqualTo(4319); // truncamiento hacia abajo
    }

    @Test
    void exactly24HoursIsModerate() { // borde exacto de 24 horas
        CancellationClassifier.Classification result = classifier.classifyForRenter(ZARPE.minus(Duration.ofHours(24)), ZARPE);

        assertThat(result.subtype()).isEqualTo(CancellationSubtype.MODERATE); // politica moderada
        assertThat(result.anticipationMinutes()).isEqualTo(1440); // 24 horas en minutos
    }

    @Test
    void oneSecondLessThan24HoursIsLate() { // un segundo por debajo del borde moderado
        CancellationClassifier.Classification result =
                classifier.classifyForRenter(ZARPE.minus(Duration.ofHours(24)).plusSeconds(1), ZARPE);

        assertThat(result.subtype()).isEqualTo(CancellationSubtype.LATE); // politica tardia
        assertThat(result.anticipationMinutes()).isEqualTo(1439); // truncamiento hacia abajo
    }

    @Test
    void oneMinuteBeforeIsLateWithOneMinute() { // un minuto antes del zarpe
        CancellationClassifier.Classification result = classifier.classifyForRenter(ZARPE.minusSeconds(60), ZARPE);

        assertThat(result.subtype()).isEqualTo(CancellationSubtype.LATE); // politica tardia
        assertThat(result.anticipationMinutes()).isEqualTo(1); // un minuto entero
    }

    @Test
    void fiftyNineSecondsBeforeIsLateWithZeroMinutes() { // menos de un minuto antes del zarpe
        CancellationClassifier.Classification result = classifier.classifyForRenter(ZARPE.minusSeconds(59), ZARPE);

        assertThat(result.subtype()).isEqualTo(CancellationSubtype.LATE); // politica tardia
        assertThat(result.anticipationMinutes()).isZero(); // truncamiento a cero
    }

    @Test
    void atDepartureIsRejected() { // no se permite cancelar en el zarpe
        assertThatThrownBy(() -> classifier.classifyForRenter(ZARPE, ZARPE))
                .isInstanceOfSatisfying(ReservationRuleException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.INVALID_RESERVATION_STATE.getCode()));
    }

    @Test
    void afterDepartureIsRejected() { // no se permite cancelar despues del zarpe
        assertThatThrownBy(() -> classifier.classifyForRenter(ZARPE.plusSeconds(1), ZARPE))
                .isInstanceOfSatisfying(ReservationRuleException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.INVALID_RESERVATION_STATE.getCode()));
    }
}