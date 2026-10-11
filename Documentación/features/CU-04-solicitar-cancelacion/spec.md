# Feature Specification: Solicitar Cancelación

**Módulo**: Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones  
**Fecha de Creación**: 2026-09-06 (Actualizado: 2026-09-28 por sincronización de arquitectura, relaciones UML y limpieza de User Stories)  
**Actores Primarios**: Arrendatario y Propietario (ambos interactúan directamente con este caso de uso en la plataforma)  
**Dependencias Externas (APIs)**:
- **Casos de uso internos de Módulo 2**:
    - `Ver detalle de reserva` (`<<extend>>`): Este caso de uso (`Solicitar cancelación`) **es la extensión** que se ancla a `Ver detalle de reserva` (la flecha del diagrama apunta al caso base). Se activa cuando el usuario, revisando su reserva confirmada en la vista profunda, decide ejercer la cancelación.
    - `Actualizar estado reserva` (`<<include>>`): Para ejecutar la transición formal al estado principal terminal `Cancelada` con el sub-estado clasificado (`Flexible`, `Moderado`, `Tardío`, `Por Propietario` o `Por Inasistencia`).
    - `Proveer información de embarcación` (`CU-09`, `<<include>>`): Para consultar el puerto de atraque de la embarcación y determinar la zona horaria oficial del activo aplicable al cálculo de anticipación.

---

## User Scenarios & Testing

### User Story 1 - El Arrendatario solicita cancelar una reserva en estado Reservada antes del inicio del servicio (Priority: P1)

Como Arrendatario, quiero solicitar la cancelación voluntaria de mi reserva en estado Reservada desde la vista de detalles para desistir del viaje y que el sistema determine la clasificación de mi cancelación según el tiempo de anticipación respecto al zarpe, permitiendo a Módulo 3 tramitar el reembolso correspondiente.

**Why this priority**: Constituye el mecanismo contractual de salida unilateral para el cliente, activando de forma controlada la política de cancelaciones de la plataforma, clasificando la penalidad/reembolso según los tiempos pactados y devolviendo la disponibilidad del activo al inventario náutico.

**Independent Test**: Se prueba accediendo a una reserva en estado "Reservada" desde `Ver detalle de reserva`, solicitando la cancelación en los tres umbrales temporales del proyecto (>72h, entre 72h y 24h, y <24h antes del zarpe, evaluados en la zona horaria del puerto de atraque). Se verifica el cálculo algorítmico interno de anticipación, la asignación directa del sub-estado correspondiente, la invocación a `(<<include>>)` a `Actualizar estado reserva`, la liberación del activo en Módulo 1 y la notificación a Módulo 3 sin que Módulo 2 realice cálculos monetarios.

**Acceptance Scenarios**:

1. **Scenario**: Cancelación voluntaria con más de 72 horas de anticipación (Flexible) detonada desde el detalle
    - **Given** un Arrendatario titular visualizando el detalle de una reserva en estado "Reservada" cuya hora pactada de inicio es en más de 72 horas en la zona horaria del puerto
    - **When** el usuario ejecuta la acción de cancelar (disparando el `<<extend>>` hacia `Ver detalle de reserva`)
    - **Then** el sistema calcula la anticipación temporal (>72h), determina internamente la clasificación "Flexible", invoca la relación `(<<include>>)` con `Actualizar estado reserva` fijando el estado "Cancelada" con el sub-estado "Flexible", notifica a Módulo 1 para liberar la embarcación a estado "Disponible" y notifica a Módulo 3 para tramitar el reembolso total al cliente.

2. **Scenario**: Cancelación voluntaria entre 72 y 24 horas de anticipación (Moderado)
    - **Given** una reserva en estado principal "Reservada" cuya hora pactada de inicio es entre 24 y 72 horas en la zona horaria del puerto de atraque
    - **When** el Arrendatario titular solicita cancelarla desde el detalle de la reserva
    - **Then** el sistema calcula la anticipación temporal (entre 24h y 72h), determina la clasificación "Moderado", invoca `Actualizar estado reserva` fijando el estado "Cancelada" con el sub-estado "Moderado" y notifica a Módulo 3.

3. **Scenario**: Cancelación voluntaria con menos de 24 horas de anticipación (Tardío)
    - **Given** una reserva en estado principal "Reservada" cuya hora pactada de inicio es en menos de 24 horas en la zona horaria del puerto de atraque
    - **When** el Arrendatario titular solicita cancelarla desde el detalle de la reserva
    - **Then** el sistema calcula la anticipación temporal (<24h), determina la clasificación "Tardío", invoca `Actualizar estado reserva` fijando el estado "Cancelada" con el sub-estado "Tardío" y notifica a Módulo 3.

4. **Scenario**: Desestimación de la cancelación desde la ventana emergente
    - **Given** un Arrendatario en la ventana emergente de cancelación viendo la clasificación calculada
    - **When** el usuario hace clic en el botón "MANTENER RESERVA"
    - **Then** el sistema cierra la ventana emergente, no ejecuta ninguna transición de estado y conserva la reserva en estado "Reservada".

---

### User Story 2 - El Propietario solicita cancelar una reserva en estado Reservada antes del inicio del servicio (Priority: P1)

Como Propietario de la embarcación, quiero cancelar una reserva en estado Reservada antes del zarpe por inconvenientes de fuerza mayor logística o averías mecánicas desde el detalle de la reserva, para liberar el compromiso contractual, registrar la causa y permitir que el sistema asigne la clasificación "Por Propietario" para reembolsar al cliente.

**Why this priority**: Es la contraparte de protección al consumidor que garantiza que, ante incumplimiento o indisponibilidad del propietario, la reserva quede formalmente cerrada con clasificación directa "Por Propietario" (sin aplicar franjas horarias de anticipación), el activo náutico se actualice adecuadamente y el Arrendatario reciba la devolución íntegra de su dinero gestionada por Módulo 3.

**Independent Test**: Se prueba ejecutando la cancelación por parte del Propietario sobre una reserva en estado Reservada antes del check-in, independientemente del tiempo restante para el zarpe. Se valida que el sistema asigne directamente el sub-estado "Por Propietario" sin evaluar umbrales horarios, registre el motivo, invoque a `Actualizar estado reserva`, actualice el activo náutico en Módulo 1 de acuerdo a la causa e instruya la liquidación a Módulo 3.

**Acceptance Scenarios**:

1. **Scenario**: Cancelación por Propietario por indisponibilidad logística (Embarcación liberada)
    - **Given** una reserva en estado principal "Reservada" previa al zarpe visualizada por el Propietario
    - **When** el Propietario registrado solicita la cancelación indicando motivos de fuerza mayor logística
    - **Then** el sistema asigna directamente la clasificación "Por Propietario" sin evaluar la anticipación horaria, invoca a `Actualizar estado reserva` asentando "Cancelada" con sub-estado "Por Propietario", actualiza la embarcación a "Disponible" en Módulo 1 y notifica a Módulo 3.

2. **Scenario**: Cancelación por Propietario por desperfecto mecánico (Embarcación a mantenimiento)
    - **Given** una reserva en estado principal "Reservada" previa al zarpe
    - **When** el Propietario solicita la cancelación notificando una avería mecánica en el motor
    - **Then** el sistema asigna la clasificación "Por Propietario", invoca a `Actualizar estado reserva`, y notifica a Módulo 1 para actualizar la embarcación a estado operativo "En Mantenimiento/Limpieza", bloqueando su oferta comercial.

3. **Scenario**: Intento de confirmación sin seleccionar motivo de cancelación
    - **Given** un Propietario en la ventana emergente de cancelación de reserva
    - **When** el usuario intenta hacer clic en "CONFIRMAR CANCELACIÓN" sin seleccionar entre "Fuerza mayor logística" o "Avería o desperfecto mecánico"
    - **Then** el sistema bloquea el envío de la solicitud y resalta visualmente la obligación de seleccionar una causa de cancelación.

---

## Edge Cases

- **Resolución de límites exactos de anticipación (72h o 24h en punto)**:
    - Si la solicitud del Arrendatario se procesa exactamente en el límite de las 72 horas y 0 segundos (`anticipacion == 72h:00m:00s`), el sistema clasifica la cancelación como **Flexible** (inclusivo a favor del Arrendatario).
    - Si la solicitud se procesa exactamente en el límite de las 24 horas y 0 segundos (`anticipacion == 24h:00m:00s`), el sistema clasifica la cancelación como **Moderado** (inclusivo a favor del Arrendatario).
    - Para tiempos estrictamente inferiores a 24 horas (`anticipacion < 24h`), se clasifica como **Tardío**.
- **Inconsistencia o ausencia de hora pactada de inicio**:
    - Si el sistema no puede determinar la hora pactada de inicio de la reserva por datos corruptos o incompletos, DEBE rechazar el flujo con un error descriptivo en lugar de asumir una clasificación por defecto.
- **Huso horario del puerto de atraque**:
    - La anticipación temporal DEBE computarse utilizando la fecha y hora oficial del puerto donde está atracada la embarcación, obtenida mediante la invocación a `Proveer información de embarcación` (`CU-09`, `<<include>>`) (nunca la hora del dispositivo del usuario ni la del servidor central sin ajuste de huso).
- **Indisponibilidad o timeout de `Proveer información de embarcación`**:
    - Si el sistema no puede obtener el puerto de atraque y su zona horaria al momento de procesar la cancelación, el sistema NO DEBE calcular la anticipación con una zona horaria asumida por defecto. DEBE detener temporalmente la solicitud y retornar un error de servicio no disponible para reintento.
- **Inadmisibilidad en reservas "Pendiente de Pago"**:
    - El estado "Pendiente de Pago" no es cancelable voluntariamente por ningún actor. La salida pasiva consiste en no realizar el pago y permitir que el temporizador TTL expire automáticamente la reserva y libere el activo en Módulo 1.
- **Diferenciación estricta entre Cancelación Tardía (<24h) y No-Show**:
    - Ambas figuras conllevan una anticipación inferior a 24 horas respecto al zarpe, pero el "No-Show" es un caso de uso independiente (`Marcar inasistencia`) administrado exclusivamente por el Propietario tras cumplirse la ventana de tolerancia de 30 minutos en el muelle. Este caso de uso solo gestiona cancelaciones voluntarias explícitas previas al zarpe.
- **Condición de carrera entre Arrendatario y Propietario (Doble cancelación simultánea)**:
    - Si ambos actores envían la cancelación de la misma reserva exactamente en el mismo instante, el control de concurrencia de `Actualizar estado reserva` asegura que la primera transacción confirmada asiente el sub-estado correspondiente. La segunda solicitud es rechazada al encontrar la reserva en estado terminal `Cancelada`.
- **Cancelación concurrente con el inicio de navegación**:
    - Si el Arrendatario solicita cancelar mientras el Propietario registra el inicio de la navegación en el muelle, prevalece la primera transacción confirmada. Si se confirma primero el inicio de navegación, la cancelación es rechazada indicando que el viaje ya comenzó.
- **Prohibición absoluta de cálculo financiero en Módulo 2**:
    - Módulo 2 **no calcula importes a devolver, comisiones de retención ni montos de penalidad monetaria**. Módulo 2 calcula la anticipación temporal, asigna el sub-estado (`Flexible`, `Moderado`, `Tardío`, `Por Propietario` o `Por Inasistencia`), asienta la transición de estado y delega en Módulo 3 toda la dispersión financiera de fondos.

---

## Requirements

### Functional Requirements

- **FR-001**: El sistema DEBE activarse como una extensión (`<<extend>>`) desde el caso base `Ver detalle de reserva` cuando el usuario desea ejercer una cancelación.
- **FR-002**: El sistema DEBE permitir solicitar la cancelación de una reserva si y solo si la reserva existe y se encuentra en estado principal `Reservada` (previo al check-in o inicio formal de la navegación).
- **FR-003**: El sistema DEBE verificar y validar que el usuario solicitante sea unívocamente el Arrendatario titular o el Propietario registrado de la embarcación asociada a la reserva. Si el usuario no está legitimado o es un tercero no autorizado, el sistema DEBE rechazar la operación de manera estricta.
- **FR-004**: El sistema DEBE excluir explícitamente y denegar por completo las solicitudes de cancelación activa sobre reservas que se encuentren en estado `Iniciada`, `Pendiente de Pago`, `Pago Fallido`, `En Navegación`, `Completada`, `Expirada` o previamente `Cancelada`. En los casos de `Iniciada` y `Pendiente de Pago`, el sistema debe rechazar la cancelación indicando que no admite cancelación activa y debe esperarse la expiración natural del TTL.
- **FR-005**: El sistema DEBE invocar a `Proveer información de embarcación` (`CU-09`, `<<include>>`) para obtener el puerto de atraque de la embarcación y determinar la zona horaria oficial del activo. Si la consulta a Módulo 1 no responde o falla, el sistema NO DEBE asumir una zona horaria por defecto y DEBE detener el flujo con error descriptivo.
- **FR-006**: Si el solicitante es el Arrendatario, el sistema DEBE calcular el tiempo de anticipación exacto (en horas y minutos) como la diferencia entre la fecha/hora de la solicitud de cancelación y la fecha/hora pactada de inicio de la reserva, bajo la zona horaria oficial del puerto de atraque.
- **FR-007**: Si el solicitante es el Arrendatario, el sistema DEBE clasificar automáticamente la cancelación aplicando las siguientes reglas de negocio temporales:
    - **Flexible**: Cuando la anticipación es mayor o igual a 72 horas (`anticipacion >= 72 horas`).
    - **Moderado**: Cuando la anticipación es mayor o igual a 24 horas y estrictamente menor a 72 horas (`24 horas <= anticipacion < 72 horas`).
    - **Tardío**: Cuando la anticipación es estrictamente menor a 24 horas (`anticipacion < 24 horas`).
- **FR-008**: Está permitido cancelar hasta el último minuto antes de la hora exacta de zarpe, aplicando la penalización correspondiente según la franja `Tardío` (clasificación que corresponde a anticipación inferior a 24 horas). La cancelación con anticipación inferior a 24 horas respecto al zarpe se procesa como `Tardío` con su penalización, sin que ello implique bloqueo de la opción de cancelación hasta el instante previo al zarpe.
- **FR-009**: Si el solicitante es el Propietario, el sistema DEBE asignar directamente la clasificación **Por Propietario**, sin evaluar el tiempo de anticipación ni aplicar franjas horarias.
- **FR-010**: Si la cancelación es solicitada por el Propietario, el sistema DEBE permitir registrar el motivo o justificación de la cancelación, identificando si la causa se debe a fuerza mayor logística o a una avería/desperfecto mecánico que requiera inhabilitar la embarcación.
- **FR-011**: Una vez determinada la clasificación contractual (`Flexible`, `Moderado`, `Tardío` o `Por Propietario`), el sistema DEBE invocar obligatoriamente el caso de uso interno subordinado `Actualizar estado reserva` mediante una relación `(<<include>>)`.
- **FR-012**: El sistema DEBE delegar en `Actualizar estado reserva` la notificación hacia la API externa `Asignar estado operativo` de Módulo 1 para actualizar el activo náutico:
    - Asignar `Disponible` si la cancelación fue realizada por el Arrendatario o por el Propietario sin reporte de avería (fuerza mayor logística).
    - Asignar `En Mantenimiento/Limpieza` si el Propietario canceló reportando daño mecánico o necesidad de reparación.
- **FR-013**: El sistema DEBE delegar en `Actualizar estado reserva` la notificación hacia la API externa `Recibir estado de reserva` de Módulo 3 enviando el estado principal `Cancelada`, el sub-estado contractual determinado (`Flexible`, `Moderado`, `Tardío`, `Por Propietario` o `Por Inasistencia`), el actor solicitante y la marca de tiempo, para que Módulo 3 gestione la liquidación y dispersión financiera de fondos.
- **FR-014**: **REGLA DE NEGOCIO ESTRICTA (Sin cálculo financiero):** El sistema **NO DEBE en ningún caso calcular montos de devolución, deducciones de comisiones, penalidades monetarias ni ejecutar transferencias de dinero**. Módulo 2 actúa como orquestador de tiempos, reglas de anticipación y estados; la valoración económica y dispersión de dinero pertenece exclusivamente a Módulo 3.
- **FR-015**: El sistema DEBE guardar un registro claro y auditable de la cancelación (`CancellationEvent`), capturando: identificador del evento, identificador de la reserva, actor solicitante, fecha y hora de la solicitud, anticipación temporal calculada, sub-estado contractual resultante y la justificación/motivo si aplica.
- **FR-016**: El sistema DEBE desplegar una ventana emergente de confirmación para el Arrendatario que incluya los siguientes componentes visuales:
    - Título "Cancelar Reserva" seguido del identificador (ej. "#RS-4492") y un subtítulo de contexto con el nombre del barco, fechas y nombre del propietario.
    - Un bloque de desglose temporal que muestre explícitamente el "Zarpe pactado", la "Solicitud de cancelación" (ambos con fecha, hora y zona horaria del puerto) y el resultado de la "Anticipación calculada" en horas.
    - Un bloque titulado "ESTADO DE LA POLÍTICA" que exhiba la clasificación asignada (ej. "MODERADA") mediante una etiqueta destacada y un texto confirmatorio.
    - Un texto aclaratorio inferior indicando: "El monto de retención lo calcula el sistema de pagos.".
    - Dos botones de acción en la parte inferior: "MANTENER RESERVA" (secundario)  la cual cierra la ventana sin alterar el estado de la reserva ni registrar eventos y "CONFIRMAR CANCELACIÓN" (primario).
- **FR-017**: El sistema DEBE desplegar una ventana emergente de cancelación para el Propietario que estructure la captura del motivo de la siguiente manera:
    - Título "Cancelar Reserva" seguido del identificador y una etiqueta roja destacada con el texto "CANCELACIÓN POR PROPIETARIO".
    - Una sección obligatoria "Motivo de la cancelación" compuesta por dos tarjetas de selección excluyentes: "Fuerza mayor logística" (incluyendo la etiqueta verde indicadora "La embarcación quedará Disponible") y "Averia o desperfecto mecanico" (incluyendo la etiqueta roja indicadora "La embarcación pasará a En mantenimiento/ Limpieza").
    - Un área de texto libre titulada "Justificación" con el placeholder "Describa brevemente lo ocurrido (quedará registrado en el evento cancelación)".
    - Dos botones de acción en la base: "MANTENER RESERVA"  la cual cierra la ventana sin alterar el estado de la reserva ni registrar eventos y "CONFIRMAR CANCELACIÓN".
---

## Key Entities

- **Reserva (`Reservation`)**: Entidad principal de Módulo 2 que cambia de estado de `Reservada` a `Cancelada`, adoptando el sub-estado clasificado (`Flexible`, `Moderado`, `Tardío`, `Por Propietario` o `Por Inasistencia`).
- **Evento de Cancelación (`CancellationEvent`)**: Registro auditable de dominio en Módulo 2. Atributos clave: identificador, referencia a reserva, actor solicitante, timestamp oficial, anticipación calculada, sub-estado contractual resultante y justificación.
- **Clasificación de Cancelación**: Categoría de dominio resultante del cálculo de anticipación o rol del actor, persistida como sub-estado de la reserva y comunicada a Módulo 3.
- **Embarcación** *(entidad externa, propiedad de Módulo 1)*: Referenciada por identificador; su puerto de atraque (y zona horaria) se consulta vía `Proveer información de embarcación` (`CU-09`, `<<include>>`) y su estado operativo se actualiza a `Disponible` o `En Mantenimiento/Limpieza` vía `Actualizar estado reserva` (`CU-08`).

---

## Success Criteria

### Measurable Outcomes

- **SC-001**: Cero (0%) cancelaciones permitidas sobre reservas en estado no cancelable (`Iniciada`, `Pendiente de Pago`, `Pago Fallido`, `En Navegación`, `Completada`, `Expirada` o `Cancelada`).
- **SC-002**: El 100% de las solicitudes válidas de cancelación del Arrendatario reciben la clasificación correcta según la franja horaria correspondiente (`Flexible` ≥ 72h, `Moderado` 24h a 72h, `Tardío` < 24h).
- **SC-003**: El 100% de las solicitudes de cancelación del Propietario reciben la clasificación `Por Propietario`, independientemente del tiempo restante para el zarpe.
- **SC-004**: Cero (0%) clasificaciones emitidas cuando alguna entrada obligatoria (hora pactada de zarpe, zona horaria o identidad del solicitante) esté ausente o sea inválida.
- **SC-005**: El 100% de las cancelaciones confirmadas invocan secuencialmente a `Actualizar estado reserva` y emiten el primer intento de notificación para la actualización de la embarcación en Módulo 1 (`Asignar estado operativo`) en menos de 1 segundo tras procesar la solicitud.
- **SC-006**: El 100% de los eventos de cancelación y sus sub-estados son notificados y confirmados por la API de Módulo 3 (`Recibir estado de reserva`) para la liquidación de fondos (0% de eventos perdidos silenciosamente).
- **SC-007**: Cero (0) cálculos de reembolsos, comisiones o penalidades monetarias realizados dentro de Módulo 2.
- **SC-008**: Cero (0%) cancelaciones autorizadas a usuarios que no sean el Arrendatario titular o el Propietario registrado de la reserva.
