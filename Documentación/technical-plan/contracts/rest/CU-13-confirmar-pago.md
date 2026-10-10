# Contrato REST: Confirmar Pago (CU-13)
**Módulo:** Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones
**Spec de referencia:** [`Documentación/features/CU-13-confirmar-pago/spec.md`](../../features/CU-13-confirmar-pago/spec.md)
**Fecha:** 2026-10-09

Este caso de uso expone el punto de entrada REST mediante el cual Módulo 3 (Finanzas / pasarela de pagos) puede notificar a Módulo 2 el resultado de la transacción de cobro realizada por el Arrendatario (FR-001 a FR-008).

Canales de entrada del resultado de pago: la lógica de negocio de este caso de uso es única y recibe un mismo objeto de confirmación (`PaymentConfirmationPayload`) por dos canales:

- Canal A — Endpoint REST (FR-001): `POST /api/v1/reservas/{reservation_id}/confirmacion-pago`, definido en este contrato para el consumo de Módulo 3.
- Canal B — Adaptador de consulta a Módulo 3 (vigente hoy): Módulo 3 no cuenta actualmente con ningún contrato que invoque a Módulo 2; su contrato UC06 «Solicitar confirmación de pago» (`GET /api/v1/reservations/{reservation_id}/payment-confirmation`, ver `m3-confirmacion-pago.md`) es de consulta. Por eso un adaptador interno de Módulo 2 consulta periódicamente a Módulo 3 el estado del cobro de las reservas en Pendiente de Pago, traduce la respuesta al mismo `PaymentConfirmationPayload` y la entrega a la misma lógica que el Canal A. [NEEDS CLARIFICATION: acordar con Módulo 3 si mantendrá solo la consulta (Canal B) o si construirá además un cliente que invoque el Canal A. Mientras no lo haga, el Canal B es el único activo.]

**Al procesar la confirmación (por cualquiera de los dos canales):**

**Si el resultado es Aprobado y la reserva se encuentra en estado Pendiente de Pago dentro de la ventana de 15 minutos de Time-To-Live (TTL):**
- Cancela de forma inmediata el temporizador TTL (FR-004).
- Registra el identificador de la transacción externa y la fecha de confirmación para auditoría (FR-004).
- Invoca a Actualizar estado reserva (CU-08) para transicionar la reserva a Reservada (FR-004).

**Si el resultado es Rechazado o Fallido:**
- Registra el motivo del fallo en el historial de la reserva (FR-005).
- Si el rechazo admite reintento (resultado Rechazado) y resta tiempo en el TTL, mantiene la reserva en Pendiente de Pago permitiendo reintentos hasta el vencimiento estricto del TTL (FR-005).
- Ante un resultado definitivo sin reintento (resultado Fallido) invoca a CU-08 transicionando a Pago Fallido y liberando la embarcación en Módulo 1; al vencer el TTL sin aprobación la reserva transiciona a Expirada (FR-005).
- **Control de Idempotencia Estricto**: utiliza la clave única (external_transaction_id, result) por reserva para responder afirmativamente (HTTP 200 en el Canal A; sin efectos en el Canal B) ante reintentos sin duplicar transiciones ni efectos colaterales (FR-007, SC-004).
- **Condición de Carrera en el Límite del TTL**: si la confirmación de pago aprobada se recibe cuando el TTL ya expiró y la reserva se encuentra en Expirada, el sistema rechaza la confirmación e instruye mandatoriamente a Módulo 3 la reversión automática de los fondos en la pasarela (FR-006, SC-002, SC-003): en el Canal A mediante la respuesta 409 con reversal_required: true; en el Canal B mediante la publicación del estado EXPIRADA en el evento de estado de reserva (CU-14), que Módulo 3 debe reconocer para reembolsar [NEEDS CLARIFICATION: M3 debe agregar EXPIRADA a su enum reservation.status.changed].

## Endpoint — Notificación de Resultado de Pago

### Método HTTP y URL

POST /api/v1/reservas/{reservation_id}/confirmacion-pago

### Elementos de la Petición (Request)

**Headers:**

| Nombre | Obligatorio | Descripción |
| --- | --- | --- |
| Authorization | Sí | Bearer <token> — JWT firmado de identidad de servicio emitido para Módulo 3 [NEEDS CLARIFICATION] |
| Content-Type | Sí | application/json |
| Accept | No | application/json |

**Path Parameters:**

| Nombre | Tipo | Obligatorio | Descripción |
| --- | --- | --- | --- |
| reservation_id | string (UUID) | Sí | Identificador de la reserva en Módulo 2 sobre la cual se ejecutó el cobro |

**Query Parameters: No tiene.**

**Body (JSON):**

```json
{
  "external_transaction_id": "string (identificador unívoco de la pasarela/Módulo 3)",
  "result": "string (enum: 'Aprobado' | 'Rechazado' | 'Fallido')",
  "processed_at": "string (ISO 8601 timestamp)",
  "gateway_response_code": "string (opcional)",
  "rejection_reason": "string (opcional si result != 'Aprobado')"
}
```

**Validaciones de entrada (FR-002, FR-003):**

- external_transaction_id: no nulo ni vacío.
- result: debe coincidir con Aprobado, Rechazado o Fallido.
- processed_at: formato ISO 8601 válido.
- La reserva referenciada en reservation_id debe existir en Módulo 2.

### Elementos de la Respuesta (Response)

Código de estado HTTP (éxito): 200 OK

Contrato de respuesta tipado:

```json
{
  "reservation_id": "string (UUID)",
  "status": "string (Reservada | Pendiente de Pago | Pago Fallido | Expirada)",
  "processed": "boolean (true)",
  "message": "string (descripción operativa del resultado)",
  "reversal_required": "boolean (false si consolidó; true si venció TTL y M3 debe revertir)"
}
```

**Headers de respuesta:**

| Nombre | Valor |
| --- | --- |
| Content-Type | application/json |

### Ejemplos de Petición y Respuestas Exitosas

#### Ejemplo 1 — Pago Aprobado dentro del TTL (Transición a Reservada)

Petición curl:

```bash
curl -X POST "https://api.seashare.com/api/v1/reservas/e4f81c92-7a20-4215-9c5e-8812c3f1a001/confirmacion-pago" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.m3ServiceToken" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "external_transaction_id": "tx_pasarela_live_998877665544",
    "result": "Aprobado",
    "processed_at": "2026-10-09T12:08:45-05:00",
    "gateway_response_code": "AUTH_SUCCESS_00"
  }'
```

Respuesta (200 OK):

```json
{
  "reservation_id": "e4f81c92-7a20-4215-9c5e-8812c3f1a001",
  "status": "Reservada",
  "processed": true,
  "message": "Pago confirmado exitosamente. Reserva consolidada en estado Reservada.",
  "reversal_required": false
}
```

#### Ejemplo 2 — Notificación Duplicada Idempotente (Reintento de red de M3)

Petición curl:

```bash
curl -X POST "https://api.seashare.com/api/v1/reservas/e4f81c92-7a20-4215-9c5e-8812c3f1a001/confirmacion-pago" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.m3ServiceToken" \
  -H "Content-Type: application/json" \
  -d '{
    "external_transaction_id": "tx_pasarela_live_998877665544",
    "result": "Aprobado",
    "processed_at": "2026-10-09T12:08:45-05:00",
    "gateway_response_code": "AUTH_SUCCESS_00"
  }'
```

Respuesta (200 OK):

```json
{
  "reservation_id": "e4f81c92-7a20-4215-9c5e-8812c3f1a001",
  "status": "Reservada",
  "processed": true,
  "message": "Transacción previamente procesada de forma idempotente. Estado actual: Reservada.",
  "reversal_required": false
}
```

#### Ejemplo 3 — Pago Aprobado Extemporáneo (TTL Expirado / Requiere Reversión)

Petición curl:

```bash
curl -X POST "https://api.seashare.com/api/v1/reservas/7b1a2c3d-4e5f-6a7b-8c9d-0e1f2a3b4c5d/confirmacion-pago" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.m3ServiceToken" \
  -H "Content-Type: application/json" \
  -d '{
    "external_transaction_id": "tx_pasarela_late_11223344",
    "result": "Aprobado",
    "processed_at": "2026-10-09T12:21:00-05:00",
    "gateway_response_code": "AUTH_SUCCESS_00"
  }'
```

Respuesta (409 Conflict):

```json
{
  "code": "RESERVA_EXPIRADA_REVERSION_REQUERIDA",
  "message": "La confirmación de pago fue recibida tras el vencimiento estricto del TTL de 15 minutos. El activo fue liberado y no puede ser consolidado.",
  "reservation_id": "7b1a2c3d-4e5f-6a7b-8c9d-0e1f2a3b4c5d",
  "status": "Expirada",
  "reversal_required": true
}
```

#### Ejemplo 4 — Pago Rechazado con TTL Vigente (Permite Reintento)

Petición curl:

```bash
curl -X POST "https://api.seashare.com/api/v1/reservas/e4f81c92-7a20-4215-9c5e-8812c3f1a001/confirmacion-pago" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.m3ServiceToken" \
  -H "Content-Type: application/json" \
  -d '{
    "external_transaction_id": "tx_declined_55443322",
    "result": "Rechazado",
    "processed_at": "2026-10-09T12:10:00-05:00",
    "gateway_response_code": "ERR_INSUFFICIENT_FUNDS",
    "rejection_reason": "Fondos insuficientes en la tarjeta"
  }'
```

Respuesta (200 OK):

```json
{
  "reservation_id": "e4f81c92-7a20-4215-9c5e-8812c3f1a001",
  "status": "Pendiente de Pago",
  "processed": true,
  "message": "Intento de pago fallido registrado. La reserva continúa en Pendiente de Pago hasta el fin del TTL para permitir reintentos.",
  "reversal_required": false
}
```

### Manejo de Errores

| Código | Caso | Cuerpo de respuesta (ejemplo) |
| --- | --- | --- |
| 400 Bad Request | Payload mal formado, campos faltantes o reservation_id no es UUID válido (FR-002) | { "code": "PARAMETROS_INVALIDOS", "message": "La notificación no contiene los campos obligatorios de la transacción" } |
| 401 Unauthorized | Token de servicio de Módulo 3 ausente, inválido o expirado | { "code": "NO_AUTENTICADO", "message": "Token de identidad de servicio inválido o ausente" } |
| 403 Forbidden | El token no cuenta con la identidad autorizada de Módulo 3 | { "code": "PERFIL_NO_AUTORIZADO", "message": "Solo el servicio de Finanzas (Módulo 3) está autorizado para invocar este endpoint" } |
| 404 Not Found | La reserva no existe en Módulo 2 (Edge Case) | { "code": "RESERVA_NO_ENCONTRADA", "message": "La reserva especificada no existe en el sistema" } |
| 409 Conflict | Confirmación recibida sobre reserva ya expirada (requiere reversión automática en M3, FR-006) | { "code": "RESERVA_EXPIRADA_REVERSION_REQUERIDA", "message": "Reserva expirada por tiempo límite. Se requiere reversión automática de fondos en pasarela", "reversal_required": true } |
| 409 Conflict | Confirmación recibida en estado incompatible (ej. Pago Fallido, Completada) | { "code": "ESTADO_INCOMPATIBLE", "message": "La reserva se encuentra en un estado terminal que no admite confirmación de pago" } |
| 500 Internal Server Error | Falla de persistencia al invocar a CU-08 (error transitorio que habilita reintento de M3, Edge Case) | { "code": "ERROR_TRANSITORIO_PERSISTENCIA", "message": "No se pudo asentar la confirmación localmente; reintente la entrega" } |

### Canal B — Adaptador de Consulta a Módulo 3 (contrato UC06)

Este canal es un proceso interno de Módulo 2 (no expone endpoint) que alimenta la misma lógica del endpoint anterior con el resultado que obtiene de Módulo 3.

#### Disparador

Ejecución periódica cada 5 segundos (configurable) sobre las reservas en estado Pendiente de Pago con TTL vigente. Si no existen reservas en ese estado, no consulta.

#### Consulta a Módulo 3

GET /api/v1/reservations/{reservation_id}/payment-confirmation (ver m3-confirmacion-pago.md). Headers: Authorization (JWT de servicio de Módulo 2 [NEEDS CLARIFICATION]), Accept: application/json. Connect Timeout 100 ms, Read Timeout 500 ms, sin reintentos dentro del mismo ciclo.

#### Traducción de la respuesta de M3 al `PaymentConfirmationPayload`

| Campo del payload (Canal A) | Origen en la respuesta de M3 (Canal B) |  |
| --- | --- | --- |
| external_transaction_id | external_reference. Si es null (por ejemplo, rechazo sin referencia), el adaptador genera una referencia sintética estable m3-<reservation_id>-<status>-<intento> |  |
| result | Según la tabla de traducción siguiente |  |
| processed_at | Instante de la consulta que obtuvo el resultado (M3 no entrega marca temporal de procesamiento) |  |
| gateway_response_code | detail (opcional) |  |
| rejection_reason | detail (opcional, si el result no es Aprobado) |  |
| status de M3 | result traducido | Tratamiento |
| APROBADO | Aprobado | Se entrega a la lógica de FR-004 (TTL vigente) o FR-006 (TTL expirado) |
| RECHAZADO | Rechazado | FR-005: la reserva continúa en Pendiente de Pago permitiendo reintento con otro token (CU-03) hasta el fin del TTL |
| CANCELADO / EXPIRADO | Fallido | FR-005: resultado definitivo; transición a Pago Fallido y liberación de la embarcación en M1 |
| EN_PROCESO / DESCONOCIDO | (no se entrega) | No se invoca la lógica: la reserva permanece sin cambios y se vuelve a consultar en el siguiente ciclo; nunca se asume un resultado |

#### Reacción ante errores de Módulo 3 (Canal B)

| Respuesta de M3 | Reacción de Módulo 2 |
| --- | --- |
| 404 CHARGE_INTENT_NOT_FOUND | Esperado en los primeros segundos (M3 crea la intención de forma asíncrona tras el evento PENDIENTE). Reintenta en el siguiente ciclo |
| 500 / 503 / Timeout | Sin cambios de estado; reintenta en el siguiente ciclo (fail-safe: nunca se asume aprobado) |
| 401 / 403 | Alerta crítica de infraestructura; no cambia el estado |
| 400 | Alerta de integración; no reintentable |

#### Respuesta de la lógica en el Canal B

La lógica de negocio es la misma que en el Canal A, pero no existe un llamador HTTP al cual responder. Los resultados se reflejan así: el estado de la reserva queda consultable en CU-21 (el frontend consulta hasta ver Reservada, Pago Fallido o Expirada), y la instrucción de reversión de FR-006 se envía a Módulo 3 como el estado EXPIRADA del evento CU-14 más una alerta crítica de observabilidad.

#### Ejemplo 5 — Pago Aprobado detectado por consulta (Canal B)

Respuesta de M3 (200 OK) a la consulta del adaptador:

```json
{
  "reservation_id": "e4f81c92-7a20-4215-9c5e-8812c3f1a001",
  "status": "APROBADO",
  "detail": null,
  "authorized_amount": "10040000.00",
  "captured_amount": null,
  "released_amount": null,
  "charged_amount": null,
  "external_reference": "pg-8841"
}
```

El adaptador construye internamente el payload equivalente al Ejemplo 1 (external_transaction_id: "pg-8841", result: "Aprobado", processed_at: instante de la consulta) y lo entrega a la lógica: la reserva pasa a Reservada, se guarda pg-8841 y se publica el evento CU-14 con status RESERVADO. Los montos devueltos por M3 se ignoran para el negocio de M2 (solo auditoría).

### Paginación

No aplica. Operación puntual sobre una transacción de reserva.

### Seguridad y Perfiles

Autenticación Servicio a Servicio (Canal A): este endpoint está protegido estrictamente para ser invocado por el servicio backend de Módulo 3 mediante JWT de servicio (sub: "seashare-modulo-3" o similar).
No admite llamadas directas de usuarios ni del rol Arrendatario/Propietario.
Autenticación Servicio a Servicio (Canal B): la consulta hacia Módulo 3 usa el JWT de servicio de Módulo 2 (sub: "seashare-modulo-2" o similar).
Regla estricta "Sin dinero": Módulo 2 no calcula montos, no procesa reembolsos directamente ni interactúa con la pasarela bancaria. Módulo 3 es el único autor y operador financiero (FR-008, SC-005).

### Notas Transversales

Idempotencia Garantizada (FR-007): la clave compuesta (external_transaction_id, result) se registra en la tabla de auditoría de pagos de la reserva bajo restricción de unicidad. Ante recepciones duplicadas por reintentos de red de Módulo 3 (Canal A) o por ciclos repetidos del adaptador (Canal B), se responde 200 OK / no se genera ningún efecto sin reejecutar la máquina de estados. Los dos canales comparten esta restricción, por lo que no pueden procesar dos veces el mismo resultado.
Transición Atómica y Evento Saliente: al consolidarse el paso a Reservada, CU-08 dispara automáticamente el evento asíncrono CU-14-estado-reserva.md hacia Módulo 3 vía RabbitMQ (status RESERVADO) y notifica a Módulo 1 si correspondiera.
Reversión Obligatoria ante Carrera Expirada (SC-002, SC-003): el corte del TTL es riguroso a los 15 minutos exactos (segundo 900). Si la confirmación aprobada ingresa en el segundo 901 con la reserva ya expirada, el activo ya pudo haber sido alquilado por otro usuario; por tanto, M2 protege la integridad rechazando la consolidación y ordenando la reversión en M3. El bloqueo pesimista de fila decide cuál transacción gana: la expiración o la confirmación.
Convivencia de los canales: si Módulo 3 llega a invocar el Canal A mientras el adaptador (Canal B) también está activo, ambos entregan el mismo resultado a la misma lógica y la restricción de idempotencia evita efectos duplicados.

### Trazabilidad FR/SC → Elemento del Contrato

| Requisito / Criterio | Descripción en Spec | Elemento de este Contrato |
| --- | --- | --- |
| FR-001 | Punto de entrada accesible para Módulo 3 | Endpoint POST /api/v1/reservas/{reservation_id}/confirmacion-pago (Canal A); el Canal B alimenta la misma lógica |
| FR-002 | Validación de campos obligatorios de la transacción | Request body: external_transaction_id, result, processed_at; traducción de la respuesta de M3 en el Canal B |
| FR-003 | Verificación de existencia y estado Pendiente de Pago | Validaciones de backend reflejadas en errores 404 y 409; el adaptador solo consulta reservas en Pendiente de Pago |
| FR-004 | Aprobación dentro de TTL: cancelación de timer y paso a Reservada | Respuesta exitosa 200 OK con estado: "Reservada" (Ejemplos 1 y 5) |
| FR-005 | Manejo de rechazos/fallos y reintentos dentro del TTL | Respuesta 200 OK manteniendo Pendiente de Pago (Ejemplo 4) y tabla de traducción RECHAZADO / CANCELADO / EXPIRADO |
| FR-006 | Aprobación con TTL expirado instruye reversión automática | Error 409 Conflict con reversal_required: true (Ejemplo 3); en el Canal B, estado EXPIRADA en CU-14 |
| FR-007 | Idempotencia estricta por par (external_transaction_id, result) | Manejo de idempotencia documentado en Ejemplo 2 y Notas; compartida por ambos canales |
| FR-008 | Prohibición de cálculo monetario o llamadas a pasarelas | Contrato sin aritmética monetaria ni dependencias de pasarela |
| SC-001 | Transición a Reservada en < 1 segundo tras confirmación | Canal A: procesamiento síncrono. Canal B: hasta 5 segundos adicionales por el periodo del adaptador [NEEDS CLARIFICATION: la spec fija 1 segundo tras la recepción del resultado; en el Canal B la "recepción" ocurre en el ciclo de consulta, por lo que el criterio se cumple desde ese instante y no desde que M3 registra el resultado] |
| SC-002 | Cero reservas pasadas a Reservada con TTL expirado | Bloqueo estricto reflejado en código RESERVA_EXPIRADA_REVERSION_REQUERIDA y en la fila APROBADO con TTL vencido del Canal B |
| SC-003 | Cero cobros huérfanos sin instrucción de reversión | Bandera reversal_required: true (Canal A) y estado EXPIRADA en CU-14 (Canal B) |
| SC-004 | 100% de confirmaciones repetidas respondidas de forma idempotente | Garantizado por clave única de transacción en la Sección 2 y 3 |
| SC-005 | Cero cálculos monetarios en Módulo 2 | Neutralidad financiera de la interfaz |
