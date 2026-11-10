# CU-19 — Pruebas backend de detalle de embarcación

Casos para los endpoints de detalle y cotización propuestos. CU-19 no está implementado; adaptadores de M1 y M3 son componentes parciales.

**Estado global**: Draft por implementación y contratos pendientes.

## Trazabilidad

| TC | Escenario | FR | SC | Test automatizado parcial | Status |
|---|---|---|---|---|---|
| [TC-CU19-001](TC-CU19-001-ficha-tecnica.md) | Ficha técnica | FR-001, FR-002 | SC-003 | [flota](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/HttpFleetClientTest.java) | Blocked |
| [TC-CU19-002](TC-CU19-002-capacidad-excedida.md) | Capacidad excedida | FR-004, FR-005 | SC-002 | [flota](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/HttpFleetClientTest.java); [cotizacion](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpIndividualEstimateAdapterTest.java) | Blocked |
| [TC-CU19-003](TC-CU19-003-cotizacion-individual.md) | Cotización de detalle | FR-005, FR-006 | SC-001 | [cotizacion](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpIndividualEstimateAdapterTest.java) | Draft |
| [TC-CU19-004](TC-CU19-004-cotizacion-no-disponible.md) | Fallo de cotización | FR-005, FR-007 | SC-001 | [cotizacion](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpIndividualEstimateAdapterTest.java) | Blocked |
| [TC-CU19-005](TC-CU19-005-embarcacion-no-disponible.md) | Fallo de datos de flota | FR-001, FR-002 | SC-003 | [flota](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/HttpFleetClientTest.java) | Blocked |
| [TC-CU19-006](TC-CU19-006-duracion-cotizacion.md) | Duración de un día | FR-005, FR-006 | SC-001 | Sin prueba de CU-19 | Draft |

## Cobertura de requisitos

| FR | Cobertura | TC | Alcance | Motivo / Observación |
|---|---|---|---|---|
| FR-001 | Bloqueado — CU-19 pendiente | 001, 005 | Backend | Identificador |
| FR-002 | Bloqueado — CU-19 pendiente | 001, 005 | Backend | Ficha M1 |
| FR-003 | Sin cobertura backend | — | UI | UI: selectores |
| FR-004 | Bloqueado — CU-19 pendiente | 002 | Backend | Capacidad backend; UI fuera |
| FR-005 | Bloqueado — CU-19 pendiente | 002, 003, 004, 006 | Backend | Cotización |
| FR-006 | Bloqueado — CU-19 pendiente | 003, 006 | Backend | Monto literal; presentación UI fuera |
| FR-007 | Bloqueado — CU-19 pendiente | 004 | Backend | Requiere integración de CU-02 |
| FR-008 | Sin cobertura backend | — | UI | UI: navegación |
| FR-009 | Sin cobertura backend | — | UI | UI: ficha técnica |
| FR-010 | Sin cobertura backend | — | UI | UI: amenidades |


## Criterios de éxito

| SC | Estado | Observación |
|---|---|---|
| SC-001 | Bloqueado | TC-003, 004 y 006; no prueba ausencia global de aritmética. |
| SC-002 | Fuera de alcance UI | TC-002 cubre la validación backend propuesta; requiere prueba UI. |
| SC-003 | Bloqueado | TC-001 y 005; sin persistencia local de ficha. |

## Checklist de ejecución

| TC | Documentado | Ejecutado | Resultado | Ejecutado por | Fecha | Observaciones |
|---|---|---|---|---|---|---|
| TC-CU19-001 | Sí | No | Pendiente | — | — | Blocked |
| TC-CU19-002 | Sí | No | Pendiente | — | — | Blocked |
| TC-CU19-003 | Sí | No | Pendiente | — | — | Draft |
| TC-CU19-004 | Sí | No | Pendiente | — | — | Blocked |
| TC-CU19-005 | Sí | No | Pendiente | — | — | Blocked |
| TC-CU19-006 | Sí | No | Pendiente | — | — | Draft |

## Brechas detectadas

1. [Contexto](../../../context/implementContext.md): CU-19 no está implementado; CU-09 expone DTO parcial y CU-19 aún no consume CU-11.
2. El plan exige quotation_id y quotation_expires_at, pero el contrato individual CU-11 indica que M3 no los devuelve.
3. CU-19 parece rechazar fechas iguales; CU-11 acepta un día inclusivo. Aclarar duración y ejemplo.
4. CU-02 recibe monto del cliente sin verificar origen en cotización; integración aún pendiente.
5. JWT no está implementado; UI requiere pruebas separadas.
6. M1 y M3 requieren configuración real y contratos ratificados.

## Convenciones de estado

- **Draft**: contrato incompleto o inconsistente.
- **In Process**: ejecutable con implementación actual.
- **Approved**: aprobado tras revisión y ejecución; ninguno aquí.
- **Blocked**: depende de funcionalidad pendiente.
