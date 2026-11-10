# CU-13 — Pruebas backend de confirmación de pago

Casos del canal A de notificación y canal B de consulta propuestos. CU-13 no está implementado; el motor CU-08 cubre solo componentes internos.

**Estado global**: Draft por implementación y contratos pendientes.

## Trazabilidad

| TC | Escenario | FR | SC | Test automatizado parcial | Status |
|---|---|---|---|---|---|
| [TC-CU13-001](TC-CU13-001-aprobacion-vigente.md) | Aprobación con TTL vigente | FR-002, FR-003, FR-004 | SC-001 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | Blocked |
| [TC-CU13-002](TC-CU13-002-aprobacion-tardia.md) | Aprobación tras TTL | FR-003, FR-006 | SC-002, SC-003 | [ttl](../../../../src/test/java/com/seashare/m2/reservas/integration/TtlExpiryIntegrationTest.java); [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | Blocked |
| [TC-CU13-003](TC-CU13-003-rechazo-reintentable.md) | Rechazo con TTL vigente | FR-005 | SC-002 | [matriz](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java) | Blocked |
| [TC-CU13-004](TC-CU13-004-fallo-definitivo.md) | Fallo definitivo | FR-005 | SC-002 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [matriz](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java) | Blocked |
| [TC-CU13-005](TC-CU13-005-confirmacion-duplicada.md) | Idempotencia de confirmación | FR-007 | SC-004 | Sin prueba de CU-13 | Blocked |
| [TC-CU13-006](TC-CU13-006-consulta-estados.md) | Consulta del resultado en M3 | FR-001, FR-003, FR-005 | SC-001, SC-002 | Sin prueba de CU-13 | Blocked |
| [TC-CU13-007](TC-CU13-007-payload-invalido.md) | Validación del mensaje | FR-002, FR-003 | SC-001 | Sin prueba de CU-13 | Blocked |

## Cobertura de requisitos

| FR | Cobertura | TC | Alcance | Motivo / Observación |
|---|---|---|---|---|
| FR-001 | Bloqueado — CU-13 pendiente | 006 | Backend | Canales pendientes |
| FR-002 | Bloqueado — CU-13 pendiente | 001, 007 | Backend | Payload |
| FR-003 | Bloqueado — CU-13 pendiente | 001, 002, 006, 007 | Backend | Estado y existencia |
| FR-004 | Bloqueado — CU-13 pendiente | 001 | Backend | Aprobación |
| FR-005 | Bloqueado — CU-13 pendiente | 003, 004, 006 | Backend | Rechazos |
| FR-006 | Bloqueado — CU-13 pendiente | 002 | Backend | Reversión |
| FR-007 | Bloqueado — CU-13 pendiente | 005 | Backend | Idempotencia por transacción |
| FR-008 | Bloqueado — CU-13 pendiente | 001, 006 | Backend | Sin dinero requiere auditoría integral |


## Criterios de éxito

| SC | Estado | Observación |
|---|---|---|
| SC-001 | Bloqueado | TC-001, 006 y 007; plazo requiere medición. |
| SC-002 | Bloqueado | TC-002 a 004 y 006; corte del motor no cubre CU-13 completo. |
| SC-003 | Bloqueado | TC-002; instrucción de reversión pendiente. |
| SC-004 | Bloqueado | TC-005; idempotencia por transacción pendiente. |
| SC-005 | Bloqueado | Neutralidad financiera requiere revisión integral. |

## Checklist de ejecución

| TC | Documentado | Ejecutado | Resultado | Ejecutado por | Fecha | Observaciones |
|---|---|---|---|---|---|---|
| TC-CU13-001 | Sí | No | Pendiente | — | — | Blocked |
| TC-CU13-002 | Sí | No | Pendiente | — | — | Blocked |
| TC-CU13-003 | Sí | No | Pendiente | — | — | Blocked |
| TC-CU13-004 | Sí | No | Pendiente | — | — | Blocked |
| TC-CU13-005 | Sí | No | Pendiente | — | — | Blocked |
| TC-CU13-006 | Sí | No | Pendiente | — | — | Blocked |
| TC-CU13-007 | Sí | No | Pendiente | — | — | Blocked |

## Brechas detectadas

1. [Contexto](../../../context/implementContext.md) confirma que CU-13 y ambos canales aún no existen, pese a que el plan usa lenguaje de canal activo.
2. Reversión de aprobación tardía en canal B requiere aclarar cómo se detecta cuando el job solo consulta reservas PENDING_PAYMENT con TTL vigente.
3. CU-03 y clave de cobro por intento están pendientes; idempotencia de CU-02 no cubre confirmaciones.
4. JWT de servicio, payload M3 T-12 y acuerdos de reversión requieren integración.
5. Tiempo menor a un segundo y entrega real de reversión requieren medición con servicios externos.

## Convenciones de estado

- **Draft**: contrato incompleto o inconsistente.
- **In Process**: ejecutable con implementación actual.
- **Approved**: aprobado tras revisión y ejecución; ninguno aquí.
- **Blocked**: depende de funcionalidad pendiente.
