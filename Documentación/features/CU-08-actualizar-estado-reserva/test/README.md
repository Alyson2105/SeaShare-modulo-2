# CU-08 — Pruebas backend de actualización de estado

Casos del puerto interno ChangeReservationStatusUseCase. El motor y la creación por CU-08 están implementados; la evidencia de entrega externa sigue siendo parcial.

**Estado global**: In Process; los nueve casos tienen cobertura parcial. Evaluado el 2026-11-13.

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
| [TC-CU08-009](TC-CU08-009-creacion-mediante-motor.md) | Creación por CU-08 | FR-001, FR-002, FR-003 | SC-001 | [creación](../../../../src/test/java/com/seashare/m2/reservas/integration/CreateReservationTest.java); [auditoría](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | In Process |

## Cobertura de requisitos

| FR | Cobertura | TC | Alcance | Motivo / Observación |
|---|---|---|---|---|
| FR-001 | Parcial — ver caso y brechas | 009 | Backend | Creación delegada al motor; cobertura parcial |
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
| TC-CU08-001 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Integración: ChangeReservationStatusServiceTest#validChangeSavesStatusAndAudit y #initiatedAndPendingPaymentDoNotEnqueueModuleOne pasaron: estado persistido, auditoría simulada y sin M1. Falta aserción del TTL persistido. |
| TC-CU08-002 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Unitario: ReservationStateMachineTest#finalStatesHaveNoExit pasó para estados terminales; integración: ChangeReservationStatusServiceTest#cancelFromPendingPaymentIsRejected pasó sin mutación ni eventos. Falta verificar persistencia para cada salida terminal. |
| TC-CU08-003 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Integración: ChangeReservationStatusServiceTest#versionConflictRaisesVERSION_CONFLICT pasó al guardar una copia obsoleta mediante repositorio. Falta comprobar la invocación del servicio y la reserva tras el rechazo. |
| TC-CU08-004 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Integración: TtlExpiryIntegrationTest#expiresPendingPaymentPastTtl y #pendingPaymentBeforeTtlIsNotExpired pasaron para PENDING_PAYMENT y ausencia de evento de estado. Falta demostrar ambos estados iniciales y ausencia de M1 en el mismo caso. |
| TC-CU08-005 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Integración: ChangeReservationStatusServiceTest#initiatedAndPendingPaymentDoNotEnqueueModuleOne, #enqueuesReservedToModuleOneOnPaymentConfirmed y #checkInAndCheckOutTransitionsWork pasaron con puertos simulados. Falta verificar cada destino operativo y fila persistida en una secuencia completa. |
| TC-CU08-006 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Integración: ChangeReservationStatusServiceTest#cancelledByOwnerWithMechanicalFailureEnqueuesMaintenance pasó; unitario: ModuleOneNotificationMatrixTest#cancelledAfterReservedNotifiesAvailable y #cancelledBeforeReservedDoesNotNotifyModuleOne pasaron. Falta comprobar ambas decisiones de cancelación en flujo integrado. |
| TC-CU08-007 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Integración: ChangeReservationStatusServiceTest#transitionAuditCarriesActorIdAndEventIds pasó: actor e IDs en auditoría simulada. Faltan todas las propiedades y comparación de filas persistidas de auditoría y outbox. |
| TC-CU08-008 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Integración: ChangeReservationStatusServiceTest#persistsCompletionNotesOnCompleted y #checkInAndCheckOutTransitionsWork pasaron: notas persistidas y DTO en puerto simulado. No se demuestra ausencia global de cálculo monetario. |
| TC-CU08-009 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Integración: CreateReservationTest#createsReservationInInitiated pasó con TTL en respuesta; ChangeReservationStatusServiceTest#creationWritesAuditRow pasó; unitario: CreateReservationServiceTest#creationDelegatesToInitiateUseCase pasó con escritor simulado. Falta invocación directa del motor con reloj controlado y TTL persistido. |

## Brechas detectadas

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

## Ejecución automatizada — 2026-11-13

`./mvnw -B clean test`: 395 pruebas, 0 fallos, 0 errores, 0 omitidas; BUILD SUCCESS. La integración del motor utiliza puertos de salida simulados; no demuestra entrega externa.

| Clase | Pruebas | Fallos | Errores | Omitidos | Resultado |
|---|---:|---:|---:|---:|---|
| [ChangeReservationStatusServiceTest](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | 28 | 0 | 0 | 0 | Exitosa |
| [ModuleOneNotificationMatrixTest](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java) | 7 | 0 | 0 | 0 | Exitosa |
| [ReservationStateMachineTest](../../../../src/test/java/com/seashare/m2/reservas/domain/state/ReservationStateMachineTest.java) | 7 | 0 | 0 | 0 | Exitosa |
| [ReservationStatusAuditImmutabilityTest](../../../../src/test/java/com/seashare/m2/reservas/integration/ReservationStatusAuditImmutabilityTest.java) | 2 | 0 | 0 | 0 | Exitosa |
| [TtlExpiryIntegrationTest](../../../../src/test/java/com/seashare/m2/reservas/integration/TtlExpiryIntegrationTest.java) | 8 | 0 | 0 | 0 | Exitosa |
| [CreateReservationTest](../../../../src/test/java/com/seashare/m2/reservas/integration/CreateReservationTest.java) | 5 | 0 | 0 | 0 | Exitosa |
| [CreateReservationServiceTest](../../../../src/test/java/com/seashare/m2/reservas/application/service/CreateReservationServiceTest.java) | 4 | 0 | 0 | 0 | Exitosa |
