# CU-01 — Pruebas backend de búsqueda de embarcaciones

Casos del catálogo y cotización en lote. CU-01 dispone de endpoint y orquestación; las pruebas de búsqueda usan dobles de los proveedores.

**Estado global**: Draft; TC-006 conserva la contradicción entre spec y plan. Evaluado el 2026-11-13.

## Trazabilidad

| TC | Escenario | FR | SC | Test automatizado | Status |
|---|---|---|---|---|---|
| [TC-CU01-001](TC-CU01-001-busqueda-con-filtros.md) | Búsqueda con filtros | FR-001, FR-002, FR-003 | SC-001 | [lote](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpBatchEstimateAdapterTest.java); [VesselSearchServiceTest](../../../../src/test/java/com/seashare/m2/reservas/application/service/VesselSearchServiceTest.java) | In Process |
| [TC-CU01-002](TC-CU01-002-busqueda-sin-filtros.md) | Catálogo sin filtros | FR-001, FR-002, FR-009 | SC-003 | [lote](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpBatchEstimateAdapterTest.java); [VesselSearchControllerTest](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/in/web/VesselSearchControllerTest.java) | In Process |
| [TC-CU01-003](TC-CU01-003-paginacion.md) | Página de catálogo | FR-003, FR-008 | SC-001 | [lote](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpBatchEstimateAdapterTest.java); [VesselSearchControllerTest](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/in/web/VesselSearchControllerTest.java) | In Process |
| [TC-CU01-004](TC-CU01-004-formato-invalido.md) | Rechazo de formato inválido | FR-001, FR-010 | SC-004 | Sin prueba de CU-01; [VesselSearchControllerTest](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/in/web/VesselSearchControllerTest.java) | In Process |
| [TC-CU01-005](TC-CU01-005-cotizacion-no-disponible.md) | Degradación de cotizaciones | FR-003, FR-004 | SC-001 | [lote](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpBatchEstimateAdapterTest.java); [VesselSearchServiceTest](../../../../src/test/java/com/seashare/m2/reservas/application/service/VesselSearchServiceTest.java) | In Process |
| [TC-CU01-006](TC-CU01-006-disponibilidad-rango.md) | Filtrado por disponibilidad temporal | FR-002 | SC-003 | Sin prueba de CU-01; [VesselSearchServiceTest](../../../../src/test/java/com/seashare/m2/reservas/application/service/VesselSearchServiceTest.java) | Draft |

## Cobertura de requisitos

| FR | Cobertura | TC | Alcance | Motivo / Observación |
|---|---|---|---|---|
| FR-001 | Parcial | 001, 002, 004 | Backend | Criterios opcionales |
| FR-002 | Parcial | 001, 002, 006 | Backend | Catálogo; discrepancia temporal |
| FR-003 | Parcial | 001, 003, 005 | Backend | Cotización lote |
| FR-004 | Parcial | 005 | Backend | Datos backend parciales; presentación UI |
| FR-005 | Sin cobertura backend | — | UI | Selección UI |
| FR-006 | Sin cobertura backend | — | UI | Navegación UI |
| FR-007 | Sin cobertura backend | — | UI | Contexto y contador UI |
| FR-008 | Parcial | 003 | Backend | Página de 20 |
| FR-009 | Parcial | 002 | Backend | Sin filtros |
| FR-010 | Parcial | 004 | Backend | Formato previo a proveedores |


## Criterios de éxito

| SC | Estado | Observación |
|---|---|---|
| SC-001 | Parcial | Búsqueda y cotización con dobles probadas; falta demostrar todos los resultados con M3 real. |
| SC-002 | Fuera de alcance | Tiempo de 2000 ms requiere servicios y medición. |
| SC-003 | Parcial | Primera página sin filtros probada con catálogo simulado; falta demostrar el catálogo completo. |
| SC-004 | Parcial | Rechazos HTTP probados; no se verifican todas las entradas ni la ausencia de llamadas. |

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
- **Approved**: aprobado tras revisión y ejecución; ninguno aquí.
- **Blocked**: depende de funcionalidad pendiente.

## Ejecución automatizada — 2026-11-13

`./mvnw -B clean test`: 395 pruebas, 0 fallos, 0 errores, 0 omitidas; BUILD SUCCESS. Las pruebas de CU-01 indicadas usan dobles locales; no validan servicios reales.

| Clase | Pruebas | Fallos | Errores | Omitidos | Resultado |
|---|---:|---:|---:|---:|---|
| [VesselSearchServiceTest](../../../../src/test/java/com/seashare/m2/reservas/application/service/VesselSearchServiceTest.java) | 7 | 0 | 0 | 0 | Exitosa |
| [VesselSearchControllerTest](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/in/web/VesselSearchControllerTest.java) | 7 | 0 | 0 | 0 | Exitosa |
| [HttpBatchEstimateAdapterTest](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpBatchEstimateAdapterTest.java) | 10 | 0 | 0 | 0 | Exitosa |
