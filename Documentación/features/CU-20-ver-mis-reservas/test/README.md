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
| FR-001 | Parcial | 001, 002 | Backend | Roles RENTER y OWNER |
| FR-002 | Parcial | 001, 002 | Backend | Aislamiento por identificador |
| FR-003 | Parcial | 001, 004 | Backend | Datos básicos y nombre con flota fake; faltan campos y servicios reales |
| FR-004 | Sin evidencia | 004 | Backend | Sin aserción del importe original de una reserva cancelada |
| FR-005 | Parcial | 006 | Backend | Subestado FLEXIBLE verificado; otros subtipos no comprobados |
| FR-006 | Sin evidencia | — | UI | Selección UI |
| FR-007 | Parcial | 005 | Backend | Filtro backend probado con algunos estados; encabezados UI sin prueba |
| FR-008 | Sin evidencia | — | UI | Badges UI |
| FR-009 | Sin evidencia | — | UI | Datos enriquecidos parciales con dobles; UI sin prueba |
| FR-010 | Sin evidencia | — | UI | Etiquetas UI |
| FR-011 | Sin evidencia | — | UI | Enlace UI |
| FR-012 | Parcial | 003, 005 | Backend | Paginación parcial; límite de spec y plan difiere |

## Criterios de éxito

| SC | Estado | Observación |
|---|---|---|
| SC-001 | No demostrado | TC-004 carece de aserción del importe original en una reserva cancelada; no se demuestra ausencia global de cálculos. |
| SC-002 | Parcialmente demostrado | TC-001 a 003; identidad por headers es temporal y no autenticada por JWT. |

## Checklist de ejecución

| TC | Documentado | Ejecutado | Resultado | Ejecutado por | Fecha | Observaciones |
|---|---|---|---|---|---|---|
| TC-CU20-001 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Comprueba dos reservas propias frente a una ajena y campos de respuesta con headers; no verifica JWT ni todos los campos del paso. |
| TC-CU20-002 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Comprueba tres reservas del propietario; no incluye otro propietario para comprobar exclusión. |
| TC-CU20-003 | Sí | Sí | Draft | Agente automatizado | 2026-11-13 | Comprueba page=0 y limit=51; no comprueba orden ni segunda página. FR-012 fija 10 y plan permite hasta 50. |
| TC-CU20-004 | Sí | No | Pendiente | — | — | No se encontró aserción del importe original en una reserva cancelada. |
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
- **Approved**: todos los pasos, postcondiciones y FR/SC se demuestran mediante pruebas V4 ejecutadas y aserciones suficientes.
- **Blocked**: depende de funcionalidad pendiente.

## Ejecución automatizada — 2026-11-13

`sg docker -c 'bash ./mvnw -B clean test'`; inicio 2026-11-13T19:02:00-05:00, fin 2026-11-13T19:03:11-05:00, salida 0; código base `568290a0d4334b477a4b0f9d5e949e981da3fa55`. La identidad se suministra por headers y los datos de flota son ficticios.

| Clase | Tests | Fallos | Errores | Omitidos | Resultado |
|---|---:|---:|---:|---:|---|
| [ReservationQueryTest](../../../../src/test/java/com/seashare/m2/reservas/integration/ReservationQueryTest.java) | 13 | 0 | 0 | 0 | XML V4 exitoso; alcance parcial según cada TC |

**Decisión aprobada 2026-11-13 — TC-CU20-003-paginacion.md:** Se aprobó limit=10 por defecto y valores configurables de 1 a 50.
