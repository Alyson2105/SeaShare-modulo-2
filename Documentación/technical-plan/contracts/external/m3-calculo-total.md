# Contrato de Integración Externa: Cálculo Total de la Reserva (M3)

Módulo Proveedor: Módulo 3 (UC04 Solicitar el valor calculado de la reserva)
Módulo Consumidor: Módulo 2
CUs de M2 que lo consumen: CU-12 Brindar cálculo total de la reserva; dependiente: CU-03 Iniciar pago

## 1. Propósito
M3 calcula alquiler, seguro y depósito de una reserva ya registrada vía `reservation.info.provided` y devuelve el desglose y el total vinculante. M2 jamás calcula, suma ni altera montos.

## 2. Endpoint
POST /api/v1/reservations/{reservation_id}/calculated-value
Sin body (la reserva se identifica solo por la ruta).

Headers: Authorization (JWT de servicio de M2 [OQ-01]), Accept: application/json, X-Correlation-Id (opcional).
Path: reservation_id (UUID v4).

## 3. Respuesta 200 OK
{
  "reservation_id": "uuid",
  "rental_amount": "string decimal",
  "insurance_amount": "string decimal",
  "guarantee_deposit_amount": "string decimal",
  "total_amount": "string decimal"
}

Ejemplo:
{
  "reservation_id": "e4f81c92-7a20-4215-9c5e-8812c3f1a001",
  "rental_amount": "9600000.00",
  "insurance_amount": "120000.00",
  "guarantee_deposit_amount": "320000.00",
  "total_amount": "10040000.00"
}

Notas:
- Los decimales llegan como string; M2 los parsea como BigDecimal y los transporta sin redondeo.
- M3 no envía `currency` (COP es la constante de la plataforma en M2), ni `calculation_id`, ni `expires_at`: el vencimiento es el TTL de 15 minutos de la reserva en M2.
- Depósito = 10 % de la tarifa base diaria (no se multiplica por días ni pasajeros).
- Idempotente: repetir la solicitud devuelve el mismo desglose mientras la información de la reserva no cambie.

## 4. Mapeo hacia el contrato REST de CU-03 / CU-12 de M2
| M3 | M2 (calculo_total) |
|---|---|
| rental_amount | desglose.alquiler_base |
| insurance_amount | desglose.seguro_nautico |
| guarantee_deposit_amount | desglose.deposito_garantia |
| total_amount | monto_total |
| (constante de M2) | moneda = COP |

## 5. Errores (application/problem+json con `code`)
| HTTP M3 | code | Reacción de M2 | Mapeo M2 |
|---|---|---|---|
| 400 | VALIDATION_ERROR | Alerta de integración; no reintenta | 500 ERROR_INTERNO |
| 401 / 403 | UNAUTHENTICATED / FORBIDDEN | Alerta crítica | 500 ERROR_INTERNO |
| 404 | RESERVATION_INFO_NOT_FOUND | Hasta 3 reintentos de 300 ms (el mensaje de info llega de forma asíncrona); si persiste, reserva sigue en Iniciada | 503 CALCULO_NO_DISPONIBLE |
| 422 | RESERVATION_INFO_INCOMPLETE | No reintenta | 503 CALCULO_NO_DISPONIBLE |
| 503 | FINANCIAL_PARAMETERS_NOT_CONFIGURED | Fail-safe, reserva sigue en Iniciada | 503 CALCULO_NO_DISPONIBLE |
| 500 / 503 / timeout | INTERNAL_ERROR | Fail-safe; un reintento rápido si el tiempo lo permite | 503 CALCULO_NO_DISPONIBLE |

## 6. Resiliencia
Connect 100 ms · Read timeout 1000 ms · máximo 1 reintento rápido ante fallo de socket · cero reintentos ante 4xx (excepto el 404 de la fila anterior). Ante cualquier falla la reserva permanece en Iniciada con su TTL en curso y no se bloquea inventario en M1.

## 7. [PEDIR A M3]
- Ratificar SLA 800 ms / timeout 1000 ms.
- Aclarar si UC04 incluye los incrementos de fin de semana y temporada que sí aplican las estimaciones (hoy el total final podría salir menor que la estimación).