package com.seashare.m2.shared.domain;

import java.time.Instant;

public interface ClockPort { // puerto para consultar la hora actual del sistema

    Instant now(); // obtiene el instante actual
}
