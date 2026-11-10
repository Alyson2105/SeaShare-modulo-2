# CU-14 — Pruebas backend de eventos a M3

Casos de notificación de estado mediante outbox y relay; el payload final para M3 sigue pendiente de T-12.

**Estado global**: Draft por integraciones y contratos pendientes.

## Trazabilidad

| TC | Escenario | FR | SC | Test automatizado parcial | Status |
|---|---|---|---|---|---|
| [TC-CU14-001](TC-CU14-001-transiciones-publicables.md) | Encolado de transiciones | FR-001, FR-006 | SC-001, SC-003 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [matriz](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java) | In Process |
| [TC-CU14-002](TC-CU14-002-payload-contrato.md) | Payload para M3 | FR-001, FR-002, FR-003, FR-004 | SC-001 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | Blocked |
| [TC-CU14-003](TC-CU14-003-reintento-broker.md) | Reintentos del relay | FR-005 | SC-001 | [relay](../../../../src/test/java/com/seashare/m2/reservas/application/service/PublishPendingOutboxServiceTest.java) | In Process |
| [TC-CU14-004](TC-CU14-004-confirmacion-broker.md) | Confirmación del broker | FR-005 | SC-001 | [relay](../../../../src/test/java/com/seashare/m2/reservas/application/service/PublishPendingOutboxServiceTest.java); [integracion](../../../../src/test/java/com/seashare/m2/reservas/integration/OutboxRelayIntegrationTest.java) | In Process |
| [TC-CU14-005](TC-CU14-005-deduplicacion-consumidor.md) | Entrega duplicada | FR-005 | SC-001 | [deduplicacion](../../../../src/test/java/com/seashare/m2/reservas/integration/EventDeduplicationTest.java) | In Process |
| [TC-CU14-006](TC-CU14-006-reversion-tardia.md) | Reversión de cobro tardío | FR-001 | SC-001 | Sin prueba de CU-14 | Blocked |

## Cobertura de requisitos

| FR | Cobertura | TC | Alcance | Motivo / Observación |
|---|---|---|---|---|
| FR-001 | Parcial — ver caso y brechas | 001, 002, 006 | Backend | Estados y reversión tardía |
| FR-002 | Parcial — ver caso y brechas | 002 | Backend | Payload T-12 pendiente |
| FR-003 | Parcial — ver caso y brechas | 002 | Backend | Subtipo y anticipación pendientes |
| FR-004 | Parcial — ver caso y brechas | 002 | Backend | Notas pendientes en payload |
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
| TC-CU14-001 | Sí | No | Pendiente | — | — | In Process |
| TC-CU14-002 | Sí | No | Pendiente | — | — | Blocked |
| TC-CU14-003 | Sí | No | Pendiente | — | — | In Process |
| TC-CU14-004 | Sí | No | Pendiente | — | — | In Process |
| TC-CU14-005 | Sí | No | Pendiente | — | — | In Process |
| TC-CU14-006 | Sí | No | Pendiente | — | — | Blocked |

## Brechas detectadas

1. [Contexto](../../../context/implementContext.md): payload final M3 y mapeo PENDING/CANCELLED_* pendientes T-12.
2. Evento de información `reservation.info.provided` se emite al crear reserva; es distinto del evento de estado.
3. Aprobación tardía depende de CU-13 y mapeo de reversión todavía no implementados.
4. Publisher confirm acredita recepción por broker, no procesamiento por M3; deduplicación solo demostrada por consumidor de prueba.
5. El relay marca dead tras quinto fallo, pero aún no republica a DLQ; primer intento en 500 ms requiere medición.
6. Spec exige actor, subtipo y novedades, mientras el plan de M3 enumera un payload distinto; aclarar contrato final.

## Convenciones de estado

- **Draft**: contrato incompleto o inconsistente.
- **In Process**: ejecutable con implementación actual.
- **Approved**: aprobado tras revisión y ejecución; ninguno aquí.
- **Blocked**: depende de funcionalidad pendiente.
