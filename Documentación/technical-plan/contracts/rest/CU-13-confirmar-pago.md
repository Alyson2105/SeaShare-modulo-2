# Contrato REST: Confirmar Pago (CU-13)

**Módulo**: Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones  
**Spec de referencia**: [`Documentación/features/CU-13-confirmar-pago/spec.md`](../../features/CU-13-confirmar-pago/spec.md)  
**Fecha**: 2026-10-09  

Este caso de uso expone el punto de entrada REST mediante el cual **Módulo 3 (Finanzas / pasarela de pagos)** notifica a Módulo 2 el resultado de la transacción de cobro realizada por el Arrendatario (FR-001 a FR-008).

Al procesar la confirmación:
1. Si el resultado es **`Aprobado`** y la reserva se encuentra en estado `Pendiente de Pago` dentro de la ventana de 15 minutos de Time-To-Live (TTL):
   - Cancela de forma inmediata el temporizador TTL (FR-004).
   - Registra el identificador de la transacción externa y la fecha de confirmación para auditoría (FR-004).
   - Invoca a `Actualizar estado reserva` (CU-08) para transicionar la reserva a **`Reservada`** (FR-004).
2. Si el resultado es **`Rechazado`** o **`Fallido`**:
   - Registra el motivo del fallo en el historial de la reserva (FR-005).
   - Si el rechazo admite reintento y resta tiempo en el TTL, mantiene la reserva en `Pendiente de Pago` permitiendo reintentos hasta el vencimiento estricto del TTL (FR-005).
   - Ante rechazo definitivo sin reintento, invoca a CU-08 transicionando a **`Pago Fallido`** y liberando la embarcación en Módulo 1 (FR-005).
3. **Control de Idempotencia Estricto**: utiliza la clave única `(id_transaccion_externo, resultado)` por reserva para responder afirmativamente (HTTP 200) ante reintentos de red de Módulo 3 sin duplicar transiciones ni efectos colaterales (FR-007, SC-004).
4. **Condición de Carrera en el Límite del TTL**: si la confirmación de pago llega cuando el TTL ya expiró y la reserva se encuentra en `Expirada`, el sistema rechaza la confirmación e instruye mandatoriamente a Módulo 3 la reversión automática de los fondos en la pasarela (FR-006, SC-002, SC-003).

---

## Endpoint — Notificación de Resultado de Pago

### Método HTTP y URL

```http
POST /api/v1/reservas/{reservaId}/confirmacion-pago
```

### Elementos de la Petición (Request)

**Headers**:

| Nombre | Obligatorio | Descripción |
|---|---|---|
| `Authorization` | Sí | `Bearer <token>` — JWT firmado de identidad de servicio emitido para Módulo 3 [NEEDS CLARIFICATION] |
| `Content-Type` | Sí | `application/json` |
| `Accept` | No | `application/json` |

**Path Parameters**:

| Nombre | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `reservaId` | string (UUID) | Sí | Identificador de la reserva en Módulo 2 sobre la cual se ejecutó el cobro |

**Query Parameters**: No tiene.

**Body (JSON)**:

```json
{
  "id_transaccion_externo": "string (identificador unívoco de la pasarela/Módulo 3)",
  "resultado": "string (enum: 'Aprobado' | 'Rechazado' | 'Fallido')",
  "timestamp_procesamiento": "string (ISO 8601 timestamp)",
  "codigo_respuesta_pasarela": "string (opcional)",
  "motivo_rechazo": "string (opcional si resultado != 'Aprobado')"
}
```

*Validaciones de entrada (FR-002, FR-003)*:
- `id_transaccion_externo`: no nulo ni vacío.
- `resultado`: debe coincidir con `Aprobado`, `Rechazado` o `Fallido`.
- `timestamp_procesamiento`: formato ISO 8601 válido.
- La reserva referenciada en `reservaId` debe existir en Módulo 2.

---

### Elementos de la Respuesta (Response)

**Código de estado HTTP (éxito)**: `200 OK`

**Contrato de respuesta tipado**:

```json
{
  "reserva_id": "string (UUID)",
  "estado": "string (Reservada | Pendiente de Pago | Pago Fallido | Expirada)",
  "procesado": "boolean (true)",
  "mensaje": "string (descripción operativa del resultado)",
  "reversion_requerida": "boolean (false si consolidó; true si venció TTL y M3 debe revertir)"
}
```

**Headers de respuesta**:

| Nombre | Valor |
|---|---|
| `Content-Type` | `application/json` |

---

### Ejemplos de Petición y Respuestas Exitosas

#### Ejemplo 1 — Pago Aprobado dentro del TTL (Transición a Reservada)

**Petición `curl`**:

```bash
curl -X POST "https://api.seashare.com/api/v1/reservas/e4f81c92-7a20-4215-9c5e-8812c3f1a001/confirmacion-pago" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.m3ServiceToken" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "id_transaccion_externo": "tx_pasarela_live_998877665544",
    "resultado": "Aprobado",
    "timestamp_procesamiento": "2026-10-09T12:08:45-05:00",
    "codigo_respuesta_pasarela": "AUTH_SUCCESS_00"
  }'
```

**Respuesta (`200 OK`)**:

```json
{
  "reserva_id": "e4f81c92-7a20-4215-9c5e-8812c3f1a001",
  "estado": "Reservada",
  "procesado": true,
  "mensaje": "Pago confirmado exitosamente. Reserva consolidada en estado Reservada.",
  "reversion_requerida": false
}
```

---

#### Ejemplo 2 — Notificación Duplicada Idempotente (Reintento de red de M3)

**Petición `curl`**:

```bash
curl -X POST "https://api.seashare.com/api/v1/reservas/e4f81c92-7a20-4215-9c5e-8812c3f1a001/confirmacion-pago" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.m3ServiceToken" \
  -H "Content-Type: application/json" \
  -d '{
    "id_transaccion_externo": "tx_pasarela_live_998877665544",
    "resultado": "Aprobado",
    "timestamp_procesamiento": "2026-10-09T12:08:45-05:00",
    "codigo_respuesta_pasarela": "AUTH_SUCCESS_00"
  }'
```

**Respuesta (`200 OK`)**:

```json
{
  "reserva_id": "e4f81c92-7a20-4215-9c5e-8812c3f1a001",
  "estado": "Reservada",
  "procesado": true,
  "mensaje": "Transacción previamente procesada de forma idempotente. Estado actual: Reservada.",
  "reversion_requerida": false
}
```

---

#### Ejemplo 3 — Pago Aprobado Extemporáneo (TTL Expirado / Requiere Reversión)

**Petición `curl`**:

```bash
curl -X POST "https://api.seashare.com/api/v1/reservas/7b1a2c3d-4e5f-6a7b-8c9d-0e1f2a3b4c5d/confirmacion-pago" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.m3ServiceToken" \
  -H "Content-Type: application/json" \
  -d '{
    "id_transaccion_externo": "tx_pasarela_late_11223344",
    "resultado": "Aprobado",
    "timestamp_procesamiento": "2026-10-09T12:21:00-05:00",
    "codigo_respuesta_pasarela": "AUTH_SUCCESS_00"
  }'
```

**Respuesta (`409 Conflict`)**:

```json
{
  "codigo": "RESERVA_EXPIRADA_REVERSION_REQUERIDA",
  "mensaje": "La confirmación de pago fue recibida tras el vencimiento estricto del TTL de 15 minutos. El activo fue liberado y no puede ser consolidado.",
  "reserva_id": "7b1a2c3d-4e5f-6a7b-8c9d-0e1f2a3b4c5d",
  "estado": "Expirada",
  "reversion_requerida": true
}
```

---

#### Ejemplo 4 — Pago Rechazado con TTL Vigente (Permite Reintento)

**Petición `curl`**:

```bash
curl -X POST "https://api.seashare.com/api/v1/reservas/e4f81c92-7a20-4215-9c5e-8812c3f1a001/confirmacion-pago" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.m3ServiceToken" \
  -H "Content-Type: application/json" \
  -d '{
    "id_transaccion_externo": "tx_declined_55443322",
    "resultado": "Rechazado",
    "timestamp_procesamiento": "2026-10-09T12:10:00-05:00",
    "codigo_respuesta_pasarela": "ERR_INSUFFICIENT_FUNDS",
    "motivo_rechazo": "Fondos insuficientes en la tarjeta"
  }'
```

**Respuesta (`200 OK`)**:

```json
{
  "reserva_id": "e4f81c92-7a20-4215-9c5e-8812c3f1a001",
  "estado": "Pendiente de Pago",
  "procesado": true,
  "mensaje": "Intento de pago fallido registrado. La reserva continúa en Pendiente de Pago hasta el fin del TTL para permitir reintentos.",
  "reversion_requerida": false
}
```

---

### Manejo de Errores

| Código | Caso | Cuerpo de respuesta (ejemplo) |
|---|---|---|
| `400 Bad Request` | Payload mal formado, campos faltantes o `reservaId` no es UUID válido (FR-002) | `{ "codigo": "PARAMETROS_INVALIDOS", "mensaje": "La notificación no contiene los campos obligatorios de la transacción" }` |
| `401 Unauthorized` | Token de servicio de Módulo 3 ausente, inválido o expirado | `{ "codigo": "NO_AUTENTICADO", "mensaje": "Token de identidad de servicio inválido o ausente" }` |
| `403 Forbidden` | El token no cuenta con la identidad autorizada de Módulo 3 | `{ "codigo": "PERFIL_NO_AUTORIZADO", "mensaje": "Solo el servicio de Finanzas (Módulo 3) está autorizado para invocar este endpoint" }` |
| `404 Not Found` | La reserva no existe en Módulo 2 (Edge Case) | `{ "codigo": "RESERVA_NO_ENCONTRADA", "mensaje": "La reserva especificada no existe en el sistema" }` |
| `409 Conflict` | Confirmación recibida sobre reserva ya expirada (requiere reversión automática en M3, FR-006) | `{ "codigo": "RESERVA_EXPIRADA_REVERSION_REQUERIDA", "mensaje": "Reserva expirada por tiempo límite. Se requiere reversión automática de fondos en pasarela", "reversion_requerida": true }` |
| `409 Conflict` | Confirmación recibida en estado incompatible (ej. `Pago Fallido`, `Completada`) | `{ "codigo": "ESTADO_INCOMPATIBLE", "mensaje": "La reserva se encuentra en un estado terminal que no admite confirmación de pago" }` |
| `500 Internal Server Error` | Falla de persistencia al invocar a CU-08 (error transitorio que habilita reintento de M3, Edge Case) | `{ "codigo": "ERROR_TRANSITORIO_PERSISTENCIA", "mensaje": "No se pudo asentar la confirmación localmente; reintente la entrega" }` |

---

### Paginación

No aplica. Operación puntual sobre una transacción de reserva.

---

### Seguridad y Perfiles

- **Autenticación Servicio a Servicio**: este endpoint está protegido estrictamente para ser invocado por el servicio backend de Módulo 3 mediante JWT de servicio (`sub: "seashare-modulo-3"` o similar).
- No admite llamadas directas de usuarios ni del rol Arrendatario/Propietario.
- **Regla estricta "Sin dinero"**: Módulo 2 **no** calcula montos, no procesa reembolsos directamente ni interactúa con la pasarela bancaria. Módulo 3 es el único autor y operador financiero (FR-008, SC-005).

---

### Notas Transversales

- **Idempotencia Garantizada (FR-007)**: la clave compuesta `(id_transaccion_externo, resultado)` se registra en la tabla de auditoría de pagos de la reserva bajo restricción de unicidad. Ante recepciones duplicadas por reintentos de red de Módulo 3, se retorna `200 OK` inmediatamente sin reejecutar la máquina de estados.
- **Transición Atómica y Evento Saliente**: al consolidarse el paso a `Reservada`, CU-08 dispara automáticamente el evento asíncrono [`CU-14-estado-reserva.md`](../event/CU-14-estado-reserva.md) hacia Módulo 3 vía RabbitMQ y notifica a Módulo 1 si correspondiera.
- **Reversión Obligatoria ante Carrera Expirada (SC-002, SC-003)**: el corte del TTL es riguroso a los 15 minutos exactos (segundo 900). Si la confirmación de Módulo 3 ingresa en el segundo 901 con la reserva ya expirada, el activo ya pudo haber sido alquilado por otro usuario; por tanto, M2 protege la integridad rechazando la consolidación y ordenando la reversión en M3.

---

### Trazabilidad FR/SC → Elemento del Contrato

| Requisito / Criterio | Descripción en Spec | Elemento de este Contrato |
|---|---|---|
| **FR-001** | Punto de entrada accesible para Módulo 3 | Endpoint `POST /api/v1/reservas/{reservaId}/confirmacion-pago` |
| **FR-002** | Validación de campos obligatorios de la transacción | Request body: `id_transaccion_externo`, `resultado`, `timestamp` |
| **FR-003** | Verificación de existencia y estado `Pendiente de Pago` | Validaciones de backend reflejadas en errores `404` y `409` |
| **FR-004** | Aprobación dentro de TTL: cancelación de timer y paso a Reservada | Respuesta exitosa `200 OK` con `estado: "Reservada"` |
| **FR-005** | Manejo de rechazos/fallos y reintentos dentro del TTL | Respuesta `200 OK` manteniendo `Pendiente de Pago` (Ejemplo 4) |
| **FR-006** | Aprobación con TTL expirado instruye reversión automática | Error `409 Conflict` con `reversion_requerida: true` (Ejemplo 3) |
| **FR-007** | Idempotencia estricta por par `(id_transaccion, resultado)` | Manejo de idempotencia documentado en Ejemplo 2 y Notas |
| **FR-008** | Prohibición de cálculo monetario o llamadas a pasarelas | Contrato sin aritmética monetaria ni dependencias de pasarela |
| **SC-001** | Transición a `Reservada` en < 1 segundo tras confirmación | SLA operativo garantizado por procesamiento síncrono |
| **SC-002** | Cero reservas pasadas a Reservada con TTL expirado | Bloqueo estricto reflejado en código `RESERVA_EXPIRADA_REVERSION_REQUERIDA` |
| **SC-003** | Cero cobros huérfanos sin instrucción de reversión | Bandera `reversion_requerida: true` retornada a Módulo 3 |
| **SC-004** | 100% de confirmaciones repetidas respondidas de forma idempotente | Garantizado por clave única de transacción en la Sección 2 y 3 |
| **SC-005** | Cero cálculos monetarios en Módulo 2 | Neutralidad financiera de la interfaz |
