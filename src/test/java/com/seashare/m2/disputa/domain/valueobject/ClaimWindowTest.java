package com.seashare.m2.disputa.domain.valueobject;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ClaimWindowTest { // bordes exactos de la ventana fija de 24 h (CU-16 FR-005)

    private static final Instant START = Instant.parse("2026-11-13T10:00:00Z");

    @Test
    void windowLastsExactly24Hours() { // la ventana es fija, no configurable
        ClaimWindow window = ClaimWindow.startingAt(START);

        assertThat(window.start()).isEqualTo(START);
        assertThat(Duration.between(window.start(), window.end())).isEqualTo(Duration.ofHours(24));
    }

    @Test
    void acceptsClaimOneSecondBeforeEnd() { // ultimo instante valido
        ClaimWindow window = ClaimWindow.startingAt(START);

        assertThat(window.isOpenAt(window.end().minusSeconds(1))).isTrue();
        assertThat(window.isClosedAt(window.end().minusSeconds(1))).isFalse();
    }

    @Test
    void rejectsClaimAtExactEnd() { // now == window_end ya esta cerrada
        ClaimWindow window = ClaimWindow.startingAt(START);

        assertThat(window.isOpenAt(window.end())).isFalse();
        assertThat(window.isClosedAt(window.end())).isTrue();
    }

    @Test
    void rejectsClaimAfterEnd() { // pasado el fin
        ClaimWindow window = ClaimWindow.startingAt(START);

        assertThat(window.isOpenAt(window.end().plusSeconds(1))).isFalse();
        assertThat(window.isClosedAt(window.end().plusSeconds(1))).isTrue();
    }
}
