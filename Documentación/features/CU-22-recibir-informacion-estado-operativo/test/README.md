# CU-22 — Pruebas backend de notificación operativa a M1

Casos de mapeo de estado operativo, outbox y despacho M1_HTTP. El adaptador de entrega a M1 es fake.

**Estado global**: Draft por integraciones y contratos pendientes.

## Trazabilidad

| TC | Escenario | FR | SC | Test automatizado parcial | Status |
|---|---|---|---|---|---|
| [TC-CU22-001](TC-CU22-001-mapeo-estados.md) | Mapeo operativo | FR-001, FR-002 | SC-005 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [matriz](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java) | In Process |
| [TC-CU22-002](TC-CU22-002-sin-notificacion.md) | Estados sin notificación M1 | FR-001, FR-002 | SC-001 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [matriz](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java) | In Process |
| [TC-CU22-003](TC-CU22-003-cancelacion-condicional.md) | Cancelación con guarda M1 | FR-001, FR-002 | SC-005 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [matriz](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java) | In Process |
| [TC-CU22-004](TC-CU22-004-payload-operativo.md) | Payload operativo e identidad | FR-003, FR-004, FR-005, FR-007 | SC-003, SC-004 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [outbox](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/messaging/outbox/OutboxAdapterTest.java) | In Process |
| [TC-CU22-005](TC-CU22-005-reintentos-locales.md) | Reintentos del relay | FR-006, FR-008 | SC-001 | [relay](../../../../src/test/java/com/seashare/m2/reservas/application/service/PublishPendingOutboxServiceTest.java) | In Process |
| [TC-CU22-006](TC-CU22-006-entrega-m1-real.md) | Entrega HTTP a M1 | FR-006, FR-008 | SC-001, SC-002 | Sin prueba de CU-22 | Blocked |

## Cobertura de requisitos

| FR | Cobertura | TC | Alcance | Motivo / Observación |
|---|---|---|---|---|
| FR-001 | Parcial — ver caso y brechas | 001, 002, 003 | Backend | Encolado condicionado |
| FR-002 | Parcial — ver caso y brechas | 001, 002, 003 | Backend | Matriz de flota |
| FR-003 | Parcial — ver caso y brechas | 004 | Backend | Campos operativos |
| FR-004 | Parcial — ver caso y brechas | 004 | Backend | Identidad de evento |
| FR-005 | Parcial — ver caso y brechas | 004 | Backend | Outbox transaccional |
| FR-006 | Parcial — ver caso y brechas | 005, 006 | Backend | Relay parcial; DLQ y HTTP reales pendientes |
| FR-007 | Parcial — ver caso y brechas | 004 | Backend | Sin dinero |
| FR-008 | Parcial — ver caso y brechas | 005, 006 | Backend | Entrega asíncrona |
| FR-009 | Parcial — ver caso y brechas | — | Backend | FCFS corresponde a CU-03 |

## Criterios de éxito

| SC | Estado | Observación |
|---|---|---|
| SC-001 | Parcial | TC-001 a 003, 005 y 006; entrega real pendiente. |
| SC-002 | Bloqueado | TC-006; primer intento en 500 ms requiere implementación y medición. |
| SC-003 | Parcial | TC-004; payload operativo sin montos. |
| SC-004 | Parcial | TC-004; deduplicación del receptor no demostrada. |
| SC-005 | Parcial | TC-001 y 003; mapeo local. |

## Checklist de ejecución

| TC | Documentado | Ejecutado | Resultado | Ejecutado por | Fecha | Observaciones |
|---|---|---|---|---|---|---|
| TC-CU22-001 | Sí | No | Pendiente | — | — | In Process |
| TC-CU22-002 | Sí | No | Pendiente | — | — | In Process |
| TC-CU22-003 | Sí | No | Pendiente | — | — | In Process |
| TC-CU22-004 | Sí | No | Pendiente | — | — | In Process |
| TC-CU22-005 | Sí | No | Pendiente | — | — | In Process |
| TC-CU22-006 | Sí | No | Pendiente | — | — | Blocked |

## Brechas detectadas

1. [Contexto](../../../context/implementContext.md): matriz y outbox implementados, pero notificador M1_HTTP es fake.
2. Plan mezcla PATCH HTTP y descripción AMQP/broker; último salto y confirmación requieren coherencia con M1.
3. Quinto fallo o 4xx permanente marca dead; republicación a DLQ aún no implementada.
4. SLA de 500 ms y deduplicación en M1 requieren integración y medición externas.
5. Mapeo de estados sigue FR-002; FCFS es responsabilidad de CU-03.
6. Los eventos AMQP de M3 son distintos de las notificaciones M1_HTTP.

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
| [ChangeReservationStatusServiceTest](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | 25 | 0 | 25 | Bloqueada por Docker/Testcontainers |
| [ModuleOneNotificationMatrixTest](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java) | 7 | 0 | 0 | Exitosa |
| [OutboxAdapterTest](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/messaging/outbox/OutboxAdapterTest.java) | 5 | 0 | 0 | Exitosa |
| [PublishPendingOutboxServiceTest](../../../../src/test/java/com/seashare/m2/reservas/application/service/PublishPendingOutboxServiceTest.java) | 10 | 0 | 0 | Exitosa |
