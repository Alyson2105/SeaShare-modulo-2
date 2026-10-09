# Plan Técnico: Ver Detalle de Embarcación (CU-19)

**Módulo**: Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones
**Spec de referencia**: `features/CU-19/spec.md`
**Fecha**: 2026-09-28

Este caso de uso expone dos operaciones REST independientes, porque atiende dos momentos distintos del flujo (FR-001 a FR-010 del spec):

1. **Obtener la ficha técnica de la embarcación** (datos estáticos de Módulo 1, FR-002/FR-009/FR-010).
2. **Solicitar la cotización individual** para un rango de fechas y una cantidad de pasajeros específicos (FR-004/FR-005/FR-006).

Se separan en dos *endpoints* en vez de uno solo porque tienen ciclos de vida distintos: el primero se consulta una sola vez al entrar al detalle; el segundo se vuelve a consultar cada vez que el Arrendatario cambia fechas o pasajeros (ver Edge Case "Cambio dinámico de parámetros" del spec).

---

## Endpoint 1 — Obtener ficha técnica de la embarcación

### Método HTTP y URL

```
GET /api/v1/embarcaciones/{embarcacionId}
```

### Elementos de la Petición (Request)

**Headers**:

| Nombre | Obligatorio | Descripción |
|---|---|---|
| `Authorization` | Sí | `Bearer <token>` — JWT del Arrendatario autenticado |
| `Accept` | No | `application/json` (valor por defecto si se omite) |

**Query Parameters**: No tiene.

**Path Parameters**:

| Nombre | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `embarcacionId` | string (UUID) | Sí | Identificador de la embarcación, recibido desde `Buscar embarcaciones disponibles` (`<<extend>>`, FR-001) |

**Body**: No tiene (operación `GET`).

### Elementos de la Respuesta (Response)

**Código de estado HTTP (éxito)**: `200 OK`

**Contrato de respuesta**:

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
    "longitud": "number"
  },
  "amenidades": ["string"],
  "propietario": {
    "nombreValidado": "string",
    "verificado": "boolean"
  }
}
```

*(Campos obtenidos íntegramente de `Proveer información embarcación` (`<<include>>`, FR-002/FR-009/FR-010) — Módulo 2 no los persiste ni los transforma, FR-002/SC-003).*

**Headers de respuesta**:

| Nombre | Valor |
|---|---|
| `Content-Type` | `application/json` |
| `Cache-Control` | `no-store` (los datos deben consultarse en tiempo real, no cachearse localmente) |

### Manejo de Errores

| Código | Caso | Cuerpo de respuesta (ejemplo) |
|---|---|---|
| `400 Bad Request` | `embarcacionId` no tiene formato de UUID válido | `{ "codigo": "ID_INVALIDO", "mensaje": "El identificador de embarcación no es válido" }` |
| `401 Unauthorized` | Token ausente, expirado o inválido | `{ "codigo": "NO_AUTENTICADO", "mensaje": "Token inválido o ausente" }` |
| `403 Forbidden` | El perfil autenticado no es Arrendatario | `{ "codigo": "PERFIL_NO_AUTORIZADO", "mensaje": "Este recurso requiere perfil Arrendatario" }` |
| `404 Not Found` | La embarcación no existe en Módulo 1, o Módulo 1 devuelve datos corruptos (ej. capacidad máxima = 0) — ver Edge Case "Inconsistencia de datos desde Módulo 1" | `{ "codigo": "EMBARCACION_NO_DISPONIBLE", "mensaje": "La embarcación no está disponible para mostrar su detalle" }` |
| `503 Service Unavailable` | Módulo 1 no responde o agota el tiempo de espera (*fail-safe*, no se asume ningún dato) | `{ "codigo": "SERVICIO_FLOTA_NO_DISPONIBLE", "mensaje": "No se pudo consultar la información de la embarcación en este momento" }` |
| `500 Internal Server Error` | Error inesperado no controlado | `{ "codigo": "ERROR_INTERNO", "mensaje": "Ocurrió un error inesperado" }` |

### Paginación

No aplica — devuelve un único recurso (una embarcación), no un conjunto de datos.

### Seguridad y Perfiles

- Requiere JWT válido emitido por el módulo de seguridad/identidad del sistema (fuera del alcance de Módulo 2).
- Perfil autorizado: **Arrendatario**.
- El *endpoint* valida el `claim` de rol en el token antes de procesar la solicitud (la autenticación en sí la resuelve el módulo de seguridad, pero el servicio verifica el perfil antes de responder, como pide el profesor).
- No se expone ningún dato financiero en esta respuesta — cumple la regla "sin dinero" (FR-006 del spec).

---

## Endpoint 2 — Solicitar cotización individual para el detalle

### Método HTTP y URL

```
GET /api/v1/embarcaciones/{embarcacionId}/cotizacion
```

### Elementos de la Petición (Request)

**Headers**:

| Nombre | Obligatorio | Descripción |
|---|---|---|
| `Authorization` | Sí | `Bearer <token>` — JWT del Arrendatario autenticado |

**Query Parameters**:

| Nombre | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `fechaInicio` | string (ISO 8601, `YYYY-MM-DD`) | Sí | Fecha de inicio del rango seleccionado |
| `fechaFin` | string (ISO 8601, `YYYY-MM-DD`) | Sí | Fecha de fin del rango seleccionado |
| `pasajeros` | number (entero) | Sí | Cantidad de pasajeros ingresada por el Arrendatario |

**Path Parameters**:

| Nombre | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `embarcacionId` | string (UUID) | Sí | Identificador de la embarcación |

**Body**: No tiene (operación `GET`, los criterios van en `Query Parameters`).

### Elementos de la Respuesta (Response)

**Código de estado HTTP (éxito)**: `200 OK`

**Contrato de respuesta**:

```json
{
  "embarcacionId": "string (UUID)",
  "fechaInicio": "string (ISO 8601)",
  "fechaFin": "string (ISO 8601)",
  "diasTotales": "number (entero)",
  "pasajeros": "number (entero)",
  "montoTotal": "number (BigDecimal/string según convención del equipo)",
  "moneda": "string (ej. COP)",
  "cotizacionId": "string",
  "cotizacionExpiraEn": "string (ISO 8601 — fin del temporizador de la cotización, ver Edge Case 'Expiración del Temporizador')"
}
```

*(El `montoTotal` viene íntegro de `Proveer información cotización de reserva` en modo individual (`<<include>>`, FR-005) — Módulo 2 no divide, no calcula tarifa por noche ni manipula el valor, FR-006/SC-001).*

**Headers de respuesta**:

| Nombre | Valor |
|---|---|
| `Content-Type` | `application/json` |
| `Cache-Control` | `no-store` |

### Manejo de Errores

| Código | Caso | Cuerpo de respuesta (ejemplo) |
|---|---|---|
| `400 Bad Request` | `fechaInicio`/`fechaFin` con formato inválido, `fechaFin` anterior a `fechaInicio`, o `pasajeros` no numérico/negativo | `{ "codigo": "PARAMETROS_INVALIDOS", "mensaje": "Las fechas o la cantidad de pasajeros no son válidas" }` |
| `409 Conflict` | `pasajeros` excede la capacidad máxima de la embarcación (revalidación de backend, además de la validación de interfaz de FR-004) | `{ "codigo": "CAPACIDAD_EXCEDIDA", "mensaje": "La cantidad de pasajeros supera la capacidad máxima de la embarcación" }` |
| `401 Unauthorized` | Token ausente, expirado o inválido | `{ "codigo": "NO_AUTENTICADO", ... }` |
| `403 Forbidden` | Perfil no autorizado | `{ "codigo": "PERFIL_NO_AUTORIZADO", ... }` |
| `404 Not Found` | La embarcación no existe | `{ "codigo": "EMBARCACION_NO_DISPONIBLE", ... }` |
| `503 Service Unavailable` | Módulo 3 no responde o falla al cotizar (Edge Case "Falla en la obtención de la cotización") | `{ "codigo": "COTIZACION_NO_DISPONIBLE", "mensaje": "Cotización temporalmente no disponible" }` |
| `500 Internal Server Error` | Error inesperado no controlado | `{ "codigo": "ERROR_INTERNO", ... }` |

### Paginación

No aplica — devuelve un único resultado de cotización, no un conjunto de datos.

### Seguridad y Perfiles

- Mismas reglas que el Endpoint 1: JWT válido, perfil **Arrendatario**, verificación de `claim` de rol antes de procesar.
- Esta operación SÍ debe revalidar la capacidad máxima en el backend (no confiar solo en la validación de interfaz de FR-004), ya que es la última barrera antes de habilitar `Iniciar reserva` (FR-007).

---

## Notas transversales a ambos *endpoints*

- Ninguno de los dos persiste datos en la base de datos de Módulo 2 (SC-003 del spec) — son *pass-through* hacia Módulo 1 y Módulo 3 respectivamente.
- El cliente (frontend) es responsable de disparar el Endpoint 2 nuevamente cada vez que el usuario cambie fechas o pasajeros (Edge Case "Cambio dinámico de parámetros") — Módulo 2 no mantiene estado de sesión entre ambas llamadas.
- El objeto "Selección Temporal de Viaje" (Key Entity del spec) vive solo en memoria del lado del cliente hasta que se invoque `Iniciar reserva`; no hay un *endpoint* de persistencia aquí.
