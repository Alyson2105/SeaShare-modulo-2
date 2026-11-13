# CU-09 — Pruebas backend de información de embarcación

Casos para FleetInfoPort y su adaptador HTTP de M1; modo fake activo por defecto. Los contratos de M1 requieren confirmación; existen consumidores de catálogo y detalle con cobertura parcial.

**Estado global**: In Process; TC-007 está bloqueado por falta de matrícula; se conserva evidencia parcial. Evaluado el 2026-11-13.

## Trazabilidad

| TC | Escenario | FR | SC | Test automatizado parcial | Status |
|---|---|---|---|---|---|
| [TC-CU09-001](TC-CU09-001-consulta-individual.md) | Consulta de datos de embarcación | FR-001, FR-003, FR-004 | SC-003, SC-005 | [cliente](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/HttpFleetClientTest.java) | In Process |
| [TC-CU09-002](TC-CU09-002-capacidad-reserva.md) | Rechazo por exceso de capacidad | FR-002, FR-005, FR-006 | SC-002 | [reserva](../../../../src/test/java/com/seashare/m2/reservas/integration/CreateReservationTest.java) | In Process |
| [TC-CU09-003](TC-CU09-003-embarcacion-inexistente.md) | Embarcación no encontrada | FR-003, FR-007 | SC-004 | [fallos](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/FleetUnavailableTest.java) | In Process |
| [TC-CU09-004](TC-CU09-004-fallo-transitorio.md) | Reintento y fallo seguro | FR-003, FR-008 | SC-004 | [cliente](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/HttpFleetClientTest.java); [fallos](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/FleetUnavailableTest.java) | In Process |
| [TC-CU09-005](TC-CU09-005-zona-horaria-invalida.md) | Zona horaria ausente o inválida | FR-004, FR-008 | SC-004, SC-005 | [fallos](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/FleetUnavailableTest.java) | In Process |
| [TC-CU09-006](TC-CU09-006-registro-consulta.md) | Registro de consulta | FR-010 | SC-003 | [registro](../../../../src/test/java/com/seashare/m2/reservas/integration/ExternalQueryLogRegistrationTest.java) | In Process |
| [TC-CU09-007](TC-CU09-007-ficha-completa.md) | Ficha completa y ubicación GPS | FR-001, FR-004 | SC-005 | [detalle](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/HttpVesselReadAdapterTest.java); [consumidor](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/in/web/VesselDetailControllerTest.java) | Blocked |

## Cobertura de requisitos

| FR | Cobertura | TC | Alcance | Motivo / Observación |
|---|---|---|---|---|
| FR-001 | Parcial — ver caso y brechas | 001, 007 | Backend | Interfaz y consumidores implementados; pruebas con dobles |
| FR-002 | Parcial — ver caso y brechas | 002 | Backend | Pasajeros validados por CU-02 |
| FR-003 | Parcial — ver caso y brechas | 001, 003, 004 | Backend | Consulta HTTP |
| FR-004 | Parcial — ver caso y brechas | 001, 005, 007 | Backend | DTO reducido y detalle con GPS; matrícula pendiente |
| FR-005 | Parcial — ver caso y brechas | 002 | Backend | Capacidad en CU-02 |
| FR-006 | Parcial — ver caso y brechas | 002 | Backend | Exceso en CU-02 |
| FR-007 | Parcial — ver caso y brechas | 003 | Backend | 404 sin reintento |
| FR-008 | Parcial — ver caso y brechas | 004, 005 | Backend | Fallo seguro; divergencia de reintentos |
| FR-009 | Parcial — ver caso y brechas | 001 | Backend | Regla sin dinero requiere auditoría integral |
| FR-010 | Parcial — ver caso y brechas | 006 | Backend | Registro de consulta |

## Criterios de éxito

| SC | Estado | Observación |
|---|---|---|
| SC-001 | Fuera de alcance | Un timeout de lectura no demuestra latencia de extremo a extremo menor a 300 ms. |
| SC-002 | Parcial | TC-002 cubre rechazo en CU-02; no todas las operaciones. |
| SC-003 | Parcial | TC-001 y 006; ausencia global de acceso directo y cálculos requiere auditoría. |
| SC-004 | Parcial | TC-003 a 005; no demuestra todas las fallas. |
| SC-005 | Parcial | TC-001 y 005 cubren zona; GPS en detalle probado con WireMock; matrícula pendiente y M1 real sin verificar. |

## Checklist de ejecución

| TC | Documentado | Ejecutado | Resultado | Ejecutado por | Fecha | Observaciones |
|---|---|---|---|---|---|---|
| TC-CU09-001 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Componente WireMock: HttpFleetClientTest#mapsVesselInfoFromSuccessfulResponse y #sendsInternalServiceTokenHeader pasaron: propietario, capacidad, zona y cabecera. M1 real no fue consultado. |
| TC-CU09-002 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Integración: CreateReservationTest#rejectsPassengersOverCapacity pasó: 409 CAPACITY_EXCEEDED y cero reservas del arrendatario; flota simulada. La validación se realiza en CU-02. |
| TC-CU09-003 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Componente WireMock: FleetUnavailableTest#vesselNotFoundOn404WithoutRetry pasó: VESSEL_NOT_FOUND y una llamada. No comprueba la respuesta de todos los consumidores. |
| TC-CU09-004 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Componente WireMock: FleetUnavailableTest#double503FailsSafeAfterSingleRetry y #timeoutRetriesOnceThenFailsSafe pasaron con dos intentos y fallo seguro. ExternalQueryLogRegistrationTest#vesselServerError500RegistersErrorRow pasó con un solo intento; el plan exige reintentar 500/502. |
| TC-CU09-005 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Componente WireMock: FleetUnavailableTest#missingHarborTimezoneFailsSafe y #invalidHarborTimezoneFailsSafe pasaron con HARBOR_TIMEZONE_UNAVAILABLE. No verifica todos los consumidores ni M1 real. |
| TC-CU09-006 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Integración: ExternalQueryLogRegistrationTest#vesselInfoOkRegistersOkRow y #registrationFailureDoesNotBreakQuery pasaron: fila OK y consulta exitosa con registro fallido. No garantiza trazabilidad ante fallos del almacén. |
| TC-CU09-007 | Sí | Sí | Blocked | Agente automatizado | 2026-11-13 | Componente WireMock: HttpVesselReadAdapterTest#mapsFullDetail pasó para nombre, tipo, capacidad, puerto y GPS; VesselDetailControllerTest#vesselDetailReturnsFleetDataWithoutCache pasó con adaptador fake. Falta matrícula y validación integral con M1 real. |

## Brechas detectadas

1. [Contexto](../../../context/implementContext.md): contrato M1 no firmado y modo fake predeterminado.
4. El plan sección 4 menciona 500/502, mientras la sección 5 enumera 503 y errores de acceso; el cliente no reintenta 500/502.
5. Un 503 persistente se registra como TIMEOUT y un fallo de auditoría no detiene la consulta.
6. Latencia y disponibilidad real de M1 requieren medición externa; no se infieren del timeout.

## Convenciones de estado

- **Draft**: contrato incompleto o inconsistente.
- **In Process**: ejecutable con implementación actual.
- **Approved**: aprobado tras revisión y ejecución; ninguno aquí.
- **Blocked**: depende de funcionalidad pendiente.

## Ejecución automatizada — 2026-11-13

`sg docker -c 'bash ./mvnw -B clean test'`: salida 1 por ErrorCodeTest#noDuplicateCodes, sin atribuir ese fallo a CU-09. Las pruebas citadas usan las dependencias indicadas en cada caso; no acreditan servicios externos reales.

| Clase | Pruebas | Fallos | Errores | Omitidos | Resultado |
|---|---:|---:|---:|---:|---|
| [CreateReservationTest](../../../../src/test/java/com/seashare/m2/reservas/integration/CreateReservationTest.java) | 5 | 0 | 0 | 0 | Verde; cobertura según TC |
| [FleetUnavailableTest](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/FleetUnavailableTest.java) | 10 | 0 | 0 | 0 | Verde; cobertura según TC |
| [HttpFleetClientTest](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/HttpFleetClientTest.java) | 5 | 0 | 0 | 0 | Verde; cobertura según TC |
| [HttpVesselReadAdapterTest](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/HttpVesselReadAdapterTest.java) | 3 | 0 | 0 | 0 | Verde; cobertura según TC |
| [VesselDetailControllerTest](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/in/web/VesselDetailControllerTest.java) | 7 | 0 | 0 | 0 | Verde; cobertura según TC |
| [ExternalQueryLogRegistrationTest](../../../../src/test/java/com/seashare/m2/reservas/integration/ExternalQueryLogRegistrationTest.java) | 19 | 0 | 0 | 0 | Verde; cobertura según TC |
