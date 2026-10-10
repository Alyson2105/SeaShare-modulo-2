# Módulo 3 — Confirmación de pago (consumido por Módulo 2)

| Campo | Valor |
|---|---|
| Caso de uso de Módulo 2 | CU-13 Confirmar pago, ejecutado como proceso interno programado (job). Relacionados: CU-03 Iniciar pago (publica el evento `PENDIENTE` con el token) y CU-08 Actualizar estado de reserva (aplica la transición resultante) |
| Contraparte en Módulo 3 | `rest/UC06-confirmacion-pago.md` |
| Dirección | Sistema de Reservas y Operaciones (Módulo 2) → sistema (Módulo 3) |
| ¿Responde? | Sí (síncrono) |
| Efectos secundarios | Ninguno (solo lectura). No crea ni modifica registros en Módulo 3 ni contacta a la pasarela |
| Fecha | 2026-10-09 |

Leyenda y convenciones comunes: [README](../README.md).

## 1. Propósito

Módulo 3 ejecuta el cobro con la pasarela (Mercado Pago) mediante un worker asíncrono, a partir del evento `reservation.status.changed` con `status = PENDIENTE`. Módulo 3 no invoca a Módulo 2, así que Módulo 2 conoce el resultado del cobro consultando este endpoint [CU-13 FR-001].

**Invariantes:**

- Módulo 2 no interpreta, calcula ni altera los montos que devuelve este endpoint. Los transporta solo para auditoría [CU-13 FR-008, SC-005].
- Solo un estado `APROBADO` verificable dentro del TTL permite avanzar la reserva a Reservada [CU-13 FR-004, SC-002].
- `RECHAZADO`, `CANCELADO` o `EXPIRADO` nunca se tratan como aprobado.
- `APROBADO` no implica que la captura o la liquidación posterior ya se haya ejecutado.

## 2. Petición

```http
GET /api/v1/reservations/{reservation_id}/payment-confirmation
```

[CONV, definido por Módulo 3]

### Headers

| Header | Obligatorio | Valor |
|---|---|---|
| `Authorization` | Sí | `Bearer <token>` — JWT de servicio de Módulo 2 [PEND] OQ-01 |
| `Accept` | No | `application/json` |
| `X-Correlation-Id` | No | Cadena libre para trazabilidad distribuida |

### Path parameters

| Parámetro | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `reservation_id` | UUID v4 | Sí | Identificador de la reserva en Módulo 2 cuyo cobro se consulta |

**Query parameters:** No tiene.

**Body:** No tiene (operación `GET`).

## 3. Reglas de procesamiento (lado Módulo 2)

1. La consulta la hace un job interno cada 5 segundos (configurable), solo mientras la reserva esté en Pendiente de Pago con el TTL vigente [CU-13 FR-003].
2. No hay reintentos dentro de un mismo ciclo. El reintento natural es el siguiente ciclo del job.
3. Ante cualquier error o timeout, Módulo 2 nunca asume que el pago fue aprobado ni rechazado. La reserva permanece en su estado hasta obtener un `status` definitivo o hasta que venza el TTL (fail-safe).
4. Repetir la consulta devuelve el estado vigente en ese instante, sin efectos secundarios.
5. Módulo 2 no contacta a la pasarela [CU-13 FR-008, SC-005].
6. La transición resultante la aplica CU-08. Módulo 2 no cambia el estado directamente.

## 4. Respuesta esperada

**`200 OK` — `application/json`**

| Campo | Tipo | Descripción |
|---|---|---|
| `reservation_id` | UUID | Reserva consultada |
| `status` | string | Estado del cobro (ver §5) |
| `detail` | string o `null` | Detalle del estado cuando esté disponible |
| `authorized_amount` | string decimal o `null` | Monto autorizado |
| `captured_amount` | string decimal o `null` | Monto capturado |
| `released_amount` | string decimal o `null` | Monto liberado |
| `charged_amount` | string decimal o `null` | Monto cobrado |
| `external_reference` | string o `null` | Referencia externa de la pasarela |

### Ejemplo 1: cobro aprobado (autorizado, aún sin captura)

```json
{
  "reservation_id": "e4f81c92-7a20-4215-9c5e-8812c3f1a001",
  "status": "APROBADO",
  "detail": null,
  "authorized_amount": "10040000.00",
  "captured_amount": null,
  "released_amount": null,
  "charged_amount": null,
  "external_reference": "pg-8841"
}
```

### Ejemplo 2: cobro en proceso

```json
{
  "reservation_id": "e4f81c92-7a20-4215-9c5e-8812c3f1a001",
  "status": "EN_PROCESO",
  "detail": null,
  "authorized_amount": null,
  "captured_amount": null,
  "released_amount": null,
  "charged_amount": null,
  "external_reference": null
}
```

### Ejemplo 3: cobro rechazado

```json
{
  "reservation_id": "e4f81c92-7a20-4215-9c5e-8812c3f1a001",
  "status": "RECHAZADO",
  "detail": "cc_rejected_insufficient_amount",
  "authorized_amount": null,
  "captured_amount": null,
  "released_amount": null,
  "charged_amount": null,
  "external_reference": "pg-8842"
}
```

## 5. Cómo trata Módulo 2 cada `status`

| `status` | Significado en Módulo 3 | Reacción de Módulo 2 (CU-13 / CU-08) |
|---|---|---|
| `EN_PROCESO` | La pasarela aún no reportó un resultado definitivo | Sin cambios. Vuelve a consultar en el siguiente ciclo |
| `APROBADO` | La pasarela aprobó la autorización o el cobro | Con TTL vigente: transición a Reservada. Con la reserva ya Expirada o en Pago Fallido: cobro huérfano, Módulo 2 publica el estado `EXPIRADA` a Módulo 3 y genera alerta crítica |
| `RECHAZADO` | La pasarela rechazó la operación | Registra el motivo. Con TTL vigente la reserva sigue en Pendiente de Pago, permitiendo reintento con otro token (CU-03) |
| `CANCELADO` | La operación fue cancelada | Transición a Pago Fallido y liberación de la embarcación en Módulo 1 |
| `EXPIRADO` | La autorización venció sin captura | Transición a Pago Fallido y liberación de la embarcación en Módulo 1 |
| `DESCONOCIDO` | Estado externo no determinado (incluye falla de comunicación con la pasarela) | Se trata como `EN_PROCESO`. Se registra para auditoría |

## 6. Cómo trata Módulo 2 cada error

Los errores de Módulo 3 llegan como `application/problem+json` con `type`, `title`, `status`, `detail`, `code` y `retryable`.

| HTTP de Módulo 3 | `code` | Causa en Módulo 3 | Reacción de Módulo 2 |
|---|---|---|---|
| `400` | `VALIDATION_ERROR` | `reservation_id` con formato inválido | Alerta de integración en observabilidad. No reintenta |
| `401` / `403` | `UNAUTHENTICATED` / `FORBIDDEN` | Credencial de servicio ausente, inválida o sin permisos | Alerta crítica de infraestructura. No cambia el estado de la reserva |
| `404` | `CHARGE_INTENT_NOT_FOUND` | No existe IntenciónDeCobro: Módulo 3 la crea de forma asíncrona al procesar el evento `PENDIENTE` y puede no haberlo procesado aún | Reintenta en el siguiente ciclo (reintentable) |
| `500` / `503` / timeout | `INTERNAL_ERROR` | Falla interna o sobrecarga | Sin cambios de estado. Reintenta en el siguiente ciclo |

## 7. Resiliencia

| Parámetro | Valor |
|---|---|
| Connect timeout | 100 ms |
| Read timeout | 500 ms [PEND] propuesta pendiente de ratificación por Módulo 3 |
| Reintentos dentro del mismo ciclo | Cero |
| Periodo del job | 5 s, configurable |
| Idempotencia | Solo lectura. Repetir la consulta no tiene efectos |
| Fail-safe | Ante error o timeout, la reserva permanece en su estado |

## 8. Seguridad

Módulo 2 llama con la identidad de servicio `Sistema de Reservas y Operaciones`. Mecanismo [PEND] OQ-01.

## 9. Pendientes

1. **Autenticación servicio a servicio:** validar emisor y claim del JWT de servicio.
2. **SLA y timeout:** ratificar con Módulo 3 SLA menor a 300 ms y read timeout de 500 ms.
3. **Estado `EXPIRADA` en el evento de estado:** para reembolsar un cobro aprobado después de que Módulo 2 expiró la reserva, Módulo 3 debe aceptar `EXPIRADA` en `reservation.status.changed` (ver `events/CU-14-estado-reserva.md`).
4. **Clave idempotente de cobro con número de intento:** la clave actual `cobro-<reservation_id>` impide reintentar con otro token tras un `RECHAZADO`. Debe incluir el intento.

## 10. Trazabilidad

CU-13 FR-001, FR-003, FR-004, FR-005, FR-006, FR-008 · SC-001, SC-002, SC-003, SC-005 · Módulo 3 UC06.
