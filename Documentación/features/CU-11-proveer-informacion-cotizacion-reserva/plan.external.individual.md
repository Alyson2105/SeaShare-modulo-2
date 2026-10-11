# Contrato de Integración Externa: Estimación Individual (M3)

**Módulo proveedor**: Módulo 3 – Finanzas y Pasarela de Pagos  
**Responsable de implementarlo**: Equipo de Módulo 3  
**Módulo consumidor**: Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones  
**Casos de uso consumidores**:
- **Invocador principal**: `CU-11 Proveer información cotización de reserva` (modo individual, `features/CU-11-proveer-informacion-cotizacion-reserva/spec.md`).
- **Consumidor directo en UI**: `CU-19 Ver detalle de embarcación`.
- **Receptor para persistencia**: `CU-02 Iniciar reserva`.

**Fecha**: 2026-10-09

---

## 1. Resumen y propósito de la integración

Este contrato formaliza el servicio síncrono que Módulo 3 ofrece a Módulo 2 para obtener la estimación tarifaria de un viaje náutico con embarcación, fechas y cantidad de pasajeros específicos.

Módulo 3 aplica su fórmula interna:

\[
\text{Costo Total} = (\text{tarifa base} \times \text{duración}) + (\text{tarifa de seguro} \times \text{pasajeros})
\]

y devuelve el monto consolidado junto con una advertencia legal que debe conservarse y mostrarse literalmente.

### Principios de diseño e invariantes

- **Regla estricta «sin dinero»**: Módulo 2 no calcula, suma ni altera montos. Consume `estimated_total` como `BigDecimal`, sin realizar redondeos aritméticos.
- **Operación de solo lectura**: la estimación no reserva inventario en el calendario náutico ni efectúa cargos en la pasarela de pagos. La exclusividad del inventario se adquiere en `CU-03 Iniciar pago`, mediante bloqueo transaccional.
- **Inmutabilidad de la advertencia**: Módulo 2 debe preservar y exponer al arrendatario el siguiente texto exactamente como lo recibe:

  `Valor estimado. El valor incluye el seguro náutico, pero no incluye el depósito de garantía ni penalidades o ajustes derivados de cambios posteriores de la reserva`

---

## 2. Definición del endpoint

### Método HTTP y URL

```http
POST /api/v1/finance/estimates/individual
```

### Elementos de la petición (request)

**Headers**

| Nombre | Obligatorio | Descripción |
|---|---|---|
| `Authorization` | Sí | `Bearer <token_servicio_m2>` — token de identidad de servicio de Módulo 2 |
| `Content-Type` | Sí | `application/json` |
| `Accept` | No | `application/json` |

**Path parameters**: no tiene.

**Query parameters**: no tiene.

**Body (JSON)**

```json
{
  "boat_id": "string (UUID)",
  "start_date": "string (ISO 8601, YYYY-MM-DD)",
  "end_date": "string (ISO 8601, YYYY-MM-DD)",
  "passengers": "number (entero positivo)"
}
```

Módulo 3 no recibe `renterId` ni datos de usuario en esta operación de cálculo tarifario.

### Elementos de la respuesta esperada (response)

**Código HTTP de éxito**: `200 OK`

**Contrato de respuesta**

```json
{
  "boat_id": "string (UUID)",
  "start_date": "string (ISO 8601, YYYY-MM-DD)",
  "end_date": "string (ISO 8601, YYYY-MM-DD)",
  "duration_days": "number (entero, días inclusivos)",
  "passengers": "number (entero positivo)",
  "estimated_total": "string (decimal, ej. '9720000.00')",
  "warning": "string (texto de advertencia literal)"
}
```

**Tipo de dato monetario**: `estimated_total` se representa como una cadena decimal. Módulo 2 lo parsea como `BigDecimal` sin realizar redondeo aritmético.

### Ejemplo de petición y respuesta exitosa

**Petición `curl`**

```bash
curl -X POST "https://finanzas.seashare.internal/api/v1/finance/estimates/individual" \
  -H "Authorization: Bearer <token_servicio_m2>" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "boat_id": "d3b07384-d113-49cd-a5d6-812e9bcfc101",
    "start_date": "2026-11-15",
    "end_date": "2026-11-17",
    "passengers": 8
  }'
```

**Respuesta (`200 OK`)**

```json
{
  "boat_id": "d3b07384-d113-49cd-a5d6-812e9bcfc101",
  "start_date": "2026-11-15",
  "end_date": "2026-11-17",
  "duration_days": 3,
  "passengers": 8,
  "estimated_total": "9720000.00",
  "warning": "Valor estimado. El valor incluye el seguro náutico, pero no incluye el depósito de garantía ni penalidades o ajustes derivados de cambios posteriores de la reserva"
}
```

---

## 3. Matriz de errores y reacción de Módulo 2

Los errores de Módulo 3 se devuelven bajo el estándar `application/problem+json` e incluyen un campo `code`. Módulo 2 los mapea de la siguiente manera:

| HTTP M3 | `code` en M3 | Reacción de M2 | Mapeo en respuesta REST de M2 |
|---|---|---|---|
| `400` | `INVALID_DATE_RANGE` | Aborta sin crear reserva | `400 INVALID_PARAMETERS` |
| `400` | `VALIDATION_ERROR` | Aborta y genera alerta de integración | `400 INVALID_PARAMETERS` |
| `401 / 403` | `UNAUTHENTICATED` / `FORBIDDEN` | Alerta crítica de infraestructura | `500 INTERNAL_ERROR` |
| `422` | `BASE_RATE_NOT_AVAILABLE` | Bloquea el avance | `503 QUOTE_UNAVAILABLE` |
| `503` | `FLEET_UNAVAILABLE` | Activa *fail-safe* | `503 QUOTE_UNAVAILABLE` |
| `503` | `FINANCIAL_PARAMETERS_NOT_CONFIGURED` | Activa *fail-safe* | `503 QUOTE_UNAVAILABLE` |
| `500` / timeout | `INTERNAL_ERROR` | Activa *fail-safe* | `503 QUOTE_UNAVAILABLE` |

Módulo 2 no debe crear una reserva con un precio nulo, cero, aproximado o no confirmado por Módulo 3.

---

## 4. Parámetros de resiliencia, timeouts y reintentos

- **SLA de respuesta**: `800 ms` — propuesta pendiente de ratificación formal por Módulo 3.
- **Read timeout**: `1000 ms` — propuesta pendiente de ratificación.
- **Connect timeout**: `100 ms`.
- **Política de reintentos**:
  - No se realizan reintentos automáticos ante errores `4xx`.
  - En modo individual, no se reintenta automáticamente si el timeout compromete la experiencia interactiva.
  - Se permite un (1) reintento rápido opcional ante una caída transitoria de socket, si el presupuesto de tiempo lo permite.
- **Invalidez de estimaciones obsoletas**: si el arrendatario cambia las fechas o la cantidad de pasajeros antes de formalizar la reserva, la estimación anterior se descarta y se realiza una nueva llamada al endpoint.

---

## 5. Notas transversales de integración

- **Identificador y vigencia de la cotización**: Módulo 3 no devuelve `quote_id` ni fechas de expiración. Módulo 2 genera su propia referencia interna (`quote_id`) y define operativamente la vigencia, según CU-19.
- **Moneda**: Módulo 3 no devuelve un campo `currency`; Módulo 2 asume la constante de plataforma `COP`.
- **Advertencia legal**: el campo `warning` debe transportarse y mostrarse sin cambios, respetando exactamente el texto definido en la sección 1.
- **Solo estados de lectura**: la estimación no bloquea inventario ni produce cargos.

---

## 6. Trazabilidad FR/SC → elemento del contrato

| Requisito / criterio | Descripción | Elemento de este contrato |
|---|---|---|
| **FR-001** | Modo individual para obtener una estimación previa a la reserva | Endpoint `POST /api/v1/finance/estimates/individual` |
| **FR-002** | Módulo 2 no realiza cálculos monetarios | Uso literal de `estimated_total`, parseado como `BigDecimal` |
| **FR-009** | Invocación desde `CU-19 Ver detalle de embarcación` | Parámetros `boat_id`, fechas y pasajeros |
| **FR-010** | Validación de fechas y parámetros | Matriz de errores de la sección 3 |
| **FR-011** | Fechas inválidas impiden crear la reserva | Mapeo de `INVALID_DATE_RANGE` a `400 INVALID_PARAMETERS` |
| **FR-012** | Recepción del monto y de la advertencia obligatoria literal | Campos `estimated_total` y `warning` |
| **FR-013** | Transferencia del valor a `CU-02 Iniciar reserva` | Uso del monto recibido en el flujo de creación de reserva |
| **FR-014** | Recotización al cambiar fechas o pasajeros | Descarte de estimaciones obsoletas, sección 4 |
| **FR-015** | Bloqueo si no existe tarifa base disponible | Mapeo de `BASE_RATE_NOT_AVAILABLE` a `503 QUOTE_UNAVAILABLE` |
| **FR-016** | Una falla o timeout no debe producir una reserva con precio inválido | Política *fail-safe*, secciones 3 y 4 |
| **FR-017** | Operación de solo lectura, sin compromisos contables | Principios de diseño, sección 1 |
| **SC-001** | El monto procede directamente de Módulo 3 | Uso literal de `estimated_total` |
| **SC-002** | Advertencia exacta, sin modificaciones | Conservación literal de `warning` |
| **SC-005** | Fechas inválidas impiden crear una reserva | Manejo de error `400` |
| **SC-006** | No crear reservas con montos nulos o negativos | Bloqueo preventivo ante errores o montos inválidos |
| **SC-007** | Cambios de itinerario invalidan la estimación previa | Regla de invalidez de la sección 4 |
| **SC-008** | Latencia objetivo para estimaciones individuales | Propuesta de SLA `800 ms` y read timeout `1000 ms`, pendiente de ratificación |
