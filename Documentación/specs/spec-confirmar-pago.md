# Feature Specification: Confirmar Pago

**Módulo**: Módulo 2 – Gestión de Reserva  
**Creado**: 2026-09-05  
**Actor primario / Disparador**: Módulo 3 – Liquidación, Seguros y Dispersión de Fondos (sistema externo que invoca este punto de entrada tras procesar el cobro contra la pasarela de pagos)  
**Dependencias externas (APIs)**:

- Módulo 3 – Liquidación, Seguros y Dispersión de Fondos (`Confirmar pago`: punto de entrada consumido por Módulo 3 para reportar el resultado de la transacción).
- Módulo 1 – Gestión de Embarcación (afectación indirecta a través de la invocación a "Actualizar estado reserva").

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - [Confirmar reserva tras aprobación exitosa del pago dentro del TTL] (Priority: P1)

Módulo 3 notifica a Módulo 2 que la transacción de pago para una reserva pendiente ha sido aprobada exitosamente dentro de los 15 minutos de Time-To-Live (TTL). El sistema cancela el temporizador de expiración, registra la constancia de pago e invoca "Actualizar estado reserva" para formalizar la reserva en estado "Confirmada", asegurando que el activo quede debidamente bloqueado.

**Why this priority**: Es la transición crítica que convierte una solicitud temporal de reserva en un contrato formal y confirmado. Sin la confirmación de pago, ninguna reserva puede consolidarse en la plataforma.

**Independent Test**: Se puede probar aislando el endpoint/servicio de entrada con una reserva en estado "Pendiente de Pago" cuyo temporizador TTL esté activo, enviando un payload de confirmación de pago exitoso desde Módulo 3, y verificando que el TTL se desactiva, la reserva pasa a "Confirmada" a través de "Actualizar estado reserva" y la referencia de pago queda registrada.

**Acceptance Scenarios**:

1. **Scenario**: Aprobación de pago dentro de la ventana de 15 minutos
   - **Given** una reserva existente en estado "Pendiente de Pago" con un temporizador TTL de 15 minutos activo (tiempo restante > 0)
   - **When** Módulo 3 invoca "Confirmar pago" indicando resultado "Aprobado" y un identificador de transacción válido
   - **Then** el sistema cancela inmediatamente el temporizador TTL de 15 minutos, invoca "Actualizar estado reserva" para transicionar la reserva a "Confirmada" y almacena el identificador de transacción y fecha de confirmación

2. **Scenario**: Persistencia de trazabilidad de la transacción
   - **Given** una confirmación de pago exitosa recibida desde Módulo 3
   - **When** el sistema procesa la confirmación
   - **Then** el sistema almacena la referencia externa de la transacción en el registro de la reserva para fines de auditoría sin intervenir ni consultar directamente a la pasarela de pagos

---

### User Story 2 - [Procesar notificación de pago rechazado o fallido] (Priority: P2)

Módulo 3 notifica que la transacción de pago fue rechazada por la pasarela (fondos insuficientes, tarjeta declinada, fallo de autenticación 3DS). El sistema registra el intento fallido y gestiona el ciclo de vida de la reserva según la política de reintentos definida.

**Why this priority**: Garantiza que los intentos de pago fallidos sean manejados de manera controlada, evitando que una reserva quede congelada en limbo y permitiendo al Arrendatario conocer el estado de su intento o liberar el inventario.

**Independent Test**: Se puede probar enviando una notificación de pago con resultado "Rechazado" para una reserva en estado "Pendiente de Pago" y verificando que el sistema actualiza el registro con el motivo devuelto por Módulo 3 sin marcar la reserva como confirmada.

**Acceptance Scenarios**:

1. **Scenario**: Pago rechazado con tiempo de TTL remanente
   - **Given** una reserva en estado "Pendiente de Pago" con tiempo remanente en su TTL de 15 minutos
   - **When** Módulo 3 notifica que el pago fue "Rechazado" con un motivo específico
   - **Then** el sistema registra el fallo del intento de pago [NEEDS CLARIFICATION: ¿el rechazo cancela inmediatamente la reserva y libera el activo, o mantiene el estado "Pendiente de Pago" permitiendo al Arrendatario reintentar el pago con otro medio antes de que expiren los 15 minutos?]

2. **Scenario**: Rechazo definitivo que culmina la reserva
   - **Given** una reserva cuyo intento de pago fue rechazado y no admite más reintentos (o agotó su TTL)
   - **When** se procesa el cierre del intento de pago
   - **Then** el sistema invoca "Actualizar estado reserva" para transicionar la reserva a "Pago Fallido" o "Expirada" y liberar la embarcación en Módulo 1

---

### User Story 3 - [Garantizar idempotencia ante notificaciones duplicadas de pago] (Priority: P2)

Si Módulo 3 reintenta la entrega del mensaje de confirmación de pago (por reintentos de red o desfasaje en la confirmación de recepción), el sistema procesa la solicitud de forma idempotente, reconociendo que el pago ya fue asentado sin generar inconsistencias de estado ni duplicar efectos colaterales.

**Why this priority**: En sistemas distribuidos con pasarelas de pago, los webhooks y llamadas de confirmación suelen entregarse bajo semántica *at-least-once*; la falta de idempotencia podría causar bloqueos o transiciones contradictorias.

**Independent Test**: Se puede probar enviando dos veces consecutivas la misma notificación de confirmación de pago con el mismo identificador de transacción y reserva, validando que la primera solicitud transiciona la reserva a "Confirmada" y la segunda responde con éxito sin reejecutar transiciones ni corromper estados.

**Acceptance Scenarios**:

1. **Scenario**: Recepción de confirmación duplicada para reserva ya confirmada
   - **Given** una reserva que ya se encuentra en estado "Confirmada" con una referencia de transacción T_123
   - **When** Módulo 3 reenvía la confirmación de pago con la misma referencia T_123
   - **Then** el sistema responde con confirmación exitosa a Módulo 3, no dispara nuevas transiciones de estado ni invoca nuevamente a Módulo 1

---

### Edge Cases

- **Confirmación de pago recibida tras la expiración del TTL (Race Condition)**: el temporizador de 15 minutos expira y "Actualizar estado reserva" marca la reserva como "Expirada" liberando la embarcación. Milisegundos o segundos después, llega la confirmación de pago de Módulo 3 reportando "Aprobado". ¿Cómo procede el sistema?
  - *Regla*: Módulo 2 NO PUEDE confirmar una reserva expirada cuyo inventario pudo haber sido tomado por otro usuario. El sistema DEBE rechazar la confirmación indicando "Reserva expirada por TTL" y notificar inmediatamente a Módulo 3 para que proceda con la reversión/reembolso automático del cobro en la pasarela de pagos.
  - [NEEDS CLARIFICATION: ¿existe una ventana de gracia técnica (p. ej. 30 segundos) posterior al minuto 15 para admitir pagos en tránsito, o el corte es estrictamente al segundo 900?]
- **Confirmación de pago sobre reserva cancelada**: el Arrendatario solicita cancelar la reserva mientras el pago estaba en proceso. Si llega la confirmación de pago posterior a la cancelación, el sistema debe rechazar la confirmación y requerir a Módulo 3 la reversión del importe.
- **Payload con datos incompletos o reserva inexistente**: si Módulo 3 envía una confirmación con un identificador de reserva que no existe en Módulo 2 o sin estado de transacción, el sistema DEBE responder con error de validación (400/Unprocessable Entity) sin alterar ningún registro.
- **Falla en el caso de uso subordinado "Actualizar estado reserva"**: si al invocar "Actualizar estado reserva" ocurre una falla de persistencia, la confirmación de pago NO debe considerarse completada y el sistema debe devolver un error transitorio a Módulo 3 para habilitar su reintento según el protocolo de integración.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE exponer un punto de entrada accesible para Módulo 3 que reciba la notificación del resultado del cobro de una reserva.
- **FR-002**: El sistema DEBE validar que la notificación contenga el identificador de la reserva, el resultado de la transacción ("Aprobado", "Rechazado" o "Fallido"), el identificador único de transacción externa y la marca temporal de procesamiento.
- **FR-003**: El sistema DEBE verificar que la reserva exista en Módulo 2 y se encuentre en estado "Pendiente de Pago".
- **FR-004**: Si el resultado de la transacción es **Aprobado** y la reserva está dentro de su ventana de TTL de 15 minutos:
  - El sistema DEBE cancelar inmediatamente el temporizador de expiración TTL de 15 minutos asociado a la reserva.
  - El sistema DEBE registrar en la reserva el identificador de la transacción externa provisto por Módulo 3 y la marca temporal de la confirmación.
  - El sistema DEBE invocar el caso de uso "Actualizar estado reserva" (vía `<<include>>`) para transicionar el estado de la reserva a "Confirmada".
- **FR-005**: Si el resultado de la transacción es **Rechazado** o **Fallido**:
  - El sistema DEBE registrar el resultado fallido y el motivo en el historial de la reserva.
  - [NEEDS CLARIFICATION: definir si el sistema invoca inmediatamente "Actualizar estado reserva" para marcar "Pago Fallido/Cancelada" y liberar la embarcación en Módulo 1, o si se mantiene "Pendiente de Pago" hasta el agotamiento del TTL para permitir reintento de pago].
- **FR-006**: Si el sistema recibe una notificación de pago **Aprobado** cuando el temporizador TTL ya expiró y la reserva se encuentra en estado "Expirada":
  - El sistema NO DEBE transicionar la reserva a "Confirmada".
  - El sistema DEBE responder a Módulo 3 con un código/mensaje de rechazo indicando que la reserva expiró por tiempo límite.
  - El sistema DEBE solicitar/instruir a Módulo 3 la reversión automática de los fondos cobrados al Arrendatario en la pasarela.
- **FR-007**: El sistema DEBE ser estrictamente idempotente: si recibe una notificación con un identificador de transacción y estado idénticos a una confirmación previamente procesada para la misma reserva, DEBE responder afirmativamente sin repetir transiciones de estado ni generar nuevas llamadas colaterales.
- **FR-008**: El sistema **NO DEBE realizar cálculos de montos, cobros directos, retenciones de depósitos de garantía ni comunicarse directamente con pasarelas de pago**; toda esa operación es de exclusiva competencia de Módulo 3.

### Key Entities

- **Reserva (`Reservation`)**: Entidad central de Módulo 2. Campos afectados: estado, ttl_expira_en, id_transaccion_pago, fecha_confirmacion_pago.
- **Notificación de Confirmación de Pago (`PaymentConfirmationPayload`)**: Objeto de integración provisto por Módulo 3. Atributos: reserva_id, resultado (Aprobado / Rechazado / Fallido), id_transaccion_externo, timestamp_procesamiento, codigo_respuesta_pasarela, motivo_rechazo (opcional).

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de las notificaciones de pago aprobado recibidas dentro del TTL transicionan la reserva a "Confirmada" y cancelan el temporizador en menos de 1 segundo tras la recepción del evento.
- **SC-002**: Cero (0%) reservas confirmadas de forma extemporánea cuando el TTL de 15 minutos ya ha expirado y el activo ha sido liberado.
- **SC-003**: Cero (0%) discrepancias de cobro huérfano sin notificación de reversión enviada a Módulo 3 ante condiciones de carrera en el límite del TTL.
- **SC-004**: El 100% de las confirmaciones repetidas o duplicadas por reintentos de red son respondidas de forma idempotente sin corromper el estado de la reserva ni generar dobles bloqueos en Módulo 1.
- **SC-005**: Cero (0%) operaciones de liquidación, cálculo monetario o llamadas directas a pasarelas de pago originadas en Módulo 2.
