package com.seashare.m2.reservas.infrastructure.adapter.in.scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Activa el scheduling de Spring para el módulo 2.
 * Se activa si el job TTL (m2.scheduler.ttl.enabled) o el relay del outbox
 * (m2.scheduler.outbox.enabled) estan habilitados; con ambos en false no hay
 * scheduling (util en tests que no necesitan ningun job).
 */
@Configuration
@EnableScheduling
@ConditionalOnExpression("'${m2.scheduler.ttl.enabled:true}' == 'true' or '${m2.scheduler.outbox.enabled:false}' == 'true' or '${m2.scheduler.payments.enabled:false}' == 'true'") // OR de los jobs
public class SchedulerConfig { // configuración que habilita el scheduling condicional del módulo 2
}
