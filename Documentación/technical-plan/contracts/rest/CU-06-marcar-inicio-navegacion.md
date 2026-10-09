# Contrato REST: Marcar Inicio de la Navegación (CU-06)

**Módulo**: Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones  
**Spec de referencia**: [`Documentación/features/CU-06-marcar-inicio-navegacion/spec.md`](../../features/CU-06-marcar-inicio-navegacion/spec.md)  
**Fecha**: 2026-10-09  

Este caso de uso expone la operación REST que formaliza el **check-in de salida y entrega del activo náutico** en muelle por parte del Propietario registrado (FR-001 a FR-015). Certifica que el Arrendatario ha abordado, que se inspeccionaron los elementos de seguridad y que la embarcación zarpa formalmente.

Al ejecutarse exitosamente:
1. Valida que la reserva exista y se encuentre estrictamente en estado **`Reservada`** (FR-001, FR-004).
2. Valida la legitimación: el usuario autenticado debe ser unívocamente el **Propietario registrado** de la embarcación (FR-002, SC-006).
3. Verifica la ventana de zarpe: la solicitud solo se habilita a partir de la **fecha y hora exacta de zarpe pactada** (sin antelación, FR-003). Si se intenta registrar anticipadamente, rechaza con `409 Conflict` indicando el tiempo faltante (FR-003, FR-011).
4. No existe límite posterior estricto: una vez alcanzada la hora de zarpe, permanece habilitado mientras la reserva siga en `Reservada`. Si el cliente llegó tarde (superando los 30 min de tolerancia) y el Propietario decide admitirlo en lugar de marcar inasistencia, este registro consuma el inicio del viaje (FR-009).
5. Invoca a `Actualizar estado reserva` (CU-08) transicionando el agregado al estado **`En Navegación`** (FR-005).
6. Desactiva de forma irreversible la potestad de ejecutar `CU-05 Marcar inasistencia` o `CU-04 Solicitar cancelación` (FR-009, SC-003).
7. Sincroniza las dependencias externas a través de CU-08:
   - Notifica a Módulo 1 ([`m1-asignar-estado-operativo.md`](../external/m1-asignar-estado-operativo.md)) actualizando la embarcación al estado **`En Navegación`** (FR-007, SC-002).
   - Publica el evento asíncrono en RabbitMQ ([`CU-14-estado-reserva.md`](../event/CU-14-estado-reserva.md)) hacia Módulo 3 informando el estado **`En Navegación`** para la **activación formal de la cobertura del seguro náutico** (FR-008, SC-004).
8. **Regla estricta "Sin dinero"**: Módulo 2 no gestiona cobros de combustible, retenciones de garantía ni importes adicionales en muelle (FR-010, SC-005).

---

## Endpoint — Registrar Check-in de Salida

### Método HTTP y URL

```http
POST /api/v1/reservas/{reservaId}/inicio-navegacion
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
| `reservaId` | string (UUID) | Sí | Identificador de la reserva en estado `Reservada` |

**Query Parameters**: No tiene.

**Body (JSON)**:

```json
{
  "hora_real_salida": "string (ISO 8601 timestamp opcional; si se omite, se asigna el instante actual del servidor)",
  "notas_entrega": "string (texto libre opcional con observaciones de la entrega en muelle)"
}
```

*Validaciones de entrada (FR-006, FR-013)*:
- `notas_entrega`: texto opcional (máx. 1000 caracteres).
- `hora_real_salida`: si se suministra, debe ser una marca temporal válida posterior o igual a la hora pactada de inicio.

---

### Elementos de la Respuesta (Response)

**Código de estado HTTP (éxito)**: `200 OK`

**Contrato de respuesta tipado**:

```json
{
  "reserva_id": "string (UUID)",
  "estado": "string (valor literal: 'En Navegación')",
  "fecha_inicio_real": "string (ISO 8601 timestamp con zona horaria del puerto)",
  "embarcacion_id": "string (UUID)",
  "embarcacion_estado_operativo": "string (valor literal: 'En Navegación')",
  "seguro_activado": "boolean (true)",
  "mensaje": "string (confirmación operativa del zarpe)"
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
curl -X POST "https://api.seashare.com/api/v1/reservas/e4f81c92-7a20-4215-9c5e-8812c3f1a001/inicio-navegacion" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.propietarioToken" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "hora_real_salida": "2026-11-15T09:05:00-05:00",
    "notas_entrega": "Inspección de chalecos completada. Pasajeros informados sobre rutas de seguridad. Embarcación zarpa de Marina Santa Marta en condiciones óptimas."
  }'
```

**Respuesta (`200 OK`)**:

```json
{
  "reserva_id": "e4f81c92-7a20-4215-9c5e-8812c3f1a001",
  "estado": "En Navegación",
  "fecha_inicio_real": "2026-11-15T09:05:00-05:00",
  "embarcacion_id": "d3b07384-d113-49cd-a5d6-812e9bcfc101",
  "embarcacion_estado_operativo": "En Navegación",
  "seguro_activado": true,
  "mensaje": "Inicio de navegación registrado exitosamente. La reserva ha pasado a En Navegación y la póliza de seguro náutico ha sido activada en Finanzas."
}
```

---

#### Ejemplo 2 — Inicio de Navegación tras Llegada Tardía (Renuncia al No-Show)

**Petición `curl`**:

```bash
curl -X POST "https://api.seashare.com/api/v1/reservas/e4f81c92-7a20-4215-9c5e-8812c3f1a001/inicio-navegacion" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.propietarioToken" \
  -H "Content-Type: application/json" \
  -d '{
    "notas_entrega": "El cliente arribó con 40 minutos de demora; se acordó salida efectiva manteniendo la hora de regreso estipulada."
  }'
```

**Respuesta (`200 OK`)**:

```json
{
  "reserva_id": "e4f81c92-7a20-4215-9c5e-8812c3f1a001",
  "estado": "En Navegación",
  "fecha_inicio_real": "2026-11-15T09:40:00-05:00",
  "embarcacion_id": "d3b07384-d113-49cd-a5d6-812e9bcfc101",
  "embarcacion_estado_operativo": "En Navegación",
  "seguro_activado": true,
  "mensaje": "Inicio de navegación registrado exitosamente tras llegada tardía. La opción de inasistencia ha quedado inhabilitada de forma permanente."
}
```

---

### Manejo de Errores

| Código | Caso | Cuerpo de respuesta (ejemplo) |
|---|---|---|
| `400 Bad Request` | `reservaId` no es un UUID válido | `{ "codigo": "ID_INVALIDO", "mensaje": "El identificador de reserva no es válido" }` |
| `401 Unauthorized` | Token ausente, inválido o expirado | `{ "codigo": "NO_AUTENTICADO", "mensaje": "Token de autenticación ausente o inválido" }` |
| `403 Forbidden` | El usuario autenticado no es el Propietario registrado de la embarcación (FR-002, SC-006) | `{ "codigo": "PERFIL_NO_AUTORIZADO", "mensaje": "Solo el propietario registrado de la embarcación puede marcar el inicio de la navegación" }` |
| `404 Not Found` | La reserva no existe en Módulo 2 | `{ "codigo": "RESERVA_NO_ENCONTRADA", "mensaje": "La reserva especificada no existe" }` |
| `409 Conflict` (Salida Anticipada) | Intento de registrar el zarpe antes de la fecha y hora pactada (FR-003, FR-011) | `{ "codigo": "SALIDA_ANTICIPADA", "mensaje": "El registro de salida solo se habilita a partir de la fecha y hora exacta programada para el zarpe (2026-11-15T09:00:00-05:00)" }` |
| `409 Conflict` (Estado Incompatible) | La reserva no está en estado `Reservada` (ej. está `Iniciada`, `Pendiente de Pago`, `Cancelada`, `Expirada`) (FR-004, SC-001) | `{ "codigo": "ESTADO_INCOMPATIBLE", "mensaje": "No se puede iniciar navegación en una reserva con estado actual: Pendiente de Pago" }` |
| `409 Conflict` (Viaje Ya Iniciado) | La reserva ya se encuentra en `En Navegación` (reintento duplicado) | `{ "codigo": "VIAJE_YA_INICIADO", "mensaje": "La navegación ya ha sido registrada previamente para esta reserva" }` |
| `500 Internal Server Error` | Excepción interna no controlada durante la actualización | `{ "codigo": "ERROR_INTERNO", "mensaje": "Ocurrió un error inesperado al registrar el inicio de navegación" }` |

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

- **Inhabilitación Irreversible de Inasistencia y Cancelación**: la consolidación del paso a `En Navegación` es atómica. Una vez asentado el estado, el sistema inhabilita de forma definitiva cualquier solicitud posterior de `CU-05 Marcar inasistencia` o `CU-04 Solicitar cancelación` (FR-009, SC-003).
- **Activación de Cobertura de Seguros en Módulo 3**: la emisión del evento a RabbitMQ garantiza que Módulo 3 active de inmediato la cobertura del seguro náutico por pasajero adquirido en la reserva (FR-008, SC-004).

---

### Trazabilidad FR/SC → Elemento del Contrato

| Requisito / Criterio | Descripción en Spec | Elemento de este Contrato |
|---|---|---|
| **FR-001** | Inicio exclusivo sobre reservas en estado `Reservada` | Validación de estado previo y error `409 ESTADO_INCOMPATIBLE` |
| **FR-002** | Validación estricta de identidad del Propietario | Chequeo de pertenencia reflejado en `403 PERFIL_NO_AUTORIZADO` |
| **FR-003** | Habilitación estricta a partir de la hora pactada (sin antelación) | Error `409 SALIDA_ANTICIPADA` |
| **FR-004** | Incompatibilidad con estados no permitidos | Matriz de errores tipados en la Sección 3 |
| **FR-005** | Transición a `En Navegación` mediante CU-08 | Respuesta `200 OK` con `estado: "En Navegación"` |
| **FR-006** | Registro de auditoría (hora real y notas de salida) | Campos `hora_real_salida` y `notas_entrega` del request |
| **FR-007** | Notificación de `En Navegación` a Módulo 1 | Integración respaldada por [`m1-asignar-estado-operativo.md`](../external/m1-asignar-estado-operativo.md) |
| **FR-008** | Notificación a Módulo 3 para activación de seguro | Evento AMQP respaldado por [`CU-14-estado-reserva.md`](../event/CU-14-estado-reserva.md) |
| **FR-009** | Desactivación permanente de No-Show y cancelación | Invariante técnica garantizada en Notas Transversales |
| **FR-010** | Regla "Sin dinero" (cero cobros en muelle) | Respuesta sin conceptos monetarios |
| **SC-001** | Cero inicios permitidos fuera de estado `Reservada` | Verificado en control previo de máquina de estados |
| **SC-002** | Notificación a M1 emitida en < 1 segundo | Outbox transaccional y SLA operativo |
| **SC-003** | Cero posibilidad de cancelar o No-Show tras inicio | Bloqueo definitivo documentado |
| **SC-004** | 100% de inicios notifican a M3 para activar póliza | Publicación garantizada en RabbitMQ |
| **SC-005** | Cero cobros monetarios ejecutados en Módulo 2 | Neutralidad financiera de la interfaz |
| **SC-006** | Cero registros autorizados a no-propietarios | Control estricto de identidad en JWT |
