# CU-04 — Pruebas backend de solicitar cancelación

Documentación del contrato propuesto `POST /api/v1/reservations/{reservation_id}/cancellation`. CU-04 cuenta con controlador y servicio; CU-08 ejecuta la transición. La UI y las entregas reales a M1/M3 se validan por separado.

**Estado global**: Draft; la idempotencia presenta disposiciones incompatibles en el plan y la cobertura es parcial. Evaluado el 2026-11-13.

## Trazabilidad

| TC | Escenario | FR | SC | Prueba automatizada parcial | Status |
|---|---|---|---|---|---|
| [TC-CU04-001](TC-CU04-001-clasificacion-arrendatario.md) | Clasificación por anticipación | FR-005, FR-006, FR-007, FR-008, FR-011 | SC-002 | CancellationClassifierTest#exactly72HoursIsFlexible (parcial); [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | In Process |
| [TC-CU04-002](TC-CU04-002-cancelacion-propietario.md) | Clasificación por propietario | FR-009, FR-010, FR-011 | SC-003 | RequestCancellationServiceTest#ownerWithLogisticsForceMajeureAndM1BlockedNotifiesAvailable (parcial); [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | In Process |
| [TC-CU04-003](TC-CU04-003-estado-no-cancelable.md) | Rechazo por estado | FR-002, FR-004 | SC-001 | RequestCancellationServiceTest#reservationNotReservedIsRejected (parcial); [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | In Process |
| [TC-CU04-004](TC-CU04-004-tercero-no-autorizado.md) | Autorización de actor | FR-003 | SC-008 | RequestCancellationServiceTest#foreignRenterIsRejected (parcial) | In Process |
| [TC-CU04-005](TC-CU04-005-zona-horaria-no-disponible.md) | Fallo de zona horaria | FR-005, FR-006 | SC-004 | RequestCancellationServiceTest#nullHarborTimezoneIsUnavailable (parcial) | In Process |
| [TC-CU04-006](TC-CU04-006-notificacion-flota.md) | Destino operativo M1 | FR-010, FR-011, FR-012 | SC-005 | RequestCancellationServiceTest#ownerWithMechanicalFailureAndM1BlockedNotifiesMaintenance (parcial); [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java), [matriz](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java) | In Process |
| [TC-CU04-007](TC-CU04-007-evento-auditoria-m3.md) | Auditoría y notificación M3 | FR-013, FR-014, FR-015 | SC-006, SC-007 | RequestCancellationServiceTest#renterWith72HoursCancelsFlexible (parcial); [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java), [matriz](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java) | In Process |
| [TC-CU04-008](TC-CU04-008-cancelacion-concurrente.md) | Carrera de cancelación | FR-002, FR-011 | SC-001 | Sin prueba de carrera; [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | In Process |
| [TC-CU04-009](TC-CU04-009-idempotencia-solicitud.md) | Repetición idempotente | FR-011, FR-015 | SC-001 | Sin prueba de repetición | Draft |

## Cobertura de requisitos

| FR | Cobertura | TC | Alcance | Motivo / Observación |
|---|---|---|---|---|
| FR-001 | Sin cobertura backend | — | UI / integración | Navegación desde detalle requiere UI |
| FR-002 | Parcial | 003, 008 | Backend | Evidencia parcial en tests de clasificador, servicio o motor; ver TC. |
| FR-003 | Parcial | 004 | Backend | Evidencia parcial en tests de clasificador, servicio o motor; ver TC. |
| FR-004 | Parcial | 003 | Backend | Evidencia parcial en tests de clasificador, servicio o motor; ver TC. |
| FR-005 | Parcial | 001, 005 | Backend | Evidencia parcial en tests de clasificador, servicio o motor; ver TC. |
| FR-006 | Parcial | 001, 005 | Backend | Evidencia parcial en tests de clasificador, servicio o motor; ver TC. |
| FR-007 | Parcial | 001 | Backend | Evidencia parcial en tests de clasificador, servicio o motor; ver TC. |
| FR-008 | Parcial | 001 | Backend | Evidencia parcial en tests de clasificador, servicio o motor; ver TC. |
| FR-009 | Parcial | 002 | Backend | Evidencia parcial en tests de clasificador, servicio o motor; ver TC. |
| FR-010 | Parcial | 002, 006 | Backend/UI | Evidencia parcial en tests de clasificador, servicio o motor; ver TC. |
| FR-011 | Parcial | 001, 002, 008, 009 | Backend | Evidencia parcial en tests de clasificador, servicio o motor; ver TC. |
| FR-012 | Parcial | 006 | Backend | Evidencia parcial en tests de clasificador, servicio o motor; ver TC. |
| FR-013 | Parcial | 007 | Backend | Evidencia parcial en tests de clasificador, servicio o motor; ver TC. |
| FR-014 | Sin evidencia | 007 | Backend | No se ha probado ausencia integral de operaciones financieras. |
| FR-015 | Parcial | 007, 009 | Backend | Evidencia parcial en tests de clasificador, servicio o motor; ver TC. |
| FR-016 | Sin cobertura backend | — | UI | Modal del arrendatario |
| FR-017 | Sin cobertura backend | — | UI | Modal del propietario |

## Criterios de éxito

| SC | Estado | Observación |
|---|---|---|
| SC-001 | Parcialmente demostrado | Evidencia parcial; ver trazabilidad y límites de los TC. |
| SC-002 | Parcialmente demostrado | Evidencia parcial; ver trazabilidad y límites de los TC. |
| SC-003 | Parcialmente demostrado | Evidencia parcial; ver trazabilidad y límites de los TC. |
| SC-004 | Parcialmente demostrado | Evidencia parcial; ver trazabilidad y límites de los TC. |
| SC-005 | Parcialmente demostrado | Evidencia parcial; ver trazabilidad y límites de los TC. |
| SC-006 | Parcialmente demostrado | Evidencia parcial; ver trazabilidad y límites de los TC. |
| SC-007 | No demostrado | Neutralidad financiera requiere verificación integral. |
| SC-008 | Parcialmente demostrado | Evidencia parcial; ver trazabilidad y límites de los TC. |

## Checklist de ejecución

| TC | Documentado | Ejecutado | Resultado | Ejecutado por | Fecha | Observaciones |
|---|---|---|---|---|---|---|
| TC-CU04-001 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | CancellationClassifierTest#exactly72HoursIsFlexible (parcial). |
| TC-CU04-002 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | RequestCancellationServiceTest#ownerWithLogisticsForceMajeureAndM1BlockedNotifiesAvailable (parcial). |
| TC-CU04-003 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | RequestCancellationServiceTest#reservationNotReservedIsRejected (parcial). |
| TC-CU04-004 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | RequestCancellationServiceTest#foreignRenterIsRejected (parcial). |
| TC-CU04-005 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | RequestCancellationServiceTest#nullHarborTimezoneIsUnavailable (parcial). |
| TC-CU04-006 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | RequestCancellationServiceTest#ownerWithMechanicalFailureAndM1BlockedNotifiesMaintenance (parcial). |
| TC-CU04-007 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | RequestCancellationServiceTest#renterWith72HoursCancelsFlexible (parcial). |
| TC-CU04-008 | Sí | No | Pendiente | — | — | Sin prueba de carrera. |
| TC-CU04-009 | Sí | No | Draft | — | — | Plan sección 7.5 exige idempotencia y DIFERENCIA H8-CU04-05 la deja fuera; sin prueba pertinente. |

## Brechas detectadas

1. [Contexto](../../../context/implementContext.md): existen endpoint y clasificación; faltan pruebas de flujo completo y conexiones externas reales.
2. El plan usa dos vocabularios para `reason` entre la tabla de petición y el ejemplo de error; requiere aclaración.
3. La respuesta del plan afirma `PUBLISHED`, pero el relay y confirmación efectiva requieren verificación; T-12 deja pendiente el payload de M3.
4. El primer intento a M1 en menos de un segundo y la confirmación de M3 requieren servicios y medición; no se infieren del encolado.
5. Spring Security/JWT no están implementados; M1 opera con adaptadores fake por defecto.
6. La idempotencia de CU-02 no demuestra la clave de cancelación: CancellationController#cancel no procesa X-Idempotency-Key; plan sección 7.5 la exige y DIFERENCIA H8-CU04-05 la excluye de la primera versión.
7. El modal, navegación y controles visuales necesitan pruebas frontend.
8. El spec limita la cancelación del propietario al periodo previo al zarpe; RequestCancellationService#execute no aplica ese límite y RequestCancellationServiceTest#ownerCanCancelAfterDeparture pasa con mocks.
9. FR-005 exige consultar CU-09; RequestCancellationService#resolveZone usa la zona guardada en la reserva.

## Convenciones de estado

- **Draft**: contrato incompleto o inconsistente.
- **In Process**: contrato suficiente para ejecución contra implementación actual.
- **Approved**: escenario y postcondiciones demostrados por aserciones de la corrida V4, sin impedimentos de aprobación.
- **Blocked**: depende de funcionalidad pendiente.

## Ejecución automatizada — 2026-11-13

`sg docker -c 'bash ./mvnw -B clean test'`; inicio 2026-11-13T19:02:00-05:00, fin 2026-11-13T19:03:11-05:00; BASELINE_HEAD `568290a0d4334b477a4b0f9d5e949e981da3fa55`; salida 0. Las pruebas unitarias usan dependencias simuladas y no acreditan entrega real.

| Clase | Pruebas | Fallos | Errores | Omitidos | Resultado |
|---|---:|---:|---:|---:|---|
| com.seashare.m2.reservas.application.service.ModuleOneNotificationMatrixTest | 7 | 0 | 0 | 0 | Evidencia parcial según TC |
| com.seashare.m2.reservas.application.service.RequestCancellationServiceTest | 20 | 0 | 0 | 0 | Evidencia parcial según TC |
| com.seashare.m2.reservas.domain.service.CancellationClassifierTest | 8 | 0 | 0 | 0 | Evidencia parcial según TC |
| com.seashare.m2.reservas.infrastructure.adapter.in.web.CancellationControllerTest | 9 | 0 | 0 | 0 | Evidencia parcial según TC |
| com.seashare.m2.reservas.integration.ChangeReservationStatusServiceTest | 28 | 0 | 0 | 0 | Evidencia parcial según TC |
| com.seashare.m2.reservas.integration.NavigationStartIntegrationTest | 10 | 0 | 0 | 0 | Evidencia parcial según TC |
