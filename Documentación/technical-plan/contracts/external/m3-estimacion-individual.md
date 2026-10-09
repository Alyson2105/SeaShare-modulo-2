# Contrato de Integración Externa: Estimación Individual (M3)

**Módulo Proveedor**: Módulo 3 – Liquidación, Seguros y Dispersión de Fondos ("el sistema")  
**Responsable de implementarlo**: Equipo de Módulo 3  
**Módulo Consumidor**: Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones ("Sistema de Reservas y Operaciones")  
**CUs de Módulo 2 que lo consumen**: 
- Invocador principal: `CU-11 Proveer información cotización de reserva` (Modo Individual, `features/CU-11-proveer-informacion-cotizacion-reserva/spec.md`).
- Consumidor directo en UI: `CU-19 Ver detalle de embarcación` (Endpoint 2: cotización individual para el detalle, FR-005).
- Receptor para persistencia: `CU-02 Iniciar reserva` (adopta el valor tarifario oficial para crear la reserva en estado `Iniciada`, FR-012, FR-013).
**Fecha**: 2026-10-09  

---

## 1. Resumen y Propósito de la Integración

Este contrato formaliza el servicio síncrono de cálculo tarifario oficial que Módulo 3 ofrece a Módulo 2 para un viaje náutico determinado. Módulo 3 aplica su fórmula interna:
$$\text{Costo Total} = (\text{tarifa base} \times \text{duración}) + (\text{tarifa de seguro} \times \text{pasajeros})$$
y emite el monto consolidado junto con la **bandera de advertencia legal inmutable** (FR-012).

**Principios de Diseño e Invariantes**:
- **Regla Estricta "Sin Dinero"**: Módulo 2 jamás calcula, suma ni altera montos. Recibe `estimated_total` como `BigDecimal` literal (FR-002 de CU-11, SC-001).
- **Operación de Solo Lectura**: la cotización individual no reserva inventario en el calendario náutico ni efectúa cargos en pasarela; la exclusividad de inventario se adquiere en `Iniciar pago` (CU-03) mediante bloqueo transaccional (FR-017, SC-002 de CU-03).
- **Inmutabilidad de Advertencia**: Módulo 2 debe preservar y exponer al Arrendatario la advertencia legal textualmente: `"Valor estimado. No incluye cargos adicionales ni depósito de seguridad"` (FR-012, SC-002).

---

## 2. Definición del Endpoint

### Método HTTP y URL

```http
POST /api/v1/estimates/individual
```

### Elementos de la Petición (Request)

**Headers**:

| Nombre | Obligatorio | Descripción |
|---|---|---|
| `Authorization` | Sí | `Bearer <token>` — JWT de identidad de servicio expedido para Módulo 2 |
| `Content-Type` | Sí | `application/json` |
| `Accept` | No | `application/json` |

**Path Parameters**: No tiene.

**Query Parameters**: No tiene.

**Body (JSON)** *(convención snake_case requerida por Módulo 3)*:

```json
{
  "boat_id": "string (UUID)",
  "start_date": "string (ISO 8601, YYYY-MM-DD)",
  "end_date": "string (ISO 8601, YYYY-MM-DD)",
  "passengers": "number (entero positivo)"
}
```

*(Nota: Módulo 3 no recibe `arrendatarioId` ni datos de usuario en esta operación de cálculo tarifario).*

---

### Elementos de la Respuesta Esperada (Response)

**Código de estado HTTP (éxito)**: `200 OK`

**Contrato de respuesta tipado (plano y en snake_case)**:

```json
{
  "boat_id": "string (UUID)",
  "start_date": "string (ISO 8601, YYYY-MM-DD)",
  "end_date": "string (ISO 8601, YYYY-MM-DD)",
  "duration_days": "number (entero)",
  "passengers": "number (entero)",
  "estimated_total": "number (monto monetario consolidado)",
  "warning": "string (texto obligatorio: 'Valor estimado. No incluye cargos adicionales ni depósito de seguridad')"
}
```

---

### Ejemplo de Petición y Respuesta Exitosa

**Petición `curl`**:

```bash
curl -X POST "https://finanzas.seashare.internal/api/v1/estimates/individual" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.serviceTokenM2" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "boat_id": "d3b07384-d113-49cd-a5d6-812e9bcfc101",
    "start_date": "2026-11-15",
    "end_date": "2026-11-18",
    "passengers": 8
  }'
```

**Respuesta (`200 OK`)**:

```json
{
  "boat_id": "d3b07384-d113-49cd-a5d6-812e9bcfc101",
  "start_date": "2026-11-15",
  "end_date": "2026-11-18",
  "duration_days": 3,
  "passengers": 8,
  "estimated_total": 9600000.00,
  "warning": "Valor estimado. No incluye cargos adicionales ni depósito de seguridad"
}
```

---

## 3. Matriz de Errores y Reacción de Módulo 2

| Código HTTP M3 | Causa en Módulo 3 | Reacción Arquitectónica de Módulo 2 | Mapeo al Endpoint de M2 |
|---|---|---|---|
| `400 Bad Request` | Fechas en el pasado, fin anterior o igual a inicio, duración de cero días o pasajeros inválidos (FR-010) | Módulo 2 captura el detalle del error y aborta el flujo sin crear ninguna reserva en BD (FR-011, SC-005) | `400 Bad Request` (`PARAMETROS_INVALIDOS`) |
| `401 Unauthorized` / `403 Forbidden` | Token de servicio inválido o permisos insuficientes | Alerta crítica de infraestructura; corte preventivo de ejecución | `500 Internal Server Error` (`ERROR_INTERNO`) |
| `404 Not Found` | Embarcación no registrada en tarifas de M3 o sin esquema activo (FR-015) | Bloquea inmediatamente el avance a reserva notificando inconsistencia tarifaria | `503 Service Unavailable` (`COTIZACION_NO_DISPONIBLE`) |
| `422 Unprocessable Entity` | Valor total devuelto menor o igual a cero sin promoción autorizada (FR-015, SC-006) | Rechaza la cotización por anomalía financiera preventiva | `503 Service Unavailable` (`COTIZACION_NO_DISPONIBLE`) |
| `500 Internal Server Error` / `503 Service Unavailable` | Falla del motor de liquidación | Activa *fail-safe* preventivo (FR-016): no genera reservas con precio en cero ni aproximado | `503 Service Unavailable` (`COTIZACION_NO_DISPONIBLE`) |
| `Timeout` (> 1000 ms) | Latencia excedida en Módulo 3 | Corta la conexión inmediatamente y cancela la cotización | `503 Service Unavailable` (`COTIZACION_NO_DISPONIBLE`) |

---

## 4. Parámetros de Resiliencia, Timeouts y Reintentos

- **SLA de Respuesta**: `800 ms` [NEEDS CLARIFICATION: PROPUESTA SLA 800 ms pendiente de ratificación formal por Módulo 3].
- **Read Timeout**: `1000 ms` [NEEDS CLARIFICATION: PROPUESTA Timeout 1000 ms].
- **Connect Timeout**: `100 ms`.
- **Política de Reintentos**:
  - En modo individual no se realizan reintentos automáticos si el fallo es 4xx o si el timeout compromete la experiencia interactiva del usuario (FR-016).
  - Un (1) reintento rápido opcional ante caída transitoria de socket si el presupuesto de tiempo lo permite.
- **Invalidez de Cotizaciones Obsoletas**: si el Arrendatario cambia fechas o pasajeros antes de formalizar la reserva, la cotización previa queda automáticamente descartada en memoria y se dispara una nueva llamada hacia este endpoint (FR-014, SC-007).

---

## 5. Puntos Abiertos y Aclaraciones Necesarias

- `[NEEDS CLARIFICATION: SLA y Timeout de Módulo 3 en Individual]`: Módulo 3 omite los tiempos de respuesta exigidos en sus especificaciones hacia Módulo 2. Se registra formalmente la propuesta técnica de **SLA de 800 ms** y **Timeout de 1000 ms** para revisión y ratificación entre ambos equipos.
- `[NEEDS CLARIFICATION: Identificador de Cotización en Respuesta Plana]`: el contrato plano acordado con Módulo 3 devuelve `boat_id`, fechas, duración, pasajeros, total y advertencia. Para trazabilidad y auditoría de creación de reserva en CU-02, Módulo 2 asocia internamente una referencia temporal de cotización mientras se acuerda si Módulo 3 agregará un `quote_id` formal en revisiones futuras.

---

## 6. Trazabilidad FR/SC → Elemento del Contrato

| Requisito / Criterio | Descripción en Spec CU-11 | Elemento de este Contrato |
|---|---|---|
| **FR-001** | Modo Individual para cotización exacta previa a reserva | Endpoint `POST /api/v1/estimates/individual` |
| **FR-002** | Cero cálculos de dinero en Módulo 2 | Inmutabilidad del campo `estimated_total` |
| **FR-009** | Invocación desde `CU-19 Ver detalle de embarcación` | Parámetros del request: `boat_id`, fechas y pasajeros |
| **FR-010** | Validación temporal de fechas delegada a Módulo 3 | Matriz de errores (Sección 3) ante códigos `400` de M3 |
| **FR-011** | Error de fechas de M3 traslada sin persistir reserva | Comportamiento fail-safe documentado en la Sección 3 |
| **FR-012** | Recepción de monto total y advertencia obligatoria literal | Campos devueltos `estimated_total` y `warning` |
| **FR-013** | Transferencia íntegra a `Iniciar reserva` | Traspaso al payload de creación de reserva en estado `Iniciada` |
| **FR-014** | Recotización ante cambio de fechas o pasajeros | Regla de descarte de cotizaciones obsoletas (Sección 4) |
| **FR-015** | Bloqueo preventivo ante activo sin tarifas o total $\le 0$ | Respuestas `404` y `422` mapeadas a `503 COTIZACION_NO_DISPONIBLE` |
| **FR-016** | Falla o timeout de M3 cancela creación de reserva | Fail-safe documentado en la Sección 3 y 4 |
| **FR-017** | Operación de solo lectura sin compromisos contables | Especificación stateless en la Sección 1 |
| **SC-001** | 100% de montos provistos directamente por M3 | Uso literal de `estimated_total` |
| **SC-002** | Advertencia obligatoria exacta sin modificaciones | Validación de la cadena literal del campo `warning` |
| **SC-005** | Rechazo por fechas inválidas impide crear reserva | Manejo de error 400 |
| **SC-006** | 0% de reservas con montos nulos o negativos | Bloqueo ante valores anómalos de M3 |
| **SC-007** | Modificación de itinerario invalida cotización previa | Invalidez en memoria descrita en la Sección 4 |
| **SC-008** | Latencia objetivo de Módulo 3 en individual | Propuesta SLA 800 ms / Timeout 1000 ms (Sección 4 y 5) |
