# CU-18 — Pruebas backend de eventos de disputa

Casos del evento asíncrono de resolución de disputa hacia M3. El contexto de disputa y su publicación aún no están implementados.

**Estado global**: Draft por implementación y contradicciones contractuales.

## Trazabilidad

| TC | Escenario | FR | SC | Test automatizado parcial | Status |
|---|---|---|---|---|---|
| [TC-CU18-001](TC-CU18-001-estados-finales.md) | Publicación de estado final | FR-001, FR-002 | SC-001 | Sin prueba de CU-18 | Draft |
| [TC-CU18-002](TC-CU18-002-pending-interno.md) | Ausencia de evento PENDING | FR-001 | SC-001 | Sin prueba de CU-18 | Blocked |
| [TC-CU18-003](TC-CU18-003-privacidad-evento.md) | Contenido del evento | FR-002, FR-003, FR-006 | SC-002 | Sin prueba de CU-18 | Draft |
| [TC-CU18-004](TC-CU18-004-identidad-evento.md) | Identificador y reenvío | FR-004, FR-005 | SC-003, SC-004 | Sin prueba de CU-18 | Blocked |
| [TC-CU18-005](TC-CU18-005-fallo-outbox.md) | Atomicidad de transición y outbox | FR-001, FR-005 | SC-001, SC-004 | Sin prueba de CU-18 | Blocked |

## Cobertura de requisitos

| FR | Cobertura | TC | Alcance | Motivo / Observación |
|---|---|---|---|---|
| FR-001 | Bloqueado — CU-18 pendiente | 001, 002, 005 | Backend | Estados finales |
| FR-002 | Bloqueado — CU-18 pendiente | 001, 003 | Backend | Mapeo y motivo contradictorios |
| FR-003 | Bloqueado — CU-18 pendiente | 003 | Backend | Sin datos financieros |
| FR-004 | Bloqueado — CU-18 pendiente | 004 | Backend | Identificador |
| FR-005 | Bloqueado — CU-18 pendiente | 004, 005 | Backend | Reintento y atomicidad |
| FR-006 | Bloqueado — CU-18 pendiente | 003 | Backend | Sin dinero |
| FR-007 | Bloqueado — CU-18 pendiente | — | Backend | SLA de 24 horas requiere medición |


## Criterios de éxito

| SC | Estado | Observación |
|---|---|---|
| SC-001 | Bloqueado | TC-001, 002 y 005; contrato de mapeo en revisión. |
| SC-002 | Bloqueado | TC-003; privacidad y ausencia de montos. |
| SC-003 | Bloqueado | TC-004; identificadores para deduplicación. |
| SC-004 | Bloqueado | TC-004 y 005; política de agotamiento requiere aclaración. |

## Checklist de ejecución

| TC | Documentado | Ejecutado | Resultado | Ejecutado por | Fecha | Observaciones |
|---|---|---|---|---|---|---|
| TC-CU18-001 | Sí | No | Pendiente | — | — | Draft |
| TC-CU18-002 | Sí | No | Pendiente | — | — | Blocked |
| TC-CU18-003 | Sí | No | Pendiente | — | — | Draft |
| TC-CU18-004 | Sí | No | Pendiente | — | — | Blocked |
| TC-CU18-005 | Sí | No | Pendiente | — | — | Blocked |

## Brechas detectadas

1. [Contexto](../../../context/implementContext.md): contexto de disputa, publicación y cola de disputa T-18c pendientes.
2. Spec CU-18 usa ACCEPTED/REJECTED, su plan usa COMPLETED/REJECTED y CU-16 usa COMPLETADO/APPROVED.
3. FR-002 permite motivo opcional, pero plan CU-18 y specs CU-16/CU-17 prohíben transmitirlo.
4. Spec dice reintentar hasta confirmar; plan limita a cinco intentos. Política de agotamiento y DLQ necesita acuerdo.
5. Identidad, orden y versión del evento requieren definición con M3; publisher confirm no demuestra consumo.
6. SLA de 24 horas requiere medición y responsabilidades concretas.

## Convenciones de estado

- **Draft**: contrato incompleto o inconsistente.
- **In Process**: ejecutable con implementación actual.
- **Approved**: aprobado tras revisión y ejecución; ninguno aquí.
- **Blocked**: depende de funcionalidad pendiente.
