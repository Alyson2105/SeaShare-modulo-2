# Contrato REST: Marcar Inasistencia (CU-05)

**Módulo**: Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones  
**Spec de referencia**: [`Documentación/features/CU-05-marcar-inasistencia/spec.md`](../../features/CU-05-marcar-inasistencia/spec.md)  
**Fecha**: 2026-10-09  

Este caso de uso expone la operación REST que permite al **Propietario registrado** de la embarcación reportar la inasistencia del Arrendatario (*No-Show*) tras haber transcurrido los **30 minutos de cortesía obligatorios** posteriores a la hora acordada de zarpe (FR-001 a FR-019).

Al ejecutarse exitosamente:
1. Valida que la reserva exista y se encuentre estrictamente en estado **`RESERVED`** (FR-001, FR-006).
2. Valida la legitimación del actor: el usuario autenticado debe ser unívocamente el **Propietario registrado** de la embarcación asociada a la reserva (FR-002, SC-006).
3. Obtiene la zona horaria oficial del puerto de atraque invocando internamente a `Proveer información de embarcación` (CU-09, `<<include>>`, FR-004). Si la consulta falla, el sistema aplica *fail-safe* retornando `503 Service Unavailable` sin asumir zona horaria por defecto (FR-012).
4. Verifica que hayan transcurrido al menos **30 minutos continuos** desde la hora pactada de inicio de la reserva (`tiempo_actual >= scheduled_departure + 30 minutos`, FR-003). Si el tiempo no se ha cumplido, rechaza con `409 Conflict` informando el tiempo exacto faltante (FR-005, SC-001, SC-003).
5. Invoca a `Actualizar estado reserva` (CU-08) transicionando la reserva al estado terminal **`CANCELLED`** con sub-estado **`NO_SHOW`** (FR-007).
6. Notifica de forma asíncrona a las dependencias externas a través de CU-08:
   - Encola en la cola de notificación a Módulo 1 definida en CU-22 (`Recibir información estado operativo`, invocado vía `<<include>>`) la señal de liberación de la embarcación al estado **`AVAILABLE`** (FR-010). El flujo no espera respuesta de Módulo 1 ni depende de ella: la entrega se garantiza con reintentos y backoff, DLQ e idempotencia por identificador único de evento (CU-22 FR-006), y un fallo de entrega no revierte la transición local de Módulo 2.
   - Publica el evento asíncrono en RabbitMQ ([plan de CU-14](../CU-14-recibir-estado-reserva/plan.md)) hacia Módulo 3 informando el estado `CANCELLED` y sub-estado `NO_SHOW` para que Finanzas ejecute la compensación económica al Propietario (FR-011).
7. **Regla estricta "Sin dinero"**: Módulo 2 no calcula compensaciones, retenciones ni dispersiones monetarias (FR-009, SC-005).

---

## Endpoint — Reportar No-Show del Arrendatario

### Método HTTP y URL

```http
POST /api/v1/reservations/{reservation_id}/no-show
```

### Elementos de la Petición (Request)

**Headers**:

| Nombre | Obligatorio | Descripción |
|---|---|---|
| `Authorization` | Sí | `Bearer <token>` — JWT válido del Propietario registrado del barco |
| `Content-Type` | Sí | `application/json` |
| `Accept` | No | `application/json` |

**Path Parameters**:

| Nombre | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `reservation_id` | string (UUID) | Sí | Identificador de la reserva en estado `RESERVED` |

**Query Parameters**: No tiene.

**Body (JSON)**:

```json
{
  "observations": "string (opcional, comentarios del Propietario sobre la espera)"
}
```

*Validaciones de entrada*:
- `observations`: texto libre opcional (máx. 500 caracteres).

---

  ### Elementos de la Respuesta (Response)
 
 **Código de estado HTTP (éxito)**: `200 OK`
 
 **Contrato de respuesta tipado**:
 
 ```json
 {
   "reservation_id": "string (UUID)",
   "status": "string (valor literal: 'CANCELLED')",
   "cancellation_subtype": "string (valor literal: 'NO_SHOW')",
   "cancelled_at": "string (ISO 8601 timestamp con zona horaria del puerto)",
   "wait_minutes_recorded": "number (entero con los minutos transcurridos desde el zarpe)",
   "vessel_id": "string (UUID)",
   "vessel_operational_status": "string (valor literal: 'AVAILABLE')",
   "message": "string (texto confirmatorio de la cancelación y compensación)"
 }
 ```

**Headers de respuesta**:

| Nombre | Valor |
|---|---|
| `Content-Type` | `application/json` |

---

### Ejemplos de Petición y Respuestas Exitosas

#### Ejemplo 1 — Inasistencia Reportada tras 35 Minutos de Espera

**Petición `curl`**:

```bash
curl -X POST "https://api.seashare.com/api/v1/reservations/e4f81c92-7a20-4215-9c5e-8812c3f1a001/no-show" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.propietarioToken" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "observations": "El cliente no se presentó en la Marina Internacional de Santa Marta; no respondió a llamadas telefónicas."
  }'
```

**Respuesta (`200 OK`)**:

```json
{
  "reservation_id": "e4f81c92-7a20-4215-9c5e-8812c3f1a001",
  "status": "CANCELLED",
  "cancellation_subtype": "NO_SHOW",
  "cancelled_at": "2026-11-15T09:35:00-05:00",
  "wait_minutes_recorded": 35,
  "vessel_id": "d3b07384-d113-49cd-a5d6-812e9bcfc101",
  "vessel_operational_status": "AVAILABLE",
  "message": "Inasistencia confirmada. La reserva ha sido cancelada y la embarcación quedó Disponible. La compensación al propietario será gestionada por el sistema de pagos."
}
```

---

#### Ejemplo 2 — Inasistencia Reportada en el Minuto 30 Exacto (Límite Mínimo)

**Petición `curl`**:

```bash
curl -X POST "https://api.seashare.com/api/v1/reservations/e4f81c92-7a20-4215-9c5e-8812c3f1a001/no-show" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.propietarioToken" \
  -H "Content-Type: application/json" \
  -d '{}'
```

**Respuesta (`200 OK`)**:

```json
{
  "reservation_id": "e4f81c92-7a20-4215-9c5e-8812c3f1a001",
  "status": "CANCELLED",
  "cancellation_subtype": "NO_SHOW",
  "cancelled_at": "2026-11-15T09:30:00-05:00",
  "wait_minutes_recorded": 30,
  "vessel_id": "d3b07384-d113-49cd-a5d6-812e9bcfc101",
  "vessel_operational_status": "AVAILABLE",
  "message": "Inasistencia confirmada al cumplirse el tiempo de tolerancia reglamentario. La embarcación quedó Disponible."
}
```

---

### Manejo de Errores

| Código | Caso | Cuerpo de respuesta (ejemplo) |
|---|---|---|
| `400 Bad Request` | `reservation_id` no es un UUID válido | `{ "code": "INVALID_UUID", "message": "El identificador de reserva no es válido" }` |
| `401 Unauthorized` | Token ausente, inválido o expirado | `{ "code": "UNAUTHENTICATED", "message": "Token de autenticación inválido o ausente" }` |
| `403 Forbidden` | El usuario autenticado no es el Propietario registrado del barco de esta reserva (FR-002, SC-006) | `{ "code": "PROFILE_NOT_AUTHORIZED", "message": "Solo el propietario registrado de la embarcación puede marcar inasistencia" }` |
| `404 Not Found` | La reserva no existe en Módulo 2 | `{ "code": "RESERVATION_NOT_FOUND", "message": "La reserva especificada no existe" }` |
| `409 Conflict` (Tolerancia No Cumplida) | Intento de reporte antes de cumplir los 30 minutos de cortesía desde el zarpe (FR-005, SC-001, SC-003) | `{ "code": "GRACE_PERIOD_NOT_ELAPSED", "message": "El tiempo de espera de cortesía continúa activo. Faltan 12 minutos y 30 segundos para habilitar el reporte de inasistencia", "minutes_remaining": 12, "seconds_remaining": 30 }` |
| `409 Conflict` (Viaje No Iniciado) | Intento de reporte antes de la hora pactada de zarpe (FR-003) | `{ "code": "TRIP_NOT_STARTED", "message": "No se puede reportar inasistencia antes de la fecha y hora programada para el zarpe" }` |
 | `409 Conflict` (Estado Incompatible) | La reserva no está en `RESERVED` (ej. ya está `IN_NAVIGATION`, `CANCELLED`, `INITIATED`, `PENDING_PAYMENT`) (FR-006, SC-002) | `{ "code": "INVALID_RESERVATION_STATE", "message": "No se puede marcar inasistencia en una reserva con estado actual: En Navegación" }` |
| `503 Service Unavailable` | Fallo o timeout al consultar la zona horaria del puerto en M1 (*fail-safe*, FR-012) | `{ "code": "FLEET_SERVICE_UNAVAILABLE", "message": "No se pudo consultar la zona horaria del puerto para verificar la tolerancia. Reintente en unos momentos" }` |
| `500 Internal Server Error` | Error interno no controlado durante la transición | `{ "code": "INTERNAL_ERROR", "message": "Ocurrió un error inesperado al procesar la inasistencia" }` |

---

### Paginación

No aplica. Operación puntual sobre una reserva individual.

---

### Seguridad y Perfiles

- Perfil autorizado: **Propietario**.
- **Regla estricta de legitimación**: el servicio valida que el `sub` del token corresponda al `owner_id` del activo registrado en Módulo 1 asociado a la reserva. Arrendatarios, administradores u otros propietarios reciben `403 Forbidden` (FR-002, SC-006).
- **Regla "Sin dinero"**: Módulo 2 **no** calcula montos de retención, penalidades o compensaciones; únicamente certifica el cumplimiento temporal de los 30 minutos y el sub-estado contractual `NO_SHOW` (FR-009, SC-005).

---

### Notas Transversales

- **Disyuntiva del Propietario post-tolerancia**: cumplidos los 30 minutos de cortesía, si el cliente arriba tardíamente (ej. al minuto 35), el Propietario tiene potestad discrecional:
  - Puede optar por admitirlo ejecutando [`CU-06-marcar-inicio-navegacion.md`](CU-06-marcar-inicio-navegacion.md) $\rightarrow$ la reserva pasa a `IN_NAVIGATION` e inhabilita definitivamente el No-Show (FR-009 de CU-06).
  - Puede ejecutar este endpoint $\rightarrow$ la reserva transiciona a `CANCELLED` (`NO_SHOW`), y encolando la liberación asíncrona del barco a `AVAILABLE` de forma irreversible (vía CU-22).
- **Reloj y Zona Horaria Deterministas**: la ventana de 30 minutos se calcula obligatoriamente contra el huso horario oficial del puerto obtenido de Módulo 1 (CU-09), garantizando consistencia si el dispositivo del propietario se encuentra desfasado (FR-004).

---

### Trazabilidad FR/SC → Elemento del Contrato

| Requisito / Criterio | Descripción en Spec | Elemento de este Contrato |
|---|---|---|
| **FR-001** | Reporte exclusivo sobre reservas en estado `RESERVED` | Validación de estado y error `409 INVALID_RESERVATION_STATE` |
| **FR-002** | Validación estricta de identidad del Propietario | Chequeo de pertenencia reflejado en `403 PROFILE_NOT_AUTHORIZED` |
| **FR-003** | Ventana de tolerancia fijada en 30 minutos exactos | Regla de validación temporal y cómputo de `waiting_minutes` |
| **FR-004** | Evaluación en zona horaria oficial del puerto (CU-09) | Integración previa vía CU-09 documentada en Sección 1 |
| **FR-005** | Rechazo previo indicando tiempo faltante exacto | Error `409 GRACE_PERIOD_NOT_ELAPSED` con minutos/segundos faltantes |
| **FR-006** | Incompatibilidad con estados no reservables | Exclusión explícita de `INITIATED`, `PENDING_PAYMENT`, etc. |
| **FR-007** | Transición a `CANCELLED` con sub-estado `NO_SHOW` | Respuesta `200 OK` con `status: CANCELLED` y `cancellation_subtype` |
| **FR-008** | Registro de auditoría (evento No-Show) | Almacenamiento de fecha, minutos de espera y `observations` |
| **FR-009** | Regla estricta "Sin dinero" (cero cálculos monetarios) | Payload de respuesta sin campos monetarios |
| **FR-010** | Liberación de la embarcación a `AVAILABLE` en M1 | Notificación asíncrona respaldada por el [plan de CU-22](../CU-22-recibir-informacion-estado-operativo/plan.md) y por la cola de notificación definida en CU-22 |
| **FR-011** | Notificación de `NO_SHOW` a Módulo 3 | Publicación respaldada por el [plan de CU-14](../CU-14-recibir-estado-reserva/plan.md) |
| **FR-012** | Fail-safe si no se resuelve la zona horaria del puerto | Error `503 FLEET_SERVICE_UNAVAILABLE` |
| **SC-001** | 0% reportes aceptados antes de los 30 minutos | Validación cronológica estricta |
| **SC-002** | 0% reportes sobre estados inválidos | Matriz de validación de máquina de estados |
| **SC-003** | 100% de intentos anticipados informan tiempo restante | Campos `minutes_remaining` y `seconds_remaining` en error 409 |
| **SC-004** | Notificación externa encolada en < 1 segundo | Outbox transaccional, cola de notificación de CU-22 y SLA de actualización |
| **SC-005** | Cero operaciones de cálculo monetario en M2 | Neutralidad financiera del endpoint |
| **SC-006** | Cero reportes autorizados a no-propietarios | Control de claim de rol y pertenencia |
