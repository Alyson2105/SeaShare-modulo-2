# CU-12 — Pruebas backend de cálculo total

Casos de CalculatedValuePort y cliente HTTP de M3; modo fake activo por defecto. CU-03 consume el puerto y asocia los montos.

**Estado global**: Draft; TC-001 depende de aclarar el identificador de cálculo. Evaluado el 2026-11-13.

## Trazabilidad

| TC | Escenario | FR | SC | Test automatizado parcial | Status |
|---|---|---|---|---|---|
| [TC-CU12-001](TC-CU12-001-calculo-exitoso.md) | Cálculo vinculante | FR-002, FR-003, FR-004, FR-005 | SC-001, SC-002 | [adaptador](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpCalculatedValueAdapterTest.java) | Draft |
| [TC-CU12-002](TC-CU12-002-montos-invalidos.md) | Desglose inválido | FR-005, FR-007 | SC-002, SC-003 | [adaptador](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpCalculatedValueAdapterTest.java) | In Process |
| [TC-CU12-003](TC-CU12-003-reintentos-404.md) | Información aún no disponible | FR-003, FR-004, FR-008 | SC-003, SC-004 | [adaptador](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpCalculatedValueAdapterTest.java) | In Process |
| [TC-CU12-004](TC-CU12-004-fallo-servicio.md) | Fallo del servicio financiero | FR-007, FR-008 | SC-004 | [adaptador](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpCalculatedValueAdapterTest.java) | In Process |
| [TC-CU12-005](TC-CU12-005-integracion-pago.md) | Asociación al pago | FR-001, FR-006, FR-009 | SC-001, SC-003 | [pago](../../../../src/test/java/com/seashare/m2/reservas/integration/PaymentIntegrationTest.java) | In Process |
| [TC-CU12-006](TC-CU12-006-auditoria-calculo.md) | Auditoría de cálculo | FR-010 | SC-001 | Sin prueba de CU-12 | Blocked |

## Cobertura de requisitos

| FR | Cobertura | TC | Alcance | Motivo / Observación |
|---|---|---|---|---|
| FR-001 | Parcial | 005 | Backend | CU-03 consume el puerto con cobertura parcial |
| FR-002 | Parcial | 001 | Backend | Sin aritmética local |
| FR-003 | Parcial | 001, 003 | Backend | Identificador de reserva |
| FR-004 | Parcial | 001, 003 | Backend | POST implementado; 404 sin reintento pese al plan |
| FR-005 | No verificable | 001, 002 | Backend | Desglose probado; identificador exigido por spec y ausente del plan |
| FR-006 | Parcial | 005 | Backend | Asociación implementada; cobertura con dobles |
| FR-007 | Parcial | 002, 004 | Backend | Errores M3 |
| FR-008 | Parcial | 003, 004 | Backend | Fallo de red |
| FR-009 | Parcial | 005 | Backend | Pago implementado; cobro real externo pendiente |
| FR-010 | Bloqueado | 006 | Backend | Auditoría de solicitud y respuesta no implementada |

## Criterios de éxito

| SC | Estado | Observación |
|---|---|---|
| SC-001 | Parcialmente demostrado | Montos y asociación probados con dobles; auditoría y neutralidad global pendientes. |
| SC-002 | Parcialmente demostrado | TC-001 y 002 cubren el desglose del adaptador. |
| SC-003 | Parcialmente demostrado | TC-005 prueba asociación con cálculo simulado; no demuestra todos los cobros. |
| SC-004 | Parcialmente demostrado | TC-004 prueba rechazo unitario y fallos de adaptador; faltan efectos integrados. |
| SC-005 | No verificable | SLA pendiente de ratificación y medición externa. |

## Checklist de ejecución

| TC | Documentado | Ejecutado | Resultado | Ejecutado por | Fecha | Observaciones |
|---|---|---|---|---|---|---|
| TC-CU12-001 | Sí | Sí | Draft | Agente automatizado | 2026-11-13 | Contrato contradictorio: FR-005 exige ID de cálculo y plan y entidades indican que M3 no lo envía. Componente WireMock: HttpCalculatedValueAdapterTest#correct200ReturnsBreakdown pasó para cuatro montos; no aclara el ID ni verifica cuerpo vacío. |
| TC-CU12-002 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Componente WireMock: HttpCalculatedValueAdapterTest#missingFieldThrowsCalculationUnavailable, #zeroTotalThrowsCalculationUnavailable, #zeroRentalThrowsCalculationUnavailable y #zeroInsuranceAndDepositAreAccepted pasaron. No cubren todas las variantes ni el flujo persistido. |
| TC-CU12-003 | Sí | Sí | Failed | Agente automatizado | 2026-11-13 | Componente WireMock: HttpCalculatedValueAdapterTest#notFoundFailsImmediatelyWithoutRetry y #persistentNotFoundThrowsCalculationUnavailable pasaron: un único POST y error. El plan y el caso esperan reintentos de 404; la prueba verifica un único POST. El test Java pasó, pero demuestra el fallo de ese comportamiento. |
| TC-CU12-004 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Componente WireMock: HttpCalculatedValueAdapterTest#serviceUnavailable503ThrowsCalculationUnavailable y #readTimeoutThrowsCalculationUnavailableWithoutRetry pasaron; unitario: StartPaymentServiceTest#calculationFailureAbortsWithoutTransition pasó con escritor no invocado. Falta comprobar reserva y M1 persistidos en integración. |
| TC-CU12-005 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Integración: PaymentIntegrationTest#startPaymentTransitionsAndReturnsBreakdown y #breakdownIsPersistedForDetailView pasaron con cálculo simulado; unitario: StartPaymentServiceTest#validPaymentDelegatesToTransactionalWriter pasó. No se verifica M3 real ni la entrega del evento previo. |
| TC-CU12-006 | Sí | No | Blocked | — | — | Sin ejecución automatizada; no existe asiento de solicitud y respuesta de cálculo. |

## Brechas detectadas

2. FR-005 exige identificador de cálculo, pero [plan](../plan.md) y entidades indican que M3 no lo envía.
3. Auditoría de solicitud y montos FR-010 no está implementada.
5. El SLA de M3 está pendiente; modo fake activo por defecto.
6. La confirmación del evento previo y la ausencia de bloqueo M1 requieren integración; la asociación a la reserva está implementada.

## Convenciones de estado

- **Draft**: contrato incompleto o inconsistente.
- **In Process**: ejecutable con implementación actual.
- **Approved**: escenario y postcondiciones demostrados por aserciones de la corrida V4, sin impedimentos de aprobación.
- **Blocked**: depende de funcionalidad pendiente.

## Ejecución automatizada — 2026-11-13

`sg docker -c 'bash ./mvnw -B clean test'`; inicio 2026-11-13T19:02:00-05:00, fin 2026-11-13T19:03:11-05:00; BASELINE_HEAD `568290a0d4334b477a4b0f9d5e949e981da3fa55`; salida 0. Las pruebas del adaptador usan WireMock; un test verde confirma ausencia de reintento de 404 contraria al caso.

| Clase | Pruebas | Fallos | Errores | Omitidos | Alcance |
|---|---:|---:|---:|---:|---|
| com.seashare.m2.reservas.application.service.StartPaymentServiceTest | 10 | 0 | 0 | 0 | Según TC; revisar discrepancia 404 |
| com.seashare.m2.reservas.infrastructure.adapter.out.finance.HttpCalculatedValueAdapterTest | 22 | 0 | 0 | 0 | Según TC; revisar discrepancia 404 |
| com.seashare.m2.reservas.integration.PaymentIntegrationTest | 10 | 0 | 0 | 0 | Según TC; revisar discrepancia 404 |

**Actualización local 2026-11-13:** TC-CU12-003-reintentos-404.md — HttpCalculatedValueAdapterTest#notFoundRetriesUntilCalculationIsAvailable y #persistentNotFoundThrowsCalculationUnavailableAfterThreeRetries: respuestas HTTP simuladas, no integración real con M3; ambas pasaron en la suite completa de 640 pruebas del 2026-11-13. Resultado parcial.

**Actualización local 2026-11-13:** TC-CU12-006-auditoria-calculo.md — CalculationAuditIntegrationTest verifica inserción de asientos SUCCESS y FAILED en PostgreSQL local; StartPaymentServiceTest verifica la invocación del puerto. Las 35 pruebas focalizadas pasaron el 2026-11-13. Falta demostrar el recorrido completo y la integración real con M3. Resultado parcial.
