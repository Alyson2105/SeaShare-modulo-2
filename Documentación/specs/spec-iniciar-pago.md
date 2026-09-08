# Feature Specification: Iniciar Pago

**Módulo**: Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones  
**Fecha de Creación**: 2026-09-08 (Actualizado para arquitectura de estado Borrador y cálculo total)  
**Actores Primarios**: Arrendatario (Turista / Cliente)  
**Dependencias Externas (APIs)**:
- **Módulo 3 – Liquidación, Seguros y Dispersión de Fondos**: Interfaz de la pasarela o motor de pagos donde se enviará al usuario con el monto exacto a cobrar.

- **Casos de uso internos de Módulo 2**:
    - `Brindar cálculo total de la reserva` `(<<include>>)`: Para obtener el monto final, exacto y desglosado (con seguro y garantía) antes de cobrar.
    - `Actualizar estado reserva` `(<<include>>)`: Para transicionar la reserva a `Pendiente de Pago` e iniciar el bloqueo de la embarcación.

---

## User Scenarios & Testing

### User Story 1 - Obtener cálculo exacto, bloquear inventario e iniciar el pago (Priority: P1)

Como Arrendatario, quiero proceder al pago de mi reserva en estado borrador, obteniendo el cálculo total exacto (incluyendo seguro y garantía) y asegurando el bloqueo de la embarcación por 15 minutos para poder completar mi transacción sin que otro usuario me gane las fechas.

***Why this priority***: Es el embudo transaccional crítico. Garantiza que el usuario pague exactamente lo que dictamina Finanzas y que la plataforma proteja la disponibilidad del barco exclusivamente para él mientras introduce su método de pago.

***Independent Test***: Se prueba con una reserva en estado `Borrador`. Se ejecuta la acción de pagar y se verifica que el sistema llame a `Brindar cálculo total de la reserva`, transicione la reserva a `Pendiente de Pago` (llamando a `Actualizar estado reserva`), inicie el TTL de 15 minutos exactos[cite: 2] y entregue los datos correctos para redirigir a la pasarela de Módulo 3.

***Acceptance Scenarios***:

1. **Scenario**: Transición exitosa a Pendiente de Pago e inicio de pasarela
    - **Given** un Arrendatario con una reserva en estado `Borrador` para fechas disponibles
    - **When** el usuario decide proceder con el pago
    - **Then** el sistema invoca `(<<include>>)` a "Brindar cálculo total de la reserva" para obtener el valor final, luego invoca `(<<include>>)` a "Actualizar estado reserva" pasando la reserva a `Pendiente de Pago`, enciende el TTL de 15 minutos y redirige al motor de Módulo 3

2. **Scenario**: Falla al obtener el cálculo total desde Módulo 3
    - **Given** un Arrendatario intentando pagar una reserva en estado `Borrador`
    - **When** el sistema invoca "Brindar cálculo total de la reserva" pero Módulo 3 está indisponible o arroja error
    - **Then** el sistema aborta la operación, mantiene la reserva en `Borrador`, NO bloquea el inventario y muestra un mensaje de error al usuario

---

### User Story 2 - Resolución de colisiones por concurrencia en la intención de pago (Priority: P1)

Como sistema, quiero evitar que dos usuarios bloqueen la misma embarcación para las mismas fechas, validando la disponibilidad justo antes de transicionar a Pendiente de Pago, para garantizar que solo el primero en intentar pagar obtenga el bloqueo del inventario.

***Why this priority***: Evita la sobreventa. Como el estado `Borrador` no bloquea el barco, la validación final y atómica debe ocurrir en este instante preciso.

***Independent Test***: Se crean dos reservas en `Borrador` para el mismo barco y las mismas fechas desde dos cuentas distintas. Se disparan ambas peticiones de pago en el mismo segundo. Se verifica que el sistema bloquee el barco para la primera petición (pasando a `Pendiente de Pago`) y rechace la segunda transacción informando que las fechas ya no están disponibles.

***Acceptance Scenarios***:

1. **Scenario**: Colisión de dos reservas en Borrador al iniciar el pago
    - **Given** dos reservas en estado `Borrador` compitiendo por la misma embarcación en fechas superpuestas
    - **When** ambos Arrendatarios intentan iniciar el pago simultáneamente
    - **Then** el sistema aplica control de concurrencia, permite que solo la primera transacción actualice a `Pendiente de Pago` (ganando el bloqueo) y rechaza la segunda solicitud indicando que el activo acaba de ser reservado por otro usuario

---

### Edge Cases

- **Inicio de pago sobre reserva no válida**: Si la reserva no se encuentra en estado `Borrador` (ej. ya fue pagada, cancelada o está en `Pendiente de Pago` en otra pestaña), el sistema DEBE denegar inmediatamente el inicio del flujo.
- **Temporizador TTL de 15 minutos en curso**: Una vez que se entra a `Pendiente de Pago`, el usuario tiene un Time-To-Live estricto de 15 minutos[cite: 2]. Si el usuario abandona la pasarela y vuelve a entrar, el temporizador NO se reinicia; sigue consumiéndose desde el primer inicio de pago.

---

## Requirements

### Functional Requirements

- **FR-001**: El sistema DEBE permitir iniciar el proceso de pago única y exclusivamente si la reserva se encuentra en estado principal `Borrador`.
- **FR-002**: El sistema DEBE invocar obligatoriamente al caso de uso subordinado `Brindar cálculo total de la reserva` `(<<include>>)` para solicitar a Módulo 3 el monto final vinculante, incluyendo el desglose de tarifa base, seguro náutico y depósito de garantía.
- **FR-003**: **REGLA DE NEGOCIO ESTRICTA**: El sistema **NO DEBE** manipular, sumar ni recalcular el valor devuelto por el cálculo total[cite: 2]. Debe utilizar la estructura financiera entregada por Módulo 3 de manera intacta.
- **FR-004**: Si el cálculo total es devuelto con éxito, el sistema DEBE validar de forma atómica que las fechas de la reserva sigan disponibles (que no hayan sido bloqueadas por otra reserva que haya entrado a `Pendiente de Pago` o `Confirmada` instantes antes).
- **FR-005**: Si la disponibilidad es validada, el sistema DEBE invocar al orquestador `Actualizar estado reserva` `(<<include>>)` para transicionar la reserva de `Borrador` a `Pendiente de Pago`.
- **FR-006**: Al confirmarse la transición a `Pendiente de Pago`, el sistema DEBE iniciar un temporizador de expiración (TTL) estricto de 15 minutos asociado a la reserva[cite: 2].
- **FR-007**: Si el proceso de validación concurrente falla (las fechas acaban de ser ocupadas), el sistema DEBE rechazar el inicio del pago, mantener la reserva en `Borrador` (o marcarla como inválida) y notificar al usuario.
- **FR-008**: El sistema DEBE transferir el identificador de la reserva, el monto total devuelto por el cálculo y los datos del usuario hacia la interfaz o API de cobro de Módulo 3 para que el usuario efectúe la transacción.

---

### Key Entities

- **Reserva (`Reservation`)**: Entidad que experimenta la transición crítica de `Borrador` a `Pendiente de Pago`, adquiriendo el bloqueo de inventario temporal y la marca de tiempo de expiración (TTL de 15 minutos).

---

## Success Criteria

### Measurable Outcomes

- **SC-001**: El 100% de los intentos de inicio de pago transicionan la reserva a `Pendiente de Pago` y arrancan correctamente el temporizador de 15 minutos.
- **SC-002**: Cero (0%) sobreventas ante intentos de pago concurrentes sobre la misma embarcación en el mismo rango de fechas.
- **SC-003**: Cero (0) valores financieros calculados dentro del alcance de Módulo 2; el 100% de los cobros utilizan el dato provisto por `Brindar cálculo total de la reserva`.