# Módulo 3 — Cálculo total de la reserva (consumido por Módulo 2)

| Campo | Valor |
|---|---|
| Caso de uso de Módulo 2 | CU-12 Brindar cálculo total de la reserva (invocado por CU-03 Iniciar pago, `<<include>>`) |
| Contraparte en Módulo 3 | `rest/UC04-valor-calculado-reserva.md` |
| Dirección | Sistema de Reservas y Operaciones (Módulo 2) → sistema (Módulo 3) |
| ¿Responde? | Sí (síncrono) |
| Efectos secundarios | En Módulo 3: actualiza la `InformaciónDeReserva` con los montos calculados (por eso es `POST`) |
| Fecha | 2026-10-09 |

Leyenda y convenciones comunes: [`../README.md`](../README.md).

## 1. Propósito

Módulo 3 calcula alquiler, seguro y depósito de garantía de una reserva ya registrada mediante el evento `reservation.info.provided`, y devuelve el desglose y el total vinculante. Módulo 2 nunca calcula, suma ni altera montos [CU-12 FR-002, FR-005, FR-006].

## 2. Petición

`POST /api/v1/reservations/{reservation_id}/calculated-value` [CONV, definido por Módulo 3]

**Headers**

| Header | Obligatorio | Valor |
|---|---|---|
| `Authorization` | Sí | `Bearer <token>` — JWT de servicio de Módulo 2 [PEND] OQ-01 |
| `Accept` | No | `application/json` |
| `X-Correlation-Id` | No | El de la petición de origen |

**Path Parameters**

| Parámetro | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `reservation_id` | UUID v4 | Sí | Identificador de la reserva (el mismo de Módulo 2) |

**Query Parameters**: No tiene.

**Body**: No tiene. La reserva se identifica solo por la ruta; Módulo 3 calcula con la información que ya tiene registrada.

## 3. Reglas de procesamiento (lado Módulo 2)

1. **Precondición**: Módulo 3 solo puede calcular si ya recibió el evento `reservation.info.provided`. Módulo 2 debe haberlo publicado antes de llamar. Qué CU lo publica y cuándo es [PEND].
2. Los decimales llegan como cadena. Módulo 2 los parsea como `BigDecimal` y los transporta sin redondeo [CU-12 FR-002].
3. El total que entrega Módulo 3 es la suma de alquiler, seguro y depósito de garantía. Módulo 2 no lo recompone.
4. El depósito de garantía es el 10 % de la tarifa base diaria. No se multiplica por días ni por pasajeros.
5. Módulo 3 no envía `currency`: Módulo 2 usa COP como constante de la plataforma [PEND] (Pendiente 2).
6. Módulo 3 no envía `calculation_id` ni `expires_at`. El vencimiento es el TTL de 15 minutos de la reserva en Módulo 2 [CU-12 Edge Cases, TTL del snapshot].
7. Idempotente: repetir la solicitud devuelve el mismo desglose mientras la información de la reserva no cambie. Módulo 2 canaliza una sola petición activa por reserva [CU-12 Edge Cases].
8. Si la llamada falla, la reserva permanece en `Iniciada`, el TTL sigue corriendo y no se bloquea la embarcación en Módulo 1 [CU-12 FR-007, FR-008, SC-004].
9. Cada solicitud y su respuesta se registran para auditoría [CU-12 FR-010].

## 4. Respuesta esperada

`200 OK` — `application/json`

| Campo | Tipo | Descripción |
|---|---|---|
| `reservation_id` | UUID | Reserva calculada |
| `rental_amount` | string decimal | Monto de alquiler bruto |
| `insurance_amount` | string decimal | Seguro náutico |
| `guarantee_deposit_amount` | string decimal | Depósito de garantía |
| `total_amount` | string decimal | Valor total |

```json
{
  "reservation_id": "e4f81c92-7a20-4215-9c5e-8812c3f1a001",
  "rental_amount": "9600000.00",
  "insurance_amount": "120000.00",
  "guarantee_deposit_amount": "320000.00",
  "total_amount": "10040000.00"
}
```

## 5. Cómo trata Módulo 2 cada respuesta de Módulo 3

Los errores de Módulo 3 llegan como `application/problem+json` con su `code`.

| HTTP de Módulo 3 | `code` | Reacción de Módulo 2 | Mapeo en Módulo 2 |
|---|---|---|---|
| `200` | n/a | Entrega el desglose a CU-03, que lo asocia a la reserva al pasar a Pendiente de Pago | n/a |
| `400` | `VALIDATION_ERROR` | Alerta de integración. No reintenta | `500 ERROR_INTERNO` [PEND] |
| `401` / `403` | `UNAUTHENTICATED` / `FORBIDDEN` | Alerta crítica | `500 ERROR_INTERNO` [PEND] |
| `404` | `RESERVATION_INFO_NOT_FOUND` | Hasta 3 reintentos de 300 ms (el mensaje de información llega de forma asíncrona). Si persiste, la reserva sigue en `Iniciada` | `503 CALCULO_NO_DISPONIBLE` [PEND] |
| `422` | `RESERVATION_INFO_INCOMPLETE` | No reintenta | `503 CALCULO_NO_DISPONIBLE` [PEND] |
| `503` | `FINANCIAL_PARAMETERS_NOT_CONFIGURED` | Fail-safe. La reserva sigue en `Iniciada` | `503 CALCULO_NO_DISPONIBLE` [PEND] |
| `500`, timeout, sin conexión | `INTERNAL_ERROR` | Fail-safe. Un reintento rápido si el tiempo lo permite | `503 CALCULO_NO_DISPONIBLE` [PEND] |

## 6. Resiliencia

| Parámetro | Valor |
|---|---|
| Connect timeout | 100 ms |
| Read timeout | 1000 ms [PEND] (Pendiente 1) |
| Reintentos ante fallo de socket | Máximo 1 reintento rápido |
| Reintentos ante `4xx` | Cero, salvo el `404` de la fila anterior |
| Ante cualquier falla | La reserva permanece en `Iniciada` con su TTL en curso y no se bloquea inventario en Módulo 1 |

## 7. Mapeo hacia el contrato REST de CU-03 / CU-12 de Módulo 2

Ese contrato REST aún no está escrito, por eso los nombres de la derecha son [PEND].

| Módulo 3 | Módulo 2 (`calculo_total`) |
|---|---|
| `rental_amount` | `desglose.alquiler_base` |
| `insurance_amount` | `desglose.seguro_nautico` |
| `guarantee_deposit_amount` | `desglose.deposito_garantia` |
| `total_amount` | `monto_total` |
| (constante de Módulo 2) | `moneda = COP` |

## 8. Seguridad

Módulo 2 llama con la identidad de servicio "Sistema de Reservas y Operaciones". Mecanismo [PEND] OQ-01.

## 9. Pendientes

1. **SLA y timeout**: ratificar con Módulo 3 SLA de 800 ms y timeout de 1000 ms. CU-12 SC-005 sigue con placeholder.
2. **Moneda y `calculation_id`**: CU-12 FR-005 exige moneda e identificador de liquidación, y CU-11 prohíbe asumir una divisa por defecto. Módulo 3 no los envía, y Módulo 2 usaría COP fijo.
3. **Incrementos de fin de semana y temporada**: confirmar si el UC04 de Módulo 3 los incluye, porque las estimaciones sí los aplican y hoy el total final podría salir menor que la estimación.
4. **Evento `reservation.info.provided`**: ningún CU de Módulo 2 lo publica con claridad. Ver `events/reservation-info-provided.md`.
5. **Nombres de campos**: CU-12 usa `base_rental_amount`, `insurance_total_amount` y `security_deposit_amount`. Módulo 3 usa `rental_amount`, `insurance_amount` y `guarantee_deposit_amount`.
6. **Códigos de error de Módulo 2**: `ERROR_INTERNO` y `CALCULO_NO_DISPONIBLE` no siguen el formato `type/title/status/detail/code/retryable` del README.
7. **Semántica del total (H13)**: Módulo 3 define `total_amount` con el depósito incluido. Confirmar que la vista y el snapshot de Módulo 2 adoptan esa definición.

## 10. Trazabilidad

CU-12 FR-001 a FR-010 · US1, US2, US3 · SC-001 a SC-005 · CU-03 FR-003, FR-004 · Módulo 3 UC04.