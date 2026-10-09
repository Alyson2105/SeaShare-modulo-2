# Contrato de Integración Externa: Cálculo Total de la Reserva (M3)

**Módulo Proveedor**: Módulo 3 – Liquidación, Seguros y Dispersión de Fondos ("el sistema")  
**Responsable de implementarlo**: Equipo de Módulo 3  
**Módulo Consumidor**: Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones ("Sistema de Reservas y Operaciones")  
**CUs de Módulo 2 que lo consumen**: 
- Invocador principal: `CU-12 Brindar cálculo total de la reserva` (`features/CU-12-brindar-calculo-total-reserva/spec.md`).
- Consumidor dependiente: `CU-03 Iniciar pago` (invocado en el checkout para obtener la liquidación final vinculante antes de transicionar a `Pendiente de Pago`, FR-001, FR-003).
**Fecha**: 2026-10-09  

---

## 1. Resumen y Propósito de la Integración

Módulo 3 es el **único motor financiero y la autoridad contable** de SEA-SHARE. Mientras que [`m3-estimacion-individual.md`](m3-estimacion-individual.md) emite una cotización preliminar de vista previa (sin depósito de garantía y con bandera informativa), este contrato formaliza la **liquidación final, completa y vinculante** que se asocia a la reserva ya persistida en estado `Iniciada` al momento de pulsar el pago en [`CU-03-iniciar-pago.md`](../rest/CU-03-iniciar-pago.md).

**Fórmula Financiera Oficial Aplicada por Módulo 3**:
$$\text{Total Vinculante} = (\text{Tarifa Base} \times \text{Duración}) + (\text{Seguro Náutico} \times \text{Pasajeros}) + \text{Depósito de Garantía}$$

**Invariantes de Diseño**:
- **Regla Estricta "Sin Dinero" en Módulo 2**: Módulo 2 jamás calcula, suma ni altera montos (FR-002, SC-001). Los tres rubros del desglose (`alquiler_base`, `seguro_nautico`, `deposito_garantia`) y el `total_amount` se persisten íntegramente de manera literal en los atributos financieros de la reserva.
- **Sincronización con el Temporizador TTL**: el snapshot de liquidación emitido por Módulo 3 expira sincronizadamente a los 15 minutos del TTL de la reserva nacido en `Iniciada` (Edge Case "TTL del snapshot").
- **Principio Fail-Safe**: si Módulo 3 no responde o rechaza el cálculo, la reserva **permanece en estado `Iniciada`** con su TTL en curso y no se bloquea inventario en Módulo 1 (FR-007, FR-008, SC-004).

---

## 2. Definición del Endpoint

### Método HTTP y URL

```http
POST /api/v1/settlements/calculate
```

*(Ruta candidata alternativa sujeta a acuerdo: `POST /api/v1/calculations/total` [NEEDS CLARIFICATION]).*

### Elementos de la Petición (Request)

**Headers**:

| Nombre | Obligatorio | Descripción |
|---|---|---|
| `Authorization` | Sí | `Bearer <token>` — JWT firmado de identidad de servicio expedido para Módulo 2 |
| `Content-Type` | Sí | `application/json` |
| `Accept` | No | `application/json` |

**Path Parameters**: No tiene.

**Query Parameters**: No tiene.

**Body (JSON)** *(convención snake_case de Módulo 3)*:

```json
{
  "reservation_id": "string (UUID)",
  "boat_id": "string (UUID)",
  "start_date": "string (ISO 8601, YYYY-MM-DD)",
  "end_date": "string (ISO 8601, YYYY-MM-DD)",
  "passengers": "number (entero positivo)",
  "renter_id": "string (UUID)"
}
```

*Validaciones requeridas*:
- `reservation_id`: identificador de la reserva previamente creada y persistida en estado `Iniciada` (FR-003).
- `boat_id`: identificador de la embarcación en Módulo 1.
- `start_date` y `end_date`: rango de fechas pactado.
- `passengers`: número total de personas que realizarán el viaje.
- `renter_id`: identificador del Arrendatario titular autenticado.

---

### Elementos de la Respuesta Esperada (Response)

**Código de estado HTTP (éxito)**: `200 OK`

**Contrato de respuesta tipado**:

```json
{
  "calculation_id": "string (UUID)",
  "reservation_id": "string (UUID)",
  "total_amount": "number (BigDecimal literal vinculante)",
  "currency": "string (ej. COP)",
  "breakdown": {
    "base_rental_amount": "number (BigDecimal literal)",
    "insurance_total_amount": "number (BigDecimal literal)",
    "security_deposit_amount": "number (BigDecimal literal)"
  },
  "security_deposit_policy": "string (texto aclaratorio de M3)",
  "created_at": "string (ISO 8601 timestamp)",
  "expires_at": "string (ISO 8601 timestamp sincronizado con el TTL)"
}
```

*Detalles del payload financiero*:
- `breakdown.base_rental_amount`: tarifa base del activo por la duración total del viaje.
- `breakdown.insurance_total_amount`: prima acumulada de la póliza de seguro náutico para la totalidad de los pasajeros.
- `breakdown.security_deposit_amount`: importe del depósito de garantía retenido temporalmente (10% de la tarifa base diaria, congelado para cubrir eventuales daños menores).
- `security_deposit_policy`: texto literal explicativo emitido por Finanzas: `"El depósito se reembolsa completo si el barco se devuelve sin daños"`.
- `expires_at`: vencimiento idéntico al límite de 15 minutos de la reserva en `Iniciada`.

---

### Ejemplo de Petición y Respuesta Exitosa

**Petición `curl`**:

```bash
curl -X POST "https://finanzas.seashare.internal/api/v1/settlements/calculate" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.serviceTokenM2" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "reservation_id": "e4f81c92-7a20-4215-9c5e-8812c3f1a001",
    "boat_id": "d3b07384-d113-49cd-a5d6-812e9bcfc101",
    "start_date": "2026-11-15",
    "end_date": "2026-11-18",
    "passengers": 8,
    "renter_id": "11223344-4455-6677-8899-aabbccddeeff"
  }'
```

**Respuesta (`200 OK`)**:

```json
{
  "calculation_id": "f8a912bc-81d3-41bb-92cc-77aa12dd34ee",
  "reservation_id": "e4f81c92-7a20-4215-9c5e-8812c3f1a001",
  "total_amount": 10560000.00,
  "currency": "COP",
  "breakdown": {
    "base_rental_amount": 9600000.00,
    "insurance_total_amount": 160000.00,
    "security_deposit_amount": 800000.00
  },
  "security_deposit_policy": "El depósito se reembolsa completo si el barco se devuelve sin daños",
  "created_at": "2026-10-09T12:05:00-05:00",
  "expires_at": "2026-10-09T12:20:00-05:00"
}
```

---

## 3. Matriz de Errores y Reacción de Módulo 2

| Código HTTP M3 | Causa en Módulo 3 | Reacción Arquitectónica de Módulo 2 | Mapeo al Endpoint de M2 |
|---|---|---|---|
| `400 Bad Request` | Parámetros de fechas o pasajeros con sintaxis errónea | Módulo 2 aborta el flujo sin transicionar la reserva (permanece en `Iniciada`) | `400 Bad Request` (`PARAMETROS_INVALIDOS`) |
| `401 Unauthorized` / `403 Forbidden` | Credencial de servicio de M2 inválida | Alerta crítica de seguridad en observabilidad | `500 Internal Server Error` (`ERROR_INTERNO`) |
| `404 Not Found` / `422 Unprocessable` | Embarcación sin esquema de tarifas o depósito de garantía no parametrizado en M3 (FR-007) | Bloquea el cobro; la reserva permanece en `Iniciada` y se informa imposibilidad de cobro | `503 Service Unavailable` (`CALCULO_NO_DISPONIBLE`) |
| `500 Internal Server Error` / `503 Service Unavailable` | Caída o sobrecarga interna del motor de liquidación | Aplica *fail-safe* preventivo (FR-008): la reserva permanece en `Iniciada` y no bloquea inventario en M1 | `503 Service Unavailable` (`CALCULO_NO_DISPONIBLE`) |
| `Timeout` (> 1000 ms) | Tiempo de espera agotado hacia Módulo 3 | Corta la conexión inmediatamente; no genera cobros parciales ni con montos en cero | `503 Service Unavailable` (`CALCULO_NO_DISPONIBLE`) |

---

## 4. Parámetros de Resiliencia, Timeouts y Reintentos

- **SLA de Respuesta**: `< 800 ms` [NEEDS CLARIFICATION: PROPUESTA SLA 800 ms pendiente de ratificación formal por Módulo 3].
- **Read Timeout**: `1000 ms` [NEEDS CLARIFICATION: PROPUESTA Timeout 1000 ms].
- **Connect Timeout**: `100 ms`.
- **Política de Reintentos**:
  - Máximo **un (1) reintento rápido** ante contingencias transitorias de socket si el tiempo restante lo permite.
  - **Cero reintentos** ante errores `4xx`.
- **Garantía Fail-Safe y Neutralidad Financiera**: ante cualquier falla de red o rechazo de Módulo 3, el 100% de los casos mantiene la reserva en estado `Iniciada` con su TTL en curso y **no** bloquea inventario en Módulo 1 (FR-008, SC-004). Módulo 2 jamás calcula importes por defecto ni permite avanzar a cobro sin liquidación oficial.
- **Idempotencia Transaccional**: si el usuario envía múltiples pulsaciones simultáneas en el checkout, Módulo 2 canaliza una única llamada remota a Módulo 3 evitando cálculos duplicados redundantes (Edge Case "Idempotencia").

---

## 5. Puntos Abiertos y Aclaraciones Necesarias

- `[NEEDS CLARIFICATION: Nombre formal del endpoint en Módulo 3]`: acordar con el equipo de Módulo 3 si la ruta oficial es `POST /api/v1/settlements/calculate` o `POST /api/v1/calculations/total` (mencionado como tema abierto en FR-004 de CU-12 y en la sección B.2 de plan.md).
- `[NEEDS CLARIFICATION: SLA y Timeout de Liquidación Final]`: ratificar formalmente con Módulo 3 la propuesta técnica de **SLA de 800 ms** y **Timeout de 1000 ms**.

---

## 6. Trazabilidad FR/SC → Elemento del Contrato

| Requisito / Criterio | Descripción en Spec CU-12 | Elemento de este Contrato |
|---|---|---|
| **FR-001** | Invocación subordinada desde `CU-03 Iniciar pago` | Propósito de integración formalizado en la Sección 1 |
| **FR-002** | Prohibición absoluta de cálculos de dinero en Módulo 2 | Inmutabilidad de los campos `total_amount` y `breakdown` |
| **FR-003** | Parámetros consolidados de la reserva en `Iniciada` | Request body: `reservation_id`, `boat_id`, fechas, pasajeros, `renter_id` |
| **FR-004** | Consulta a API de liquidación de Módulo 3 | Endpoint `POST /api/v1/settlements/calculate` |
| **FR-005** | Recepción obligatoria de desglose completo y total vinculante | Objeto `breakdown` con alquiler, seguro y depósito de garantía |
| **FR-006** | Asociación íntegra a la reserva al transicionar a `Pendiente de Pago` | Transferencia literal hacia `CU-03 Iniciar pago` |
| **FR-007** | Manejo de rechazo por falta de tarifas o depósito | Matriz de errores (Sección 3) con fail-safe a `503` |
| **FR-008** | Fail-safe preventivo ante caída de red o timeout | Reserva permanece en `Iniciada` sin bloqueo en M1 |
| **FR-009** | Cifra oficial a cobrar en pasarela | Valor vinculante reflejado en `total_amount` |
| **FR-010** | Asiento auditable de la liquidación recibida | Trazabilidad con `calculation_id`, timestamp y correlation id |
| **SC-001** | 100% de importes provenientes directamente de Módulo 3 | Contrato sin cálculos matemáticos en M2 |
| **SC-002** | Desglose explícito de los tres rubros en la respuesta | Verificado en estructura del objeto `breakdown` |
| **SC-003** | Cero reservas en `Pendiente de Pago` sin cálculo previo | Condición previa obligatoria documentada en CU-03 |
| **SC-004** | Fallas de red mantienen reserva en `Iniciada` sin bloqueo M1 | Garantizado por la arquitectura fail-safe de la Sección 4 |
| **SC-005** | Latencia objetivo de Módulo 3 en liquidación final | Propuesta SLA 800 ms / Timeout 1000 ms (Sección 4 y 5) |
