# Contrato REST: Buscar embarcaciones disponibles (CU-01)

**Módulo**: Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones  
**Spec de referencia**: [`Documentación/features/CU-01-buscar-embarcaciones-disponibles/spec.md`](../../features/CU-01-buscar-embarcaciones-disponibles/spec.md)  
**Fecha**: 2026-10-09

Este caso de uso expone la operación REST de exploración y búsqueda paginada del catálogo de embarcaciones de SEA-SHARE (FR-001 a FR-010). Permite filtrar por rango de fechas, cantidad de pasajeros, tipo de embarcación y texto de búsqueda libre, o explorar el catálogo completo sin filtros (User Story 2).

El endpoint orquesta internamente dos dependencias:
1. Recupera la información técnica y de puerto invocando en modo lote a `Proveer información de embarcación` (`<<include>>`, FR-002), respaldado por el contrato de [`CU-09 Proveer información de embarcación`](../CU-09-proveer-informacion-embarcacion/plan.md).
2. Obtiene las tarifas estimadas de referencia invocando en modo lote a `CU-11 Proveer información cotización de reserva` (`<<include>>`, FR-003), respaldado por el contrato de [`CU-11 Proveer información cotización de reserva`](../CU-11-proveer-informacion-cotizacion-reserva/plan.external.lote.md).

Cada elemento retornado constituye el punto de extensión hacia la vista detallada [`CU-19-ver-detalle-embarcacion.md`](CU-19-ver-detalle-embarcacion.md) (`<<extend>>`, FR-005).

---

## Endpoint — Búsqueda y exploración de embarcaciones

### Método HTTP y URL

```http
GET /api/v1/vessels
```

### Elementos de la Petición (Request)

**Headers**:

| Nombre | Obligatorio | Descripción |
|---|---|---|
| `Authorization` | Sí | `Bearer <token>` — JWT válido del usuario con perfil Arrendatario autenticado (requisito de seguridad de plataforma) |
| `Accept` | No | `application/json` (valor predeterminado si se omite) |

**Query Parameters**:

| Nombre | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `start_date` | string (ISO 8601, `YYYY-MM-DD`) | No | Fecha inicial deseada para el alquiler náutico (FR-001) |
| `end_date` | string (ISO 8601, `YYYY-MM-DD`) | No | Fecha final deseada para el alquiler náutico (FR-001) |
| `passengers` | number (entero positivo) | No | Cantidad de personas requeridas para la travesía (FR-001) |
| `type` | string | No | Categoría de embarcación (`MOTORBOAT`, `SAILBOAT`, `YACHT`, `CATAMARAN`) (FR-001, FR-006) |
| `q` | string | No | Búsqueda textual rápida por nombre comercial, marina o isla (FR-006) |
| `page` | number (entero $\ge 1$) | No | Número de página solicitada. Valor por defecto: `1` (FR-008) |

*(Nota: la paginación opera con un tamaño fijo inmutable de 20 elementos por página, FR-008).*

**Path Parameters**: No tiene.

**Body**: No tiene (operación `GET`).

---

### Elementos de la Respuesta (Response)

**Código de estado HTTP (éxito)**: `200 OK`

**Contrato de respuesta tipado**:

```json
{
  "pagination": {
    "page": "number (entero)",
    "page_size": "number (entero, fijo 20)",
    "total_items": "number (entero)",
    "total_pages": "number (entero)"
  },
  "context_label": "string",
  "vessels": [
    {
      "vessel_id": "string (UUID)",
      "name": "string",
      "type": "string",
      "photo_url": "string (URL)",
      "port": {
        "name": "string",
        "latitude": "number",
        "longitude": "number",
        "timezone": "string"
      },
      "max_capacity": "number (entero)",
      "captain_included": "boolean",
      "quotation_available": "boolean",
      "estimated_total": "string (decimal literal entregado por Módulo 3)",
      "currency": "string (COP fijo)"
    }
  ]
}
```

*(Si la cotización para una embarcación no está disponible por contingencia en Módulo 3, `quotation_available` es `false` y `estimated_total` es `null`, cumpliendo SC-001 y Edge Case "Ausencia de Cotización Temporal").*

**Headers de respuesta**:

| Nombre | Valor |
|---|---|
| `Content-Type` | `application/json` |
| `Cache-Control` | `no-store` (catálogo y cotizaciones consultados en tiempo real) |

---

### Ejemplos de Petición y Respuestas Exitosas

#### Ejemplo 1 — Búsqueda con filtros y cotización exitosa

**Petición `curl`**:

```bash
curl -X GET "https://api.seashare.com/api/v1/vessels?start_date=2026-11-15&end_date=2026-11-17&passengers=6&type=YACHT&page=1" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMTIyMzMzNC00NDU1LTY2NzctODg5OS1hYWJiY2NkZGVlZmYiLCJyb2wiOiJBcnJlbmRhdGFyaW8iLCJpYXQiOjE3OTE1NDAwMDB9.sampleToken" \
  -H "Accept: application/json"
```

**Respuesta (`200 OK`)**:

```json
{
  "pagination": {
    "page": 1,
    "page_size": 20,
    "total_items": 2,
    "total_pages": 1
  },
  "context_label": "Resultados de búsqueda en el Caribe colombiano",
  "vessels": [
    {
      "vessel_id": "d3b07384-d113-49cd-a5d6-812e9bcfc101",
      "name": "Yate Tayrona Sea Breeze",
      "type": "YACHT",
      "photo_url": "https://cdn.seashare.com/flota/yate-tayrona-01.jpg",
      "port": {
        "name": "Marina Internacional de Santa Marta",
        "latitude": 11.2443,
        "longitude": -74.2125,
        "timezone": "America/Bogota"
      },
      "max_capacity": 12,
      "captain_included": true,
      "quotation_available": true,
      "estimated_total": "3200000.00",
      "currency": "COP"
    },
    {
      "vessel_id": "8f4b2319-58b9-4c8d-b0a3-9e41f7d12a99",
      "name": "Cartagena Grand Luxury",
      "type": "YACHT",
      "photo_url": "https://cdn.seashare.com/flota/cartagena-luxury-03.jpg",
      "port": {
        "name": "Muelle de la Bodeguita, Cartagena",
        "latitude": 10.4211,
        "longitude": -75.5489,
        "timezone": "America/Bogota"
      },
      "max_capacity": 10,
      "captain_included": true,
      "quotation_available": true,
      "estimated_total": "4500000.00",
      "currency": "COP"
    }
  ]
}
```

---

#### Ejemplo 2 — Exploración de catálogo completo sin filtros (User Story 2)

**Petición `curl`**:

```bash
curl -X GET "https://api.seashare.com/api/v1/vessels?page=1" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMTIyMzMzNC00NDU1LTY2NzctODg5OS1hYWJiY2NkZGVlZmYiLCJyb2wiOiJBcnJlbmRhdGFyaW8iLCJpYXQiOjE3OTE1NDAwMDB9.sampleToken" \
  -H "Accept: application/json"
```

**Respuesta (`200 OK`)**:

```json
{
  "pagination": {
    "page": 1,
    "page_size": 20,
    "total_items": 25,
    "total_pages": 2
  },
  "context_label": "Populares esta semana",
  "vessels": [
    {
      "vessel_id": "d3b07384-d113-49cd-a5d6-812e9bcfc101",
      "name": "Yate Tayrona Sea Breeze",
      "type": "YACHT",
      "photo_url": "https://cdn.seashare.com/flota/yate-tayrona-01.jpg",
      "port": {
        "name": "Marina Internacional de Santa Marta",
        "latitude": 11.2443,
        "longitude": -74.2125,
        "timezone": "America/Bogota"
      },
      "max_capacity": 12,
      "captain_included": true,
      "quotation_available": true,
      "estimated_total": "3200000.00",
      "currency": "COP"
    },
    {
      "vessel_id": "6a9e10fa-13f5-4de9-9e87-a25e9821d303",
      "name": "Catamarán Rosario Dreams",
      "type": "CATAMARAN",
      "photo_url": "https://cdn.seashare.com/flota/rosario-catamaran-01.jpg",
      "port": {
        "name": "Club Náutico de Cartagena",
        "latitude": 10.4072,
        "longitude": -75.5398,
        "timezone": "America/Bogota"
      },
      "max_capacity": 16,
      "captain_included": true,
      "quotation_available": true,
      "estimated_total": "2800000.00",
      "currency": "COP"
    }
  ]
}
```

---

#### Ejemplo 3 — Degradación controlada por falla parcial o total en Módulo 3 (Edge Case)

**Petición `curl`**:

```bash
curl -X GET "https://api.seashare.com/api/v1/vessels?page=1" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.sampleToken"
```

**Respuesta (`200 OK`)**:

```json
{
  "pagination": {
    "page": 1,
    "page_size": 20,
    "total_items": 1,
    "total_pages": 1
  },
  "context_label": "Catálogo general (Tarifas en actualización)",
  "vessels": [
    {
      "vessel_id": "d3b07384-d113-49cd-a5d6-812e9bcfc101",
      "name": "Yate Tayrona Sea Breeze",
      "type": "YACHT",
      "photo_url": "https://cdn.seashare.com/flota/yate-tayrona-01.jpg",
      "port": {
        "name": "Marina Internacional de Santa Marta",
        "latitude": 11.2443,
        "longitude": -74.2125,
        "timezone": "America/Bogota"
      },
      "max_capacity": 12,
      "captain_included": true,
      "quotation_available": false,
      "estimated_total": null,
      "currency": "COP"
    }
  ]
}
```

---

#### Ejemplo 4 — Búsqueda sin coincidencias en catálogo (User Story 1, Escenario 2)

**Petición `curl`**:

```bash
curl -X GET "https://api.seashare.com/api/v1/vessels?q=IslaFicticiaInexistente" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.sampleToken"
```

**Respuesta (`200 OK`)**:

```json
{
  "pagination": {
    "page": 1,
    "page_size": 20,
    "total_items": 0,
    "total_pages": 0
  },
  "context_label": "Sin resultados disponibles",
  "vessels": []
}
```

---

### Manejo de Errores

| Código | Caso | Cuerpo de respuesta (ejemplo) |
|---|---|---|
| `400 Bad Request` | Parámetros de búsqueda con formato sintáctico inválido (ej. fecha no ISO 8601, número de pasajeros negativo o no numérico, tipo no admitido) (FR-010, SC-004) | `{ "code": "INVALID_PARAMETERS", "message": "Los parámetros de búsqueda proporcionados no cumplen con el formato requerido" }` |
| `401 Unauthorized` | Token ausente, inválido o expirado | `{ "code": "UNAUTHENTICATED", "message": "Token de autenticación ausente o inválido" }` |
| `403 Forbidden` | El claim de rol del JWT no corresponde a Arrendatario | `{ "code": "PROFILE_NOT_AUTHORIZED", "message": "Este recurso requiere perfil Arrendatario" }` |
| `503 Service Unavailable` | Caída o tiempo de espera agotado al consultar el catálogo de Módulo 1 (*fail-safe* preventivo) | `{ "code": "FLEET_SERVICE_UNAVAILABLE", "message": "No se pudo consultar el catálogo de embarcaciones en este momento" }` |
| `500 Internal Server Error` | Excepción interna no controlada en el servicio | `{ "code": "INTERNAL_ERROR", "message": "Ocurrió un error inesperado al procesar la búsqueda" }` |

---

### Paginación

- **Política de tamaño**: la paginación es fija y mandatoria en **20 embarcaciones por página** (FR-008). No se permite al cliente alterar el tamaño de la página para resguardar los umbrales máximos de cotización en lote hacia Módulo 3.
- **Transmisión segmentada**: el backend de Módulo 2 transmite a Módulo 1 y Módulo 3 únicamente los identificadores correspondientes al lote de 20 embarcaciones de la página solicitada (FR-008).
- **Control de límites**: si `page` excede `total_pages`, la lista `vessels` se entrega vacía con los metadatos correspondientes.

---

### Seguridad y Perfiles

- Requiere JWT emitido por el servicio de identidad centralizado de SEA-SHARE.
- Perfil autorizado: **Arrendatario**.
- El controlador REST valida estrictamente el claim de rol (`role == "RENTER"`) antes de disparar las consultas subordinadas de catálogo y cotización.
- **Regla estricta "Sin dinero"**: Módulo 2 jamás calcula subtotales, promedios ni tarifas nocturnas. El importe monetario contenido en `estimated_total` se transporta como string decimal literal entregado por Módulo 3, sin redondeo (FR-002 de CU-11, SC-001).

---

### Notas Transversales

- **Disponibilidad por rango de fechas y filtrado en M2**: Módulo 1 no recibe fechas en la consulta de catálogo (`CU-09`). Si la petición incluye `start_date` y `end_date`, Módulo 2 realiza un filtrado local de disponibilidad consultando sus reservas locales activas: excluye del listado devuelto por M1 a toda embarcación que tenga una reserva en estado `PENDING_PAYMENT`, `RESERVED` o `IN_NAVIGATION` cuyo rango de fechas se solape con el solicitado (`daterange(start_date, end_date, '[]') && daterange(start_date, end_date, '[]')`). Esta depuración se ejecuta previamente a solicitar la cotización en lote hacia Módulo 3 (`CU-11`), optimizando el rendimiento de la integración. La verificación física de estado operativo instantáneo (`AVAILABLE`) se revalida en `Iniciar reserva` (`CU-02`) e `Iniciar pago` (`CU-03`).
- **Degradación funcional ante falla de Módulo 3**: si el adaptador hacia Módulo 3 experimenta timeout o indisponibilidad (Edge Case "Ausencia de Cotización Temporal"), el catálogo **no** retorna 503; entrega la lista de embarcaciones con `quotation_available: false` y `estimated_total: null`. En cambio, si falla Módulo 1 (fuente de verdad física), se aplica el *fail-safe* obligatorio retornando `503 Service Unavailable`.
- **Navegación e interacción UI**: las entidades retornadas incluyen los metadatos visuales requeridos (`photo_url`, `type`, `captain_included`, `context_label`) para renderizar directamente las tarjetas del catálogo y permitir la selección que extiende hacia [`CU-19-ver-detalle-embarcacion.md`](CU-19-ver-detalle-embarcacion.md) (FR-004, FR-005, FR-007).
- **Filtro por tipo**: El filtro por `type` lo aplica Módulo 2 sobre la lista recibida de Módulo 1; M1 no recibe ese parámetro.

---

### Trazabilidad FR/SC → Elemento del Contrato

| Requisito / Criterio | Descripción en Spec | Elemento de este Contrato |
|---|---|---|
| **FR-001** | Criterios opcionales de búsqueda (fechas, pasajeros, tipo) | Query parameters: `start_date`, `end_date`, `passengers`, `type` |
| **FR-002** | Consulta de inventario mediante `Proveer información de embarcación` | Invocación interna al adaptador de catálogo respaldado por el [plan de CU-09](../CU-09-proveer-informacion-embarcacion/plan.md) |
| **FR-003** | Cotización en lote mediante `CU-11` | Invocación interna al adaptador en lote respaldado por el [plan de CU-11](../CU-11-proveer-informacion-cotizacion-reserva/plan.external.lote.md) |
| **FR-004** | Datos mínimos del activo y tarifa estimada devuelta | Propiedades del objeto `vessels`: `name`, `type`, `photo_url`, `port`, `max_capacity`, `estimated_total`, `currency` |
| **FR-005** | Punto de extensión hacia `Ver detalle de embarcación` | Identificador `vessel_id` para invocar [`CU-19-ver-detalle-embarcacion.md`](CU-19-ver-detalle-embarcacion.md) |
| **FR-006** | Búsqueda rápida de texto y categorías | Query parameters: `q`, `type` |
| **FR-007** | Contexto dinámico y contador de resultados | Campos del envelope: `context_label`, `pagination.total_items` |
| **FR-008** | Paginación en lotes fijos de 20 embarcaciones | Objeto `pagination` con `page_size: 20` y parámetro `page` |
| **FR-009** | Búsqueda sin filtros retorna catálogo completo | Manejo en query params vacíos (Ejemplo exitoso 2) |
| **FR-010** | Rechazo preventivo ante entradas con formato inválido | Respuesta `400 Bad Request` con código `INVALID_PARAMETERS` |
| **SC-001** | 100% de resultados con tarifa de M3 (salvo caídas) | Campos `estimated_total` y `currency` poblados literalmente y campo `quotation_available` |
| **SC-002** | Latencia < 2000 ms en condiciones normales | Documentado en objetivos técnicos de integración |
| **SC-003** | 100% de búsquedas sin filtros devuelven catálogo completo | Validado en contrato y Ejemplo 2 |
| **SC-004** | Rechazo previo sin consultar Módulo 1 o Módulo 3 | Validación sintáctica previa reflejada en error `400` |