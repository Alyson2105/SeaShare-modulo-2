# CU-03 — Pruebas backend de iniciar pago

Casos documentales para `POST /api/v1/reservations/{reservation_id}/payments`. El endpoint y la orquestación CU-03 están implementados. La UI y el cobro efectivo de M3 están fuera de alcance.

**Estado global**: Draft; TC-010 conserva una contradicción contractual sobre reintentos. Evaluado el 2026-11-13.

## Trazabilidad

| TC | Escenario | FR | SC | Test automatizado | Status |
|---|---|---|---|---|---|
| [TC-CU03-001](TC-CU03-001-inicio-pago-exitoso.md) | Inicio exitoso | FR-002, FR-003, FR-004, FR-005, FR-006 | SC-001, SC-003 | [estado](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [maquina](../../../../src/test/java/com/seashare/m2/reservas/domain/state/ReservationStateMachineTest.java) [PaymentIntegrationTest](../../../../src/test/java/com/seashare/m2/reservas/integration/PaymentIntegrationTest.java) | In Process |
| [TC-CU03-002](TC-CU03-002-fallo-calculo.md) | Fallo de cálculo externo | FR-003, FR-006 | SC-001, SC-003 | [StartPaymentServiceTest](../../../../src/test/java/com/seashare/m2/reservas/application/service/StartPaymentServiceTest.java) | In Process |
| [TC-CU03-003](TC-CU03-003-concurrencia-fcfs.md) | Contienda FCFS | FR-005, FR-008 | SC-002 | [estado](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) [PaymentIntegrationTest](../../../../src/test/java/com/seashare/m2/reservas/integration/PaymentIntegrationTest.java) | In Process |
| [TC-CU03-004](TC-CU03-004-ttl-original.md) | Conservación del TTL | FR-006, FR-007 | SC-001 | [ttl](../../../../src/test/java/com/seashare/m2/reservas/integration/TtlExpiryIntegrationTest.java); [estado](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) [PaymentIntegrationTest](../../../../src/test/java/com/seashare/m2/reservas/integration/PaymentIntegrationTest.java) | In Process |
| [TC-CU03-005](TC-CU03-005-politica-cancelacion.md) | Consentimiento obligatorio | FR-013, FR-014 | SC-004 | [PaymentIntegrationTest](../../../../src/test/java/com/seashare/m2/reservas/integration/PaymentIntegrationTest.java) | In Process |
| [TC-CU03-006](TC-CU03-006-datos-medio-pago.md) | Datos obligatorios de pago | FR-009, FR-014 | SC-004 | [PaymentIntegrationTest](../../../../src/test/java/com/seashare/m2/reservas/integration/PaymentIntegrationTest.java) | In Process |
| [TC-CU03-007](TC-CU03-007-reserva-no-admisible.md) | Estado o TTL no admisible | FR-002, FR-007 | SC-001 | [ttl](../../../../src/test/java/com/seashare/m2/reservas/integration/TtlExpiryIntegrationTest.java); [maquina](../../../../src/test/java/com/seashare/m2/reservas/domain/state/ReservationStateMachineTest.java) [StartPaymentServiceTest](../../../../src/test/java/com/seashare/m2/reservas/application/service/StartPaymentServiceTest.java) | In Process |
| [TC-CU03-008](TC-CU03-008-evento-m3-sin-notificacion-m1.md) | Evento M3 sin bloqueo M1 | FR-006, FR-009 | SC-001 | [matriz](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java); [estado](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) [PaymentIntegrationTest](../../../../src/test/java/com/seashare/m2/reservas/integration/PaymentIntegrationTest.java) | In Process |
| [TC-CU03-009](TC-CU03-009-acceso-reserva.md) | Autorización del titular | FR-001, FR-002 | SC-001 | [StartPaymentServiceTest](../../../../src/test/java/com/seashare/m2/reservas/application/service/StartPaymentServiceTest.java) | Blocked |
| [TC-CU03-010](TC-CU03-010-reintento-pago.md) | Reintento de pago rechazado | FR-002, FR-009 | SC-001, SC-003 | [StartPaymentServiceTest](../../../../src/test/java/com/seashare/m2/reservas/application/service/StartPaymentServiceTest.java) | Draft |

## Cobertura de requisitos

| FR | Cobertura | TC | Alcance | Observación |
|---|---|---|---|---|
| FR-001 | Parcial | 009 | Backend/UI | Contexto desde detalle; navegación UI fuera |
| FR-002 | Parcial | 001, 007, 009, 010 | Backend | Regla de estado y contradicción de reintento |
| FR-003 | Parcial | 001, 002 | Backend | Cálculo orquestado; proveedor real no probado |
| FR-004 | Parcial | 001 | Backend | Conservación literal de valores |
| FR-005 | Parcial | 001, 003 | Backend | Disponibilidad bajo lock |
| FR-006 | Parcial | 001, 002, 004, 008 | Backend | Transición y fallos |
| FR-007 | Parcial | 004, 007 | Backend | TTL original |
| FR-008 | Parcial | 003 | Backend | Colisión FCFS |
| FR-009 | Parcial | 006, 008, 010 | Backend | Datos y evento para cobro; reintento en Draft |
| FR-010 | Sin evidencia | — | UI | Resumen visual |
| FR-011 | Sin evidencia | — | UI | Temporizador visual |
| FR-012 | Parcial | 001 | Backend/UI | Datos backend; presentación visual fuera |
| FR-013 | Parcial | 005 | Backend/UI | Consentimiento backend; componente UI fuera |
| FR-014 | Parcial | 005, 006 | Backend/UI | Validación backend; habilitación UI fuera |
| FR-015 | Sin evidencia | — | UI | Panel y retroalimentación visual |

## Criterios de éxito

| SC | Estado | Observación |
|---|---|---|
| SC-001 | Parcialmente demostrado | Inicio y TTL de respuesta probados; falta demostrar el 100 % y persistencia completa. |
| SC-002 | Parcialmente demostrado | Conflicto secuencial probado; carrera simultánea no demostrada. |
| SC-003 | Parcialmente demostrado | Desglose simulado probado; no demuestra ausencia global de cálculos ni todos los cobros. |
| SC-004 | Parcialmente demostrado | Se probó rechazo de consentimiento falso; no se demuestran todas las transacciones ni la UI. |

## Checklist de ejecución

| TC | Documentado | Ejecutado | Resultado | Ejecutado por | Fecha | Observaciones |
|---|---|---|---|---|---|---|
| TC-CU03-001 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Integración: PaymentIntegrationTest#startPaymentTransitionsAndReturnsBreakdown pasó: 200, PENDING_PAYMENT y desglose simulado. No demuestra el proveedor real ni todos los efectos de retención. |
| TC-CU03-002 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Unitario: StartPaymentServiceTest#calculationFailureAbortsWithoutTransition pasó: CALCULATION_UNAVAILABLE y escritor no invocado. Faltan HTTP 503 y comprobación de TTL, outbox y M1 persistidos. |
| TC-CU03-003 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Integración: PaymentIntegrationTest#overlappingReservationLosesTheFcfsRace pasó: primer pago 200 y segundo 409 secuenciales. Falta carrera simultánea y verificar estado de la perdedora. |
| TC-CU03-004 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Integración: PaymentIntegrationTest#ttlIsNotRestarted pasó: vencimiento devuelto igual al original. Falta comparar vencimiento persistido y valor exacto de segundos restantes. |
| TC-CU03-005 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Integración: PaymentIntegrationTest#policyNotAcceptedIs400 pasó: 400 POLICY_NOT_ACCEPTED. Faltan demás variantes y comprobación de ausencia de transición y envío. |
| TC-CU03-006 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Integración: PaymentIntegrationTest#missingPaymentTokenIs400 pasó: 400 PAYMENT_DATA_INCOMPLETE. Faltan variantes y comprobación de estado y outbox. |
| TC-CU03-007 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Unitario: StartPaymentServiceTest#nonInitiatedReservationIsConflict y #expiredTtlIsConflict pasaron con los códigos de dominio esperados. Falta comprobar HTTP 409 y ausencia de efectos persistidos. |
| TC-CU03-008 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Integración con outbox simulado: PaymentIntegrationTest#pendingEventCarriesPaymentToken pasó para estado interno y token; unitario: ModuleOneNotificationMatrixTest#initiatedAndPendingPaymentNotifyNobody pasó sin destino M1. Falta inspección de payload y ausencia de M1 en flujo integrado. |
| TC-CU03-009 | Sí | Sí | Blocked | Agente automatizado | 2026-11-13 | Unitario: StartPaymentServiceTest#otherRenterIsForbidden pasó con RESERVATION_NOT_OWNED. PaymentIntegrationTest#ownerCannotPay verifica 403 HTTP con cabeceras temporales. Falta autenticación Bearer y no se comprueba ausencia de transición persistida. |
| TC-CU03-010 | Sí | Sí | Draft | Agente automatizado | 2026-11-13 | Contrato contradictorio: spec limita a INITIATED y plan permite reintento tras REJECTED. StartPaymentServiceTest#retryAfterRejectedReusesThePersistedCalculation pasó: intento 2 y reutilización; no resuelve el contrato ni verifica el evento. |

## Brechas detectadas

2. [Spec](../spec.md) FR-002 exige INITIATED exclusivamente; [plan](../plan.md) permite reintentos desde PENDING_PAYMENT tras REJECTED. Falta resolver el contrato y la idempotencia del intento con M3.
5. Spring Security y JWT están pendientes; no confundir la autenticación propuesta con la identidad temporal de CU-02.
6. M1 opera con adaptadores fake por defecto; las pruebas UI y los servicios externos necesitan cobertura separada.
7. El plan plantea lock de fila de reserva y GiST para la carrera; se debe comprobar la exclusividad real entre reservas distintas.
8. `plan.md` plantea consultar M1 bajo el lock; StartPaymentService#execute consulta disponibilidad antes de la transacción del escritor.

## Convenciones de estado

- **Draft**: información insuficiente o inconsistencia contractual que impide cerrar el caso.
- **In Process**: caso ejecutable contra implementación actual con contrato suficiente.
- **Approved**: escenario y postcondiciones demostrados por aserciones de la corrida V4, sin impedimentos de aprobación.
- **Blocked**: depende de una funcionalidad pendiente.

## Ejecución automatizada — 2026-11-13

`sg docker -c 'bash ./mvnw -B clean test'`; inicio 2026-11-13T19:02:00-05:00, fin 2026-11-13T19:03:11-05:00; BASELINE_HEAD `568290a0d4334b477a4b0f9d5e949e981da3fa55`; salida 0. Las integraciones utilizan dependencias simuladas; no acreditan cobro externo real.

| Clase | Pruebas | Fallos | Errores | Omitidos | Alcance |
|---|---:|---:|---:|---:|---|
| com.seashare.m2.reservas.application.service.ModuleOneNotificationMatrixTest | 7 | 0 | 0 | 0 | Evidencia parcial según TC |
| com.seashare.m2.reservas.application.service.StartPaymentServiceTest | 10 | 0 | 0 | 0 | Evidencia parcial según TC |
| com.seashare.m2.reservas.domain.state.ReservationStateMachineTest | 7 | 0 | 0 | 0 | Evidencia parcial según TC |
| com.seashare.m2.reservas.integration.ChangeReservationStatusServiceTest | 28 | 0 | 0 | 0 | Evidencia parcial según TC |
| com.seashare.m2.reservas.integration.PaymentIntegrationTest | 10 | 0 | 0 | 0 | Evidencia parcial según TC |
| com.seashare.m2.reservas.integration.TtlExpiryIntegrationTest | 8 | 0 | 0 | 0 | Evidencia parcial según TC |
