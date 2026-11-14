# CU-11 — Pruebas backend de cotización

Casos para estimación individual y en lote de M3; modo fake activo por defecto. CU-01 y CU-19 consumen respectivamente los puertos de lote e individual.

**Estado global**: Draft; TC-003 presenta contradicción contractual, TC-007 carece del flujo de recotización y TC-008 conserva evidencia parcial. Evaluado el 2026-11-13.

## Trazabilidad

| TC | Escenario | FR | SC | Test automatizado parcial | Status |
|---|---|---|---|---|---|
| [TC-CU11-001](TC-CU11-001-estimacion-individual.md) | Estimación individual | FR-001, FR-002, FR-010, FR-012 | SC-001, SC-002 | [individual](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpIndividualEstimateAdapterTest.java) | In Process |
| [TC-CU11-002](TC-CU11-002-lote-fragmentacion.md) | Fragmentación de lote | FR-003, FR-004, FR-005 | SC-003 | [lote](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpBatchEstimateAdapterTest.java) | In Process |
| [TC-CU11-003](TC-CU11-003-lote-parcial.md) | Estimación parcial | FR-006, FR-007 | SC-004 | [lote](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpBatchEstimateAdapterTest.java) | Draft |
| [TC-CU11-004](TC-CU11-004-lote-fallo.md) | Fallo de bloque en lote | FR-008 | SC-004 | [lote](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpBatchEstimateAdapterTest.java) | In Process |
| [TC-CU11-005](TC-CU11-005-importe-invalido.md) | Importe individual inválido | FR-015, FR-016 | SC-006 | [individual](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpIndividualEstimateAdapterTest.java) | In Process |
| [TC-CU11-006](TC-CU11-006-error-fechas.md) | Error de parámetros M3 | FR-010, FR-011 | SC-005 | [individual](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpIndividualEstimateAdapterTest.java) | In Process |
| [TC-CU11-007](TC-CU11-007-recotizacion.md) | Recotización al cambiar viaje | FR-009, FR-014 | SC-007 | Sin prueba de CU-11 | Blocked |
| [TC-CU11-008](TC-CU11-008-advertencia-obligatoria.md) | Advertencia en interfaz | FR-012, FR-013 | SC-002 | [individual](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpIndividualEstimateAdapterTest.java) | In Process |

## Cobertura de requisitos

| FR | Cobertura | TC | Alcance | Motivo / Observación |
|---|---|---|---|---|
| FR-001 | Parcial | 001, 002 | Backend | Modos individual y lote |
| FR-002 | Parcial | 001, 002 | Backend | Transporte sin aritmética |
| FR-003 | Parcial | 002 | Backend | Lote |
| FR-004 | Parcial | 002 | Backend | Deduplicación y lista vacía |
| FR-005 | Parcial | 002 | Backend | Fragmentación |
| FR-006 | Parcial | 003 | Backend | Valor de lote; contenido contradictorio |
| FR-007 | Parcial | 003 | Backend | No disponibles |
| FR-008 | Parcial | 004 | Backend | Degradación del lote |
| FR-009 | Parcial | 007 | Backend | Recotización UI pendiente |
| FR-010 | Parcial | 001, 006 | Backend | Parámetros y errores |
| FR-011 | Parcial | 006 | Backend | Mensaje específico no conservado |
| FR-012 | Parcial | 001, 008 | Backend | Advertencia y duración parciales |
| FR-013 | Parcial | 008 | Backend | UI y flujo completo pendientes |
| FR-014 | Parcial | 007 | Backend | Recotización pendiente |
| FR-015 | Parcial | 005 | Backend | Importe inválido |
| FR-016 | Parcial | 005 | Backend | Bloqueo del flujo completo pendiente |
| FR-017 | Parcial | 001, 002 | Backend | Solo lectura requiere verificar M3 |

## Criterios de éxito

| SC | Estado | Observación |
|---|---|---|
| SC-001 | Parcialmente demostrado | TC-001 a 003; no prueba todos los precios ni UI. |
| SC-002 | Parcialmente demostrado | TC-008; la advertencia no es obligatoria en el adaptador y falta UI. |
| SC-003 | Parcialmente demostrado | TC-002; partición de ejemplo, no garantía universal. |
| SC-004 | Parcialmente demostrado | TC-003 y 004; catálogo backend existente; falta UI. |
| SC-005 | Parcialmente demostrado | TC-006 prueba el rechazo del adaptador; no conserva el motivo específico ni demuestra el flujo completo. |
| SC-006 | Parcialmente demostrado | TC-005; CU-03 implementado; CU-02 no consulta directamente. |
| SC-007 | Bloqueado | TC-007; CU-19 implementado; UI pendiente. |
| SC-008 | No verificable | SLA individual y lote pendientes de ratificación; requiere medición. |

## Checklist de ejecución

| TC | Documentado | Ejecutado | Resultado | Ejecutado por | Fecha | Observaciones |
|---|---|---|---|---|---|---|
| TC-CU11-001 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Componente WireMock: HttpIndividualEstimateAdapterTest#successful200WithWarning pasó: monto y warning arbitrario se conservan. No verifica todos los parámetros ni el texto contractual obligatorio. |
| TC-CU11-002 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Componente WireMock: HttpBatchEstimateAdapterTest#duplicateIdsAreDeduplicatedBeforeSend y #sixtyIdsSplitIntoTwoBlocksOfFifty pasaron: deduplicación y 60 IDs en dos llamadas. El caso requiere 120 UUID más duplicados; solo se prueban 60 y duplicados en pruebas separadas, sin inspección del tamaño de los cuerpos. |
| TC-CU11-003 | Sí | Sí | Draft | Agente automatizado | 2026-11-13 | Componente WireMock: HttpBatchEstimateAdapterTest#batch200PartialMarksUnavailable, #unknownIdInResponseBecomesUnavailable y #invalidEstimatedTotalMakesVesselUnavailable pasaron. El contrato de contenido del precio lote es contradictorio; no se prueba composición financiera. |
| TC-CU11-004 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Componente WireMock: HttpBatchEstimateAdapterTest#failedBlockMakesItsIdsUnavailableAndKeepsGoing y #readTimeoutMarksBlockUnavailableWithSingleCall pasaron: otros bloques siguen y timeout se consulta una vez. No se verifica UI real. |
| TC-CU11-005 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Componente WireMock: HttpIndividualEstimateAdapterTest#zeroTotalThrowsQuotationUnavailable, #negativeTotalThrowsQuotationUnavailable y #unparseableTotalThrowsQuotationUnavailable pasaron. Falta demostrar bloqueo completo de reserva. |
| TC-CU11-006 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Componente WireMock: HttpIndividualEstimateAdapterTest#badRequest400ThrowsInvalidParameters pasó: código INVALID_PARAMETERS y una llamada. No comprueba mensaje específico ni ausencia de reserva. |
| TC-CU11-007 | Sí | No | Blocked | — | — | Sin ejecución automatizada; recotización y descarte en UI pendientes. |
| TC-CU11-008 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Componente WireMock: HttpIndividualEstimateAdapterTest#successful200WithWarning pasó con warning arbitrario y #successful200WithoutWarning pasó aceptando null; unitario: VesselDetailServiceTest#quotationUsesLiteralAmountAndInclusiveDays pasó. Falta UI, modal y obligación del texto contractual. |

## Brechas detectadas

2. La especificación de lote excluye seguro, mientras su prueba independiente y [plan lote](../plan.external.lote.md) indican que lo incluye.
3. El adaptador individual acepta warning ausente y no expone duration_days.
4. La respuesta 400 se mapea a INVALID_PARAMETERS sin preservar el mensaje específico de M3.
5. CU-02 recibe el monto del cliente y no verifica directamente su origen en M3; CU-03 está implementado.
6. SLA y timeouts de M3 están sujetos a ratificación; modo fake activo por defecto.
7. Recotización, descarte de respuestas obsoletas y UI corresponden a flujos no implementados.

## Convenciones de estado

- **Draft**: contrato incompleto o inconsistente.
- **In Process**: ejecutable con implementación actual.
- **Approved**: escenario y postcondiciones demostrados por aserciones de la corrida V4, sin impedimentos de aprobación.
- **Blocked**: depende de funcionalidad pendiente.

## Ejecución automatizada — 2026-11-13

`sg docker -c 'bash ./mvnw -B clean test'`; inicio 2026-11-13T19:02:00-05:00, fin 2026-11-13T19:03:11-05:00; BASELINE_HEAD `568290a0d4334b477a4b0f9d5e949e981da3fa55`; salida 0. Las pruebas de adaptador usan WireMock; no acreditan integración con M3 real.

| Clase | Pruebas | Fallos | Errores | Omitidos | Alcance |
|---|---:|---:|---:|---:|---|
| com.seashare.m2.reservas.application.service.VesselDetailServiceTest | 8 | 0 | 0 | 0 | Evidencia parcial según TC |
| com.seashare.m2.reservas.application.service.VesselSearchServiceTest | 7 | 0 | 0 | 0 | Evidencia parcial según TC |
| com.seashare.m2.reservas.infrastructure.adapter.out.finance.HttpBatchEstimateAdapterTest | 10 | 0 | 0 | 0 | Evidencia parcial según TC |
| com.seashare.m2.reservas.infrastructure.adapter.out.finance.HttpIndividualEstimateAdapterTest | 15 | 0 | 0 | 0 | Evidencia parcial según TC |
