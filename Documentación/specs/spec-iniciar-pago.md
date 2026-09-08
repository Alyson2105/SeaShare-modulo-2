# Feature Specification: Iniciar Pago

**Módulo**: Módulo 2 (Operación de Reservas, Tiempos y Cancelaciones)  
**Created**: 2026-09-06 (Actualizado: 2026-09-08)  
**Primary Actor**: Arrendatario (Turista que reserva la embarcación)  
**External Dependencies (APIs)**:
- **Módulo 3 (Liquidación, Seguros y Dispersión de Fondos)**: API externa de Módulo 3 (`Recibir solicitud de pago` / `Procesar cobro`). Módulo 2 le transfiere los datos identificadores de la reserva para que Módulo 3 tome el control del flujo financiero y procese el cobro. Módulo 2 nunca gestiona pasarelas, enlaces de pago, tarjetas ni cifras de dinero.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Transferir la solicitud de pago a Módulo 3 dentro del tiempo límite (Priority: P1)

Habiendo creado una reserva en estado "Pendiente de Pago", el Arrendatario titular decide formalizar su contratación y presiona la opción para proceder al pago ("Iniciar pago"). El sistema verifica que el temporizador de 15 minutos (TTL) continúe vigente, corrobora la titularidad del usuario y que la reserva esté en estado "Pendiente de Pago". Al validar que todo está en regla, el sistema recopila los identificadores de la reserva y transfiere la solicitud de pago a Módulo 3 (`Recibir solicitud de pago`) para que este módulo gestione el cobro correspondiente, manteniendo la reserva en estado "Pendiente de Pago".

**Why this priority**: Es el paso que conecta la reserva temporal con el inicio del proceso de cobro en el sistema financiero. Sin esta acción, el proceso se detiene y la reserva expira automáticamente al agotarse el tiempo.

**Independent Test**: Se puede probar simulando una reserva en estado "Pendiente de Pago" con tiempo disponible en el temporizador de 15 minutos, solicitando el inicio de pago por el Arrendatario titular y verificando que el sistema valida la vigencia, transfiere con éxito la petición a Módulo 3 y mantiene la reserva en "Pendiente de Pago" a la espera de la confirmación financiera posterior.

**Acceptance Scenarios**:

1. **Scenario**: Transferencia exitosa de la solicitud de pago a Módulo 3
    - **Given** una reserva en estado "Pendiente de Pago" con tiempo disponible en sus 15 minutos de tolerancia y un Arrendatario titular autenticado
    - **When** el Arrendatario solicita iniciar el pago
    - **Then** el sistema valida la vigencia del tiempo, transfiere la solicitud de pago con los identificadores de la reserva a la API de Módulo 3 y mantiene la reserva en estado "Pendiente de Pago"

2. **Scenario**: Bloqueo del inicio de pago por tiempo de tolerancia (TTL) expirado
    - **Given** una reserva cuyo temporizador de 15 minutos ha llegado a cero o se encuentra en estado "Expirada"
    - **When** el Arrendatario intenta iniciar el pago
    - **Then** el sistema rechaza la solicitud, informa que el tiempo límite para efectuar el pago ha vencido y NO transfiere ninguna petición a Módulo 3

3. **Scenario**: Reintento de solicitud de pago dentro del tiempo límite
    - **Given** una reserva en estado "Pendiente de Pago" a la que aún le restan minutos de tolerancia tras un intento previo no concretado
    - **When** el Arrendatario solicita nuevamente iniciar el pago
    - **Then** el sistema admite la solicitud por encontrarse dentro del margen de tiempo de los 15 minutos y transfiere nuevamente la solicitud a Módulo 3

---

### User Story 2 - Rechazar inicio de pago sobre reservas no elegibles o por actores no autorizados (Priority: P1)

Si se intenta iniciar el pago de una reserva que ya se encuentra confirmada, cancelada o en navegación, o si la petición es enviada por un usuario diferente al Arrendatario que solicitó la reserva, el sistema rechaza la operación inmediatamente sin comunicarse con Módulo 3.

**Why this priority**: Evita envíos duplicados sobre contratos ya pagados y protege la privacidad e integridad de las reservas frente a terceros no autorizados.

**Independent Test**: Se prueba emitiendo la acción de inicio de pago desde una cuenta no titular y sobre reservas en estados "Confirmada", "Cancelada" o "En Navegación", verificando que el sistema deniega formalmente la petición sin contactar a Módulo 3.

**Acceptance Scenarios**:

1. **Scenario**: Intento de pago sobre reserva ya confirmada o en estado incompatible
    - **Given** una reserva en estado "Confirmada", "Cancelada" o "En Navegación"
    - **When** el Arrendatario intenta iniciar el pago
    - **Then** el sistema rechaza la solicitud informando que el estado actual de la reserva no admite pagos y no contacta a Módulo 3

2. **Scenario**: Intento de pago por un usuario no titular
    - **Given** una reserva en estado "Pendiente de Pago" con tiempo vigente
    - **When** un usuario que no es el Arrendatario registrado en la reserva intenta iniciar el pago
    - **Then** el sistema deniega la solicitud por falta de autorización

---

### User Story 3 - Manejar fallas de conexión o indisponibilidad con la API de Módulo 3 (Priority: P2)

Si al intentar transferir la solicitud de pago la API de Módulo 3 no responde o presenta una falla de comunicación, el sistema gestiona el error de forma segura sin cancelar ni expirar la reserva, manteniéndola en "Pendiente de Pago" para que el cliente pueda reintentar mientras continúe vigente su temporizador de 15 minutos.

**Why this priority**: Asegura la resiliencia operativa evitando que un problema transitorio de comunicación aborte prematuramente una reserva que aún tiene tiempo de pago disponible.

**Independent Test**: Se prueba simulando una falla de conexión al comunicarse con la API de Módulo 3 para una reserva válida con tiempo restante; se comprueba que el sistema registra el fallo, mantiene la reserva intacta en "Pendiente de Pago" y le muestra un mensaje claro al usuario indicando que puede volver a intentar.

**Acceptance Scenarios**:

1. **Scenario**: Falla temporal de conexión con Módulo 3
    - **Given** una reserva en estado "Pendiente de Pago" a la que aún le resta tiempo en su temporizador de 15 minutos
    - **When** el sistema intenta comunicarse con la API de Módulo 3 y la conexión falla o agota el tiempo de espera
    - **Then** el sistema registra la incidencia, mantiene la reserva en estado "Pendiente de Pago" e informa al Arrendatario que el servicio está temporalmente indispuesto y puede reintentar

---

### Edge Cases

- **Inicio de pago en los últimos instantes del temporizador**: Si el Arrendatario solicita el pago justo en el límite de los 15 minutos, el sistema evalúa la vigencia en el instante exacto de la petición: si el tiempo restante es mayor a cero, admite la solicitud y transfiere la petición a Módulo 3; si la reserva ya fue transicionada a "Expirada", prevalece la expiración y se rechaza la solicitud.
- **Múltiples solicitudes simultáneas (doble clic del usuario)**: Si el usuario presiona repetidamente el botón de pago, el sistema canaliza una única solicitud activa hacia Módulo 3 para evitar envíos duplicados.
- **Mantenimiento estricto del estado de la reserva**: Este caso de uso **NO cambia la reserva a estado "Confirmada" ni a ningún otro estado**. La reserva permanece en "Pendiente de Pago"; la transición a "Confirmada" ocurrirá únicamente cuando Módulo 3 complete el cobro y llame al caso de uso "Confirmar pago".
- **Separación total de responsabilidades financieras**: Módulo 2 **no procesa pasarelas, ni maneja enlaces de pago, datos de tarjetas de crédito o cálculo de montos/comisiones**. Módulo 2 únicamente valida la vigencia del tiempo y notifica la intención de pago a Módulo 3, quien asume por completo la interacción financiera.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE permitir al Arrendatario solicitar el inicio de pago de una reserva si y solo si la reserva existe y se encuentra en estado "Pendiente de Pago".
- **FR-002**: El sistema DEBE verificar que el usuario autenticado corresponda estrictamente al Arrendatario titular asociado a la reserva.
- **FR-003**: El sistema DEBE verificar que el temporizador de quince (15) minutos (TTL) asociado a la reserva se encuentre vigente al momento de la solicitud (`tiempo_restante_ttl > 0`).
- **FR-004**: Si el temporizador de 15 minutos ha expirado o la reserva no está en estado "Pendiente de Pago", el sistema DEBE denegar la solicitud de inmediato, informar el motivo al usuario y NO DEBE transferir información a Módulo 3.
- **FR-005**: Al validar satisfactoriamente la reserva y la vigencia del tiempo, el sistema DEBE transferir la solicitud de pago con los identificadores correspondientes (identificador de la reserva, identificador del cliente, identificador de la embarcación y fechas reservadas) a la API de Módulo 3 (`Recibir solicitud de pago`).
- **FR-006**: El sistema DEBE delegar la gestión del cobro en su totalidad a Módulo 3 tras la transferencia de la solicitud, sin intervenir en pasarelas bancarias, enlaces o transacciones financieras.
- **FR-007**: El sistema DEBE mantener la reserva en estado "Pendiente de Pago" durante todo este flujo, sin realizar cambios de estado en este caso de uso.
- **FR-008**: Si la API de Módulo 3 no responde o devuelve un error de comunicación, el sistema DEBE registrar el incidente técnico y mantener la reserva en estado "Pendiente de Pago" para permitir nuevos intentos mientras el temporizador siga vigente.
- **FR-009**: **REGLA DE NEGOCIO ESTRICTA (Sin interacción financiera):** El sistema **NO DEBE capturar credenciales bancarias, procesar pasarelas de cobro, gestionar enlaces de pago ni calcular tarifas, comisiones o garantías**. La responsabilidad de Módulo 2 concluye al transferir válidamente la orden a Módulo 3.
- **FR-010**: El sistema DEBE registrar un asiento auditable de cada solicitud de pago transferida a Módulo 3, capturando: identificador de la reserva, identificador del arrendatario, tiempo remanente de tolerancia, marca temporal y resultado de la comunicación con Módulo 3.

---

### Key Entities

- **Reserva**: Entidad de Módulo 2 que debe encontrarse en estado "Pendiente de Pago" con temporizador de tolerancia activo, cuyo estado permanece inalterado durante este caso de uso.
- **Solicitud de Pago**: Registro de dominio en Módulo 2 que documenta la transferencia de la intención de pago hacia Módulo 3 (identificador de reserva, identificador de arrendatario, fecha/hora de transferencia y estado de entrega).
- **Arrendatario**: Usuario turista autenticado titular de la reserva.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Cero (0%) solicitudes de pago admitidas o transferidas a Módulo 3 sobre reservas cuyos 15 minutos de tolerancia hayan expirado o que no se encuentren en estado "Pendiente de Pago".
- **SC-002**: Cero (0%) solicitudes de pago autorizadas a usuarios que no correspondan al Arrendatario titular de la reserva.
- **SC-003**: El cien por ciento (100%) de las solicitudes válidas son verificadas y transferidas a la API de Módulo 3 en menos de 500 milisegundos.
- **SC-004**: Cero por ciento (0%) de cambios de estado en la reserva durante este caso de uso (la reserva conserva su estado "Pendiente de Pago").
- **SC-005**: Cero (0) operaciones financieras, enlaces de pasarelas, captura de tarjetas o cálculos de montos ejecutados dentro de Módulo 2.
- **SC-006**: El cien por ciento (100%) de las transferencias exitosas a Módulo 3 quedan registradas en auditoría con su respectiva marca temporal.