package com.seashare.m2.reservas.infrastructure.adapter.in.scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Activa el scheduling de Spring para el módulo 2.
 * Se puede deshabilitar con m2.scheduler.ttl.enabled=false (útil en tests que no necesitan el job).
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "m2.scheduler.ttl.enabled", havingValue = "true", matchIfMissing = true) // habilitado por defecto; desactivable en tests
public class SchedulerConfig { // configuración que habilita el scheduling condicional del módulo 2
}
