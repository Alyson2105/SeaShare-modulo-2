# Feature Specification: Ver mis reservas

**Módulo**: Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones  
**Fecha de Creación**: 2026-09-28  
**Actores Primarios**: Arrendatario y Propietario  
**Dependencias Externas (APIs)**: Ninguna directa.
- **Casos de uso internos de Módulo 2**:
    - `Ver detalle de reserva` (`<<extend>>`): Este caso de uso recibe una extensión desde la vista de detalle cuando el usuario selecciona una reserva específica de la lista para verla a fondo.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consultar el listado de reservas como Arrendatario (Priority: P1)

Como Arrendatario, quiero ver un listado de todas mis reservas (pasadas, vigentes y futuras) con su estado actual y el precio total, para llevar un control de mis viajes y saber rápidamente cuáles están confirmadas, pendientes o canceladas.

**Why this priority**: Es el panel de control del usuario. Sin esta vista, el turista no tiene forma de hacer seguimiento a las transacciones que ha realizado ni de acceder a sus viajes próximos.

**Independent Test**: Se prueba ingresando con un usuario Arrendatario que tenga reservas en diferentes estados (`Pendiente de Pago`, `Reservada`, `Cancelada`, `Completada`). Se verifica que la lista cargue correctamente mostrando la embarcación, las fechas, el estado principal, y el total original registrado sin cálculos locales.

**Acceptance Scenarios**:

1. **Scenario**: Listado exitoso de reservas activas e históricas
    - **Given** un Arrendatario autenticado con historial de viajes en la plataforma
    - **When** el usuario ingresa a la sección "Mis Reservas"
    - **Then** el sistema presenta el listado ordenado cronológicamente, mostrando para cada una: nombre de la embarcación, rango de fechas, estado actual y el monto total guardado al momento de su creación.

2. **Scenario**: Visualización de reservas canceladas con su total original congelado
    - **Given** una reserva en estado `Cancelada` (con sub-estado `Por Anfitrión` o `Por Inasistencia`)
    - **When** el Arrendatario visualiza esa fila en su listado
    - **Then** el sistema presenta el estado "Cancelada" y expone únicamente el total original que fue congelado al crear la reserva, sin mostrar cálculos, deducciones ni devoluciones, delegando esa liquidación a Módulo 3.

---

### User Story 2 - Consultar el listado de reservas como Propietario (Priority: P1)

Como Propietario, quiero consultar el listado de las reservas asociadas exclusivamente a mis embarcaciones, con su estado y precio total, para organizar mi operación en el muelle y saber qué viajes debo atender.

**Why this priority**: Es fundamental para la operación logística en el muelle. El Propietario debe saber exactamente qué reservas están confirmadas (`Reservada`) para alistar el barco.

**Independent Test**: Se prueba ingresando con un usuario Propietario que tenga embarcaciones publicadas. Se verifica que solo vea las reservas hechas sobre sus activos, diferenciando claramente los estados de las mismas.

**Acceptance Scenarios**:

1. **Scenario**: Listado exclusivo de activos propios
    - **Given** un Propietario registrado con embarcaciones en Módulo 1
    - **When** el usuario solicita el listado de reservas
    - **Then** el sistema recupera y muestra únicamente las reservas donde sus embarcaciones están involucradas, excluyendo reservas de terceros.

---

## Edge Cases

- **Ausencia de reservas**: Si el usuario (Arrendatario o Propietario) no tiene ninguna reserva registrada, el sistema debe presentar un "Empty State" (estado vacío) amigable, invitando al Arrendatario a explorar el catálogo o al Propietario a esperar nuevas solicitudes.
- **Inmutabilidad financiera**: Si una reserva está `Cancelada`, el Módulo 2 **NO** recalcula el total ni muestra penalidades en la lista. Se expone el total original guardado en la base de datos.
- **Punto de extensión**: Hacer clic en cualquiera de las reservas de la lista dispara la condición para ejecutar el caso de uso `Ver detalle de reserva` (`<<extend>>`).

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE permitir tanto al actor Arrendatario como al Propietario acceder a su lista de reservas.
- **FR-002**: El sistema DEBE filtrar los registros en la base de datos para devolver únicamente las reservas donde el usuario en sesión coincida con el `arrendatario_id` o el `propietario_id`.
- **FR-003**: El sistema DEBE presentar para cada reserva en el listado: Identificador/Nombre de la embarcación, rango de fechas del viaje, estado principal vigente y precio total.
- **FR-004**: **REGLA DE NEGOCIO ESTRICTA**: El precio total expuesto DEBE ser el monto original congelado durante la creación de la reserva. El sistema NO DEBE calcular deducciones, aplicar reembolsos ni restar porcentajes en las reservas `Canceladas`.
- **FR-005**: El sistema DEBE mostrar claramente los sub-estados en las reservas canceladas (ej. `Cancelada - Por Anfitrión`, `Cancelada - Por Inasistencia`) para dar contexto al usuario sin comprometer lógica financiera.
- **FR-006**: El sistema DEBE permitir seleccionar una reserva específica del listado para invocar la vista profunda (extensión hacia `Ver detalle de reserva`).
- **FR-007**: El sistema DEBE mostrar encabezados diferenciados según el rol del usuario en sesión: "Mis reservas" para el Arrendatario y "Reservas recibidas" para el Propietario, acompañados en ambos casos de un contador total de elementos (ej. "12 en total") y controles de navegación por pestañas para filtrar entre "Historial" y "Canceladas".
- **FR-008**: El sistema DEBE renderizar el estado vigente de cada reserva mediante etiquetas visuales (badges) con colores semánticos que faciliten su identificación rápida (ej. "Confirmada" en verde, "En navegación" en amarillo, "Pendiente de pago" en gris, "Cancelada" en rojo).
- **FR-009**: El sistema DEBE incluir en la tarjeta de resumen para el **Propietario** el nombre del Arrendatario que realizó la reserva (ej. "Laura Gomez") y la cantidad de pasajeros, junto a los datos básicos de la embarcación (imagen, nombre, rango de fechas y noches).
- **FR-010**: El sistema DEBE incluir en la tarjeta de resumen para el **Arrendatario** líneas de detalle contextual debajo de las fechas cuando aplique a los estados finales, tales como confirmaciones de devolución (ej. "+ $200.00 depósito reembolsado") o razones/efectos de cancelación (ej. "Cancelada - ventana 72h+", "Reembolso emitido - Sin cargo - cancelación gratuita").
- **FR-011**: El sistema DEBE proveer un enlace de acción textual explícito en cada fila o tarjeta (ej. "Ver detalles >") ubicado junto al monto total, para invocar la vista profunda (extensión hacia `Ver detalle de reserva`).


### Key Entities

- **Lista de Reservas (`ReservationList`)**: Colección de entidades `Reservation` filtradas por el ID del usuario en sesión, conteniendo los atributos inmutables de fechas, estados y montos totales guardados.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Cero (0%) cálculos de reembolsos o penalidades realizados por Módulo 2 sobre las reservas en estado Cancelada.
- **SC-002**: Aislamiento total de datos: 100% de efectividad en asegurar que un Propietario jamás visualice las reservas asociadas a embarcaciones que no le pertenecen.