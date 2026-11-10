# CU-03 — Pruebas backend de iniciar pago

Casos documentales para el contrato propuesto `POST /api/v1/reservations/{reservation_id}/payments`. El endpoint y la orquestación CU-03 no existen aún. La UI y el cobro efectivo de M3 están fuera de alcance.

**Estado global**: Draft; CU-03 está pendiente y el contrato contiene una contradicción sobre reintentos.

## Trazabilidad

| TC | Escenario | FR | SC | Pruebas parciales de componentes | Status |
|---|---|---|---|---|---|
| [TC-CU03-001](TC-CU03-001-inicio-pago-exitoso.md) | Inicio exitoso | FR-002, FR-003, FR-004, FR-005, FR-006 | SC-001, SC-003 | [estado](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [maquina](../../../../src/test/java/com/seashare/m2/reservas/domain/state/ReservationStateMachineTest.java) | Blocked |
| [TC-CU03-002](TC-CU03-002-fallo-calculo.md) | Fallo de cálculo externo | FR-003, FR-006 | SC-001, SC-003 | Sin prueba de CU-03 | Blocked |
| [TC-CU03-003](TC-CU03-003-concurrencia-fcfs.md) | Contienda FCFS | FR-005, FR-008 | SC-002 | [estado](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | Blocked |
| [TC-CU03-004](TC-CU03-004-ttl-original.md) | Conservación del TTL | FR-006, FR-007 | SC-001 | [ttl](../../../../src/test/java/com/seashare/m2/reservas/integration/TtlExpiryIntegrationTest.java); [estado](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | Blocked |
| [TC-CU03-005](TC-CU03-005-politica-cancelacion.md) | Consentimiento obligatorio | FR-013, FR-014 | SC-004 | Sin prueba de CU-03 | Blocked |
| [TC-CU03-006](TC-CU03-006-datos-medio-pago.md) | Datos obligatorios de pago | FR-009, FR-014 | SC-004 | Sin prueba de CU-03 | Blocked |
| [TC-CU03-007](TC-CU03-007-reserva-no-admisible.md) | Estado o TTL no admisible | FR-002, FR-007 | SC-001 | [ttl](../../../../src/test/java/com/seashare/m2/reservas/integration/TtlExpiryIntegrationTest.java); [maquina](../../../../src/test/java/com/seashare/m2/reservas/domain/state/ReservationStateMachineTest.java) | Blocked |
| [TC-CU03-008](TC-CU03-008-evento-m3-sin-notificacion-m1.md) | Evento M3 sin bloqueo M1 | FR-006, FR-009 | SC-001 | [matriz](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java); [estado](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | Blocked |
| [TC-CU03-009](TC-CU03-009-acceso-reserva.md) | Autorización del titular | FR-001, FR-002 | SC-001 | Sin prueba de CU-03 | Blocked |
| [TC-CU03-010](TC-CU03-010-reintento-pago.md) | Reintento de pago rechazado | FR-002, FR-009 | SC-001, SC-003 | Sin prueba de CU-03 | Draft |

## Cobertura de requisitos

| FR | Cobertura | TC | Alcance | Observación |
|---|---|---|---|---|
| FR-001 | Bloqueado — CU-03 pendiente | 009 | Backend/UI | Contexto desde detalle; navegación UI fuera |
| FR-002 | Bloqueado — CU-03 pendiente | 001, 007, 009, 010 | Backend | Regla de estado y contradicción de reintento |
| FR-003 | Bloqueado — CU-03 pendiente | 001, 002 | Backend | Cálculo aún no orquestado |
| FR-004 | Bloqueado — CU-03 pendiente | 001 | Backend | Conservación literal de valores |
| FR-005 | Bloqueado — CU-03 pendiente | 001, 003 | Backend | Disponibilidad bajo lock |
| FR-006 | Bloqueado — CU-03 pendiente | 001, 002, 004, 008 | Backend | Transición y fallos |
| FR-007 | Bloqueado — CU-03 pendiente | 004, 007 | Backend | TTL original |
| FR-008 | Bloqueado — CU-03 pendiente | 003 | Backend | Colisión FCFS |
| FR-009 | Bloqueado — CU-03 pendiente | 006, 008, 010 | Backend | Datos y evento para cobro; reintento en Draft |
| FR-010 | Sin cobertura backend | — | UI | Resumen visual |
| FR-011 | Sin cobertura backend | — | UI | Temporizador visual |
| FR-012 | Bloqueado — CU-03 pendiente | 001 | Backend/UI | Datos backend; presentación visual fuera |
| FR-013 | Bloqueado — CU-03 pendiente | 005 | Backend/UI | Consentimiento backend; componente UI fuera |
| FR-014 | Bloqueado — CU-03 pendiente | 005, 006 | Backend/UI | Validación backend; habilitación UI fuera |
| FR-015 | Sin cobertura backend | — | UI | Panel y retroalimentación visual |

## Criterios de éxito

| SC | Estado | Observación |
|---|---|---|
| SC-001 | Bloqueado | Inicio y TTL dependen de CU-03; componentes internos no prueban el flujo completo. |
| SC-002 | Bloqueado | TC-003; FCFS aún no implementado. |
| SC-003 | Bloqueado | TC-001 y TC-002; no demuestra todos los cobros. |
| SC-004 | Bloqueado | TC-005 y TC-006; confirmación visual corresponde a UI. |

## Checklist de ejecución

| TC | Documentado | Ejecutado | Resultado | Ejecutado por | Fecha | Observaciones |
|---|---|---|---|---|---|---|
| TC-CU03-001 | Sí | No | Pendiente | — | — | Blocked |
| TC-CU03-002 | Sí | No | Pendiente | — | — | Blocked |
| TC-CU03-003 | Sí | No | Pendiente | — | — | Blocked |
| TC-CU03-004 | Sí | No | Pendiente | — | — | Blocked |
| TC-CU03-005 | Sí | No | Pendiente | — | — | Blocked |
| TC-CU03-006 | Sí | No | Pendiente | — | — | Blocked |
| TC-CU03-007 | Sí | No | Pendiente | — | — | Blocked |
| TC-CU03-008 | Sí | No | Pendiente | — | — | Blocked |
| TC-CU03-009 | Sí | No | Pendiente | — | — | Blocked |
| TC-CU03-010 | Sí | No | Pendiente | — | — | Draft |

## Brechas detectadas

1. [CU-03 no implementado](../../../context/implementContext.md): endpoint, FCFS y orquestación pendientes; existe un motor interno de CU-08, no un flujo de pagos completo.
2. [Spec](../spec.md) FR-002 exige INITIATED exclusivamente; [plan](../plan.md) permite reintentos desde PENDING_PAYMENT tras REJECTED. Falta resolver el contrato y la idempotencia del intento con M3.
3. El payload de estado actual usa PENDING_PAYMENT; traducción a PENDING y payload final de M3 pendientes de T-12, según [contexto](../../../context/implementContext.md).
4. El cliente de cálculo de CU-12 existe parcialmente, pero CU-03 aún no lo invoca; base URL de M3 pendiente.
5. Spring Security y JWT están pendientes; no confundir la autenticación propuesta con la identidad temporal de CU-02.
6. M1 opera con adaptadores fake por defecto; las pruebas UI y los servicios externos necesitan cobertura separada.
7. El plan plantea lock de fila de reserva y GiST para la carrera; se debe comprobar la exclusividad real entre reservas distintas una vez implementado.

## Convenciones de estado

- **Draft**: información insuficiente o inconsistencia contractual que impide cerrar el caso.
- **In Process**: caso ejecutable contra implementación actual con contrato suficiente.
- **Approved**: aprobación posterior a ejecución y revisión; ninguno se aprueba aquí.
- **Blocked**: depende de una funcionalidad pendiente.
