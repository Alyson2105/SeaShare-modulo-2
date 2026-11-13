# CU-17 — Pruebas backend de estado de disputa

Casos de resolución administrativa y transición interna de disputas. El contexto de disputa está implementado y es distinto del estado de reserva.

**Estado global**: Draft; hay decisiones contractuales pendientes. Evaluado el 2026-11-13.

## Trazabilidad

| TC | Escenario | FR | SC | Test automatizado parcial | Status |
|---|---|---|---|---|---|
| [TC-CU17-001](TC-CU17-001-resolucion-admin.md) | Resolución administrativa | FR-001, FR-002, FR-004, FR-005 | SC-001, SC-003, SC-004 | UpdateDisputeStatusServiceTest#adminAcceptsDisputeWithClaim y #adminRejectsWithReason (parcial) | In Process |
| [TC-CU17-002](TC-CU17-002-revision-pendiente.md) | Revisión en curso | FR-001, FR-002, FR-007 | SC-001 | UpdateDisputeStatusServiceTest#adminKeepsPendingWithoutTransition; DisputePublishingIntegrationTest#pendingDisputeNeverPublishes (parcial) | In Process |
| [TC-CU17-003](TC-CU17-003-estado-terminal.md) | Inmutabilidad terminal | FR-003 | SC-002 | UpdateDisputeStatusServiceTest#finalDisputeCannotBeChanged; DisputeFinalStateImmutabilityTest#updateOnFinalDisputeFailsAndRowStaysIntact (parcial) | In Process |
| [TC-CU17-004](TC-CU17-004-cierre-automatico.md) | Cierre automático | FR-002, FR-006 | SC-001, SC-002 | DisputeWindowCloseIntegrationTest#closesAtExactWindowEnd | In Process |
| [TC-CU17-005](TC-CU17-005-carrera-resolucion.md) | Carrera con vencimiento | FR-002, FR-003, FR-006 | SC-001, SC-002 | UpdateDisputeStatusServiceTest#optimisticLockBecomesAlreadyResolved (parcial) | Draft |
| [TC-CU17-006](TC-CU17-006-evento-estado-final.md) | Evento de estado final | FR-004, FR-007 | SC-003, SC-004 | DisputePublishingIntegrationTest#acceptedIsPublishedAsCompletedWithoutAmounts, #expiryRejectionIsPublishedAsRejected y #pendingDisputeNeverPublishes (parcial) | Draft |
| [TC-CU17-007](TC-CU17-007-sin-reclamo.md) | Resolución sin reclamo | FR-001, FR-002 | SC-001 | UpdateDisputeStatusServiceTest#adminCannotAcceptWithoutClaim y #adminCanRejectWithoutClaim (parcial) | Draft |

## Cobertura de requisitos

| FR | Cobertura | TC | Alcance | Motivo / Observación |
|---|---|---|---|---|
| FR-001 | Parcial | 001, 002, 007 | Backend | Aceptación administrativa unitaria; JWT y cobertura completa pendientes. |
| FR-002 | Parcial | 001, 002, 004, 005, 007 | Backend | Destinos y PENDING probados parcialmente con mocks; D-01 sin resolver. |
| FR-003 | Parcial | 003, 005 | Backend | Bloqueo de terminales probado parcialmente por servicio y trigger. |
| FR-004 | Parcial | 001, 006 | Backend | Motivo local en servicio simulado y privacidad en publicación ACCEPTED. |
| FR-005 | Sin evidencia integral | 001, 006 | Backend | Sin prueba integral de ausencia de operaciones financieras. |
| FR-006 | Parcial | 004, 005 | Backend | Barrido directo con reloj simulado y PostgreSQL verifica cierre; scheduler desactivado por defecto. |
| FR-007 | Parcial | 002, 006 | Backend | PENDING sin mensaje y estados finales publicados en RabbitMQ; mapeo CU-16 incompatible. |
| FR-008 | Sin cobertura backend | — | UI | Visualización e interacción requieren pruebas frontend. |
| FR-009 | Sin cobertura backend | — | UI | Visualización e interacción requieren pruebas frontend. |
| FR-010 | Sin cobertura backend | — | UI | Visualización e interacción requieren pruebas frontend. |
| FR-011 | Sin cobertura backend | — | UI | Visualización e interacción requieren pruebas frontend. |
| FR-012 | Sin cobertura backend | — | UI | Visualización e interacción requieren pruebas frontend. |
| FR-013 | Fuera de alcance | — | SLA | Requiere definición de medición y ejecución. |

## Criterios de éxito

| SC | Estado | Observación |
|---|---|---|
| SC-001 | Parcial | Actualizaciones probadas parcialmente; D-01 sin resolver. |
| SC-002 | Parcial | Inmutabilidad probada parcialmente; carrera real no ejecutada. |
| SC-003 | Sin evidencia integral | Sin prueba integral de ausencia de operaciones financieras. |
| SC-004 | Parcial | Motivo de rechazo probado con repositorio simulado; falta persistencia integral. |

## Checklist de ejecución

| TC | Documentado | Ejecutado | Resultado | Ejecutado por | Fecha | Observaciones |
|---|---|---|---|---|---|---|
| TC-CU17-001 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | servicio y controlador con mocks; falta integración completa. |
| TC-CU17-002 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | revisión mockeada y ausencia de mensaje en RabbitMQ; falta persistencia de nota. |
| TC-CU17-003 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | pruebas parciales de estados finales; falta matriz HTTP completa. |
| TC-CU17-004 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | barrido directo, reloj simulado y PostgreSQL; no se ejecutó el scheduler. |
| TC-CU17-005 | Sí | Sí | Draft | Agente automatizado | 2026-11-13 | conflicto simulado, no carrera real. |
| TC-CU17-006 | Sí | Sí | Draft | Agente automatizado | 2026-11-13 | publicación y privacidad parciales; mapeo contractual incompatible. |
| TC-CU17-007 | Sí | Sí | Draft | Agente automatizado | 2026-11-13 | D-01 sigue abierta; decisiones distintas con mocks. |

## Brechas detectadas

1. [Contexto](../../../context/implementContext.md): CU-17, CU-16 y publicación de CU-18 existen; CU-07 aún no conecta la apertura de CU-16.
2. El spec deja abierta la resolución de PENDING sin reclamo y el tamaño del motivo; el plan propone un límite.
3. CU-16 FR-011 traduce ACCEPTED a COMPLETADO y REJECTED a APPROVED, mientras CU-17 FR-007 y plan CU-18 indican COMPLETED y REJECTED, y CU-18 spec FR-002 presenta ACCEPTED; confirmar contrato de M3.
4. El plan describe `m3_event_published` como emisión a RabbitMQ; UpdateDisputeStatusService#update lo deriva del estado final tras encolar, sin confirmar entrega ni consumo.
5. FR-008 a FR-012 requieren UI; SLA de 24 horas requiere definición y medición.
6. DisputeAdminController#update comprueba rol ADMIN; la identidad por JWT aún no está implementada.

## Convenciones de estado

- **Draft**: contrato incompleto o inconsistente.
- **In Process**: ejecutable con implementación actual.
- **Approved**: aprobado tras revisión y ejecución; ninguno aquí.
- **Blocked**: depende de funcionalidad pendiente.

## Ejecución automatizada — 2026-11-13

`sg docker -c 'bash ./mvnw -B clean test'`: salida 1 por ErrorCodeTest#noDuplicateCodes; el fallo no se atribuye a CU-17. Las pruebas unitarias usan puertos simulados; las de integración usan PostgreSQL y, cuando corresponde, RabbitMQ.

| Clase | Pruebas | Fallos | Errores | Omitidos | Resultado |
|---|---:|---:|---:|---:|---|
| com.seashare.m2.disputa.application.service.UpdateDisputeStatusServiceTest | 9 | 0 | 0 | 0 | Verde; alcance según TC |
| com.seashare.m2.disputa.infrastructure.adapter.in.web.DisputeAdminControllerTest | 9 | 0 | 0 | 0 | Verde; alcance según TC |
| com.seashare.m2.disputa.integration.DisputeFinalStateImmutabilityTest | 2 | 0 | 0 | 0 | Verde; alcance según TC |
| com.seashare.m2.disputa.integration.DisputeWindowCloseIntegrationTest | 4 | 0 | 0 | 0 | Verde; alcance según TC |
| com.seashare.m2.disputa.integration.DisputePublishingIntegrationTest | 4 | 0 | 0 | 0 | Verde; alcance según TC |
