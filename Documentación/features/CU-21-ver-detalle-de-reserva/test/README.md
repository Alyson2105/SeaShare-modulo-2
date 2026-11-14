# CU-21 — Pruebas backend de detalle de reserva

Casos para GET /api/v1/reservations/{id}, con identidad temporal por headers y datos enriquecidos.

**Estado global**: Draft por discrepancia del total en TC-004; evaluado el 2026-11-13.

## Trazabilidad

| TC | Escenario | FR | SC | Test automatizado parcial | Status |
|---|---|---|---|---|---|
| [TC-CU21-001](TC-CU21-001-detalle-propio.md) | Detalle de reserva propia | FR-001, FR-002, FR-003 | SC-002 | src/test/java/com/seashare/m2/reservas/integration/ReservationQueryTest.java#detailIncludesContractFields | In Process |
| [TC-CU21-002](TC-CU21-002-detalle-ajeno.md) | Rechazo de detalle ajeno | FR-001, FR-002 | SC-002 | src/test/java/com/seashare/m2/reservas/integration/ReservationQueryTest.java#detailOfOtherUserIs403 | In Process |
| [TC-CU21-003](TC-CU21-003-identificador-invalido.md) | Identificador inválido o inexistente | FR-001, FR-002 | SC-002 | src/test/java/com/seashare/m2/reservas/integration/ReservationQueryTest.java#invalidUuidIs400 | In Process |
| [TC-CU21-004](TC-CU21-004-total-estimado.md) | Total estimado persistido | FR-003, FR-004 | SC-002 | src/test/java/com/seashare/m2/reservas/integration/ReservationQueryTest.java#detailIncludesContractFields | Draft |
| [TC-CU21-005](TC-CU21-005-acciones-disponibles.md) | Acciones según estado | FR-005, FR-021 | SC-001 | src/test/java/com/seashare/m2/reservas/integration/ReservationQueryTest.java#detailIncludesContractFields | In Process |
| [TC-CU21-006](TC-CU21-006-desglose-financiero.md) | Desglose financiero oficial | FR-003, FR-004, FR-010 | SC-001 | PaymentIntegrationTest#breakdownIsPersistedForDetailView: cálculo simulado | In Process |

## Cobertura de requisitos

| FR | Cobertura | TC | Alcance | Motivo / Observación |
|---|---|---|---|---|
| FR-001 | Parcial | 001, 003 | Backend | Identificador |
| FR-002 | Parcial | 001, 002, 003 | Backend | Pertenencia |
| FR-003 | Parcial | 001, 004, 006 | Backend | Desglose probado con cálculo simulado |
| FR-004 | Parcial | 004, 006 | Backend | Detalle conserva cálculo del puerto; M3 real sin verificar |
| FR-005 | Parcial | 005 | Backend | Algunos flags implementados y probados; UI fuera |
| FR-006 | Sin evidencia | — | UI | UI: subestado y total |
| FR-007 | Sin evidencia | — | UI | UI: itinerario |
| FR-008 | Sin evidencia | — | UI | UI: servicios |
| FR-009 | Sin evidencia | — | UI | UI: contraparte |
| FR-010 | Parcial | 006 | Backend | Rubros probados con cálculo simulado |
| FR-011 | Sin evidencia | — | UI | UI: resumen de pago |
| FR-012 | Sin evidencia | — | UI | UI: desglose navegación |
| FR-013 | Sin evidencia | — | UI | UI: garantía |
| FR-014 | Sin evidencia | — | UI | UI: compensación |
| FR-015 | Sin evidencia | — | UI | UI: acciones y badges |
| FR-016 | Sin evidencia | — | UI | UI: embarque |
| FR-017 | Sin evidencia | — | UI | UI: cortesía |
| FR-018 | Sin evidencia | — | UI | UI: inasistencia |
| FR-019 | Sin evidencia | — | UI | UI: viaje en curso |
| FR-020 | Sin evidencia | — | UI | UI: no-show |
| FR-021 | Parcial | 005 | Backend | Acción de pago pendiente; UI fuera |

## Criterios de éxito

| SC | Estado | Observación |
|---|---|---|
| SC-001 | Parcialmente demostrado | TC-005 prueba flag en RESERVED; no prueba acceso al flujo ni UI. |
| SC-002 | Parcialmente demostrado | TC-001 a 004; pertenencia según headers temporales, sin autenticación JWT. |

## Checklist de ejecución

| TC | Documentado | Ejecutado | Resultado | Ejecutado por | Fecha | Observaciones |
|---|---|---|---|---|---|---|
| TC-CU21-001 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Comprueba 200 y numerosos campos con identidad por headers y flota fake; no verifica todos los campos ni servicios reales. |
| TC-CU21-002 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Comprueba 403 para arrendatario ajeno; no comprueba propietario ajeno ni JWT. |
| TC-CU21-003 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Comprueba 400 INVALID_UUID; unknownIdIs404 comprueba 404 RESERVATION_NOT_FOUND. No verifica efectos persistidos. |
| TC-CU21-004 | Sí | Sí | Draft | Agente automatizado | 2026-11-13 | Comprueba total_paid nulo sin liquidación; spec FR-004 y plan §2 difieren para la estimación previa, y el caso pide estimated_total como cadena. |
| TC-CU21-005 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Comprueba can_cancel en RESERVED; no demuestra navegación UI ni acción INITIATED para arrendatario. |
| TC-CU21-006 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | PaymentIntegrationTest#breakdownIsPersistedForDetailView pasó: rubros y total en GET tras pago con cálculo simulado; M3 real no verificado. |

## Brechas detectadas

1. CU-21 expone subestado, acciones y tiempos; el desglose persiste con cálculo simulado, no se prueba M3 real.
2. Headers temporales no autentican la identidad; Spring Security y JWT están pendientes.
3. `estimated_total` se recibe del cliente en CU-02; CU-03 asocia cálculo recibido del puerto de M3.
4. Cancelación, check-in y check-out están implementados; requieren pruebas completas con sus dependencias.
5. La mayoría de requisitos visuales necesitan pruebas frontend; no se infieren del backend.

## Convenciones de estado

- **Draft**: contrato incompleto o inconsistente.
- **In Process**: ejecutable con implementación actual.
- **Approved**: escenario, postcondiciones y FR/SC demostrados por aserciones de pruebas V4 ejecutadas.
- **Blocked**: depende de funcionalidad pendiente.

## Ejecución automatizada — 2026-11-13

`sg docker -c 'bash ./mvnw -B clean test'`; inicio 2026-11-13T19:02:00-05:00, fin 2026-11-13T19:03:11-05:00, salida 0; código base `568290a0d4334b477a4b0f9d5e949e981da3fa55`. Identidad mediante headers; flota y cálculo de M3 simulados.

| Clase | Tests | Fallos | Errores | Omitidos | Resultado |
|---|---:|---:|---:|---:|---|
| [ReservationQueryTest](../../../../src/test/java/com/seashare/m2/reservas/integration/ReservationQueryTest.java) | 13 | 0 | 0 | 0 | XML V4 exitoso; alcance parcial |
| [ReservationDetailPolicyTest](../../../../src/test/java/com/seashare/m2/reservas/application/service/ReservationDetailPolicyTest.java) | 7 | 0 | 0 | 0 | XML V4 exitoso; política unitaria |
| [PaymentIntegrationTest](../../../../src/test/java/com/seashare/m2/reservas/integration/PaymentIntegrationTest.java) | 10 | 0 | 0 | 0 | XML V4 exitoso; cálculo simulado |
