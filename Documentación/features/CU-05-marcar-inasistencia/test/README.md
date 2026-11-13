# CU-05 — Pruebas backend de marcar inasistencia

Casos para el contrato propuesto `POST /api/v1/reservations/{reservation_id}/no-show`. CU-05 no está implementado; las pruebas del motor CU-08 son evidencia parcial. No se ejecutan pruebas UI ni servicios externos.

**Estado global**: Blocked; los flujos específicos aún no están implementados. Evaluado el 2026-11-13.

## Trazabilidad

| TC | Escenario | FR | SC | Test automatizado parcial | Status |
|---|---|---|---|---|---|
| [TC-CU05-001](TC-CU05-001-reporte-limite.md) | Reporte al cumplir la tolerancia | FR-001, FR-003, FR-004, FR-007, FR-008 | SC-001, SC-004 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | Blocked |
| [TC-CU05-002](TC-CU05-002-reporte-anticipado.md) | Rechazo antes de la tolerancia | FR-003, FR-004, FR-005 | SC-001, SC-003 | Sin prueba de CU-05 | Blocked |
| [TC-CU05-003](TC-CU05-003-estado-incompatible.md) | Rechazo por estado incompatible | FR-001, FR-006 | SC-002 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | Blocked |
| [TC-CU05-004](TC-CU05-004-propietario-no-autorizado.md) | Rechazo de solicitante ajeno | FR-002 | SC-006 | Sin prueba de CU-05 | Blocked |
| [TC-CU05-005](TC-CU05-005-zona-horaria-no-disponible.md) | Fallo de zona horaria | FR-004, FR-012 | SC-001 | Sin prueba de CU-05 | Blocked |
| [TC-CU05-006](TC-CU05-006-notificaciones-y-registro.md) | Registro y notificación de inasistencia | FR-007, FR-008, FR-009, FR-010, FR-011 | SC-004, SC-005 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [matriz](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java) | Blocked |
| [TC-CU05-007](TC-CU05-007-carrera-inicio-navegacion.md) | Carrera con inicio de viaje | FR-001, FR-006, FR-007 | SC-002 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | Blocked |

## Cobertura de requisitos

| FR | Cobertura | TC | Alcance | Motivo / Observación |
|---|---|---|---|---|
| FR-001 | Bloqueado — CU-05 pendiente | 001, 003, 007 | Backend | Estado válido |
| FR-002 | Bloqueado — CU-05 pendiente | 004 | Backend | Solo propietario |
| FR-003 | Bloqueado — CU-05 pendiente | 001, 002 | Backend | Límite 30 minutos |
| FR-004 | Bloqueado — CU-05 pendiente | 001, 002, 005 | Backend | Zona horaria |
| FR-005 | Bloqueado — CU-05 pendiente | 002 | Backend | Tiempo restante |
| FR-006 | Bloqueado — CU-05 pendiente | 003, 007 | Backend | Estados incompatibles |
| FR-007 | Bloqueado — CU-05 pendiente | 001, 006, 007 | Backend | Transición |
| FR-008 | Bloqueado — CU-05 pendiente | 001, 006 | Backend | Auditoría |
| FR-009 | Bloqueado — CU-05 pendiente | 006 | Backend | Sin dinero |
| FR-010 | Bloqueado — CU-05 pendiente | 006 | Backend | Notificación M1 |
| FR-011 | Bloqueado — CU-05 pendiente | 006 | Backend | Notificación M3 |
| FR-012 | Bloqueado — CU-05 pendiente | 005 | Backend | Zona horaria no disponible |
| FR-013 | Sin cobertura backend | — | UI | Visualización e interacción requieren pruebas frontend. |
| FR-014 | Sin cobertura backend | — | UI | Visualización e interacción requieren pruebas frontend. |
| FR-015 | Sin cobertura backend | — | UI | Visualización e interacción requieren pruebas frontend. |
| FR-016 | Sin cobertura backend | — | UI | Visualización e interacción requieren pruebas frontend. |
| FR-017 | Sin cobertura backend | — | UI | Visualización e interacción requieren pruebas frontend. |
| FR-018 | Sin cobertura backend | — | UI | Visualización e interacción requieren pruebas frontend. |
| FR-019 | Sin cobertura backend | — | UI | Visualización e interacción requieren pruebas frontend. |

## Criterios de éxito

| SC | Estado | Observación |
|---|---|---|
| SC-001 | Bloqueado | TC-001, 002 y 005; tolerancia y zona. |
| SC-002 | Bloqueado | TC-003 y 007; estado y carrera. |
| SC-003 | Bloqueado | TC-002; tiempo restante. |
| SC-004 | Bloqueado | TC-001 y 006; primer intento externo requiere medición. |
| SC-005 | Bloqueado | TC-006; ausencia de cálculos requiere revisión integral. |
| SC-006 | Bloqueado | TC-004; autorización del propietario. |

## Checklist de ejecución

| TC | Documentado | Ejecutado | Resultado | Ejecutado por | Fecha | Observaciones |
|---|---|---|---|---|---|---|
| TC-CU05-001 | Sí | No | Blocked | Agente automatizado | — | Sin ejecución automatizada del escenario; falta implementación específica. |
| TC-CU05-002 | Sí | No | Blocked | Agente automatizado | — | Sin ejecución automatizada del escenario; falta implementación específica. |
| TC-CU05-003 | Sí | No | Blocked | Agente automatizado | — | Sin ejecución automatizada del escenario; falta implementación específica. |
| TC-CU05-004 | Sí | No | Blocked | Agente automatizado | — | Sin ejecución automatizada del escenario; falta implementación específica. |
| TC-CU05-005 | Sí | No | Blocked | Agente automatizado | — | Sin ejecución automatizada del escenario; falta implementación específica. |
| TC-CU05-006 | Sí | Sí | Blocked | Agente automatizado | 2026-11-13 | matriz unitaria decide AVAILABLE para cancelación tras RESERVED; falta reporte NO_SHOW, registro y entrega. |
| TC-CU05-007 | Sí | No | Blocked | Agente automatizado | — | Sin ejecución automatizada del escenario; falta implementación específica. |

## Brechas detectadas

1. [CU-05 no está implementado](../../../context/implementContext.md): faltan endpoint y validación de 30 minutos.
2. El motor de CU-08 no demuestra la orquestación ni el cálculo temporal propios de CU-05.
3. La entrega externa y el primer intento en menos de un segundo requieren servicios y medición; el encolado no los demuestra.
4. El payload de M3 está pendiente de T-12; flota usa adaptadores fake por defecto.
5. JWT está pendiente; autorización y pertenencia no son verificables en el endpoint inexistente.
6. FR-013 a FR-019 requieren pruebas de frontend o integración UI.

## Convenciones de estado

- **Draft**: contrato incompleto o inconsistente.
- **In Process**: ejecutable con implementación actual.
- **Approved**: aprobado tras revisión y ejecución; ninguno aquí.
- **Blocked**: depende de funcionalidad pendiente.

## Ejecución automatizada — 2026-11-13

`./mvnw -B clean test`: 395 pruebas, 0 fallos, 0 errores, 0 omitidas; BUILD SUCCESS. Las clases siguientes prueban componentes compartidos de CU-08; no implementan el flujo específico de CU-05.

| Clase | Pruebas | Fallos | Errores | Omitidos | Resultado |
|---|---:|---:|---:|---:|---|
| [ChangeReservationStatusServiceTest](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | 28 | 0 | 0 | 0 | Exitosa |
| [ModuleOneNotificationMatrixTest](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java) | 7 | 0 | 0 | 0 | Exitosa |
