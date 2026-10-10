# Contrato de Interfaz REST: CU-20 Ver Mis Reservas

## 1. Identificación y Metadatos

- **Identificador del Contrato**: `REST-M2-CU20-VER-MIS-RESERVAS`
- **Módulo Responsable**: Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones
- **Tipo de Interfaz**: REST sincrónico expuesto por Módulo 2
- **Actor / Consumidor Autorizado**: 
  - `RENTER` (recupera sus reservas contratadas)
  - `OWNER` (recupera las reservas recibidas para sus embarcaciones)
- **Caso de Uso Base / Relaciones**: 
  - Punto base extendido por: `Ver detalle de reserva` (`<<extend>>` - CU-21)
- **Versión del Contrato**: 1.0.0
- **Fecha de Creación**: 2026-10-09

---

## 2. Propósito y Alcance del Endpoint

Este endpoint de solo lectura permite a los usuarios autenticados consultar el listado histórico y vigente de sus reservas con soporte de paginación y filtrado por pestañas.

### Responsabilidades del Endpoint:
1. **Aislamiento Estricto por Rol (Data Isolation)**:
   - Si el solicitante es **Arrendatario**, el sistema filtra automáticamente las reservas donde `renter_id` coincide con el identificador del usuario autenticado en el token JWT.
   - Si el solicitante es **Propietario**, el sistema filtra las reservas donde `owner_id` coincide con el identificador del usuario autenticado en el token JWT (reservas sobre sus activos náuticos).
   - Un usuario jamás puede ver reservas que no le pertenezcan directamente (`SC-002`).
2. **Paginación y Ordenamiento**:
   - Aplica paginación determinista fijando por defecto 10 reservas por página (`FR-012`).
   - Ordena cronológicamente de forma descendente por fecha de creación (`created_at DESC`), ubicando las transacciones más recientes al inicio.
3. **Pestañas y Filtrado Operativo**:
   - Soporta filtrado contextual por pestañas de la interfaz:
     - `all`: devuelve el universo completo de reservas del usuario.
     - `history`: reservas activas o exitosamente completadas (`INITIATED`, `PENDING_PAYMENT`, `RESERVED`, `IN_NAVIGATION`, `COMPLETED`).
     - `cancelled`: reservas terminadas prematuramente (`CANCELLED`, `EXPIRED`, `PAYMENT_FAILED`).
4. **Regla de Negocio "Sin Dinero" (Inmutabilidad Financiera)**:
   - El monto total expuesto en cada reserva es el valor original congelado al momento de su liquidación por Módulo 3 (`FR-004`).
   - Módulo 2 **no calcula deducciones, penalidades ni reembolsos en reservas canceladas** (`FR-004`, `SC-001`). Los sub-estados contractuales (`FLEXIBLE`, `MODERATE`, `LATE`, `BY_OWNER`, `NO_SHOW`) se exponen como contexto cualitativo sin alterar el monto total original.
5. **Diferenciación Semántica de Vistas**:
   - Para el **Arrendatario**: expone datos de la embarcación, fechas, total congelado y etiquetas contextuales de reembolso gestionado por M3 (`FR-010`).
   - Para el **Propietario**: expone el nombre del arrendatario titular, cantidad de pasajeros y fechas para la coordinación logística de muelle (`FR-009`).
6. **Manejo de Estado Vacío (Empty State)**:
   - Cuando el usuario no cuenta con reservas asociadas, devuelve un arreglo vacío `[]` con los metadatos de paginación en ceros (`total_items: 0`), permitiendo a la UI renderizar estados informativos amigables.

---

## 3. Definición del Endpoint

- **Método HTTP**: `GET`
- **Ruta**: `/api/v1/reservations`
- **Formato de Petición / Respuesta**: `application/json`
- **Codificación**: `UTF-8`

### 3.1 Encabezados HTTP (Headers)

| Header | Tipo | Obligatorio | Descripción |
| :--- | :--- | :--- | :--- |
| `Authorization` | String | Sí | Token Bearer JWT del usuario autenticado (`Bearer eyJhbG...`). |
| `Accept` | String | Sí | Debe ser `application/json`. |

### 3.2 Parámetros de Consulta (Query Parameters)

| Parámetro | Tipo | Obligatorio | Valor por Defecto | Descripción / Reglas |
| :--- | :--- | :--- | :--- | :--- |
| `page` | Entero | No | `1` | Número de página a recuperar ($\ge 1$). |
| `limit` | Entero | No | `10` | Cantidad de reservas por página ($1 \le limit \le 50$). Por defecto 10 según FR-012. |
| `filter` | String (Enum) | No | `all` | Filtro por pestaña: `"all"`, `"history"`, `"cancelled"`. |

---

## 4. Estructura de Datos (Schema JSON)

### 4.1 Cuerpo de Respuesta Exitosa (`200 OK`)

```json
{
  "pagination": {
    "page": 1,
    "page_size": 10,
    "total_items": 12,
    "total_pages": 2
  },
  "applied_filter": "all | history | cancelled",
  "user_role": "RENTER | OWNER",
  "view_title": "Mis reservas | Reservas recibidas",
  "reservations": [
    {
      "reservation_id": "c1f7a8b2-5e4d-4c3b-8a1e-9f0a2b3c4d5e",
      "reservation_code": "#RS-4492",
      "status": "RESERVED",
      "sub_status": null,
      "status_badge": {
        "text": "Confirmada",
        "semantic_color": "verde",
        "description": "Pago confirmado y lista para zarpe"
      },
      "vessel": {
        "vessel_id": "b3e2a1d0-4f5c-6b7a-8e9f-0a1b2c3d4e5f",
        "name": "Catamarán Sea Breeze",
        "image_url": "https://cdn.seashare.com/images/boats/sea-breeze-main.jpg",
        "port": "Marina Santa Marta"
      },
      "dates": {
        "start_at": "2026-11-20T09:00:00-05:00",
        "end_at": "2026-11-22T18:00:00-05:00",
        "duration_days": 3,
        "duration_nights": 2
      },
      "passengers": 4,
      "total_price": {
        "amount": 3450000.00,
        "currency": "COP",
        "is_frozen": true
      },
      "renter": {
        "renter_id": "u1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
        "name": "Laura Gómez"
      },
      "owner": {
        "owner_id": "p9a8b7c6-d5e4-3f2a-1b0c-9d8e7f6a5b4c",
        "name": "Carlos Mendoza"
      },
      "contextual_label": null,
      "detail_url": "/api/v1/reservations/c1f7a8b2-5e4d-4c3b-8a1e-9f0a2b3c4d5e",
      "created_at": "2026-10-09T10:00:00-05:00"
    }
  ]
}
```

#### Descripción de Campos de Salida

| Campo | Tipo | Nulabilidad | Descripción |
| :--- | :--- | :--- | :--- |
| `pagination` | Objeto | No nulo | Objeto envoltorio de control de paginación (`page`, `page_size`, `total_items`, `total_pages`). |
| `applied_filter` | String | No nulo | Criterio de filtro activo (`"all"`, `"history"`, `"cancelled"`). |
| `user_role` | String | No nulo | Rol con el que se resolvió la consulta (`"RENTER"` o `"OWNER"`). |
| `view_title` | String | No nulo | Título semántico según rol (`"Mis reservas"` o `"Reservas recibidas"`). |
| `reservations` | Array | No nulo | Lista de objetos de reserva. Vacía `[]` si no hay resultados. |
| `reservations[].reservation_id` | String (UUID) | No nulo | Identificador universal único de la reserva. |
| `reservations[].reservation_code` | String | No nulo | Código amigable de referencia náutica (ej. `"#RS-4492"`). |
| `reservations[].status` | String (Enum) | No nulo | Estado operativo principal (`"INITIATED"`, `"PENDING_PAYMENT"`, `"RESERVED"`, `"IN_NAVIGATION"`, `"COMPLETED"`, `"CANCELLED"`, `"EXPIRED"`, `"PAYMENT_FAILED"`). |
| `reservations[].sub_status` | String (Enum) | Nulo condicional | Sub-clasificación contractual en reservas canceladas (`"FLEXIBLE"`, `"MODERATE"`, `"LATE"`, `"BY_OWNER"`, `"NO_SHOW"`). Nulo en otros estados. |
| `reservations[].status_badge` | Objeto | No nulo | Metadatos visuales para badges (texto legible y color semántico: `verde`, `amarillo`, `gris`, `rojo`). |
| `reservations[].vessel` | Objeto | No nulo | Datos referenciales mínimos de la embarcación (ID, nombre, imagen y puerto). |
| `reservations[].dates` | Objeto | No nulo | Rango temporal pactado, días y noches de navegación. |
| `reservations[].passengers` | Entero | No nulo | Cantidad de ocupantes autorizados para el viaje. |
| `reservations[].total_price` | Objeto | No nulo | Monto consolidado original provisto por M3 al crear/pagar la reserva. **No recalculado en canceladas**. |
| `reservations[].renter` | Objeto | No nulo | Identificador y nombre del arrendatario titular. |
| `reservations[].owner` | Objeto | No nulo | Identificador y nombre del propietario anfitrión. |
| `reservations[].contextual_label`| String | Nulo condicional | Mensaje aclaratorio de políticas para el Arrendatario (ej. `"Reembolso gestionado por Módulo 3"`). |
| `reservations[].detail_url` | String | No nulo | Ruta relativa para invocar `Ver detalle de reserva` (`CU-21`). |
| `reservations[].created_at` | String (ISO 8601) | No nulo | Marca de tiempo de creación de la reserva. |

---

## 5. Ejemplos de Petición y Respuesta

### Ejemplo 1: Consulta exitosa como Arrendatario (Pestaña "todos", página 1)

#### Petición HTTP (`curl`)
```bash
curl -X GET "https://api.seashare.com/api/v1/reservations?page=1&limit=10&filter=all" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Accept: application/json"
```

#### Respuesta Exitosa (`200 OK`)
```json
{
  "pagination": {
    "page": 1,
    "page_size": 10,
    "total_items": 3,
    "total_pages": 1
  },
  "applied_filter": "all",
  "user_role": "RENTER",
  "view_title": "Mis reservas",
  "reservations": [
    {
      "reservation_id": "c1f7a8b2-5e4d-4c3b-8a1e-9f0a2b3c4d5e",
      "reservation_code": "#RS-4492",
      "status": "RESERVED",
      "sub_status": null,
      "status_badge": {
        "text": "Confirmada",
        "semantic_color": "verde",
        "description": "Reserva lista para zarpe"
      },
      "vessel": {
        "vessel_id": "b3e2a1d0-4f5c-6b7a-8e9f-0a1b2c3d4e5f",
        "name": "Catamarán Sea Breeze",
        "image_url": "https://cdn.seashare.com/images/boats/sea-breeze.jpg",
        "port": "Marina Santa Marta"
      },
      "dates": {
        "start_at": "2026-11-20T09:00:00-05:00",
        "end_at": "2026-11-22T18:00:00-05:00",
        "duration_days": 3,
        "duration_nights": 2
      },
      "passengers": 4,
      "total_price": {
        "amount": 3450000.00,
        "currency": "COP",
        "is_frozen": true
      },
      "renter": {
        "renter_id": "u1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
        "name": "Laura Gómez"
      },
      "owner": {
        "owner_id": "p9a8b7c6-d5e4-3f2a-1b0c-9d8e7f6a5b4c",
        "name": "Carlos Mendoza"
      },
      "contextual_label": null,
      "detail_url": "/api/v1/reservations/c1f7a8b2-5e4d-4c3b-8a1e-9f0a2b3c4d5e",
      "created_at": "2026-10-09T10:00:00-05:00"
    },
    {
      "reservation_id": "d2e3f4a5-6b7c-8d9e-0f1a-2b3c4d5e6f7a",
      "reservation_code": "#RS-3810",
      "status": "CANCELLED",
      "sub_status": "FLEXIBLE",
      "status_badge": {
        "text": "Cancelada",
        "semantic_color": "rojo",
        "description": "Cancelada voluntariamente con anticipación > 72h"
      },
      "vessel": {
        "vessel_id": "e4f5a6b7-8c9d-0e1f-2a3b-4c5d6e7f8a9b",
        "name": "Yate Poseidón",
        "image_url": "https://cdn.seashare.com/images/boats/poseidon.jpg",
        "port": "Club Náutico Cartagena"
      },
      "dates": {
        "start_at": "2026-10-15T08:00:00-05:00",
        "end_at": "2026-10-15T17:00:00-05:00",
        "duration_days": 1,
        "duration_nights": 0
      },
      "passengers": 6,
      "total_price": {
        "amount": 2100000.00,
        "currency": "COP",
        "is_frozen": true
      },
      "renter": {
        "renter_id": "u1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
        "name": "Laura Gómez"
      },
      "owner": {
        "owner_id": "p8b7a6c5-d4e3-2f1a-0b9c-8d7e6f5a4b3c",
        "name": "Andrés Morales"
      },
      "contextual_label": "Cancelada - Ventana Flexible / Reembolso gestionado por Módulo 3",
      "detail_url": "/api/v1/reservations/d2e3f4a5-6b7c-8d9e-0f1a-2b3c4d5e6f7a",
      "created_at": "2026-10-01T14:20:00-05:00"
    },
    {
      "reservation_id": "f5a6b7c8-9d0e-1f2a-3b4c-5d6e7f8a9b0c",
      "reservation_code": "#RS-2950",
      "status": "COMPLETED",
      "sub_status": null,
      "status_badge": {
        "text": "Completada",
        "semantic_color": "verde",
        "description": "Viaje finalizado exitosamente"
      },
      "vessel": {
        "vessel_id": "b3e2a1d0-4f5c-6b7a-8e9f-0a1b2c3d4e5f",
        "name": "Catamarán Sea Breeze",
        "image_url": "https://cdn.seashare.com/images/boats/sea-breeze.jpg",
        "port": "Marina Santa Marta"
      },
      "dates": {
        "start_at": "2026-09-10T10:00:00-05:00",
        "end_at": "2026-09-11T16:00:00-05:00",
        "duration_days": 2,
        "duration_nights": 1
      },
      "passengers": 4,
      "total_price": {
        "amount": 2300000.00,
        "currency": "COP",
        "is_frozen": true
      },
      "renter": {
        "renter_id": "u1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
        "name": "Laura Gómez"
      },
      "owner": {
        "owner_id": "p9a8b7c6-d5e4-3f2a-1b0c-9d8e7f6a5b4c",
        "name": "Carlos Mendoza"
      },
      "contextual_label": "Depósito sin disputa / Reembolso gestionado por Módulo 3",
      "detail_url": "/api/v1/reservations/f5a6b7c8-9d0e-1f2a-3b4c-5d6e7f8a9b0c",
      "created_at": "2026-09-02T11:00:00-05:00"
    }
  ]
}
```

---

### Ejemplo 2: Consulta exitosa como Propietario (Pestaña "historial", reservas recibidas)

#### Petición HTTP (`curl`)
```bash
curl -X GET "https://api.seashare.com/api/v1/reservations?page=1&limit=10&filter=history" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Accept: application/json"
```

#### Respuesta Exitosa (`200 OK`)
```json
{
  "pagination": {
    "page": 1,
    "page_size": 10,
    "total_items": 2,
    "total_pages": 1
  },
  "applied_filter": "history",
  "user_role": "OWNER",
  "view_title": "Reservas recibidas",
  "reservations": [
    {
      "reservation_id": "c1f7a8b2-5e4d-4c3b-8a1e-9f0a2b3c4d5e",
      "reservation_code": "#RS-4492",
      "status": "RESERVED",
      "sub_status": null,
      "status_badge": {
        "text": "Confirmada",
        "semantic_color": "verde",
        "description": "Reserva lista para zarpe"
      },
      "vessel": {
        "vessel_id": "b3e2a1d0-4f5c-6b7a-8e9f-0a1b2c3d4e5f",
        "name": "Catamarán Sea Breeze",
        "image_url": "https://cdn.seashare.com/images/boats/sea-breeze.jpg",
        "port": "Marina Santa Marta"
      },
      "dates": {
        "start_at": "2026-11-20T09:00:00-05:00",
        "end_at": "2026-11-22T18:00:00-05:00",
        "duration_days": 3,
        "duration_nights": 2
      },
      "passengers": 4,
      "total_price": {
        "amount": 3450000.00,
        "currency": "COP",
        "is_frozen": true
      },
      "renter": {
        "renter_id": "u1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
        "name": "Laura Gómez"
      },
      "owner": {
        "owner_id": "p9a8b7c6-d5e4-3f2a-1b0c-9d8e7f6a5b4c",
        "name": "Carlos Mendoza"
      },
      "contextual_label": null,
      "detail_url": "/api/v1/reservations/c1f7a8b2-5e4d-4c3b-8a1e-9f0a2b3c4d5e",
      "created_at": "2026-10-09T10:00:00-05:00"
    },
    {
      "reservation_id": "a9b8c7d6-e5f4-3a2b-1c0d-9e8f7a6b5c4d",
      "reservation_code": "#RS-4480",
      "status": "IN_NAVIGATION",
      "sub_status": null,
      "status_badge": {
        "text": "En navegación",
        "semantic_color": "amarillo",
        "description": "Viaje náutico en curso"
      },
      "vessel": {
        "vessel_id": "b3e2a1d0-4f5c-6b7a-8e9f-0a1b2c3d4e5f",
        "name": "Catamarán Sea Breeze",
        "image_url": "https://cdn.seashare.com/images/boats/sea-breeze.jpg",
        "port": "Marina Santa Marta"
      },
      "dates": {
        "start_at": "2026-10-09T08:00:00-05:00",
        "end_at": "2026-10-09T17:00:00-05:00",
        "duration_days": 1,
        "duration_nights": 0
      },
      "passengers": 6,
      "total_price": {
        "amount": 1800000.00,
        "currency": "COP",
        "is_frozen": true
      },
      "renter": {
        "renter_id": "u9z8y7x6-w5v4-3u2t-1s0r-9q8p7o6n5m4l",
        "name": "Mateo Rossi"
      },
      "owner": {
        "owner_id": "p9a8b7c6-d5e4-3f2a-1b0c-9d8e7f6a5b4c",
        "name": "Carlos Mendoza"
      },
      "contextual_label": null,
      "detail_url": "/api/v1/reservations/a9b8c7d6-e5f4-3a2b-1c0d-9e8f7a6b5c4d",
      "created_at": "2026-10-08T16:00:00-05:00"
    }
  ]
}
```

---

### Ejemplo 3: Consulta sin resultados (Empty State)

#### Petición HTTP (`curl`)
```bash
curl -X GET "https://api.seashare.com/api/v1/reservations?page=1&limit=10&filter=cancelled" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Accept: application/json"
```

#### Respuesta Exitosa (`200 OK`)
```json
{
  "pagination": {
    "page": 1,
    "page_size": 10,
    "total_items": 0,
    "total_pages": 0
  },
  "applied_filter": "cancelled",
  "user_role": "RENTER",
  "view_title": "Mis reservas",
  "reservations": []
}
```

---

## 6. Respuestas de Error y Códigos HTTP

Todos los errores retornan el sobre estándar con `code` y `message`:

```json
{
  "code": "ERROR_CODE",
  "message": "Descripción detallada del motivo de rechazo."
}
```

| Código HTTP | Código Interno (`code`) | Causa / Condición de Disparo | Cuerpo de Respuesta de Ejemplo |
| :--- | :--- | :--- | :--- |
| **`400 Bad Request`** | `INVALID_QUERY_PARAMS` | Parámetros de paginación inválidos (`page < 1`, `limit < 1`, `limit > 50`, o filtro no soportado). | `{"code": "INVALID_QUERY_PARAMS", "message": "El parámetro 'page' debe ser mayor o igual a 1 y 'limit' debe encontrarse entre 1 y 50."}` |
| **`401 Unauthorized`** | `AUTH_TOKEN_MISSING_OR_INVALID` | El header `Authorization` no fue provisto o el token JWT expiró. | `{"code": "AUTH_TOKEN_MISSING_OR_INVALID", "message": "Token de autenticación ausente o inválido."}` |
| **`403 Forbidden`** | `FORBIDDEN_INVALID_ROLE` | El usuario autenticado posee un rol no autorizado para consultar este catálogo personal. | `{"code": "FORBIDDEN_INVALID_ROLE", "message": "El perfil de usuario no cuenta con autorización para acceder al panel de reservas."}` |
| **`500 Internal Server Error`** | `INTERNAL_SERVER_ERROR` | Fallo no controlado de la base de datos de Módulo 2. | `{"code": "INTERNAL_SERVER_ERROR", "message": "Error interno del servidor al recuperar el listado de reservas."}` |

---

## 7. Notas Transversales y Reglas de Negocio Arquitecturales

### 7.1 Regla de Oro "Sin Dinero" (Zero Financial Calculations)
En estricto apego a `FR-004` y `SC-001`, Módulo 2 se abstiene por completo de calcular devoluciones, porcentajes o deducciones sobre reservas `CANCELLED`.
- En una reserva en estado `CANCELLED` con sub-estado `FLEXIBLE`, `MODERATE` o `LATE`, el campo `total_price.amount` exhibe literalmente el monto total que fue acordado y congelado al inicio.
- Cualquier valor derivado de liquidación, compensación o indemnización es propiedad de Módulo 3 y se gestionará en los reportes contables independientes de dicho módulo.

### 7.2 Aislamiento de Datos por Identidad JWT
El controlador extrae directamente el ID del usuario (`sub` o `user_id`) y su rol del token JWT verificado en el contexto de seguridad de Spring.
- Un usuario no puede pasar un parámetro `user_id` en la query string para usurpar consultas ajenas; el filtro en base de datos (`WHERE renter_id = :id` o `WHERE owner_id = :id`) es forzado por el backend a nivel de repositorio.

### 7.3 Orden Determinista y Paginación Eficiente
Las consultas se ejecutan con índices compuestos en base de datos (`renter_id, created_at DESC` y `owner_id, created_at DESC`), garantizando respuestas ágiles por debajo de 50 ms aun cuando el usuario acumule cientos de registros históricos.

---

## 8. Trazabilidad de Requisitos

| Requisito Funcional / Criterio | Descripción en Spec | Cobertura en este Contrato |
| :--- | :--- | :--- |
| **FR-001** | Permitir a Arrendatario y Propietario acceder a su lista de reservas. | Soportado para ambos perfiles mediante token JWT y discriminación de rol. |
| **FR-002** | Filtrar registros en BD según `renter_id` o `owner_id`. | Garantizado en lógica de backend descrita en sección 2 y 7.2. |
| **FR-003** | Presentar embarcación, rango de fechas, estado vigente y precio total. | Presente en cada elemento del arreglo `reservations[]`. |
| **FR-004** | Precio total expuesto debe ser el monto original congelado; cero cálculos en canceladas. | Objeto `total_price` con `is_frozen: true`. Cero cálculos de penalidad o deducción. |
| **FR-005** | Mostrar sub-estados en reservas canceladas (`BY_OWNER`, `NO_SHOW`, etc.). | Campo `sub_status` e `status_badge` contextual. |
| **FR-006** | Enlace para invocar la vista profunda (`Ver detalle de reserva`). | Atributo `detail_url` que apunta a `/api/v1/reservations/{reservation_id}`. |
| **FR-007** | Encabezados diferenciados ("Mis reservas" / "Reservas recibidas") y filtros de pestañas. | Campos `view_title` y parámetro `filter` (`all`, `history`, `cancelled`). |
| **FR-008** | Badges de estado con colores semánticos (`verde`, `amarillo`, `gris`, `rojo`). | Objeto `status_badge` con `semantic_color`. |
| **FR-009** | En Propietario incluir nombre de arrendatario y cantidad de pasajeros. | Campos `renter.name` y `passengers` incluidos en el schema. |
| **FR-010** | En Arrendatario incluir insignias cualitativas sin montos numéricos de reembolso. | Campo `contextual_label` con texto cualitativo (ej. "Reembolso gestionado por Módulo 3"). |
| **FR-011** | Enlace explícito para ver detalles junto al monto total. | Cubierto por `detail_url` para renderizado en frontend. |
| **FR-012** | Paginación fijando límite de 10 reservas por página ordenadas por `created_at DESC`. | Paginación `limit=10` por defecto y ordenación cronológica descendente. |
| **SC-001** | Cero (0%) cálculos de reembolsos o penalidades realizados por Módulo 2. | Esquema estricto sin campos monetarios calculados. |
| **SC-002** | Aislamiento total de datos: 100% de efectividad impidiendo visualización cruzada ajena. | Garantizado por filtrado forzado a través del token JWT del solicitante. |
