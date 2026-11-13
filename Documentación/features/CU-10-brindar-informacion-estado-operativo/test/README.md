# CU-10 — Pruebas backend de estado operativo

Casos para FleetOperationalStatusPort y su adaptador HTTP de M1; modo fake activo por defecto. El contrato de M1 sigue pendiente de firma.

**Estado global**: In Process; siete casos tienen cobertura parcial. Evaluado el 2026-11-13.

## Trazabilidad

| TC | Escenario | FR | SC | Test automatizado parcial | Status |
|---|---|---|---|---|---|
| [TC-CU10-001](TC-CU10-001-estado-disponible.md) | Estado disponible | FR-001, FR-002, FR-003, FR-004 | SC-002 | [cliente](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/HttpFleetClientTest.java) | In Process |
| [TC-CU10-002](TC-CU10-002-estados-no-disponibles.md) | Estados no disponibles | FR-003, FR-004, FR-005 | SC-002 | [cliente](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/HttpFleetClientTest.java); [reserva](../../../../src/test/java/com/seashare/m2/reservas/integration/CreateReservationTest.java) | In Process |
| [TC-CU10-003](TC-CU10-003-estado-desconocido.md) | Estado desconocido | FR-003, FR-004, FR-009 | SC-002 | [cliente](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/HttpFleetClientTest.java) | In Process |
| [TC-CU10-004](TC-CU10-004-respuesta-invalida.md) | Respuesta ilegible | FR-002, FR-006 | SC-003 | [fallos](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/FleetUnavailableTest.java) | In Process |
| [TC-CU10-005](TC-CU10-005-fallo-conexion.md) | Reintento y fallo seguro | FR-002, FR-006 | SC-003 | [cliente](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/HttpFleetClientTest.java); [fallos](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/FleetUnavailableTest.java) | In Process |
| [TC-CU10-006](TC-CU10-006-embarcacion-inexistente.md) | Embarcación inexistente | FR-001, FR-002, FR-006 | SC-005 | [fallos](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/FleetUnavailableTest.java) | In Process |
| [TC-CU10-007](TC-CU10-007-registro-estado.md) | Registro de consulta | FR-009 | SC-004 | [registro](../../../../src/test/java/com/seashare/m2/reservas/integration/ExternalQueryLogRegistrationTest.java) | In Process |

## Cobertura de requisitos

| FR | Cobertura | TC | Alcance | Motivo / Observación |
|---|---|---|---|---|
| FR-001 | Parcial — ver caso y brechas | 001, 006 | Backend | Identificador |
| FR-002 | Parcial — ver caso y brechas | 001, 004, 005, 006 | Backend | Consulta HTTP |
| FR-003 | Parcial — ver caso y brechas | 001, 002, 003 | Backend | Estados |
| FR-004 | Parcial — ver caso y brechas | 001, 002, 003 | Backend | Solo AVAILABLE es apto |
| FR-005 | Parcial — ver caso y brechas | 002 | Backend | El booleano no informa motivo |
| FR-006 | Parcial — ver caso y brechas | 004, 005, 006 | Backend | Fallo seguro |
| FR-007 | Parcial — ver caso y brechas | 001 | Backend | Sin acceso directo requiere auditoría integral |
| FR-008 | Parcial — ver caso y brechas | 001 | Backend | Sin dinero requiere auditoría integral |
| FR-009 | Parcial — ver caso y brechas | 003, 007 | Backend | Registro de consulta |

## Criterios de éxito

| SC | Estado | Observación |
|---|---|---|
| SC-001 | Fuera de alcance | Timeout de lectura no demuestra latencia extremo a extremo menor a 300 ms. |
| SC-002 | Parcial | TC-001 a 003; no demuestra todas las reservas. |
| SC-003 | Parcial | TC-004 y 005; no cubre toda falla posible. |
| SC-004 | Parcial | TC-001 y 007; requiere auditoría integral de accesos y cálculos. |
| SC-005 | Parcial | TC-006; no demuestra todas las consultas inexistentes. |

## Checklist de ejecución

| TC | Documentado | Ejecutado | Resultado | Ejecutado por | Fecha | Observaciones |
|---|---|---|---|---|---|---|
| TC-CU10-001 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Componente WireMock: HttpFleetClientTest#availableStatusIsTrue pasó: AVAILABLE produce true y se verifica ruta y token. No se consulta M1 real. |
| TC-CU10-002 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Componente WireMock: HttpFleetClientTest#nonAvailableStatusesAreFalse pasó para los tres estados; integración: CreateReservationValidationTest#vesselNotAvailableIs409 pasó con puerto simulado. Falta motivo específico por estado. |
| TC-CU10-003 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Integración: ExternalQueryLogRegistrationTest#statusUnknownRawStatusRegistersOkRow pasó: false y registro OK con valor crudo WEIRD_STATE. M1 se simula con WireMock. |
| TC-CU10-004 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Integración: ExternalQueryLogRegistrationTest#statusEmptyBodyThrowsAndRegistersSingleErrorRow, #statusInvalidJsonThrowsAndRegistersSingleErrorRow y #statusMissingFieldThrowsAndRegistersSingleErrorRow pasaron: error FLEET_SERVICE_UNAVAILABLE y registro ERROR. |
| TC-CU10-005 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Integración: ExternalQueryLogRegistrationTest#statusTimeoutRegistersTimeoutRow pasó: timeout produce FLEET_SERVICE_UNAVAILABLE y registro TIMEOUT. El test no cuenta intentos ni comprueba 503 persistente. |
| TC-CU10-006 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Componente WireMock: FleetUnavailableTest#operationalStatusNotFoundOn404 pasó: VESSEL_NOT_FOUND y una llamada; falta verificar respuesta de cada consumidor. |
| TC-CU10-007 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Integración: ExternalQueryLogRegistrationTest#statusAvailableRegistersOkWithRawStatus pasó: estado AVAILABLE, resultado OK y tipo OPERATIONAL_STATUS persistidos. No demuestra registro ante toda falla de almacenamiento. |

## Brechas detectadas

1. [Contexto](../../../context/implementContext.md): contrato M1 no firmado y modo fake predeterminado.
2. El puerto devuelve booleano: FR-005 pide comunicar el motivo de indisponibilidad, que no llega al consumidor.
3. El plan menciona reintentos 500/502; el cliente solo reintenta 503 y errores de acceso.
4. Un 503 persistente se registra como TIMEOUT y un fallo de auditoría no detiene la consulta.
5. Latencia y disponibilidad real de M1 requieren medición externa; no se infieren del timeout.
6. Consulta puntual no garantiza exclusividad FCFS; CU-03 implementa el inicio de pago y requiere prueba de concurrencia simultánea.

## Convenciones de estado

- **Draft**: contrato incompleto o inconsistente.
- **In Process**: ejecutable con implementación actual.
- **Approved**: aprobado tras revisión y ejecución; ninguno aquí.
- **Blocked**: depende de funcionalidad pendiente.

## Ejecución automatizada — 2026-11-13

`sg docker -c 'bash ./mvnw -B clean test'`: salida 1 por ErrorCodeTest#noDuplicateCodes, sin atribuir ese fallo a CU-10. Las pruebas citadas usan las dependencias indicadas en cada caso; no acreditan servicios externos reales.

| Clase | Pruebas | Fallos | Errores | Omitidos | Resultado |
|---|---:|---:|---:|---:|---|
| [HttpFleetClientTest](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/HttpFleetClientTest.java) | 5 | 0 | 0 | 0 | Verde; cobertura según TC |
| [FleetUnavailableTest](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/FleetUnavailableTest.java) | 10 | 0 | 0 | 0 | Verde; cobertura según TC |
| [ExternalQueryLogRegistrationTest](../../../../src/test/java/com/seashare/m2/reservas/integration/ExternalQueryLogRegistrationTest.java) | 19 | 0 | 0 | 0 | Verde; cobertura según TC |
| [CreateReservationValidationTest](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/in/web/CreateReservationValidationTest.java) | 13 | 0 | 0 | 0 | Verde; cobertura según TC |
