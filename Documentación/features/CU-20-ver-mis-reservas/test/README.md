# CU-20 — Pruebas backend de listado de reservas

Casos para GET /api/v1/reservations, con identidad temporal por headers y contrato de respuesta simplificado.

**Estado global**: Draft por discrepancia contractual de paginación; evaluado el 2026-11-13.

## Trazabilidad

| TC | Escenario | FR | SC | Test automatizado parcial | Status |
|---|---|---|---|---|---|
| [TC-CU20-001](TC-CU20-001-aislamiento-arrendatario.md) | Listado de arrendatario | FR-001, FR-002, FR-003 | SC-002 | src/test/java/com/seashare/m2/reservas/integration/ReservationQueryTest.java#listsOnlyOwnReservations | In Process |
| [TC-CU20-002](TC-CU20-002-aislamiento-propietario.md) | Listado de propietario | FR-001, FR-002 | SC-002 | src/test/java/com/seashare/m2/reservas/integration/ReservationQueryTest.java#listsOnlyOwnReservations | In Process |
| [TC-CU20-003](TC-CU20-003-paginacion.md) | Paginación de reservas | FR-012 | SC-002 | src/test/java/com/seashare/m2/reservas/integration/ReservationQueryTest.java#invalidPaginationIs400 | Draft |
| [TC-CU20-004](TC-CU20-004-total-original.md) | Total de reserva cancelada | FR-003, FR-004 | SC-001 | Sin prueba automatizada del importe cancelado | In Process |
| [TC-CU20-005](TC-CU20-005-filtro-pestanas.md) | Filtro de pestañas | FR-007, FR-012 | SC-002 | src/test/java/com/seashare/m2/reservas/integration/ReservationQueryTest.java#listsWithAppliedFilter | In Process |
| [TC-CU20-006](TC-CU20-006-subestado-cancelacion.md) | Subestado de cancelación | FR-005 | SC-001 | src/test/java/com/seashare/m2/reservas/integration/ReservationQueryTest.java#listsWithAppliedFilter | In Process |

## Cobertura de requisitos

| FR | Cobertura | TC | Alcance | Motivo / Observación |
|---|---|---|---|---|
| FR-001 | Parcial — ver caso y brechas | 001, 002 | Backend | Roles RENTER y OWNER |
| FR-002 | Parcial — ver caso y brechas | 001, 002 | Backend | Aislamiento por identificador |
| FR-003 | Parcial — ver caso y brechas | 001, 004 | Backend | Datos básicos; nombre de barco pendiente |
| FR-004 | Parcial — ver caso y brechas | 004 | Backend | Estimated total sin deducción |
| FR-005 | Parcial — ver caso y brechas | 006 | Backend | Subestado FLEXIBLE verificado; otros subtipos no comprobados |
| FR-006 | Sin cobertura backend | — | UI | Selección UI |
| FR-007 | Parcial — ver caso y brechas | 005 | Backend | Filtro backend probado con algunos estados; encabezados UI sin prueba |
| FR-008 | Sin cobertura backend | — | UI | Badges UI |
| FR-009 | Sin cobertura backend | — | UI | Datos enriquecidos parciales con dobles; UI sin prueba |
| FR-010 | Sin cobertura backend | — | UI | Etiquetas UI |
| FR-011 | Sin cobertura backend | — | UI | Enlace UI |
| FR-012 | Parcial — ver caso y brechas | 003, 005 | Backend | Paginación parcial; límite de spec y plan difiere |

## Criterios de éxito

| SC | Estado | Observación |
|---|---|---|
| SC-001 | Parcial | TC-004; no demuestra ausencia global de cálculos. |
| SC-002 | Parcial | TC-001 a 003; identidad por headers es temporal y no autenticada por JWT. |

## Checklist de ejecución

| TC | Documentado | Ejecutado | Resultado | Ejecutado por | Fecha | Observaciones |
|---|---|---|---|---|---|---|
| TC-CU20-001 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Comprueba dos reservas propias frente a una ajena y campos de respuesta con headers; no verifica JWT ni todos los campos del paso. |
| TC-CU20-002 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Comprueba tres reservas del propietario; no incluye otro propietario para comprobar exclusión. |
| TC-CU20-003 | Sí | Sí | Draft | Agente automatizado | 2026-11-13 | Comprueba page=0 y limit=51; no comprueba orden ni segunda página. FR-012 fija 10 y plan permite hasta 50. |
| TC-CU20-004 | Sí | No | Pendiente | Agente automatizado | — | No se encontró aserción del importe original en una reserva cancelada. |
| TC-CU20-005 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Comprueba subconjuntos history y cancelled, contadores y estado CANCELLED; no todos los estados del contrato. |
| TC-CU20-006 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Comprueba subtipo FLEXIBLE y etiqueta contextual; no compara el importe ni otros subtipos. |

## Brechas detectadas

1. CU-20 implementa filtro y estructura del plan; faltan pruebas completas y servicios reales.
2. Identidad temporal por headers no prueba autenticación real ni seguridad contra suplantación.
3. La respuesta incluye envoltorio, badges y subtipo; pruebas usan datos de flota ficticios.
4. FR-012 fija 10 por página; plan y código permiten limit hasta 50.
5. El importe es estimated_total recibido del cliente en CU-02; no implica liquidación confirmada de M3.
6. Elementos visuales y navegación requieren pruebas UI.

## Convenciones de estado

- **Draft**: contrato incompleto o inconsistente.
- **In Process**: ejecutable con implementación actual.
- **Approved**: aprobado tras revisión y ejecución; ninguno aquí.
- **Blocked**: depende de funcionalidad pendiente.

## Ejecución automatizada — 2026-11-13

La suite terminó con 395 pruebas, 0 fallos, 0 errores y 0 omitidas. La clase usa PostgreSQL de Testcontainers, MockMvc y dependencias de flota simuladas.

| Clase | Tests | Fallos | Errores | Omitidos | Resultado |
|---|---:|---:|---:|---:|---|
| ReservationQueryTest | 12 | 0 | 0 | 0 | Exitosa |
