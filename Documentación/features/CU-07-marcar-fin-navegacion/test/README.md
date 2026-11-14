# CU-07 — Pruebas backend de fin de navegación

Casos para el contrato propuesto `POST /api/v1/reservations/{reservation_id}/navigation-end`. CU-07 tiene controlador y servicio; CU-08 ofrece componentes internos. UI y servicios externos requieren pruebas independientes.

**Estado global**: Draft; evidencia parcial y brechas de integración. Evaluado el 2026-11-13.

## Trazabilidad

| TC | Escenario | FR | SC | Test automatizado parcial | Status |
|---|---|---|---|---|---|
| [TC-CU07-001](TC-CU07-001-cierre-exitoso.md) | Cierre de navegación | FR-001, FR-003, FR-004, FR-005 | SC-002, SC-004 | NavigationEndIntegrationTest#successfulEndPersistsArrivalCompletedStateAndCheckoutEvent (parcial) ; [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | In Process |
| [TC-CU07-002](TC-CU07-002-estado-incompatible.md) | Estado no admisible | FR-001, FR-009 | SC-001 | NavigationEndIntegrationTest#reservedReservationIsInvalidState (parcial) ; [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | Draft |
| [TC-CU07-003](TC-CU07-003-propietario-no-autorizado.md) | Autorización del propietario | FR-002 | SC-005 | NavigationEndIntegrationTest#renterRoleAndForeignOwnerAreForbidden (parcial) | In Process |
| [TC-CU07-004](TC-CU07-004-notificaciones-y-novedades.md) | Notificación de cierre | FR-003, FR-006, FR-007, FR-008 | SC-002, SC-003 | EndNavigationServiceTest#remarksArePassedAsCompletionNotesAndChangeMessage (parcial) ; [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [matriz](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java) | In Process |
| [TC-CU07-005](TC-CU07-005-disputa-garantia.md) | Apertura de disputa | FR-004, FR-010 | SC-002, SC-004 | NavigationEndDisputeRollbackTest#disputeFailureLeavesReservationInNavigation y OpenDisputeIntegrationTest#opensPendingDisputeWith24HourWindow (parcial) | In Process |
| [TC-CU07-006](TC-CU07-006-doble-intento.md) | Duplicación de cierre | FR-001, FR-004, FR-005 | SC-001, SC-004 | NavigationEndIntegrationTest#secondClickReturnsTripAlreadyCompleted (parcial) ; [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | Draft |

## Cobertura de requisitos

| FR | Cobertura | TC | Alcance | Motivo / Observación |
|---|---|---|---|---|
| FR-001 | Parcial | 001, 002, 006 | Backend | Evidencia parcial y límites concretos en los TC asociados. |
| FR-002 | Parcial | 003 | Backend | Evidencia parcial y límites concretos en los TC asociados. |
| FR-003 | Parcial | 001, 004 | Backend | Evidencia parcial y límites concretos en los TC asociados. |
| FR-004 | Parcial | 001, 005, 006 | Backend | Evidencia parcial y límites concretos en los TC asociados. |
| FR-005 | Parcial | 001, 006 | Backend | Evidencia parcial y límites concretos en los TC asociados. |
| FR-006 | Parcial | 004 | Backend | Evidencia parcial y límites concretos en los TC asociados. |
| FR-007 | Parcial | 004 | Backend | Evidencia parcial y límites concretos en los TC asociados. |
| FR-008 | Sin evidencia integral | 004 | Backend | La ausencia de operaciones financieras requiere verificación integral. |
| FR-009 | Parcial | 002 | Backend | Evidencia parcial y límites concretos en los TC asociados. |
| FR-010 | Parcial | 005 | Backend | CU-07 invoca CU-16; la apertura directa se prueba por separado y falta verificar sus datos persistidos y HTTP en el cierre integrado. |
| FR-011 | Sin cobertura backend | — | UI | Visualización e interacción requieren pruebas frontend. |
| FR-012 | Sin cobertura backend | — | UI | Visualización e interacción requieren pruebas frontend. |
| FR-013 | Sin cobertura backend | — | UI | Visualización e interacción requieren pruebas frontend. |

## Criterios de éxito

| SC | Estado | Observación |
|---|---|---|
| SC-001 | Parcialmente demostrado | Evidencia parcial; revisar límites y pruebas de los TC asociados. |
| SC-002 | Parcialmente demostrado | Evidencia parcial; revisar límites y pruebas de los TC asociados. |
| SC-003 | No demostrado | La ausencia integral de operaciones financieras requiere verificación. |
| SC-004 | Parcialmente demostrado | Evidencia parcial; revisar límites y pruebas de los TC asociados. |
| SC-005 | Parcialmente demostrado | Evidencia parcial; revisar límites y pruebas de los TC asociados. |

## Checklist de ejecución

| TC | Documentado | Ejecutado | Resultado | Ejecutado por | Fecha | Observaciones |
|---|---|---|---|---|---|---|
| TC-CU07-001 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | NavigationEndIntegrationTest#successfulEndPersistsArrivalCompletedStateAndCheckoutEvent: verde; Verifica HTTP, llegada y checkout persistido; no prueba todas las variantes ni entrega externa. |
| TC-CU07-002 | Sí | Sí | Draft | Agente automatizado | 2026-11-13 | NavigationEndIntegrationTest#reservedReservationIsInvalidState: verifica 409 en RESERVED; el plan asigna dos códigos a COMPLETED y faltan efectos persistidos. |
| TC-CU07-003 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | NavigationEndIntegrationTest#renterRoleAndForeignOwnerAreForbidden: verde; Verifica ambos 403; usa identidad por cabeceras, sin comprobar JWT ni efectos posteriores. |
| TC-CU07-004 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | EndNavigationServiceTest#remarksArePassedAsCompletionNotesAndChangeMessage: verde; Captura novedades con CU-08 simulado; no verifica entrega M1/M3 ni payload completo. |
| TC-CU07-005 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | NavigationEndDisputeRollbackTest#disputeFailureLeavesReservationInNavigation: rollback de estado y checkout; faltan aserciones integradas de disputa, respuesta y eventos del cierre exitoso. |
| TC-CU07-006 | Sí | Sí | Draft | Agente automatizado | 2026-11-13 | NavigationEndIntegrationTest#secondClickReturnsTripAlreadyCompleted: verde; Verifica primer 200 y segundo 409; la tabla del plan asigna dos códigos a COMPLETED; efectos completos sin verificar. |

## Brechas detectadas

1. [Contexto](../../../context/implementContext.md): hay controlador y servicio; las pruebas parciales no acreditan el flujo completo.
2. H10 conecta CU-07 con CU-16; la apertura directa y el rollback tienen pruebas separadas, pero faltan aserciones integradas sobre disputa persistida y respuesta HTTP.
3. La entrega a M1, el primer intento en menos de un segundo y la notificación a M3 requieren servicios y medición; encolar no demuestra entrega ni pago.
4. JWT está pendiente y flota usa adaptadores fake por defecto; payload M3 T-12 pendiente.
5. El edge case exige respuesta correcta y no duplicación; la tabla de errores del plan incluye COMPLETED tanto en INVALID_RESERVATION_STATE como en TRIP_ALREADY_COMPLETED.
6. FR-011 a FR-013 requieren pruebas de frontend o integración UI.

Discrepancias: la ausencia de llamada CU-07 a CU-16 quedó resuelta en H10; aún falta evidencia del flujo exitoso integrado de disputa. El DTO StatusChangedEvent no incluye hora real aunque el spec la solicita para M3. Identidad por headers, no JWT.

## Convenciones de estado

- **Draft**: contrato incompleto o inconsistente.
- **In Process**: ejecutable con implementación actual.
- **Approved**: escenario y postcondiciones demostrados por aserciones de la corrida V4, sin impedimentos de aprobación.
- **Blocked**: depende de funcionalidad pendiente.

## Ejecución automatizada — 2026-11-13

`sg docker -c 'bash ./mvnw -B clean test'`; inicio 2026-11-13T19:02:00-05:00, fin 2026-11-13T19:03:11-05:00; BASELINE_HEAD `568290a0d4334b477a4b0f9d5e949e981da3fa55`; salida 0. Se muestran suites pertinentes, no totales globales. Las pruebas con mocks no acreditan entrega externa.

| Clase | Pruebas | Fallos | Errores | Omitidos | Resultado |
|---|---:|---:|---:|---:|---|
| com.seashare.m2.disputa.integration.OpenDisputeIntegrationTest | 3 | 0 | 0 | 0 | Parcial para los escenarios relacionados |
| com.seashare.m2.reservas.application.service.EndNavigationServiceTest | 21 | 0 | 0 | 0 | Parcial para los escenarios relacionados |
| com.seashare.m2.reservas.infrastructure.adapter.in.web.NavigationEndControllerTest | 7 | 0 | 0 | 0 | Parcial para los escenarios relacionados |
| com.seashare.m2.reservas.integration.NavigationEndDisputeRollbackTest | 1 | 0 | 0 | 0 | Parcial para los escenarios relacionados |
| com.seashare.m2.reservas.integration.NavigationEndIntegrationTest | 7 | 0 | 0 | 0 | Parcial para los escenarios relacionados |
