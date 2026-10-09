# Contrato de Interfaz REST: CU-20 Ver Mis Reservas

## 1. Identificación y Metadatos

- **Identificador del Contrato**: `REST-M2-CU20-VER-MIS-RESERVAS`
- **Módulo Responsable**: Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones
- **Tipo de Interfaz**: REST sincrónico expuesto por Módulo 2
- **Actor / Consumidor Autorizado**: 
  - `ARRENDATARIO` (recupera sus reservas contratadas)
  - `PROPIETARIO` (recupera las reservas recibidas para sus embarcaciones)
- **Caso de Uso Base / Relaciones**: 
  - Punto base extendido por: `Ver detalle de reserva` (`<<extend>>` - CU-21)
- **Versión del Contrato**: 1.0.0
- **Fecha de Creación**: 2026-10-09

---

## 2. Propósito y Alcance del Endpoint

Este endpoint de solo lectura permite a los usuarios autenticados consultar el listado histórico y vigente de sus reservas con soporte de paginación y filtrado por pestañas.

### Responsabilidades del Endpoint:
1. **Aislamiento Estricto por Rol (Data Isolation)**:
   - Si el solicitante es **Arrendatario**, el sistema filtra automáticamente las reservas donde `arrendatario_id` coincide con el identificador del usuario autenticado en el token JWT.
   - Si el solicitante es **Propietario**, el sistema filtra las reservas donde `propietario_id` coincide con el identificador del usuario autenticado en el token JWT (reservas sobre sus activos náuticos).
   - Un usuario jamás puede ver reservas que no le pertenezcan directamente (`SC-002`).
2. **Paginación y Ordenamiento**:
   - Aplica paginación determinista fijando por defecto 10 reservas por página (`FR-012`).
   - Ordena cronológicamente de forma descendente por fecha de creación (`created_at DESC`), ubicando las transacciones más recientes al inicio.
3. **Pestañas y Filtrado Operativo**:
   - Soporta filtrado contextual por pestañas de la interfaz:
     - `todos`: devuelve el universo completo de reservas del usuario.
     - `historial`: reservas activas o exitosamente completadas (`Iniciada`, `Pendiente de Pago`, `Reservada`, `En Navegación`, `Completada`).
     - `canceladas`: reservas terminadas prematuramente (`Cancelada`, `Expirada`, `Pago Fallido`).
4. **Regla de Negocio "Sin Dinero" (Inmutabilidad Financiera)**:
   - El monto total expuesto en cada reserva es el valor original congelado al momento de su liquidación por Módulo 3 (`FR-004`).
   - Módulo 2 **no calcula deducciones, penalidades ni reembolsos en reservas canceladas** (`FR-004`, `SC-001`). Los sub-estados contractuales (`Flexible`, `Moderado`, `Tardío`, `Por Propietario`, `Por Inasistencia`) se exponen como contexto cualitativo sin alterar el monto total original.
5. **Diferenciación Semántica de Vistas**:
   - Para el **Arrendatario**: expone datos de la embarcación, fechas, total congelado y etiquetas contextuales de reembolso gestionado por M3 (`FR-010`).
   - Para el **Propietario**: expone el nombre del arrendatario titular, cantidad de pasajeros y fechas para la coordinación logística de muelle (`FR-009`).
6. **Manejo de Estado Vacío (Empty State)**:
   - Cuando el usuario no cuenta con reservas asociadas, devuelve un arreglo vacío `[]` con los metadatos de paginación en ceros (`total_elementos: 0`), permitiendo a la UI renderizar estados informativos amigables.

---

## 3. Definición del Endpoint

- **Método HTTP**: `GET`
- **Ruta**: `/api/v1/reservas`
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
| `filtro` | String (Enum) | No | `todos` | Filtro por pestaña: `"todos"`, `"historial"`, `"canceladas"`. |

---

## 4. Estructura de Datos (Schema JSON)

### 4.1 Cuerpo de Respuesta Exitosa (`200 OK`)

```json
{
  "paginacion": {
    "pagina": 1,
    "tamano_pagina": 10,
    "total_elementos": 12,
    "total_paginas": 2
  },
  "filtro_aplicado": "todos | historial | canceladas",
  "rol_usuario": "Arrendatario | Propietario",
  "titulo_vista": "Mis reservas | Reservas recibidas",
  "reservas": [
    {
      "reserva_id": "c1f7a8b2-5e4d-4c3b-8a1e-9f0a2b3c4d5e",
      "codigo_reserva": "#RS-4492",
      "estado": "Reservada",
      "sub_estado": null,
      "insignia_estado": {
        "texto": "Confirmada",
        "color_semantico": "verde",
        "descripcion": "Pago confirmado y lista para zarpe"
      },
      "embarcacion": {
        "embarcacion_id": "b3e2a1d0-4f5c-6b7a-8e9f-0a1b2c3d4e5f",
        "nombre": "Catamarán Sea Breeze",
        "imagen_url": "https://cdn.seashare.com/images/boats/sea-breeze-main.jpg",
        "puerto": "Marina Santa Marta"
      },
      "fechas": {
        "fecha_inicio": "2026-11-20T09:00:00-05:00",
        "fecha_fin": "2026-11-22T18:00:00-05:00",
        "duracion_dias": 3,
        "duracion_noches": 2
      },
      "pasajeros": 4,
      "precio_total": {
        "monto": 3450000.00,
        "moneda": "COP",
        "es_congelado": true
      },
      "arrendatario": {
        "arrendatario_id": "u1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
        "nombre": "Laura Gómez"
      },
      "propietario": {
        "propietario_id": "p9a8b7c6-d5e4-3f2a-1b0c-9d8e7f6a5b4c",
        "nombre": "Carlos Mendoza"
      },
      "etiqueta_contextual": null,
      "url_detalle": "/api/v1/reservas/c1f7a8b2-5e4d-4c3b-8a1e-9f0a2b3c4d5e",
      "created_at": "2026-10-09T10:00:00-05:00"
    }
  ]
}
```

#### Descripción de Campos de Salida

| Campo | Tipo | Nulabilidad | Descripción |
| :--- | :--- | :--- | :--- |
| `paginacion` | Objeto | No nulo | Objeto envoltorio de control de paginación (`pagina`, `tamano_pagina`, `total_elementos`, `total_paginas`). |
| `filtro_aplicado` | String | No nulo | Criterio de filtro activo (`"todos"`, `"historial"`, `"canceladas"`). |
| `rol_usuario` | String | No nulo | Rol con el que se resolvió la consulta (`"Arrendatario"` o `"Propietario"`). |
| `titulo_vista` | String | No nulo | Título semántico según rol (`"Mis reservas"` o `"Reservas recibidas"`). |
| `reservas` | Array | No nulo | Lista de objetos de reserva. Vacía `[]` si no hay resultados. |
| `reservas[].reserva_id` | String (UUID) | No nulo | Identificador universal único de la reserva. |
| `reservas[].codigo_reserva` | String | No nulo | Código amigable de referencia náutica (ej. `"#RS-4492"`). |
| `reservas[].estado` | String (Enum) | No nulo | Estado operativo principal (`"Iniciada"`, `"Pendiente de Pago"`, `"Reservada"`, `"En Navegación"`, `"Completada"`, `"Cancelada"`, `"Expirada"`, `"Pago Fallido"`). |
| `reservas[].sub_estado` | String (Enum) | Nulo condicional | Sub-clasificación contractual en reservas canceladas (`"Flexible"`, `"Moderado"`, `"Tardío"`, `"Por Propietario"`, `"Por Inasistencia"`). Nulo en otros estados. |
| `reservas[].insignia_estado` | Objeto | No nulo | Metadatos visuales para badges (texto legible y color semántico: `verde`, `amarillo`, `gris`, `rojo`). |
| `reservas[].embarcacion` | Objeto | No nulo | Datos referenciales mínimos de la embarcación (ID, nombre, imagen y puerto). |
| `reservas[].fechas` | Objeto | No nulo | Rango temporal pactado, días y noches de navegación. |
| `reservas[].pasajeros` | Entero | No nulo | Cantidad de ocupantes autorizados para el viaje. |
| `reservas[].precio_total` | Objeto | No nulo | Monto consolidado original provisto por M3 al crear/pagar la reserva. **No recalculado en canceladas**. |
| `reservas[].arrendatario` | Objeto | No nulo | Identificador y nombre del arrendatario titular. |
| `reservas[].propietario` | Objeto | No nulo | Identificador y nombre del propietario anfitrión. |
| `reservas[].etiqueta_contextual`| String | Nulo condicional | Mensaje aclaratorio de políticas para el Arrendatario (ej. `"Reembolso gestionado por Módulo 3"`). |
| `reservas[].url_detalle` | String | No nulo | Ruta relativa para invocar `Ver detalle de reserva` (`CU-21`). |
| `reservas[].created_at` | String (ISO 8601) | No nulo | Marca de tiempo de creación de la reserva. |

---

## 5. Ejemplos de Petición y Respuesta

### Ejemplo 1: Consulta exitosa como Arrendatario (Pestaña "todos", página 1)

#### Petición HTTP (`curl`)
```bash
curl -X GET "https://api.seashare.com/api/v1/reservas?page=1&limit=10&filtro=todos" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Accept: application/json"
```

#### Respuesta Exitosa (`200 OK`)
```json
{
  "paginacion": {
    "pagina": 1,
    "tamano_pagina": 10,
    "total_elementos": 3,
    "total_paginas": 1
  },
  "filtro_aplicado": "todos",
  "rol_usuario": "Arrendatario",
  "titulo_vista": "Mis reservas",
  "reservas": [
    {
      "reserva_id": "c1f7a8b2-5e4d-4c3b-8a1e-9f0a2b3c4d5e",
      "codigo_reserva": "#RS-4492",
      "estado": "Reservada",
      "sub_estado": null,
      "insignia_estado": {
        "texto": "Confirmada",
        "color_semantico": "verde",
        "descripcion": "Reserva lista para zarpe"
      },
      "embarcacion": {
        "embarcacion_id": "b3e2a1d0-4f5c-6b7a-8e9f-0a1b2c3d4e5f",
        "nombre": "Catamarán Sea Breeze",
        "imagen_url": "https://cdn.seashare.com/images/boats/sea-breeze.jpg",
        "puerto": "Marina Santa Marta"
      },
      "fechas": {
        "fecha_inicio": "2026-11-20T09:00:00-05:00",
        "fecha_fin": "2026-11-22T18:00:00-05:00",
        "duracion_dias": 3,
        "duracion_noches": 2
      },
      "pasajeros": 4,
      "precio_total": {
        "monto": 3450000.00,
        "moneda": "COP",
        "es_congelado": true
      },
      "arrendatario": {
        "arrendatario_id": "u1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
        "nombre": "Laura Gómez"
      },
      "propietario": {
        "propietario_id": "p9a8b7c6-d5e4-3f2a-1b0c-9d8e7f6a5b4c",
        "nombre": "Carlos Mendoza"
      },
      "etiqueta_contextual": null,
      "url_detalle": "/api/v1/reservas/c1f7a8b2-5e4d-4c3b-8a1e-9f0a2b3c4d5e",
      "created_at": "2026-10-09T10:00:00-05:00"
    },
    {
      "reserva_id": "d2e3f4a5-6b7c-8d9e-0f1a-2b3c4d5e6f7a",
      "codigo_reserva": "#RS-3810",
      "estado": "Cancelada",
      "sub_estado": "Flexible",
      "insignia_estado": {
        "texto": "Cancelada",
        "color_semantico": "rojo",
        "descripcion": "Cancelada voluntariamente con anticipación > 72h"
      },
      "embarcacion": {
        "embarcacion_id": "e4f5a6b7-8c9d-0e1f-2a3b-4c5d6e7f8a9b",
        "nombre": "Yate Poseidón",
        "imagen_url": "https://cdn.seashare.com/images/boats/poseidon.jpg",
        "puerto": "Club Náutico Cartagena"
      },
      "fechas": {
        "fecha_inicio": "2026-10-15T08:00:00-05:00",
        "fecha_fin": "2026-10-15T17:00:00-05:00",
        "duracion_dias": 1,
        "duracion_noches": 0
      },
      "pasajeros": 6,
      "precio_total": {
        "monto": 2100000.00,
        "moneda": "COP",
        "es_congelado": true
      },
      "arrendatario": {
        "arrendatario_id": "u1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
        "nombre": "Laura Gómez"
      },
      "propietario": {
        "propietario_id": "p8b7a6c5-d4e3-2f1a-0b9c-8d7e6f5a4b3c",
        "nombre": "Andrés Morales"
      },
      "etiqueta_contextual": "Cancelada - Ventana Flexible / Reembolso gestionado por Módulo 3",
      "url_detalle": "/api/v1/reservas/d2e3f4a5-6b7c-8d9e-0f1a-2b3c4d5e6f7a",
      "created_at": "2026-10-01T14:20:00-05:00"
    },
    {
      "reserva_id": "f5a6b7c8-9d0e-1f2a-3b4c-5d6e7f8a9b0c",
      "codigo_reserva": "#RS-2950",
      "estado": "Completada",
      "sub_estado": null,
      "insignia_estado": {
        "texto": "Completada",
        "color_semantico": "verde",
        "descripcion": "Viaje finalizado exitosamente"
      },
      "embarcacion": {
        "embarcacion_id": "b3e2a1d0-4f5c-6b7a-8e9f-0a1b2c3d4e5f",
        "nombre": "Catamarán Sea Breeze",
        "imagen_url": "https://cdn.seashare.com/images/boats/sea-breeze.jpg",
        "puerto": "Marina Santa Marta"
      },
      "fechas": {
        "fecha_inicio": "2026-09-10T10:00:00-05:00",
        "fecha_fin": "2026-09-11T16:00:00-05:00",
        "duracion_dias": 2,
        "duracion_noches": 1
      },
      "pasajeros": 4,
      "precio_total": {
        "monto": 2300000.00,
        "moneda": "COP",
        "es_congelado": true
      },
      "arrendatario": {
        "arrendatario_id": "u1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
        "nombre": "Laura Gómez"
      },
      "propietario": {
        "propietario_id": "p9a8b7c6-d5e4-3f2a-1b0c-9d8e7f6a5b4c",
        "nombre": "Carlos Mendoza"
      },
      "etiqueta_contextual": "Depósito sin disputa / Reembolso gestionado por Módulo 3",
      "url_detalle": "/api/v1/reservas/f5a6b7c8-9d0e-1f2a-3b4c-5d6e7f8a9b0c",
      "created_at": "2026-09-02T11:00:00-05:00"
    }
  ]
}
```

---

### Ejemplo 2: Consulta exitosa como Propietario (Pestaña "historial", reservas recibidas)

#### Petición HTTP (`curl`)
```bash
curl -X GET "https://api.seashare.com/api/v1/reservas?page=1&limit=10&filtro=historial" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Accept: application/json"
```

#### Respuesta Exitosa (`200 OK`)
```json
{
  "paginacion": {
    "pagina": 1,
    "tamano_pagina": 10,
    "total_elementos": 2,
    "total_paginas": 1
  },
  "filtro_aplicado": "historial",
  "rol_usuario": "Propietario",
  "titulo_vista": "Reservas recibidas",
  "reservas": [
    {
      "reserva_id": "c1f7a8b2-5e4d-4c3b-8a1e-9f0a2b3c4d5e",
      "codigo_reserva": "#RS-4492",
      "estado": "Reservada",
      "sub_estado": null,
      "insignia_estado": {
        "texto": "Confirmada",
        "color_semantico": "verde",
        "descripcion": "Reserva lista para zarpe"
      },
      "embarcacion": {
        "embarcacion_id": "b3e2a1d0-4f5c-6b7a-8e9f-0a1b2c3d4e5f",
        "nombre": "Catamarán Sea Breeze",
        "imagen_url": "https://cdn.seashare.com/images/boats/sea-breeze.jpg",
        "puerto": "Marina Santa Marta"
      },
      "fechas": {
        "fecha_inicio": "2026-11-20T09:00:00-05:00",
        "fecha_fin": "2026-11-22T18:00:00-05:00",
        "duracion_dias": 3,
        "duracion_noches": 2
      },
      "pasajeros": 4,
      "precio_total": {
        "monto": 3450000.00,
        "moneda": "COP",
        "es_congelado": true
      },
      "arrendatario": {
        "arrendatario_id": "u1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
        "nombre": "Laura Gómez"
      },
      "propietario": {
        "propietario_id": "p9a8b7c6-d5e4-3f2a-1b0c-9d8e7f6a5b4c",
        "nombre": "Carlos Mendoza"
      },
      "etiqueta_contextual": null,
      "url_detalle": "/api/v1/reservas/c1f7a8b2-5e4d-4c3b-8a1e-9f0a2b3c4d5e",
      "created_at": "2026-10-09T10:00:00-05:00"
    },
    {
      "reserva_id": "a9b8c7d6-e5f4-3a2b-1c0d-9e8f7a6b5c4d",
      "codigo_reserva": "#RS-4480",
      "estado": "En Navegación",
      "sub_estado": null,
      "insignia_estado": {
        "texto": "En navegación",
        "color_semantico": "amarillo",
        "descripcion": "Viaje náutico en curso"
      },
      "embarcacion": {
        "embarcacion_id": "b3e2a1d0-4f5c-6b7a-8e9f-0a1b2c3d4e5f",
        "nombre": "Catamarán Sea Breeze",
        "imagen_url": "https://cdn.seashare.com/images/boats/sea-breeze.jpg",
        "puerto": "Marina Santa Marta"
      },
      "fechas": {
        "fecha_inicio": "2026-10-09T08:00:00-05:00",
        "fecha_fin": "2026-10-09T17:00:00-05:00",
        "duracion_dias": 1,
        "duracion_noches": 0
      },
      "pasajeros": 6,
      "precio_total": {
        "monto": 1800000.00,
        "moneda": "COP",
        "es_congelado": true
      },
      "arrendatario": {
        "arrendatario_id": "u9z8y7x6-w5v4-3u2t-1s0r-9q8p7o6n5m4l",
        "nombre": "Mateo Rossi"
      },
      "propietario": {
        "propietario_id": "p9a8b7c6-d5e4-3f2a-1b0c-9d8e7f6a5b4c",
        "nombre": "Carlos Mendoza"
      },
      "etiqueta_contextual": null,
      "url_detalle": "/api/v1/reservas/a9b8c7d6-e5f4-3a2b-1c0d-9e8f7a6b5c4d",
      "created_at": "2026-10-08T16:00:00-05:00"
    }
  ]
}
```

---

### Ejemplo 3: Consulta sin resultados (Empty State)

#### Petición HTTP (`curl`)
```bash
curl -X GET "https://api.seashare.com/api/v1/reservas?page=1&limit=10&filtro=canceladas" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Accept: application/json"
```

#### Respuesta Exitosa (`200 OK`)
```json
{
  "paginacion": {
    "pagina": 1,
    "tamano_pagina": 10,
    "total_elementos": 0,
    "total_paginas": 0
  },
  "filtro_aplicado": "canceladas",
  "rol_usuario": "Arrendatario",
  "titulo_vista": "Mis reservas",
  "reservas": []
}
```

---

## 6. Respuestas de Error y Códigos HTTP

Todos los errores retornan el sobre estándar con `codigo` y `mensaje`:

```json
{
  "codigo": "CODIGO_ERROR",
  "mensaje": "Descripción detallada del motivo de rechazo."
}
```

| Código HTTP | Código Interno (`codigo`) | Causa / Condición de Disparo | Cuerpo de Respuesta de Ejemplo |
| :--- | :--- | :--- | :--- |
| **`400 Bad Request`** | `INVALID_QUERY_PARAMS` | Parámetros de paginación inválidos (`page < 1`, `limit < 1`, `limit > 50`, o filtro no soportado). | `{"codigo": "INVALID_QUERY_PARAMS", "mensaje": "El parámetro 'page' debe ser mayor o igual a 1 y 'limit' debe encontrarse entre 1 y 50."}` |
| **`401 Unauthorized`** | `AUTH_TOKEN_MISSING_OR_INVALID` | El header `Authorization` no fue provisto o el token JWT expiró. | `{"codigo": "AUTH_TOKEN_MISSING_OR_INVALID", "mensaje": "Token de autenticación ausente o inválido."}` |
| **`403 Forbidden`** | `FORBIDDEN_INVALID_ROLE` | El usuario autenticado posee un rol no autorizado para consultar este catálogo personal. | `{"codigo": "FORBIDDEN_INVALID_ROLE", "mensaje": "El perfil de usuario no cuenta con autorización para acceder al panel de reservas."}` |
| **`500 Internal Server Error`** | `INTERNAL_SERVER_ERROR` | Fallo no controlado de la base de datos de Módulo 2. | `{"codigo": "INTERNAL_SERVER_ERROR", "mensaje": "Error interno del servidor al recuperar el listado de reservas."}` |

---

## 7. Notas Transversales y Reglas de Negocio Arquitecturales

### 7.1 Regla de Oro "Sin Dinero" (Zero Financial Calculations)
En estricto apego a `FR-004` y `SC-001`, Módulo 2 se abstiene por completo de calcular devoluciones, porcentajes o deducciones sobre reservas `Canceladas`.
- En una reserva en estado `Cancelada` con sub-estado `Flexible`, `Moderado` o `Tardío`, el campo `precio_total.monto` exhibe literalmente el monto total que fue acordado y congelado al inicio.
- Cualquier valor derivado de liquidación, compensación o indemnización es propiedad de Módulo 3 y se gestionará en los reportes contables independientes de dicho módulo.

### 7.2 Aislamiento de Datos por Identidad JWT
El controlador extrae directamente el ID del usuario (`sub` o `user_id`) y su rol del token JWT verificado en el contexto de seguridad de Spring.
- Un usuario no puede pasar un parámetro `usuario_id` en la query string para usurpar consultas ajenas; el filtro en base de datos (`WHERE arrendatario_id = :id` o `WHERE propietario_id = :id`) es forzado por el backend a nivel de repositorio.

### 7.3 Orden Determinista y Paginación Eficiente
Las consultas se ejecutan con índices compuestos en base de datos (`arrendatario_id, created_at DESC` y `propietario_id, created_at DESC`), garantizando respuestas ágiles por debajo de 50 ms aun cuando el usuario acumule cientos de registros históricos.

---

## 8. Trazabilidad de Requisitos

| Requisito Funcional / Criterio | Descripción en Spec | Cobertura en este Contrato |
| :--- | :--- | :--- |
| **FR-001** | Permitir a Arrendatario y Propietario acceder a su lista de reservas. | Soportado para ambos perfiles mediante token JWT y discriminación de rol. |
| **FR-002** | Filtrar registros en BD según `arrendatario_id` o `propietario_id`. | Garantizado en lógica de backend descrita en sección 2 y 7.2. |
| **FR-003** | Presentar embarcación, rango de fechas, estado vigente y precio total. | Presente en cada elemento del arreglo `reservas[]`. |
| **FR-004** | Precio total expuesto debe ser el monto original congelado; cero cálculos en canceladas. | Objeto `precio_total` con `es_congelado: true`. Cero cálculos de penalidad o deducción. |
| **FR-005** | Mostrar sub-estados en reservas canceladas (`Por Propietario`, `Por Inasistencia`, etc.). | Campo `sub_estado` e `insignia_estado` contextual. |
| **FR-006** | Enlace para invocar la vista profunda (`Ver detalle de reserva`). | Atributo `url_detalle` que apunta a `/api/v1/reservas/{reservaId}`. |
| **FR-007** | Encabezados diferenciados ("Mis reservas" / "Reservas recibidas") y filtros de pestañas. | Campos `titulo_vista` y parámetro `filtro` (`todos`, `historial`, `canceladas`). |
| **FR-008** | Badges de estado con colores semánticos (`verde`, `amarillo`, `gris`, `rojo`). | Objeto `insignia_estado` con `color_semantico`. |
| **FR-009** | En Propietario incluir nombre de arrendatario y cantidad de pasajeros. | Campos `arrendatario.nombre` y `pasajeros` incluidos en el schema. |
| **FR-010** | En Arrendatario incluir insignias cualitativas sin montos numéricos de reembolso. | Campo `etiqueta_contextual` con texto cualitativo (ej. "Reembolso gestionado por Módulo 3"). |
| **FR-011** | Enlace explícito para ver detalles junto al monto total. | Cubierto por `url_detalle` para renderizado en frontend. |
| **FR-012** | Paginación fijando límite de 10 reservas por página ordenadas por `created_at DESC`. | Paginación `limit=10` por defecto y ordenación cronológica descendente. |
| **SC-001** | Cero (0%) cálculos de reembolsos o penalidades realizados por Módulo 2. | Esquema estricto sin campos monetarios calculados. |
| **SC-002** | Aislamiento total de datos: 100% de efectividad impidiendo visualización cruzada ajena. | Garantizado por filtrado forzado a través del token JWT del solicitante. |
