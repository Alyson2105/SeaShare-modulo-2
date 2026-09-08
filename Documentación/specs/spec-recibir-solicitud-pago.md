# Feature Specification: Recibir Solicitud de Pago

**Módulo**: Módulo 2 (Operación de Reservas, Tiempos y Cancelaciones)  
**Created**: 2026-09-06  
**Primary Actor / Disparador**: Invocación interna desde el caso de uso `Iniciar pago` (`<<include>>`) / Interfaz de integración hacia Módulo 3  
**External Dependencies (APIs)**:
- **Módulo 3 (Liquidación, Seguros y Dispersión de Fondos)**: API externa `Procesar cobro y custodia de reserva` (punto de contacto al cual se le envía la orden para que Módulo 3 gestione el cobro con la pasarela bancaria y retenga el depósito de garantía).
- **Casos de uso internos de Módulo 2**: Invocado por `Iniciar pago` (`<<include>>`). ⚠️ No invoca a `Actualizar estado reserva` — este caso de uso solo lee el estado y el TTL de la Reserva directamente (es su propia entidad), sin pasar por el motor de transiciones, ya que aquí nunca se cambia el estado de la reserva.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Enviar la orden de cobro al Módulo 3 dentro del tiempo de espera de 15 minutos (Priority: P1)

Cuando el cliente presiona la opción para pagar desde 'Iniciar pago' (`<<include>>`), el sistema recibe la solicitud a través de esta función. El sistema comprueba que la reserva continúe en estado 'Pendiente de Pago' y que los 15 minutos de tolerancia para pagar sigan corriendo. Al confirmar que todo está en regla, el sistema reúne la información de la reserva (código de reserva, datos del cliente, código del barco y fechas del viaje) y le envía la orden de cobro a la API `Procesar cobro y custodia de reserva` de Módulo 3, manteniendo la reserva en estado 'Pendiente de Pago' mientras el cliente realiza el pago en la pasarela.

**Why this priority**: Es el canal de comunicación que conecta la reserva del cliente con el sistema financiero. Sin esta integración, la orden de pago no se le podría entregar al Módulo 3 para procesar el cobro.

**Independent Test**: Se prueba enviando una solicitud desde "Iniciar pago" para una reserva en "Pendiente de Pago" con tiempo disponible en el reloj de 15 minutos. Se utiliza un simulador de Módulo 3 y se verifica que la reserva no cambia de estado y que Módulo 3 recibe la información completa en menos de 500 milisegundos.

**Acceptance Scenarios**:

1. **Scenario**: Envío exitoso de la orden de cobro a Módulo 3
    - **Given** una reserva en estado "Pendiente de Pago" con tiempo disponible en los 15 minutos de tolerancia
    - **When** "Iniciar pago" invoca a "Recibir solicitud de pago"
    - **Then** el sistema confirma la validez de la reserva, organiza la información del cobro y la envía a la API `Procesar cobro y custodia de reserva` de Módulo 3, manteniendo la reserva en estado "Pendiente de Pago"

2. **Scenario**: Mantenimiento del estado de la reserva durante el envío
    - **Given** una reserva en estado "Pendiente de Pago" cuya orden de cobro se envía con éxito a Módulo 3
    - **When** la API de Módulo 3 recibe la solicitud para procesarla
    - **Then** el sistema NO cambia el estado de la reserva, manteniéndola en "Pendiente de Pago" hasta que Módulo 3 confirme el resultado final del pago a través del caso de uso "Confirmar pago"

---

### User Story 2 - Bloquear el envío al Módulo 3 si los 15 minutos de tolerancia han expirado (Priority: P1)

Si al momento de procesar la solicitud los 15 minutos de tolerancia ya se agotaron o la reserva cambió al estado "Expirado", el sistema detiene el proceso de inmediato, no envía la orden de cobro al Módulo 3 y le muestra al cliente un mensaje indicando que el tiempo para pagar ha vencido.

**Why this priority**: Evita enviar órdenes de cobro al sistema financiero por reservas cuyo tiempo ya venció y cuyo barco ya fue liberado para otros usuarios, previniendo cobros erróneos.

**Independent Test**: Se prueba enviando una solicitud de pago para una reserva cuyo tiempo de 15 minutos llegó a cero o está en estado "Expirado", verificando que no se envía ningún mensaje a Módulo 3 y se muestra una respuesta de rechazo por tiempo agotado.

**Acceptance Scenarios**:

1. **Scenario**: Orden de pago bloqueada por tiempo agotado
    - **Given** una reserva cuyo tiempo de 15 minutos venció en el instante en que llegó la solicitud
    - **When** el sistema verifica el tiempo de la reserva
    - **Then** el sistema rechaza la solicitud, NO consulta a la API de Módulo 3 e informa que el tiempo límite para pagar ha expirado

---

### User Story 3 - Manejar fallas o problemas de conexión con la API del Módulo 3 (Priority: P2)

Si al intentar enviar la orden de cobro la API de Módulo 3 no responde o presenta fallas de red, el sistema maneja el error de forma segura sin cancelar la reserva, manteniéndola en "Pendiente de Pago" para que el cliente pueda volver a intentar el pago si todavía le queda tiempo disponible dentro de sus 15 minutos.

**Why this priority**: Evita que una falla temporal en la pasarela de pagos cancele una reserva que todavía tiene tiempo válido para ser pagada.

**Independent Test**: Se prueba simulando una falla de conexión en la API de Módulo 3; se comprueba que la reserva permanece en estado "Pendiente de Pago", se registra la falla de red y se le muestra un mensaje al usuario indicándole que puede reintentar.

**Acceptance Scenarios**:

1. **Scenario**: Falla temporal de conexión con el Módulo 3
    - **Given** una reserva en "Pendiente de Pago" a la que aún le quedan 8 minutos de tolerancia
    - **When** el sistema intenta enviar la solicitud a Módulo 3 y la conexión falla
    - **Then** el sistema registra el problema de red, mantiene la reserva en "Pendiente de Pago" y le muestra un mensaje al cliente indicando que el servicio de pago está indispuesto momentáneamente

---

### Edge Cases

- **Prohibición de cambiar el estado de la reserva**:
    - "Recibir solicitud de pago" **NO cambia la reserva a ningún nuevo estado**. La reserva nace en `Pendiente de Pago` desde `Iniciar reserva` y se mantiene en `Pendiente de Pago` mientras el cliente paga. El cambio de estado a `Confirmada` ocurrirá únicamente después, cuando Módulo 3 complete el cobro en el banco y llame al caso de uso `Confirmar pago`.
- **Por qué este caso de uso NO invoca a "Actualizar estado reserva"**:
    - "Actualizar estado reserva" es el motor exclusivo de *transiciones* de la máquina de estados; su lista cerrada de invocadores (`Iniciar reserva`, `Confirmar pago`, `Marcar inicio de la navegación`, `Marcar fin de navegación`, `Solicitar cancelación`, `Marcar inasistencia`, el temporizador TTL) no incluye a "Recibir solicitud de pago", porque este caso de uso nunca cambia el estado de la reserva. En su lugar, "Recibir solicitud de pago" **lee directamente** el estado y el TTL de la Reserva (su propia entidad de Módulo 2) para validar si puede proceder, sin pasar por el orquestador de transiciones.
- **Entrega del enlace de la pasarela de pagos**:
    - Cuando Módulo 3 responde entregando el enlace o código de sesión de la pasarela de pagos, este caso de uso se lo entrega al cliente para que ingrese los datos de su tarjeta directamente en la plataforma segura del banco.
- **Prohibición de realizar cálculos de dinero en Módulo 2**:
    - Este proceso no calcula precios, tarifas, comisiones ni garantías. Toda la información de montos y el cobro como tal son responsabilidad exclusiva del Módulo 3.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE ofrecer una interfaz interna para recibir la solicitud de pago enviada por el caso de uso "Iniciar pago" (`<<include>>`).
- **FR-002**: El sistema DEBE verificar de forma obligatoria, leyendo directamente la entidad Reserva, que la reserva exista, esté en estado principal "Pendiente de Pago" y sus 15 minutos de tolerancia sigan vigentes (`tiempo_restante_ttl > 0`).
- **FR-003**: Si la reserva no está en "Pendiente de Pago" o los 15 minutos ya pasaron, el sistema DEBE rechazar la solicitud de inmediato y NO DEBE enviar información a Módulo 3.
- **FR-004**: El sistema **NO DEBE cambiar la reserva a ningún estado nuevo** dentro de este caso de uso; la reserva DEBE permanecer en "Pendiente de Pago" durante todo el envío a Módulo 3.
- **FR-005**: El sistema DEBE registrar un asiento de auditoría del intento de envío (independientemente de si tuvo éxito), sin invocar a "Actualizar estado reserva" — este caso de uso no transiciona el estado de la reserva, solo lo consulta.
- **FR-006**: Al confirmar que la reserva sigue vigente, el sistema DEBE organizar los datos del viaje (código de reserva, cliente, barco y fechas) y enviarlos a la API `Procesar cobro y custodia de reserva` de Módulo 3.
- **FR-007**: El sistema DEBE recibir la respuesta inicial de Módulo 3 (enlace o código de la pasarela de pagos) y entregársela al cliente para que realice el pago de forma segura.
- **FR-008**: Si la API de Módulo 3 no responde o presenta fallas de red, el sistema DEBE guardar el registro del problema y mantener la reserva en "Pendiente de Pago" mientras le quede tiempo en el reloj de 15 minutos.
- **FR-009**: **REGLA DE NEGOCIO ESTRICTA (Sin dinero):** El sistema **NO DEBE calcular precios, ni cobrar comisiones o realizar cobros monetarios directamente**. Todas las tarifas, impuestos y la conexión con la pasarela de pagos son responsabilidad del Módulo 3.
- **FR-010**: El sistema DEBE guardar un registro del envío realizado a Módulo 3, incluyendo: código de la reserva, fecha y hora de envío, tiempo restante de los 15 minutos y respuesta recibida de Módulo 3.

---

### Key Entities

- **Reserva (`Reservation`)**: Entidad de Módulo 2 cuyo estado "Pendiente de Pago" y TTL se leen directamente (sin pasar por "Actualizar estado reserva") y se mantienen sin cambios durante el envío.
- **Solicitud de Pago (`PaymentDispatchPayload`)**: Datos de la orden enviada a Módulo 3. Incluye: código de la reserva, código del cliente, código del barco, fechas del viaje y fecha/hora de envío.
- **Respuesta de Sesión de Pago (`PaymentSessionResponse`)**: Información enviada por Módulo 3. Incluye: confirmación de recepción, código de sesión y el enlace seguro para la pasarela de pago.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Cero (0%) órdenes de pago enviadas a Módulo 3 sobre reservas cuyos 15 minutos de tolerancia hayan expirado o que no estén en estado "Pendiente de Pago".
- **SC-002**: El 100% de las solicitudes válidas se procesan y envían a Módulo 3 en menos de 500 milisegundos desde su recepción.
- **SC-003**: Cero (0%) cambios de estado realizados en la reserva durante el envío a Módulo 3 (la reserva conserva su estado "Pendiente de Pago").
- **SC-004**: Cero (0) cálculos de precios, comisiones o manejo de dinero realizados dentro de Módulo 2.
- **SC-005**: El 100% de los envíos exitosos guardan un registro de auditoría con la hora exacta de la transacción.
- **SC-006**: El 100% de los enlaces de la pasarela de pagos entregados por Módulo 3 se le muestran al cliente sin modificar ningún parámetro de pago.
