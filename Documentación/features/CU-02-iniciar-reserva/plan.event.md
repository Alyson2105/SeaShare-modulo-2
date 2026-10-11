# Contrato de Evento: Información de Reserva (CU-02 → M3)

| Campo | Valor |
|---|---|
| Módulo productor | Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones |
| Módulo consumidor | Módulo 3 (UC03 Brindar información de reserva) |
| Caso de uso de M2 que lo emite | CU-02 Iniciar reserva |
| Tipo | Asíncrono, unidireccional (AMQP / RabbitMQ). M3 no responde |
| Fecha | 2026-10-09 |

## 1. Propósito

Cuando se crea una reserva en estado Iniciada, M2 le informa a M3 los datos de la reserva para que M3 pueda calcular después el valor total (CU-12). Este mensaje **no es un cambio de estado**: es la información que M3 necesita para cotizar.

## 2. Cuándo se publica

- Una vez, cuando CU-02 guarda la reserva en estado Iniciada.
- Si la reserva se vuelve a registrar, se publica de nuevo. M3 actualiza la información y descarta los montos calculados antes.

## 3. Dónde se publica

| Elemento | Valor |
|---|---|
| Exchange | `seashare.reservations` (topic) |
| Routing key | `reservation.info.provided` |
| Cola de M3 | `finance.reservation-info.v1` |

## 4. Mensaje

Headers AMQP:

| Header | Obligatorio | Valor |
|---|---|---|
| `Message-Id` | Sí | UUID único del mensaje |
| `Content-Type` | Sí | `application/json` |
| `App-Id` | No | `reservations-service` |

Payload:

| Campo | Tipo | Obligatorio | De dónde sale en M2 |
|---|---|---|---|
| `reservation_id` | UUID | Sí | La reserva recién creada |
| `vessel_id` | UUID | Sí | `vessel_id` de la reserva |
| `start_date` | date (YYYY-MM-DD) | Sí | `start_at` |
| `end_date` | date (YYYY-MM-DD) | Sí | `end_at` |
| `passengers` | integer | Sí | `passengers` |
| `owner_id` | UUID | Sí | `owner.owner_id` de M1 (CU-09) |
| `max_capacity` | integer | Sí | `max_capacity` de M1 (CU-09) |

```json
{
  "reservation_id": "e4f81c92-7a20-4215-9c5e-8812c3f1a001",
  "vessel_id": "d3b07384-d113-49cd-a5d6-812e9bcfc101",
  "start_date": "2026-11-15",
  "end_date": "2026-11-17",
  "passengers": 8,
  "owner_id": "a1c2e3f4-5678-90ab-cdef-1234567890ab",
  "max_capacity": 12
}
```

## 5. Reglas de procesamiento

1. **Cuándo se publica**: una vez, al guardar la reserva en estado Iniciada. El mensaje se guarda en la misma transacción que la reserva (outbox) y un proceso aparte lo publica.
2. **Datos de M1**: CU-02 consulta a M1 (CU-09) para obtener `owner_id` y `max_capacity`. Si M1 no responde, no se crea la reserva ni se publica nada (503 `FLEET_SERVICE_UNAVAILABLE`). M3 no consulta a Flota para obtener estos datos.
3. **Validaciones previas**: `end_date >= start_date` y `1 <= passengers <= max_capacity`. Si no se cumplen, CU-02 responde 400 y no se crea la reserva ni se publica nada.
4. **Sin dinero**: el mensaje no lleva montos ni tarifas. M3 busca la tarifa base por su cuenta.
5. **Reemisión**: si la reserva se vuelve a registrar, se publica de nuevo con la información más reciente. M3 la sobrescribe (upsert) y descarta los montos calculados antes.
6. **Orden**: este mensaje se publica antes del primer cambio de estado (`PENDING`). M3 solo puede calcular (CU-12) cuando ya lo procesó; si M3 responde que no encuentra la información, M2 reintenta unos instantes antes de devolver 503 `CALCULATION_UNAVAILABLE`.
7. **Idempotencia**: cada mensaje lleva un `Message-Id` único; si se entrega más de una vez, M3 lo descarta por ese identificador.
8. **Respuesta**: no aplica (unidireccional). Para M2 el mensaje se considera entregado cuando el broker confirma la recepción (publisher confirm); recién entonces se marca como publicado en el outbox.
9. **Si algo falla**:
   - Broker caído o conexión rechazada: el mensaje queda en el outbox y se reintenta hasta 5 veces, con espera de 1, 5, 25 y 125 segundos. Si sigue fallando, se genera una alarma crítica.
   - M3 no puede procesar el mensaje: lo maneja con su propia DLQ y sus reintentos. M2 no se entera (no hay respuesta).

## 6. Trazabilidad

| Requisito | Descripción | Dónde se cumple |
|---|---|---|
| FR-015 de CU-02 | Publicar la información de la reserva a M3 | Este contrato |
| FR-010 de CU-02 | Consultar M1 antes de crear la reserva | Origen de `owner_id` y `max_capacity` |
