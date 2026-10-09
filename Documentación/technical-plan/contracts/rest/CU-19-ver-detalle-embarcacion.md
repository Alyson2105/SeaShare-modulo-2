# Contrato REST: Ver Detalle de Embarcación (CU-19)

**Módulo**: Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones  
**Spec de referencia**: [`Documentación/features/CU-19-ver-detalle-embarcacion/spec.md`](../../features/CU-19-ver-detalle-embarcacion/spec.md)  
**Fecha**: 2026-10-09  

Este caso de uso expone dos operaciones REST independientes para satisfacer dos momentos diferenciados en el ciclo de interacción del usuario (FR-001 a FR-010):

1. **Obtener la ficha técnica de la embarcación**: consulta estática hacia Módulo 1 (FR-002, FR-009, FR-010) respaldada por el contrato externo [`m1-consultar-informacion-embarcacion.md`](../external/m1-consultar-informacion-embarcacion.md).
2. **Solicitar la cotización individual para el detalle**: consulta dinámica hacia Módulo 3 para un rango de fechas y cantidad de pasajeros específicos (FR-004, FR-005, FR-006) respaldada por el contrato externo [`m3-estimacion-individual.md`](../external/m3-estimacion-individual.md).

Ambas operaciones se segregan debido a que poseen ciclos de vida desacoplados: la ficha técnica se consulta una sola vez al ingresar a la vista, mientras que la cotización se reejecuta ante cualquier cambio dinámico de fechas o pasajeros introducido por el Arrendatario (Edge Case "Cambio dinámico de parámetros"). Este caso de uso es extendido desde [`CU-01-buscar-embarcaciones-disponibles.md`](CU-01-buscar-embarcaciones-disponibles.md) (`<<extend>>`, FR-001) y constituye el punto previo que habilita la extensión hacia `Iniciar reserva` (`<<extend>>`, FR-007).

---

## Endpoint 1 — Obtener ficha técnica de la embarcación

### Método HTTP y URL

```http
GET /api/v1/embarcaciones/{embarcacionId}
```

### Elementos de la Petición (Request)

**Headers**:

| Nombre | Obligatorio | Descripción |
|---|---|---|
| `Authorization` | Sí | `Bearer <token>` — JWT válido del Arrendatario autenticado |
| `Accept` | No | `application/json` (predeterminado si se omite) |

**Path Parameters**:

| Nombre | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `embarcacionId` | string (UUID) | Sí | Identificador único de la embarcación seleccionada en el catálogo (FR-001) |

**Query Parameters**: No tiene.

**Body**: No tiene (operación `GET`).

---

### Elementos de la Respuesta (Response)

**Código de estado HTTP (éxito)**: `200 OK`

**Contrato de respuesta tipado**:

```json
{
  "embarcacionId": "string (UUID)",
  "nombre": "string",
  "tipoNavegacion": "string",
  "capacidadMaxima": "number (entero)",
  "esloraPies": "number",
  "numeroCamarotes": "number (entero)",
  "capitanIncluido": "boolean",
  "puerto": {
    "nombre": "string",
    "latitud": "number",
    "longitud": "number",
    "zona_horaria": "string"
  },
  "amenidades": [
    "string"
  ],
  "propietario": {
    "nombreValidado": "string",
    "verificado": "boolean"
  }
}
```

*(Campos obtenidos íntegramente de Módulo 1 vía `Proveer información embarcación` — Módulo 2 no los persiste ni los altera, cumpliendo FR-002, SC-003).*

**Headers de respuesta**:

| Nombre | Valor |
|---|---|
| `Content-Type` | `application/json` |
| `Cache-Control` | `no-store` (información consultada en tiempo real) |

---

### Ejemplo de Petición y Respuesta Exitosa (Endpoint 1)

**Petición `curl`**:

```bash
curl -X GET "https://api.seashare.com/api/v1/embarcaciones/d3b07384-d113-49cd-a5d6-812e9bcfc101" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMTIyMzMzNC00NDU1LTY2NzctODg5OS1hYWJiY2NkZGVlZmYiLCJyb2wiOiJBcnJlbmRhdGFyaW8iLCJpYXQiOjE3OTE1NDAwMDB9.sampleToken" \
  -H "Accept: application/json"
```

**Respuesta (`200 OK`)**:

```json
{
  "embarcacionId": "d3b07384-d113-49cd-a5d6-812e9bcfc101",
  "nombre": "Yate Tayrona Sea Breeze",
  "tipoNavegacion": "Yate a Motor",
  "capacidadMaxima": 12,
  "esloraPies": 48.5,
  "numeroCamarotes": 3,
  "capitanIncluido": true,
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
    "nombreValidado": "Inversiones Náuticas del Caribe S.A.S.",
    "verificado": true
  }
}
```

---

### Manejo de Errores (Endpoint 1)

| Código | Caso | Cuerpo de respuesta (ejemplo) |
|---|---|---|
| `400 Bad Request` | `embarcacionId` no tiene estructura de UUID válido | `{ "codigo": "ID_INVALIDO", "mensaje": "El identificador de embarcación proporcionado no es un UUID válido" }` |
| `401 Unauthorized` | Token de autorización ausente, corrupto o expirado | `{ "codigo": "NO_AUTENTICADO", "mensaje": "Token de autenticación inválido o ausente" }` |
| `403 Forbidden` | El claim de rol en el token no corresponde a Arrendatario | `{ "codigo": "PERFIL_NO_AUTORIZADO", "mensaje": "Este recurso requiere perfil Arrendatario" }` |
| `404 Not Found` | La embarcación no existe en Módulo 1 o retorna capacidad inválida (capacidad = 0) (Edge Case "Inconsistencia de datos desde Módulo 1") | `{ "codigo": "EMBARCACION_NO_DISPONIBLE", "mensaje": "La embarcación no está disponible para mostrar su detalle" }` |
| `503 Service Unavailable` | Módulo 1 no responde o agota el tiempo de espera (*fail-safe* preventivo) | `{ "codigo": "SERVICIO_FLOTA_NO_DISPONIBLE", "mensaje": "No se pudo consultar la información técnica de la embarcación en este momento" }` |
| `500 Internal Server Error` | Excepción interna no controlada | `{ "codigo": "ERROR_INTERNO", "mensaje": "Ocurrió un error inesperado al recuperar la embarcación" }` |

---

### Paginación (Endpoint 1)

No aplica. Retorna un único recurso identificado unívocamente por su clave primaria.

---

### Seguridad y Perfiles (Endpoint 1)

- Requiere JWT emitido por el servicio de identidad centralizado.
- Perfil autorizado: **Arrendatario**.
- El endpoint valida el claim de rol antes de transferir la consulta a Módulo 1.
- No se expone ningún campo de orden financiero (cumplimiento de la regla "sin dinero", FR-006, SC-001).

---

## Endpoint 2 — Solicitar cotización individual para el detalle

### Método HTTP y URL

```http
GET /api/v1/embarcaciones/{embarcacionId}/cotizacion
```

### Elementos de la Petición (Request)

**Headers**:

| Nombre | Obligatorio | Descripción |
|---|---|---|
| `Authorization` | Sí | `Bearer <token>` — JWT válido del Arrendatario autenticado |
| `Accept` | No | `application/json` (predeterminado si se omite) |

**Path Parameters**:

| Nombre | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `embarcacionId` | string (UUID) | Sí | Identificador de la embarcación a cotizar (FR-001) |

**Query Parameters**:

| Nombre | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `fechaInicio` | string (ISO 8601, `YYYY-MM-DD`) | Sí | Fecha de inicio seleccionada en el calendario (FR-003) |
| `fechaFin` | string (ISO 8601, `YYYY-MM-DD`) | Sí | Fecha de finalización seleccionada en el calendario (FR-003) |
| `pasajeros` | number (entero positivo) | Sí | Cantidad de personas ingresada en el selector (FR-003, FR-004) |

**Body**: No tiene (operación `GET`).

---

### Elementos de la Respuesta (Response)

**Código de estado HTTP (éxito)**: `200 OK`

**Contrato de respuesta tipado**:

```json
{
  "embarcacionId": "string (UUID)",
  "fechaInicio": "string (ISO 8601, YYYY-MM-DD)",
  "fechaFin": "string (ISO 8601, YYYY-MM-DD)",
  "diasTotales": "number (entero)",
  "pasajeros": "number (entero)",
  "montoTotal": "number (BigDecimal/string según convención, literal de M3)",
  "moneda": "string (ej. COP)",
  "advertencia": "string (literal exacto de Módulo 3)",
  "cotizacionId": "string (UUID/referencia de M3)",
  "cotizacionExpiraEn": "string (ISO 8601 timestamp)"
}
```

*(El valor `montoTotal` proviene íntegro de Módulo 3 vía `m3-estimacion-individual` — Módulo 2 jamás calcula divisiones ni exhibe tarifas por noche, FR-006, SC-001. La propiedad `advertencia` preserva textualmente la bandera obligatoria de Módulo 3).*

**Headers de respuesta**:

| Nombre | Valor |
|---|---|
| `Content-Type` | `application/json` |
| `Cache-Control` | `no-store` |

---

### Ejemplo de Petición y Respuesta Exitosa (Endpoint 2)

**Petición `curl`**:

```bash
curl -X GET "https://api.seashare.com/api/v1/embarcaciones/d3b07384-d113-49cd-a5d6-812e9bcfc101/cotizacion?fechaInicio=2026-11-15&fechaFin=2026-11-18&pasajeros=8" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMTIyMzMzNC00NDU1LTY2NzctODg5OS1hYWJiY2NkZGVlZmYiLCJyb2wiOiJBcnJlbmRhdGFyaW8iLCJpYXQiOjE3OTE1NDAwMDB9.sampleToken" \
  -H "Accept: application/json"
```

**Respuesta (`200 OK`)**:

```json
{
  "embarcacionId": "d3b07384-d113-49cd-a5d6-812e9bcfc101",
  "fechaInicio": "2026-11-15",
  "fechaFin": "2026-11-18",
  "diasTotales": 3,
  "pasajeros": 8,
  "montoTotal": 9600000.00,
  "moneda": "COP",
  "advertencia": "Valor estimado. No incluye cargos adicionales ni depósito de seguridad",
  "cotizacionId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
  "cotizacionExpiraEn": "2026-10-09T12:15:00Z"
}
```

---

### Manejo de Errores (Endpoint 2)

| Código | Caso | Cuerpo de respuesta (ejemplo) |
|---|---|---|
| `400 Bad Request` | Parámetros con formato inválido, `fechaFin` menor o igual a `fechaInicio`, fecha en el pasado o `pasajeros` no numérico | `{ "codigo": "PARAMETROS_INVALIDOS", "mensaje": "Las fechas ingresadas o la cantidad de pasajeros no son válidas" }` |
| `401 Unauthorized` | Token de autenticación ausente o expirado | `{ "codigo": "NO_AUTENTICADO", "mensaje": "Token de autenticación inválido o ausente" }` |
| `403 Forbidden` | Perfil de usuario distinto de Arrendatario | `{ "codigo": "PERFIL_NO_AUTORIZADO", "mensaje": "Este recurso requiere perfil Arrendatario" }` |
| `404 Not Found` | La embarcación indicada no existe en el catálogo | `{ "codigo": "EMBARCACION_NO_DISPONIBLE", "mensaje": "La embarcación solicitada no se encuentra registrada" }` |
| `409 Conflict` | La cantidad de pasajeros ingresada supera la capacidad máxima del activo (revalidación mandatoria de backend, FR-004) | `{ "codigo": "CAPACIDAD_EXCEDIDA", "mensaje": "La cantidad de pasajeros (8) supera la capacidad máxima de la embarcación (6)" }` |
| `503 Service Unavailable` | Módulo 3 no responde, tiempo agotado o activo sin esquema tarifario configurado (Edge Case "Falla en la obtención de la cotización") | `{ "codigo": "COTIZACION_NO_DISPONIBLE", "mensaje": "Cotización temporalmente no disponible" }` |
| `500 Internal Server Error` | Falla interna no controlada | `{ "codigo": "ERROR_INTERNO", "mensaje": "Ocurrió un error inesperado al procesar la cotización" }` |

---

### Paginación (Endpoint 2)

No aplica. Retorna un único cálculo presupuestal consolidado para los parámetros de viaje remitidos.

---

### Seguridad y Perfiles (Endpoint 2)

- Perfil autorizado: **Arrendatario**.
- Verificación mandatoria de backend: antes de consultar a Módulo 3, este endpoint revalida la capacidad máxima consultando los datos oficiales de Módulo 1, garantizando que ninguna solicitud que viole la capacidad técnica avance hacia el motor de liquidación (FR-004, SC-002).
- Cumplimiento de la regla "Sin dinero": Módulo 2 expone exclusivamente el `montoTotal` provisto por Módulo 3. Se prohíbe terminantemente realizar divisiones locales entre `diasTotales` para proyectar tarifas promedio o por noche (FR-006, SC-001).

---

## Notas Transversales a Ambos Endpoints

- **Ausencia de Persistencia Local**: ninguno de los dos endpoints persiste registros en la base de datos de Módulo 2 (SC-003). Actúan como adaptadores orquestadores hacia Módulo 1 y Módulo 3 respectivamente.
- **Ciclo de Vida sin Estado en Servidor**: Módulo 2 no guarda estado de sesión entre ambas llamadas. El frontend es el encargado de retener en memoria la entidad `Selección Temporal de Viaje` y volver a disparar el Endpoint 2 ante cualquier modificación del usuario en fechas o pasajeros (Edge Case "Cambio dinámico de parámetros").
- **Expiración de la Cotización**: al expirar la ventana de validez indicada en `cotizacionExpiraEn`, el cliente debe solicitar una nueva cotización invocando nuevamente el Endpoint 2 antes de habilitar la acción de `Iniciar reserva` (Edge Case "Expiración del Temporizador").

---

### Trazabilidad FR/SC → Elemento del Contrato

| Requisito / Criterio | Descripción en Spec | Elemento de este Contrato |
|---|---|---|
| **FR-001** | Recepción de identificador desde búsqueda | Path parameter `embarcacionId` en ambos endpoints |
| **FR-002** | Consulta de datos técnicos vía `Proveer información embarcación` | Endpoint 1 respaldado por [`m1-consultar-informacion-embarcacion.md`](../external/m1-consultar-informacion-embarcacion.md) |
| **FR-003** | Selectores de fechas y pasajeros | Query parameters `fechaInicio`, `fechaFin`, `pasajeros` del Endpoint 2 |
| **FR-004** | Validación estricta de capacidad máxima | Manejo de error `409 Conflict` con código `CAPACIDAD_EXCEDIDA` en Endpoint 2 |
| **FR-005** | Cotización individual vía `Proveer información cotización de reserva` | Endpoint 2 respaldado por [`m3-estimacion-individual.md`](../external/m3-estimacion-individual.md) |
| **FR-006** | Regla estricta "Sin dinero" (cero divisiones por noche) | Campo `montoTotal` literal en Endpoint 2; sin campos de desglose de tarifa nocturna |
| **FR-007** | Transición hacia `Iniciar reserva` solo con capacidad válida y cotización exitosa | Código `200 OK` en Endpoint 2 condiciona el avance a CU-02 |
| **FR-008** | Navegación de retorno al catálogo | Metadatos de navegación preservados en el cliente |
| **FR-009** | Ficha técnica con atributos estructurados | Campos de respuesta de Endpoint 1: `capacidadMaxima`, `tipoNavegacion`, `esloraPies`, `numeroCamarotes` |
| **FR-010** | Amenidades y validación del propietario | Campos de respuesta de Endpoint 1: `amenidades`, `propietario` |
| **SC-001** | Cero operaciones matemáticas de división ejecutadas en M2 | Contrato del Endpoint 2 sin cálculos locales derivados |
| **SC-002** | 100% de intentos con exceso de pasajeros bloqueados | Validación de backend reflejada en error `409 CAPACIDAD_EXCEDIDA` |
| **SC-003** | Cero persistencia en BD de M2 de datos de flota | Comportamiento stateless y pass-through documentado |
