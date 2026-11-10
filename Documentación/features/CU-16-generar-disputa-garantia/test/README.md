# CU-16 — Pruebas backend de disputa de garantía

Casos de creación automática, reclamo y vencimiento de disputa. El contexto disputa no está implementado; su estado es distinto del de reserva.

**Estado global**: Draft por implementación y contratos pendientes.

## Trazabilidad

| TC | Escenario | FR | SC | Test automatizado parcial | Status |
|---|---|---|---|---|---|
| [TC-CU16-001](TC-CU16-001-apertura-automatica.md) | Apertura automática | FR-001, FR-003, FR-004, FR-005 | SC-001 | Sin prueba de CU-16 | Blocked |
| [TC-CU16-002](TC-CU16-002-cancelacion-sin-disputa.md) | Reserva cancelada sin disputa | FR-002 | SC-002 | Sin prueba de CU-16 | Blocked |
| [TC-CU16-003](TC-CU16-003-reclamo-en-ventana.md) | Reclamo dentro de ventana | FR-005, FR-006, FR-010, FR-011 | SC-004, SC-005 | Sin prueba de CU-16 | Blocked |
| [TC-CU16-004](TC-CU16-004-vencimiento-sin-reclamo.md) | Cierre sin reclamo | FR-007, FR-008, FR-011 | SC-003 | Sin prueba de CU-16 | Blocked |
| [TC-CU16-005](TC-CU16-005-reclamo-fuera-ventana.md) | Reclamo fuera de ventana | FR-005, FR-009 | SC-005 | Sin prueba de CU-16 | Blocked |
| [TC-CU16-006](TC-CU16-006-reclamo-repetido.md) | Segundo reclamo o edición | FR-006 | SC-005 | Sin prueba de CU-16 | Draft |

## Cobertura de requisitos

| FR | Cobertura | TC | Alcance | Motivo / Observación |
|---|---|---|---|---|
| FR-001 | Bloqueado — CU-16 pendiente | 001 | Backend | Apertura por cierre |
| FR-002 | Bloqueado — CU-16 pendiente | 002 | Backend | No apertura en cancelación |
| FR-003 | Bloqueado — CU-16 pendiente | 001 | Backend | Sistema crea |
| FR-004 | Bloqueado — CU-16 pendiente | 001 | Backend | CU-17 inicial |
| FR-005 | Bloqueado — CU-16 pendiente | 001, 003, 005 | Backend | Ventana 24 h |
| FR-006 | Bloqueado — CU-16 pendiente | 003, 006 | Backend | Reclamo; repetición por aclarar |
| FR-007 | Bloqueado — CU-16 pendiente | 004 | Backend | Novedades no son reclamo |
| FR-008 | Bloqueado — CU-16 pendiente | 004 | Backend | Cierre automático |
| FR-009 | Bloqueado — CU-16 pendiente | 005 | Backend | Fuera de ventana |
| FR-010 | Bloqueado — CU-16 pendiente | 003 | Backend | Sin dinero |
| FR-011 | Bloqueado — CU-16 pendiente | 003, 004 | Backend | Eventos finales y PENDING interno |
| FR-012 | Sin cobertura backend | — | UI | Visualización e interacción requieren pruebas frontend. |
| FR-013 | Sin cobertura backend | — | UI | Visualización e interacción requieren pruebas frontend. |
| FR-014 | Sin cobertura backend | — | UI | Visualización e interacción requieren pruebas frontend. |
| FR-015 | Sin cobertura backend | — | UI | Visualización e interacción requieren pruebas frontend. |
| FR-016 | Sin cobertura backend | — | UI | Visualización e interacción requieren pruebas frontend. |

## Criterios de éxito

| SC | Estado | Observación |
|---|---|---|
| SC-001 | Bloqueado | TC-001; creación y ventana. |
| SC-002 | Bloqueado | TC-002; cancelación sin disputa. |
| SC-003 | Bloqueado | TC-004; expiración sin reclamo. |
| SC-004 | Bloqueado | TC-003; neutralidad financiera requiere auditoría integral. |
| SC-005 | Bloqueado | TC-005; límite de ventana. |

## Checklist de ejecución

| TC | Documentado | Ejecutado | Resultado | Ejecutado por | Fecha | Observaciones |
|---|---|---|---|---|---|---|
| TC-CU16-001 | Sí | No | Pendiente | — | — | Blocked |
| TC-CU16-002 | Sí | No | Pendiente | — | — | Blocked |
| TC-CU16-003 | Sí | No | Pendiente | — | — | Blocked |
| TC-CU16-004 | Sí | No | Pendiente | — | — | Blocked |
| TC-CU16-005 | Sí | No | Pendiente | — | — | Blocked |
| TC-CU16-006 | Sí | No | Pendiente | — | — | Draft |

## Brechas detectadas

1. [Contexto](../../../context/implementContext.md): CU-16, CU-17 y CU-18 no están implementados; tablas Flyway no equivalen a funcionalidad.
2. El spec deja abierto D-01 sobre segundo reclamo o edición; el plan rechaza el segundo.
3. Política de reintentos y tolerancia del job de vencimiento no definida.
4. CU-07 y apertura de disputa automática aún están pendientes.
5. Evento final y privacidad dependen de CU-18; PENDING es interno.
6. FR-012 a FR-016 requieren pruebas frontend; JWT aún está pendiente.

## Convenciones de estado

- **Draft**: contrato incompleto o inconsistente.
- **In Process**: ejecutable con implementación actual.
- **Approved**: aprobado tras revisión y ejecución; ninguno aquí.
- **Blocked**: depende de funcionalidad pendiente.
