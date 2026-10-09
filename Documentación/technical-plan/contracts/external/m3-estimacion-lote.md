# Contrato de Integración Externa: Estimación en Lote (M3)

**Módulo Proveedor**: Módulo 3 – Liquidación, Seguros y Dispersión de Fondos ("el sistema")  
**Responsable de implementarlo**: Equipo de Módulo 3  
**Módulo Consumidor**: Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones ("Sistema de Reservas y Operaciones")  
**CUs de Módulo 2 que lo consumen**: 
- Invocador principal: `CU-11 Proveer información cotización de reserva` (Modo Lote, `features/CU-11-proveer-informacion-cotizacion-reserva/spec.md`).
- Consumidor final de la información: `CU-01 Buscar embarcaciones disponibles` (previsualización de tarifas de catálogo, FR-003).
**Fecha**: 2026-10-09  

---

## 1. Resumen y Propósito de la Integración

Módulo 3 es la **autoridad única en cálculos y tarifas financieras** en la plataforma SEA-SHARE. Para evitar el bloqueo de formularios y permitir la exploración rápida en el catálogo de búsqueda de CU-01, Módulo 3 expone este endpoint en lote que estima la tarifa base preliminar para múltiples embarcaciones de forma simultánea.

**Regla de Negocio del Cálculo**:
- Módulo 3 procesa la solicitud asumiendo por defecto **1 día de duración y 1 pasajero** (FR-006 de CU-11).
- Esta cotización de previsualización excluye estrictamente el seguro náutico y el depósito de garantía; representa únicamente la tarifa base referencial del activo naval (FR-006, Edge Case).
- La operación es de **solo lectura, idempotente y sin efectos contables**: no retiene fondos, no aparta inventario y no requiere rollback si el usuario abandona la navegación (FR-017).

---

## 2. Definición del Endpoint

### Método HTTP y URL

```http
POST /api/v1/estimates/batch
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

**Body (JSON)**:

```json
{
  "boat_ids": [
    "string (UUID)"
  ]
}
```

*Restricciones de la petición*:
- Tamaño máximo del lote por solicitud: **50 embarcaciones** (FR-005, SC-003).
- Si Módulo 2 recibe una lista con duplicados, los deduplica previamente antes de enviar (FR-004).
- Si la lista está vacía, Módulo 2 no invoca este endpoint y retorna un arreglo vacío inmediatamente (FR-004).

---

### Elementos de la Respuesta Esperada (Response)

**Código de estado HTTP (éxito)**: `200 OK`

**Contrato de respuesta tipado (snake_case estricto)**:

```json
{
  "estimates": [
    {
      "boat_id": "string (UUID)",
      "estimated_total": "number (monto decimal de tarifa base)"
    }
  ],
  "unavailable": [
    {
      "boat_id": "string (UUID)",
      "reason": "string (código de indisponibilidad)"
    }
  ]
}
```

*Detalles del payload de Módulo 3*:
- No transporta el campo `currency` / `moneda` en el payload JSON; Módulo 2 asume la divisa oficial de operación de la plataforma (`COP`) según los acuerdos de consistencia.
- La lista `unavailable` identifica embarcaciones que carecen de esquema tarifario o que están deshabilitadas en el motor financiero, sin tumbar la cotización de las demás embarcaciones del lote (FR-007, SC-004).

---

### Ejemplos de Petición y Respuestas Exitosas

#### Ejemplo 1 — Estimación exitosa completa de un lote de 2 embarcaciones

**Petición `curl`**:

```bash
curl -X POST "https://finanzas.seashare.internal/api/v1/estimates/batch" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.serviceTokenM2" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "boat_ids": [
      "d3b07384-d113-49cd-a5d6-812e9bcfc101",
      "8f4b2319-58b9-4c8d-b0a3-9e41f7d12a99"
    ]
  }'
```

**Respuesta (`200 OK`)**:

```json
{
  "estimates": [
    {
      "boat_id": "d3b07384-d113-49cd-a5d6-812e9bcfc101",
      "estimated_total": 3200000.00
    },
    {
      "boat_id": "8f4b2319-58b9-4c8d-b0a3-9e41f7d12a99",
      "estimated_total": 4500000.00
    }
  ],
  "unavailable": []
}
```

---

#### Ejemplo 2 — Respuesta parcial con una embarcación no cotizable

**Petición `curl`**:

```bash
curl -X POST "https://finanzas.seashare.internal/api/v1/estimates/batch" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.serviceTokenM2" \
  -H "Content-Type: application/json" \
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
  "estimates": [
    {
      "boat_id": "d3b07384-d113-49cd-a5d6-812e9bcfc101",
      "estimated_total": 3200000.00
    }
  ],
  "unavailable": [
    {
      "boat_id": "a9999999-0000-0000-0000-000000000001",
      "reason": "TARIFA_NO_CONFIGURADA"
    }
  ]
}
```

---

## 3. Matriz de Errores y Reacción de Módulo 2

| Código HTTP M3 | Causa en Módulo 3 | Reacción de Módulo 2 en Catálogo | Mapeo al Cliente Final |
|---|---|---|---|
| `400 Bad Request` | Formato JSON corrupto o arreglo `boat_ids` mal estructurado | Módulo 2 detecta falla de integración interna, registra error y no reintenta | Despliega catálogo con `cotizacion_disponible: false` |
| `401 Unauthorized` / `403 Forbidden` | Token de servicio expirado o no autorizado | Alerta crítica de seguridad en observabilidad | Despliega catálogo con `cotizacion_disponible: false` |
| `500 Internal Server Error` / `503 Service Unavailable` | Caída interna o sobrecarga del motor de liquidación | Módulo 2 captura la excepción (FR-008) y continúa con la renderización del catálogo | Despliega catálogo con `cotizacion_disponible: false` y `tarifa_estimada: null` |
| `Timeout` (> 2000 ms) | Tiempo de espera agotado en red o base de datos de M3 | Corta la conexión inmediatamente (*circuit breaker* / timeout) | Despliega catálogo con `cotizacion_disponible: false` |

---

## 4. Parámetros de Resiliencia, Timeouts y Reintentos

- **Límite de Tamaño de Lote**: máximo **50 identificadores** por llamada (FR-005, SC-003). Si una operación de catálogo o carga requiriera cotizar más de 50 embarcaciones, Módulo 2 segmenta en bloques de $\le 50$, ejecuta las llamadas en paralelo o secuencial y consolida los arreglos `estimates` y `unavailable` antes de responder.
- **SLA de Respuesta**: `1500 ms` [NEEDS CLARIFICATION: PROPUESTA SLA 1500 ms pendiente de ratificación formal por Módulo 3].
- **Read Timeout**: `2000 ms` [NEEDS CLARIFICATION: PROPUESTA Timeout 2000 ms].
- **Connect Timeout**: `150 ms`.
- **Política de Reintentos**:
  - En modo lote, **cero reintentos agresivos**: para evitar degradar el SLA global del catálogo de CU-01 (fijado en $< 2000\text{ ms}$ en SC-002), si Módulo 3 no responde en el primer intento, se aplica degradación funcional inmediata (*graceful degradation*) mostrando las tarjetas del catálogo sin precio de referencia (FR-008).
- **Regla Estricta "Sin Dinero"**: Módulo 2 almacena y transporta `estimated_total` como `BigDecimal` literal entregado por Módulo 3, sin aplicar redondeos ni operaciones locales (FR-002, SC-001).

---

## 5. Puntos Abiertos y Aclaraciones Necesarias

- `[NEEDS CLARIFICATION: SLA y Timeout de Módulo 3 en Lote]`: Módulo 3 no declara un SLA contractual en sus especificaciones hacia Módulo 2. Se registra formalmente la propuesta arquitectónica de **SLA de 1500 ms** y **Timeout de 2000 ms** para revisión y ratificación entre equipos.
- `[NEEDS CLARIFICATION: Moneda en estimación de lote]`: Módulo 3 omite el campo `currency` en su payload de lote. Módulo 2 asume contractualmente la moneda oficial de la plataforma (`COP`) en concordancia con el diseño de catálogo.

---

## 6. Trazabilidad FR/SC → Elemento del Contrato

| Requisito / Criterio | Descripción en Spec CU-11 | Elemento de este Contrato |
|---|---|---|
| **FR-001** | Modo Lote para previsualización masiva en catálogo | Endpoint `POST /api/v1/estimates/batch` |
| **FR-002** | Prohibición estricta de cálculos matemáticos en M2 | Uso directo del valor `estimated_total` |
| **FR-003** | Invocación en lote desde CU-01 en bloques de página | Consumo orquestado desde la paginación de 20 barcos |
| **FR-004** | Deduplicación de IDs y control de lista vacía | Reglas de cliente implementadas en la Sección 2 |
| **FR-005** | Fragmentación de peticiones si superan 50 barcos | Umbral fijado en la Sección 4 |
| **FR-006** | Exclusión de seguro náutico y depósito en catálogo | Supuestos de M3 documentados en la Sección 1 |
| **FR-007** | Manejo de embarcaciones sin tarifa sin abortar | Arreglo `unavailable` de M3 manejado en Sección 2 y 3 |
| **FR-008** | Falla de M3 no interrumpe la navegación de catálogo | Matriz de degradación (Sección 3) hacia `cotizacion_disponible: false` |
| **FR-017** | Operación de solo lectura sin compromisos contables | Especificación stateless en la Sección 1 |
| **SC-001** | 100% de precios entregados literalmente por M3 | Inmutabilidad de `estimated_total` |
| **SC-003** | Fragmentación sin pérdida de datos ante lotes extensos | Segmentación documentada en Sección 4 |
| **SC-004** | 0% caídas de catálogo por activos sin tarifa | Segmentación en arreglos `estimates` / `unavailable` |
| **SC-008** | Latencia objetivo de Módulo 3 en lote | Propuesta SLA 1500 ms / Timeout 2000 ms (Sección 4 y 5) |
