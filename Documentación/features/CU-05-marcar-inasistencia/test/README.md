# CU-05 — Pruebas backend de marcar inasistencia

Casos para el contrato propuesto `POST /api/v1/reservations/{reservation_id}/no-show`. CU-05 tiene controlador y servicio; las pruebas del motor CU-08 son evidencia parcial. No se ejecutan pruebas UI ni servicios externos.

**Estado global**: In Process; evidencia parcial y brechas de integración. Evaluado el 2026-11-13.

## Trazabilidad

| TC | Escenario | FR | SC | Test automatizado parcial | Status |
|---|---|---|---|---|---|
| [TC-CU05-001](TC-CU05-001-reporte-limite.md) | Reporte al cumplir la tolerancia | FR-001, FR-003, FR-004, FR-007, FR-008 | SC-001, SC-004 | NoShowIntegrationTest#successfulNoShowPersistsEventAndCancelsReservation (parcial) ; [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | In Process |
| [TC-CU05-002](TC-CU05-002-reporte-anticipado.md) | Rechazo antes de la tolerancia | FR-003, FR-004, FR-005 | SC-001, SC-003 | NoShowIntegrationTest#oneSecondBeforeGraceEndIsRejected (parcial) | In Process |
| [TC-CU05-003](TC-CU05-003-estado-incompatible.md) | Rechazo por estado incompatible | FR-001, FR-006 | SC-002 | ReportNoShowServiceTest#invalidStatesAreRejectedWithoutClockOrWrites (parcial) ; [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | In Process |
| [TC-CU05-004](TC-CU05-004-propietario-no-autorizado.md) | Rechazo de solicitante ajeno | FR-002 | SC-006 | NoShowIntegrationTest#renterAndForeignOwnerAreForbiddenAndUnknownIs404 (parcial) | In Process |
| [TC-CU05-005](TC-CU05-005-zona-horaria-no-disponible.md) | Fallo de zona horaria | FR-004, FR-012 | SC-001 | NoShowIntegrationTest#invalidHarborTimezoneReturns503 (parcial) | Blocked |
| [TC-CU05-006](TC-CU05-006-notificaciones-y-registro.md) | Registro y notificación de inasistencia | FR-007, FR-008, FR-009, FR-010, FR-011 | SC-004, SC-005 | NoShowIntegrationTest#successfulNoShowPersistsEventAndCancelsReservation (parcial) ; [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [matriz](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java) | In Process |
| [TC-CU05-007](TC-CU05-007-carrera-inicio-navegacion.md) | Carrera con inicio de viaje | FR-001, FR-006, FR-007 | SC-002 | NoShowIntegrationTest#navigationStartAfterNoShowIsInvalidState (parcial) ; [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | In Process |

## Cobertura de requisitos

| FR | Cobertura | TC | Alcance | Motivo / Observación |
|---|---|---|---|---|
| FR-001 | Parcial | 001, 003, 007 | Backend | Evidencia parcial y límites concretos en los TC asociados. |
| FR-002 | Parcial | 004 | Backend | Evidencia parcial y límites concretos en los TC asociados. |
| FR-003 | Parcial | 001, 002 | Backend | Evidencia parcial y límites concretos en los TC asociados. |
| FR-004 | Parcial | 001, 002, 005 | Backend | Evidencia parcial y límites concretos en los TC asociados. |
| FR-005 | Parcial | 002 | Backend | Evidencia parcial y límites concretos en los TC asociados. |
| FR-006 | Parcial | 003, 007 | Backend | Evidencia parcial y límites concretos en los TC asociados. |
| FR-007 | Parcial | 001, 006, 007 | Backend | Evidencia parcial y límites concretos en los TC asociados. |
| FR-008 | Parcial | 001, 006 | Backend | Evidencia parcial y límites concretos en los TC asociados. |
| FR-009 | Sin evidencia | 006 | Backend | La ausencia de operaciones financieras requiere verificación integral. |
| FR-010 | Parcial | 006 | Backend | Evidencia parcial y límites concretos en los TC asociados. |
| FR-011 | Parcial | 006 | Backend | Evidencia parcial y límites concretos en los TC asociados. |
| FR-012 | Parcial | 005 | Backend | Evidencia parcial y límites concretos en los TC asociados. |
| FR-013 | Sin cobertura backend | — | UI | Visualización e interacción requieren pruebas frontend. |
| FR-014 | Sin cobertura backend | — | UI | Visualización e interacción requieren pruebas frontend. |
| FR-015 | Sin cobertura backend | — | UI | Visualización e interacción requieren pruebas frontend. |
| FR-016 | Sin cobertura backend | — | UI | Visualización e interacción requieren pruebas frontend. |
| FR-017 | Sin cobertura backend | — | UI | Visualización e interacción requieren pruebas frontend. |
| FR-018 | Sin cobertura backend | — | UI | Visualización e interacción requieren pruebas frontend. |
| FR-019 | Sin cobertura backend | — | UI | Visualización e interacción requieren pruebas frontend. |

## Criterios de éxito

| SC | Estado | Observación |
|---|---|---|
| SC-001 | Parcialmente demostrado | Evidencia parcial; revisar límites y pruebas de los TC asociados. |
| SC-002 | Parcialmente demostrado | Evidencia parcial; revisar límites y pruebas de los TC asociados. |
| SC-003 | Parcialmente demostrado | Evidencia parcial; revisar límites y pruebas de los TC asociados. |
| SC-004 | Parcialmente demostrado | Evidencia parcial; revisar límites y pruebas de los TC asociados. |
| SC-005 | No demostrado | La ausencia de operaciones financieras requiere verificación integral. |
| SC-006 | Parcialmente demostrado | Evidencia parcial; revisar límites y pruebas de los TC asociados. |

## Checklist de ejecución

| TC | Documentado | Ejecutado | Resultado | Ejecutado por | Fecha | Observaciones |
|---|---|---|---|---|---|---|
| TC-CU05-001 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | NoShowIntegrationTest#successfulNoShowPersistsEventAndCancelsReservation: verde; Comprueba HTTP, estado y evento persistidos al minuto 30; el caso de 35 minutos solo tiene evidencia unitaria y faltan consulta CU-09 y SLA externo. |
| TC-CU05-002 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | NoShowIntegrationTest#oneSecondBeforeGraceEndIsRejected: verde; Comprueba 409, estado y ausencia de evento; no comprueba outbox ni texto completo. |
| TC-CU05-003 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | ReportNoShowServiceTest#invalidStatesAreRejectedWithoutClockOrWrites: verde; Prueba cuatro estados con mocks; no prueba todos los estados de la ficha. |
| TC-CU05-004 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | NoShowIntegrationTest#renterAndForeignOwnerAreForbiddenAndUnknownIs404: verde; Comprueba ambos 403; identidad por cabeceras, no JWT ni persistencia posterior al rechazo. |
| TC-CU05-005 | Sí | Sí | Blocked | Agente automatizado | 2026-11-13 | NoShowIntegrationTest#invalidHarborTimezoneReturns503: verde; Prueba zona almacenada inválida y 503 HARBOR_TIMEZONE_UNAVAILABLE; no existe consulta a CU-09 durante el reporte, requerida por plan para FLEET_SERVICE_UNAVAILABLE. |
| TC-CU05-006 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | NoShowIntegrationTest#successfulNoShowPersistsEventAndCancelsReservation: verde; Comprueba registro y transición; no inspecciona outbox ni entrega M1/M3 o plazo. |
| TC-CU05-007 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | NoShowIntegrationTest#navigationStartAfterNoShowIsInvalidState: verde; Prueba no-show seguido de inicio de forma secuencial; no demuestra carrera concurrente. |

## Brechas detectadas

1. [Contexto](../../../context/implementContext.md): hay controlador y servicio; las pruebas parciales no acreditan el flujo completo.
2. El motor de CU-08 no demuestra la orquestación ni el cálculo temporal propios de CU-05.
3. La entrega externa y el primer intento en menos de un segundo requieren servicios y medición; el encolado no los demuestra.
4. El payload de M3 está pendiente de T-12; flota usa adaptadores fake por defecto.
5. JWT está pendiente; la pertenencia se prueba con identidad temporal por headers, sin autenticación JWT.
6. FR-013 a FR-019 requieren pruebas de frontend o integración UI.

Discrepancias verificadas: La zona se lee de la reserva, no de CU-09; el plan prevé FLEET_SERVICE_UNAVAILABLE y el código usa HARBOR_TIMEZONE_UNAVAILABLE. El plan prevé campos de tiempo restante y el servicio los presenta en el mensaje.

## Convenciones de estado

- **Draft**: contrato incompleto o inconsistente.
- **In Process**: ejecutable con implementación actual.
- **Approved**: escenario y postcondiciones demostrados por aserciones de la corrida V4, sin impedimentos de aprobación.
- **Blocked**: depende de funcionalidad pendiente.

## Ejecución automatizada — 2026-11-13

`sg docker -c 'bash ./mvnw -B clean test'`; inicio 2026-11-13T19:02:00-05:00, fin 2026-11-13T19:03:11-05:00; BASELINE_HEAD `568290a0d4334b477a4b0f9d5e949e981da3fa55`; salida 0. Las pruebas integradas usan PostgreSQL y dependencias simuladas; no prueban entrega externa real.

| Clase | Pruebas | Fallos | Errores | Omitidos | Resultado |
|---|---:|---:|---:|---:|---|
| com.seashare.m2.reservas.application.service.ModuleOneNotificationMatrixTest | 7 | 0 | 0 | 0 | Evidencia parcial según TC |
| com.seashare.m2.reservas.application.service.ReportNoShowServiceTest | 18 | 0 | 0 | 0 | Evidencia parcial según TC |
| com.seashare.m2.reservas.infrastructure.adapter.in.web.NoShowControllerTest | 6 | 0 | 0 | 0 | Evidencia parcial según TC |
| com.seashare.m2.reservas.integration.ChangeReservationStatusServiceTest | 28 | 0 | 0 | 0 | Evidencia parcial según TC |
| com.seashare.m2.reservas.integration.NoShowIntegrationTest | 9 | 0 | 0 | 0 | Evidencia parcial según TC |
