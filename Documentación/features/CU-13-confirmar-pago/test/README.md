# CU-13 — Pruebas backend de confirmación de pago

Casos del canal A de notificación y canal B de consulta. La lógica de confirmación y consulta periódica están implementadas; no se encontró endpoint del canal A.

**Estado global**: In Process; cinco casos tienen cobertura parcial y dos dependen del canal A ausente. Evaluado el 2026-11-13.

## Trazabilidad

| TC | Escenario | FR | SC | Test automatizado parcial | Status |
|---|---|---|---|---|---|
| [TC-CU13-001](TC-CU13-001-aprobacion-vigente.md) | Aprobación con TTL vigente | FR-002, FR-003, FR-004 | SC-001 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [ConfirmPaymentIntegrationTest](../../../../src/test/java/com/seashare/m2/reservas/integration/ConfirmPaymentIntegrationTest.java) | In Process |
| [TC-CU13-002](TC-CU13-002-aprobacion-tardia.md) | Aprobación tras TTL | FR-003, FR-006 | SC-002, SC-003 | [ttl](../../../../src/test/java/com/seashare/m2/reservas/integration/TtlExpiryIntegrationTest.java); [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [ConfirmPaymentIntegrationTest](../../../../src/test/java/com/seashare/m2/reservas/integration/ConfirmPaymentIntegrationTest.java) | In Process |
| [TC-CU13-003](TC-CU13-003-rechazo-reintentable.md) | Rechazo con TTL vigente | FR-005 | SC-002 | [matriz](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java); [ConfirmPaymentIntegrationTest](../../../../src/test/java/com/seashare/m2/reservas/integration/ConfirmPaymentIntegrationTest.java) | In Process |
| [TC-CU13-004](TC-CU13-004-fallo-definitivo.md) | Fallo definitivo | FR-005 | SC-002 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [matriz](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java); [ConfirmPaymentIntegrationTest](../../../../src/test/java/com/seashare/m2/reservas/integration/ConfirmPaymentIntegrationTest.java) | In Process |
| [TC-CU13-005](TC-CU13-005-confirmacion-duplicada.md) | Idempotencia de confirmación | FR-007 | SC-004 | [ConfirmPaymentIntegrationTest](../../../../src/test/java/com/seashare/m2/reservas/integration/ConfirmPaymentIntegrationTest.java) | Blocked |
| [TC-CU13-006](TC-CU13-006-consulta-estados.md) | Consulta del resultado en M3 | FR-001, FR-003, FR-005 | SC-001, SC-002 | [PollPaymentStatusServiceTest](../../../../src/test/java/com/seashare/m2/reservas/application/service/PollPaymentStatusServiceTest.java) | In Process |
| [TC-CU13-007](TC-CU13-007-payload-invalido.md) | Validación del mensaje | FR-002, FR-003 | SC-001 | Sin prueba de CU-13 | Blocked |

## Cobertura de requisitos

| FR | Cobertura | TC | Alcance | Motivo / Observación |
|---|---|---|---|---|
| FR-001 | Parcial | 006 | Backend | Canal B implementado; canal A ausente |
| FR-002 | Parcial | 001, 007 | Backend | Payload |
| FR-003 | Parcial | 001, 002, 006, 007 | Backend | Estado y existencia |
| FR-004 | Parcial | 001 | Backend | Aprobación |
| FR-005 | Parcial | 003, 004, 006 | Backend | Rechazos |
| FR-006 | Parcial | 002 | Backend | Reversión |
| FR-007 | Parcial | 005 | Backend | Idempotencia por transacción |
| FR-008 | Parcial | 001, 006 | Backend | Sin dinero requiere auditoría integral |


## Criterios de éxito

| SC | Estado | Observación |
|---|---|---|
| SC-001 | Parcial | Confirmación probada en integración; plazo y canal externo requieren medición. |
| SC-002 | Parcial | Reversión tardía y estados probados con puertos simulados. |
| SC-003 | Parcial | Evento de reversión encolado a puerto simulado; falta entrega a M3. |
| SC-004 | Parcial | Duplicado probado en lógica interna; falta canal A. |
| SC-005 | Pendiente | Neutralidad financiera requiere revisión integral. |

## Checklist de ejecución

| TC | Documentado | Ejecutado | Resultado | Ejecutado por | Fecha | Observaciones |
|---|---|---|---|---|---|---|
| TC-CU13-001 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Integración: ConfirmPaymentIntegrationTest#approvedWithinTtlReservesWithPaymentReference pasó: RESERVED y referencia persistida; falta comprobar hora y canal externo completo. |
| TC-CU13-002 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Integración: ConfirmPaymentIntegrationTest#lateApprovalPublishesCancelledFlexibleOnceAndKeepsState y #approvedOnExpiredReservationPublishesReversal pasaron: no se reserva y se encola reversión en puerto simulado. Falta entrega real y canal A. |
| TC-CU13-003 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Integración: ConfirmPaymentIntegrationTest#rejectedChargeKeepsPendingPaymentAndAllowsRetry pasó: estado pendiente y nuevo token. Falta comprobar motivo, TTL original y ausencia de M1 en ese escenario. |
| TC-CU13-004 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Integración: ConfirmPaymentIntegrationTest#cancelledChargeFailsTheReservation pasó para CANCELLED de M3; unitario: ModuleOneNotificationMatrixTest#expiredAndPaymentFailedNotifyNobody pasó sin M1. Falta canal A FAILED y verificación integrada de notificaciones. |
| TC-CU13-005 | Sí | Sí | Blocked | Agente automatizado | 2026-11-13 | Integración: ConfirmPaymentIntegrationTest#duplicateApprovalDoesNotRepeatTransitions pasó para lógica interna y un evento RESERVED. Falta el canal A requerido por el caso y su respuesta HTTP. |
| TC-CU13-006 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Unitario: PollPaymentStatusServiceTest#inProcessIsSkipped pasó sin confirmar; componente: HttpPaymentStatusAdapterTest#notFoundMeansRetryNextCycle pasó con UNKNOWN. Falta observar ciclos sucesivos completos y persistencia. |
| TC-CU13-007 | Sí | No | Blocked | — | — | Sin ejecución automatizada; endpoint del canal A ausente. |

## Brechas detectadas

4. JWT de servicio, payload M3 T-12 y acuerdos de reversión requieren integración.
5. Tiempo menor a un segundo y entrega real de reversión requieren medición con servicios externos.

## Convenciones de estado

- **Draft**: contrato incompleto o inconsistente.
- **In Process**: ejecutable con implementación actual.
- **Approved**: aprobado tras revisión y ejecución; ninguno aquí.
- **Blocked**: depende de funcionalidad pendiente.

## Ejecución automatizada — 2026-11-13

`sg docker -c 'bash ./mvnw -B clean test'`: salida 1 por ErrorCodeTest#noDuplicateCodes, sin atribuir ese fallo a CU-13. Las pruebas citadas usan las dependencias indicadas en cada caso; no acreditan servicios externos reales.

| Clase | Pruebas | Fallos | Errores | Omitidos | Resultado |
|---|---:|---:|---:|---:|---|
| [ConfirmPaymentIntegrationTest](../../../../src/test/java/com/seashare/m2/reservas/integration/ConfirmPaymentIntegrationTest.java) | 6 | 0 | 0 | 0 | Verde; cobertura según TC |
| [ConfirmPaymentServiceTest](../../../../src/test/java/com/seashare/m2/reservas/application/service/ConfirmPaymentServiceTest.java) | 10 | 0 | 0 | 0 | Verde; cobertura según TC |
| [PollPaymentStatusServiceTest](../../../../src/test/java/com/seashare/m2/reservas/application/service/PollPaymentStatusServiceTest.java) | 5 | 0 | 0 | 0 | Verde; cobertura según TC |
| [HttpPaymentStatusAdapterTest](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpPaymentStatusAdapterTest.java) | 7 | 0 | 0 | 0 | Verde; cobertura según TC |
| [ModuleOneNotificationMatrixTest](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java) | 7 | 0 | 0 | 0 | Verde; cobertura según TC |
