# CU-07 — Pruebas backend de fin de navegación

Casos para el contrato propuesto `POST /api/v1/reservations/{reservation_id}/navigation-end`. CU-07 tiene controlador y servicio; CU-08 ofrece componentes internos. UI y servicios externos requieren pruebas independientes.

**Estado global**: Draft; evidencia parcial y brechas de integración. Evaluado el 2026-11-13.

## Trazabilidad

| TC | Escenario | FR | SC | Test automatizado parcial | Status |
|---|---|---|---|---|---|
| [TC-CU07-001](TC-CU07-001-cierre-exitoso.md) | Cierre de navegación | FR-001, FR-003, FR-004, FR-005 | SC-002, SC-004 | NavigationEndIntegrationTest#successfulEndPersistsArrivalCompletedStateAndCheckoutEvent (parcial) ; [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | In Process |
| [TC-CU07-002](TC-CU07-002-estado-incompatible.md) | Estado no admisible | FR-001, FR-009 | SC-001 | NavigationEndIntegrationTest#reservedReservationIsInvalidState (parcial) ; [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | In Process |
| [TC-CU07-003](TC-CU07-003-propietario-no-autorizado.md) | Autorización del propietario | FR-002 | SC-005 | NavigationEndIntegrationTest#renterRoleAndForeignOwnerAreForbidden (parcial) | In Process |
| [TC-CU07-004](TC-CU07-004-notificaciones-y-novedades.md) | Notificación de cierre | FR-003, FR-006, FR-007, FR-008 | SC-002, SC-003 | EndNavigationServiceTest#remarksArePassedAsCompletionNotesAndChangeMessage (parcial) ; [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [matriz](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java) | In Process |
| [TC-CU07-005](TC-CU07-005-disputa-garantia.md) | Apertura de disputa | FR-004, FR-010 | SC-002, SC-004 | OpenDisputeIntegrationTest#opensPendingDisputeWith24HourWindow (parcial) | Blocked |
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
| FR-010 | Bloqueado | 005 | Backend | CU-07 no invoca CU-16; apertura directa probada por separado. |
| FR-011 | Sin cobertura backend | — | UI | Visualización e interacción requieren pruebas frontend. |
| FR-012 | Sin cobertura backend | — | UI | Visualización e interacción requieren pruebas frontend. |
| FR-013 | Sin cobertura backend | — | UI | Visualización e interacción requieren pruebas frontend. |

## Criterios de éxito

| SC | Estado | Observación |
|---|---|---|
| SC-001 | Parcial | Evidencia parcial; revisar límites y pruebas de los TC asociados. |
| SC-002 | Parcial | Evidencia parcial; revisar límites y pruebas de los TC asociados. |
| SC-003 | Sin evidencia integral | La ausencia integral de operaciones financieras requiere verificación. |
| SC-004 | Parcial | Evidencia parcial; revisar límites y pruebas de los TC asociados. |
| SC-005 | Parcial | Evidencia parcial; revisar límites y pruebas de los TC asociados. |

## Checklist de ejecución

| TC | Documentado | Ejecutado | Resultado | Ejecutado por | Fecha | Observaciones |
|---|---|---|---|---|---|---|
| TC-CU07-001 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | NavigationEndIntegrationTest#successfulEndPersistsArrivalCompletedStateAndCheckoutEvent: verde; Verifica HTTP, llegada y checkout persistido; no prueba todas las variantes ni entrega externa. |
| TC-CU07-002 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | NavigationEndIntegrationTest#reservedReservationIsInvalidState: verde; Verifica 409 en RESERVED; no cubre todos los estados y efectos persistidos. |
| TC-CU07-003 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | NavigationEndIntegrationTest#renterRoleAndForeignOwnerAreForbidden: verde; Verifica ambos 403; usa identidad por cabeceras, sin comprobar JWT ni efectos posteriores. |
| TC-CU07-004 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | EndNavigationServiceTest#remarksArePassedAsCompletionNotesAndChangeMessage: verde; Captura novedades con CU-08 simulado; no verifica entrega M1/M3 ni payload completo. |
| TC-CU07-005 | Sí | Sí | Blocked | Agente automatizado | 2026-11-13 | OpenDisputeIntegrationTest#opensPendingDisputeWith24HourWindow: verde; La apertura directa pasa; EndNavigationService#execute no invoca CU-16 y devuelve campos de disputa nulos. |
| TC-CU07-006 | Sí | Sí | Draft | Agente automatizado | 2026-11-13 | NavigationEndIntegrationTest#secondClickReturnsTripAlreadyCompleted: verde; Verifica primer 200 y segundo 409; spec exige respuesta correcta al reintento y plan precisa 409, queda decisión contractual y efectos completos sin verificar. |

## Brechas detectadas

1. [Contexto](../../../context/implementContext.md): hay controlador y servicio; las pruebas parciales no acreditan el flujo completo.
2. CU-16 existe y abre una disputa de 24 horas al invocarse directamente; CU-07 aún no lo invoca.
3. La entrega a M1, el primer intento en menos de un segundo y la notificación a M3 requieren servicios y medición; encolar no demuestra entrega ni pago.
4. JWT está pendiente y flota usa adaptadores fake por defecto; payload M3 T-12 pendiente.
5. El edge case de reintentos no define la respuesta correcta y el plan prevé 409 para cierre duplicado.
6. FR-011 a FR-013 requieren pruebas de frontend o integración UI.

Discrepancias verificadas: CU-07 no invoca CU-16 y devuelve campos de disputa nulos; el DTO StatusChangedEvent no incluye hora real aunque el spec la solicita para M3. Identidad por headers, no JWT.

## Convenciones de estado

- **Draft**: contrato incompleto o inconsistente.
- **In Process**: ejecutable con implementación actual.
- **Approved**: aprobado tras revisión y ejecución; ninguno aquí.
- **Blocked**: depende de funcionalidad pendiente.

## Ejecución automatizada — 2026-11-13

`sg docker -c 'bash ./mvnw -B clean test'`; salida 1 por ErrorCodeTest#noDuplicateCodes, sin atribuir ese fallo a CU-07. Las pruebas individuales no demuestran los flujos completos ni entrega externa.

| Clase | Pruebas | Fallos | Errores | Omitidos | Resultado |
|---|---:|---:|---:|---:|---|
| com.seashare.m2.reservas.application.service.EndNavigationServiceTest | 19 | 0 | 0 | 0 | Verde; cobertura parcial |
| com.seashare.m2.reservas.infrastructure.adapter.in.web.NavigationEndControllerTest | 7 | 0 | 0 | 0 | Verde; cobertura parcial |
| com.seashare.m2.reservas.integration.NavigationEndIntegrationTest | 7 | 0 | 0 | 0 | Verde; cobertura parcial |
| com.seashare.m2.disputa.integration.OpenDisputeIntegrationTest | 3 | 0 | 0 | 0 | Verde; cobertura parcial |
