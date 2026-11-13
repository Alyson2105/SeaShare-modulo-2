# CU-17 — Pruebas backend de estado de disputa

Casos de resolución administrativa y transición interna de disputas. El contexto de disputa no está implementado y es distinto del estado de reserva.

**Estado global**: Draft por implementación y contratos pendientes.

## Trazabilidad

| TC | Escenario | FR | SC | Test automatizado parcial | Status |
|---|---|---|---|---|---|
| [TC-CU17-001](TC-CU17-001-resolucion-admin.md) | Resolución administrativa | FR-001, FR-002, FR-004, FR-005 | SC-001, SC-003, SC-004 | Sin prueba de CU-17 | Blocked |
| [TC-CU17-002](TC-CU17-002-revision-pendiente.md) | Revisión en curso | FR-001, FR-002, FR-007 | SC-001 | Sin prueba de CU-17 | Blocked |
| [TC-CU17-003](TC-CU17-003-estado-terminal.md) | Inmutabilidad terminal | FR-003 | SC-002 | Sin prueba de CU-17 | Blocked |
| [TC-CU17-004](TC-CU17-004-cierre-automatico.md) | Cierre automático | FR-002, FR-006 | SC-001, SC-002 | Sin prueba de CU-17 | Blocked |
| [TC-CU17-005](TC-CU17-005-carrera-resolucion.md) | Carrera con vencimiento | FR-002, FR-003, FR-006 | SC-001, SC-002 | Sin prueba de CU-17 | Blocked |
| [TC-CU17-006](TC-CU17-006-evento-estado-final.md) | Evento de estado final | FR-004, FR-007 | SC-003, SC-004 | Sin prueba de CU-17 | Draft |
| [TC-CU17-007](TC-CU17-007-sin-reclamo.md) | Resolución sin reclamo | FR-001, FR-002 | SC-001 | Sin prueba de CU-17 | Draft |

## Cobertura de requisitos

| FR | Cobertura | TC | Alcance | Motivo / Observación |
|---|---|---|---|---|
| FR-001 | Bloqueado — CU-17 pendiente | 001, 002, 007 | Backend | Actualización administrativa |
| FR-002 | Bloqueado — CU-17 pendiente | 001, 002, 004, 005, 007 | Backend | Destinos |
| FR-003 | Bloqueado — CU-17 pendiente | 003, 005 | Backend | Estado terminal |
| FR-004 | Bloqueado — CU-17 pendiente | 001, 006 | Backend | Motivo local |
| FR-005 | Bloqueado — CU-17 pendiente | 001, 006 | Backend | Sin dinero |
| FR-006 | Bloqueado — CU-17 pendiente | 004, 005 | Backend | Cierre automático |
| FR-007 | Bloqueado — CU-17 pendiente | 002, 006 | Backend | Evento final y PENDING interno |
| FR-008 | Sin cobertura backend | — | UI | Visualización e interacción requieren pruebas frontend. |
| FR-009 | Sin cobertura backend | — | UI | Visualización e interacción requieren pruebas frontend. |
| FR-010 | Sin cobertura backend | — | UI | Visualización e interacción requieren pruebas frontend. |
| FR-011 | Sin cobertura backend | — | UI | Visualización e interacción requieren pruebas frontend. |
| FR-012 | Sin cobertura backend | — | UI | Visualización e interacción requieren pruebas frontend. |
| FR-013 | Fuera de alcance | — | SLA | Requiere definición de medición y ejecución. |

## Criterios de éxito

| SC | Estado | Observación |
|---|---|---|
| SC-001 | Bloqueado | TC-001, 002, 004, 005 y 007; estado y concurrencia. |
| SC-002 | Bloqueado | TC-003 a 005; estados finales. |
| SC-003 | Bloqueado | TC-001 y 006; requiere auditoría de neutralidad. |
| SC-004 | Bloqueado | TC-001 y 006; motivo local. |

## Checklist de ejecución

| TC | Documentado | Ejecutado | Resultado | Ejecutado por | Fecha | Observaciones |
|---|---|---|---|---|---|---|
| TC-CU17-001 | Sí | No | Pendiente | — | — | Blocked |
| TC-CU17-002 | Sí | No | Pendiente | — | — | Blocked |
| TC-CU17-003 | Sí | No | Pendiente | — | — | Blocked |
| TC-CU17-004 | Sí | No | Pendiente | — | — | Blocked |
| TC-CU17-005 | Sí | No | Pendiente | — | — | Blocked |
| TC-CU17-006 | Sí | No | Pendiente | — | — | Draft |
| TC-CU17-007 | Sí | No | Pendiente | — | — | Draft |

## Brechas detectadas

1. [Contexto](../../../context/implementContext.md): CU-17 y contexto de disputa no implementados; CU-16 y CU-18 pendientes.
2. El spec deja abierta la resolución de PENDING sin reclamo y el tamaño del motivo; el plan propone un límite.
3. CU-16 FR-011 traduce ACCEPTED a COMPLETADO y REJECTED a APPROVED, mientras CU-17 FR-007 indica COMPLETED y REJECTED; confirmar contrato de M3.
4. El plan afirma publicación síncrona, pero outbox no garantiza entrega ni consumo inmediato.
5. FR-008 a FR-012 requieren UI; SLA de 24 horas requiere definición y medición.
6. Roles y JWT administrativos aún no están implementados.

## Convenciones de estado

- **Draft**: contrato incompleto o inconsistente.
- **In Process**: ejecutable con implementación actual.
- **Approved**: aprobado tras revisión y ejecución; ninguno aquí.
- **Blocked**: depende de funcionalidad pendiente.

## Ejecución automatizada — 2026-11-10

Se ejecutó `mvn clean test` en una copia aislada del commit `3e4e6a4`, con JDK 25 y compilación configurada para Java 21. El conjunto completo registró **234 pruebas: 132 exitosas, 0 fallos de aserción y 102 errores** al iniciar contextos que requieren Docker/Testcontainers. Maven terminó con `BUILD FAILURE`. Las pruebas con WireMock usaron dobles locales, no los servicios reales.

Los resultados siguientes corresponden a clases automatizadas vinculadas a este CU, no a la ejecución íntegra de cada caso documental. Los pasos y el checklist de cada TC permanecen pendientes cuando requieren acciones o comprobaciones adicionales; ningún caso se aprueba por esta ejecución.

No hay clases automatizadas vinculadas en los casos documentados de este CU.
