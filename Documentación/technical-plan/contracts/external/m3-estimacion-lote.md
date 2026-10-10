# Contrato de Integración Externa: Estimación en Lote (M3)

**Módulo Proveedor**: Módulo 3 – Liquidación, Seguros y Dispersión de Fondos ("el sistema")  
**Responsable de implementarlo**: Equipo de Módulo 3  
**Módulo Consumidor**: Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones ("Sistema de Reservas y Operaciones")  
**CUs de Módulo 2 que lo consumen**:

* Invocador principal: `CU-11 Proveer información cotización de reserva` (Modo Lote, `features/CU-11-proveer-informacion-cotizacion-reserva/spec.md`).
* Consumidor final de la información: `CU-01 Buscar embarcaciones disponibles` (previsualización de tarifas de catálogo, FR-003).

**Fecha**: 2026-10-09

---

## 1. Resumen y Propósito de la Integración

Módulo 3 es la **autoridad única en cálculos y tarifas financieras** en la plataforma SEA-SHARE. Para evitar el bloqueo de formularios y permitir la exploración rápida en el catálogo de búsqueda de CU-01, Módulo 3 expone este endpoint en lote que estima la tarifa base preliminar para múltiples embarcaciones de forma simultánea.

**Regla de Negocio del Cálculo**:

* El lote supone **1 día**, **1 pasajero** y la **fecha actual como fecha de evaluación de la tarifa** (FR-006 de CU-11).
* La estimación **incluye el seguro náutico de 1 pasajero** y no incluye el depósito de garantía.
* La operación es de **solo lectura, idempotente y sin efectos contables**: no retiene fondos, no aparta inventario y no requiere rollback si el usuario abandona la navegación (FR-017).

---

## 2. Definición del Endpoint

### Método HTTP y URL

```http
POST /api/v1/estimates/batch
```

### Elementos de la Petición (Request)

**Headers**:

|Nombre|Obligatorio|Descripción|
|-|-|-|
|`Authorization`|Sí|`Bearer <token>` — JWT de identidad de servicio expedido para Módulo 2|
|`Content-Type`|Sí|`application/json`|
|`Accept`|No|`application/json`|

**Path Parameters**: No tiene.

**Query Parameters**: No tiene.

**Body (JSON)**:

```json
{
  "boat_ids": [
    "string (UUID)"
  ]
}
```

**Restricciones de la petición**:

* El body solo acepta `boat_ids`. Si M2 incluye `start_date`, `end_date`, `passengers` u otros campos, M3 responde `400 VALIDATION_ERROR`.
* Tamaño máximo del lote por solicitud: **50 embarcaciones** (FR-005, SC-003).
* Si Módulo 2 recibe una lista con duplicados, los deduplica previamente antes de enviar (FR-004).
* Si la lista está vacía, Módulo 2 no invoca este endpoint y retorna un arreglo vacío inmediatamente (FR-004).

\---

### Elementos de la Respuesta Esperada (Response)

**Código de estado HTTP (éxito)**: `200 OK`

**Contrato de respuesta tipado (snake_case estricto)**:

```json
{
  "evaluation_date": "string (ISO 8601, YYYY-MM-DD)",
  "duration_days": 1,
  "passengers": 1,
  "estimates": [
    {
      "boat_id": "string (UUID)",
      "estimated_total": "string decimal"
    }
  ],
  "unavailable": [
    {
      "boat_id": "string (UUID)",
      "reason": "SIN_TARIFA_BASE"
    }
  ]
}
```

Módulo 2 parsea `estimated_total` como `BigDecimal`, sin redondeo ni operaciones monetarias locales.

**Detalles del payload de Módulo 3**:

* No transporta el campo `currency` / `moneda` en el payload JSON; Módulo 2 asume la divisa oficial de operación de la plataforma (`COP`) según los acuerdos de consistencia.
* La lista `unavailable` identifica embarcaciones que carecen de tarifa base, sin tumbar la cotización de las demás embarcaciones del lote (FR-007, SC-004).
* Las embarcaciones que M3/Flota no reconocen no aparecen ni en `estimates` ni en `unavailable`: M2 las trata como cotización no disponible (`cotizacion_disponible: false`).

\---

### Ejemplos de Petición y Respuestas Exitosas

#### Ejemplo 1 — Estimación exitosa completa de un lote de 2 embarcaciones

**Petición `curl`**:

```bash
curl -X POST "https://finanzas.seashare.internal/api/v1/estimates/batch" \\
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.serviceTokenM2" \\
  -H "Content-Type: application/json" \\
  -H "Accept: application/json" \\
  -d '{
    "boat\_ids": \[
      "d3b07384-d113-49cd-a5d6-812e9bcfc101",
      "8f4b2319-58b9-4c8d-b0a3-9e41f7d12a99"
    ]
  }'
```

**Respuesta (`200 OK`)**:

```json
{
  "evaluation_date": "2026-10-09",
  "duration_days": 1,
  "passengers": 1,
  "estimates": [
    {
      "boat_id": "d3b07384-d113-49cd-a5d6-812e9bcfc101",
      "estimated_total": "3215000.00"
    },
    {
      "boat_id": "8f4b2319-58b9-4c8d-b0a3-9e41f7d12a99",
      "estimated_total": "4500000.00"
    }
  ],
  "unavailable": []
}
```

#### Ejemplo 2 — Respuesta parcial con una embarcación sin tarifa base

**Petición `curl`**:

```bash
curl -X POST "https://finanzas.seashare.internal/api/v1/estimates/batch" \\
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.serviceTokenM2" \\
  -H "Content-Type: application/json" \\
  -d '{
    "boat_ids": [
      "d3b07384-d113-49cd-a5d6-812e9bcfc101",
      "a9999999-0000-0000-0000-000000000001"
    ]
  }'
```

**Respuesta (`200 OK`)**:

```json
{
  "evaluation_date": "2026-10-09",
  "duration_days": 1,
  "passengers": 1,
  "estimates": [
    {
      "boat_id": "d3b07384-d113-49cd-a5d6-812e9bcfc101",
      "estimated_total": "3215000.00"
    }
  ],
  "unavailable": [
    {
      "boat_id": "a9999999-0000-0000-0000-000000000001",
      "reason": "SIN_TARIFA_BASE"
    }
  ]
}
```

\---

## 3\. Matriz de Errores y Reacción de Módulo 2

Los errores de Módulo 3 se reciben en formato `application/problem+json` e incluyen el campo `code` cuando corresponde.

|Código HTTP M3|`code` causa|Reacción de Módulo 2|Mapeo al cliente final|
|-|-|-|-|
|400|`BATCH_SIZE_EXCEEDED`|Registra el error de integración; no reintenta|Catálogo con `cotizacion_disponible: false`|
|400|`VALIDATION_ERROR`|Alerta de integración; no reintenta|Catálogo con `cotizacion_disponible: false`|
|401 / 403|`UNAUTHENTICATED` / `FORBIDDEN`|Alerta crítica de seguridad|Catálogo con `cotizacion_disponible: false`|
|503|`FLEET_UNAVAILABLE`|Fail-safe; continúa con la renderización del catálogo|Catálogo con `cotizacion_disponible: false` y `tarifa_estimada: null`|
|503|`FINANCIAL_PARAMETERS_NOT_CONFIGURED`|Fail-safe; continúa con la renderización del catálogo|Catálogo con `cotizacion_disponible: false` y `tarifa_estimada: null`|
|500 / timeout|`INTERNAL_ERROR`  tiempo de espera agotado|Fail-safe; continúa con la renderización del catálogo|Catálogo con `cotizacion_disponible: false` y `tarifa_estimada: null`|

\---

## 4\. Parámetros de Resiliencia, Timeouts y Reintentos

* **Límite de Tamaño de Lote**: máximo **50 identificadores** por llamada (FR-005, SC-003). Si una operación de catálogo o carga requiriera cotizar más de 50 embarcaciones, Módulo 2 segmenta en bloques de ≤ 50, ejecuta las llamadas en paralelo o secuencial y consolida los arreglos `estimates` y `unavailable` antes de responder.
* **SLA de Respuesta**: `1500 ms` [NEEDS CLARIFICATION: propuesta de SLA de 1500 ms pendiente de ratificación formal por Módulo 3].
* **Read Timeout**: `1000 ms` [NEEDS CLARIFICATION: propuesta de timeout de 1000 ms]. Este límite busca que la llamada de M3 quepa en el SLA de `2000 ms` de CU-01, considerando el presupuesto de M1 (300 ms más un reintento).
* **Connect Timeout**: `150 ms`.
* **Política de Reintentos**:

  * En modo lote, **cero reintentos agresivos**: para evitar degradar el SLA global del catálogo de CU-01 (fijado en `< 2000 ms` en SC-002), si Módulo 3 no responde en el primer intento, se aplica degradación funcional inmediata (*graceful degradation*) mostrando las tarjetas del catálogo sin precio de referencia (FR-008).
* **Regla Estricta "Sin Dinero"**: Módulo 2 almacena y transporta `estimated_total` como `BigDecimal` literal entregado por Módulo 3, sin aplicar redondeos ni operaciones locales (FR-002, SC-001).

---

## 5. Puntos Abiertos y Aclaraciones Necesarias

* `[NEEDS CLARIFICATION: SLA y Timeout de Módulo 3 en Lote]`: Módulo 3 no declara un SLA contractual en sus especificaciones hacia Módulo 2. Se registra formalmente la propuesta arquitectónica de **SLA de 1500 ms** y **Timeout de 1000 ms** para revisión y ratificación entre equipos.
* **Moneda en estimación de lote**: Módulo 3 omite el campo `currency` en su payload de lote. Módulo 2 asume contractualmente la moneda oficial de la plataforma (`COP`) en concordancia con el diseño de catálogo.

---

## 6. Trazabilidad FR/SC → Elemento del Contrato

|Requisito / Criterio|Descripción en Spec CU-11|Elemento de este Contrato|
|-|-|-|
|**FR-001**|Modo Lote para previsualización masiva en catálogo|Endpoint `POST /api/v1/estimates/batch`|
|**FR-002**|Prohibición estricta de cálculos matemáticos en M2|Uso directo del valor `estimated_total`|
|**FR-003**|Invocación en lote desde CU-01 en bloques de página|Consumo orquestado desde la paginación de 20 barcos|
|**FR-004**|Deduplicación de IDs y control de lista vacía|Reglas de cliente implementadas en la Sección 2|
|**FR-005**|Fragmentación de peticiones si superan 50 barcos|Umbral fijado en la Sección 4|
|**FR-006**|Estimación de lote con tarifa base final + seguro de 1 pasajero|Supuestos de M3 documentados en la Sección 1|
|**FR-007**|Manejo de embarcaciones sin tarifa sin abortar|Arreglo `unavailable` de M3 manejado en las secciones 2 y 3|
|**FR-008**|Falla de M3 no interrumpe la navegación de catálogo|Matriz de degradación (Sección 3) hacia `cotizacion_disponible: false`|
|**FR-017**|Operación de solo lectura sin compromisos contables|Especificación stateless en la Sección 1|
|**SC-001**|100% de precios entregados literalmente por M3|Inmutabilidad de `estimated_total`|
|**SC-003**|Fragmentación sin pérdida de datos ante lotes extensos|Segmentación documentada en la Sección 4|
|**SC-004**|0% caídas de catálogo por activos sin tarifa|Segmentación en arreglos `estimates` / `unavailable`|
|**SC-008**|Latencia objetivo de Módulo 3 en lote|Propuesta SLA 1500 ms / Timeout 1000 ms (secciones 4 y 5)|



