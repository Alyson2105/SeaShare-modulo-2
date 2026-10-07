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
- **FR-006**: Si la reserva se encuentra en estado `Cancelada`, el sistema DEBE mostrar el sub-estado exacto (`Flexible`, `Moderado`, `Tardío`, `Por Propietario`, `Por Inasistencia`) junto con el monto total original.
- **FR-007**: El sistema DEBE mostrar un bloque de "Itinerario" que liste la fecha/hora de Embarque, Desembarque, Puerto de zarpe y cantidad de pasajeros. Este bloque DEBE actualizarse dinámicamente para incorporar la "Salida real" y "Llegada real" conforme la reserva avance en sus estados operativos.
- **FR-008**: El sistema DEBE mostrar para el Arrendatario una sección de "Servicios que incluye su reserva" renderizando las comodidades de la embarcación en formato de etiquetas (chips).
- **FR-009**: El sistema DEBE mostrar al Arrendatario una línea inferior con los datos del "Propietario" (nombre). Para el Propietario, el sistema DEBE renderizar una tarjeta dedicada de "Cliente" con el nombre del titular y la cantidad de viajes previos en la plataforma.
- **FR-010**: El sistema DEBE mostrar en el resumen financiero el monto consolidado de la reserva ("Total de la reserva:" o "Total pagado:") en COP, correspondiente al valor oficial registrado por Módulo 3.
- **FR-011**: En el estado `Reservada` (previo al inicio del viaje), el sistema DEBE mostrar un bloque titulado "Resumen de pago" que contenga el desglose oficial en filas de: "Tarifa base de alquiler", "Seguro obligatorio" y "Depósito de garantía". Debajo de una línea divisoria, el sistema DEBE mostrar el total en fuente de mayor tamaño y negrita como "Total pagado (COP):".
- **FR-012**: Al transicionar al estado `En Navegación`, el sistema DEBE mantener visible el desglose oficial del "Resumen de pago" (Tarifa base, Seguro, Depósito) y la fila de "Total pagado (COP):".
- **FR-013**: Al transicionar al estado `Completada` (durante la ventana de disputa de garantía), el sistema DEBE modificar el bloque de "Resumen de pago" para el Propietario y Arrendatario, ocultando los rubros de tarifa base y seguro, para mostrar de forma exclusiva la fila "Depósito de garantía" con su monto respectivo.
- **FR-014**: Al transicionar a `Cancelada por inasistencia`, el sistema DEBE modificar estructuralmente el bloque para el Propietario: el título "Resumen de pago" cambia a "Compensación", se muestra una única fila llamada "Total original" con el monto en fuente grande, y se añade en la parte inferior el texto aclaratorio: "El sistema de pagos gestionará la compensación al propietario.".
- **FR-015**: El sistema DEBE renderizar dinámicamente botones o etiquetas (badges) de estado directamente debajo del bloque de resumen de pago, dependiendo de la etapa del viaje:
    - En estado `Reservada`: Mostrar el botón interactivo "X Cancelar reserva" con texto rojo, fondo blanco y contorno rojo para ambos roles.
    - En estado `En Navegación`: Ocultar permanentemente el botón de cancelación y reemplazarlo por una etiqueta de estado "En navegación" con fondo y texto verde.
    - En estado `Completada`: Mostrar una etiqueta verde de estado, variando su texto a "Completada" (durante la ventana de disputa) o "Completada sin incidentes" (tras resolución sin daños).
    - En estado `Cancelada`: Mostrar una etiqueta roja de estado, tal como "Cancelado por inasistencia".
- **FR-016**: En estado `Reservada` antes de la hora de zarpe, el sistema DEBE mostrar al Propietario un banner superior indicando el tiempo restante (ej. "Faltan 45 minutos para la hora de salida") y un panel lateral de "Acciones de embarque" con los botones "Marcar inicio de la navegación" y "Marcar inasistencia" presentes pero visualmente deshabilitados, acompañados de un texto indicando a qué hora se habilitarán.
- **FR-017**: Al superar la hora pactada sin registrar el embarque, el sistema DEBE cambiar el banner superior por una advertencia (color amarillo) con el título "El cliente aún no llega" y un contador regresivo dinámico ("Tiempo de cortesía en curso") mostrando el tiempo restante para reportar inasistencia (ej. "Faltan 12 min 45 s..."). En el panel de acciones, el sistema DEBE habilitar el botón principal "Marcar inicio de la navegación" y mantener deshabilitado "Marcar inasistencia".
- **FR-018**: Al agotarse los 30 minutos de tolerancia, el sistema DEBE cambiar el banner a una alerta roja ("Tiempo de cortesía cumplido") indicando que han pasado los 30 minutos y el cliente no se ha presentado. En el panel lateral, el sistema DEBE habilitar el botón "Marcar inasistencia" con fondo rojo y mantener habilitado el botón de inicio de navegación por si el cliente llega tarde.
- **FR-019**: Al registrar la salida y transicionar a `En Navegación`, el sistema DEBE remover por completo el panel lateral de "Acciones de embarque" y reemplazar los banners de espera por un banner ancho de color verde ("El viaje está en curso") que muestre la hora real de salida y contenga en su interior el botón de acción "Marcar fin de la navegación".
- **FR-020**: Si el Propietario marca el No-Show (estado `Cancelada por inasistencia`), el sistema DEBE remover el panel de acciones de embarque y mostrar un banner superior de estado terminal (fondo rojo tenue con icono de verificación) indicando "Reserva cancelada por inasistencia" junto con los minutos de espera registrados y el nombre del actor que reportó la acción.
### Key Entities

- **Detalle de Reserva (`ReservationDetail`)**: La entidad `Reservation` completa recuperada de la base de datos de Módulo 2, conteniendo el estado consolidado de la transacción y sus tiempos.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de las reservas en estado `Reservada` proveen acceso directo al flujo de cancelación para los actores autorizados.
- **SC-002**: Cero (0%) filtraciones de reservas: el sistema deniega el acceso a vistas de detalle si el usuario no es parte directa de la transacción (ni propietario ni arrendatario de ese ID específico).
