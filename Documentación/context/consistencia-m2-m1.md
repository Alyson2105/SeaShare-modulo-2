# Contrato de integración M1–M2

**Versión:** 2.0 · **Fecha:** 2026-11-19
**Módulos:** M1 – Gestión de Flota y Activos P2P · M2 – Operación de Reservas, Tiempos y Cancelaciones
**Estado:** diseño acordado y **normativo**. M1 implementa lo aquí descrito; las dudas se plantean después de aplicarlo.
**Diagnóstico de factibilidad:** `context/consistencia-m1-m2-mantenimiento.md`.

---

## 0. Propósito y propiedad de datos

| Dato | Dueño | Regla |
|---|---|---|
| Reservas, fechas, solapamiento, TTL, FCFS | **M2** | M1 no conoce reservas ni fechas de reserva |
| Estado operativo de la embarcación | **M1** | M2 lo consulta y lo recibe por aviso |
| Mantenimiento y su ventana | **M1** | M2 lo consume para bloquear fechas y cancelar |
| Catálogo e información técnica | **M1** | M2 no lo persiste |

Ningún módulo accede a la base del otro. Toda comunicación es HTTP.

**Regla de oro:** M2 es la autoridad de disponibilidad por fechas; M1 sólo informa impedimentos operativos.

---

## 1. Inventario de conexiones

| ID | Dirección | Método y ruta | Transporte | Hoy | Objetivo |
|---|---|---|---|---|---|
| **C1** | M2 → M1 | `GET /api/v1/vessels/{vessel_id}` | HTTP | Existe | Igual; se completan campos opcionales |
| **C2** | M2 → M1 | `GET /api/v1/vessels?page&size&q` | HTTP | **No existe** | Crear la operación de lote **antes del 2026-11-21** |
| **C3** | M2 → M1 | `GET /api/v1/vessels/{vessel_id}/operational-status` | HTTP | Existe, sin fechas | Añadir rango y ventana de mantenimiento |
| **C4** | M2 → M1 | `POST /api/v1/internal/status-events` | HTTP | Existe, envía `RESERVED` | Dejar de enviar `RESERVED` |
| **C5** | M1 → M2 | `POST {M2_URL}/api/v1/internal/maintenance-events` | HTTP | **No existe** | Crear en ambos módulos |

**Autenticación:** ninguna entre módulos. M1 deja `POST /internal/status-events` y `GET /vessels/**` en `permitAll` (`SecurityConfig.java:92-96`). M2 no tiene Spring Security y su nuevo endpoint interno será igualmente público.

---

## 2. C1 — Ficha individual de embarcación

### Petición

```http
GET {M1_URL}/api/v1/vessels/37a31793-29e9-4765-9d3d-39b74a744476
Accept: application/json
```

### Respuesta `200 OK` (actual de M1)

```json
{
  "vessel_id": "37a31793-29e9-4765-9d3d-39b74a744476",
  "name": "Embarcación demo 01",
  "registration_number": "MAT-12345",
  "type": "YACHT",
  "max_capacity": 8,
  "operational_status": "AVAILABLE",
  "photo_url": "https://cdn.example/1.jpg",
  "captain_included": true,
  "port": { "name": "Marina demo", "latitude": 11.24, "longitude": -74.21, "timezone": "America/Bogota" },
  "amenities": ["Capitán", "Combustible"],
  "owner": { "owner_id": "…", "validated_name": "…" }
}
```

### Campos que M2 lee

| Campo | Obligatorio para M2 | Si falta |
|---|---|---|
| `name` | Sí | `503 FLEET_SERVICE_UNAVAILABLE` (`HttpVesselReadAdapter.java:131-134`) |
| `max_capacity` | Sí en CU-02 | Capacidad inválida → activo no disponible |
| `port.timezone` | Sí en CU-02 | `503 HARBOR_TIMEZONE_UNAVAILABLE` (`CreateReservationService.java:71-79`) |
| `owner.owner_id` | Sí en CU-02 | Sin propietario no se crea la reserva |
| `type`, `photo_url`/`image_url`, `amenities`, `owner.validated_name` | No | Se muestran vacíos |
| `length_feet`, `cabin_count`, `owner.verified` | No | M1 no los envía hoy; M2 los deja en `null` |

### Errores

| HTTP M1 | Causa | Respuesta de M2 |
|---|---|---|
| `400` | UUID malformado | `400 INVALID_UUID` |
| `404` | No existe, eliminada o borrador | `404 VESSEL_NOT_FOUND` |
| `5xx` / timeout | Falla de M1 | `503 FLEET_SERVICE_UNAVAILABLE` |
| cuerpo vacío o ilegible | — | `503 FLEET_SERVICE_UNAVAILABLE` |

### Resiliencia
Connect 100 ms / read 300 ms. Un reintento inmediato solo ante `503` o error de conexión; nunca ante `4xx`. El detalle de CU-19 aplica *fail-safe* por elemento y no reintenta.

---

## 3. C2 — Catálogo de embarcaciones (compromiso del sábado)

### Petición objetivo

```http
GET {M1_URL}/api/v1/vessels?page=1&size=20&q=texto
Accept: application/json
```

| Parámetro | Obligatorio | Regla |
|---|---|---|
| `page` | No | Entero ≥ 1; por defecto 1 |
| `size` | No | Entero; M2 envía siempre 20 |
| `q` | No | Texto libre por nombre o ubicación |

> M2 **no envía** fechas, tipo, pasajeros ni token. El filtrado por fechas, tipo y capacidad es local de M2.

### Respuesta objetivo `200 OK`

```json
{
  "vessels": [
    {
      "vessel_id": "…",
      "name": "Embarcación demo 01",
      "type": "YACHT",
      "photo_url": "https://cdn.example/1.jpg",
      "port": { "name": "Marina demo", "latitude": 11.24, "longitude": -74.21, "timezone": "America/Bogota" },
      "max_capacity": 8,
      "captain_included": true
    }
  ],
  "pagination": { "page": 1, "page_size": 20, "total_items": 25, "total_pages": 2 }
}
```

M2 ignora cualquier campo extra y acepta elementos sin `vessel_id` descartándolos (`HttpFleetCatalogAdapter.java:77-81`).

### Errores y resiliencia
Cualquier respuesta no `2xx` se traduce a `503 FLEET_SERVICE_UNAVAILABLE` (`HttpFleetCatalogAdapter.java:52-54`). Connect 100 ms / read 300 ms, sin reintento.

### Estado actual (VERIFICADO)
M1 **no expone** esta ruta: sólo `/{vessel_id}` y `/{vessel_id}/operational-status` (`ReservationsVesselController.java:35,60`). El listado existente `/api/v1/fleet/boats` exige JWT y devuelve sólo las embarcaciones del propietario (`BoatController.java:94-98`). Con `mode=http`, CU-01 devuelve `503`.

---

## 4. C3 — Estado operativo

### Petición actual

```http
GET {M1_URL}/api/v1/vessels/37a31793-29e9-4765-9d3d-39b74a744476/operational-status
Accept: application/json
```

### Petición objetivo

```http
GET {M1_URL}/api/v1/vessels/{vessel_id}/operational-status?start_date=2026-12-20&end_date=2026-12-22&timezone=America/Bogota
Accept: application/json
```

| Parámetro | Obligatorio | Regla |
|---|---|---|
| `start_date`, `end_date` | No | Rango de la reserva en la zona del puerto; si faltan, el veredicto es sólo del instante |
| `timezone` | No | Zona IANA del puerto; por defecto `America/Bogota` |

### Respuesta actual

```json
{
  "vessel_id": "…",
  "operational_status": "AVAILABLE",
  "queried_at": "2026-11-19T10:00:00-05:00"
}
```

### Respuesta objetivo

```json
{
  "vessel_id": "…",
  "operational_status": "MAINTENANCE",
  "queried_at": "2026-11-19T10:00:00-05:00",
  "maintenance_start": "2026-12-21T08:00:00-05:00",
  "maintenance_until": "2026-12-27T18:00:00-05:00"
}
```

Sin mantenimiento, ambos campos van en `null`.

### Veredicto que aplica M2

| `operational_status` | Mantenimiento | Resultado |
|---|---|---|
| `AVAILABLE` | — | Disponible |
| `RESERVED` | — | Disponible para el rango (el bloqueo es de M2) |
| `IN_NAVIGATION` / `NAVIGATION` | — | Disponible para el rango |
| `MAINTENANCE` / `MAINTENANCE_CLEANING` | fechas que **no** cruzan | Disponible |
| `MAINTENANCE` / `MAINTENANCE_CLEANING` | fechas que cruzan | No disponible |
| `MAINTENANCE` | sin fechas | No disponible (seguro) |
| desconocido / ilegible / vacío | — | `503 FLEET_SERVICE_UNAVAILABLE` (fail-safe) |

**Cruce de fechas:** se convierten los instantes a fecha local del puerto y se comparan con el rango inclusivo de la reserva. Cualquier día tocado por el mantenimiento se considera bloqueado.

**Vencimiento:** si `maintenance_until` ya pasó, M1 devuelve `AVAILABLE` con `maintenance_start` y `maintenance_until` en `null`, aunque el estado persistido siga en `MAINTENANCE`. La corrección es perezosa, en la consulta; no hay job de liberación. M2 aplica la misma regla como defensa.

### Errores y resiliencia
Igual que hoy: `400 INVALID_UUID`, `404 VESSEL_NOT_FOUND`, `503 FLEET_SERVICE_UNAVAILABLE`; un reintento ante `503` o conexión. Connect 100 ms / read 300 ms.

---

## 5. C4 — Avisos de estado M2 → M1

### Petición

```http
POST {M1_URL}/api/v1/internal/status-events
Content-Type: application/json
```

```json
{
  "event_id": "7f3c1a9e-2b64-4d0f-9c81-5a2d6e4b1101",
  "vessel_id": "37a31793-29e9-4765-9d3d-39b74a744476",
  "operational_status": "IN_NAVIGATION",
  "occurred_at": "2026-12-20T08:05:00Z"
}
```

| Campo | Tipo | Regla |
|---|---|---|
| `event_id` | string | Obligatorio, ≤ 100; mismo valor en todos los reintentos |
| `vessel_id` | UUID | Obligatorio |
| `operational_status` | string | `IN_NAVIGATION` o `MAINTENANCE_CLEANING` o `AVAILABLE` (ver §8) |
| `occurred_at` | ISO-8601 con offset | M2 lo emite en UTC (`Z`); M1 acepta `Z` y offsets |

### Respuesta `200 OK`

```json
{ "event_id": "7f3c1a9e-2b64-4d0f-9c81-5a2d6e4b1101", "outcome": "APPLIED" }
```

| `outcome` | Significado | M2 |
|---|---|---|
| `APPLIED` | El estado cambió | `published_at` |
| `DUPLICATE` | `event_id` ya procesado | `published_at` |
| `NO_CHANGE` | Ya tenía ese estado | `published_at` |
| `STALE` | Anterior al último cambio | `published_at` |
| `UNKNOWN_VESSEL` | No existe o es borrador | `dead=true` |
| `INVALID_TRANSITION` | Transición fuera de la tabla | `dead=true` |

### Errores
`400` con `ProblemDetail` y la propiedad `errors[{field,message}]` (no `{code,message}`).
`4xx` → permanente (`dead`); `5xx` o timeout → reintento.

### Resiliencia
Connect 100 ms / read 5 000 ms. Cinco intentos con backoff 1/5/25/125 s y jitter solo ante `5xx` o timeout. Sin DLQ: `dead=true` + alerta.

### Idempotencia
`event_id` se genera en la transición local y se conserva en todos los reintentos. M1 deduplica por `processed_event`.

### Orden
No garantizado. M1 descarta los anteriores como `STALE` (comparación estricta contra `status_changed_at`).

---

## 6. C5 — Avisos de mantenimiento M1 → M2 (nuevo)

### Cuándo
Cuando una embarcación pasa a `MAINTENANCE`, una sola vez por evento.

### Petición

```http
POST {M2_URL}/api/v1/internal/maintenance-events
Content-Type: application/json
```

```json
{
  "event_id": "b0f5f0a2-6a0e-4f0e-9a59-1f1c2f3a4b5c",
  "vessel_id": "37a31793-29e9-4765-9d3d-39b74a744476",
  "maintenance_start": "2026-11-20T08:00:00-05:00",
  "maintenance_until": "2026-11-27T18:00:00-05:00",
  "occurred_at": "2026-11-19T15:15:00Z",
  "reason": "OWNER"
}
```

| Campo | Obligatorio | Regla |
|---|---|---|
| `event_id` | Sí | ≤ 100; clave de idempotencia |
| `vessel_id` | Sí | UUID |
| `maintenance_start` | No | `null` = empieza ahora |
| `maintenance_until` | Sí | Debe ser posterior a `maintenance_start` |
| `occurred_at` | Sí | ISO-8601 con offset |
| `reason` | No | `OWNER` o `ADMIN` |

### Respuesta `200 OK`

```json
{ "event_id": "b0f5f0a2-6a0e-4f0e-9a59-1f1c2f3a4b5c", "outcome": "APPLIED" }
```

| `outcome` | Cuándo |
|---|---|
| `APPLIED` | Canceló al menos una reserva, o no había ninguna |
| `DUPLICATE` | `event_id` ya procesado |
| `NO_AFFECTED_RESERVATIONS` | Ninguna `RESERVED` cruza el rango |
| `UNKNOWN_VESSEL` | M2 no conoce la embarcación |
| `INVALID_RANGE` | Rango bien formado pero íntegramente en el pasado; no hay nada que aplicar |
| `AUTO_CANCEL_DISABLED` | `m2.maintenance.auto-cancel=false` |

### Errores

| HTTP | `code` | Cuándo | Reintento de M1 |
|---|---|---|---|
| `400` | `INVALID_PARAMETERS` | Campo faltante o mal formado | No |
| `400` | `INVALID_MAINTENANCE_RANGE` | Rango incoherente | No |
| `503` | `MAINTENANCE_EVENT_PARTIAL_FAILURE` | Alguna cancelación falló | **Sí**, mismo `event_id` |
| `500` | `INTERNAL_ERROR` | Fallo inesperado | Sí, una vez |

La respuesta de error de M2 usa el sobre `{code, message}` (`ErrorBody.java:3`).

### Idempotencia y fallo parcial
Clave `event_id` en `idempotency_key` con `endpoint = /api/v1/internal/maintenance-events` y `response_ref = outcome`. Un reenvío devuelve `DUPLICATE`. Un `503` no guarda registro: el reintento reprocesa y salta las reservas ya canceladas por estado.

### Efecto en M2
- Cancela sólo reservas `RESERVED` cuyo rango cruza el mantenimiento, con subtipo `BY_OWNER` y causa de sistema.
- `INITIATED` y `PENDING_PAYMENT` no se cancelan: expiran por TTL.
- Emite a M3 `CANCELLED_BY_HOST` (reembolso 100 %), fuera de este contrato.

### Resiliencia de M1
Connect 1 000 ms / read 5 000 ms. Cinco intentos con backoff 1/5/25/125 s ante timeout, `503` o `5xx`; nunca ante `4xx`. **M1 debe persistir el aviso en un outbox** y reintentarlo para no perderlo si M2 está caído.

---

## 7. Semántica transversal

### Identificadores
- `event_id`: obligatorio, ≤ 100 caracteres, único por evento, estable en reintentos.
- `vessel_id`: UUID de M1.
- Idempotencia en M1 por `processed_event`; en M2 por `idempotency_key`.

### Fechas y zonas
- `occurred_at`, `maintenance_start`, `maintenance_until`, `queried_at`: ISO-8601 con offset.
- M1 devuelve `queried_at` en `America/Bogota` (`ReservationsVesselController.java:25-27`).
- Reservas: `DATE` inclusivo, último día ocupado.
- Mantenimiento: instantes; se convierten a fecha local para el cruce.
- Regla: cualquier día tocado por el mantenimiento queda bloqueado.

### Estados

| M1 | M2 envía | M2 interpreta |
|---|---|---|
| `AVAILABLE` | `AVAILABLE` | disponible |
| `RESERVED` | — (ya no se envía) | disponible para el rango |
| `NAVIGATION` | `IN_NAVIGATION` | disponible para el rango |
| `MAINTENANCE` | `MAINTENANCE_CLEANING` | bloquea según fechas |
| alias `NAVIGATION`/`MAINTENANCE` | — | M1 los acepta; M2 no los emite |

### Timeouts y reintentos

| Conexión | Connect / read | Reintentos |
|---|---|---|
| C1 | 100 / 300 ms | 1 ante `503` o conexión |
| C2 | 100 / 300 ms | 0 |
| C3 | 100 / 300 ms | 1 ante `503` o conexión |
| C4 | 100 / 5 000 ms | 5 con backoff |
| C5 | 1 000 / 5 000 ms | 5 con backoff |

### Errores
- M1: `ProblemDetail` con `errors[]` en `400`; `404` para recurso inexistente; `500` genérico.
- M2: `{code, message}`; `503` para dependencia caída.

---

## 8. Reglas de negocio de C4 (FR-002 sin `RESERVED`)

| Transición de la reserva | Valor enviado | Condición |
|---|---|---|
| `RESERVED` | **ninguno** | — |
| `IN_NAVIGATION` | `IN_NAVIGATION` | Siempre; activa la guarda de ocupación en M2 |
| `COMPLETED` | `MAINTENANCE_CLEANING` | Solo si M1 conocía la ocupación |
| `CANCELLED` ordinaria o no-show | `AVAILABLE` | Solo si M1 conocía la ocupación **y** no hay mantenimiento activo |
| `CANCELLED` por avería | `MAINTENANCE_CLEANING` | Igual condición |
| `INITIATED`, `PENDING_PAYMENT`, `EXPIRED`, `PAYMENT_FAILED` | ninguno | — |

**Cambio obligatorio en M1:** permitir `AVAILABLE → NAVIGATION` en su máquina de estados. Hoy no está permitida (`AssignOperationalStatusService.java:33-37`) y el check-in devolvería `INVALID_TRANSITION`.

**Cambio obligatorio en M2:** la guarda de ocupación (`m1_blocked`) debe activarse al enviar `IN_NAVIGATION`, no al enviar `RESERVED`, o se perderá el `MAINTENANCE_CLEANING` de `COMPLETED`.

---

## 9. Decisiones aplicables

Estas decisiones son de cumplimiento obligatorio para el despliegue del **2026-11-21**. No hay preguntas abiertas en este contrato.

| ID | Decisión |
|---|---|
| **D-01** | C4 y C5 son HTTP POST sin autenticación entre módulos. M2 responde `{event_id, outcome}` en C4 y C5. |
| **D-02** | M1 crea C2 (`GET /api/v1/vessels`) **antes del 2026-11-21**, público, con `{vessels[], pagination{page,page_size,total_items,total_pages}}` y parámetros `page`, `size`, `q`. |
| **D-03** | M1 añade `AVAILABLE → NAVIGATION` a su máquina de estados. No se permite `AVAILABLE → MAINTENANCE` por aviso: el mantenimiento lo inicia el propietario o el admin. |
| **D-04** | C4 deja de enviar `RESERVED`. Se envía `IN_NAVIGATION` (check-in), `MAINTENANCE_CLEANING` (completada) y `AVAILABLE` o `MAINTENANCE_CLEANING` (cancelación), según §8. |
| **D-05** | `maintenance_until` es **obligatorio**; `maintenance_start` es opcional (`null` = ahora) y debe ser anterior a `until`. Si falta `until`, no se puede leer o `until <= start`, M2 responde `400 INVALID_MAINTENANCE_RANGE`. Un rango válido pero íntegramente en el pasado devuelve `200` con `outcome=INVALID_RANGE`. |
| **D-06** | El mantenimiento solo se inicia desde `AVAILABLE`. `reason` acepta `OWNER` o `ADMIN`. |
| **D-07** | La liberación es **perezosa al consultar**: si `maintenance_until` ya pasó, C3 devuelve `AVAILABLE` con fechas `null`. No hay job de liberación. |
| **D-08** | El cruce usa la zona del puerto y el rango inclusivo de la reserva. **Cualquier día tocado por el mantenimiento queda bloqueado.** |
| **D-09** | M1 **debe** persistir los avisos de C5 en un outbox y reintentarlos: connect 1 000 ms, read 5 000 ms, 5 intentos 1/5/25/125 s solo ante timeout, `503` o `5xx`. |
| **D-10** | M2 cancela automáticamente las reservas `RESERVED` cuyo rango cruza el mantenimiento, con subtipo `BY_OWNER` y actor sistema. `INITIATED` y `PENDING_PAYMENT` no se cancelan: expiran por TTL. |
| **D-11** | M1 **no** emite un aviso al cancelar un mantenimiento antes de tiempo. M2 se guía por `maintenance_until`. |
| **D-12** | La URL de M2 en M1 se configura por variable de entorno (`M2_BASE_URL`), sin valor por defecto en producción. |

---

## 10. Trazabilidad

| Requisito | Dónde se cumple |
|---|---|
| CU-09 FR-003/FR-004 (ficha y catálogo) | C1 y C2 |
| CU-10 FR-002 a FR-006 (estado operativo y fail-safe) | C3 |
| CU-12/CU-19 FR-004 (capacidad) | C1 |
| CU-22 FR-001 a FR-009 (aviso de estado) | C4 y §8 |
| Diseño de mantenimiento (C5) | §6 y `context/consistencia-m1-m2-mantenimiento.md` |
| Disponibilidad por fechas (M2) | §0 y §4 |
