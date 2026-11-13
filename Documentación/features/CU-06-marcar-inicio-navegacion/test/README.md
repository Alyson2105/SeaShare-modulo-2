# CU-06 — Pruebas backend de inicio de navegación

Casos para el contrato propuesto `POST /api/v1/reservations/{reservation_id}/navigation-start`. CU-06 tiene controlador y servicio; CU-08 ofrece componentes internos. UI y servicios externos requieren pruebas independientes.

**Estado global**: Draft; evidencia parcial y brechas de integración. Evaluado el 2026-11-13.

## Trazabilidad

| TC | Escenario | FR | SC | Test automatizado parcial | Status |
|---|---|---|---|---|---|
| [TC-CU06-001](TC-CU06-001-inicio-puntual.md) | Inicio de navegación | FR-001, FR-003, FR-005, FR-006 | SC-002 | NavigationStartIntegrationTest#successfulStartPersistsDepartureStateAndCheckinEvent (parcial) ; [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | In Process |
| [TC-CU06-002](TC-CU06-002-inicio-anticipado.md) | Rechazo de salida anticipada | FR-003 | SC-002 | NavigationStartIntegrationTest#clockBeforeScheduledDepartureReturnsEarlyDeparture (parcial) | Draft |
| [TC-CU06-003](TC-CU06-003-estado-incompatible.md) | Estado no admisible | FR-001, FR-004 | SC-001 | StartNavigationServiceTest#pendingPaymentCancelledAndCompletedAreInvalidStateWithoutWrites (parcial) ; [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | In Process |
| [TC-CU06-004](TC-CU06-004-propietario-no-autorizado.md) | Autorización del propietario | FR-002 | SC-006 | NavigationStartIntegrationTest#renterAndForeignOwnerAreForbiddenAndUnknownIs404 (parcial) | In Process |
| [TC-CU06-005](TC-CU06-005-travesia-anterior.md) | Travesía anterior sin cerrar | FR-001, FR-004 | SC-001 | NavigationStartIntegrationTest#anotherReservationOfSameVesselInNavigationIsRejected (parcial) | In Process |
| [TC-CU06-006](TC-CU06-006-notificaciones-checkin.md) | Notificaciones y registro | FR-005, FR-006, FR-007, FR-008, FR-010 | SC-002, SC-004, SC-005 | NavigationStartIntegrationTest#successfulStartPersistsDepartureStateAndCheckinEvent (parcial) ; [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [matriz](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java) | In Process |
| [TC-CU06-007](TC-CU06-007-bloqueo-cancelacion-inasistencia.md) | Inadmisibilidad después del inicio | FR-009 | SC-003 | NavigationStartIntegrationTest#renterCancellationAfterStartIsRejected (parcial) ; [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | In Process |
| [TC-CU06-008](TC-CU06-008-carrera-inasistencia.md) | Carrera con inasistencia | FR-001, FR-005, FR-009 | SC-001, SC-003 | NoShowIntegrationTest#navigationStartAfterNoShowIsInvalidState (parcial) ; [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | In Process |

## Cobertura de requisitos

| FR | Cobertura | TC | Alcance | Motivo / Observación |
|---|---|---|---|---|
| FR-001 | Parcial | 001, 003, 005, 008 | Backend | Evidencia parcial y límites concretos en los TC asociados. |
| FR-002 | Parcial | 004 | Backend | Evidencia parcial y límites concretos en los TC asociados. |
| FR-003 | Parcial | 001, 002 | Backend | Evidencia parcial y límites concretos en los TC asociados. |
| FR-004 | Parcial | 003, 005 | Backend | Evidencia parcial y límites concretos en los TC asociados. |
| FR-005 | Parcial | 001, 006, 008 | Backend | Evidencia parcial y límites concretos en los TC asociados. |
| FR-006 | Parcial | 001, 006 | Backend | Evidencia parcial y límites concretos en los TC asociados. |
| FR-007 | Parcial | 006 | Backend | Evidencia parcial y límites concretos en los TC asociados. |
| FR-008 | Parcial | 006 | Backend | Evidencia parcial y límites concretos en los TC asociados. |
| FR-009 | Parcial | 007, 008 | Backend | Evidencia parcial y límites concretos en los TC asociados. |
| FR-010 | Sin evidencia integral | 006 | Backend | No se ha probado la ausencia integral de operaciones financieras. |
| FR-011 | Sin cobertura backend | — | UI | Visualización e interacción requieren pruebas frontend. |
| FR-012 | Sin cobertura backend | — | UI | Visualización e interacción requieren pruebas frontend. |
| FR-013 | Sin cobertura backend | — | UI | Visualización e interacción requieren pruebas frontend. |
| FR-014 | Sin cobertura backend | — | UI | Visualización e interacción requieren pruebas frontend. |
| FR-015 | Sin cobertura backend | — | UI | Visualización e interacción requieren pruebas frontend. |

## Criterios de éxito

| SC | Estado | Observación |
|---|---|---|
| SC-001 | Parcial | Evidencia parcial; revisar límites y pruebas de los TC asociados. |
| SC-002 | Parcial | Evidencia parcial; revisar límites y pruebas de los TC asociados. |
| SC-003 | Parcial | Evidencia parcial; revisar límites y pruebas de los TC asociados. |
| SC-004 | Parcial | Evidencia parcial; revisar límites y pruebas de los TC asociados. |
| SC-005 | Sin evidencia integral | La ausencia integral de operaciones financieras requiere verificación. |
| SC-006 | Parcial | Evidencia parcial; revisar límites y pruebas de los TC asociados. |

## Checklist de ejecución

| TC | Documentado | Ejecutado | Resultado | Ejecutado por | Fecha | Observaciones |
|---|---|---|---|---|---|---|
| TC-CU06-001 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | NavigationStartIntegrationTest#successfulStartPersistsDepartureStateAndCheckinEvent: verde; Verifica HTTP y check-in persistido; no verifica conservación de hora pactada ni entrega externa. |
| TC-CU06-002 | Sí | Sí | Draft | Agente automatizado | 2026-11-13 | NavigationStartIntegrationTest#clockBeforeScheduledDepartureReturnsEarlyDeparture: verde; Verifica rechazo 409; el edge case del spec admite salida ligeramente anterior y FR-003 la prohíbe. |
| TC-CU06-003 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | StartNavigationServiceTest#pendingPaymentCancelledAndCompletedAreInvalidStateWithoutWrites: verde; Cubre tres estados con mocks, no todos los de la ficha. |
| TC-CU06-004 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | NavigationStartIntegrationTest#renterAndForeignOwnerAreForbiddenAndUnknownIs404: verde; Verifica 403; identidad temporal por headers, no JWT ni efectos persistidos posteriores. |
| TC-CU06-005 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | NavigationStartIntegrationTest#anotherReservationOfSameVesselInNavigationIsRejected: verde; Verifica dos reservas secuenciales y rechazo; no demuestra inicios simultáneos. |
| TC-CU06-006 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | NavigationStartIntegrationTest#successfulStartPersistsDepartureStateAndCheckinEvent: verde; Verifica check-in persistido; no acredita entrega externa, seguro real ni plazo a M1. |
| TC-CU06-007 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | NavigationStartIntegrationTest#renterCancellationAfterStartIsRejected: verde; Verifica rechazo de cancelación tras inicio; no ejecuta inicio seguido de no-show. |
| TC-CU06-008 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | NoShowIntegrationTest#navigationStartAfterNoShowIsInvalidState: verde; Prueba no-show seguido de inicio secuencial; no prueba simultaneidad ni un único ganador. |

## Brechas detectadas

1. [Contexto](../../../context/implementContext.md): hay controlador y servicio; las pruebas parciales no acreditan el flujo completo.
2. El edge case del [spec](../spec.md) menciona salida ligeramente anterior, mientras FR-003 y el [plan](../plan.md) la prohíben; confirmar contrato.
3. El plan declara `insurance_activated: true` pese a notificación asíncrona; emisión no prueba activación del seguro por M3.
4. El primer intento a M1 en menos de un segundo requiere medición con servicios; el encolado no lo demuestra.
5. JWT está pendiente y flota usa adaptadores fake por defecto; payload M3 T-12 pendiente.
6. FR-011 a FR-015 requieren pruebas de frontend o integración UI.

Discrepancias verificadas: El spec FR-003 prohíbe salida temprana, pero su edge case admite una salida ligeramente anterior; StartNavigationService rechaza antes de la hora. La identidad actual usa headers, no JWT.

## Convenciones de estado

- **Draft**: contrato incompleto o inconsistente.
- **In Process**: ejecutable con implementación actual.
- **Approved**: aprobado tras revisión y ejecución; ninguno aquí.
- **Blocked**: depende de funcionalidad pendiente.

## Ejecución automatizada — 2026-11-13

`sg docker -c 'bash ./mvnw -B clean test'`; salida 1 por ErrorCodeTest#noDuplicateCodes, sin atribuir ese fallo a CU-06. Las pruebas individuales no demuestran los flujos completos ni entrega externa.

| Clase | Pruebas | Fallos | Errores | Omitidos | Resultado |
|---|---:|---:|---:|---:|---|
| com.seashare.m2.reservas.application.service.StartNavigationServiceTest | 21 | 0 | 0 | 0 | Verde; cobertura parcial |
| com.seashare.m2.reservas.infrastructure.adapter.in.web.NavigationStartControllerTest | 7 | 0 | 0 | 0 | Verde; cobertura parcial |
| com.seashare.m2.reservas.integration.NavigationStartIntegrationTest | 10 | 0 | 0 | 0 | Verde; cobertura parcial |
| com.seashare.m2.reservas.integration.NoShowIntegrationTest | 9 | 0 | 0 | 0 | Verde; cobertura parcial |
