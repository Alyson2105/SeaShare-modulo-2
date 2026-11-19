# Consistencia M2–M3: decisiones finales

**Fecha:** 2026-11-19
**Estado:** decisiones cerradas para la entrega del sábado. No hay preguntas abiertas ni alcance adicional; solo queda ejecutar I1 y la configuración ya decidida.
**Alcance:** M2 (este repo), M3 (`C:\Users\josue\Documents\IJ\seaShareM3`) y M1 (`C:\Users\josue\Documents\IJ\SeaShareM1`), en modo solo lectura.
**Regla de esta entrega:** **M3 es la referencia y no se modifica. M1 no se modifica. Solo M2 se adapta.** El contrato vivo es `consistencia-m2-m3.md`.

---

## 0. Método

- **VERIFICADO:** leído en el código del módulo indicado, con `archivo:línea`.
- **DECISIÓN:** cambio o no cambio acordado para M2.
- **ACEPTADO:** limitación conocida que se deja como está para esta entrega.

No se ejecutó RabbitMQ ni la suite; el conflicto de declaración de colas es verificable estáticamente y el `PRECONDITION_FAILED` es la consecuencia esperada.

---

## 1. Inconsistencias documentadas (I1–I5)

| ID | Hallazgo | Evidencia en M2 | Evidencia en M3 | Decisión final |
| --- | --- | --- | --- | --- |
| **I1** | Las tres colas compartidas se declaran incompatibles: M2 clásica durable con `x-dead-letter-exchange=seashare.reservations.dlx`; M3 quorum con `x-dead-letter-exchange=""` y `x-dead-letter-routing-key=<cola>.dlq`. | `RabbitTopologyConfig.java:35-47,60-66`; `DisputeRabbitTopologyConfig.java:32-50`; `RabbitTopologyTest.java:64-112`. | `RabbitTopologyConfig.java:158-168`. | **M2 se adapta**: las 3 colas pasan a `quorum()`, `DLX ""`, `rk <cola>.dlq`; DLQ durables clásicas; se elimina `seashare.reservations.dlx` y sus bindings en **los dos** configs; se actualiza `RabbitTopologyTest`. Las colas existentes deben borrarse y recrearse. |
| **I2** | M3 llama `POST /api/v1/fleet/base-rates`; M1 expone `POST /api/v1/boats/base-rates`. | No aplica: M2 no participa en esa llamada. | `FleetRateRestClient.java:32`; `RegisterReservationInformationService.java:84-97`. M1: `BaseRateController.java:22`. | **Fuera de M2.** Es una dependencia M3→M1. No se toca M1 ni M3. Se documenta como bloqueo externo: si el M1 cerrado no tiene el alias, UC02/UC03 no funciona. |
| **I3** | `GET /api/v1/internal/reservations/{id}` sin consumidor en M3. | `InternalReservationController.java:24`; `InternalReservationResponse.java:25-26,37`. | Sin cliente HTTP a M2; `ReservationInfoListener.java:57` usa el evento. | **Se conserva sin cambios.** M3 no lo usa; no se invierte en él. Se documenta como “no consumido por M3”. |
| **I4** | M2 usa `Message-Id = event_id`; M3 deduplica por clave de dominio, no por `Message-Id`. | `RabbitEventPublisherAdapter.java:52-54`; `OutboxAdapter.java:82,110,159`. | `ReservationStatusLogJpaRepository.java:20-27`; `HandleReservationStatusService.java:218-223`; `DisputeEventLogJpaRepository.java:20-27`. | **Sin cambio de código.** M2 ya cumple. Se documenta que `Message-Id` es trazabilidad y que en `PENDING` la clave incluye `status_changed_at` (un reintento con nueva marca es un cobro nuevo, por diseño). |
| **I5** | `CANCELLED_BY_HOST` reconocido, reembolso 100 %, sin montos ni `payment_metadata`. Faltaba confirmar si exige `CAPTURED`. | `FinanceStatusMapper.java:31`; `RequestCancellationService.java:107-111`; `OutboxAdapter.java:48-51`. | `ReservationStatus.java:13`; `ReservationStatusPolicy.java:33`; `RefundCalculator.java:24-25`; `ChargeIntentPersistenceAdapter.java:44-49`; `ReservationStatusMessageMapper.java:34-38`. | **Sin cambio de código.** M2 solo pasa a `RESERVED` con `APPROVED`, y M3 solo devuelve `APPROVED` para `CAPTURED`; la precondición se cumple. Se documenta. |

---

## 2. Hallazgos nuevos (N1–N16)

| ID | Hallazgo | Evidencia | Decisión final |
| --- | --- | --- | --- |
| **N1** | El contrato estaba truncado y referenciaba una §6 inexistente. | `consistencia-m2-m3.md` anterior, líneas 10, 96, 99 y 118. | **Corregido en `consistencia-m2-m3.md`**: §5 cerrada y §6 “Decisiones finales” creada. |
| **N2** | El contrato decía `ACCEPTED`; M3 usa `COMPLETED`; M2 traduce `ACCEPTED→COMPLETED`. | `DisputeStatus.java:5-9`; `DisputeEventMapper.java:20-22,31-33`; M3 `DisputeStatus.java:4-7`. | **Contrato corregido a `PENDING`/`REJECTED`/`COMPLETED`.** Código M2 sin cambio. |
| **N3** | El contrato admitía “motivo opcional” en la disputa; M2 no lo envía y M3 no lo acepta (`FAIL_ON_UNKNOWN_PROPERTIES=true`). | `DisputeOutboxAdapter.java:39-44`; `DisputeNotificationMessage.java:14-19`; `JacksonConfig.java:50-53`. | **Contrato corregido: sin motivo.** El motivo queda local en la revisión admin de M2. |
| **N4** | El contrato decía que M2 notifica `RESERVED` a M1 y libera al cancelar; M2 no envía `RESERVED` y `m1_blocked` solo se activa en `IN_NAVIGATION`. | `ModuleNotificationMatrix.java:41-45,52-64`; `ChangeReservationStatusService.java:134-136`. | **Contrato corregido**: M1 no recibe `RESERVED`; la ocupación se marca en `IN_NAVIGATION`; una cancelación desde `RESERVED` no envía `AVAILABLE`. Código M2 sin cambio. |
| **N5** | “Mismo conjunto de estados” y “5 sub-estados” no se sostenían: M3 tiene 10 valores, no tiene `EXPIRED/PAYMENT_FAILED` y colapsa `LATE/NO_SHOW` en `CANCELLED_STRICT`. | `ReservationStatus.java:3-15`; `CancellationSubtype.java:3-9`; `FinanceStatusMapper.java:27-32`; M3 `ReservationStatus.java:4-15`. | **Contrato corregido** con la tabla real de estados y el mapeo. Código M2 sin cambio. |
| **N6** | El contrato describía “estado principal + sub-estado”; el payload real es un único `status` compuesto. | `OutboxAdapter.java:48-52`; `ReservationStatusMessage.java:15-21`. | **Contrato corregido**: un solo campo `status`. Código M2 sin cambio. |
| **N7** | La estimación en lote no acepta fechas/pasajeros; M3 fija hoy/1 día/1 pasajero. | `HttpBatchEstimateAdapter.java:167-170`; `EstimateBatchRequest.java:19-22`; `EstimateBatchService.java:40-41,84-87`. | **Contrato corregido**: el lote solo lleva `boat_ids` y es una cotización de referencia. Código M2 sin cambio. |
| **N8** | `seashare.finance.base-url` por defecto 8083; M3 sirve en 8080. | `application.yaml:87`; M3 sin `server.port`; `docker-compose.yml:53`. | **M2 cambia el default a `http://localhost:8080`.** En producción sigue por variable de entorno. |
| **N9** | Los compose de M2 y M3 chocan en 5433 y 5672/15672. | `docker-compose.yml:15,26-27` (M2); `docker-compose.yml:13,29-30` (M3). | **M2 deja de publicar RabbitMQ en 5672/15672 y usa el broker de M3; su PostgreSQL pasa a 5434.** Puertos configurables por entorno. |
| **N10** | Un reenvío de `reservation.info.provided` invalida los montos calculados en M3; un estado posterior se ackea y se pierde. | `OutboxAdapter.java:110-116`; `PublishPendingOutboxService.java:129-136`; M3 `ReservationInformationPersistenceAdapter.java:44-50`; `HandleReservationStatusService.java:112-116`. | **ACEPTADO.** No se añade reconciliación en M2 para esta entrega. El camino feliz funciona; el riesgo de un reenvío ambiguo se documenta. |
| **N11** | Si el estado llega antes de la info (o tras DLQ de info), M3 lo trata como error permanente y hace ack. | `OutboxMessageJpaRepository.java:13-22`; M3 `HandleReservationStatusService.java:107-111`; `ReservationStatusListener.java:102-111`. | **ACEPTADO**, cubierto por la misma limitación de N10. No se añade cola de reconciliación. |
| **N12** | M2 exige `installments` y `payment_method_id`, pero no los envía; M3 fija `installments=1` y deriva la marca de `payment_method_type`. | `PaymentController.java:71-84`; `StartPaymentRequest.java:32-46`; `OutboxAdapter.java:60-71`; M3 `SendChargeToGatewayService.java:42,111-116`. | **ACEPTADO.** No se cambia la API de M2. Se documenta en `CU-03`, `CU-14` y `cambios-contrato-frontend.md`: M2 los valida, M3 no los usa; la marca debe ir en `payment_method_type` si se quiere que llegue a la pasarela. |
| **N13** | Frontera de 72 h: el contrato decía `>`; el código usa `>=`. | `CancellationClassifier.java:12,26-31`. | **Se documenta `>= 72 h` como regla final.** No se toca la lógica ni los tests. |
| **N14** | El contrato decía “10 % de la tarifa base diaria”; M3 aplica 10 % sobre la tarifa ajustada por fin de semana/temporada alta. | `ReservationValueCalculator.java:28,38-42`; `ProvideBaseRateService.java:108-121`; `DynamicRatePolicy.java:44-66`. | **Contrato corregido**: 10 % de la tarifa diaria vigente con ajuste dinámico aplicado. Código M2 sin cambio. |
| **N15** | M2 tiene `PaymentStatus.FAILED`; M3 nunca lo emite. | `PaymentStatus.java:5-17`; `ConfirmPaymentService.java:119-137`; M3 `PaymentConfirmationStatus.java:9-30`. | **ACEPTADO.** Se conserva como valor tolerado; no se toca código. |
| **N16** | `X-Correlation-Id` no se propaga en 3 de 4 llamadas HTTP; en AMQP el relay no tiene MDC y M3 no lo lee. | `HttpCalculatedValueAdapter.java:102-105`; `RabbitEventPublisherAdapter.java:55-58`; M3 `CorrelationIdFilter.java:25-36`. | **ACEPTADO.** No se cambia para esta entrega. La trazabilidad queda limitada a los logs de cada módulo. |

---

## 3. Qué cambia en M2

### 3.1 Documentación (esta rama)

- `Documentación/context/consistencia-m2-m3.md`: contrato cerrado y corregido.
- `Documentación/context/consistencia-m2-m3-pendientes.md`: este registro de decisiones finales.
- `Documentación/context/cambios-contrato-frontend.md`: nota de pagos/cuotas.
- `Documentación/features/CU-03-iniciar-pago/plan.md`: nota de `payment_method_id`/`installments`.
- `Documentación/features/CU-14-recibir-estado-reserva/plan.md`: `payment_metadata` real y deduplicación.

### 3.2 Código (solo I1)

- `RabbitTopologyConfig`: colas quorum, `DLX ""`, `rk <cola>.dlq`; sin `seashare.reservations.dlx`.
- `DisputeRabbitTopologyConfig`: lo mismo para la cola de disputa.
- `MessagingProperties` y `DisputeMessagingProperties`: retirar `dead-letter-exchange`.
- `application.yaml`: retirar `m2.messaging.dead-letter-exchange`.
- `RabbitTopologyTest`: ajustar a la topología de M3.

### 3.3 Configuración/entorno

- **N8**: default `seashare.finance.base-url` → `http://localhost:8080`.
- **N9**: M2 no publica 5672/15672; usa el broker de M3. PostgreSQL de M2 a 5434.
- Para la demo real: `SEASHARE_FINANCE_MODE=http`, `SEASHARE_FINANCE_BASE_URL=http://localhost:8080`, `SEASHARE_FLEET_MODE=http`, `SEASHARE_FLEET_BASE_URL=http://localhost:8081`.

### 3.4 Lo que no se cambia

- **N10/N11**: no hay job de reconciliación ni deduplicación de info. Riesgo aceptado.
- **N12**: la API de pagos de M2 sigue igual; solo se documenta la limitación.
- **N13**: la lógica de cancelación sigue con `>= 72 h`.
- **N14**: M2 no calcula montos; solo se corrige el documento.
- **N15**: `PaymentStatus.FAILED` se conserva.
- **N16**: no se añade propagación de correlación.

---

## 4. Lo que ya concuerda (verificado)

| Elemento | M2 | M3 |
| --- | --- | --- |
| Exchange | `seashare.reservations` (`MessagingProperties.java:18`) | `AmqpNames.java:10` |
| Cola de estado | `finance.reservation-status.v1` (`:26`) | `AmqpNames.java:14` |
| Cola de info | `finance.reservation-info.v1` (`:28`) | `AmqpNames.java:13` |
| Cola de disputa | `finance.guarantee-dispute.v1` (`DisputeMessagingProperties.java:23`) | `AmqpNames.java:15` |
| Routing keys | `reservation.status.changed`, `reservation.info.provided`, `reservation.dispute.updated` | `AmqpNames.java:21-23` |
| Sobre AMQP | `Message-Id` + `Content-Type` JSON (`RabbitEventPublisherAdapter.java:52-54`) | lo exige (`ReservationInfoListener.java:65`; `ReservationStatusListener.java:63`) |
| Payload de info | 7 campos (`OutboxAdapter.java:94-100`) | mismos 7 (`ReservationInfoMessage.java:14-21`) |
| Payload de estado | `payment_metadata` con `payer_email` y `last_four` (`OutboxAdapter.java:60-71`) | `Map<String,String>` (`ReservationStatusMessage.java:21`) |
| Estimación individual | `POST /api/v1/estimates/individual` (`HttpIndividualEstimateAdapter.java:32`) | `EstimateController.java:41` |
| Estimación en lote | `POST /api/v1/estimates/batch` (`HttpBatchEstimateAdapter.java:40`) | `EstimateController.java:33` |
| Valor calculado | `POST /api/v1/reservations/{id}/calculated-value` (`HttpCalculatedValueAdapter.java:31-32`) | `ReservationValueController.java:51` |
| Estado de cobro | `GET /api/v1/reservations/{id}/payment-confirmation` (`HttpPaymentStatusAdapter.java:30,32`) | `PaymentConfirmationController.java:51` |
| Reintentos | Publicación outbox: 5 intentos (`application.yaml:55`) | Consumo: 5 intentos (`max-retries=4` → 5, `application.properties:25`) |
| Dirección | M2 consulta a M3; no hay M3→M2 | M3 no llama a M2 |

---

## 5. Dependencia externa I2

- **Qué**: M3 consulta la tarifa base a M1 en `POST /api/v1/fleet/base-rates` (`FleetRateRestClient.java:32`); M1 la expone en `POST /api/v1/boats/base-rates` (`BaseRateController.java:22`).
- **Impacto**: sin tarifa base, UC03 no registra `reservation_information`; sin esa fila, UC04 responde 404 y no hay cobro. Afecta al flujo real, no solo a las estimaciones.
- **Acción de M2**: ninguna. Es una llamada M3→M1.
- **Única salida**: alias en M1 o cambio de constante en M3. Ambas fuera de M2 y cerradas para esta entrega. Si el M1 que se levanta el sábado no tiene el alias, la demo debe usar `SEASHARE_FINANCE_MODE=fake` o no podrá mostrar la integración real.

---

## 6. Orden de ejecución del sábado

1. Aplicar esta documentación (rama `docs/consistencia-m2-m3-final`).
2. Aplicar **I1** en M2 y actualizar `RabbitTopologyTest`.
3. Aplicar **N8** y **N9**; levantar M2 contra el broker de M3 con `finance.mode=http`.
4. Smoke test: reserva → `reservation.info.provided` → `calculated-value` → pago → `PENDING` → `RESERVED` → `COMPLETED` → disputa → `REJECTED`/`COMPLETED`.
5. Congelar. No hay fase 2.
