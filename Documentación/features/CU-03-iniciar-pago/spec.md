# Feature Specification: Iniciar Pago

**Módulo**: Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones  
**Fecha de Creación**: 2026-09-08 (Actualizado: 2026-09-28 por cambios de orquestación transaccional)  
**Actores Primarios**: Arrendatario (Turista / Cliente)  
**Dependencias Externas (APIs)**:
- **Módulo 3 – Liquidación, Seguros y Dispersión de Fondos**: Interfaz de la pasarela o motor de pagos donde se enviará al usuario con el monto exacto a cobrar.

- **Casos de uso internos de Módulo 2**:
    - `Iniciar reserva` `(<<include>>)`: Para instanciar obligatoriamente la reserva en BD en estado `Iniciada` si el usuario avanzó directamente al cobro sin persistencia previa.
    - `Brindar cálculo total de la reserva` `(<<include>>)`: Para obtener el monto final, exacto y desglosado (con seguro y garantía) antes de cobrar.
    - `Actualizar estado reserva` `(<<include>>)`: Para transicionar la reserva de `Iniciada` a `Pendiente de Pago` y activar el bloqueo de la embarcación (el TTL ya viene corriendo desde `Iniciada`).

---

## User Scenarios & Testing

### User Story 1 - Obtener cálculo exacto, bloquear inventario e iniciar el pago (Priority: P1)

Como Arrendatario que ha confirmado su intención de alquilar, quiero que el sistema guarde mi reserva, obtenga el cálculo total exacto de Finanzas y asegure el bloqueo de la embarcación, para poder completar mi transacción en la pasarela sin que otro usuario me gane las fechas.

***Why this priority***: Es el embudo transaccional crítico y orquestador maestro (Facade). Garantiza que el usuario pague exactamente lo que dictamina Finanzas y que la plataforma proteja la disponibilidad del barco exclusivamente para él mientras introduce su método de pago.

***Independent Test***: Se prueba completando los datos de viaje y accionando "Ir a Pagar". Se verifica que el sistema orqueste correctamente: llamando a `Iniciar reserva` para persistirla, a `Brindar cálculo total de la reserva` para obtener el valor, transicionando a `Pendiente de Pago` (con `Actualizar estado reserva`) sin reiniciar el TTL, y entregando los datos correctos para redirigir a la pasarela.

***Acceptance Scenarios***:

1. **Scenario**: Orquestación y transición exitosa a Pendiente de Pago
    - **Given** un Arrendatario que completó sus datos y presiona pagar
    - **When** el sistema detecta que no existe reserva y dispara la orquestación
    - **Then** el sistema invoca `(<<include>>)` a `Iniciar reserva` creándola en `Iniciada` (arrancando el TTL), invoca `Brindar cálculo total de la reserva` para obtener el valor final, luego invoca a `Actualizar estado reserva` transicionando a `Pendiente de Pago` (el TTL sigue corriendo, no se reinicia) y redirige al motor de Módulo 3.

2. **Scenario**: Falla al obtener el cálculo total desde Módulo 3
    - **Given** un Arrendatario en pleno proceso de orquestación de pago
    - **When** el sistema invoca "Brindar cálculo total de la reserva" pero Módulo 3 está indisponible o arroja error
    - **Then** el sistema aborta la operación, la reserva permanece en `Iniciada` (sin bloqueo de inventario) y se muestra un mensaje de error al usuario.

3. **Scenario**: Intento de pago sin aceptar la política de cancelación
    - **Given** un Arrendatario en la pantalla de pago con un temporizador activo
    - **When** el usuario intenta accionar el botón "Confirmar y Pagar" sin haber seleccionado el checkbox obligatorio "Acepto la Política de Cancelación"
    - **Then** el sistema bloquea el avance hacia la pasarela de Módulo 3, mantiene al usuario en la vista actual y resalta una advertencia indicando la obligación de aceptar las políticas.

---

### User Story 2 - Resolución de colisiones por concurrencia en la intención de pago (Priority: P1)

Como sistema, quiero evitar que dos usuarios bloqueen la misma embarcación para las mismas fechas, validando la disponibilidad justo antes de transicionar a Pendiente de Pago, para garantizar que solo el primero en intentar pagar obtenga el bloqueo del inventario.

***Why this priority***: Evita la sobreventa. Como antes de esta orquestación (en `Iniciada`) no existe ningún bloqueo sobre el barco, la validación final y atómica debe ocurrir en este instante preciso de transición.

***Independent Test***: Se preparan dos usuarios intentando pagar el mismo barco para las mismas fechas de forma simultánea. Se verifica que el sistema bloquee el barco para la primera petición (transicionando a `Pendiente de Pago`) y rechace la segunda transacción.

***Acceptance Scenarios***:

1. **Scenario**: Colisión de dos pagos simultáneos
    - **Given** dos reservas en estado `Iniciada` compitiendo por la misma embarcación en fechas superpuestas
    - **When** ambos Arrendatarios intentan iniciar el pago simultáneamente y el orquestador intenta pasarlas a `Pendiente de Pago`
    - **Then** el sistema aplica control de concurrencia atómico, permite que solo la primera transacción transicione a `Pendiente de Pago` (ganando el bloqueo) y rechaza la segunda solicitud indicando que el activo acaba de ser reservado por otro usuario.

---

### Edge Cases

- **Inicio de pago sin disponibilidad**: Si al validar de forma atómica las fechas ya se encuentran bloqueadas por otra reserva (en `Pendiente de Pago` o `Reservada`), el sistema DEBE denegar inmediatamente el avance sin transicionar la reserva (permanece en `Iniciada`).
- **Temporizador TTL de 15 minutos en curso**: El temporizador nace por el `<<include>>` de `Iniciar reserva` y sigue consumiéndose al entrar a `Pendiente de Pago`. Si el usuario abandona la pasarela y vuelve a entrar, el temporizador NO se reinicia.
- **Expiración del temporizador TTL en pantalla (00:00)**: Si el contador en cuenta regresiva llega a cero mientras el usuario permanece en la pantalla de pago, el sistema DEBE inhabilitar la acción de "Confirmar y Pagar", notificar la expiración del tiempo de reserva y redirigir al usuario o liberar el inventario bloqueado.

---

## Requirements

### Functional Requirements

- **FR-001**: Al detonarse la intención de pago, el sistema DEBE invocar obligatoriamente a `Iniciar reserva` (`<<include>>`) para instanciar la reserva en estado `Iniciada` con su respectivo TTL, en caso de que no haya sido persistida previamente.
- **FR-002**: El sistema DEBE invocar obligatoriamente al caso de uso subordinado `Brindar cálculo total de la reserva` (`<<include>>`) para solicitar a Módulo 3 el monto final vinculante, incluyendo el desglose de tarifa base, seguro náutico y depósito de garantía.
- **FR-003**: **REGLA DE NEGOCIO ESTRICTA**: El sistema **NO DEBE** manipular, sumar ni recalcular el valor devuelto por el cálculo total. Debe utilizar la estructura financiera entregada por Módulo 3 de manera intacta.
- **FR-004**: Si el cálculo total es devuelto con éxito, el sistema DEBE validar de forma atómica que las fechas de la reserva sigan disponibles (que no hayan sido bloqueadas por otra reserva que haya entrado a `Pendiente de Pago` o `Reservada` instantes antes).
- **FR-005**: Si la disponibilidad es validada, el sistema DEBE invocar al orquestador `Actualizar estado reserva` (`<<include>>`) para transicionar la reserva de `Iniciada` a `Pendiente de Pago`.
- **FR-006**: Al confirmarse la transición a `Pendiente de Pago`, el sistema NO DEBE reiniciar el temporizador TTL: el temporizador iniciado en `Iniciada` sigue corriendo y conserva su vencimiento original.
- **FR-007**: Si el proceso de validación concurrente falla (las fechas acaban de ser ocupadas), el sistema DEBE rechazar el inicio del pago sin transicionar la reserva (permanece en `Iniciada`) y notificar al usuario.
- **FR-008**: El sistema DEBE transferir el identificador de la reserva, el monto total devuelto por el cálculo y los datos del usuario hacia la interfaz o API de cobro de Módulo 3 para que el usuario efectúe la transacción.
- **FR-009**: El sistema DEBE desplegar visualmente en la pantalla la información resumida de la reserva: imagen de portada, nombre de la embarcación, rango de fechas, número total de noches, cantidad de pasajeros y ubicación/marina.
- **FR-010**: El sistema DEBE renderizar en la interfaz un temporizador dinámico visible en cuenta regresiva basado en el TTL de 15 minutos (ej. "Reserva expira en: 14:52").
- **FR-011**: El sistema DEBE mostrar el desglose financiero detallado proveniente de `Brindar cálculo total de la reserva`, incluyendo la fórmula explicativa de la tarifa base (días × tarifa diaria), comisión de la plataforma, seguro náutico, depósito de garantía reembolsable y el mensaje aclaratorio sobre las condiciones del reembolso.
- **FR-012**: El sistema DEBE incluir un componente de confirmación interactivo "Acepto la Política de Cancelación" junto con la condición explícita.
- **FR-013**: El sistema DEBE exigir la selección obligatoria del checkbox "Acepto la Política de Cancelación" como condición requerida antes de permitir la ejecución o habilitación del botón primario "Confirmar y Pagar".

---

### Key Entities

- **Reserva (`Reservation`)**: Entidad que sufre transiciones múltiples orquestadas en este flujo, de inexistente a `Iniciada` y luego a `Pendiente de Pago`, adquiriendo el bloqueo de inventario temporal; la marca de tiempo de expiración del TTL proviene de su nacimiento en `Iniciada` (el temporizador no se reinicia).

---

## Success Criteria

### Measurable Outcomes

- **SC-001**: El 100% de los intentos de inicio de pago exitosos culminan en una reserva orquestada a `Pendiente de Pago` sin reiniciar el temporizador heredado de `Iniciada`.
- **SC-002**: Cero (0%) sobreventas ante intentos de pago concurrentes sobre la misma embarcación en el mismo rango de fechas.
- **SC-003**: Cero (0) valores financieros calculados dentro del alcance de Módulo 2; el 100% de los cobros utilizan el dato provisto por `Brindar cálculo total de la reserva`.
- **SC-004**: El 100% de las transacciones redirigidas hacia Módulo 3 cuentan con la confirmación previa y explícita de la política de cancelación por parte del usuario.