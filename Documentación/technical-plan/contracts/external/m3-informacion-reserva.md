# Contrato de Evento Asíncrono: Información de Reserva (M3 UC03)

**Módulo Productor:** Módulo 2  
**Módulo Consumidor:** Módulo 3  
**Caso de uso de M3:** UC03 Brindar información de reserva  
**CU de M2 que lo emite:** CU-02 Iniciar reserva (tras persistir la reserva en `Iniciada`)

---

## 1. Topología

| Elemento | Valor |
|---|---|
| Exchange | `seashare.reservations` (topic, durable) |
| Routing key | `reservation.info.provided` |
| Cola consumidora (M3) | `finance.reservation-info.v1` |
| DLX / DLQ | `seashare.reservations.dlx` / `finance.reservation-info.dlq` [PEDIR A M3: confirmar] |

**Propiedades AMQP:** `Message-Id` (UUID, obligatorio), `Content-Type: application/json`, `delivery_mode: 2`, `App-Id: reservations-service`.

---

## 2. Payload

```json
{
  "reservation_id": "uuid",
  "boat_id": "uuid",
  "start_date": "YYYY-MM-DD",
  "end_date": "YYYY-MM-DD",
  "passengers": 8,
  "owner_id": "uuid",
  "max_capacity": 12
}
```

`owner_id` y `max_capacity` provienen de M1 (CU-09 Proveer información de embarcación, `propietario.propietario_id` y `capacidad_maxima`). M3 no los consulta a Flota.

**Ejemplo:**

```json
{
  "reservation_id": "e4f81c92-7a20-4215-9c5e-8812c3f1a001",
  "boat_id": "d3b07384-d113-49cd-a5d6-812e9bcfc101",
  "start_date": "2026-11-15",
  "end_date": "2026-11-17",
  "passengers": 8,
  "owner_id": "a1c2e3f4-5678-90ab-cdef-1234567890ab",
  "max_capacity": 12
}
```

---

## 3. Reglas de emisión

- Se publica por outbox en la misma transacción que crea la reserva en `Iniciada` (garantía at-least-once, mismos reintentos 1/5/25/125 s y DLQ).
- Validaciones previas en M2: `end_date >= start_date` y `1 <= passengers <= max_capacity`. M3 ignora (hace ack) los mensajes que no cumplan.
- M3 hace upsert por `reservation_id`: reemitir actualiza la información e invalida montos calculados previos.
- No contiene dinero (M3 consulta la tarifa base a Flota por su cuenta).
- No es un cambio de estado: no contradice la regla "`Iniciada` no publica `reservation.status.changed`".

---

## 4. Consecuencia en CU-12

CU-12 solo puede obtener el cálculo (UC04) si este mensaje ya fue procesado; si M3 responde `404 RESERVATION_INFO_NOT_FOUND`, M2 reintenta brevemente (ver sección 7).
