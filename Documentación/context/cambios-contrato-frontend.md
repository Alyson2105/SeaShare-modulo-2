# Cambios de contrato para el frontend

Cambios de las ramas `feature/h10-cierre` y `feature/h10-limpieza`, fecha 2026-11-13.

## total_paid

- El detalle (CU-21) arma `payment_summary.total_paid` con `view.totalAmount()` en `ReservationDetailPolicy.java:49`; puede ser `null` si aún no hay liquidación y **no** tiene respaldo en `estimated_total`.
- Test: `ReservationDetailPolicyTest.totalPaidIsNullWhenTotalAmountIsMissing`.
- El listado (CU-20) no tiene `total_paid`; su `total_price` sí usa `estimated_total` cuando `total_amount` es `null` (`ReservationListItemResponse.java:150-151`).

## Límites de tamaño

- `CancellationRequest` (CU-04): `reason` `@Size(max = 500)` y `justification` `@Size(max = 500)`.
- `StartPaymentRequest` (CU-03): `payment_token_ref` 255, `payment_method_type` 50, `payer_email` 254, `payment_method_id` 100, `last_four` 4.
- Violación de `@Size`: HTTP 400 con `INVALID_PARAMETERS` y message = primer mensaje del campo (`GlobalExceptionHandler`).

## Cache-Control

- `no-store` presente en: `POST /api/v1/reservations`, `GET /api/v1/reservations`, `GET /api/v1/reservations/{id}`, `GET /api/v1/internal/reservations/{id}`, `POST /api/v1/reservations/{reservationId}/payments`, `GET /api/v1/vessels`, `GET /api/v1/vessels/{vessel_id}`, `GET /api/v1/vessels/{vessel_id}/quotations`.
- `no-store` ausente en: `POST .../cancellation`, `POST .../no-show`, `POST .../navigation-start`, `POST .../navigation-end`.

## Ruta inexistente

- `GlobalExceptionHandler` responde 404 con code `RESERVATION_NOT_FOUND` y message `"Resource not found"` en rutas inexistentes.
- Desajuste semántico conocido: el código genérico quedó aplazado; el frontend no lee `body.code`, solo `message` y `status`.

## Pendiente de decidir con el grupo

- Código 404 genérico para rutas inexistentes (hoy `RESERVATION_NOT_FOUND`).
- Si las respuestas de `cancellation`, `no-show`, `navigation-start` y `navigation-end` deben llevar `Cache-Control: no-store`.
- `RESERVATION_EXPIRED_REVERSION_REQUIRED`: el plan de CU-13 lo define como 409, pero el código resuelve el pago tardío con `LatePaymentReversalEvent` (`ConfirmPaymentService.java:149-160`, resultado `LATE_REVERSAL`). La constante en `ErrorCode.java:25` no se usa; borrarla cambiaría el conteo de 32 (decisión del grupo).
