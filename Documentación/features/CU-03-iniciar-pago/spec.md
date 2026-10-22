# Feature Specification: Iniciar Pago

**Módulo**: Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones  
**Fecha de Creación**: 2026-09-08 (Actualizado: 2026-09-28 por corrección estricta de relaciones UML de Iniciar Pago)  
**Actores Primarios**: Arrendatario (Turista / Cliente)  
**Dependencias Externas (APIs)**:
- **Módulo 3 – Liquidación, Seguros y Dispersión de Fondos**: Interfaz de la pasarela o motor de pagos donde se enviará al usuario con el monto exacto a cobrar.

- **Casos de uso internos de Módulo 2**:
    - `Ver detalle de reserva` (`<<extend>>`): Este caso de uso (`Iniciar pago`) **es la extensión** que se ancla a `Ver detalle de reserva` (la flecha del diagrama apunta al caso base). Se activa cuando el Arrendatario decide proceder al pago de una reserva en estado `Iniciada`.
    - `Brindar cálculo total de la reserva` `(<<include>>)`: Para obtener el monto final, exacto y desglosado (con seguro y garantía) antes de cobrar.
    - `Actualizar estado reserva` `(<<include>>)`: Para transicionar la reserva de `Iniciada` a `Pendiente de Pago` (el TTL ya viene corriendo desde `Iniciada`). Esta transición **no** notifica nada a Módulo 1: el bloqueo operativo en Módulo 1 se materializa al alcanzar `Reservada` (ver CU-22 FR-002).
    - `Brindar información de estado operativo` `(<<include>>)`: Para verificar, vía la API de Módulo 1, que la embarcación figura como `Disponible` en la validación atómica previa al bloqueo (guarda frente a activos ya bloqueados en otros estados; el bloqueo efectivo en Módulo 1 se produce al alcanzar `Reservada`, ver CU-22 FR-002).

> [!NOTE] Resolución (2026-10-07): el caso base de `Iniciar pago` es `Ver detalle de reserva` (CU-21), donde se muestra el resumen de pago. `Ver mis reservas` (CU-20) solo habilita la navegación hacia el detalle.

---

## User Scenarios & Testing

### User Story 1 - Obtener cálculo exacto, iniciar el pago y asegurar las fechas (Priority: P1)

Como Arrendatario con una reserva en estado `Iniciada` (con su TTL ya en curso desde que se creó), quiero proceder al pago obteniendo el cálculo total exacto (incluyendo seguro y garantía) y asegurando la retención exclusiva de las fechas dentro de Módulo 2 (sin bloquear aún el inventario en Módulo 1) para poder completar mi transacción sin que otro usuario me gane las fechas.

***Why this priority***: Es el embudo transaccional crítico. Garantiza que el usuario pague exactamente lo que dictamina Finanzas y que la plataforma proteja la disponibilidad del barco exclusivamente para él mientras introduce su método de pago.

***Independent Test***: Se prueba con una reserva en estado `Iniciada` con su TTL en curso (accesible desde `Ver detalle de reserva`). Se ejecuta la acción de pagar y se verifica que el sistema llame a `Brindar cálculo total de la reserva`, transicione la reserva a estado `Pendiente de Pago` (llamando a `Actualizar estado reserva`, sin reiniciar el TTL que sigue corriendo desde `Iniciada`, sin generar ninguna notificación a Módulo 1) y entregue los datos correctos para redirigir a la pasarela de Módulo 3.

***Acceptance Scenarios***:

1. **Scenario**: Transición exitosa a Pendiente de Pago e inicio de pasarela
    - **Given** un Arrendatario con una reserva en estado `Iniciada` con su TTL en curso, para fechas disponibles
    - **When** el usuario decide proceder con el pago (extendiendo desde `Ver detalle de reserva`)
    - **Then** el sistema invoca `(<<include>>)` a "Brindar cálculo total de la reserva" para obtener el valor final, luego invoca `(<<include>>)` a "Actualizar estado reserva" transicionando la reserva de `Iniciada` a `Pendiente de Pago` (el TTL sigue corriendo desde `Iniciada`, no se reinicia; esta transición no notifica nada a Módulo 1) y redirige al motor de Módulo 3.

2. **Scenario**: Falla al obtener el cálculo total desde Módulo 3
    - **Given** un Arrendatario intentando pagar una reserva en estado `Iniciada`
    - **When** el sistema invoca "Brindar cálculo total de la reserva" pero Módulo 3 está indisponible o arroja error
    - **Then** el sistema aborta la operación sin transicionar la reserva (permanece en `Iniciada` con su TTL en curso), NO bloquea el inventario y muestra un mensaje de error al usuario.

3. **Scenario**: Intento de pago sin aceptar la política de cancelación
    - **Given** un Arrendatario en la pantalla de pago con un temporizador activo
    - **When** el usuario intenta accionar el botón "Confirmar y Pagar" sin haber seleccionado el checkbox obligatorio "Acepto la Política de Cancelación"
    - **Then** el sistema bloquea el avance hacia la pasarela de Módulo 3, mantiene al usuario en la vista actual y resalta una advertencia indicando la obligación de aceptar las políticas de cancelación.

---

### User Story 2 - Resolución de colisiones por concurrencia en la intención de pago (Priority: P1)

Como sistema, quiero evitar que dos usuarios retengan las mismas fechas para la misma embarcación, validando la disponibilidad justo antes de transicionar a Pendiente de Pago, para garantizar que solo el primero en intentar pagar obtenga la retención exclusiva de las fechas.

***Why this priority***: Evita la sobreventa. Como antes del pago no existe ninguna retención sobre el barco (ni en Módulo 2 ni en Módulo 1), la validación final y atómica debe ocurrir en este instante preciso.

***Independent Test***: Se preparan dos reservas en estado `Iniciada` para el mismo barco y las mismas fechas desde dos cuentas distintas. Se disparan ambas peticiones de pago en el mismo segundo. Se verifica que el sistema retenga las fechas para la primera petición (transicionando a `Pendiente de Pago`) y rechace la segunda transacción informando que las fechas ya no están disponibles.

***Acceptance Scenarios***:

1. **Scenario**: Colisión de dos pagos simultáneos sobre reservas en Iniciada
    - **Given** dos reservas en estado `Iniciada` compitiendo por la misma embarcación en fechas superpuestas
    - **When** ambos Arrendatarios intentan iniciar el pago simultáneamente
    - **Then** el sistema aplica control de concurrencia, permite que solo la primera transacción transicione a `Pendiente de Pago` (ganando la retención exclusiva) y rechaza la segunda solicitud indicando que el activo acaba de ser reservado por otro usuario.

---

## Edge Cases

- **Inicio de pago sin disponibilidad**: Si al validar de forma atómica bajo lock la embarcación ya no figura Disponible en el chequeo instantáneo de estado operativo (CU-10) (bloqueada en Módulo 1 por otra reserva que alcanzó `Reservada`, o retenida internamente por otra reserva en `Pendiente de Pago`), el sistema DEBE denegar inmediatamente el inicio del flujo sin transicionar la reserva (permanece en `Iniciada`).
- **Temporizador TTL de 15 minutos en curso**: El temporizador corre desde que la reserva entró a `Iniciada` y sigue consumiéndose al entrar a `Pendiente de Pago`. Si el usuario abandona la pasarela y vuelve a entrar, el temporizador NO se reinicia.
- **Expiración del temporizador TTL en pantalla (00:00)**: El corte en el backend es estricto a los 15 minutos exactos sin ventana de gracia. Al llegar el contador a 00:00 en pantalla, el sistema DEBE deshabilitar los botones de inmediato, mostrar un modal de expiración, redirigir a CU-01 y el backend debe liberar el inventario.

---

## Requirements

### Functional Requirements

- **FR-001**: El sistema DEBE activarse como una extensión (`<<extend>>`) desde el caso base `Ver detalle de reserva` cuando el Arrendatario decide efectuar el pago de una reserva en estado `Iniciada`.
- **FR-002**: El sistema DEBE permitir iniciar el proceso de pago única y exclusivamente si la reserva se encuentra en estado `Iniciada`.
- **FR-003**: El sistema DEBE invocar obligatoriamente al caso de uso subordinado `Brindar cálculo total de la reserva` (`<<include>>`) para solicitar a Módulo 3 el monto final vinculante, incluyendo el desglose de tarifa base, seguro náutico y depósito de garantía.
- **FR-004**: **REGLA DE NEGOCIO ESTRICTA**: El sistema **NO DEBE** manipular, sumar ni recalcular el valor devuelto por el cálculo total. Debe utilizar la estructura financiera entregada por Módulo 3 de manera intacta.
- **FR-005**: Si el cálculo total es devuelto con éxito, el sistema DEBE validar de forma atómica bajo lock que la embarcación figure Disponible en el chequeo instantáneo de estado operativo mediante la invocación `(<<include>>)` a `Brindar información de estado operativo` (CU-10) (verificando que no haya sido bloqueada en Módulo 1 por otra reserva que haya alcanzado `Reservada`; las reservas que siguen en `Pendiente de Pago` se detectan mediante los registros de disponibilidad de Módulo 2).
- **FR-006**: Si la disponibilidad es validada, el sistema DEBE invocar al orquestador `Actualizar estado reserva` (`<<include>>`) para transicionar la reserva de `Iniciada` a `Pendiente de Pago`.
- **FR-007**: Al confirmarse la transición a `Pendiente de Pago`, el sistema NO DEBE reiniciar el temporizador TTL: el temporizador iniciado en `Iniciada` sigue corriendo y conserva su vencimiento original.
- **FR-008**: Si el proceso de validación concurrente falla (las fechas acaban de ser ocupadas), el sistema DEBE rechazar el inicio del pago sin transicionar la reserva (permanece en `Iniciada`) y notificar al usuario.
- **FR-009**: El sistema DEBE transferir el identificador de la reserva, el monto total devuelto por el cálculo y los datos del usuario hacia la interfaz o API de cobro de Módulo 3 para que el usuario efectúe la transacción.
- **FR-010**: El sistema DEBE desplegar visualmente en la pantalla la información resumida de la reserva: imagen de portada, nombre de la embarcación, rango de fechas, número total de noches, cantidad de pasajeros, modalidad de viaje (ej. "Viaje con capitán") y ubicación/marina.
- **FR-011**: El sistema DEBE renderizar en la interfaz un temporizador dinámico visible en cuenta regresiva basado en el TTL de 15 minutos (ej. "Reserva expira en: 14:52") acompañado en la parte inferior por el texto confirmatorio "Precio y disponibilidad bloqueados"
- **FR-012**: El sistema DEBE exigir mostrar el total de la reserva y el desglose oficial proporcionado por Módulo 3 a través de `Brindar cálculo total de la reserva` (tarifa base, seguro náutico, depósito de garantía reembolsable y el mensaje aclaratorio sobre las condiciones del reembolso: "El depósito se reembolsa completo si el barco se devuelve sin daños"). El sistema NO DEBE mostrar, calcular ni presentar montos derivados como comisión o neto a recibir.
- **FR-013**: El sistema DEBE incluir un componente de confirmación interactivo "Acepto la Política de Cancelación" junto con la condición explícita (ej. "Cancelación gratis hasta 72h antes del inicio del viaje").
- **FR-014**: El sistema DEBE exigir la selección obligatoria del checkbox "Acepto la Política de Cancelación" como condición requerida antes de permitir la ejecución o habilitación del botón primario "Confirmar y Pagar".
- **FR-015**: El sistema DEBE presentar un panel lateral de resumen que destaque el "Total a pagar" general en mayor tamaño, un sub-desglose de los rubros, el botón de acción principal y un texto de retroalimentación dinámico indicando "Se requiere tu consentimiento para completar el pago." cuando las políticas aún no hayan sido aceptadas.

---

### Key Entities

- **Reserva (`Reservation`)**: Entidad que transiciona de `Iniciada` a `Pendiente de Pago` en este flujo, adquiriendo la retención exclusiva temporal de las fechas dentro de Módulo 2 (sin bloqueo de inventario en Módulo 1, que se materializa al alcanzar `Reservada`); la marca de tiempo de expiración del TTL proviene del estado `Iniciada` (el temporizador no se reinicia).

---

## Success Criteria

### Measurable Outcomes

- **SC-001**: El 100% de los intentos de inicio de pago exitosos transicionan la reserva de `Iniciada` a `Pendiente de Pago` sin reiniciar el temporizador heredado de `Iniciada`.
- **SC-002**: Cero (0%) sobreventas ante intentos de pago concurrentes sobre la misma embarcación en el mismo rango de fechas.
- **SC-003**: Cero (0) valores financieros calculados dentro del alcance de Módulo 2; el 100% de los cobros utilizan el dato provisto por `Brindar cálculo total de la reserva`.
- **SC-004**: El 100% de las transacciones redirigidas hacia Módulo 3 cuentan con la confirmación previa y explícita de la política de cancelación por parte del usuario.
