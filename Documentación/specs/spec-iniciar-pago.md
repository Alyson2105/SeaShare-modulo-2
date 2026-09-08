# Feature Specification: Iniciar Pago

**Módulo**: Módulo 2 (Operación de Reservas, Tiempos y Cancelaciones)  
**Created**: 2026-09-06  
**Primary Actor**: Arrendatario (Turista que reserva la embarcación)  
**External Dependencies (APIs)**:
- **Módulo 3 (Liquidación, Seguros y Dispersión de Fondos)**: Servicio financiero externo (consumido de manera indirecta a través del caso de uso subordinado `Recibir solicitud de pago`).
- **Casos de uso internos de Módulo 2**: `Recibir solicitud de pago` (`<<include>>`).

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - El Arrendatario confirma su intención de pagar dentro del tiempo límite (Priority: P1)

Habiendo creado una reserva en estado 'Pendiente de Pago', el Arrendatario decide formalizar su contratación y presiona la opción para proceder al pago ('Iniciar pago'). En ese instante, el sistema verifica que los 15 minutos de tolerancia continúen vigentes, que el usuario autenticado en la sesión coincida exactamente con la persona (turista) que realizó la reserva y que la reserva no haya sido confirmada, expirada o cancelada previamente. Al confirmar que todo está en regla, el sistema le pasa la solicitud a 'Recibir solicitud de pago' para que envíe la orden al Módulo 3 y se procese el cobro, garantizando que el Módulo 2 no toque datos bancarios ni realice cálculos de dinero.

**Why this priority**: Es el paso clave que le permite al cliente pasar de una reserva temporal a la confirmación de su viaje. Sin esta acción, el proceso se detiene y la reserva termina cancelándose automáticamente.

**Independent Test**: Se puede probar simulando una reserva en estado "Pendiente de Pago" a la que aún le quede tiempo en el reloj de 15 minutos, emitiendo la solicitud de iniciar pago por parte del Arrendatario titular, y verificando que el sistema confirma que el tiempo no ha vencido y delega la solicitud hacia "Recibir solicitud de pago" sin realizar cálculos de dinero ni conectarse con pasarelas de pago.

**Acceptance Scenarios**:

1. **Scenario**: Inicio de pago válido con tiempo de tolerancia vigente
    - **Given** una reserva en estado principal "Pendiente de Pago" con tiempo disponible en sus 15 minutos de tolerancia
    - **When** el Arrendatario titular solicita iniciar el pago
    - **Then** el sistema valida que el tiempo límite sigue activo, corrobora la titularidad de la reserva e invoca a "Recibir solicitud de pago" para transferir la intención de pago hacia Módulo 3

2. **Scenario**: Reintento de pago tras intento no completado dentro del tiempo límite
    - **Given** una reserva en estado "Pendiente de Pago" que aún dispone de 6 minutos en su reloj tras un intento previo no concretado
    - **When** el Arrendatario solicita nuevamente iniciar el pago
    - **Then** el sistema admite la solicitud por estar dentro del margen de 15 minutos e invoca a "Recibir solicitud de pago"

---

### User Story 2 - Rechazar el inicio de pago si los 15 minutos de tolerancia han expirado (Priority: P1)

Si el Arrendatario intenta iniciar el pago habiendo transcurrido más de 15 minutos desde la creación de la reserva (tiempo límite vencido), el sistema deniega la solicitud de forma inmediata, informando que el tiempo para efectuar el pago ha expirado y que la reserva ya no es válida.

**Why this priority**: Evita que se cobren reservas cuyo barco ya fue liberado o se encuentra disponible nuevamente para otros usuarios, previniendo cobros erróneos y sobreventas.

**Independent Test**: Se prueba ejecutando la acción de iniciar pago sobre una reserva cuyos 15 minutos de tolerancia han llegado a cero o que ya se encuentra en estado "Expirado", verificando que la acción se bloquea de inmediato, no se le pasa la solicitud a "Recibir solicitud de pago" y se muestra un mensaje explicativo al usuario.

**Acceptance Scenarios**:

1. **Scenario**: Intento de inicio de pago con tiempo límite expirado
    - **Given** una reserva creada hace más de 15 minutos cuyo tiempo de tolerancia se encuentra vencido
    - **When** el Arrendatario intenta iniciar el pago
    - **Then** el sistema rechaza la solicitud, informa que el tiempo de bloqueo temporal de 15 minutos ha expirado y no remite la petición a "Recibir solicitud de pago"

---

### User Story 3 - Rechazar inicio de pago sobre reservas no elegibles o por actores no autorizados (Priority: P2)

Si se intenta iniciar el pago de una reserva que ya se encuentra confirmada, cancelada o en navegación, o si la petición es enviada por un usuario diferente al Arrendatario que solicitó la reserva, el sistema rechaza la operación.

**Why this priority**: Asegura que no se procesen pagos duplicados para viajes ya confirmados y previene que personas ajenas intervengan en reservas de otros usuarios.

**Independent Test**: Se prueba emitiendo la acción desde una cuenta de usuario que no sea la titular y sobre reservas en estados "Confirmada" y "Cancelado", comprobando que el sistema retorna una denegación formal sin enviar órdenes al Módulo 3.

**Acceptance Scenarios**:

1. **Scenario**: Intento de pago sobre reserva ya confirmada
    - **Given** una reserva en estado principal "Confirmada"
    - **When** el Arrendatario intenta iniciar el pago nuevamente
    - **Then** el sistema deniega la acción informando que la reserva ya se encuentra pagada y confirmada

2. **Scenario**: Intento de pago por un usuario no titular
    - **Given** una reserva en estado principal "Pendiente de Pago" con tiempo límite vigente
    - **When** un usuario que no es el arrendatario registrado intenta iniciar el pago
    - **Then** el sistema rechaza la solicitud por falta de autorización

---

### Edge Cases

- **Inicio de pago en los últimos segundos de tolerancia**:
    - Si el Arrendatario presiona iniciar pago justo en el segundo final de los 15 minutos, el sistema evalúa la vigencia en el instante de recepción: si el tiempo aún es mayor a cero, admite la petición y la deriva inmediatamente a "Recibir solicitud de pago"; si expira en ese instante, prevalece la expiración de la reserva.
- **Múltiples clics simultáneos (doble envío de intención de pago)**:
    - Si el usuario presiona repetidamente el botón de pago por impaciencia, el sistema controla la concurrencia de la petición para canalizar una única solicitud activa hacia "Recibir solicitud de pago", evitando peticiones duplicadas.
- **Prohibición absoluta de cálculo o procesamiento de dinero en Módulo 2**:
    - En este caso de uso, el sistema **NO captura números de tarjeta, no interactúa con pasarelas de pago externas ni calcula montos, comisiones o recargos**. La pasarela, la captura de datos financieros y el cobro son responsabilidad exclusiva de Módulo 3.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE permitir al Arrendatario iniciar el proceso de pago de una reserva si y solo si la reserva existe y se encuentra en estado principal "Pendiente de Pago".
- **FR-002**: El sistema DEBE verificar que los quince (15) minutos de tiempo de tolerancia asociados a la reserva se encuentren vigentes al momento de la solicitud (`tiempo_restante_ttl > 0`).
- **FR-003**: Si el tiempo de 15 minutos ha expirado, el sistema DEBE rechazar el intento de pago, informar al Arrendatario que el tiempo límite de reserva expiró y NO DEBE transferir la solicitud al caso de uso subordinado ni a Módulo 3.
- **FR-004**: El sistema DEBE validar que el usuario autenticado sea estrictamente el Arrendatario titular registrado en la reserva.
- **FR-005**: Si la reserva se encuentra en cualquier estado distinto a "Pendiente de Pago" (incluyendo `Confirmada`, `Cancelado`, `Expirado`, `En Navegación` o `Completado`), el sistema DEBE denegar la solicitud informando el estado incompatible.
- **FR-006**: Al validar satisfactoriamente la solicitud y la vigencia del tiempo, el sistema DEBE invocar de forma obligatoria el caso de uso subordinado "Recibir solicitud de pago" (`<<include>>`), transfiriendo el identificador de la reserva y la identificación del arrendatario para que este canalice la petición a Módulo 3.
- **FR-007**: El sistema DEBE registrar un asiento auditable de la intención de pago, capturando: identificador de la reserva, identificador del arrendatario solicitante, tiempo remanente de tolerancia y marca temporal de la acción.
- **FR-008**: **REGLA DE NEGOCIO ESTRICTA (Sin cálculo financiero):** El sistema **NO DEBE capturar credenciales bancarias, procesar pasarelas de cobro ni calcular montos monetarios, comisiones o recargos**. La función de Módulo 2 se limita a certificar la vigencia del tiempo de reserva y canalizar la intención de pago hacia la interfaz de integración con Módulo 3.

---

### Key Entities

- **Reserva (`Reservation`)**: Entidad de Módulo 2 que debe encontrarse en estado "Pendiente de Pago" con temporizador de tolerancia activo.
- **Intención de Pago (`PaymentIntent`)**: Registro de dominio en Módulo 2 que documenta la voluntad expresa del arrendatario de proceder al pago. Atributos: identificador de intención, identificador de la reserva, identificador del arrendatario, marca temporal de emisión, tiempo remanente de tolerancia.
- **Arrendatario**: Usuario turista autenticado que posee la titularidad del alquiler temporal.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Cero (0%) intentos de pago admitidos o derivados a Módulo 3 para reservas cuyos 15 minutos de tolerancia hayan expirado.
- **SC-002**: Cero (0%) intentos de pago autorizados sobre reservas en estados no pendientes (`Confirmada`, `Cancelado`, `Expirado`, etc.).
- **SC-003**: El 100% de las solicitudes válidas de inicio de pago son verificadas y derivadas al caso de uso "Recibir solicitud de pago" en menos de 300 milisegundos.
- **SC-004**: Cero (0) operaciones financieras, cálculos de importes o procesamiento de tarjetas de crédito ejecutados dentro de Módulo 2.
- **SC-005**: Cero (0%) solicitudes de pago admitidas a usuarios que no correspondan al arrendatario titular de la reserva.