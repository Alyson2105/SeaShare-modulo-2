# CU-21 — Pruebas backend de detalle de reserva

Casos para GET /api/v1/reservations/{id}, con identidad temporal por headers y datos enriquecidos.

**Estado global**: In Process por cobertura parcial y dependencia financiera bloqueada; evaluado el 2026-11-13.

## Trazabilidad

| TC | Escenario | FR | SC | Test automatizado parcial | Status |
|---|---|---|---|---|---|
| [TC-CU21-001](TC-CU21-001-detalle-propio.md) | Detalle de reserva propia | FR-001, FR-002, FR-003 | SC-002 | src/test/java/com/seashare/m2/reservas/integration/ReservationQueryTest.java#detailIncludesContractFields | In Process |
| [TC-CU21-002](TC-CU21-002-detalle-ajeno.md) | Rechazo de detalle ajeno | FR-001, FR-002 | SC-002 | src/test/java/com/seashare/m2/reservas/integration/ReservationQueryTest.java#detailOfOtherUserIs403 | In Process |
| [TC-CU21-003](TC-CU21-003-identificador-invalido.md) | Identificador inválido o inexistente | FR-001, FR-002 | SC-002 | src/test/java/com/seashare/m2/reservas/integration/ReservationQueryTest.java#invalidUuidIs400 | In Process |
| [TC-CU21-004](TC-CU21-004-total-estimado.md) | Total estimado persistido | FR-003, FR-004 | SC-002 | src/test/java/com/seashare/m2/reservas/integration/ReservationQueryTest.java#detailIncludesContractFields | In Process |
| [TC-CU21-005](TC-CU21-005-acciones-disponibles.md) | Acciones según estado | FR-005, FR-021 | SC-001 | src/test/java/com/seashare/m2/reservas/integration/ReservationQueryTest.java#detailIncludesContractFields | In Process |
| [TC-CU21-006](TC-CU21-006-desglose-financiero.md) | Desglose financiero oficial | FR-003, FR-004, FR-010 | SC-001 | ReservationDetailPolicyTest#reservedShowsBreakdownAndUsesLiquidatedTotal: solo datos simulados | Blocked |

## Cobertura de requisitos

| FR | Cobertura | TC | Alcance | Motivo / Observación |
|---|---|---|---|---|
| FR-001 | Parcial — ver caso y brechas | 001, 003 | Backend | Identificador |
| FR-002 | Parcial — ver caso y brechas | 001, 002, 003 | Backend | Pertenencia |
| FR-003 | Parcial — ver caso y brechas | 001, 004, 006 | Backend | Respuesta parcial; desglose pendiente |
| FR-004 | Parcial — ver caso y brechas | 004, 006 | Backend | Valor estimado, no monto oficial |
| FR-005 | Parcial — ver caso y brechas | 005 | Backend | Algunos flags implementados y probados; UI fuera |
| FR-006 | Sin cobertura backend | — | UI | UI: subestado y total |
| FR-007 | Sin cobertura backend | — | UI | UI: itinerario |
| FR-008 | Sin cobertura backend | — | UI | UI: servicios |
| FR-009 | Sin cobertura backend | — | UI | UI: contraparte |
| FR-010 | Parcial — ver caso y brechas | 006 | Backend | Monto oficial pendiente |
| FR-011 | Sin cobertura backend | — | UI | UI: resumen de pago |
| FR-012 | Sin cobertura backend | — | UI | UI: desglose navegación |
| FR-013 | Sin cobertura backend | — | UI | UI: garantía |
| FR-014 | Sin cobertura backend | — | UI | UI: compensación |
| FR-015 | Sin cobertura backend | — | UI | UI: acciones y badges |
| FR-016 | Sin cobertura backend | — | UI | UI: embarque |
| FR-017 | Sin cobertura backend | — | UI | UI: cortesía |
| FR-018 | Sin cobertura backend | — | UI | UI: inasistencia |
| FR-019 | Sin cobertura backend | — | UI | UI: viaje en curso |
| FR-020 | Sin cobertura backend | — | UI | UI: no-show |
| FR-021 | Parcial — ver caso y brechas | 005 | Backend | Acción de pago pendiente; UI fuera |

## Criterios de éxito

| SC | Estado | Observación |
|---|---|---|
| SC-001 | Parcial / UI | TC-005 prueba flag en RESERVED; no prueba acceso al flujo ni UI. |
| SC-002 | Parcial | TC-001 a 004; pertenencia según headers temporales, sin autenticación JWT. |

## Checklist de ejecución

| TC | Documentado | Ejecutado | Resultado | Ejecutado por | Fecha | Observaciones |
|---|---|---|---|---|---|---|
| TC-CU21-001 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Comprueba 200 y numerosos campos con identidad por headers y flota fake; no verifica todos los campos ni servicios reales. |
| TC-CU21-002 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Comprueba 403 para arrendatario ajeno; no comprueba propietario ajeno ni JWT. |
| TC-CU21-003 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Comprueba 400 INVALID_UUID; unknownIdIs404 comprueba 404 RESERVATION_NOT_FOUND. No verifica efectos persistidos. |
| TC-CU21-004 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Comprueba que total_paid es numérico; no compara el valor persistido ni verifica ausencia de cálculos. |
| TC-CU21-005 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Comprueba can_cancel en RESERVED; no demuestra navegación UI ni acción INITIATED para arrendatario. |
| TC-CU21-006 | Sí | Sí | Blocked | Agente automatizado | 2026-11-13 | Falta asociación efectiva de rubros oficiales de M3 al pago; ReservationDetailPolicyTest verifica rubros solo con datos introducidos en memoria. |

## Brechas detectadas

1. CU-21 expone subestado, acciones y tiempos; falta verificar la asociación de importes oficiales de M3.
2. Headers temporales no autentican la identidad; Spring Security y JWT están pendientes.
3. `estimated_total` se recibe del cliente en CU-02; CU-03 aún no asocia cálculo oficial de M3.
4. El flujo de cancelación y los de check-in y check-out siguen pendientes; el pago requiere integración y pruebas completas.
5. La mayoría de requisitos visuales necesitan pruebas frontend; no se infieren del backend.

## Convenciones de estado

- **Draft**: contrato incompleto o inconsistente.
- **In Process**: ejecutable con implementación actual.
- **Approved**: aprobado tras revisión y ejecución; ninguno aquí.
- **Blocked**: depende de funcionalidad pendiente.

## Ejecución automatizada — 2026-11-13

La suite terminó con 395 pruebas, 0 fallos, 0 errores y 0 omitidas. La integración usa PostgreSQL de Testcontainers, MockMvc y dependencias simuladas; las políticas usan datos en memoria.

| Clase | Tests | Fallos | Errores | Omitidos | Resultado |
|---|---:|---:|---:|---:|---|
| ReservationQueryTest | 12 | 0 | 0 | 0 | Exitosa |
| ReservationDetailPolicyTest | 6 | 0 | 0 | 0 | Exitosa |
