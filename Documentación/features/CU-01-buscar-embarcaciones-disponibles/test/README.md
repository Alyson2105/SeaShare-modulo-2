# CU-01 — Pruebas backend de búsqueda de embarcaciones

Casos del catálogo y cotización en lote propuestos. CU-01 no está implementado; el adaptador lote de CU-11 es componente parcial.

**Estado global**: Draft por implementación y contradicciones contractuales.

## Trazabilidad

| TC | Escenario | FR | SC | Test automatizado parcial | Status |
|---|---|---|---|---|---|
| [TC-CU01-001](TC-CU01-001-busqueda-con-filtros.md) | Búsqueda con filtros | FR-001, FR-002, FR-003 | SC-001 | [lote](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpBatchEstimateAdapterTest.java) | Blocked |
| [TC-CU01-002](TC-CU01-002-busqueda-sin-filtros.md) | Catálogo sin filtros | FR-001, FR-002, FR-009 | SC-003 | [lote](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpBatchEstimateAdapterTest.java) | Blocked |
| [TC-CU01-003](TC-CU01-003-paginacion.md) | Página de catálogo | FR-003, FR-008 | SC-001 | [lote](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpBatchEstimateAdapterTest.java) | Blocked |
| [TC-CU01-004](TC-CU01-004-formato-invalido.md) | Rechazo de formato inválido | FR-001, FR-010 | SC-004 | Sin prueba de CU-01 | Blocked |
| [TC-CU01-005](TC-CU01-005-cotizacion-no-disponible.md) | Degradación de cotizaciones | FR-003, FR-004 | SC-001 | [lote](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpBatchEstimateAdapterTest.java) | Blocked |
| [TC-CU01-006](TC-CU01-006-disponibilidad-rango.md) | Filtrado por disponibilidad temporal | FR-002 | SC-003 | Sin prueba de CU-01 | Draft |

## Cobertura de requisitos

| FR | Cobertura | TC | Alcance | Motivo / Observación |
|---|---|---|---|---|
| FR-001 | Bloqueado — CU-01 pendiente | 001, 002, 004 | Backend | Criterios opcionales |
| FR-002 | Bloqueado — CU-01 pendiente | 001, 002, 006 | Backend | Catálogo; discrepancia temporal |
| FR-003 | Bloqueado — CU-01 pendiente | 001, 003, 005 | Backend | Cotización lote |
| FR-004 | Bloqueado — CU-01 pendiente | 005 | Backend | Datos backend parciales; presentación UI |
| FR-005 | Sin cobertura backend | — | UI | Selección UI |
| FR-006 | Sin cobertura backend | — | UI | Navegación UI |
| FR-007 | Sin cobertura backend | — | UI | Contexto y contador UI |
| FR-008 | Bloqueado — CU-01 pendiente | 003 | Backend | Página de 20 |
| FR-009 | Bloqueado — CU-01 pendiente | 002 | Backend | Sin filtros |
| FR-010 | Bloqueado — CU-01 pendiente | 004 | Backend | Formato previo a proveedores |


## Criterios de éxito

| SC | Estado | Observación |
|---|---|---|
| SC-001 | Bloqueado | TC-001, 003 y 005; catálogo no implementado. |
| SC-002 | Fuera de alcance | Tiempo de 2000 ms requiere servicios y medición. |
| SC-003 | Bloqueado | TC-002 y 006; contrato de disponibilidad por aclarar. |
| SC-004 | Bloqueado | TC-004; validación previa aún no implementada. |

## Checklist de ejecución

| TC | Documentado | Ejecutado | Resultado | Ejecutado por | Fecha | Observaciones |
|---|---|---|---|---|---|---|
| TC-CU01-001 | Sí | No | Pendiente | — | — | Blocked |
| TC-CU01-002 | Sí | No | Pendiente | — | — | Blocked |
| TC-CU01-003 | Sí | No | Pendiente | — | — | Blocked |
| TC-CU01-004 | Sí | No | Pendiente | — | — | Blocked |
| TC-CU01-005 | Sí | No | Pendiente | — | — | Blocked |
| TC-CU01-006 | Sí | No | Pendiente | — | — | Draft |

## Brechas detectadas

1. [Contexto](../../../context/implementContext.md): CU-01 no tiene endpoint ni orquestación de catálogo y cotización.
2. El spec excluye la disponibilidad por rango en búsqueda; el plan indica filtrar solapamientos locales.
3. CU-09 expone DTO individual reducido; el contrato de catálogo lote M1 aún no está implementado.
4. CU-11 ofrece adaptador lote pero aún no es consumido por CU-01; M3 requiere configuración real.
5. JWT y UI están pendientes; los requisitos visuales requieren pruebas frontend.
6. SLA de 2000 ms requiere medición en condiciones acordadas.

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
| [HttpBatchEstimateAdapterTest](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpBatchEstimateAdapterTest.java) | 10 | 0 | 0 | Exitosa |
