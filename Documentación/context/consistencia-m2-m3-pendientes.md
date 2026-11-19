# Consistencia M2–M3: pendientes e inconsistencias

**Fecha:** 2026-11-19
**Alcance:** M2 (este repo) y M3 (`C:\Users\josue\Documents\IJ\SeaShareM3`), en modo solo lectura.
**Naturaleza:** documento provisional de trabajo. No sustituye al contrato `consistencia-m2-m3.md`; recoge lo que hay que corregir o confirmar.
**Contrato M1–M2:** `consistencia-m2-m1.md`. **Diagnóstico M1:** `consistencia-m1-m2-mantenimiento.md`.

---

## 0. Método

- **VERIFICADO:** leído en el código del módulo indicado, con `archivo:línea`.
- **DECLARADO:** decisión del equipo sin código que la respalde.
- **NO VERIFICABLE:** requiere entorno o ejecución.

---

## 1. Inconsistencias

### I1 — Declaración de colas incompatible (BLOQUEANTE)

Las tres colas compartidas se declaran con argumentos distintos en cada módulo. RabbitMQ rechaza redeclarar una cola existente con argumentos diferentes (`PRECONDITION_FAILED`, canal cerrado); el módulo que arranque segundo puede fallar al declarar.

| Argumento | M2 | M3 |
|---|---|---|
| Tipo | Clásica durable (`QueueBuilder.durable`) | **Quorum** (`QueueBuilder.durable(...).quorum()`) |
| `x-dead-letter-exchange` | `seashare.reservations.dlx` | `""` (exchange por defecto) |
| `x-dead-letter-routing-key` | Nombre de la cola | `<cola>.dlq` |
| Evidencia | M2 `RabbitTopologyConfig.java:39-46`; disputa en `DisputeRabbitTopologyConfig.java:32-50` | M3 `RabbitTopologyConfig.java:158-168` |

Colas afectadas: `finance.reservation-status.v1`, `finance.reservation-info.v1`, `finance.guarantee-dispute.v1`.

**Decisión 8 (DECLARADA):** se adopta la declaración de M3. M2 debe:

1. Declarar las tres colas como `quorum()`.
2. Usar `x-dead-letter-exchange=""` y `x-dead-letter-routing-key=<cola>.dlq`.
3. Eliminar el exchange `seashare.reservations.dlx` y sus bindings (M2 `RabbitTopologyConfig.java:35-37,60-66`).
4. Mantener las DLQ `<cola>.dlq` durables y clásicas.
5. Actualizar `RabbitTopologyTest`.

**Esfuerzo:** S · **Prioridad sábado:** **SÍ** (es el único cambio M2↔M3 que bloquea correr ambos contra el mismo broker).

### I2 — Tarifas base: ruta distinta entre M3 y M1

| Lado | Ruta | Evidencia |
|---|---|---|
| M3 llama | `POST /api/v1/fleet/base-rates` | M3 `FleetRateRestClient.java:32`; mock `wiremock/mappings/fleet-base-rates.json` |
| M1 expone | `POST /api/v1/boats/base-rates` | M1 `BaseRateController.java:22`; permitida en `SecurityConfig.java:89-90` |

Cualquier `4xx`/`5xx` se traduce a `BaseRateException(FLEET_UNAVAILABLE)` (M3 `FleetRateRestClient.java:125-128`), así que las estimaciones que dependan de tarifa base fallan.

**Resolución (DECLARADA):** se resuelve en M3 cambiando la constante, o con un alias en M1. **Esfuerzo:** S · **Prioridad sábado:** NO.

### I3 — CU-15 sin consumidor

M2 expone `GET /api/v1/internal/reservations/{id}` (M2 `InternalReservationController.java:24`). M3 **no lo llama**: obtiene la información por el evento `reservation.info.provided` (M3 `ReservationInfoListener.java:57`).

No es un defecto, pero hay que decidir si el endpoint se retira o se mantiene como contrato de consulta puntual. **Prioridad:** acuerdo, no bloquea.

### I4 — Convención de idempotencia distinta

- M2 publica con `Message-Id = event_id` y `Content-Type` JSON (M2 `RabbitEventPublisherAdapter.java:52-53`).
- M3 **no deduplica por `Message-Id`**: usa una clave de dominio en `reservation_status_log`, con `UNIQUE (reservation_id, status, operation_type, idempotency_key)` y `ON CONFLICT DO NOTHING` (M3 `ReservationStatusLogJpaRepository.java:22-27`; derivación de la clave en `HandleReservationStatusService.java:215-222`).

Funciona, pero conviene confirmarlo por escrito: un reenvío del mismo estado no duplica el efecto aunque cambie el `Message-Id`.

### I5 — `CANCELLED_BY_HOST`: verificado, pendiente de confirmar

| Hecho | Evidencia |
|---|---|
| M3 reconoce el estado | M3 `ReservationStatus.java:13` |
| Dispara reembolso | M3 `ReservationStatusPolicy.java:33` |
| Reembolso del 100 % | M3 `RefundCalculator.java:24-26` |
| El cobro nace en `PENDING` | M3 `HandleReservationStatusService.java:147-149` |
| El reembolso se pide al cancelar | M3 `:149,158-161` |
| No necesita montos ni `payment_metadata` | M3 `ReservationStatusListener.java:132-141` (solo exige `reservation_id`, `status`, `status_changed_at`; el resto puede ser `null`) |

**Pendiente:** confirmar con M3 que el actor sistema no requiere nada especial y que el reembolso no necesita que el cobro esté `CAPTURED`.

---

## 2. Lo que ya concuerda (verificado)

| Elemento | M2 | M3 |
|---|---|---|
| Exchange | `seashare.reservations` (`MessagingProperties.java:18`) | `AmqpNames.java:10` |
| Cola de estado | `finance.reservation-status.v1` (`:26`) | `AmqpNames.java:14` |
| Cola de info | `finance.reservation-info.v1` (`:28`) | `:13` |
| Cola de disputa | `finance.guarantee-dispute.v1` (`DisputeMessagingProperties.java:23`) | `:15` |
| Routing keys | `reservation.status.changed`, `reservation.info.provided`, `reservation.dispute.updated` | `AmqpNames.java:21-23` |
| Sobre AMQP | `Message-Id` + `Content-Type` JSON (`RabbitEventPublisherAdapter.java:52-53`) | lo exige (`ReservationInfoListener.java:65`; `ReservationStatusListener.java:63`) |
| Payload de info | 7 campos (`OutboxAdapter.java:94-100`) | mismos 7 (`ReservationInfoMessage.java:14-21`; `ReservationInfoListener.java:135-141`) |
| Payload de estado | `payment_metadata` con `payer_email` y `last_four` (`OutboxAdapter.java:60-71`) | `Map<String,String>` (`ReservationStatusMessage.java:21`) |
| Estimación individual | `POST /api/v1/estimates/individual` (`HttpIndividualEstimateAdapter.java:32`) | `EstimateController.java:41` |
| Estimación en lote | `POST /api/v1/estimates/batch` (`HttpBatchEstimateAdapter.java:40`) | `:33` |
| Valor calculado | `POST /api/v1/reservations/{id}/calculated-value` (`HttpCalculatedValueAdapter.java:31-32`) | `ReservationValueController.java:51` |
| Estado de cobro | `GET /api/v1/reservations/{id}/payment-confirmation` (`HttpPaymentStatusAdapter.java:30,32`) | `PaymentConfirmationController.java:51` |
| Reintentos de consumo | 5 intentos (M2 `BackoffPolicy`) | `max-retries=4` → 5 intentos (`application.properties:25`) |
| Dirección del pago | M2 consulta a M3 (canal B) | M3 no llama a M2 |

---

## 3. Preguntas para M3 (para enviar tal cual)

1. ¿Confirman que `CANCELLED_BY_HOST` con actor sistema reembolsa el 100 % sin pasos manuales?
2. ¿El reembolso requiere que el cobro esté `CAPTURED`, o funciona con `PENDING`?
3. ¿Aceptan que `payment_token_ref` y `payment_method_type` lleguen en `null` en la cancelación?
4. ¿Prefieren recibir `payment_metadata` también en la cancelación o lo dejan opcional?
5. ¿Pueden alinear la ruta de tarifas base a `/api/v1/boats/base-rates`, o prefieren que M1 exponga un alias?
6. ¿Mantendrán las colas como quorum con `DLX ""` y `<cola>.dlq`? Si sí, M2 se alinea con esa declaración.
7. ¿Siguen sin usar `GET /api/v1/internal/reservations/{id}` (CU-15)? ¿Se retira o se conserva?

---

## 4. Prioridad

| Pieza | ¿Sábado? | Motivo |
|---|---|---|
| I1 declaración de colas | **Sí** | Sin esto, M2 y M3 no pueden compartir broker |
| I2 tarifas base | No | Se resuelve en M3; no bloquea el mantenimiento |
| I3 CU-15 | No | Acuerdo de alcance |
| I4 idempotencia | No | Confirmación por escrito |
| I5 reembolso | No | Verificación funcional |
