# Feature Specification: Ver detalle de reserva

**Módulo**: Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones  
**Fecha de Creación**: 2026-09-28  
**Actores Primarios**: Arrendatario y Propietario  
**Dependencias Externas (APIs)**: Ninguna directa.
- **Casos de uso internos de Módulo 2**:
    - `Ver mis reservas` (`<<extend>>`): Este caso de uso extiende a la lista general, activándose cuando el usuario elige ver la información a fondo.
    - `Solicitar cancelación` (`<<extend>>`): El detalle de la reserva es extendido por la cancelación. Si el usuario está viendo los detalles y decide cancelar, se activa ese flujo.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Ver la información completa y desglosada del viaje (Priority: P1)

Como usuario (Arrendatario o Propietario), quiero ver el detalle completo de una reserva específica, incluyendo el desglose oficial de cobros y las fechas exactas, para revisar las condiciones particulares de ese contrato en cualquier momento.

**Why this priority**: Otorga transparencia absoluta sobre el contrato. Es necesario para que el cliente sepa qué pagó y para que el Propietario sepa qué servicios debe proveer.

**Independent Test**: Se prueba seleccionando una reserva confirmada (`Reservada`). Se valida que la pantalla muestre todos los campos de la entidad, incluyendo el desglose congelado (Total, Seguro, Depósito) exactamente como lo entregó Módulo 3 al momento del pago.

**Acceptance Scenarios**:

1. **Scenario**: Despliegue de datos almacenados en una reserva activa
    - **Given** un usuario que selecciona una reserva específica desde su listado
    - **When** el sistema carga la vista de "Ver detalle de reserva"
    - **Then** el sistema expone la información almacenada: ID de reserva, datos de la embarcación, fechas y horas, cantidad de pasajeros, estado actual y el desglose de valores congelados.

---

### User Story 2 - Visualizar el estado y proveer el punto de entrada para cancelaciones (Priority: P1)

Como Arrendatario o Propietario revisando los detalles de una reserva en estado `Reservada` (aún no iniciada), quiero tener a disposición la opción para cancelar el viaje, para poder ejercer mi derecho contractual de desistimiento.

**Why this priority**: Es el único punto de anclaje lógico en la interfaz para permitir que los usuarios detonen el proceso de cancelación establecido en la arquitectura.

**Independent Test**: Se prueba abriendo una reserva en estado `Reservada` y validando que el botón de "Cancelar Reserva" esté disponible. Luego se prueba con una en estado `En Navegación` o `Cancelada` y se valida que la opción no exista.

**Acceptance Scenarios**:

1. **Scenario**: Habilitación del punto de extensión para cancelar
    - **Given** una reserva cuyo estado principal es `Reservada`
    - **When** el usuario visualiza los detalles
    - **Then** el sistema provee un botón o acción visible (punto de extensión) que invoca el caso de uso `Solicitar cancelación` (`<<extend>>`).

2. **Scenario**: Ocultamiento de acciones en estados no permitidos
    - **Given** una reserva cuyo estado principal es `En Navegación`, `Completada` o `Cancelada`
    - **When** el usuario visualiza los detalles
    - **Then** el sistema NO muestra la opción de cancelar, respetando la máquina de estados.

---

## Edge Cases

- **Manejo de reservas Canceladas**: En la vista de detalle de una reserva cancelada, se debe mostrar el total congelado original junto con una etiqueta descriptiva del efecto contractual (ej. "Reembolso sujeto a políticas del Módulo 3"), protegiendo la regla de oro de no realizar matemáticas locales.
- **Privacidad de datos cruzados**: El Arrendatario no debe ver datos privados bancarios del Propietario, ni el Propietario debe ver datos de la tarjeta de crédito del Arrendatario. La vista de detalle expone únicamente la información de la entidad operativa de Módulo 2.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE recibir el identificador de la reserva específica a consultar como parámetro al extender a `Ver mis reservas`.
- **FR-002**: El sistema DEBE validar que el usuario en sesión tenga autorización sobre esa reserva (sea el Arrendatario o el Propietario).
- **FR-003**: El sistema DEBE exponer todos los campos guardados en la entidad `Reserva`: ID, fechas, horas, estado, sub-estado, y desglose monetario guardado (tarifa base, seguro, depósito, total).
- **FR-004**: **REGLA DE NEGOCIO ESTRICTA**: Todos los valores financieros mostrados DEBEN ser de solo lectura y reflejar exactamente lo devuelto por Módulo 3 al momento del pago.
- **FR-005**: Si la reserva se encuentra en estado `Reservada`, el sistema DEBE proveer un punto de acceso en la interfaz para detonar el flujo `Solicitar cancelación` (`<<extend>>`).
- **FR-006**: Si la reserva se encuentra en estado `Cancelada`, el sistema DEBE mostrar el sub-estado exacto (`Flexible`, `Moderado`, `Tardío`, `Por Anfitrión`, `Por Inasistencia`) junto con el monto total original.

### Key Entities

- **Detalle de Reserva (`ReservationDetail`)**: La entidad `Reservation` completa recuperada de la base de datos de Módulo 2, conteniendo el estado consolidado de la transacción y sus tiempos.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de las reservas en estado `Reservada` proveen acceso directo al flujo de cancelación para los actores autorizados.
- **SC-002**: Cero (0%) filtraciones de reservas: el sistema deniega el acceso a vistas de detalle si el usuario no es parte directa de la transacción (ni propietario ni arrendatario de ese ID específico).