package com.seashare.m2.shared.domain;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorCodeTest { // pruebas unitarias del catalogo de errores

    @Test
    void translatesInvalidTransition() { // traduce transicion invalida a error de estado
        ErrorCode result = ErrorCode.fromInternal("INVALID_STATE_TRANSITION"); // llamada con codigo interno
        assertThat(result).isEqualTo(ErrorCode.INVALID_RESERVATION_STATE); // enum esperado
        assertThat(result.getHttpStatus()).isEqualTo(409); // codigo http 409
    }

    @Test
    void translatesVersionConflict() { // traduce conflicto de version a cambio concurrente
        ErrorCode result = ErrorCode.fromInternal("VERSION_CONFLICT"); // llamada con codigo interno
        assertThat(result).isEqualTo(ErrorCode.CONCURRENT_STATE_CHANGE); // enum esperado
        assertThat(result.getHttpStatus()).isEqualTo(409); // codigo http 409
    }

    @Test
    void unknownCodeBecomesInternalError() { // codigos desconocidos o nulos retornan error interno
        ErrorCode fromUnknown = ErrorCode.fromInternal("NON_EXISTENT_CODE"); // codigo inexistente
        assertThat(fromUnknown).isEqualTo(ErrorCode.INTERNAL_ERROR); // enum internal error
        assertThat(fromUnknown.getHttpStatus()).isEqualTo(500); // codigo http 500

        ErrorCode fromNull = ErrorCode.fromInternal(null); // codigo nulo
        assertThat(fromNull).isEqualTo(ErrorCode.INTERNAL_ERROR); // enum internal error
        assertThat(fromNull.getHttpStatus()).isEqualTo(500); // codigo http 500
    }

    @Test
    void translatesTripAlreadyCompleted() { // CU-07: viaje ya completado
        assertThat(ErrorCode.TRIP_ALREADY_COMPLETED.getCode()).isEqualTo("TRIP_ALREADY_COMPLETED"); // codigo publico
        assertThat(ErrorCode.TRIP_ALREADY_COMPLETED.getHttpStatus()).isEqualTo(409); // codigo http 409
        assertThat(ErrorCode.fromInternal("TRIP_ALREADY_COMPLETED")).isEqualTo(ErrorCode.TRIP_ALREADY_COMPLETED); // traduccion interna
    }

    @Test
    void translatesEarlyDeparture() { // CU-06: salida antes de la hora pactada
        assertThat(ErrorCode.EARLY_DEPARTURE.getCode()).isEqualTo("EARLY_DEPARTURE"); // codigo publico
        assertThat(ErrorCode.EARLY_DEPARTURE.getHttpStatus()).isEqualTo(409); // codigo http 409
        assertThat(ErrorCode.fromInternal("EARLY_DEPARTURE")).isEqualTo(ErrorCode.EARLY_DEPARTURE); // traduccion interna
    }

    @Test
    void translatesTripAlreadyStarted() { // CU-06: viaje ya iniciado
        assertThat(ErrorCode.TRIP_ALREADY_STARTED.getCode()).isEqualTo("TRIP_ALREADY_STARTED"); // codigo publico
        assertThat(ErrorCode.TRIP_ALREADY_STARTED.getHttpStatus()).isEqualTo(409); // codigo http 409
        assertThat(ErrorCode.fromInternal("TRIP_ALREADY_STARTED")).isEqualTo(ErrorCode.TRIP_ALREADY_STARTED); // traduccion interna
    }

    @Test
    void noDuplicateCodes() { // valida cantidad exacta y ausencia de duplicados
        ErrorCode[] values = ErrorCode.values(); // todos los valores del enum
        assertThat(values).hasSize(27); // 25 previos + EARLY_DEPARTURE y TRIP_ALREADY_STARTED de CU-06

        Set<String> codes = Arrays.stream(values) // flujo de valores
                .map(ErrorCode::getCode) // extrae cada codigo
                .collect(Collectors.toSet()); // recolecta en conjunto unico

        assertThat(codes).hasSize(27); // conjunto tiene tamano 27
    }
}
