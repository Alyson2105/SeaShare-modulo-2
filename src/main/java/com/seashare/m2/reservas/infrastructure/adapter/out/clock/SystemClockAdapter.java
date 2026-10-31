package com.seashare.m2.reservas.infrastructure.adapter.out.clock;

import com.seashare.m2.shared.domain.ClockPort;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Component
public class SystemClockAdapter implements ClockPort { // adaptador de reloj del sistema

    private final Clock clock; // reloj inyectado para resolucion temporal

    public SystemClockAdapter(Clock clock) { // constructor con inyeccion de reloj
        this.clock = clock; // asigna reloj
    }

    @Override
    public Instant now() { // obtiene el instante actual
        return clock.instant(); // retorna instante provisto por el reloj
    }
}