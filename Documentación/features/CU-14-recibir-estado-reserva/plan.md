# Contrato de Evento: Estado de Reserva (CU-14 → M3)

| Campo | Valor |
|---|---|
| Módulo productor | Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones |
| Módulo consumidor | Módulo 3 (UC07 Brindar el estado de la reserva) |
| Spec de referencia | CU-14-recibir-estado-reserva/spec.md y CU-08-actualizar-estado-reserva/spec.md |
| Tipo | Asíncrono, unidireccional (AMQP / RabbitMQ). M3 no responde |
| Fecha | 2026-10-09 |

## 1. Propósito

Cada vez que una reserva cambia de estado, M2 le avisa a M3 cuál es el estado nuevo. M3 decide qué hacer con el dinero (cobrar, reembolsar, liquidar). M2 solo informa el estado y **nunca envía montos**.

## 2. Cuándo se publica

- Cada vez que CU-08 cambia el estado de una reserva y ese estado está en la tabla de la sección 4.
- La primera publicación es `PENDING`. Cuando la reserva nace en Iniciada no se publica ningún cambio de estado (solo el evento de información de reserva, ver CU-02).
- Si el arrendatario reintenta el pago con otra tarjeta (CU-03), se publica `PENDING` otra vez con un `Message-Id` y un token de pago nuevos. El TTL no se reinicia.

## 3. Dónde se publica

| Elemento | Valor |
|---|---|
| Exchange | `seashare.reservations` (topic) |
| Routing key | `reservation.status.changed` |
| Cola de M3 | `finance.reservation-status.v1` |

## 4. Mensaje

Headers AMQP:

| Header | Obligatorio | Valor |
|---|---|---|
| `Message-Id` | Sí | UUID único del mensaje (M3 lo usa para descartar duplicados) |
| `Content-Type` | Sí | `application/json` |

Payload:

| Campo | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `reservation_id` | UUID | Sí | Identificador de la reserva |
| `status` | string | Sí | Estado nuevo, según la tabla de abajo |
| `status_changed_at` | datetime (ISO 8601) | Sí | Cuándo ocurrió el cambio |
| `payment_token_ref` | string | Solo si `status` = `PENDING` | Token de pago generado por el frontend. M2 lo transporta sin interpretarlo |
| `payment_method_type` | string | No | Tipo de medio de pago (solo en `PENDING`) |
| `payment_metadata` | object | No | Datos no sensibles del pago: `payer_email`, `payment_method_id`, `installments`, `last_four` (solo en `PENDING`) |

Qué estado de M2 se publica como qué `status`:

| Estado en M2 | `status` que recibe M3 |
|---|---|
| Pendiente de Pago | `PENDING` |
| Reservada | `RESERVED` |
| En Navegación | `IN_NAVIGATION` |
| Completada | `COMPLETED` |
| Cancelada · Flexible | `CANCELLED_FLEXIBLE` |
| Cancelada · Moderado | `CANCELLED_MODERATE` |
| Cancelada · Tardío | `CANCELLED_LATE` |
| Cancelada · Por Propietario | `CANCELLED_BY_OWNER` |
| Cancelada · Por Inasistencia | `CANCELLED_LATE` (provisional: mismo tratamiento financiero) |
| Expirada con cobro aprobado tardío | `EXPIRED` (pendiente de M3, ver sección 6) |
| Iniciada, Expirada sin cobro, Pago Fallido | No se publican |

M3 solo reconoce los estados de su lista. Cualquier otro valor lo registra como inconsistencia y no hace nada, por eso M2 no publica nada fuera de esta tabla.

## 5. Ejemplos

Pendiente de pago:

```json
{
  "reservation_id": "e4f81c92-7a20-4215-9c5e-8812c3f1a001",
  "status": "PENDING",
  "status_changed_at": "2026-10-09T12:05:00-05:00",
  "payment_token_ref": "tok_12345abcdef",
  "payment_method_type": "CREDIT_CARD",
  "payment_metadata": {
    "payer_email": "carlos.mendoza@example.com",
    "payment_method_id": "visa",
    "installments": 1,
    "last_four": "4242"
  }
}
```

Reservada:

```json
{
  "reservation_id": "e4f81c92-7a20-4215-9c5e-8812c3f1a001",
  "status": "RESERVED",
  "status_changed_at": "2026-10-09T12:08:45-05:00"
}
```

Cancelada moderadamente:

```json
{
  "reservation_id": "e4f81c92-7a20-4215-9c5e-8812c3f1a001",
  "status": "CANCELLED_MODERATE",
  "status_changed_at": "2026-11-13T10:00:00-05:00"
}
```

Completada:

```json
{
  "reservation_id": "e4f81c92-7a20-4215-9c5e-8812c3f1a001",
  "status": "COMPLETED",
  "status_changed_at": "2026-11-18T18:30:00-05:00"
}
```

## 6. Reglas de procesamiento

1. **Cuándo se publica**: cada vez que CU-08 cambia el estado de una reserva y ese estado está en la tabla de la sección 4. El evento se guarda en la misma transacción que el cambio de estado (outbox) y un proceso aparte lo publica. Los eventos de una misma reserva se publican en el orden en que ocurrieron los cambios.
2. **Primera publicación**: `PENDING`. Cuando la reserva nace en Iniciada no se publica ningún cambio de estado (solo el evento de información de reserva, ver CU-02).
3. **Estados no incluidos**: M3 solo reconoce los estados de su lista; cualquier otro lo registra como inconsistencia y no hace nada. Por eso M2 no publica Iniciada, Expirada sin cobro ni Pago Fallido.
4. **Pendiente de pago**: el `payment_token_ref` es obligatorio. Si CU-03 no lo recibe, responde 400 y la reserva no cambia de estado.
5. **Reintento de pago**: si el arrendatario reintenta con otra tarjeta (CU-03), se publica `PENDING` de nuevo con un `Message-Id` y un token nuevos. El TTL no se reinicia.
6. **Sin dinero**: el mensaje nunca lleva montos, tarifas ni número de tarjeta. M3 deduce el sub-estado y la anticipación a partir del `status`.
7. **Idempotencia**: M2 no publica dos veces la misma transición. Si el mensaje se entrega más de una vez, M3 lo descarta por `Message-Id` y confirma (`ack`) solo después de guardarlo.
8. **Respuesta**: no aplica (unidireccional). Para M2 el mensaje se considera entregado cuando el broker confirma la recepción (publisher confirm); recién entonces se marca como publicado en el outbox.
9. **Si algo falla**:
   - Broker caído o conexión rechazada: el evento queda en el outbox y se reintenta hasta 5 veces, con espera de 1, 5, 25 y 125 segundos. Si sigue fallando, se genera una alarma crítica. El cambio de estado ya quedó guardado.
   - M3 rechaza el mensaje por esquema inválido: no se reintenta. M3 lo maneja con su propia DLQ y M2 no se entera (no hay respuesta).
   - El outbox no se puede escribir: falla la transacción completa y el estado de la reserva no cambia.

Pendientes con M3:
1. Agregar el estado `EXPIRED`. Si M3 ya cobró y M2 expiró la reserva por carrera de TTL, M3 debe reembolsar el 100 %. Sin este estado no hay forma de pedir ese reembolso.
2. Opcional: agregar `CANCELLED_BY_NO_SHOW`, con el mismo tratamiento que `CANCELLED_LATE`.
3. Confirmar que M3 ignora campos que no están en su contrato.

## 7. Trazabilidad

| Requisito | Descripción | Dónde se cumple |
|---|---|---|
| FR-001 | Notificar a M3 desde Pendiente de Pago | Secciones 2 y 4 |
| FR-002 | Campos del evento | Sección 4 |
| FR-005 | Entrega garantizada con reintentos | Sección 6 |
| FR-006 / SC-003 | Sin dinero en el mensaje | Sección 1 y payload |
| FR-011 de CU-08 | Identificador único del evento | `Message-Id` |
| SC-001 | Ningún cambio de estado se pierde | Outbox (sección 6) |
