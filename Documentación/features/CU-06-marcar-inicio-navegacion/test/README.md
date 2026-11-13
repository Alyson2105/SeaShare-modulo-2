# CU-06 — Pruebas backend de inicio de navegación

Casos para el contrato propuesto `POST /api/v1/reservations/{reservation_id}/navigation-start`. CU-06 no está implementado; CU-08 ofrece componentes internos. UI y servicios externos requieren pruebas independientes.

**Estado global**: Draft; un caso conserva contradicción contractual y los demás flujos no están implementados. Evaluado el 2026-11-13.

## Trazabilidad

| TC | Escenario | FR | SC | Test automatizado parcial | Status |
|---|---|---|---|---|---|
| [TC-CU06-001](TC-CU06-001-inicio-puntual.md) | Inicio de navegación | FR-001, FR-003, FR-005, FR-006 | SC-002 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | Blocked |
| [TC-CU06-002](TC-CU06-002-inicio-anticipado.md) | Rechazo de salida anticipada | FR-003 | SC-002 | Sin prueba de CU-06 | Draft |
| [TC-CU06-003](TC-CU06-003-estado-incompatible.md) | Estado no admisible | FR-001, FR-004 | SC-001 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | Blocked |
| [TC-CU06-004](TC-CU06-004-propietario-no-autorizado.md) | Autorización del propietario | FR-002 | SC-006 | Sin prueba de CU-06 | Blocked |
| [TC-CU06-005](TC-CU06-005-travesia-anterior.md) | Travesía anterior sin cerrar | FR-001, FR-004 | SC-001 | Sin prueba de CU-06 | Blocked |
| [TC-CU06-006](TC-CU06-006-notificaciones-checkin.md) | Notificaciones y registro | FR-005, FR-006, FR-007, FR-008, FR-010 | SC-002, SC-004, SC-005 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [matriz](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java) | Blocked |
| [TC-CU06-007](TC-CU06-007-bloqueo-cancelacion-inasistencia.md) | Inadmisibilidad después del inicio | FR-009 | SC-003 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | Blocked |
| [TC-CU06-008](TC-CU06-008-carrera-inasistencia.md) | Carrera con inasistencia | FR-001, FR-005, FR-009 | SC-001, SC-003 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | Blocked |

## Cobertura de requisitos

| FR | Cobertura | TC | Alcance | Motivo / Observación |
|---|---|---|---|---|
| FR-001 | Bloqueado — CU-06 pendiente | 001, 003, 005, 008 | Backend | Estado válido y carrera |
| FR-002 | Bloqueado — CU-06 pendiente | 004 | Backend | Solo propietario |
| FR-003 | Bloqueado — CU-06 pendiente | 001, 002 | Backend | Ventana de zarpe |
| FR-004 | Bloqueado — CU-06 pendiente | 003, 005 | Backend | Estados incompatibles y travesía activa |
| FR-005 | Bloqueado — CU-06 pendiente | 001, 006, 008 | Backend | Transición |
| FR-006 | Bloqueado — CU-06 pendiente | 001, 006 | Backend | Registro de check-in |
| FR-007 | Bloqueado — CU-06 pendiente | 006 | Backend | Notificación M1 |
| FR-008 | Bloqueado — CU-06 pendiente | 006 | Backend | Notificación M3 |
| FR-009 | Bloqueado — CU-06 pendiente | 007, 008 | Backend | Cancelación e inasistencia |
| FR-010 | Bloqueado — CU-06 pendiente | 006 | Backend | Sin dinero |
| FR-011 | Sin cobertura backend | — | UI | Visualización e interacción requieren pruebas frontend. |
| FR-012 | Sin cobertura backend | — | UI | Visualización e interacción requieren pruebas frontend. |
| FR-013 | Sin cobertura backend | — | UI | Visualización e interacción requieren pruebas frontend. |
| FR-014 | Sin cobertura backend | — | UI | Visualización e interacción requieren pruebas frontend. |
| FR-015 | Sin cobertura backend | — | UI | Visualización e interacción requieren pruebas frontend. |

## Criterios de éxito

| SC | Estado | Observación |
|---|---|---|
| SC-001 | Bloqueado | TC-003, 005 y 008. |
| SC-002 | Bloqueado | TC-001, 002 y 006; primer intento M1 requiere medición. |
| SC-003 | Bloqueado | TC-007 y 008; depende también de CU-04 y CU-05. |
| SC-004 | Bloqueado | TC-006; entrega M3 no está probada. |
| SC-005 | Bloqueado | TC-006; ausencia de cálculos requiere revisión integral. |
| SC-006 | Bloqueado | TC-004; autorización. |

## Checklist de ejecución

| TC | Documentado | Ejecutado | Resultado | Ejecutado por | Fecha | Observaciones |
|---|---|---|---|---|---|---|
| TC-CU06-001 | Sí | Sí | Blocked | Agente automatizado | 2026-11-13 | integración del motor transiciona a IN_NAVIGATION y encola destinos simulados; faltan endpoint, hora real y registro. |
| TC-CU06-002 | Sí | No | Draft | Agente automatizado | — | Sin ejecución automatizada del escenario; contrato contradictorio. |
| TC-CU06-003 | Sí | No | Blocked | Agente automatizado | — | Sin ejecución automatizada del escenario; falta implementación específica. |
| TC-CU06-004 | Sí | No | Blocked | Agente automatizado | — | Sin ejecución automatizada del escenario; falta implementación específica. |
| TC-CU06-005 | Sí | No | Blocked | Agente automatizado | — | Sin ejecución automatizada del escenario; falta implementación específica. |
| TC-CU06-006 | Sí | Sí | Blocked | Agente automatizado | 2026-11-13 | matriz unitaria decide IN_NAVIGATION para M1; faltan endpoint, registro y entrega; motor encola AMQP y M1_HTTP simulados. |
| TC-CU06-007 | Sí | No | Blocked | Agente automatizado | — | Sin ejecución automatizada del escenario; falta implementación específica. |
| TC-CU06-008 | Sí | No | Blocked | Agente automatizado | — | Sin ejecución automatizada del escenario; falta implementación específica. |

## Brechas detectadas

1. [CU-06 no está implementado](../../../context/implementContext.md): faltan endpoint y check-in; CU-08 solo ofrece transición interna.
2. El edge case del [spec](../spec.md) menciona salida ligeramente anterior, mientras FR-003 y el [plan](../plan.md) la prohíben; confirmar contrato.
3. El plan declara `insurance_activated: true` pese a notificación asíncrona; emisión no prueba activación del seguro por M3.
4. El primer intento a M1 en menos de un segundo requiere medición con servicios; el encolado no lo demuestra.
5. JWT está pendiente y flota usa adaptadores fake por defecto; payload M3 T-12 pendiente.
6. FR-011 a FR-015 requieren pruebas de frontend o integración UI.

## Convenciones de estado

- **Draft**: contrato incompleto o inconsistente.
- **In Process**: ejecutable con implementación actual.
- **Approved**: aprobado tras revisión y ejecución; ninguno aquí.
- **Blocked**: depende de funcionalidad pendiente.

## Ejecución automatizada — 2026-11-13

`./mvnw -B clean test`: 395 pruebas, 0 fallos, 0 errores, 0 omitidas; BUILD SUCCESS. Las clases siguientes prueban componentes compartidos de CU-08; no implementan el flujo específico de CU-06.

| Clase | Pruebas | Fallos | Errores | Omitidos | Resultado |
|---|---:|---:|---:|---:|---|
| [ChangeReservationStatusServiceTest](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | 28 | 0 | 0 | 0 | Exitosa |
| [ModuleOneNotificationMatrixTest](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java) | 7 | 0 | 0 | 0 | Exitosa |
