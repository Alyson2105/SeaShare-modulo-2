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
    void noDuplicateCodes() { // valida cantidad exacta y ausencia de duplicados
        ErrorCode[] values = ErrorCode.values(); // todos los valores del enum
        assertThat(values).hasSize(20); // verifica 20 elementos

        Set<String> codes = Arrays.stream(values) // flujo de valores
                .map(ErrorCode::getCode) // extrae cada codigo
                .collect(Collectors.toSet()); // recolecta en conjunto unico

        assertThat(codes).hasSize(20); // conjunto tiene tamano 20
    }
}
