# Contrato REST: Marcar Fin de la Navegación (CU-07)

**Módulo**: Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones  
**Spec de referencia**: [`Documentación/features/CU-07-marcar-fin-navegacion/spec.md`](../../features/CU-07-marcar-fin-navegacion/spec.md)  
**Fecha**: 2026-10-09  

Este caso de uso expone la operación REST que formaliza el **check-out de retorno y cierre de la travesía náutica** en muelle por parte del Propietario registrado (FR-001 a FR-013). Certifica la devolución física del activo, registra la hora real de desembarque y permite reportar novedades de inspección preliminares.

Al ejecutarse exitosamente:
1. Valida que la reserva exista y se encuentre estrictamente en estado **`IN_NAVIGATION`** (FR-001, FR-009).
2. Valida la legitimación: el usuario autenticado debe ser unívocamente el **Propietario registrado** de la embarcación (FR-002, SC-005).
3. Permite al Propietario capturar un texto opcional de novedades con la descripción de posibles daños, faltantes o incumplimientos detectados al regreso (FR-003).
4. Invoca a `Actualizar estado reserva` (CU-08) transicionando la reserva al estado terminal **`COMPLETED`** y guardando la hora real de llegada (FR-004, FR-005).
5. Dispara internamente la apertura del ciclo de disputa invocando a `Generar disputa de garantía` (`CU-16`, `<<include>>`, FR-010): crea la entidad de disputa en estado `PENDING` y activa la **ventana fija de 24 horas** para que el Propietario pueda formalizar reclamos de daños si fuera necesario.
6. Sincroniza las dependencias externas a través de CU-08:
   - Notifica a Módulo 1 ([`m1-asignar-estado-operativo.md`](../external/m1-asignar-estado-operativo.md)) actualizando la embarcación al estado **`AVAILABLE`** (FR-006, SC-002).
   - Publica el evento asíncrono en RabbitMQ ([`CU-14-estado-reserva.md`](../event/CU-14-estado-reserva.md)) hacia Módulo 3 informando el estado **`COMPLETED`** y adjuntando el texto de novedades si fue provisto (FR-007). En consecuencia, **Módulo 3 libera de inmediato el pago del alquiler al Propietario, pero la garantía permanece retenida hasta la resolución de la disputa (ventana de 24 h)** (decisión arquitectónica H4 de plan.md).
7. **Regla estricta "Sin dinero"**: Módulo 2 no valora económicamente reparaciones, no cobra cargos por desembarque tardío ni descuenta fondos de la garantía (FR-008, SC-003).

---

## Endpoint — Registrar Check-out de Retorno

### Método HTTP y URL

```http
POST /api/v1/reservations/{reservation_id}/navigation-end
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
| `reservation_id` | string (UUID) | Sí | Identificador de la reserva en estado `IN_NAVIGATION` |

**Query Parameters**: No tiene.

**Body (JSON)**:

```json
{
  "actual_arrival_at": "string (ISO 8601 timestamp opcional; si se omite, se asigna el instante actual)",
  "remarks": "string (texto libre opcional con descripción de daños, fallas o faltantes detectados en muelle)"
}
```

*Validaciones de entrada (FR-003, FR-012)*:
- `remarks`: texto libre opcional (máx. 2000 caracteres).
- `actual_arrival_at`: si se envía, debe ser un timestamp válido con zona horaria.

---

### Elementos de la Respuesta (Response)

**Código de estado HTTP (éxito)**: `200 OK`

**Contrato de respuesta tipado**:

```json
{
  "reservation_id": "string (UUID)",
  "status": "string (valor literal: 'COMPLETED')",
  "actual_arrival_at": "string (ISO 8601 timestamp con zona horaria del puerto)",
  "vessel_id": "string (UUID)",
  "vessel_operational_status": "string (valor literal: 'AVAILABLE')",
  "dispute_id": "string (UUID generado por CU-16)",
  "dispute_window_expires_at": "string (ISO 8601 timestamp exactamente a las 24 horas del cierre)",
  "message": "string (confirmación operativa del cierre y apertura de la ventana de disputa)"
}
```

**Headers de respuesta**:

| Nombre | Valor |
|---|---|
| `Content-Type` | `application/json` |

---

### Ejemplos de Petición y Respuestas Exitosas

#### Ejemplo 1 — Cierre Ordinario Sin Novedades

**Petición `curl`**:

```bash
curl -X POST "https://api.seashare.com/api/v1/reservations/e4f81c92-7a20-4215-9c5e-8812c3f1a001/navigation-end" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.propietarioToken" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "actual_arrival_at": "2026-11-18T18:00:00-05:00"
  }'
```

**Respuesta (`200 OK`)**:

```json
{
  "reservation_id": "e4f81c92-7a20-4215-9c5e-8812c3f1a001",
  "status": "COMPLETED",
  "actual_arrival_at": "2026-11-18T18:00:00-05:00",
  "vessel_id": "d3b07384-d113-49cd-a5d6-812e9bcfc101",
  "vessel_operational_status": "AVAILABLE",
  "dispute_id": "c1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
  "dispute_window_expires_at": "2026-11-19T18:00:00-05:00",
  "message": "Fin de navegación registrado exitosamente. La reserva ha pasado a Completada, la embarcación quedó Disponible y se abrió la ventana de 24 horas para revisión de garantía."
}
```

---

#### Ejemplo 2 — Cierre con Novedades Reportadas en Muelle

**Petición `curl`**:

```bash
curl -X POST "https://api.seashare.com/api/v1/reservations/e4f81c92-7a20-4215-9c5e-8812c3f1a001/navigation-end" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.propietarioToken" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "actual_arrival_at": "2026-11-18T18:35:00-05:00",
    "remarks": "Desembarque con 35 minutos de retraso sobre la hora pactada. Se constató rotura menor en la escalerilla de baño de popa."
  }'
```

**Respuesta (`200 OK`)**:

```json
{
  "reservation_id": "e4f81c92-7a20-4215-9c5e-8812c3f1a001",
  "status": "COMPLETED",
  "actual_arrival_at": "2026-11-18T18:35:00-05:00",
  "vessel_id": "d3b07384-d113-49cd-a5d6-812e9bcfc101",
  "vessel_operational_status": "AVAILABLE",
  "dispute_id": "c1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
  "dispute_window_expires_at": "2026-11-19T18:35:00-05:00",
  "message": "Fin de navegación registrado con novedades. Las incidencias han sido notificadas a Finanzas y se habilitó la ventana de 24 horas para formalizar el reclamo de depósito de garantía."
}
```

---

### Manejo de Errores

| Código | Caso | Cuerpo de respuesta (ejemplo) |
|---|---|---|
| `400 Bad Request` | `reservation_id` no es un UUID válido | `{ "code": "INVALID_ID", "message": "El identificador de reserva no es válido" }` |
| `401 Unauthorized` | Token ausente, inválido o expirado | `{ "code": "UNAUTHENTICATED", "message": "Token de autenticación ausente o inválido" }` |
| `403 Forbidden` | El usuario autenticado no es el Propietario registrado de la embarcación (FR-002, SC-005) | `{ "code": "PROFILE_NOT_AUTHORIZED", "message": "Solo el propietario registrado de la embarcación puede marcar el fin de la navegación" }` |
| `404 Not Found` | La reserva no existe en Módulo 2 | `{ "code": "RESERVATION_NOT_FOUND", "message": "La reserva especificada no existe" }` |
| `409 Conflict` (Estado Incompatible) | La reserva no se encuentra en estado `IN_NAVIGATION` (ej. está `RESERVED`, `COMPLETED`, `CANCELLED`) (FR-009, SC-001) | `{ "code": "INCOMPATIBLE_STATE", "message": "No se puede finalizar la navegación en una reserva con estado actual: Reservada" }` |
| `409 Conflict` (Viaje Ya Cerrado) | La reserva ya fue cerrada previamente (operación terminal inmutable) | `{ "code": "TRIP_ALREADY_COMPLETED", "message": "La reserva ya se encuentra Completada y no admite nuevas modificaciones" }` |
| `500 Internal Server Error` | Error no controlado durante la consolidación del cierre | `{ "code": "INTERNAL_ERROR", "message": "Ocurrió un error inesperado al marcar el fin de navegación" }` |

---

### Paginación

No aplica. Operación puntual sobre una reserva individual.

---

### Seguridad y Perfiles

- Perfil autorizado: **Propietario**.
- **Regla estricta de legitimación**: el servicio valida que el claim `sub` del JWT coincida con el identificador del propietario registrado en Módulo 1 para la embarcación contratada (FR-002, SC-005). Arrendatarios u otros usuarios reciben `403 Forbidden`.
- **Regla "Sin dinero"**: Módulo 2 no valora daños ni deduce garantías en muelle (FR-008, SC-003).

---

### Notas Transversales

- **Retención de Garantía y Disputa Post-Viaje (H4, FR-010)**: al confirmarse el paso a `COMPLETED`, Módulo 3 dispersa el valor del alquiler al Propietario, pero retiene el depósito de garantía. Inicia la ventana fija de 24 horas controlada por `CU-16`, tras la cual, si el Propietario no registra reclamo, un job de barrido cierra la disputa y ordena a M3 la liberación total del depósito al Arrendatario.
- **Inmutabilidad del Estado Terminal**: una vez registrada la entrega, la reserva pasa a estado terminal definitivo; no puede reabrirse ni modificarse su fecha de desembarque.

---

### Trazabilidad FR/SC → Elemento del Contrato

| Requisito / Criterio | Descripción en Spec | Elemento de este Contrato |
|---|---|---|
| **FR-001** | Cierre exclusivo sobre reservas en estado `IN_NAVIGATION` | Validación de estado previo y error `409 INCOMPATIBLE_STATE` |
| **FR-002** | Validación estricta de identidad del Propietario | Chequeo de pertenencia reflejado en `403 PROFILE_NOT_AUTHORIZED` |
| **FR-003** | Campo de texto opcional para reporte de novedades | Campo `remarks` en request body |
| **FR-004** | Transición a `COMPLETED` mediante CU-08 | Respuesta `200 OK` con `status: "COMPLETED"` |
| **FR-005** | Registro de auditoría (hora real de entrega y novedades) | Campos `actual_arrival_at` y log del evento de entrega |
| **FR-006** | Liberación de la embarcación a `AVAILABLE` en M1 | Notificación respaldada por [`m1-asignar-estado-operativo.md`](../external/m1-asignar-estado-operativo.md) |
| **FR-007** | Notificación de `COMPLETED` a M3 (pago libre, garantía retenida) | Evento AMQP respaldado por [`CU-14-estado-reserva.md`](../event/CU-14-estado-reserva.md) |
| **FR-008** | Regla estricta "Sin dinero" (cero cálculo de daños o demoras) | Cero campos monetarios en la interfaz |
| **FR-009** | Rechazo sobre reservas que no estén `IN_NAVIGATION` | Matriz de errores tipados de la Sección 3 |
| **FR-010** | Apertura de ventana de disputa de 24 horas vía CU-16 | Campos devueltos `dispute_id` y `dispute_window_expires_at` |
| **SC-001** | Cero cierres permitidos fuera de `IN_NAVIGATION` | Validación estricta de máquina de estados |
| **SC-002** | Notificación a M1 para liberar barco en < 1 segundo | Outbox transaccional y SLA operativo |
| **SC-003** | Cero evaluaciones de daños o cobros ejecutados en M2 | Neutralidad financiera garantizada |
| **SC-004** | 100% de entregas registradas con hora real | Campo `actual_arrival_at` auditado |
| **SC-005** | Cero cierres autorizados a no-propietarios | Control estricto de legitimación por JWT |
