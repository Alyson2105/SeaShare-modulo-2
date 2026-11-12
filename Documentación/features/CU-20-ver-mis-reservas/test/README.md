# CU-20 — Pruebas backend de listado de reservas

Casos para GET /api/v1/reservations, con identidad temporal por headers y contrato de respuesta simplificado.

**Estado global**: Draft por integración y contratos pendientes.

## Trazabilidad

| TC | Escenario | FR | SC | Test automatizado parcial | Status |
|---|---|---|---|---|---|
| [TC-CU20-001](TC-CU20-001-aislamiento-arrendatario.md) | Listado de arrendatario | FR-001, FR-002, FR-003 | SC-002 | [consulta](../../../../src/test/java/com/seashare/m2/reservas/integration/ReservationQueryTest.java) | In Process |
| [TC-CU20-002](TC-CU20-002-aislamiento-propietario.md) | Listado de propietario | FR-001, FR-002 | SC-002 | [consulta](../../../../src/test/java/com/seashare/m2/reservas/integration/ReservationQueryTest.java) | In Process |
| [TC-CU20-003](TC-CU20-003-paginacion.md) | Paginación de reservas | FR-012 | SC-002 | [consulta](../../../../src/test/java/com/seashare/m2/reservas/integration/ReservationQueryTest.java) | In Process |
| [TC-CU20-004](TC-CU20-004-total-original.md) | Total de reserva cancelada | FR-003, FR-004 | SC-001 | [consulta](../../../../src/test/java/com/seashare/m2/reservas/integration/ReservationQueryTest.java) | In Process |
| [TC-CU20-005](TC-CU20-005-filtro-pestanas.md) | Filtro de pestañas | FR-007, FR-012 | SC-002 | Sin prueba de CU-20 | Blocked |
| [TC-CU20-006](TC-CU20-006-subestado-cancelacion.md) | Subestado de cancelación | FR-005 | SC-001 | Sin prueba de CU-20 | Blocked |

## Cobertura de requisitos

| FR | Cobertura | TC | Alcance | Motivo / Observación |
|---|---|---|---|---|
| FR-001 | Parcial — ver caso y brechas | 001, 002 | Backend | Roles RENTER y OWNER |
| FR-002 | Parcial — ver caso y brechas | 001, 002 | Backend | Aislamiento por identificador |
| FR-003 | Parcial — ver caso y brechas | 001, 004 | Backend | Datos básicos; nombre de barco pendiente |
| FR-004 | Parcial — ver caso y brechas | 004 | Backend | Estimated total sin deducción |
| FR-005 | Parcial — ver caso y brechas | 006 | Backend | Subestado no expuesto |
| FR-006 | Sin cobertura backend | — | UI | Selección UI |
| FR-007 | Parcial — ver caso y brechas | 005 | Backend | Filtro backend pendiente; encabezados UI |
| FR-008 | Sin cobertura backend | — | UI | Badges UI |
| FR-009 | Sin cobertura backend | — | UI | Tarjeta UI y datos enriquecidos pendientes |
| FR-010 | Sin cobertura backend | — | UI | Etiquetas UI |
| FR-011 | Sin cobertura backend | — | UI | Enlace UI |
| FR-012 | Parcial — ver caso y brechas | 003, 005 | Backend | Paginación parcial; límite y filtro difieren |

## Criterios de éxito

| SC | Estado | Observación |
|---|---|---|
| SC-001 | Parcial | TC-004; no demuestra ausencia global de cálculos. |
| SC-002 | Parcial | TC-001 a 003; identidad por headers es temporal y no autenticada por JWT. |

## Checklist de ejecución

| TC | Documentado | Ejecutado | Resultado | Ejecutado por | Fecha | Observaciones |
|---|---|---|---|---|---|---|
| TC-CU20-001 | Sí | No | Pendiente | — | — | In Process |
| TC-CU20-002 | Sí | No | Pendiente | — | — | In Process |
| TC-CU20-003 | Sí | No | Pendiente | — | — | In Process |
| TC-CU20-004 | Sí | No | Pendiente | — | — | In Process |
| TC-CU20-005 | Sí | No | Pendiente | — | — | Blocked |
| TC-CU20-006 | Sí | No | Pendiente | — | — | Blocked |

## Brechas detectadas

1. [Contexto](../../../context/implementContext.md): CU-20 es parcial; falta filtro y estructura del plan.
2. Identidad temporal por headers no prueba autenticación real ni seguridad contra suplantación.
3. La respuesta actual usa items, page, limit y total_items; no incluye envoltorio, badges, nombres de flota ni subtipo.
4. FR-012 fija 10 por página; plan y código permiten limit hasta 50.
5. El importe es estimated_total recibido del cliente en CU-02; no implica liquidación confirmada de M3.
6. Elementos visuales y navegación requieren pruebas UI.

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
| [ReservationQueryTest](../../../../src/test/java/com/seashare/m2/reservas/integration/ReservationQueryTest.java) | 6 | 0 | 6 | Bloqueada por Docker/Testcontainers |
