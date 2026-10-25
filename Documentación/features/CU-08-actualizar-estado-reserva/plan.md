# Plan Técnico: Actualizar Estado de Reserva (CU-08)

**Módulo**: Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones  
**Spec de referencia**: [`Documentación/features/CU-08-actualizar-estado-reserva/spec.md`](../../features/CU-08-actualizar-estado-reserva/spec.md)  
**Fecha**: 2026-10-22

Este caso de uso es interno: no expone ningún endpoint REST. Es el motor que aplica todos los cambios de estado de la reserva y el único escritor de estado de Módulo 2 (FR-001). Lo invocan, siempre por su puerto de aplicación, CU-02, CU-03, CU-13 (con dos canales: webhook REST de M3 y job de consulta periódica), CU-06, CU-07, CU-04, CU-05 y el job de expiración del TTL.

Al ejecutarse:
1. Valida que la reserva exista y que la transición pedida esté en la matriz de estados (FR-003, FR-005).
2. Valida sub-estado, actor y, para `RESERVED`, que el TTL no haya vencido.
3. Aplica el nuevo estado de forma atómica con control de concurrencia por versión (FR-004, FR-006).
4. Escribe la auditoría del cambio.
5. En la misma transacción, escribe en el outbox los eventos que correspondan: hacia Módulo 3 (CU-14, `<<include>>`) y hacia Módulo 1 (CU-22, `<<include>>`). Cada evento lleva su `event_id` (FR-011).
6. No espera la entrega de los eventos. Si falla, la transición ya está confirmada y el evento se reintenta.
7. No calcula dinero (FR-010).

Contratos relacionados: [plan de CU-14](../CU-14-recibir-estado-reserva/plan.md) y el contrato de notificación a Módulo 1 de CU-22.

---

## Puerto de entrada — Aplicar transición de estado de la reserva

### Método HTTP y URL

No aplica. Es un puerto interno (`ApplyReservationTransitionUseCase`); ningún cliente externo lo invoca por HTTP.

### Elementos de la Petición (Request)

**Headers**: No tiene.

**Query Parameters**: No tiene.

**Path Parameters**: No tiene.

**Comando (`TransitionCommand`)**:

```json
{
  "reservation_id": "string (UUID)",
  "target_status": "string (INITIATED | PENDING_PAYMENT | RESERVED | IN_NAVIGATION | COMPLETED | CANCELLED | EXPIRED | PAYMENT_FAILED)",
  "cancellation_subtype": "string (FLEXIBLE | MODERATE | LATE | BY_OWNER | NO_SHOW), obligatorio si target_status es CANCELLED",
  "actor": "string (RENTER | OWNER | SYSTEM)",
  "actor_id": "string (UUID), obligatorio si actor no es SYSTEM",
  "reason": "string (opcional: motivo de rechazo o causa de cancelación)",
  "mechanical_failure": "boolean (obligatorio si cancela OWNER; define MAINTENANCE_CLEANING en CU-22)",
  "anticipation_minutes": "number (entero, obligatorio si cancela RENTER; lo calcula CU-04)",
  "completion_notes": "string (opcional, solo en COMPLETED)",
  "payment_reference": "string (opcional, solo en RESERVED)",
  "expected_version": "number (entero, opcional: versión que vio el invocador)"
}
```

La creación usa un comando aparte (`CreateReservationCommand`), solo desde CU-02: `vessel_id`, `owner_id`, `renter_id`, fechas, pasajeros y datos del titular. Devuelve la reserva en `INITIATED` con su `ttl_expires_at` (`now + 15 minutos`).

*Validaciones de entrada (FR-003, FR-005)*:
- `target_status` debe ser una transición permitida desde el estado actual (matriz abajo). Cualquier otra se rechaza.
- `cancellation_subtype` es obligatorio solo con `CANCELLED` y prohibido con los demás estados.
- `NO_SHOW` solo lo pide CU-05. `FLEXIBLE`, `MODERATE` y `LATE` solo con actor `RENTER`. `BY_OWNER` solo con actor `OWNER`.
- Para `RESERVED`, el TTL debe estar vigente (corte exacto en el segundo 900, sin ventana de gracia).
- Los estados terminales (`COMPLETED`, `CANCELLED`, `EXPIRED`, `PAYMENT_FAILED`) no admiten ningún cambio.

**Matriz de transiciones permitidas (FR-003)**:

| Origen | Destino | Lo dispara | Actor |
|---|---|---|---|
| (creación) | `INITIATED` | CU-02 | `RENTER` |
| `INITIATED` | `PENDING_PAYMENT` | CU-03 | `RENTER` |
| `INITIATED` | `EXPIRED` | Job de TTL | `SYSTEM` |
| `PENDING_PAYMENT` | `RESERVED` | CU-13 (pago aprobado) | `SYSTEM` |
| `PENDING_PAYMENT` | `EXPIRED` | Job de TTL | `SYSTEM` |
| `PENDING_PAYMENT` | `PAYMENT_FAILED` | CU-13 (rechazo definitivo, cancelado o expirado) | `SYSTEM` |
| `RESERVED` | `IN_NAVIGATION` | CU-06 | `OWNER` |
| `RESERVED` | `CANCELLED` | CU-04 o CU-05 | `RENTER`, `OWNER` |
| `IN_NAVIGATION` | `COMPLETED` | CU-07 | `OWNER` |

---

### Elementos de la Respuesta (Response)

**Resultado (éxito)**: la reserva con su nuevo estado.

**Contrato de respuesta tipado**:

```json
{
  "reservation_id": "string (UUID)",
  "status": "string (estado actual)",
  "cancellation_subtype": "string | null",
  "status_changed_at": "string (ISO 8601 con zona horaria)",
  "ttl_expires_at": "string (ISO 8601, no cambia nunca desde la creación)",
  "version": "number (entero, incrementada tras el cambio)",
  "events_enqueued": ["M3_QUEUE", "M1_QUEUE"]
}
```

*(`events_enqueued` indica a qué destinos se escribió un evento en el outbox; puede ser vacío. Ningún evento ni campo de la respuesta lleva montos, FR-010.)*

---

### Ejemplo de Comando y Resultado Exitoso

**Comando**: pago confirmado, la reserva pasa a `RESERVED` (lo invoca el job de CU-13):

```json
{
  "reservation_id": "e4f81c92-7a20-4215-9c5e-8812c3f1a001",
  "target_status": "RESERVED",
  "actor": "SYSTEM",
  "payment_reference": "pg-8841",
  "expected_version": 3
}
```

**Resultado**:

```json
{
  "reservation_id": "e4f81c92-7a20-4215-9c5e-8812c3f1a001",
  "status": "RESERVED",
  "cancellation_subtype": null,
  "status_changed_at": "2026-10-22T12:10:00-05:00",
  "ttl_expires_at": "2026-10-22T12:15:00-05:00",
  "version": 4,
  "events_enqueued": ["M3_QUEUE", "M1_QUEUE"]
}
```

**Evento a Módulo 1 generado en esa transición** (según CU-22 FR-002):

```json
{
  "vessel_id": "d3b07384-d113-49cd-a5d6-812e9bcfc101",
  "operational_status": "RESERVED",
  "occurred_at": "2026-10-22T12:10:00-05:00",
  "event_id": "7f3c1a9e-2b64-4d0f-9c81-5a2d6e4b1101"
}
```

**Efecto de cada transición**:

| Destino | Evento a Módulo 3 (CU-14) | Notificación a Módulo 1 (CU-22) |
|---|---|---|
| `INITIATED` | No | No |
| `PENDING_PAYMENT` | Sí | No (el TTL y el pago pendiente se gestionan solo en Módulo 2) |
| `RESERVED` | Sí | `RESERVED` |
| `IN_NAVIGATION` | Sí | `IN_NAVIGATION` |
| `COMPLETED` | Sí, con novedades si existen (FR-009) | `MAINTENANCE_CLEANING` |
| `CANCELLED` | Sí, con sub-estado y anticipación | `AVAILABLE`, o `MAINTENANCE_CLEANING` si hay avería |
| `EXPIRED` desde `PENDING_PAYMENT` | [PEND] ver Pendiente 2 | No |
| `EXPIRED` desde `INITIATED` | No | No |
| `PAYMENT_FAILED` | [PEND] ver Pendiente 2 | No |

La tabla FR-002 de CU-22 es la fuente de verdad del mapeo hacia Módulo 1; este plan no la duplica.

---

### Manejo de Errores

CU-08 no devuelve HTTP: lanza errores de aplicación que el CU invocador traduce a su respuesta. La columna HTTP es la sugerencia para ese mapeo.

| Código | Caso | HTTP sugerido | Cuerpo (ejemplo) |
|---|---|---|---|
| `RESERVATION_NOT_FOUND` | La reserva no existe | `404` | `{ "code": "RESERVATION_NOT_FOUND", "message": "La reserva especificada no existe" }` |
| `INVALID_STATE_TRANSITION` | Transición fuera de la matriz, estado terminal, sub-estado inválido o actor no permitido (FR-005) | `409` | `{ "code": "INVALID_STATE_TRANSITION", "message": "No se permite pasar de Pendiente de Pago a Cancelada" }` |
| `VERSION_CONFLICT` | Dos escrituras concurrentes sobre la misma reserva; la segunda se rechaza sin sobrescribir (FR-006) | `409` | `{ "code": "VERSION_CONFLICT", "message": "La reserva fue modificada por otra operación" }` |
| `RESERVATION_EXPIRED` | Se pide `RESERVED` con el TTL vencido | `409` | `{ "code": "RESERVATION_EXPIRED", "message": "El tiempo límite de 15 minutos de la reserva ha expirado" }` |
| `PERSISTENCE_ERROR` | Falla de base de datos; no se consolida nada (FR-004) | `500` | `{ "code": "PERSISTENCE_ERROR", "message": "No se pudo guardar el cambio de estado" }` |

*No es error de CU-08*: que el broker no responda al publicar. La transición ya está confirmada y el evento queda en el outbox.

---

### Paginación

No aplica. Operación sobre una única reserva.

---

### Seguridad y Perfiles

- Solo lo invocan los casos de uso internos de Módulo 2 y los jobs (`SYSTEM`). Ningún cliente lo llama por HTTP.
- Cada CU invocador es responsable de validar la identidad y el rol de quien actúa (Arrendatario titular, Propietario registrado de la embarcación) antes de invocar a CU-08. CU-08 solo comprueba que el `actor` enviado sea compatible con la transición.
- Regla "Sin dinero": ni el comando ni los eventos contienen montos, tarifas ni datos de pago (FR-010, CU-14 FR-006, CU-22 FR-007).
- `payment_reference` es la referencia externa del cobro, no un dato sensible; los tokens de pago nunca se escriben en logs.

---

### Notas Transversales

- **Un solo escritor de estado (FR-001)**: ninguna clase fuera del servicio de CU-08 escribe `reservation.status`. Se verifica con una regla ArchUnit.
- **Concurrencia sobre la misma reserva (FR-006)**: bloqueo optimista con `@Version`. Dos transiciones leen la versión 7; la primera confirma y la deja en 8, la segunda recibe `VERSION_CONFLICT` sin sobrescribir el estado.
- **Concurrencia sobre el mismo barco y restricción GiST**: `@Version` no protege a dos reservas distintas del mismo barco. CU-03 toma, antes de invocar a CU-08, el bloqueo pesimista por `vessel_id` y revisa en la base de Módulo 2 que no exista otra reserva del barco en `PENDING_PAYMENT`, `RESERVED` o `IN_NAVIGATION`. Como garantía física definitiva en base de datos, la restricción `EXCLUDE USING gist (vessel_id WITH =, daterange(start_date, end_date, '[]') WITH &&) WHERE (status IN ('PENDING_PAYMENT', 'RESERVED', 'IN_NAVIGATION'))` previene cualquier solapamiento o sobreventa concurrente en PostgreSQL.
- **Transacción única (FR-004, FR-011)**: la lectura, validación, cambio de estado, auditoría y escritura de eventos en `outbox_message` se confirman juntos o no se confirma nada. El `event_id` se genera una sola vez y se conserva en todos los reintentos.
- **Entrega de eventos**: un relay publica con confirmación del broker y marca `published_at` solo tras el `ack`. El primer intento sale tras el commit, sin esperar al sondeo (SC-002, menos de 500 ms). Política: máximo 5 intentos (el primero inmediato), backoff con jitter de 1 s, 5 s, 25 s y 125 s, solo ante `5xx` o timeout. Un `4xx` permanente va directo a DLQ con alerta. Tras el quinto fallo: DLQ y alerta, sin descartar el evento ni revertir la transición.
- **Guarda `m1_blocked`**: la reserva persiste el booleano `m1_blocked` que indica si ya se notificó `RESERVED` a Módulo 1. La liberación (`AVAILABLE`) o pase a mantenimiento (`MAINTENANCE_CLEANING`) hacia M1 solo se encola si este indicador es verdadero (SC-003). Así, `EXPIRED` o `CANCELLED` desde `INITIATED` o `PENDING_PAYMENT` nunca emite eventos de liberación indebidos a Módulo 1.
- **TTL**: `ttl_expires_at` se fija al crear la reserva y no se reinicia nunca. Un job (propuesta: cada 5 s) toma con `FOR UPDATE SKIP LOCKED` las reservas en `INITIATED` o `PENDING_PAYMENT` vencidas y las envía a CU-08 como `EXPIRED`, actor `SYSTEM`. Ejemplo con creación a las 10:00:00: a las 10:14:59 `RESERVED` es válido; a las 10:15:00 devuelve `RESERVATION_EXPIRED` y el job la pasa a `EXPIRED`.
- **Auditoría**: cada transición, incluida la creación, escribe una fila inmutable en `reservation_status_audit` (reserva, estados anterior y nuevo, sub-estado, actor, motivo, `occurred_at` y `event_ids`).
- **Modelo de datos mínimo**:

| Tabla | Campos clave |
|---|---|
| `reservation` | `id`, `vessel_id`, `owner_id`, `renter_id`, `status`, `cancellation_subtype`, `start_date`, `end_date`, `departure_time`, `ttl_expires_at`, `status_changed_at`, `m1_blocked`, `version` |
| `reservation_status_audit` | `reservation_id`, `from_status`, `to_status`, `subtype`, `actor`, `actor_id`, `reason`, `occurred_at`, `event_ids` |
| `outbox_message` | `id` (= `event_id`), `destination` (`M3_QUEUE` o `M1_QUEUE`), `exchange`, `routing_key`, `payload`, `published_at`, `attempts`, `next_attempt_at`, `status` |

*(Campos de fecha y control en `reservation`: `start_date` y `end_date` delimitan el rango de servicio para la restricción GiST de no solapamiento temporal; `departure_time` fija la hora programada de zarpe para el cálculo de anticipación en CU-04 y la ventana de CU-06; `m1_blocked` previene desincronizaciones operativas con Módulo 1).*

---

### Pendientes (`NEEDS CLARIFICATION`)

1. **Guarda de TTL en `RESERVED`**: este plan la pone dentro de CU-08. Confirmar si vive aquí o solo en CU-13.
2. **`EXPIRED` (desde `PENDING_PAYMENT`) y `PAYMENT_FAILED` hacia Módulo 3**: CU-14 FR-001 dice que toda transición desde `PENDING_PAYMENT` se notifica; el flujo alterno del spec de CU-08 dice que no. Módulo 3 los necesita para cancelar la autorización o revertir un cobro huérfano, y se le pidió aceptar los estados de reserva expirada y de pago fallido (`EXPIRED` y `PAYMENT_FAILED` en nuestro vocabulario; el nombre en el enum de M3 está por confirmar).
3. **Barco `RESERVED` por semanas**: con una reserva confirmada para dentro de un mes, Módulo 1 mostraría el barco ocupado todo ese tiempo. Choca con H1 (disponibilidad instantánea). Decisión de negocio.
4. **Orden de entrega a Módulo 1**: pedirle que aplique un evento solo si su `occurred_at` es más reciente que el último aplicado.
5. **Último salto a Módulo 1**: cola (AMQP) o HTTP `PATCH`, y el valor `M1_HTTP` del enum `destino` en `plan.md`. Confirmar también si `outbox_message` es una tabla compartida.
6. **Réplicas múltiples del job de TTL**: `SKIP LOCKED` o ShedLock.

---

### Trazabilidad FR/SC → Elemento del Contrato

| Requisito / Criterio | Descripción en Spec | Elemento de este Plan |
|---|---|---|
| **FR-001** | Único lugar donde se actualiza el estado | Puerto de entrada único y regla ArchUnit |
| **FR-002** | Lista oficial de estados y sub-estados | Campos `target_status` y `cancellation_subtype` |
| **FR-003** | Solo transiciones permitidas | Matriz de transiciones |
| **FR-004** | Guardado atómico | Transacción única (Notas Transversales) |
| **FR-005** | Rechazo de cambios inválidos | Error `INVALID_STATE_TRANSITION` |
| **FR-006** | Control de concurrencia | `@Version` y error `VERSION_CONFLICT`; bloqueo pesimista en CU-03 y restricción GiST sobre `(vessel_id, start_date, end_date)` |
| **FR-007** | Sincronizar Módulo 1 según CU-22 FR-002 | Tabla "Efecto de cada transición" y `m1_blocked` |
| **FR-008** | Notificar a Módulo 3 desde `PENDING_PAYMENT` | Columna "Evento a Módulo 3" |
| **FR-009** | Novedades del cierre en `COMPLETED` | Campo `completion_notes` |
| **FR-010** | Sin cálculo financiero | Ningún campo monetario en comando ni eventos |
| **FR-011** | `event_id` único generado en la transacción | `outbox_message.id` |
| **SC-001** | 0 % de cambios no permitidos | Matriz y prueba unitaria exhaustiva |
| **SC-002** | Primer intento menos de 500 ms | Publicación tras el commit |
| **SC-003** | Sin barcos bloqueados sin reserva de respaldo | Guarda `m1_blocked` |