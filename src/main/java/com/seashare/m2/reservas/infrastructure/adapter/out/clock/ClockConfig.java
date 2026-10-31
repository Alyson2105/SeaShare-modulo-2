package com.seashare.m2.reservas.infrastructure.adapter.out.clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ClockConfig { // configuracion del bean de reloj para el contexto de spring

    @Bean
    public Clock clock() { // expone el reloj utc del sistema
        return Clock.systemUTC(); // retorna reloj utc
    }
}