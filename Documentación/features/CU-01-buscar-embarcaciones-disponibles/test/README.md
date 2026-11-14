# CU-01 — Pruebas backend de búsqueda de embarcaciones

Casos del catálogo y cotización en lote. CU-01 dispone de endpoint y orquestación; las pruebas de búsqueda usan dobles de los proveedores.

**Estado global**: Draft; TC-006 conserva la contradicción entre spec y plan. Evaluado el 2026-11-13.

## Trazabilidad

| TC | Escenario | FR | SC | Test automatizado | Status |
|---|---|---|---|---|---|
| [TC-CU01-001](TC-CU01-001-busqueda-con-filtros.md) | Búsqueda con filtros | FR-001, FR-002, FR-003 | SC-001 | [lote](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpBatchEstimateAdapterTest.java); [VesselSearchServiceTest](../../../../src/test/java/com/seashare/m2/reservas/application/service/VesselSearchServiceTest.java) | In Process |
| [TC-CU01-002](TC-CU01-002-busqueda-sin-filtros.md) | Catálogo sin filtros | FR-001, FR-002, FR-009 | SC-003 | [lote](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpBatchEstimateAdapterTest.java); [VesselSearchControllerTest](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/in/web/VesselSearchControllerTest.java) | In Process |
| [TC-CU01-003](TC-CU01-003-paginacion.md) | Página de catálogo | FR-003, FR-008 | SC-001 | [lote](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpBatchEstimateAdapterTest.java); [VesselSearchControllerTest](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/in/web/VesselSearchControllerTest.java) | In Process |
| [TC-CU01-004](TC-CU01-004-formato-invalido.md) | Rechazo de formato inválido | FR-001, FR-010 | SC-004 | [VesselSearchControllerTest](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/in/web/VesselSearchControllerTest.java) | In Process |
| [TC-CU01-005](TC-CU01-005-cotizacion-no-disponible.md) | Degradación de cotizaciones | FR-003, FR-004 | SC-001 | [lote](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpBatchEstimateAdapterTest.java); [VesselSearchServiceTest](../../../../src/test/java/com/seashare/m2/reservas/application/service/VesselSearchServiceTest.java) | In Process |
| [TC-CU01-006](TC-CU01-006-disponibilidad-rango.md) | Filtrado por disponibilidad temporal | FR-002 | SC-003 | [VesselSearchServiceTest](../../../../src/test/java/com/seashare/m2/reservas/application/service/VesselSearchServiceTest.java) | Draft |

## Cobertura de requisitos

| FR | Cobertura | TC | Alcance | Motivo / Observación |
|---|---|---|---|---|
| FR-001 | Parcial | 001, 002, 004 | Backend | Criterios opcionales |
| FR-002 | Parcial | 001, 002, 006 | Backend | Catálogo; discrepancia temporal |
| FR-003 | Parcial | 001, 003, 005 | Backend | Cotización lote |
| FR-004 | Parcial | 005 | Backend | Datos backend parciales; presentación UI |
| FR-005 | Sin evidencia | — | UI | Selección UI |
| FR-006 | Sin evidencia | — | UI | Navegación UI |
| FR-007 | Sin evidencia | — | UI | Contexto y contador UI |
| FR-008 | Parcial | 003 | Backend | Página de 20 |
| FR-009 | Parcial | 002 | Backend | Sin filtros |
| FR-010 | Parcial | 004 | Backend | Formato previo a proveedores |


## Criterios de éxito

| SC | Estado | Observación |
|---|---|---|
| SC-001 | Parcialmente demostrado | Búsqueda y cotización con dobles probadas; falta demostrar todos los resultados con M3 real. |
| SC-002 | No demostrado | Tiempo de 2000 ms requiere servicios y medición. |
| SC-003 | Parcialmente demostrado | Primera página sin filtros probada con catálogo simulado; falta demostrar el catálogo completo. |
| SC-004 | Parcialmente demostrado | Rechazos HTTP probados; no se verifican todas las entradas ni la ausencia de llamadas. |

## Checklist de ejecución

| TC | Documentado | Ejecutado | Resultado | Ejecutado por | Fecha | Observaciones |
|---|---|---|---|---|---|---|
| TC-CU01-001 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Filtros de tipo y capacidad con dobles; combinación completa pendiente. |
| TC-CU01-002 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Primera página sin filtros probada; catálogo completo pendiente. |
| TC-CU01-003 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Página de 20 probada; IDs enviados a M3 no observados. |
| TC-CU01-004 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Tres rechazos HTTP probados; faltan llamadas observables y pasajeros no numéricos. |
| TC-CU01-005 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Degradación unitaria; serialización HTTP no verificada. |
| TC-CU01-006 | Sí | Sí | Draft | Agente automatizado | 2026-11-13 | Pruebas demuestran filtrado implementado; spec y plan se contradicen. |

## Brechas detectadas

2. El spec excluye la disponibilidad por rango en búsqueda; el plan indica filtrar solapamientos locales.
5. JWT y UI están pendientes; los requisitos visuales requieren pruebas frontend.
6. SLA de 2000 ms requiere medición en condiciones acordadas.

## Convenciones de estado

- **Draft**: contrato incompleto o inconsistente.
- **In Process**: ejecutable con implementación actual.
- **Approved**: escenario y postcondiciones demostrados por aserciones de la corrida V4, sin impedimentos de aprobación.
- **Blocked**: depende de funcionalidad pendiente.

## Ejecución automatizada — 2026-11-13

`sg docker -c 'bash ./mvnw -B clean test'`; inicio 2026-11-13T19:02:00-05:00, fin 2026-11-13T19:03:11-05:00; BASELINE_HEAD `568290a0d4334b477a4b0f9d5e949e981da3fa55`; salida 0. Las pruebas de búsqueda usan puertos simulados; WireMock no demuestra disponibilidad de M3 real.

| Clase | Pruebas | Fallos | Errores | Omitidos | Alcance |
|---|---:|---:|---:|---:|---|
| com.seashare.m2.reservas.application.service.VesselSearchServiceTest | 7 | 0 | 0 | 0 | Evidencia parcial según TC |
| com.seashare.m2.reservas.infrastructure.adapter.in.web.VesselSearchControllerTest | 7 | 0 | 0 | 0 | Evidencia parcial según TC |
| com.seashare.m2.reservas.infrastructure.adapter.out.finance.HttpBatchEstimateAdapterTest | 10 | 0 | 0 | 0 | Evidencia parcial según TC |

**Decisión aprobada 2026-11-13 — TC-CU01-006-disponibilidad-rango.md:** Se aprobó filtrar en M2 por reservas locales solapadas cuando existan ambas fechas, conservando la validación transaccional posterior.
