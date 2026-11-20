# CU-15 — Pruebas backend de consulta interna para M3

Casos para el GET interno a M3. CU-15 está implementado con identidad temporal por headers; las vistas de CU-21 son contratos distintos.

**Estado global**: Draft; TC-002 requiere aclarar la igualdad de respuestas con TTL variable. Evaluado el 2026-11-13.

## Trazabilidad

| TC | Escenario | FR | SC | Test automatizado parcial | Status |
|---|---|---|---|---|---|
| [TC-CU15-001](TC-CU15-001-consulta-reserva-activa.md) | Consulta de reserva activa | FR-001, FR-002, FR-007 | SC-001, SC-002 | [InternalReservationControllerTest](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/in/web/InternalReservationControllerTest.java) | In Process |
| [TC-CU15-002](TC-CU15-002-consulta-ttl.md) | Consulta de reserva pendiente | FR-002, FR-005, FR-007 | SC-002 | [GetInternalReservationServiceTest](../../../../src/test/java/com/seashare/m2/reservas/application/service/GetInternalReservationServiceTest.java) | Draft |
| [TC-CU15-003](TC-CU15-003-consulta-cierre.md) | Consulta de reserva completada | FR-003, FR-006 | SC-003 | Sin prueba pertinente del GET; [antecedente de escritura](../../../../src/test/java/com/seashare/m2/reservas/application/service/EndNavigationServiceTest.java) | In Process |
| [TC-CU15-004](TC-CU15-004-consulta-cancelacion.md) | Consulta de reserva cancelada | FR-004, FR-006 | SC-003 | [InternalReservationReadIntegrationTest](../../../../src/test/java/com/seashare/m2/reservas/integration/InternalReservationReadIntegrationTest.java) | Blocked |
| [TC-CU15-005](TC-CU15-005-acceso-y-reserva-inexistente.md) | Autorización de consulta | FR-001, FR-007 | SC-002 | [InternalReservationControllerTest](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/in/web/InternalReservationControllerTest.java) | Blocked |

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
| SC-001 | Parcialmente demostrado | Endpoint probado con vista simulada; latencia menor a 200 ms requiere medición. |
| SC-002 | Parcialmente demostrado | Respuesta y autorización temporal probadas; falta verificar ausencia de escrituras en integración. |
| SC-003 | Parcialmente demostrado | Respuesta simulada sin montos; faltan flujos completos de cierre y cancelación. |

## Checklist de ejecución

| TC | Documentado | Ejecutado | Resultado | Ejecutado por | Fecha | Observaciones |
|---|---|---|---|---|---|---|
| TC-CU15-001 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Componente MockMvc: InternalReservationControllerTest#responseHasExactFieldsAndNoMoney pasó con vista simulada: 200, estructura y ausencia de montos. Falta comprobar escritura y notificaciones con persistencia real. |
| TC-CU15-002 | Sí | Sí | Draft | Agente automatizado | 2026-11-13 | Contrato requiere aclaración: spec pide respuestas exactamente iguales y el plan expone segundos restantes variables. GetInternalReservationServiceTest#ttlAppliesForPendingPayment pasó con reloj fijo para segundos restantes; no verifica respuesta HTTP ni invariancia. |
| TC-CU15-003 | Sí | No | Pendiente | — | — | Las pruebas de escritura de CU-07 no comprueban la consulta GET de una reserva completada. |
| TC-CU15-004 | Sí | Sí | Blocked | Agente automatizado | 2026-11-13 | Integración: InternalReservationReadIntegrationTest#cancelledByOwnerIsReportedWithOwnerActor pasó para subtipo y actor desde auditoría; InternalReservationResponse#from devuelve null para anticipación pese a que CU-04 está implementado. |
| TC-CU15-005 | Sí | Sí | Blocked | Agente automatizado | 2026-11-13 | Componente MockMvc: InternalReservationControllerTest#nonFinanceRoleIs403 y #unknownReservationIs404 pasaron con headers temporales y servicio simulado. Falta autenticación JWT de servicio exigida por el plan; no prueban ausencia de escritura en base de datos. |

## Brechas detectadas

1. [Contexto](../../../context/implementContext.md): JWT de servicio pendiente; el endpoint interno usa headers temporales.
2. CU-07 y CU-04 están implementados; falta probar el GET integrado tras cierre y el DTO devuelve null para anticipación.
3. El spec pide respuestas idénticas repetidas, pero el TTL restante cambia con el reloj aun sin escrituras.
4. Ejemplos del plan incluyen identificadores con letras fuera de UUID hexadecimal; usar UUID válidos en ejecución.
5. El plan declara latencia garantizada sin medición; se necesita prueba de rendimiento.

## Convenciones de estado

- **Draft**: contrato incompleto o inconsistente.
- **In Process**: ejecutable con implementación actual.
- **Approved**: escenario y postcondiciones demostrados por aserciones de la corrida V4, sin impedimentos de aprobación.
- **Blocked**: depende de funcionalidad pendiente.

## Ejecución automatizada — 2026-11-13

`sg docker -c 'bash ./mvnw -B clean test'`; inicio 2026-11-13T19:02:00-05:00, fin 2026-11-13T19:03:11-05:00; BASELINE_HEAD `568290a0d4334b477a4b0f9d5e949e981da3fa55`; salida 0. Las pruebas HTTP usan identidad temporal y vistas simuladas; la integración de lectura utiliza PostgreSQL.

| Clase | Pruebas | Fallos | Errores | Omitidos | Alcance |
|---|---:|---:|---:|---:|---|
| com.seashare.m2.reservas.application.service.EndNavigationServiceTest | 21 | 0 | 0 | 0 | Evidencia parcial según TC |
| com.seashare.m2.reservas.application.service.GetInternalReservationServiceTest | 10 | 0 | 0 | 0 | Evidencia parcial según TC |
| com.seashare.m2.reservas.infrastructure.adapter.in.web.InternalReservationControllerTest | 5 | 0 | 0 | 0 | Evidencia parcial según TC |
| com.seashare.m2.reservas.integration.InternalReservationReadIntegrationTest | 2 | 0 | 0 | 0 | Evidencia parcial según TC |

**Actualización local 2026-11-13:** TC-CU15-004-consulta-cancelacion.md — InternalReservationReadIntegrationTest#cancelledByRenterReportsPersistedAnticipation y #cancelledByOwnerIsReportedWithOwnerActor; PostgreSQL local; corrida focalizada 2026-11-13, 18 pruebas sin fallos. Resultado parcial; consultar limitaciones del caso.

**Decisión aprobada 2026-11-13 — TC-CU15-002-consulta-ttl.md:** Se aprobó el GET sin efectos con remaining_seconds variable según el reloj.
