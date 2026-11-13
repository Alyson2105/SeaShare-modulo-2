# CU-19 — Pruebas backend de detalle de embarcación

Los endpoints de detalle y cotización están implementados; las pruebas usan dobles y no comprueban la UI ni servicios externos reales.

**Estado global**: Draft por contradicciones contractuales en TC-003 y TC-006; evaluado el 2026-11-13.

## Trazabilidad

| TC | Escenario | FR | SC | Test automatizado parcial | Status |
|---|---|---|---|---|---|
| [TC-CU19-001](TC-CU19-001-ficha-tecnica.md) | Ficha técnica | FR-001, FR-002 | SC-003 | src/test/java/com/seashare/m2/reservas/infrastructure/adapter/in/web/VesselDetailControllerTest.java#vesselDetailReturnsFleetDataWithoutCache | In Process |
| [TC-CU19-002](TC-CU19-002-capacidad-excedida.md) | Capacidad excedida | FR-004, FR-005 | SC-002 | src/test/java/com/seashare/m2/reservas/application/service/VesselDetailServiceTest.java#capacityExceededIsRejectedBeforeM3 | In Process |
| [TC-CU19-003](TC-CU19-003-cotizacion-individual.md) | Cotización de detalle | FR-005, FR-006 | SC-001 | src/test/java/com/seashare/m2/reservas/infrastructure/adapter/in/web/VesselDetailControllerTest.java#quotationReturnsLiteralValuesAndInclusiveDays | Draft |
| [TC-CU19-004](TC-CU19-004-cotizacion-no-disponible.md) | Fallo de cotización | FR-005, FR-007 | SC-001 | src/test/java/com/seashare/m2/reservas/application/service/VesselDetailServiceTest.java#financeFailurePropagates | In Process |
| [TC-CU19-005](TC-CU19-005-embarcacion-no-disponible.md) | Fallo de datos de flota | FR-001, FR-002 | SC-003 | src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/HttpVesselReadAdapterTest.java#notFoundMapsToVesselNotFound | In Process |
| [TC-CU19-006](TC-CU19-006-duracion-cotizacion.md) | Duración de un día | FR-005, FR-006 | SC-001 | Sin prueba del caso de un día | Draft |

## Cobertura de requisitos

| FR | Cobertura | TC | Alcance | Motivo / Observación |
|---|---|---|---|---|
| FR-001 | Parcial — implementación y pruebas con dobles | 001, 005 | Backend | Identificador |
| FR-002 | Parcial — implementación y pruebas con dobles | 001, 005 | Backend | Ficha M1 |
| FR-003 | Sin cobertura backend | — | UI | UI: selectores |
| FR-004 | Parcial — implementación y pruebas con dobles | 002 | Backend | Capacidad backend; UI fuera |
| FR-005 | Parcial — implementación y pruebas con dobles | 002, 003, 004, 006 | Backend | Cotización |
| FR-006 | Parcial — implementación y pruebas con dobles | 003, 006 | Backend | Monto literal; presentación UI fuera |
| FR-007 | Parcial — implementación y pruebas con dobles | 004 | Backend | Requiere integración de CU-02 |
| FR-008 | Sin cobertura backend | — | UI | UI: navegación |
| FR-009 | Sin cobertura backend | — | UI | UI: ficha técnica |
| FR-010 | Sin cobertura backend | — | UI | UI: amenidades |


## Criterios de éxito

| SC | Estado | Observación |
|---|---|---|
| SC-001 | Parcial | TC-003, 004 y 006; no prueba ausencia global de aritmética. |
| SC-002 | Fuera de alcance UI | TC-002 cubre la validación backend; requiere prueba UI. |
| SC-003 | Parcial | TC-001 y 005; sin persistencia local de ficha. |

## Checklist de ejecución

| TC | Documentado | Ejecutado | Resultado | Ejecutado por | Fecha | Observaciones |
|---|---|---|---|---|---|---|
| TC-CU19-001 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | MockMvc verifica ficha y no-store con datos falsos; no verifica ausencia de persistencia ni M1 real. |
| TC-CU19-002 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Prueba unitaria verifica rechazo previo a M3; UI no verificada. |
| TC-CU19-003 | Sí | Sí | Draft | Agente automatizado | 2026-11-13 | MockMvc verifica importe literal y tres días con adaptadores falsos; plan exige quotation_id y vencimiento que CU-11 indica que M3 no devuelve. |
| TC-CU19-004 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Prueba unitaria verifica propagación de QUOTATION_UNAVAILABLE; respuesta HTTP y efectos no verificados. |
| TC-CU19-005 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | WireMock verifica traducción del 404; flujo HTTP completo y ausencia de persistencia no verificados. |
| TC-CU19-006 | Sí | No | Draft | — | — | Plan CU-19 contradice CU-11 sobre fecha final igual e intervalo inclusivo; no hay prueba ejecutada del caso de un día. |

## Brechas detectadas

1. El detalle y la cotización están implementados; falta verificar integración con M1 y M3 reales.
2. El plan exige quotation_id y quotation_expires_at, pero el contrato individual CU-11 indica que M3 no los devuelve.
3. El código acepta fechas iguales y cuenta días inclusivos; el plan de CU-19 contiene reglas y ejemplo contradictorios con CU-11. Aclarar duración.
4. CU-02 recibe monto del cliente sin verificar origen en cotización; integración aún pendiente.
5. JWT no está implementado; UI requiere pruebas separadas.
6. M1 y M3 requieren configuración real y contratos ratificados.

## Convenciones de estado

- **Draft**: contrato incompleto o inconsistente.
- **In Process**: ejecutable con implementación actual.
- **Approved**: aprobado tras revisión y ejecución; ninguno aquí.
- **Blocked**: depende de funcionalidad pendiente.

## Ejecución automatizada — 2026-11-13

`sg docker -c 'bash ./mvnw -B clean test'`: salida 1 por ErrorCodeTest#noDuplicateCodes, sin atribuir ese fallo a CU-19. Los dobles y simulaciones no acreditan servicios externos reales.

| Clase | Tests | Fallos | Errores | Omitidos | Resultado |
|---|---:|---:|---:|---:|---|
| [VesselDetailControllerTest](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/in/web/VesselDetailControllerTest.java) | 7 | 0 | 0 | 0 | Verde; cobertura según TC |
| [VesselDetailServiceTest](../../../../src/test/java/com/seashare/m2/reservas/application/service/VesselDetailServiceTest.java) | 8 | 0 | 0 | 0 | Verde; cobertura según TC |
| [HttpVesselReadAdapterTest](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/HttpVesselReadAdapterTest.java) | 3 | 0 | 0 | 0 | Verde; cobertura según TC |
| [HttpIndividualEstimateAdapterTest](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpIndividualEstimateAdapterTest.java) | 15 | 0 | 0 | 0 | Verde; cobertura según TC |
