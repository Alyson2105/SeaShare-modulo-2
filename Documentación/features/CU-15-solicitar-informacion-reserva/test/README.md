# CU-15 — Pruebas backend de consulta interna para M3

Casos para el GET interno a M3. CU-15 está implementado con identidad temporal por headers; las vistas de CU-21 son contratos distintos.

**Estado global**: Draft; TC-002 requiere aclarar la igualdad de respuestas con TTL variable. Evaluado el 2026-11-13.

## Trazabilidad

| TC | Escenario | FR | SC | Test automatizado parcial | Status |
|---|---|---|---|---|---|
| [TC-CU15-001](TC-CU15-001-consulta-reserva-activa.md) | Consulta de reserva activa | FR-001, FR-002, FR-007 | SC-001, SC-002 | [InternalReservationControllerTest](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/in/web/InternalReservationControllerTest.java) | In Process |
| [TC-CU15-002](TC-CU15-002-consulta-ttl.md) | Consulta de reserva pendiente | FR-002, FR-005, FR-007 | SC-002 | [GetInternalReservationServiceTest](../../../../src/test/java/com/seashare/m2/reservas/application/service/GetInternalReservationServiceTest.java) | Draft |
| [TC-CU15-003](TC-CU15-003-consulta-cierre.md) | Consulta de reserva completada | FR-003, FR-006 | SC-003 | Sin prueba de CU-15 | Blocked |
| [TC-CU15-004](TC-CU15-004-consulta-cancelacion.md) | Consulta de reserva cancelada | FR-004, FR-006 | SC-003 | [InternalReservationReadIntegrationTest](../../../../src/test/java/com/seashare/m2/reservas/integration/InternalReservationReadIntegrationTest.java) | Blocked |
| [TC-CU15-005](TC-CU15-005-acceso-y-reserva-inexistente.md) | Autorización de consulta | FR-001, FR-007 | SC-002 | [InternalReservationControllerTest](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/in/web/InternalReservationControllerTest.java) | In Process |

## Cobertura de requisitos

| FR | Cobertura | TC | Alcance | Motivo / Observación |
|---|---|---|---|---|
| FR-001 | Parcial | 001, 005 | Backend | Endpoint interno implementado; JWT pendiente |
| FR-002 | Parcial | 001, 002 | Backend | Datos operativos |
| FR-003 | Parcial | 003 | Backend | Datos de cierre |
| FR-004 | Parcial | 004 | Backend | Datos de cancelación |
| FR-005 | Parcial | 002 | Backend | Vencimiento TTL |
| FR-006 | Parcial | 003, 004 | Backend | Sin cálculos financieros |
| FR-007 | Parcial | 001, 002, 005 | Backend | Solo lectura |


## Criterios de éxito

| SC | Estado | Observación |
|---|---|---|
| SC-001 | Parcial | Endpoint probado con vista simulada; latencia menor a 200 ms requiere medición. |
| SC-002 | Parcial | Respuesta y autorización temporal probadas; falta verificar ausencia de escrituras en integración. |
| SC-003 | Parcial | Respuesta simulada sin montos; faltan flujos completos de cierre y cancelación. |

## Checklist de ejecución

| TC | Documentado | Ejecutado | Resultado | Ejecutado por | Fecha | Observaciones |
|---|---|---|---|---|---|---|
| TC-CU15-001 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Componente MockMvc: InternalReservationControllerTest#responseHasExactFieldsAndNoMoney pasó con vista simulada: 200, estructura y ausencia de montos. Falta comprobar escritura y notificaciones con persistencia real. |
| TC-CU15-002 | Sí | Sí | Draft | Agente automatizado | 2026-11-13 | Contrato requiere aclaración: spec pide respuestas exactamente iguales y el plan expone segundos restantes variables. GetInternalReservationServiceTest#ttlAppliesForPendingPayment pasó con reloj fijo para segundos restantes; no verifica respuesta HTTP ni invariancia. |
| TC-CU15-003 | Sí | No | Blocked | Agente automatizado | — | Sin ejecución automatizada; check-out depende de CU-07 no implementado. |
| TC-CU15-004 | Sí | Sí | Blocked | Agente automatizado | 2026-11-13 | Integración: InternalReservationReadIntegrationTest#cancelledByOwnerIsReportedWithOwnerActor pasó para subtipo y actor desde auditoría; anticipación permanece sin implementación del flujo CU-04. |
| TC-CU15-005 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Componente MockMvc: InternalReservationControllerTest#nonFinanceRoleIs403 y #unknownReservationIs404 pasaron con headers temporales y servicio simulado. No prueban JWT de servicio ni ausencia de escritura en base de datos. |

## Brechas detectadas

1. [Contexto](../../../context/implementContext.md): JWT de servicio pendiente; el endpoint interno usa headers temporales.
2. Check-out y clasificación de cancelación dependen de CU-07 y CU-04 no implementados.
3. El spec pide respuestas idénticas repetidas, pero el TTL restante cambia con el reloj aun sin escrituras.
4. Ejemplos del plan incluyen identificadores con letras fuera de UUID hexadecimal; usar UUID válidos en ejecución.
5. El plan declara latencia garantizada sin medición; se necesita prueba de rendimiento.

## Convenciones de estado

- **Draft**: contrato incompleto o inconsistente.
- **In Process**: ejecutable con implementación actual.
- **Approved**: aprobado tras revisión y ejecución; ninguno aquí.
- **Blocked**: depende de funcionalidad pendiente.

## Ejecución automatizada — 2026-11-13

`./mvnw -B clean test`: 395 pruebas, 0 fallos, 0 errores, 0 omitidas; BUILD SUCCESS. El controlador se prueba con MockMvc y vista simulada; la lectura integrada usa PostgreSQL.

| Clase | Pruebas | Fallos | Errores | Omitidos | Resultado |
|---|---:|---:|---:|---:|---|
| [InternalReservationControllerTest](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/in/web/InternalReservationControllerTest.java) | 5 | 0 | 0 | 0 | Exitosa |
| [GetInternalReservationServiceTest](../../../../src/test/java/com/seashare/m2/reservas/application/service/GetInternalReservationServiceTest.java) | 10 | 0 | 0 | 0 | Exitosa |
| [InternalReservationReadIntegrationTest](../../../../src/test/java/com/seashare/m2/reservas/integration/InternalReservationReadIntegrationTest.java) | 2 | 0 | 0 | 0 | Exitosa |
