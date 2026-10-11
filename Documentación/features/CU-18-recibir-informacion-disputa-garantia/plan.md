# Contrato de Evento: Disputa de Garantía (CU-18 → M3)

| Campo | Valor |
|---|---|
| Módulo productor | Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones |
| Módulo consumidor | Módulo 3 (UC08 Brindar información de disputa de garantía) |
| Casos de uso de M2 que lo emiten | CU-16 (cierre automático) y CU-17 (resolución del Administrador) |
| Tipo | Asíncrono, unidireccional (AMQP / RabbitMQ). M3 no responde |
| Fecha | 2026-10-09 |

## 1. Propósito

Cuando una disputa sobre el depósito de garantía termina, M2 le avisa a M3 el resultado final. M3 decide el movimiento de dinero con sus propios registros: devuelve el 100 % del depósito al arrendatario o se lo liquida al propietario. No existe retención parcial. El mensaje **no lleva montos**.

## 2. Cuándo se publica

Solo cuando la disputa llega a un estado final:
- **Cierre automático (CU-16):** pasaron las 24 horas sin reclamo del propietario. La disputa se cierra como RECHAZADA.
- **Resolución del Administrador (CU-17):** decide ACEPTADA o RECHAZADA.

Mientras la disputa está PENDIENTE no se publica nada. M3 acepta ese estado, pero M2 no lo necesita enviar.

## 3. Dónde se publica

| Elemento | Valor |
|---|---|
| Exchange | `seashare.reservations` (topic) |
| Routing key | `reservation.dispute.updated` |
| Cola de M3 | `finance.guarantee-dispute.v1` |

## 4. Mensaje

Headers AMQP:

| Header | Obligatorio | Valor |
|---|---|---|
| `Message-Id` | Sí | UUID único del mensaje |
| `Content-Type` | Sí | `application/json` |

Payload:

| Campo | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `reservation_id` | UUID | Sí | Identificador de la reserva |
| `dispute_id` | UUID | Sí | Identificador de la disputa |
| `status` | string | Sí | `REJECTED` o `COMPLETED` |
| `status_changed_at` | datetime (ISO 8601) | Sí | Cuándo se resolvió |
| `event_key` | string | Sí | Clave única del evento para evitar duplicados |

Qué estado de la disputa en M2 se publica como qué `status`:

| Estado en M2 | `status` que recibe M3 | Qué hace M3 |
|---|---|---|
| RECHAZADA | `REJECTED` | Reembolsa el 100 % del depósito al arrendatario |
| ACEPTADA | `COMPLETED` | Liquida el 100 % del depósito al propietario |
| PENDIENTE | No se publica | — |

La API REST de M2 sigue usando ACEPTADA / RECHAZADA. La traducción a `COMPLETED` / `REJECTED` se hace solo al armar el mensaje para M3. El motivo que escribe el Administrador se guarda en M2 y no viaja a M3.

## 5. Ejemplos

Cierre automático por vencimiento (RECHAZADA → `REJECTED`):

```json
{
  "reservation_id": "c1f7a8b2-5e4d-4c3b-8a1e-9f0a2b3c4d5e",
  "dispute_id": "d1a2b3c4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
  "status": "REJECTED",
  "status_changed_at": "2026-10-10T10:00:00-05:00",
  "event_key": "disputa-d1a2b3c4-REJECTED"
}
```

El Administrador acepta el reclamo (ACEPTADA → `COMPLETED`):

```json
{
  "reservation_id": "c1f7a8b2-5e4d-4c3b-8a1e-9f0a2b3c4d5e",
  "dispute_id": "d1a2b3c4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
  "status": "COMPLETED",
  "status_changed_at": "2026-10-09T16:45:00-05:00",
  "event_key": "disputa-d1a2b3c4-COMPLETED"
}
```

## 6. Reglas de procesamiento

1. **Solo estados finales**: se publica únicamente cuando la disputa llega a ACEPTADA o RECHAZADA (ver sección 2). Mientras está PENDIENTE no se publica nada.
2. **Traducción de estados**: M2 usa ACEPTADA / RECHAZADA en su API REST y traduce a `COMPLETED` / `REJECTED` solo al armar el mensaje (tabla de la sección 4).
3. **Un solo evento final por disputa**: los estados finales son inmutables. Si el Administrador y el cierre automático coinciden, el control de concurrencia asegura que solo una transacción gane y emita el evento.
4. **Motivo**: el motivo del Administrador se guarda en M2 y no viaja a M3.
5. **Clave del evento**: `event_key` sigue el formato `disputa-<id>-<estado>`; es la misma en cualquier reenvío del mismo resultado. M3 descarta duplicados por `(reservation_id, dispute_id, event_key)`.
6. **Sin dinero**: el mensaje no lleva montos, porcentajes ni instrucciones de pago. M3 usa el depósito que tiene registrado.
7. **Ventana de 24 horas**: M3 no tiene temporizadores sobre esa ventana; la gestión es de M2, que envía el `REJECTED` cuando vence sin reclamo.
8. **Cuándo se guarda el evento**: en la misma transacción que el cambio de estado de la disputa (outbox). Un proceso aparte lo publica.
9. **Respuesta**: no aplica (unidireccional). Para M2 el mensaje se considera entregado cuando el broker confirma la recepción (publisher confirm); recién entonces se marca como publicado en el outbox.
10. **Si algo falla**:
    - Broker caído o conexión rechazada: el evento queda en el outbox y se reintenta hasta 5 veces, con espera de 1, 5, 25 y 125 segundos. Si sigue fallando, se genera una alarma crítica. El estado de la disputa ya quedó guardado.
    - M3 no encuentra depósito registrado: M3 anota un fallo controlado sin ejecutar acciones. M2 no se entera (no hay respuesta).
    - El outbox no se puede escribir: falla la transacción completa y el estado de la disputa no cambia.

## 7. Trazabilidad

| Requisito | Descripción | Dónde se cumple |
|---|---|---|
| FR-001 | Publicar solo estados finales | Sección 2 |
| FR-003 / SC-002 | Sin montos ni instrucciones de pago | Sección 4 |
| FR-004 / SC-003 | Identificador único para evitar duplicados | `event_key` y `Message-Id` |
| FR-005 / SC-004 | Ningún evento se pierde | Outbox (sección 6) |
