# CU-09 — Pruebas backend de información de embarcación

Casos para FleetInfoPort y su adaptador HTTP de M1; modo fake activo por defecto. Los contratos de M1 y los consumidores de ficha completa siguen pendientes.

**Estado global**: Draft por integración y contratos pendientes.

## Trazabilidad

| TC | Escenario | FR | SC | Test automatizado parcial | Status |
|---|---|---|---|---|---|
| [TC-CU09-001](TC-CU09-001-consulta-individual.md) | Consulta de datos de embarcación | FR-001, FR-003, FR-004 | SC-003, SC-005 | [cliente](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/HttpFleetClientTest.java) | In Process |
| [TC-CU09-002](TC-CU09-002-capacidad-reserva.md) | Rechazo por exceso de capacidad | FR-002, FR-005, FR-006 | SC-002 | [reserva](../../../../src/test/java/com/seashare/m2/reservas/integration/CreateReservationTest.java) | In Process |
| [TC-CU09-003](TC-CU09-003-embarcacion-inexistente.md) | Embarcación no encontrada | FR-003, FR-007 | SC-004 | [fallos](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/FleetUnavailableTest.java) | In Process |
| [TC-CU09-004](TC-CU09-004-fallo-transitorio.md) | Reintento y fallo seguro | FR-003, FR-008 | SC-004 | [cliente](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/HttpFleetClientTest.java); [fallos](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/FleetUnavailableTest.java) | In Process |
| [TC-CU09-005](TC-CU09-005-zona-horaria-invalida.md) | Zona horaria ausente o inválida | FR-004, FR-008 | SC-004, SC-005 | [fallos](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/FleetUnavailableTest.java) | In Process |
| [TC-CU09-006](TC-CU09-006-registro-consulta.md) | Registro de consulta | FR-010 | SC-003 | [cliente](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/HttpFleetClientTest.java) | In Process |
| [TC-CU09-007](TC-CU09-007-ficha-completa.md) | Ficha completa y ubicación GPS | FR-001, FR-004 | SC-005 | Sin prueba de CU-09 | Blocked |

## Cobertura de requisitos

| FR | Cobertura | TC | Alcance | Motivo / Observación |
|---|---|---|---|---|
| FR-001 | Parcial — ver caso y brechas | 001, 007 | Backend | Interfaz parcial; consumidores pendientes |
| FR-002 | Parcial — ver caso y brechas | 002 | Backend | Pasajeros validados por CU-02 |
| FR-003 | Parcial — ver caso y brechas | 001, 003, 004 | Backend | Consulta HTTP |
| FR-004 | Parcial — ver caso y brechas | 001, 005, 007 | Backend | DTO parcial, ficha pendiente |
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
| SC-005 | Parcial | TC-001 y 005 cubren zona; GPS y ficha completa pendientes. |

## Checklist de ejecución

| TC | Documentado | Ejecutado | Resultado | Ejecutado por | Fecha | Observaciones |
|---|---|---|---|---|---|---|
| TC-CU09-001 | Sí | No | Pendiente | — | — | In Process |
| TC-CU09-002 | Sí | No | Pendiente | — | — | In Process |
| TC-CU09-003 | Sí | No | Pendiente | — | — | In Process |
| TC-CU09-004 | Sí | No | Pendiente | — | — | In Process |
| TC-CU09-005 | Sí | No | Pendiente | — | — | In Process |
| TC-CU09-006 | Sí | No | Pendiente | — | — | In Process |
| TC-CU09-007 | Sí | No | Pendiente | — | — | Blocked |

## Brechas detectadas

1. [Contexto](../../../context/implementContext.md): contrato M1 no firmado y modo fake predeterminado.
2. FleetInfo solo contiene ownerId, maxCapacity y harborTimezone; ficha completa y GPS no están expuestos.
3. CU-01 y CU-19 están pendientes; capacidad se verifica en CU-02, no en FleetInfoPort.
4. El plan menciona reintentos 500/502; el cliente solo reintenta 503 y errores de acceso.
5. Un 503 persistente se registra como TIMEOUT y un fallo de auditoría no detiene la consulta.
6. Latencia y disponibilidad real de M1 requieren medición externa; no se infieren del timeout.

## Convenciones de estado

- **Draft**: contrato incompleto o inconsistente.
- **In Process**: ejecutable con implementación actual.
- **Approved**: aprobado tras revisión y ejecución; ninguno aquí.
- **Blocked**: depende de funcionalidad pendiente.
