# Contrato REST: Marcar Inicio de la Navegación (CU-06)

**Módulo**: Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones  
**Spec de referencia**: [`Documentación/features/CU-06-marcar-inicio-navegacion/spec.md`](../../features/CU-06-marcar-inicio-navegacion/spec.md)  
**Fecha**: 2026-10-09  

Este caso de uso expone la operación REST que formaliza el **check-in de salida y entrega del activo náutico** en muelle por parte del Propietario registrado (FR-001 a FR-015). Certifica que el Arrendatario ha abordado, que se inspeccionaron los elementos de seguridad y que la embarcación zarpa formalmente.

Al ejecutarse exitosamente:
1. Valida que la reserva exista y se encuentre estrictamente en estado **`RESERVED`** (FR-001, FR-004).
2. Valida la legitimación: el usuario autenticado debe ser unívocamente el **Propietario registrado** de la embarcación (FR-002, SC-006).
3. Verifica la ventana de zarpe: la solicitud solo se habilita a partir de la **fecha y hora exacta de zarpe pactada** (sin antelación, FR-003). Si se intenta registrar anticipadamente, rechaza con `409 Conflict` indicando el tiempo faltante (FR-003, FR-011).
4. Verifica que la embarcación no se encuentre en navegación activa por una travesía previa: comprueba que no exista otra reserva para el mismo `vessel_id` en estado **`IN_NAVIGATION`** que no haya realizado su check-out en muelle mediante `CU-07 Marcar fin de la navegación`. De existir, rechaza con HTTP `409 Conflict` (`VESSEL_CURRENTLY_IN_NAVIGATION`).
5. No existe límite posterior estricto: una vez alcanzada la hora de zarpe, permanece habilitado mientras la reserva siga en `RESERVED`. Si el cliente llegó tarde (superando los 30 min de tolerancia) y el Propietario decide admitirlo en lugar de marcar inasistencia, este registro consuma el inicio del viaje (FR-009).
6. Invoca a `Actualizar estado reserva` (CU-08) transicionando el agregado al estado **`IN_NAVIGATION`** (FR-005).
7. Desactiva de forma irreversible la potestad de ejecutar `CU-05 Marcar inasistencia` o `CU-04 Solicitar cancelación` (FR-009, SC-003).
8. Notifica de forma asíncrona a las dependencias externas a través de CU-08:
   - Encola en la cola de notificación a Módulo 1 definida en CU-22 (`Recibir información estado operativo`, invocado vía `<<include>>`) la señal de la embarcación al estado **`IN_NAVIGATION`** (FR-007, SC-002). El flujo no espera respuesta de Módulo 1 ni depende de ella: la entrega se garantiza con reintentos y backoff, DLQ e idempotencia por identificador único de evento (CU-22 FR-006), y un fallo de entrega no revierte la transición local de Módulo 2.
   - Publica el evento asíncrono en RabbitMQ ([plan de CU-14](../CU-14-recibir-estado-reserva/plan.md)) hacia Módulo 3 informando el estado **`IN_NAVIGATION`** para la **activación formal de la cobertura del seguro náutico** (FR-008, SC-004).
9. **Regla estricta "Sin dinero"**: Módulo 2 no gestiona cobros de combustible, retenciones de garantía ni importes adicionales en muelle (FR-010, SC-005).

---

## Endpoint — Registrar Check-in de Salida

### Método HTTP y URL

```http
POST /api/v1/reservations/{reservation_id}/navigation-start
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
  "actual_departure_at": "string (ISO 8601 timestamp opcional; si se omite, se asigna el instante actual del servidor)",
  "delivery_notes": "string (texto libre opcional con observaciones de la entrega en muelle)"
}
```

*Validaciones de entrada (FR-006, FR-013)*:
- `delivery_notes`: texto opcional (máx. 1000 caracteres).
- `actual_departure_at`: si se suministra, debe ser una marca temporal válida posterior o igual a la hora pactada de inicio.

---

### Elementos de la Respuesta (Response)

**Código de estado HTTP (éxito)**: `200 OK`

**Contrato de respuesta tipado**:

```json
{
  "reservation_id": "string (UUID)",
  "status": "string (valor literal: 'IN_NAVIGATION')",
  "actual_departure_at": "string (ISO 8601 timestamp con zona horaria del puerto)",
  "vessel_id": "string (UUID)",
  "vessel_operational_status": "string (valor literal: 'IN_NAVIGATION')",
  "insurance_activated": "boolean (true)",
  "message": "string (confirmación operativa del zarpe)"
}
```

**Headers de respuesta**:

| Nombre | Valor |
|---|---|
| `Content-Type` | `application/json` |

---

### Ejemplos de Petición y Respuestas Exitosas

#### Ejemplo 1 — Inicio de Navegación Puntual con Notas de Entrega

**Petición `curl`**:

```bash
curl -X POST "https://api.seashare.com/api/v1/reservations/e4f81c92-7a20-4215-9c5e-8812c3f1a001/navigation-start" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.propietarioToken" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "actual_departure_at": "2026-11-15T09:05:00-05:00",
    "delivery_notes": "Inspección de chalecos completada. Pasajeros informados sobre rutas de seguridad. Embarcación zarpa de Marina Santa Marta en condiciones óptimas."
  }'
```

**Respuesta (`200 OK`)**:

```json
{
  "reservation_id": "e4f81c92-7a20-4215-9c5e-8812c3f1a001",
  "status": "IN_NAVIGATION",
  "actual_departure_at": "2026-11-15T09:05:00-05:00",
  "vessel_id": "d3b07384-d113-49cd-a5d6-812e9bcfc101",
  "vessel_operational_status": "IN_NAVIGATION",
  "insurance_activated": true,
  "message": "Inicio de navegación registrado exitosamente. La reserva ha pasado a En Navegación y la póliza de seguro náutico ha sido activada en Finanzas."
}
```

---

#### Ejemplo 2 — Inicio de Navegación tras Llegada Tardía (Renuncia al No-Show)

**Petición `curl`**:

```bash
curl -X POST "https://api.seashare.com/api/v1/reservations/e4f81c92-7a20-4215-9c5e-8812c3f1a001/navigation-start" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.propietarioToken" \
  -H "Content-Type: application/json" \
  -d '{
    "delivery_notes": "El cliente arribó con 40 minutos de demora; se acordó salida efectiva manteniendo la hora de regreso estipulada."
  }'
```

**Respuesta (`200 OK`)**:

```json
{
  "reservation_id": "e4f81c92-7a20-4215-9c5e-8812c3f1a001",
  "status": "IN_NAVIGATION",
  "actual_departure_at": "2026-11-15T09:40:00-05:00",
  "vessel_id": "d3b07384-d113-49cd-a5d6-812e9bcfc101",
  "vessel_operational_status": "IN_NAVIGATION",
  "insurance_activated": true,
  "message": "Inicio de navegación registrado exitosamente tras llegada tardía. La opción de inasistencia ha quedado inhabilitada de forma permanente."
}
```

---

### Manejo de Errores

| Código | Caso | Cuerpo de respuesta (ejemplo) |
|---|---|---|
| `400 Bad Request` | `reservation_id` no es un UUID válido | `{ "code": "INVALID_UUID", "message": "El identificador de reserva no es válido" }` |
| `401 Unauthorized` | Token ausente, inválido o expirado | `{ "code": "UNAUTHENTICATED", "message": "Token de autenticación ausente o inválido" }` |
| `403 Forbidden` | El usuario autenticado no es el Propietario registrado de la embarcación (FR-002, SC-006) | `{ "code": "PROFILE_NOT_AUTHORIZED", "message": "Solo el propietario registrado de la embarcación puede marcar el inicio de la navegación" }` |
| `404 Not Found` | La reserva no existe en Módulo 2 | `{ "code": "RESERVATION_NOT_FOUND", "message": "La reserva especificada no existe" }` |
| `409 Conflict` (Salida Anticipada) | Intento de registrar el zarpe antes de la fecha y hora pactada (FR-003, FR-011) | `{ "code": "EARLY_DEPARTURE", "message": "El registro de salida solo se habilita a partir de la fecha y hora exacta programada para el zarpe (2026-11-15T09:00:00-05:00)" }` |
 | `409 Conflict` (Estado Incompatible) | La reserva no está en estado `RESERVED` (ej. está `INITIATED`, `PENDING_PAYMENT`, `CANCELLED`, `EXPIRED`) (FR-004, SC-001) | `{ "code": "INVALID_RESERVATION_STATE", "message": "No se puede iniciar navegación en una reserva con estado actual: Pendiente de Pago" }` |
| `409 Conflict` (Viaje Ya Iniciado) | La reserva ya se encuentra en `IN_NAVIGATION` (reintento duplicado) | `{ "code": "TRIP_ALREADY_STARTED", "message": "La navegación ya ha sido registrada previamente para esta reserva" }` |
| `409 Conflict` (Barco en Navegación Previa) | La travesía anterior de la misma embarcación no ha realizado el check-out en muelle (CU-07) | `{ "code": "VESSEL_CURRENTLY_IN_NAVIGATION", "message": "La embarcación se encuentra actualmente en navegación en otra reserva activa. Debe completarse el check-out en muelle de la travesía anterior antes de registrar un nuevo zarpe" }` |
| `500 Internal Server Error` | Excepción interna no controlada durante la actualización | `{ "code": "INTERNAL_ERROR", "message": "Ocurrió un error inesperado al registrar el inicio de navegación" }` |

---

### Paginación

No aplica. Operación puntual sobre una reserva individual.

---

### Seguridad y Perfiles

- Perfil autorizado: **Propietario**.
- **Regla estricta de legitimación**: el servicio valida que el claim `sub` del JWT coincida con el identificador del propietario registrado en Módulo 1 para la embarcación contratada (FR-002, SC-006). Arrendatarios u otros usuarios reciben `403 Forbidden`.
- **Regla "Sin dinero"**: Módulo 2 no cobra garantías ni ejecuta cargos monetarios durante el check-in (FR-010, SC-005).

---

### Notas Transversales

- **Inhabilitación Irreversible de Inasistencia y Cancelación**: la consolidación del paso a `IN_NAVIGATION` es atómica. Una vez asentado el estado, el sistema inhabilita de forma definitiva cualquier solicitud posterior de `CU-05 Marcar inasistencia` o `CU-04 Solicitar cancelación` (FR-009, SC-003).
- **Activación de Cobertura de Seguros en Módulo 3**: la emisión del evento a RabbitMQ garantiza que Módulo 3 active de inmediato la cobertura del seguro náutico por pasajero adquirido en la reserva (FR-008, SC-004).
- **Control de Salida sin Check-out Previo (CU-07)**: Para proteger la integridad física y operativa del activo, el sistema valida que no coexistan dos reservas de la misma embarcación en estado `IN_NAVIGATION`. Si una travesía previa aún no ha completado el check-out en muelle a través de `CU-07 Marcar fin de la navegación`, el inicio de la nueva reserva queda bloqueado con el error `409 VESSEL_CURRENTLY_IN_NAVIGATION` hasta que la anterior sea formalmente cerrada.

---

### Trazabilidad FR/SC → Elemento del Contrato

| Requisito / Criterio | Descripción en Spec | Elemento de este Contrato |
|---|---|---|
| **FR-001** | Inicio exclusivo sobre reservas en estado `RESERVED` | Validación de estado previo y error `409 INVALID_RESERVATION_STATE` |
| **FR-002** | Validación estricta de identidad del Propietario | Chequeo de pertenencia reflejado en `403 PROFILE_NOT_AUTHORIZED` |
| **FR-003** | Habilitación estricta a partir de la hora pactada (sin antelación) | Error `409 EARLY_DEPARTURE` |
| **FR-004** | Incompatibilidad con estados no permitidos y bloqueo si el activo sigue navegando en reserva previa (CU-07) | Matriz de errores (`409 INVALID_RESERVATION_STATE` y `409 VESSEL_CURRENTLY_IN_NAVIGATION`) |
| **FR-005** | Transición a `IN_NAVIGATION` mediante CU-08 | Respuesta `200 OK` con `status: "IN_NAVIGATION"` |
| **FR-006** | Registro de auditoría (hora real y notas de salida) | Campos `actual_departure_at` y `delivery_notes` del request |
| **FR-007** | Notificación de `IN_NAVIGATION` a Módulo 1 | Integración asíncrona respaldada por el [plan de CU-22](../CU-22-recibir-informacion-estado-operativo/plan.md) y por la cola de notificación definida en CU-22 |
| **FR-008** | Notificación a Módulo 3 para activación de seguro | Evento AMQP respaldado por el [plan de CU-14](../CU-14-recibir-estado-reserva/plan.md) |
| **FR-009** | Desactivación permanente de No-Show y cancelación | Invariante técnica garantizada en Notas Transversales |
| **FR-010** | Regla "Sin dinero" (cero cobros en muelle) | Respuesta sin conceptos monetarios |
| **SC-001** | Cero inicios permitidos fuera de estado `RESERVED` | Verificado en control previo de máquina de estados |
| **SC-002** | Notificación a M1 encolada en < 1 segundo | Outbox transaccional, cola de notificación de CU-22 y SLA operativo |
| **SC-003** | Cero posibilidad de cancelar o No-Show tras inicio | Bloqueo definitivo documentado |
| **SC-004** | 100% de inicios notifican a M3 para activar póliza | Publicación garantizada en RabbitMQ |
| **SC-005** | Cero cobros monetarios ejecutados en Módulo 2 | Neutralidad financiera de la interfaz |
| **SC-006** | Cero registros autorizados a no-propietarios | Control estricto de identidad en JWT |
