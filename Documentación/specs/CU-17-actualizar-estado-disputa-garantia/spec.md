# Feature Specification: Actualizar Estado de Disputa de Garantía

**Módulo**: Módulo 2 (Operación de Reservas, Tiempos y Cancelaciones)  
**Fecha de Creación**: 2026-09-26  
**Actores Primarios**: Admin (Administrador: revisa el reclamo presentado por el Propietario y actualiza el estado de la disputa; no introduce montos ni ejecuta operaciones de pasarela)  
**Dependencias Externas (APIs)**: Ninguna directa.  
**Casos de uso internos de Módulo 2**:
- `Generar disputa de garantía` (`CU-16`, caso base): este caso de uso es el incluido (`<<include>>`). Tanto el registro inicial en PENDIENTE como el cierre automático por vencimiento en RECHAZADA (sin intervención del Admin) se ejecutan a través de la operación definida aquí, para mantener una sola fuente de verdad sobre cómo se transiciona el estado de la disputa.

> **Nota de dominio**: los estados **PENDIENTE**, **RECHAZADA** y **ACEPTADA** (en mayúscula) pertenecen al objeto **Disputa de garantía**, que es distinto al estado de la reserva (`Completada`, `Reservada`, `Pendiente de Pago`, etc.). Ninguna regla de este spec modifica el estado de la reserva.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - El Admin revisa el reclamo y resuelve la disputa (Priority: P1)

El Admin revisa el reclamo presentado por el Propietario sobre una disputa en PENDIENTE y actualiza su estado a ACEPTADA (el reclamo procede y el depósito debe entregarse al Propietario) o a RECHAZADA (el reclamo no procede), pudiendo adjuntar un motivo opcional. La decisión es únicamente administrativa y operativa: el Admin no introduce montos ni ejecuta operaciones de pasarela ni de pago.

**Why this priority**: Es la resolución humana del flujo de disputa. Sin esta revisión, ningún reclamo llegaría a un estado final por vía administrativa y Módulo 3 no tendría un veredicto sobre el cual decidir el destino del depósito.

**Independent Test**: Se prueba con disputas en PENDIENTE con reclamo registrado, ejecutando la actualización por el Admin a ACEPTADA y a RECHAZADA (con y sin motivo), y verificando que el estado cambia, que el motivo queda guardado como campo informativo y que no se produce ningún movimiento de dinero ni cálculo en Módulo 2.

**Acceptance Scenarios**:

1. **Scenario**: Reclamo procedente → ACEPTADA
   - **Given** una disputa en estado PENDIENTE con reclamo registrado por el Propietario
   - **When** el Admin revisa el reclamo y lo encuentra procedente
   - **Then** el sistema actualiza la disputa a ACEPTADA (el depósito debe entregarse al Propietario, decisión que ejecuta Módulo 3) sin introducir montos ni operar pasarelas

2. **Scenario**: Reclamo improcedente → RECHAZADA con motivo opcional
   - **Given** una disputa en estado PENDIENTE con reclamo registrado por el Propietario
   - **When** el Admin revisa el reclamo y lo encuentra improcedente
   - **Then** el sistema actualiza la disputa a RECHAZADA, guardando el motivo si el Admin lo proveyó (campo opcional, solo informativo y de trazabilidad, sin efecto financiero)

### User Story 2 - El Admin mantiene la disputa en revisión (Priority: P2)

Si la revisión todavía no terminó, el Admin puede dejar constancia de que la disputa sigue en PENDIENTE (la revisión continúa), sin resolverla aún.

**Why this priority**: Permite trazabilidad del trabajo de revisión en curso, pero no es indispensable para el MVP: una disputa en PENDIENTE ya expresa por sí misma que la revisión no terminó.

**Independent Test**: Se prueba con una disputa en PENDIENTE ejecutando la actualización a PENDIENTE por el Admin, y verificando que el estado se mantiene, que queda registro de la revisión en curso y que no se notifica ningún cambio efectivo.

**Acceptance Scenarios**:

1. **Scenario**: Revisión en curso, sigue PENDIENTE
   - **Given** una disputa en estado PENDIENTE cuya revisión aún no terminó
   - **When** el Admin registra que la revisión continúa
   - **Then** el sistema mantiene la disputa en PENDIENTE con registro de la revisión en curso, sin cambios efectivos de estado

---

### Edge Cases

- **Actualización sobre estado final**: si la disputa ya está en RECHAZADA o ACEPTADA (incluido el cierre automático por vencimiento), el sistema DEBE rechazar cualquier intento de cambio (ni el Admin ni ningún otro actor pueden reabrirla). RECHAZADA y ACEPTADA son estados finales.
- **Carrera entre resolución del Admin y cierre por vencimiento**: si el Admin resuelve en el mismo instante en que vence la ventana, el sistema DEBE garantizar atomicidad: solo una de las dos transiciones se aplica; la otra se rechaza (si ganó el vencimiento, la disputa ya está finalizada; si ganó el Admin, el vencimiento posterior no la altera).
- **Actualización sin reclamo presentado**: la precondición de la revisión por el Admin es el estado PENDIENTE. [DUDA — ver lista de dudas al final del documento] no está definido si el Admin puede resolver una disputa PENDIENTE que aún no tiene reclamo registrado (antes del vencimiento).
- **Motivo extenso o con formato**: el motivo es texto libre opcional; [NEEDS CLARIFICATION: longitud máxima y reglas de contenido del campo motivo — se define en diseño, no en este spec].
- **Admin sin permisos**: solo el rol Admin puede ejecutar este caso de uso; cualquier otro actor DEBE ser rechazado.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE permitir al Admin actualizar el estado de una disputa si y solo si la disputa existe y se encuentra en estado PENDIENTE.
- **FR-002**: Desde PENDIENTE, los únicos estados destino permitidos son PENDIENTE (la revisión todavía no terminó), RECHAZADA (el reclamo no procede o no fue presentado dentro del plazo) o ACEPTADA (el reclamo procede y el depósito debe entregarse al Propietario).
- **FR-003**: Si la disputa ya está en RECHAZADA o ACEPTADA, el sistema DEBE rechazar cualquier intento de cambio de estado. Ambos son estados finales sin reapertura.
- **FR-004**: El motivo del rechazo DEBE ser un campo opcional. NO debe ser obligatorio ni utilizarse para determinar ninguna operación financiera; su único propósito es informativo y de trazabilidad.
- **FR-005**: **REGLA DE NEGOCIO ESTRICTA (Sin dinero):** El Admin y el sistema **NO DEBEN introducir montos, ni ejecutar operaciones de pasarela ni de pago** en este caso de uso. La decisión es únicamente administrativa y operativa; el destino del dinero lo resuelve Módulo 3 a partir del estado final.
- **FR-006**: El cierre automático por vencimiento del Caso de uso 1 (`Generar disputa de garantía`) DEBE ejecutarse a través de esta misma operación de cambio de estado (PENDIENTE → RECHAZADA, actor Sistema, motivo de sistema), para mantener una sola fuente de verdad sobre las transiciones.
- **FR-007**: Cada cambio de estado a RECHAZADA o ACEPTADA DEBE publicarse a Módulo 3 a través de `Recibir información de disputa de garantía` (`CU-18`). El registro inicial en PENDIENTE es interno y NO se publica.

### Key Entities

- **Disputa de garantía**: objeto de dominio de Módulo 2, distinto de la Reserva y de su estado. Se referencia aquí para su actualización; su creación vive en `Generar disputa de garantía` (`CU-16`).
- **Revisión administrativa**: actuación del Admin sobre una disputa en PENDIENTE. Atributos clave: identificador de la disputa, revisor (Admin), decisión (PENDIENTE / RECHAZADA / ACEPTADA), motivo opcional (texto libre, solo informativo), marca temporal de la revisión.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de las actualizaciones del Admin se aplican solo sobre disputas en PENDIENTE, con destino PENDIENTE, RECHAZADA o ACEPTADA.
- **SC-002**: Cero (0%) cambios de estado aplicados sobre disputas en RECHAZADA o ACEPTADA (estados finales inmutables).
- **SC-003**: Cero (0) montos introducidos y cero (0) operaciones de pasarela o pago ejecutadas en este flujo.
- **SC-004**: El 100% de los rechazos con motivo lo guardan solo como campo informativo, sin condicionar ninguna operación financiera.

---

## Dudas abiertas de este spec

- **D-01**: ¿Puede el Admin resolver (ACEPTADA/RECHAZADA) una disputa PENDIENTE que aún no tiene reclamo registrado, antes de que venza la ventana?
- **D-02**: Longitud máxima y reglas de contenido del campo motivo — se define en diseño.
