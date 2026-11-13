# CU-16 — Pruebas backend de disputa de garantía

Casos de creación automática, reclamo y vencimiento de disputa. El contexto de disputa está implementado; su estado es distinto del de reserva. La apertura aún no está conectada a CU-07.

**Estado global**: Draft; persisten contradicciones contractuales. Evaluado el 2026-11-13.

## Trazabilidad

| TC | Escenario | FR | SC | Test automatizado parcial | Status |
|---|---|---|---|---|---|
| [TC-CU16-001](TC-CU16-001-apertura-automatica.md) | Apertura automática | FR-001, FR-003, FR-004, FR-005 | SC-001 | OpenDisputeIntegrationTest#opensPendingDisputeWith24HourWindow (parcial) | Blocked |
| [TC-CU16-002](TC-CU16-002-cancelacion-sin-disputa.md) | Reserva cancelada sin disputa | FR-002 | SC-002 | Sin prueba pertinente | In Process |
| [TC-CU16-003](TC-CU16-003-reclamo-en-ventana.md) | Reclamo dentro de ventana | FR-005, FR-006, FR-010, FR-011 | SC-004, SC-005 | RegisterDisputeClaimIntegrationTest#registersClaimInsideWindowAndKeepsPending (parcial) | In Process |
| [TC-CU16-004](TC-CU16-004-vencimiento-sin-reclamo.md) | Cierre sin reclamo | FR-007, FR-008, FR-011 | SC-003 | DisputeWindowCloseIntegrationTest#closesAtExactWindowEnd (parcial) | Draft |
| [TC-CU16-005](TC-CU16-005-reclamo-fuera-ventana.md) | Reclamo fuera de ventana | FR-005, FR-009 | SC-005 | RegisterDisputeClaimIntegrationTest#rejectsClaimAtExactWindowEnd (parcial) | In Process |
| [TC-CU16-006](TC-CU16-006-reclamo-repetido.md) | Segundo reclamo o edición | FR-006 | SC-005 | RegisterDisputeClaimIntegrationTest#rejectsSecondClaim (parcial) | Draft |

## Cobertura de requisitos

| FR | Cobertura | TC | Alcance | Motivo / Observación |
|---|---|---|---|---|
| FR-001 | Bloqueado | 001 | Backend | Bloqueado: CU-07 no invoca apertura; apertura directa probada. |
| FR-002 | Sin evidencia | 002 | Backend | Sin prueba de consulta de disputas tras cancelación. |
| FR-003 | Parcial | 001 | Backend | Parcial: historial SYSTEM probado en apertura directa. |
| FR-004 | Sin evidencia | 001 | Backend | Sin evidencia: apertura registra historial directamente, no mediante CU-17. |
| FR-005 | Parcial | 001, 003, 005 | Backend | Parcial: ventana de 24 horas en apertura directa. |
| FR-006 | Parcial | 003, 006 | Backend | Parcial: reclamo y persistencia probados; D-01 sigue abierto. |
| FR-007 | Sin evidencia | 004 | Backend | Sin prueba de novedades de check-out vinculadas al vencimiento. |
| FR-008 | Parcial | 004 | Backend | Parcial: barrido de vencimiento y estado REJECTED probados. |
| FR-009 | Parcial | 005 | Backend | Parcial: rechazo y ausencia de reclamo en el límite; HTTP aislado con mock. |
| FR-010 | Sin evidencia | 003 | Backend | Sin prueba integral de ausencia de operaciones financieras. |
| FR-011 | Parcial | 003, 004 | Backend | Parcial: publicación RabbitMQ probada; mapeo del spec CU-16 contradice CU-18. |
| FR-012 | Sin cobertura backend | — | UI | Visualización e interacción requieren pruebas frontend. |
| FR-013 | Sin cobertura backend | — | UI | Visualización e interacción requieren pruebas frontend. |
| FR-014 | Sin cobertura backend | — | UI | Visualización e interacción requieren pruebas frontend. |
| FR-015 | Sin cobertura backend | — | UI | Visualización e interacción requieren pruebas frontend. |
| FR-016 | Sin cobertura backend | — | UI | Visualización e interacción requieren pruebas frontend. |

## Criterios de éxito

| SC | Estado | Observación |
|---|---|---|
| SC-001 | Bloqueado | Apertura no conectada a CU-07. |
| SC-002 | Sin evidencia | Sin prueba de ausencia tras cancelación. |
| SC-003 | Parcial | Barrido probado con disputa creada directamente; no demuestra universalidad. |
| SC-004 | Sin evidencia | Sin prueba integral de neutralidad financiera. |
| SC-005 | Parcial | Borde probado parcialmente; no demuestra todos los casos. |

## Checklist de ejecución

| TC | Documentado | Ejecutado | Resultado | Ejecutado por | Fecha | Observaciones |
|---|---|---|---|---|---|---|
| TC-CU16-001 | Sí | Sí | Blocked | Agente automatizado | 2026-11-13 | OpenDisputeIntegrationTest#opensPendingDisputeWith24HourWindow (parcial); pasos completos pendientes. |
| TC-CU16-002 | Sí | No | Pendiente | — | — | Sin prueba pertinente; pasos completos pendientes. |
| TC-CU16-003 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | RegisterDisputeClaimIntegrationTest#registersClaimInsideWindowAndKeepsPending (parcial); pasos completos pendientes. |
| TC-CU16-004 | Sí | Sí | Draft | Agente automatizado | 2026-11-13 | DisputeWindowCloseIntegrationTest#closesAtExactWindowEnd (parcial); pasos completos pendientes. |
| TC-CU16-005 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | RegisterDisputeClaimIntegrationTest#rejectsClaimAtExactWindowEnd (parcial); pasos completos pendientes. |
| TC-CU16-006 | Sí | Sí | Draft | Agente automatizado | 2026-11-13 | RegisterDisputeClaimIntegrationTest#rejectsSecondClaim (parcial); pasos completos pendientes. |

## Brechas detectadas

1. [Contexto](../../../context/implementContext.md): la implementación de disputa existe; queda pendiente conectar CU-07 con su apertura.
2. El spec deja abierto D-01 sobre segundo reclamo o edición; el plan rechaza el segundo.
3. Política de reintentos y tolerancia del job de vencimiento no definida.
4. CU-07 y apertura existen, pero CU-07 no invoca la apertura automática.
5. La publicación final existe; el spec CU-16 FR-011 exige ACCEPTED → COMPLETADO y REJECTED → APPROVED, mientras el plan CU-18 y DisputeEventMapper#finalEvent usan COMPLETED y REJECTED. PENDING es interno.
6. FR-012 a FR-016 requieren pruebas frontend; JWT aún está pendiente.

## Convenciones de estado

- **Draft**: contrato incompleto o inconsistente.
- **In Process**: ejecutable con implementación actual.
- **Approved**: aprobado tras revisión y ejecución; ninguno aquí.
- **Blocked**: depende de funcionalidad pendiente.

## Ejecución automatizada — 2026-11-13

`sg docker -c 'bash ./mvnw -B clean test'`; salida Maven 1 por `ErrorCodeTest#noDuplicateCodes` (espera 29 constantes, encuentra 32), sin atribuir el fallo a estos escenarios. Pruebas de integración usan PostgreSQL y, cuando corresponde, RabbitMQ; controlador usa caso de uso simulado.

| Clase | Pruebas | Fallos | Errores | Omitidos | Resultado |
|---|---:|---:|---:|---:|---|
| com.seashare.m2.disputa.integration.OpenDisputeIntegrationTest | 3 | 0 | 0 | 0 | Verde; cobertura parcial |
| com.seashare.m2.disputa.integration.RegisterDisputeClaimIntegrationTest | 6 | 0 | 0 | 0 | Verde; cobertura parcial |
| com.seashare.m2.disputa.application.service.RegisterDisputeClaimServiceTest | 11 | 0 | 0 | 0 | Verde; cobertura parcial |
| com.seashare.m2.disputa.infrastructure.adapter.in.web.DisputeClaimControllerTest | 9 | 0 | 0 | 0 | Verde; cobertura parcial |
| com.seashare.m2.disputa.integration.DisputeWindowCloseIntegrationTest | 4 | 0 | 0 | 0 | Verde; cobertura parcial |
| com.seashare.m2.disputa.integration.DisputePublishingIntegrationTest | 4 | 0 | 0 | 0 | Verde; cobertura parcial |
| com.seashare.m2.disputa.application.service.DisputeEventMapperTest | 4 | 0 | 0 | 0 | Verde; cobertura parcial |
