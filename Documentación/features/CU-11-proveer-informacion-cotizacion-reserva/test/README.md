# CU-11 — Pruebas backend de cotización

Casos para estimación individual y en lote de M3; modo fake activo por defecto. CU-01 y CU-19 aún no consumen los puertos.

**Estado global**: Draft por integración y contratos pendientes.

## Trazabilidad

| TC | Escenario | FR | SC | Test automatizado parcial | Status |
|---|---|---|---|---|---|
| [TC-CU11-001](TC-CU11-001-estimacion-individual.md) | Estimación individual | FR-001, FR-002, FR-010, FR-012 | SC-001, SC-002 | [individual](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpIndividualEstimateAdapterTest.java) | In Process |
| [TC-CU11-002](TC-CU11-002-lote-fragmentacion.md) | Fragmentación de lote | FR-003, FR-004, FR-005 | SC-003 | [lote](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpBatchEstimateAdapterTest.java) | In Process |
| [TC-CU11-003](TC-CU11-003-lote-parcial.md) | Estimación parcial | FR-006, FR-007 | SC-004 | [lote](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpBatchEstimateAdapterTest.java) | In Process |
| [TC-CU11-004](TC-CU11-004-lote-fallo.md) | Fallo de bloque en lote | FR-008 | SC-004 | [lote](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpBatchEstimateAdapterTest.java) | In Process |
| [TC-CU11-005](TC-CU11-005-importe-invalido.md) | Importe individual inválido | FR-015, FR-016 | SC-006 | [individual](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpIndividualEstimateAdapterTest.java) | In Process |
| [TC-CU11-006](TC-CU11-006-error-fechas.md) | Error de parámetros M3 | FR-010, FR-011 | SC-005 | [individual](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpIndividualEstimateAdapterTest.java) | In Process |
| [TC-CU11-007](TC-CU11-007-recotizacion.md) | Recotización al cambiar viaje | FR-009, FR-014 | SC-007 | Sin prueba de CU-11 | Blocked |
| [TC-CU11-008](TC-CU11-008-advertencia-obligatoria.md) | Advertencia en interfaz | FR-012, FR-013 | SC-002 | [individual](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpIndividualEstimateAdapterTest.java) | Blocked |

## Cobertura de requisitos

| FR | Cobertura | TC | Alcance | Motivo / Observación |
|---|---|---|---|---|
| FR-001 | Parcial — ver caso y brechas | 001, 002 | Backend | Modos individual y lote |
| FR-002 | Parcial — ver caso y brechas | 001, 002 | Backend | Transporte sin aritmética |
| FR-003 | Parcial — ver caso y brechas | 002 | Backend | Lote |
| FR-004 | Parcial — ver caso y brechas | 002 | Backend | Deduplicación y lista vacía |
| FR-005 | Parcial — ver caso y brechas | 002 | Backend | Fragmentación |
| FR-006 | Parcial — ver caso y brechas | 003 | Backend | Valor de lote; contenido contradictorio |
| FR-007 | Parcial — ver caso y brechas | 003 | Backend | No disponibles |
| FR-008 | Parcial — ver caso y brechas | 004 | Backend | Degradación del lote |
| FR-009 | Parcial — ver caso y brechas | 007 | Backend | CU-19 pendiente |
| FR-010 | Parcial — ver caso y brechas | 001, 006 | Backend | Parámetros y errores |
| FR-011 | Parcial — ver caso y brechas | 006 | Backend | Mensaje específico no conservado |
| FR-012 | Parcial — ver caso y brechas | 001, 008 | Backend | Advertencia y duración parciales |
| FR-013 | Parcial — ver caso y brechas | 008 | Backend | UI y flujo completo pendientes |
| FR-014 | Parcial — ver caso y brechas | 007 | Backend | Recotización pendiente |
| FR-015 | Parcial — ver caso y brechas | 005 | Backend | Importe inválido |
| FR-016 | Parcial — ver caso y brechas | 005 | Backend | Bloqueo del flujo completo pendiente |
| FR-017 | Parcial — ver caso y brechas | 001, 002 | Backend | Solo lectura requiere verificar M3 |

## Criterios de éxito

| SC | Estado | Observación |
|---|---|---|
| SC-001 | Parcial | TC-001 a 003; no prueba todos los precios ni UI. |
| SC-002 | Bloqueado | TC-008; la advertencia no es obligatoria en el adaptador y falta UI. |
| SC-003 | Parcial | TC-002; partición de ejemplo, no garantía universal. |
| SC-004 | Parcial | TC-003 y 004; catálogo CU-01 pendiente. |
| SC-005 | Bloqueado | TC-006; CU-19 pendiente y motivo específico no conservado. |
| SC-006 | Bloqueado | TC-005; CU-03 pendiente y CU-02 no consulta directamente. |
| SC-007 | Bloqueado | TC-007; CU-19 y UI pendientes. |
| SC-008 | Draft | SLA individual y lote pendientes de ratificación; requiere medición. |

## Checklist de ejecución

| TC | Documentado | Ejecutado | Resultado | Ejecutado por | Fecha | Observaciones |
|---|---|---|---|---|---|---|
| TC-CU11-001 | Sí | No | Pendiente | — | — | In Process |
| TC-CU11-002 | Sí | No | Pendiente | — | — | In Process |
| TC-CU11-003 | Sí | No | Pendiente | — | — | In Process |
| TC-CU11-004 | Sí | No | Pendiente | — | — | In Process |
| TC-CU11-005 | Sí | No | Pendiente | — | — | In Process |
| TC-CU11-006 | Sí | No | Pendiente | — | — | In Process |
| TC-CU11-007 | Sí | No | Pendiente | — | — | Blocked |
| TC-CU11-008 | Sí | No | Pendiente | — | — | Blocked |

## Brechas detectadas

1. [Contexto](../../../context/implementContext.md): CU-01 y CU-19 no consumen aún los puertos; falta base-url de M3.
2. La especificación de lote excluye seguro, mientras su prueba independiente y [plan lote](../plan.external.lote.md) indican que lo incluye.
3. El adaptador individual acepta warning ausente y no expone duration_days.
4. La respuesta 400 se mapea a INVALID_PARAMETERS sin preservar el mensaje específico de M3.
5. CU-02 recibe el monto del cliente y no verifica directamente su origen en M3; CU-03 está pendiente.
6. SLA y timeouts de M3 están sujetos a ratificación; modo fake activo por defecto.
7. Recotización, descarte de respuestas obsoletas y UI corresponden a flujos no implementados.

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
| [HttpIndividualEstimateAdapterTest](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpIndividualEstimateAdapterTest.java) | 15 | 0 | 0 | Exitosa |
