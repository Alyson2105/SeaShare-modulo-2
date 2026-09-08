# Feature Specification: Iniciar Reserva

**Módulo**: Módulo 2 – Gestión de Reserva  
**Creado**: 2026-09-03 (Actualizado: 2026-09-08)  
**Actor primario**: Arrendatario  
**Dependencias externas (APIs)**:

- Módulo 1 – Gestión de Embarcación (`Proveer información de embarcación`, `Brindar información estado operativo`): consulta de existencia, datos del activo (incluyendo sus horas fijas de check-in y check-out) y verificación de disponibilidad operativa.
- Módulo 3 – Gestión de Liquidación (`Proveer información cotización de reserva` [consumo], `Confirmar pago` [recepción]): cálculo de la cotización estimada y recepción de la confirmación de pago.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - [Crear reserva con periodo válido y sin traslapes] (Priority: P1)

El Arrendatario selecciona una embarcación disponible e ingresa el rango de fechas (días de inicio y fin) que desea reservarla. El sistema consulta a Módulo 1 para validar la existencia y estado operativo de la embarcación, obtiene sus horas fijas de check-in y check-out, valida que el periodo en días sea coherente y dentro de los límites permitidos, y verifica que ninguno de los días solicitados esté ya ocupado por otra reserva de la misma embarcación. Si todo es correcto, registra la reserva con su rango horario exacto y estado inicial "Pendiente de Pago", iniciando el temporizador de bloqueo temporal (TTL).

**Why this priority**: Es el flujo central de contratación de la plataforma. Garantiza la integridad básica del negocio al impedir reservas sobre embarcaciones no operativas, fechas incoherentes o días ya reservados (sobreventa).

**Independent Test**: Se puede probar simulando una embarcación disponible con horarios de check-in (ej. 09:00) y check-out (ej. 17:00) conocidos en Módulo 1, enviando solicitudes con diferentes rangos de fechas (válidas, no operativas, duración inválida, y días con traslape previo), verificando que el sistema solo persiste la reserva y asigna el rango horario exacto cuando todas las validaciones son superadas.

**Acceptance Scenarios**:

1. **Scenario**: Reserva exitosa con rango de días disponible y horas fijas de la embarcación
   - **Given** el Arrendatario está autenticado, la embarcación seleccionada tiene estado operativo "Disponible" en Módulo 1 con hora de check-in (09:00) y check-out (17:00), y no existen reservas previas en los días solicitados
   - **When** el Arrendatario solicita reservar del día 10 al día 12 de un mes determinado
   - **Then** el sistema crea la reserva en estado "Pendiente de Pago" con inicio el día 10 a las 09:00 y fin el día 12 a las 17:00, e inicia el temporizador de 15 minutos (TTL)

2. **Scenario**: Intento de reserva con embarcación no operativa en Módulo 1
   - **Given** Módulo 1 informa que la embarcación seleccionada está en estado "En Mantenimiento", "Reservado" o "En Navegación"
   - **When** el Arrendatario intenta iniciar la reserva
   - **Then** el sistema rechaza la solicitud informando que la embarcación no se encuentra disponible operativamente para reserva

3. **Scenario**: Solicitud con duración fuera de los límites permitidos
   - **Given** una duración de reserva solicitada (en días) menor a la mínima o mayor a la máxima establecida [NEEDS CLARIFICATION: duración mínima y máxima en días]
   - **When** el Arrendatario intenta iniciar la reserva
   - **Then** el sistema rechaza la solicitud indicando que la cantidad de días solicitada está fuera del rango permitido

4. **Scenario**: Intento de reserva con traslape en uno o más días con reserva existente
   - **Given** la misma embarcación ya cuenta con una reserva activa (confirmada o pendiente de pago) que ocupa uno o más días del rango solicitado
   - **When** el Arrendatario intenta solicitar ese rango de fechas
   - **Then** el sistema rechaza la solicitud informando que la embarcación ya está ocupada en los días seleccionados y no crea la reserva

---

### User Story 2 - [Resolver solicitudes simultáneas sobre el mismo rango de días] (Priority: P1 / P2 - PENDIENTE DE CONFIRMACIÓN)

Dos o más Arrendatarios intentan reservar la misma embarcación en el mismo rango de días casi al mismo tiempo. El sistema resuelve la contienda de manera exclusiva, otorgando el bloqueo a una sola solicitud y rechazando las demás por no disponibilidad, garantizando que nunca se generen reservas duplicadas para la misma embarcación y fechas.

**Why this priority**: En un marketplace con alta demanda, la concurrencia simultánea sobre la misma embarcación puede provocar dobles reservas si no se gestiona de forma atómica.

**Independent Test**: Se puede probar enviando dos o más solicitudes de reserva concurrentes sobre la misma embarcación y el mismo rango de días, verificando que exactamente una solicitud crea la reserva en estado "Pendiente de Pago" y las demás son rechazadas informando que los días ya no están disponibles.

**Acceptance Scenarios**:

1. **Scenario**: Dos solicitudes simultáneas sobre los mismos días, un único ganador
   - **Given** dos Arrendatarios envían al mismo tiempo una solicitud de reserva para la misma embarcación y el mismo rango de días disponibles
   - **When** el sistema procesa ambas solicitudes concurrentemente
   - **Then** el sistema acepta únicamente una de las solicitudes (creando su reserva en estado "Pendiente de Pago") y rechaza la otra indicando que la embarcación ya no está disponible en esas fechas

---

### User Story 3 - [Ver cotización antes de confirmar] (Priority: P2)

Antes de confirmar definitivamente la creación de la reserva, el sistema presenta al Arrendatario una cotización estimada con el desglose del costo del alquiler por los días seleccionados, calculada a partir de los datos tarifarios de Módulo 3.

**Why this priority**: Otorga transparencia de precios al usuario antes de asumir el compromiso de pago, pero no bloquea la lógica transaccional básica de validación de disponibilidad del MVP.

**Independent Test**: Se puede probar solicitando la cotización para un rango de días determinado en una embarcación con tarifas configuradas en Módulo 3, verificando que el monto presentado coincide con la tarifa por día multiplicada por la duración.

**Acceptance Scenarios**:

1. **Scenario**: Presentación de cotización estimada
   - **Given** el Arrendatario seleccionó una embarcación y un rango de días válidos
   - **When** el sistema calcula los costos consultando la tarifa en Módulo 3 (vía "Proveer información cotización de reserva")
   - **Then** se presenta al Arrendatario la cotización total estimada antes de la confirmación final de la reserva

---

### User Story 4 - [Expiración automática por vencimiento del TTL sin pago] (Priority: P1)

Una vez creada la reserva en estado "Pendiente de Pago", el sistema mantiene bloqueados los días seleccionados durante una ventana de tolerancia de 15 minutos (TTL). Si no se recibe confirmación de pago por parte de Módulo 3 al vencer dicho tiempo, el sistema expira la reserva automáticamente y libera los días y la embarcación en Módulo 1.

**Why this priority**: Evita bloqueos indefinidos de inventario provocados por usuarios que inician el proceso pero no completan el pago, protegiendo los ingresos del Propietario.

**Independent Test**: Se puede probar creando una reserva en estado "Pendiente de Pago", dejando transcurrir el lapso de 15 minutos sin emitir pago desde Módulo 3, y verificando que la reserva transiciona a "Expirada" y que los días vuelven a estar disponibles para nuevas consultas.

**Acceptance Scenarios**:

1. **Scenario**: Expiración automática tras 15 minutos sin pago
   - **Given** una reserva creada en estado "Pendiente de Pago" con un temporizador de 15 minutos activo
   - **When** transcurren los 15 minutos sin que Módulo 3 confirme el pago
   - **Then** el sistema invoca "Actualizar estado reserva" para transicionarla a "Expirada" y libera la disponibilidad de la embarcación en Módulo 1

---

### Edge Cases

- **Indisponibilidad o timeout de la API de Módulo 1**: Si la API de Módulo 1 no responde o devuelve error al consultar la información, horarios o estado operativo de la embarcación, el sistema DEBE abortar el proceso de reserva de forma segura, sin crear reservas provisionales, e informar al Arrendatario sobre la indisponibilidad temporal del servicio. [NEEDS CLARIFICATION: SLA de respuesta esperado de Módulo 1]
- **Ausencia de horarios de check-in / check-out en Módulo 1**: Si la embarcación registrada en Módulo 1 no tiene definidos sus horarios fijos de check-in y check-out, el sistema DEBE rechazar la creación de la reserva e informar el error de configuración del activo. [NEEDS CLARIFICATION: ¿existen horarios por defecto a nivel de plataforma o es obligatorio que el Propietario los configure?]
- **Abandono del flujo previo a la confirmación**: Si el Arrendatario consulta la cotización pero abandona el proceso antes de confirmar la solicitud, el sistema NO DEBE persistir ninguna reserva ni aplicar retenciones sobre los días del calendario; la embarcación permanece totalmente disponible para otros usuarios.
- **Múltiples reservas activas por el mismo Arrendatario**: El sistema permite que un Arrendatario gestione varias reservas en curso simultáneamente (para distintas fechas o embarcaciones); cada reserva opera con su propio ciclo de vida y temporizador TTL de 15 minutos de forma independiente.
- **Concurrencia entre expiración del TTL y confirmación de pago (límite del minuto 15)**: Si la confirmación de pago de Módulo 3 llega cuando la reserva ya fue marcada como "Expirada" y liberada, el sistema DEBE rechazar el evento de pago y notificar a Módulo 3 para que tramite el reembolso correspondiente; si el pago se recibe antes de la ejecución de la expiración, prevalece la confirmación y la reserva transiciona a "Confirmada".
- **Zona horaria de la embarcación**: La validación de fechas y el cálculo de inicio y fin de la reserva se realizan en la zona horaria del puerto de atraque de la embarcación, garantizando que las horas de check-in y check-out correspondan a la hora local del activo.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE permitir al Arrendatario seleccionar una embarcación y especificar un rango de fechas de reserva (día de inicio y día de fin).
- **FR-002**: El sistema DEBE consumir la API de Módulo 1 (`Proveer información de embarcación`) para verificar la existencia de la embarcación y obtener sus datos de configuración, incluyendo obligatoriamente sus horas fijas de check-in y check-out. [NEEDS CLARIFICATION: valores por defecto si no vienen configuradas].
- **FR-003**: El sistema DEBE consumir la API de Módulo 1 (`Brindar información estado operativo`) para verificar que la embarcación se encuentra en estado "Disponible" antes de proceder con la reserva.
- **FR-004**: El sistema DEBE validar que la fecha de inicio sea igual o anterior a la fecha de fin, que el inicio no se encuentre en el pasado [NEEDS CLARIFICATION: política para reservas inmediatas / mismo día], y que la duración total en días esté dentro de los límites permitidos [NEEDS CLARIFICATION: duración mínima y máxima en días].
- **FR-005**: El sistema DEBE construir el rango temporal exacto de la reserva asignando la hora fija de check-in de la embarcación al día de inicio, y la hora fija de check-out de la embarcación al día de fin.
- **FR-006**: El sistema DEBE verificar que ningún día comprendido en el rango solicitado esté ocupado por otra reserva activa (en estado "Confirmada" o "Pendiente de Pago" con TTL vigente) para la misma embarcación en Módulo 2.
- **FR-007**: Si la validación de fechas o disponibilidad de días resulta rechazada, el sistema NO DEBE crear la reserva y DEBE informar al Arrendatario el motivo específico del rechazo (embarcación no operativa, duración fuera de límites o fechas ya ocupadas).
- **FR-008**: El sistema DEBE resolver las solicitudes concurrentes sobre los mismos días de forma atómica y exclusiva, garantizando que solo una solicitud resulte admitida y las demás sean rechazadas por no disponibilidad. [NEEDS CLARIFICATION: el mecanismo técnico de concurrencia (bloqueo optimista vs. pesimista) es una decisión de diseño de arquitectura].
- **FR-009**: El sistema DEBE presentar al Arrendatario la cotización estimada antes de la confirmación final. Dicha cotización se calcula consumiendo la tarifa correspondiente desde Módulo 3 (`Proveer información cotización de reserva`); este caso de uso solo la presenta sin recalcular montos por su cuenta.
- **FR-010**: Al confirmar una solicitud con días válidos y disponibles, el sistema DEBE persistir la reserva en estado "Pendiente de Pago" e iniciar inmediatamente un temporizador (TTL) de 15 minutos.
- **FR-011**: Si el sistema recibe la confirmación de pago desde Módulo 3 (`Confirmar pago`) antes del vencimiento del TTL, la reserva DEBE transicionar a estado "Confirmada" y el temporizador DEBE cancelarse (transición ejecutada por "Actualizar estado reserva").
- **FR-012**: Si el temporizador de 15 minutos expira sin confirmación de pago, el sistema DEBE invocar "Actualizar estado reserva" para marcar la reserva como "Expirada" y notificar a Módulo 1 la liberación de la embarcación.
- **FR-013**: El sistema DEBE registrar el identificador del Arrendatario asociado a cada reserva creada.

### Key Entities

- **Reserva**: Entidad central del módulo. Atributos de dominio clave: identificador único, identificador del arrendatario, identificador de la embarcación (referencia a Módulo 1), fecha/hora de inicio (día inicial + hora check-in), fecha/hora de fin (día final + hora check-out), días reservados, estado de la reserva (ej. "Pendiente de Pago", "Confirmada", "Expirada"), cotización estimada y marca temporal de expiración del TTL (15 minutos).
- **Embarcación** *(entidad externa, propiedad de Módulo 1)*: Representa el activo náutico. Módulo 2 consulta y referencia sus atributos: identificador, estado operativo (Disponible, Reservado, En Navegación, En Mantenimiento), hora fija de check-in, hora fija de check-out y puerto/ubicación para determinar zona horaria.
- **Arrendatario**: Usuario cliente que solicita la reserva. Se asume gestionado por un servicio de autenticación/identidad externo. [NEEDS CLARIFICATION: ¿Módulo 2 gestiona usuarios o consume un servicio de identidad?].

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El Arrendatario puede completar el flujo de selección de fechas, consulta de cotización y solicitud de reserva en menos de 2 minutos.
- **SC-002**: El sistema valida la disponibilidad operativa y consulta de horarios en Módulo 1 en menos de [NEEDS CLARIFICATION: tiempo objetivo, p. ej. 2 segundos].
- **SC-003**: Cero por ciento (0%) de reservas creadas sobre días con traslape o fechas incoherentes en la misma embarcación.
- **SC-004**: El cien por ciento (100%) de las reservas creadas registran su inicio y fin vinculados a los horarios exactos de check-in y check-out configurados para la embarcación.
- **SC-005**: Bajo concurrencia simultánea sobre el mismo rango de días, exactamente una (1) solicitud es procesada exitosamente y el 100% de las demás son rechazadas limpiamente sin generar sobreventas ni inconsistencias de inventario.
- **SC-006**: El cien por ciento (100%) de las reservas en estado "Pendiente de Pago" sin confirmación de pago tras 15 minutos son marcadas como "Expirada" y la embarcación es liberada en Módulo 1 en menos de 5 segundos tras vencer el temporizador.