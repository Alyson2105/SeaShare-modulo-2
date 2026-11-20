# CU-18 — Pruebas backend de eventos de disputa

Casos del evento asíncrono de resolución de disputa hacia M3. El contexto de disputa y la publicación mediante outbox y RabbitMQ están implementados.

**Estado global**: Draft; persisten contradicciones contractuales. Evaluado el 2026-11-13.

## Trazabilidad

| TC | Escenario | FR | SC | Test automatizado parcial | Status |
|---|---|---|---|---|---|
| [TC-CU18-001](TC-CU18-001-estados-finales.md) | Publicación de estado final | FR-001, FR-002 | SC-001 | DisputePublishingIntegrationTest#acceptedIsPublishedAsCompletedWithoutAmounts y #expiryRejectionIsPublishedAsRejected (parcial) | Draft |
| [TC-CU18-002](TC-CU18-002-pending-interno.md) | Ausencia de evento PENDING | FR-001 | SC-001 | DisputePublishingIntegrationTest#pendingDisputeNeverPublishes (parcial) | In Process |
| [TC-CU18-003](TC-CU18-003-privacidad-evento.md) | Contenido del evento | FR-002, FR-003, FR-006 | SC-002 | DisputePublishingIntegrationTest#acceptedIsPublishedAsCompletedWithoutAmounts (parcial) | Draft |
| [TC-CU18-004](TC-CU18-004-identidad-evento.md) | Identificador y reenvío | FR-004, FR-005 | SC-003, SC-004 | DisputeEventMapperTest#eventIdIsDeterministicPerDisputeAndStatus; OutboxRelayIntegrationTest#falloSimuladoReintentaHastaPublicar (relay genérico) (parcial) | In Process |
| [TC-CU18-005](TC-CU18-005-fallo-outbox.md) | Atomicidad de transición y outbox | FR-001, FR-005 | SC-001, SC-004 | Sin prueba de fallo al insertar outbox de disputa | In Process |

## Cobertura de requisitos

| FR | Cobertura | TC | Alcance | Motivo / Observación |
|---|---|---|---|---|
| FR-001 | Parcial | 001, 002, 005 | Backend | Publicación de finales y ausencia de mensaje PENDING probadas; mapeo en conflicto. |
| FR-002 | No verificable | 001, 003 | Backend | Spec CU-18 y plan difieren en ACCEPTED y posibilidad de incluir motivo. |
| FR-003 | Parcial | 003 | Backend | Payload ACCEPTED de prueba tiene cinco campos y no incluye montos ni owner_id. |
| FR-004 | Parcial | 004 | Backend | Mapper produce ID determinista; falta prueba de deduplicación de M3. |
| FR-005 | No verificable | 004, 005 | Backend | Relay genérico reintenta; falta rollback y reenvío de disputa; spec y plan discrepan sobre agotamiento. |
| FR-006 | Sin evidencia | 003 | Backend | Payload ACCEPTED carece de monto; no se demuestra ausencia integral de operaciones financieras. |
| FR-007 | Sin evidencia | — | Backend | No hay medición del SLA de 24 horas. |


## Criterios de éxito

| SC | Estado | Observación |
|---|---|---|
| SC-001 | Parcialmente demostrado | Publicación y PENDING probados parcialmente; mapeo contractual en conflicto. |
| SC-002 | Parcialmente demostrado | Payload ACCEPTED inspeccionado; falta rechazo administrativo. |
| SC-003 | Parcialmente demostrado | ID determinista en unidad y mensaje de prueba; deduplicación en M3 sin comprobar. |
| SC-004 | Parcialmente demostrado | Reintento genérico probado; rollback y ausencia de pérdida no demostrados. |

## Checklist de ejecución

| TC | Documentado | Ejecutado | Resultado | Ejecutado por | Fecha | Observaciones |
|---|---|---|---|---|---|---|
| TC-CU18-001 | Sí | Sí | Draft | Agente automatizado | 2026-11-13 | publicación en RabbitMQ; mapeos incompatibles. |
| TC-CU18-002 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | cola sin mensaje PENDING tras relay; no se inspeccionó outbox al crear. |
| TC-CU18-003 | Sí | Sí | Draft | Agente automatizado | 2026-11-13 | cinco campos en ACCEPTED; política de motivo contradictoria. |
| TC-CU18-004 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | identidad determinista y reintento genérico; spec exige reintentar hasta confirmar y plan limita a cinco intentos. |
| TC-CU18-005 | Sí | No | Pendiente | — | — | sin prueba de rollback de disputa y outbox. |

## Brechas detectadas

1. [Contexto](../../../context/implementContext.md): contexto, outbox y cola de disputa existen; las pruebas alcanzan RabbitMQ de prueba, no un consumidor M3 real.
2. Spec CU-18 usa ACCEPTED/REJECTED, su plan usa COMPLETED/REJECTED y CU-16 usa COMPLETADO/APPROVED.
3. FR-002 permite motivo opcional, pero plan CU-18 y specs CU-16/CU-17 prohíben transmitirlo.
4. Spec dice reintentar hasta confirmar; plan limita a cinco intentos. PublishPendingOutboxService#registerFailure marca dead en el quinto fallo; política de agotamiento y DLQ necesita acuerdo.
5. Identidad, orden y versión del evento requieren definición con M3; publisher confirm no demuestra consumo.
6. SLA de 24 horas requiere medición y responsabilidades concretas.

## Convenciones de estado

- **Draft**: contrato incompleto o inconsistente.
- **In Process**: ejecutable con implementación actual.
- **Approved**: escenario y postcondiciones demostrados por aserciones de la corrida V4, sin impedimentos de aprobación.
- **Blocked**: depende de funcionalidad pendiente.

## Ejecución automatizada — 2026-11-13

`sg docker -c 'bash ./mvnw -B clean test'`; inicio 2026-11-13T19:02:00-05:00, fin 2026-11-13T19:03:11-05:00; BASELINE_HEAD `568290a0d4334b477a4b0f9d5e949e981da3fa55`; salida 0. Integración usa PostgreSQL y RabbitMQ de prueba; el reintento de OutboxRelayIntegrationTest es genérico, no específico de disputa.

| Clase | Pruebas | Fallos | Errores | Omitidos | Resultado |
|---|---:|---:|---:|---:|---|
| com.seashare.m2.disputa.application.service.DisputeEventMapperTest | 4 | 0 | 0 | 0 | Parcial para los escenarios relacionados |
| com.seashare.m2.disputa.application.service.UpdateDisputeStatusServiceTest | 9 | 0 | 0 | 0 | Parcial para los escenarios relacionados |
| com.seashare.m2.disputa.integration.DisputePublishingIntegrationTest | 4 | 0 | 0 | 0 | Parcial para los escenarios relacionados |
| com.seashare.m2.reservas.application.service.PublishPendingOutboxServiceTest | 10 | 0 | 0 | 0 | Parcial para los escenarios relacionados |
| com.seashare.m2.reservas.integration.OutboxRelayIntegrationTest | 4 | 0 | 0 | 0 | Parcial para los escenarios relacionados |
