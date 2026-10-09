# Contrato de Integración Externa: Consultar Información de Embarcación (M1)

**Módulo Proveedor**: Módulo 1 – Gestión de Flota y Activos P2P  
**Responsable de implementarlo**: Equipo de Módulo 1  
**Módulo Consumidor**: Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones  
**CUs de Módulo 2 que lo consumen**: 
- Invocador principal: `CU-09 Proveer información de embarcación` (`features/CU-09-proveer-informacion-embarcacion/spec.md`).
- Consumidores dependientes: 
  - `CU-01 Buscar embarcaciones disponibles` (operación lote para catálogo).
  - `CU-19 Ver detalle de embarcación` (operación individual para ficha técnica).
  - `CU-04 Solicitar cancelación` (obtención de ubicación/zona horaria del puerto para ventanas de 72 h y 24 h).
  - `CU-05 Marcar inasistencia` (obtención de zona horaria del puerto para tolerancia de 30 min).
**Fecha**: 2026-10-09  

---

## 1. Resumen y Propósito de la Integración

Módulo 1 actúa como la **única fuente de verdad física del inventario naval** en SEA-SHARE. Módulo 2 no posee base de datos de embarcaciones ni duplica sus atributos descriptivos (nombre, matrícula, eslora, camarotes, amenidades); almacena estrictamente identificadores (`embarcacion_id`, `propietario_id`) para trazabilidad operativa (FR-004 de CU-09, SC-003).

Este contrato formaliza las dos operaciones síncronas que Módulo 2 espera que Módulo 1 exponga e implemente:
1. **Consulta Individual**: obtención de la ficha técnica completa y ubicación portuaria de una embarcación por su identificador.
2. **Consulta en Lote**: recuperación paginada de embarcaciones disponibles con datos de puerto para poblar el catálogo de búsqueda de CU-01.

---

## 2. Operación 1 — Consulta Individual de Embarcación

### Método HTTP y URL

```http
GET /api/v1/embarcaciones/{embarcacionId}
```

### Elementos de la Petición (Request)

**Headers**:

| Nombre | Obligatorio | Descripción |
|---|---|---|
| `Authorization` | Sí | `Bearer <token>` — JWT firmado de identidad de servicio expedido para Módulo 2 [NEEDS CLARIFICATION] |
| `Accept` | No | `application/json` |

**Path Parameters**:

| Nombre | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `embarcacionId` | string (UUID) | Sí | Identificador único del activo naval en Módulo 1 |

**Query Parameters**: No tiene.

**Body**: No tiene (operación `GET`).

---

### Elementos de la Respuesta Esperada (Response)

**Código de estado HTTP (éxito)**: `200 OK`

**Contrato de respuesta tipado**:

```json
{
  "embarcacion_id": "string (UUID)",
  "nombre": "string",
  "matricula": "string",
  "tipo": "string (Lancha | Velero | Yate | Catamaran)",
  "capacidad_maxima": "number (entero)",
  "eslora_pies": "number",
  "numero_camarotes": "number (entero)",
  "capitan_incluido": "boolean",
  "puerto": {
    "nombre": "string",
    "latitud": "number",
    "longitud": "number",
    "zona_horaria": "string (identificador IANA, ej. America/Bogota) [NEEDS CLARIFICATION]"
  },
  "amenidades": [
    "string"
  ],
  "propietario": {
    "propietario_id": "string (UUID)",
    "nombre_validado": "string",
    "verificado": "boolean"
  }
}
```

*(Módulo 2 requiere mandatoriamente la propiedad `puerto.zona_horaria` para calcular los plazos de tolerancia y cancelación sin recurrir a horas locales del servidor).*

---

### Ejemplo de Petición y Respuesta Exitosa (Operación Individual)

**Petición `curl`**:

```bash
curl -X GET "https://flota.seashare.internal/api/v1/embarcaciones/d3b07384-d113-49cd-a5d6-812e9bcfc101" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.serviceIdentityM2Token" \
  -H "Accept: application/json"
```

**Respuesta (`200 OK`)**:

```json
{
  "embarcacion_id": "d3b07384-d113-49cd-a5d6-812e9bcfc101",
  "nombre": "Yate Tayrona Sea Breeze",
  "matricula": "CP-04-2021-0892",
  "tipo": "Yate",
  "capacidad_maxima": 12,
  "eslora_pies": 48.5,
  "numero_camarotes": 3,
  "capitan_incluido": true,
  "puerto": {
    "nombre": "Marina Internacional de Santa Marta",
    "latitud": 11.2443,
    "longitud": -74.2125,
    "zona_horaria": "America/Bogota"
  },
  "amenidades": [
    "Aire Acondicionado",
    "Sistema de Sonido Bluetooth",
    "Equipo de Esnórquel",
    "Refrigerador Marino",
    "Plataforma de Baño",
    "Ducha de Popa"
  ],
  "propietario": {
    "propietario_id": "a1c2e3f4-5678-90ab-cdef-1234567890ab",
    "nombre_validado": "Inversiones Náuticas del Caribe S.A.S.",
    "verificado": true
  }
}
```

---

## 3. Operación 2 — Consulta en Lote / Catálogo de Embarcaciones

### Método HTTP y URL

```http
GET /api/v1/embarcaciones
```

### Elementos de la Petición (Request)

**Headers**:

| Nombre | Obligatorio | Descripción |
|---|---|---|
| `Authorization` | Sí | `Bearer <token>` — JWT de identidad de servicio |
| `Accept` | No | `application/json` |

**Query Parameters**:

| Nombre | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `page` | number (entero $\ge 1$) | No | Página solicitada (default: 1) |
| `size` | number (entero) | No | Tamaño de página solicitado por M2 (fijo en 20 para el catálogo, FR-008) |
| `tipo` | string | No | Filtro de categoría opcional (`Lancha`, `Velero`, `Yate`, `Catamaran`) |
| `q` | string | No | Filtro textual opcional por nombre o ubicación |

---

### Elementos de la Respuesta Esperada (Response)

**Código de estado HTTP (éxito)**: `200 OK`

**Contrato de respuesta tipado**:

```json
{
  "paginacion": {
    "pagina": "number (entero)",
    "tamano_pagina": "number (entero)",
    "total_elementos": "number (entero)",
    "total_paginas": "number (entero)"
  },
  "embarcaciones": [
    {
      "embarcacion_id": "string (UUID)",
      "nombre": "string",
      "tipo": "string",
      "foto_url": "string (URL)",
      "puerto": {
        "nombre": "string",
        "latitud": "number",
        "longitud": "number",
        "zona_horaria": "string"
      },
      "capacidad_maxima": "number (entero)",
      "capitan_incluido": "boolean"
    }
  ]
}
```

---

### Ejemplo de Petición y Respuesta Exitosa (Operación en Lote)

**Petición `curl`**:

```bash
curl -X GET "https://flota.seashare.internal/api/v1/embarcaciones?page=1&size=20&tipo=Yate" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.serviceIdentityM2Token" \
  -H "Accept: application/json"
```

**Respuesta (`200 OK`)**:

```json
{
  "paginacion": {
    "pagina": 1,
    "tamano_pagina": 20,
    "total_elementos": 1,
    "total_paginas": 1
  },
  "embarcaciones": [
    {
      "embarcacion_id": "d3b07384-d113-49cd-a5d6-812e9bcfc101",
      "nombre": "Yate Tayrona Sea Breeze",
      "tipo": "Yate",
      "foto_url": "https://cdn.seashare.com/flota/yate-tayrona-01.jpg",
      "puerto": {
        "nombre": "Marina Internacional de Santa Marta",
        "latitud": 11.2443,
        "longitud": -74.2125,
        "zona_horaria": "America/Bogota"
      },
      "capacidad_maxima": 12,
      "capitan_incluido": true
    }
  ]
}
```

---

## 4. Matriz de Errores y Reacción de Módulo 2

| Código HTTP M1 | Causa en Módulo 1 | Reacción Arquitectónica de Módulo 2 | Código Mapeado por M2 |
|---|---|---|---|
| `400 Bad Request` | Parámetros o `embarcacionId` con sintaxis inválida | Módulo 2 registra el error y aborta inmediatamente sin reintento | `400 Bad Request` (`ID_INVALIDO` / `PARAMETROS_INVALIDOS`) |
| `401 Unauthorized` / `403 Forbidden` | Token de servicio de M2 rechazado o sin permisos | Módulo 2 genera alerta crítica en observabilidad y corta la ejecución | `500 Internal Server Error` (`ERROR_INTERNO`) |
| `404 Not Found` | Embarcación no registrada en flota | En consulta individual, informa no disponibilidad del activo sin reintentos | `404 Not Found` (`EMBARCACION_NO_DISPONIBLE`) |
| `500 Internal Server Error` / `502 Bad Gateway` | Falla interna o caída en Módulo 1 | Módulo 2 ejecuta máximo un (1) reintento rápido; si persiste, activa *fail-safe* | `503 Service Unavailable` (`SERVICIO_FLOTA_NO_DISPONIBLE`) |
| `Timeout` (sin respuesta) | Lectura supera los 300 ms | Dispara un (1) reintento inmediato; si se agota, aborta de forma segura | `503 Service Unavailable` (`SERVICIO_FLOTA_NO_DISPONIBLE`) |

---

## 5. Parámetros de Resiliencia, Timeouts y Reintentos

- **Connect Timeout**: `100 ms` (comunicación dentro de la red interna de microservicios).
- **Read Timeout**: `300 ms` (en estricta concordancia con el SLA de SC-001 de CU-09: consultas a M1 deben resolverse en < 300 ms).
- **Política de Reintentos**:
  - Máximo **un (1) reintento rápido** ante fallos transitorios (`IOException`, `SocketTimeoutException`, `503`).
  - **Cero reintentos** ante cualquier código `4xx` (FR-008).
- **Fail-Safe Obligatorio**: ante caída total de Módulo 1, el 100% de las consultas rechaza la operación preventivamente; Módulo 2 nunca asume datos predeterminados ni opera a ciegas (FR-008, SC-004).
- **Caché Prohibida**: Módulo 2 no almacena en caché local las fichas técnicas de barcos para evitar mostrar datos desactualizados si el propietario modifica servicios en M1 (SC-003, Edge Case "Información siempre actualizada").

---

## 6. Puntos Abiertos y Aclaraciones Necesarias

- `[NEEDS CLARIFICATION: zona_horaria en puerto]`: se requiere ratificar con el equipo de Módulo 1 la inclusión del atributo `zona_horaria` (ej. `"America/Bogota"`) en el objeto `puerto`. Módulo 2 depende estrictamente de este valor para evaluar ventanas operativas de zarpe, check-in y cancelación (CU-09 US2, T018 de plan.md).
- `[NEEDS CLARIFICATION: mecanismo de autenticación servicio-a-servicio]`: acordar con el equipo de arquitectura de seguridad el formato y emisor del JWT de servicio para llamadas directas M2 $\rightarrow$ M1.
- `[NEEDS CLARIFICATION: contrato formal de catálogo en lote de Módulo 1]`: alinear con el equipo de Módulo 1 la semántica de filtros de catálogo y tamaño de lote (registrado como tema abierto H9 en plan.md).

---

## 7. Trazabilidad FR/SC → Elemento del Contrato

| Requisito / Criterio | Descripción en Spec CU-09 | Elemento de este Contrato |
|---|---|---|
| **FR-001** | Interfaz interna para consultar datos de barcos | Operación 1 (individual) y Operación 2 (lote) |
| **FR-002** | Recepción de código de barco y verificación de capacidad | Path parameter `embarcacionId` y campo `capacidad_maxima` devuelto |
| **FR-003** | Consulta directa a API externa de Módulo 1 | Endpoints `/api/v1/embarcaciones/{id}` y `/api/v1/embarcaciones` |
| **FR-004** | Campos requeridos de flota (nombre, matrícula, tipo, capacidad, puerto GPS, servicios, dueño) | Objeto JSON tipado de la Operación 1 |
| **FR-005** | Verificación estricta de capacidad | Campo `capacidad_maxima` utilizado por CU-09 para validar pasajeros |
| **FR-006** | Rechazo por superación de capacidad | Lógica interna de M2 respaldada por el dato `capacidad_maxima` de M1 |
| **FR-007** | Rechazo por barco no encontrado en M1 | Manejo de respuesta HTTP `404 Not Found` |
| **FR-008** | Política de reintentos (máx 1 rápido) y fail-safe | Sección 5: Timeout 300 ms, 1 reintento, nunca ante 4xx |
| **FR-009** | Regla estricta "Sin dinero" (M1 no expone precios) | Payload de M1 carece totalmente de campos financieros |
| **FR-010** | Trazabilidad y registro de consultas | Propagación de Correlation-ID y headers de auditoría |
| **SC-001** | 100% de consultas resueltas en < 300 ms | Read Timeout fijado en 300 ms |
| **SC-002** | Cero operaciones permitidas con exceso de pasajeros | Sustentado por `capacidad_maxima` oficial de M1 |
| **SC-003** | Cero accesos directos a BD de M1 o cálculos de precios en M2 | Consumo exclusivamente vía API REST síncrona |
| **SC-004** | 100% de fallas en M1 resultan en fail-safe preventivo | Matriz de errores (Sección 4) y corte a 503 |
| **SC-005** | Obtención de puerto para hora local | Objeto `puerto` con latitud, longitud y `zona_horaria` |
