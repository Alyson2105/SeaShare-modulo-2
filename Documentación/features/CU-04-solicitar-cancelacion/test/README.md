# CU-04 — Pruebas backend de solicitar cancelación

Documentación del contrato propuesto `POST /api/v1/reservations/{reservation_id}/cancellation`. CU-04 no está implementado; CU-08 aporta componentes internos parciales. La UI y las entregas reales a M1/M3 se validan por separado.

**Estado global**: Blocked; los flujos específicos aún no están implementados. Evaluado el 2026-11-13.

## Trazabilidad

| TC | Escenario | FR | SC | Prueba automatizada parcial | Status |
|---|---|---|---|---|---|
| [TC-CU04-001](TC-CU04-001-clasificacion-arrendatario.md) | Clasificación por anticipación | FR-005, FR-006, FR-007, FR-008, FR-011 | SC-002 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | Blocked |
| [TC-CU04-002](TC-CU04-002-cancelacion-propietario.md) | Clasificación por propietario | FR-009, FR-010, FR-011 | SC-003 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | Blocked |
| [TC-CU04-003](TC-CU04-003-estado-no-cancelable.md) | Rechazo por estado | FR-002, FR-004 | SC-001 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | Blocked |
| [TC-CU04-004](TC-CU04-004-tercero-no-autorizado.md) | Autorización de actor | FR-003 | SC-008 | Sin prueba de CU-04 | Blocked |
| [TC-CU04-005](TC-CU04-005-zona-horaria-no-disponible.md) | Fallo de zona horaria | FR-005, FR-006 | SC-004 | Sin prueba de CU-04 | Blocked |
| [TC-CU04-006](TC-CU04-006-notificacion-flota.md) | Destino operativo M1 | FR-010, FR-011, FR-012 | SC-005 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [matriz](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java) | Blocked |
| [TC-CU04-007](TC-CU04-007-evento-auditoria-m3.md) | Auditoría y notificación M3 | FR-013, FR-014, FR-015 | SC-006, SC-007 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [matriz](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java) | Blocked |
| [TC-CU04-008](TC-CU04-008-cancelacion-concurrente.md) | Carrera de cancelación | FR-002, FR-011 | SC-001 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | Blocked |
| [TC-CU04-009](TC-CU04-009-idempotencia-solicitud.md) | Repetición idempotente | FR-011, FR-015 | SC-001 | Sin prueba de CU-04 | Blocked |

## Cobertura de requisitos

| FR | Cobertura | TC | Alcance | Motivo / Observación |
|---|---|---|---|---|
| FR-001 | Sin cobertura backend | — | UI / integración | Navegación desde detalle requiere UI |
| FR-002 | Bloqueado — CU-04 pendiente | 003, 008 | Backend | Estados admisibles y concurrencia |
| FR-003 | Bloqueado — CU-04 pendiente | 004 | Backend | Autorización del titular o propietario |
| FR-004 | Bloqueado — CU-04 pendiente | 003 | Backend | Estados no cancelables |
| FR-005 | Bloqueado — CU-04 pendiente | 001, 005 | Backend | Zona de puerto y falla |
| FR-006 | Bloqueado — CU-04 pendiente | 001, 005 | Backend | Anticipación y datos |
| FR-007 | Bloqueado — CU-04 pendiente | 001 | Backend | Límites 72 y 24 h |
| FR-008 | Bloqueado — CU-04 pendiente | 001 | Backend | Último minuto previo al zarpe |
| FR-009 | Bloqueado — CU-04 pendiente | 002 | Backend | Clasificación propietario |
| FR-010 | Bloqueado — CU-04 pendiente | 002, 006 | Backend/UI | Motivo; controles visuales fuera |
| FR-011 | Bloqueado — CU-04 pendiente | 001, 002, 008, 009 | Backend | Transición y repetición |
| FR-012 | Bloqueado — CU-04 pendiente | 006 | Backend | Notificación operativa |
| FR-013 | Bloqueado — CU-04 pendiente | 007 | Backend | Notificación M3 |
| FR-014 | Bloqueado — CU-04 pendiente | 007 | Backend | Sin cálculos financieros |
| FR-015 | Bloqueado — CU-04 pendiente | 007, 009 | Backend | Auditoría e idempotencia |
| FR-016 | Sin cobertura backend | — | UI | Modal del arrendatario |
| FR-017 | Sin cobertura backend | — | UI | Modal del propietario |

## Criterios de éxito

| SC | Estado | Observación |
|---|---|---|
| SC-001 | Bloqueado | TC-003, 008 y 009; estados y concurrencia. |
| SC-002 | Bloqueado | TC-001; umbrales y zona del puerto. |
| SC-003 | Bloqueado | TC-002; clasificación de propietario. |
| SC-004 | Bloqueado | TC-005 y TC-004; datos e identidad. |
| SC-005 | Bloqueado | TC-006; el plazo del primer intento requiere medición real. |
| SC-006 | Bloqueado | TC-007; entrega y confirmación externas requieren integración. |
| SC-007 | Bloqueado | TC-007; ausencia de cálculos financieros requiere verificación integral. |
| SC-008 | Bloqueado | TC-004; autorización. |

## Checklist de ejecución

| TC | Documentado | Ejecutado | Resultado | Ejecutado por | Fecha | Observaciones |
|---|---|---|---|---|---|---|
| TC-CU04-001 | Sí | Sí | Blocked | Agente automatizado | 2026-11-13 | integración del motor acepta FLEXIBLE y anticipación suministrados; no calcula umbrales. |
| TC-CU04-002 | Sí | Sí | Blocked | Agente automatizado | 2026-11-13 | integración del motor encola mantenimiento para BY_OWNER con avería; falta endpoint y respuesta. |
| TC-CU04-003 | Sí | Sí | Blocked | Agente automatizado | 2026-11-13 | integración del motor rechaza cancelar desde PENDING_PAYMENT; faltan demás estados y HTTP. |
| TC-CU04-004 | Sí | No | Blocked | Agente automatizado | — | Sin ejecución automatizada del escenario; falta implementación específica. |
| TC-CU04-005 | Sí | No | Blocked | Agente automatizado | — | Sin ejecución automatizada del escenario; falta implementación específica. |
| TC-CU04-006 | Sí | Sí | Blocked | Agente automatizado | 2026-11-13 | matriz unitaria decide AVAILABLE; matriz unitaria decide MAINTENANCE_CLEANING; falta flujo de cancelación y entrega. |
| TC-CU04-007 | Sí | Sí | Blocked | Agente automatizado | 2026-11-13 | integración del motor verifica subtipo y actor en evento simulado; falta flujo de cancelación y auditoría completa. |
| TC-CU04-008 | Sí | No | Blocked | Agente automatizado | — | Sin ejecución automatizada del escenario; falta implementación específica. |
| TC-CU04-009 | Sí | No | Blocked | Agente automatizado | — | Sin ejecución automatizada del escenario; falta implementación específica. |

## Brechas detectadas

1. [CU-04 no está implementado](../../../context/implementContext.md): falta endpoint y clasificación. El motor CU-08 solo cubre transiciones internas.
2. El plan usa dos vocabularios para `reason` entre la tabla de petición y el ejemplo de error; requiere aclaración.
3. La respuesta del plan afirma `PUBLISHED`, pero el relay y confirmación efectiva requieren verificación; T-12 deja pendiente el payload de M3.
4. El primer intento a M1 en menos de un segundo y la confirmación de M3 requieren servicios y medición; no se infieren del encolado.
5. Spring Security/JWT no están implementados; M1 opera con adaptadores fake por defecto.
6. La idempotencia de CU-02 no demuestra la clave opcional de cancelación de CU-04.
7. El modal, navegación y controles visuales necesitan pruebas frontend.

## Convenciones de estado

- **Draft**: contrato incompleto o inconsistente.
- **In Process**: contrato suficiente para ejecución contra implementación actual.
- **Approved**: aprobado después del proceso correspondiente; ninguno en esta tarea.
- **Blocked**: depende de funcionalidad pendiente.

## Ejecución automatizada — 2026-11-13

`./mvnw -B clean test`: 395 pruebas, 0 fallos, 0 errores, 0 omitidas; BUILD SUCCESS. Las clases siguientes prueban componentes compartidos de CU-08; no implementan el flujo específico de CU-04.

| Clase | Pruebas | Fallos | Errores | Omitidos | Resultado |
|---|---:|---:|---:|---:|---|
| [ChangeReservationStatusServiceTest](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | 28 | 0 | 0 | 0 | Exitosa |
| [ModuleOneNotificationMatrixTest](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java) | 7 | 0 | 0 | 0 | Exitosa |
