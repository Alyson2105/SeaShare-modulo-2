# CU-07 — Pruebas backend de fin de navegación

Casos para el contrato propuesto `POST /api/v1/reservations/{reservation_id}/navigation-end`. CU-07 no está implementado; CU-08 ofrece componentes internos. UI y servicios externos requieren pruebas independientes.

**Estado global**: Draft por implementación y contratos pendientes.

## Trazabilidad

| TC | Escenario | FR | SC | Test automatizado parcial | Status |
|---|---|---|---|---|---|
| [TC-CU07-001](TC-CU07-001-cierre-exitoso.md) | Cierre de navegación | FR-001, FR-003, FR-004, FR-005 | SC-002, SC-004 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | Blocked |
| [TC-CU07-002](TC-CU07-002-estado-incompatible.md) | Estado no admisible | FR-001, FR-009 | SC-001 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | Blocked |
| [TC-CU07-003](TC-CU07-003-propietario-no-autorizado.md) | Autorización del propietario | FR-002 | SC-005 | Sin prueba de CU-07 | Blocked |
| [TC-CU07-004](TC-CU07-004-notificaciones-y-novedades.md) | Notificación de cierre | FR-003, FR-006, FR-007, FR-008 | SC-002, SC-003 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [matriz](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java) | Blocked |
| [TC-CU07-005](TC-CU07-005-disputa-garantia.md) | Apertura de disputa | FR-004, FR-010 | SC-002, SC-004 | Sin prueba de CU-07 | Blocked |
| [TC-CU07-006](TC-CU07-006-doble-intento.md) | Duplicación de cierre | FR-001, FR-004, FR-005 | SC-001, SC-004 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | Draft |

## Cobertura de requisitos

| FR | Cobertura | TC | Alcance | Motivo / Observación |
|---|---|---|---|---|
| FR-001 | Bloqueado — CU-07 pendiente | 001, 002, 006 | Backend | Estado admisible |
| FR-002 | Bloqueado — CU-07 pendiente | 003 | Backend | Solo propietario |
| FR-003 | Bloqueado — CU-07 pendiente | 001, 004 | Backend | Novedades |
| FR-004 | Bloqueado — CU-07 pendiente | 001, 005, 006 | Backend | Transición y disputa |
| FR-005 | Bloqueado — CU-07 pendiente | 001, 006 | Backend | Registro de entrega |
| FR-006 | Bloqueado — CU-07 pendiente | 004 | Backend | Notificación M1 |
| FR-007 | Bloqueado — CU-07 pendiente | 004 | Backend | Notificación M3 |
| FR-008 | Bloqueado — CU-07 pendiente | 004 | Backend | Sin dinero |
| FR-009 | Bloqueado — CU-07 pendiente | 002 | Backend | Estados incompatibles |
| FR-010 | Bloqueado — CU-07 pendiente | 005 | Backend | Disputa de 24 horas |
| FR-011 | Sin cobertura backend | — | UI | Visualización e interacción requieren pruebas frontend. |
| FR-012 | Sin cobertura backend | — | UI | Visualización e interacción requieren pruebas frontend. |
| FR-013 | Sin cobertura backend | — | UI | Visualización e interacción requieren pruebas frontend. |

## Criterios de éxito

| SC | Estado | Observación |
|---|---|---|
| SC-001 | Bloqueado | TC-002 y 006; estado y reintentos. |
| SC-002 | Bloqueado | TC-001, 004 y 005; primer intento M1 requiere medición. |
| SC-003 | Bloqueado | TC-004; ausencia de cálculos requiere revisión integral. |
| SC-004 | Bloqueado | TC-001, 005 y 006; hora real. |
| SC-005 | Bloqueado | TC-003; autorización. |

## Checklist de ejecución

| TC | Documentado | Ejecutado | Resultado | Ejecutado por | Fecha | Observaciones |
|---|---|---|---|---|---|---|
| TC-CU07-001 | Sí | No | Pendiente | — | — | Blocked |
| TC-CU07-002 | Sí | No | Pendiente | — | — | Blocked |
| TC-CU07-003 | Sí | No | Pendiente | — | — | Blocked |
| TC-CU07-004 | Sí | No | Pendiente | — | — | Blocked |
| TC-CU07-005 | Sí | No | Pendiente | — | — | Blocked |
| TC-CU07-006 | Sí | No | Pendiente | — | — | Draft |

## Brechas detectadas

1. [CU-07 no está implementado](../../../context/implementContext.md): faltan endpoint y check-out; CU-08 solo ofrece transición interna.
2. CU-16 y la disputa de 24 horas están pendientes; la respuesta de CU-07 depende de ellos.
3. La entrega a M1, el primer intento en menos de un segundo y la notificación a M3 requieren servicios y medición; encolar no demuestra entrega ni pago.
4. JWT está pendiente y flota usa adaptadores fake por defecto; payload M3 T-12 pendiente.
5. El edge case de reintentos no define la respuesta correcta y el plan prevé 409 para cierre duplicado.
6. FR-011 a FR-013 requieren pruebas de frontend o integración UI.

## Convenciones de estado

- **Draft**: contrato incompleto o inconsistente.
- **In Process**: ejecutable con implementación actual.
- **Approved**: aprobado tras revisión y ejecución; ninguno aquí.
- **Blocked**: depende de funcionalidad pendiente.

## Ejecución automatizada — 2026-11-10

Se ejecutó `mvn clean test` en una copia aislada del commit `3e4e6a4`, con JDK 25 y compilación configurada para Java 21. El conjunto completo registró **234 pruebas: 132 exitosas, 0 fallos de aserción y 102 errores** al iniciar contextos que requieren Docker/Testcontainers. Maven terminó con `BUILD FAILURE`. Las pruebas con WireMock usaron dobles locales, no los servicios reales.

Los resultados siguientes corresponden a clases automatizadas vinculadas a este CU, no a la ejecución íntegra de cada caso documental. Los pasos y el checklist de cada TC permanecen pendientes cuando requieren acciones o comprobaciones adicionales; ningún caso se aprueba por esta ejecución.

| Clase automatizada | Pruebas | Fallos | Errores | Resultado |
|---|---:|---:|---:|---|
| [ChangeReservationStatusServiceTest](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | 25 | 0 | 25 | Bloqueada por Docker/Testcontainers |
| [ModuleOneNotificationMatrixTest](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java) | 7 | 0 | 0 | Exitosa |
