# CU-14 — Pruebas backend de eventos a M3

Casos de notificación de estado mediante outbox y relay; el payload tiene discrepancias entre spec y plan.

**Estado global**: Draft; TC-002 depende de aclarar el contrato del payload. Evaluado el 2026-11-13.

## Trazabilidad

| TC | Escenario | FR | SC | Test automatizado parcial | Status |
|---|---|---|---|---|---|
| [TC-CU14-001](TC-CU14-001-transiciones-publicables.md) | Encolado de transiciones | FR-001, FR-006 | SC-001, SC-003 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [matriz](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java) | In Process |
| [TC-CU14-002](TC-CU14-002-payload-contrato.md) | Payload para M3 | FR-001, FR-002, FR-003, FR-004 | SC-001 | [serialización](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/messaging/outbox/OutboxAdapterTest.java) | Draft |
| [TC-CU14-003](TC-CU14-003-reintento-broker.md) | Reintentos del relay | FR-005 | SC-001 | [relay](../../../../src/test/java/com/seashare/m2/reservas/application/service/PublishPendingOutboxServiceTest.java) | In Process |
| [TC-CU14-004](TC-CU14-004-confirmacion-broker.md) | Confirmación del broker | FR-005 | SC-001 | [relay](../../../../src/test/java/com/seashare/m2/reservas/application/service/PublishPendingOutboxServiceTest.java); [integracion](../../../../src/test/java/com/seashare/m2/reservas/integration/OutboxRelayIntegrationTest.java) | In Process |
| [TC-CU14-005](TC-CU14-005-deduplicacion-consumidor.md) | Entrega duplicada | FR-005 | SC-001 | [deduplicacion](../../../../src/test/java/com/seashare/m2/reservas/integration/EventDeduplicationTest.java) | In Process |
| [TC-CU14-006](TC-CU14-006-reversion-tardia.md) | Reversión de cobro tardío | FR-001 | SC-001 | [confirmación](../../../../src/test/java/com/seashare/m2/reservas/integration/ConfirmPaymentIntegrationTest.java) | In Process |

## Cobertura de requisitos

| FR | Cobertura | TC | Alcance | Motivo / Observación |
|---|---|---|---|---|
| FR-001 | Parcial — ver caso y brechas | 001, 002, 006 | Backend | Estados y reversión tardía |
| FR-002 | Parcial — ver caso y brechas | 002 | Backend | Spec y plan discrepan sobre campos |
| FR-003 | Parcial — ver caso y brechas | 002 | Backend | No se serializan subtipo separado ni anticipación |
| FR-004 | Parcial — ver caso y brechas | 002 | Backend | No se serializan notas en payload |
| FR-005 | Parcial — ver caso y brechas | 003, 004, 005 | Backend | Relay y consumidor de prueba |
| FR-006 | Parcial — ver caso y brechas | 001 | Backend | Sin dinero requiere auditoría integral |

## Criterios de éxito

| SC | Estado | Observación |
|---|---|---|
| SC-001 | Parcial | TC-001, 003 a 006; broker confirm no garantiza consumo M3. |
| SC-002 | Fuera de alcance | Primer intento en 500 ms requiere medición real; scheduler tiene configuración propia. |
| SC-003 | Parcial | TC-001; ausencia global de datos financieros requiere auditoría. |

## Checklist de ejecución

| TC | Documentado | Ejecutado | Resultado | Ejecutado por | Fecha | Observaciones |
|---|---|---|---|---|---|---|
| TC-CU14-001 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Unitario: ModuleOneNotificationMatrixTest#initiatedAndPendingPaymentNotifyNobody y #expiredAndPaymentFailedNotifyNobody pasaron para decisiones locales. Faltan todas las transiciones publicables en outbox persistido. |
| TC-CU14-002 | Sí | Sí | Draft | Agente automatizado | 2026-11-13 | Contrato contradictorio: spec exige actor, subtipo, anticipación y notas; plan no los enumera. OutboxAdapterTest#statusChangedUsesFinanceStatusNames, #pendingIncludesPaymentTokenRef y #cancelledPayloadUsesFinanceStatusAndOmitsSubtype pasaron; serialización omite campos exigidos por spec. |
| TC-CU14-003 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Unitario: PublishPendingOutboxServiceTest#backoffExactoUnoCincoVeinticincoCientoVeinticincoSegundos y #quintoFalloMarcaDeadConAlerta pasaron con reloj y jitter controlados; falta confirmar los cinco intentos con broker real. |
| TC-CU14-004 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Integración: OutboxRelayIntegrationTest#publicaEnRabbitYMarcaPublishedAt pasó con PostgreSQL y RabbitMQ; EventDeduplicationTest#publicarElMismoEventoDosVecesSeProcesaUnaSolaVez comprobó Message-Id en consumidor de prueba. Falta consumo de M3 real. |
| TC-CU14-005 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Integración: EventDeduplicationTest#publicarElMismoEventoDosVecesSeProcesaUnaSolaVez pasó con RabbitMQ: dos copias, una procesada y Message-Id igual al event_id; consumidor es de prueba; no se verifican los efectos persistidos del paso 3 ni el consumidor M3. |
| TC-CU14-006 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Integración: ConfirmPaymentIntegrationTest#approvedOnExpiredReservationPublishesReversal pasó: EXPIRED se conserva y se encola evento de reversión en puerto simulado. Falta verificar payload y entrega real a M3. |

## Brechas detectadas

2. Evento de información `reservation.info.provided` se emite al crear reserva; es distinto del evento de estado.
4. Publisher confirm acredita recepción por broker, no procesamiento por M3; deduplicación solo demostrada por consumidor de prueba.
5. El relay marca dead tras quinto fallo, pero aún no republica a DLQ; primer intento en 500 ms requiere medición.
6. Spec exige actor, subtipo y novedades, mientras el plan de M3 enumera un payload distinto; aclarar contrato final.

## Convenciones de estado

- **Draft**: contrato incompleto o inconsistente.
- **In Process**: ejecutable con implementación actual.
- **Approved**: aprobado tras revisión y ejecución; ninguno aquí.
- **Blocked**: depende de funcionalidad pendiente.

## Ejecución automatizada — 2026-11-13

`sg docker -c 'bash ./mvnw -B clean test'`: salida 1 por ErrorCodeTest#noDuplicateCodes, sin atribuir ese fallo a CU-14. Las pruebas citadas usan las dependencias indicadas en cada caso; no acreditan servicios externos reales.

| Clase | Pruebas | Fallos | Errores | Omitidos | Resultado |
|---|---:|---:|---:|---:|---|
| [ModuleOneNotificationMatrixTest](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java) | 7 | 0 | 0 | 0 | Verde; cobertura según TC |
| [OutboxAdapterTest](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/messaging/outbox/OutboxAdapterTest.java) | 15 | 0 | 0 | 0 | Verde; cobertura según TC |
| [FinanceStatusMapperTest](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/messaging/FinanceStatusMapperTest.java) | 13 | 0 | 0 | 0 | Verde; cobertura según TC |
| [PublishPendingOutboxServiceTest](../../../../src/test/java/com/seashare/m2/reservas/application/service/PublishPendingOutboxServiceTest.java) | 10 | 0 | 0 | 0 | Verde; cobertura según TC |
| [OutboxRelayIntegrationTest](../../../../src/test/java/com/seashare/m2/reservas/integration/OutboxRelayIntegrationTest.java) | 4 | 0 | 0 | 0 | Verde; cobertura según TC |
| [EventDeduplicationTest](../../../../src/test/java/com/seashare/m2/reservas/integration/EventDeduplicationTest.java) | 1 | 0 | 0 | 0 | Verde; cobertura según TC |
| [ConfirmPaymentIntegrationTest](../../../../src/test/java/com/seashare/m2/reservas/integration/ConfirmPaymentIntegrationTest.java) | 6 | 0 | 0 | 0 | Verde; cobertura según TC |
