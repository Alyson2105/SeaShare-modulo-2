# Feature Specification: Generar Disputa de Garantía

**Módulo**: Módulo 2 (Operación de Reservas, Tiempos y Cancelaciones)  
**Fecha de Creación**: 2026-09-26  
**Actores Primarios**: Sistema (creación automática de la disputa y cierre automático por vencimiento, sin intervención humana); Propietario registrado de la embarcación (registro del reclamo dentro de la ventana de 24 horas)  
**Dependencias Externas (APIs)**: Ninguna directa.  
**Casos de uso internos de Módulo 2**:
- `Marcar fin de navegación` (`CU-07`): caso de uso base que dispara este caso de uso. Al completarse el cierre (reserva a `Completada`), CU-07 invoca a `Generar disputa de garantía` (`<<include>>`). Es el único flujo por el cual una reserva llega a `Completada`; los demás cierres desembocan en `Cancelada` y no disparan este caso de uso.
- `Actualizar estado de disputa de garantía` (`CU-17`, `<<include>>`): Generar disputa de garantía es el caso base y CU-17 es el incluido. Tanto el registro inicial en PENDIENTE como el cierre automático por vencimiento en RECHAZADA se ejecutan a través de la operación de cambio de estado de CU-17, para no duplicar la lógica de transición de estados en dos lugares distintos.

> **Nota de dominio**: los estados **PENDIENTE**, **RECHAZADA** y **ACEPTADA** (en mayúscula) pertenecen al objeto **Disputa de garantía**, que es distinto al estado de la reserva (`Completada`, `Reservada`, `Pendiente de Pago`, etc.). Ninguna regla de este spec modifica el estado de la reserva.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Creación automática de la disputa al cerrar la navegación (Priority: P1)

Al completarse `Marcar fin de navegación` (la reserva pasa a `Completada`, único flujo que llega a ese estado), el Sistema crea automáticamente la disputa de garantía asociada a la reserva en estado PENDIENTE y abre una ventana de 24 horas para que el Propietario reporte daños o problemas.

**Why this priority**: Sin la creación automática no existe el objeto sobre el cual el Propietario puede reclamar ni el Admin puede resolver. Es el punto de entrada obligatorio de todo el flujo de disputa.

**Independent Test**: Se prueba cerrando navegaciones (reservas `En Navegación` → `Completada` vía CU-07) y verificando que por cada cierre se crea exactamente una disputa en PENDIENTE con su ventana de 24 horas. Se prueba además que cierres que terminan en `Cancelada` no generan ninguna disputa.

**Acceptance Scenarios**:

1. **Scenario**: Cierre de navegación genera la disputa en PENDIENTE
   - **Given** una reserva en estado `En Navegación` cuyo cierre se procesa por `Marcar fin de navegación`
   - **When** la reserva transiciona a `Completada`
   - **Then** el Sistema crea automáticamente la disputa de garantía asociada en estado PENDIENTE, con ventana de 24 horas contada desde la creación, a través de la operación de `Actualizar estado de disputa de garantía` (`<<include>>`)

2. **Scenario**: Cierres que terminan en Cancelada no generan disputa
   - **Given** una reserva cuyo flujo de cierre termina en estado `Cancelada` (cancelación voluntaria o inasistencia)
   - **When** se consolida el estado final de la reserva
   - **Then** el sistema NO crea ninguna disputa de garantía

### User Story 2 - El Propietario registra su reclamo dentro de la ventana (Priority: P1)

El Propietario registrado de la embarcación registra su reclamo (descripción de los daños o problemas) sobre la disputa ya existente, dentro de las 24 horas de ventana. El Propietario no "genera" la disputa: solo reporta un reclamo sobre una disputa que el Sistema ya creó. El registro del reclamo no cambia el estado (la disputa sigue PENDIENTE a la espera de revisión del Admin).

**Why this priority**: Es la única vía por la cual un reclamo entra al flujo. Sin este registro, el vencimiento cierra la disputa como improcedente y el Propietario pierde la oportunidad de disputar el depósito.

**Independent Test**: Se prueba con una disputa en PENDIENTE dentro de su ventana registrando un reclamo como Propietario registrado, y verificando que el reclamo queda asociado a la disputa, que el estado sigue PENDIENTE y que el cierre automático posterior ya no aplica.

**Acceptance Scenarios**:

1. **Scenario**: Registro de reclamo dentro de la ventana
   - **Given** una disputa en estado PENDIENTE con su ventana de 24 horas aún abierta
   - **When** el Propietario registrado de la embarcación registra su reclamo con la descripción de los daños o problemas
   - **Then** el sistema asocia el reclamo a la disputa, mantiene el estado PENDIENTE (pendiente de revisión del Admin) y desactiva el cierre automático por vencimiento para esa disputa

### User Story 3 - Cierre automático por vencimiento sin reclamo (Priority: P1)

Si el Propietario no reporta nada dentro de las 24 horas, el propio caso de uso cierra la disputa con estado RECHAZADA (no existe un reclamo procedente), sin intervención del Admin, mediante un mecanismo de cierre automático por vencimiento de ventana (evento diferido o job programado).

**Why this priority**: Sin el cierre automático, las disputas sin reclamo quedarían en PENDIENTE indefinidamente y Módulo 3 nunca recibiría un estado final para resolver el depósito. Es lo que garantiza que todo el flujo termine.

**Independent Test**: Se prueba creando una disputa en PENDIENTE sin registrar reclamo, avanzando el reloj más allá de las 24 horas de ventana, y verificando que el sistema la transiciona a RECHAZADA sin intervención humana y con motivo de sistema "sin reclamo en ventana".

**Acceptance Scenarios**:

1. **Scenario**: Vencimiento sin reclamo cierra en RECHAZADA
   - **Given** una disputa en estado PENDIENTE cuya ventana de 24 horas venció sin que se registrara ningún reclamo
   - **When** se dispara el cierre automático por vencimiento
   - **Then** el sistema transiciona la disputa a RECHAZADA (motivo de sistema: sin reclamo en ventana) a través de la operación de `Actualizar estado de disputa de garantía` (`<<include>>`), sin intervención del Admin

---

### Edge Cases

- **Reclamo fuera de ventana**: si el Propietario intenta reportar después de vencidas las 24 horas (disputa ya cerrada en RECHAZADA por vencimiento), el sistema DEBE rechazar el reporte sin modificar el estado de la disputa.
- **Texto de novedades del cierre no es reclamo**: el texto opcional de novedades adjuntado al cerrar la navegación (CU-07) es informativo del check-out y NO cuenta como reclamo presentado. Si no hay registro separado dentro de la ventana, la disputa se cierra en RECHAZADA al vencimiento aunque hubiera texto al cierre.
- **Segundo reclamo dentro de la ventana / edición del reclamo**: [DUDA — ver lista de dudas al final del documento] no está definido si el Propietario puede registrar más de un reclamo o editar el ya registrado mientras la ventana sigue abierta.
- **Fallo del mecanismo de vencimiento**: si el evento diferido o job programado no se ejecuta a tiempo, [NEEDS CLARIFICATION: política de reintentos y tolerancia del programador de vencimientos — se define en fase de arquitectura, no en este spec].
- **Disputa ya finalizada**: cualquier intento de registro o transición sobre una disputa en RECHAZADA o ACEPTADA DEBE rechazarse (son estados finales).
- **Reserva que nunca llega a Completada**: no existe disputa; cualquier solicitud de reclamo sobre una reserva sin disputa asociada DEBE rechazarse indicando que no hay disputa abierta.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Al completarse `Marcar fin de navegación` con la reserva en estado `Completada`, el sistema DEBE crear automáticamente la disputa de garantía asociada a esa reserva. Este es el único disparador de creación.
- **FR-002**: Los flujos de cierre que terminan en estado `Cancelada` NO DEBEN generar disputa de garantía.
- **FR-003**: El actor creador de la disputa es el Sistema (creación automática). El Propietario NO genera disputas.
- **FR-004**: La creación DEBE registrar la disputa en estado PENDIENTE ejecutando la operación de `Actualizar estado de disputa de garantía` (`CU-17`, `<<include>>`).
- **FR-005**: Junto con la creación, el sistema DEBE abrir una ventana de 24 horas contada desde el momento de la creación, dentro de la cual el Propietario puede registrar su reclamo. [NEEDS CLARIFICATION: si la duración de la ventana debe ser configurable o es fija en 24 horas]
- **FR-006**: Dentro de la ventana abierta, el sistema DEBE permitir al Propietario registrado de la embarcación registrar su reclamo (descripción de los daños o problemas) sobre la disputa existente. El registro del reclamo NO cambia el estado de la disputa (sigue PENDIENTE).
- **FR-007**: El texto opcional de novedades adjuntado al cerrar la navegación (CU-07) NO cuenta como reclamo presentado.
- **FR-008**: Vencida la ventana de 24 horas sin reclamo registrado, el sistema DEBE cerrar automáticamente la disputa en estado RECHAZADA ejecutando la operación de `Actualizar estado de disputa de garantía` (`CU-17`, `<<include>>`), sin intervención del Admin, con motivo de sistema "sin reclamo en ventana" (campo informativo).
- **FR-009**: Un intento de reclamo fuera de la ventana (disputa ya en RECHAZADA por vencimiento) DEBE rechazarse sin modificar el estado de la disputa.
- **FR-010**: **REGLA DE NEGOCIO ESTRICTA (Sin dinero):** El sistema **NO DEBE manipular el depósito en garantía, ni ejecutar pagos, reembolsos, liquidaciones ni ninguna operación financiera**. Toda decisión monetaria pertenece exclusivamente a Módulo 3 a partir del estado final de la disputa.
- **FR-011**: Solo el cierre automático en RECHAZADA DEBE publicarse a Módulo 3 a través de `Recibir información de disputa de garantía` (`CU-18`). La creación en PENDIENTE es interna de Módulo 2 y NO se publica.

### Key Entities

- **Disputa de garantía**: objeto de dominio de Módulo 2, distinto de la Reserva y de su estado. Atributos clave: identificador de la disputa, identificador de la reserva asociada, estado (PENDIENTE / RECHAZADA / ACEPTADA, en mayúscula), inicio y fin de la ventana de 24 horas, reclamo registrado (opcional), motivo (opcional, informativo), historial de cambios de estado.
- **Reclamo del Propietario**: reporte presentado por el Propietario registrado dentro de la ventana. Atributos clave: identificador de la disputa, reclamante (Propietario registrado de la embarcación), descripción de los daños o problemas, marca temporal del registro.
- **Ventana de 24 horas**: periodo contado desde la creación de la disputa durante el cual se admite el reclamo; su vencimiento sin reclamo dispara el cierre automático en RECHAZADA.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de los cierres de navegación (reservas a `Completada`) generan exactamente una (1) disputa en PENDIENTE con ventana de 24 horas.
- **SC-002**: Cero (0%) disputas generadas por cierres que terminan en `Cancelada`.
- **SC-003**: El 100% de las disputas sin reclamo registrado se cierran en RECHAZADA al vencer la ventana, sin intervención humana.
- **SC-004**: Cero (0) operaciones financieras, manipulaciones del depósito o montos gestionados por Módulo 2 en este flujo.
- **SC-005**: El 100% de los reclamos fuera de ventana son rechazados sin modificar el estado de la disputa.

---

## Dudas abiertas de este spec

- **D-01**: ¿Puede el Propietario registrar más de un reclamo o editar el ya registrado mientras la ventana sigue abierta, o solo se admite un único registro?
- **D-02**: ¿La duración de la ventana de 24 horas debe ser configurable por parámetro o es fija?
- **D-03**: Política de reintentos del mecanismo de vencimiento (evento diferido / job) ante fallos de ejecución — se define en arquitectura.
