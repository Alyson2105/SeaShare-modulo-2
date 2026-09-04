# Feature Specification: Iniciar Reserva

**Módulo**: Módulo 2 – Gestión de Reserva
**Creado**: 2026-09-03
**Actor primario**: Arrendatario
**Dependencias externas (APIs)**:

- Módulo 1 – Gestión de Embarcación (`Proveer información de embarcación`, `Brindar información estado operativo`)

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - [Crear una reserva básica] (Priority: P1)

El Arrendatario selecciona una embarcación disponible, define el periodo de tiempo que desea reservarla (fecha/hora de inicio y fin), y el sistema registra la reserva consultando previamente a la API de Módulo 1 para confirmar que la embarcación existe y está operativa.

**Why this priority**: Una creación de reserva constituye el corazón del caso de uso. Sin esto no existe el módulo de reservas.

**Independent Test**: Se puede probar completamente simulando (mock) la respuesta de la API de Módulo 1 con una embarcación disponible, y verificando que al enviar una solicitud de reserva con fechas válidas, el sistema persiste la reserva con estado inicial correcto.

**Acceptance Scenarios**:

1. **Scenario**: Reserva exitosa con embarcación disponible
   - **Given** el Arrendatario está autenticado y la embarcación seleccionada tiene estado operativo "disponible" según Módulo 1
   - **When** el Arrendatario envía una solicitud de reserva con fecha/hora de inicio y fin válidas
   - **Then** el sistema crea la reserva con estado "Pendiente de Pago" y la asocia al Arrendatario y a la embarcación

2. **Scenario**: Intento de reserva con embarcación no operativa
   - **Given** Módulo 1 informa que la embarcación está en mantenimiento, reservada o en navegación
   - **When** el Arrendatario intenta iniciar la reserva
   - **Then** el sistema rechaza la solicitud y muestra el motivo (embarcación no disponible para reserva)

3. **Scenario**: Expiración automática por TTL sin pago
   - **Given** una reserva fue creada exitosamente (periodo validado por "Establecer tiempo reserva")
   - **When** transcurren 15 minutos sin recibir confirmación de pago de Módulo 3
   - **Then** el sistema invoca "Actualizar estado reserva" para expirarla y libera la disponibilidad de la embarcación en Módulo 1


### User Story 2 - [Ver cotización antes de confirmar] (Priority: P2)

Antes de confirmar la reserva, el sistema le muestra al Arrendatario una cotización estimada (costo total según el tiempo seleccionado) para que decida si continúa.

**Why this priority**: Mejora la experiencia y transparencia, pero la reserva básica (US1) puede funcionar sin este paso (por ejemplo, cotización fija o calculada después). No es indispensable para el MVP.

**Independent Test**: Se puede probar de forma aislada invocando la funcionalidad de cotización con un rango de fechas y una embarcación ya conocidos, y verificando que el monto devuelto es correcto según la tarifa configurada.

**Acceptance Scenarios**:

1. **Scenario**: Mostrar cotización antes de confirmar

- **Given** el Arrendatario ya definió el tiempo de reserva (validado y cotizado por "Establecer tiempo reserva")
- **When** el sistema continúa el flujo hacia la confirmación
- **Then** se le muestra al Arrendatario la cotización estimada (generada por "Proveer información cotización de reserva") antes de que confirme la reserva


### Edge Cases

- ¿Qué pasa si la API de Módulo 1 no responde (timeout) al consultar disponibilidad/estado operativo? [NEEDS CLARIFICATION: ¿reintentar, fallar la reserva, o permitir reserva "provisional"?]
- ¿Qué pasa si el Arrendatario abandona el flujo después de ver la cotización pero antes de confirmar? ¿Se guarda algún estado "borrador"?
- ¿Puede un Arrendatario tener múltiples reservas activas simultáneas?
- ¿Qué pasa si la confirmación de pago llega de Módulo 3 justo en el límite de los 15 minutos (race condition entre el TTL expirando y el pago confirmándose)? [NEEDS CLARIFICATION: ¿gana el pago si llega dentro de una tolerancia de gracia, o se rechaza estrictamente pasado el minuto 15?]

> Los edge cases sobre traslape/condición de carrera entre reservas del mismo rango de tiempo se documentan en `spec-establecer-tiempo-reserva.md`.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE permitir al Arrendatario seleccionar una embarcación y especificar un periodo de reserva (fecha/hora de inicio y fin).
- **FR-002**: El sistema DEBE consumir la API de Módulo 1 (`Proveer información de embarcación`) para validar que la embarcación seleccionada existe y obtener sus datos.
- **FR-003**: El sistema DEBE consumir la API de Módulo 1 (`Brindar información estado operativo`) para verificar que la embarcación está disponible antes de confirmar la reserva.
- **FR-004**: El sistema DEBE invocar el caso de uso "Establecer tiempo reserva" para registrar y validar el periodo de tiempo solicitado (ver `spec-establecer-tiempo-reserva.md` para el detalle de sus reglas).
- **FR-005**: Si "Establecer tiempo reserva" rechaza el periodo solicitado (fechas inválidas o traslape con otra reserva), el sistema NO DEBE crear la reserva y DEBE mostrar al Arrendatario el motivo devuelto por esa validación.
- **FR-006**: El sistema DEBE mostrar al Arrendatario la cotización estimada antes de la confirmación final (US2). La cotización NO se calcula en este caso de uso: la produce "Proveer información cotización de reserva", que es invocado por "Establecer tiempo reserva" tras validar exitosamente la ventana (ver FR-010 de `spec-establecer-tiempo-reserva.md`); "Iniciar reserva" solo la presenta, sin recalcularla.
- **FR-007**: Si "Establecer tiempo reserva" valida exitosamente el periodo solicitado, el sistema DEBE persistir la reserva con estado "Pendiente de Pago" e iniciar inmediatamente un temporizador (TTL) de 15 minutos.
- **FR-008**: Si el sistema recibe confirmación de pago (vía Módulo 3 - "Confirmar pago") antes de que expire el TTL, la reserva DEBE transicionar a estado "Confirmada" y el temporizador DEBE cancelarse; la transición de estado DEBE ejecutarla "Actualizar estado reserva" (ver su spec), que es el caso de uso destinatario de la flecha de "Confirmar pago".
- **FR-009**: Si el TTL expira sin confirmación de pago, el sistema DEBE invocar "Actualizar estado reserva" para marcar la reserva como "Expirada" y liberar la disponibilidad de la embarcación en Módulo 1.
- **FR-010**: El estado inicial de la reserva queda fijado provisionalmente como "Pendiente de Pago" (ver FR-007). [NEEDS CLARIFICATION: nombre exacto y máquina de estados completa de una reserva]
- **FR-011**: El sistema DEBE registrar el Arrendatario asociado a cada reserva creada.

### Key Entities

- **Reserva**: Entidad central del módulo. Atributos clave: id, arrendatario_id, embarcación_id (referencia externa a Módulo 1), fecha_inicio, fecha_fin, estado, cotización_estimada, ttl_expira_en (timestamp calculado al persistir la reserva, usado para disparar la expiración automática).
- **Embarcación** *(entidad externa, propiedad de Módulo 1)*: Módulo 2 solo la referencia por id y consume sus datos vía API; no la persiste como fuente de verdad.
- **Arrendatario**: Usuario que solicita la reserva. Se asume que la autenticación/identidad ya existe fuera de este spec. [NEEDS CLARIFICATION: ¿Módulo 2 gestiona usuarios o eso viene de otro módulo/servicio de identidad?]

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El Arrendatario puede completar el flujo de iniciar reserva (selección + tiempo + cotización) en menos de 2 minutos.
- **SC-002**: El sistema responde con la disponibilidad de la embarcación (vía Módulo 1) en menos de [NEEDS CLARIFICATION: tiempo objetivo, p. ej. 3 segundos].
- **SC-003**: 0% de reservas creadas con un periodo de tiempo rechazado por "Establecer tiempo reserva" (ver métricas de traslape en su propio spec).
- **SC-004**: El 90% de los Arrendatarios completa la creación de una reserva sin errores en el primer intento.
- **SC-005**: El 100% de las reservas "Pendiente de Pago" sin confirmación de pago transcurridos los 15 minutos del TTL son marcadas como "Expirada" y la embarcación vuelve a "Disponible" en menos de 5 segundos tras el vencimiento del temporizador.
- **SC-006**: Cero (0%) sobreventas o solapamiento de reservas en una misma embarcación durante el mismo periodo horario.