# CU-08 — Pruebas backend de actualización de estado

Casos del puerto interno ChangeReservationStatusUseCase. El motor está implementado parcialmente; la creación por CU-08 y el payload final M3 están pendientes.

**Estado global**: Draft por integraciones y contratos pendientes.

## Trazabilidad

| TC | Escenario | FR | SC | Test automatizado parcial | Status |
|---|---|---|---|---|---|
| [TC-CU08-001](TC-CU08-001-transiciones-permitidas.md) | Transición válida | FR-002, FR-003, FR-004 | SC-001 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [maquina](../../../../src/test/java/com/seashare/m2/reservas/domain/state/ReservationStateMachineTest.java) | In Process |
| [TC-CU08-002](TC-CU08-002-transiciones-prohibidas.md) | Rechazo de transición inválida | FR-002, FR-003, FR-005 | SC-001 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [maquina](../../../../src/test/java/com/seashare/m2/reservas/domain/state/ReservationStateMachineTest.java) | In Process |
| [TC-CU08-003](TC-CU08-003-control-version.md) | Conflicto de versión | FR-004, FR-006 | SC-001 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | In Process |
| [TC-CU08-004](TC-CU08-004-expiracion-ttl.md) | Expiración al límite | FR-002, FR-003, FR-005 | SC-001, SC-003 | [ttl](../../../../src/test/java/com/seashare/m2/reservas/integration/TtlExpiryIntegrationTest.java) | In Process |
| [TC-CU08-005](TC-CU08-005-matriz-notificaciones.md) | Notificación según destino | FR-007, FR-008, FR-011 | SC-003 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [matriz](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java) | In Process |
| [TC-CU08-006](TC-CU08-006-cancelacion-averia.md) | Notificación por cancelación | FR-003, FR-007 | SC-003 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [matriz](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java) | In Process |
| [TC-CU08-007](TC-CU08-007-auditoria-eventos.md) | Auditoría de transición | FR-004, FR-011 | SC-001 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [auditoria](../../../../src/test/java/com/seashare/m2/reservas/integration/ReservationStatusAuditImmutabilityTest.java) | In Process |
| [TC-CU08-008](TC-CU08-008-notas-cierre.md) | Novedades de cierre | FR-009, FR-010 | SC-002 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | In Process |
| [TC-CU08-009](TC-CU08-009-creacion-mediante-motor.md) | Creación por CU-08 | FR-001, FR-002, FR-003 | SC-001 | Sin prueba de CU-08 | Blocked |

## Cobertura de requisitos

| FR | Cobertura | TC | Alcance | Motivo / Observación |
|---|---|---|---|---|
| FR-001 | Parcial — ver caso y brechas | 009 | Backend | Creación por motor pendiente T-24 |
| FR-002 | Parcial — ver caso y brechas | 001, 002, 004, 009 | Backend | Estados y creación |
| FR-003 | Parcial — ver caso y brechas | 001, 002, 004, 006, 009 | Backend | Matriz de transiciones |
| FR-004 | Parcial — ver caso y brechas | 001, 003, 007 | Backend | Persistencia atómica |
| FR-005 | Parcial — ver caso y brechas | 002, 004 | Backend | Rechazo |
| FR-006 | Parcial — ver caso y brechas | 003 | Backend | Versión optimista |
| FR-007 | Parcial — ver caso y brechas | 005, 006 | Backend | Matriz M1 |
| FR-008 | Parcial — ver caso y brechas | 005 | Backend | Matriz M3 con discrepancia del spec |
| FR-009 | Parcial — ver caso y brechas | 008 | Backend | Notas y payload pendiente |
| FR-010 | Parcial — ver caso y brechas | 008 | Backend | Sin dinero |
| FR-011 | Parcial — ver caso y brechas | 005, 007 | Backend | Identificadores de eventos |

## Criterios de éxito

| SC | Estado | Observación |
|---|---|---|
| SC-001 | Parcial | TC-001 a 004 y 007; no prueba todas las combinaciones posibles. |
| SC-002 | Fuera de alcance | TC-008 cubre notas; el plazo de primer intento requiere medición real. |
| SC-003 | Parcial | TC-004 a 006 cubren decisiones locales; no prueban estado físico en M1. |

## Checklist de ejecución

| TC | Documentado | Ejecutado | Resultado | Ejecutado por | Fecha | Observaciones |
|---|---|---|---|---|---|---|
| TC-CU08-001 | Sí | No | Pendiente | — | — | In Process |
| TC-CU08-002 | Sí | No | Pendiente | — | — | In Process |
| TC-CU08-003 | Sí | No | Pendiente | — | — | In Process |
| TC-CU08-004 | Sí | No | Pendiente | — | — | In Process |
| TC-CU08-005 | Sí | No | Pendiente | — | — | In Process |
| TC-CU08-006 | Sí | No | Pendiente | — | — | In Process |
| TC-CU08-007 | Sí | No | Pendiente | — | — | In Process |
| TC-CU08-008 | Sí | No | Pendiente | — | — | In Process |
| TC-CU08-009 | Sí | No | Pendiente | — | — | Blocked |

## Brechas detectadas

1. [CU-08 y contexto](../../../context/implementContext.md): creación por CU-08 pendiente T-24; CU-02 crea directamente.
2. FR-008 menciona cada estado desde PENDING_PAYMENT, pero la matriz del mismo spec y el código excluyen EXPIRED y PAYMENT_FAILED de M3.
3. Payload final M3 pendiente T-12; los eventos actuales no prueban entrega o consumo efectivos.
4. Primer intento en 500 ms y estado físico en M1 requieren servicios externos y medición; no se infieren de outbox.
5. Relay aún no republica a DLQ tras quinto fallo; flota opera con adaptadores fake por defecto.
6. FCFS de disponibilidad corresponde a CU-03; control de versión de CU-08 protege otra carrera.

## Convenciones de estado

- **Draft**: contrato incompleto o inconsistente.
- **In Process**: ejecutable con implementación actual.
- **Approved**: aprobado tras revisión y ejecución; ninguno aquí.
- **Blocked**: depende de funcionalidad pendiente.
