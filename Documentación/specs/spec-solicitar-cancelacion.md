# Feature Specification: Solicitar Cancelación

**Módulo**: Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones  
**Fecha de Creación**: 2026-09-06 (Unificado y consolidado: 2026-09-08)  
**Actores Primarios**: Arrendatario y Propietario (ambos interactúan directamente con este caso de uso en la plataforma)  
**Dependencias Externas (APIs)**:
- **Módulo 1 – Gestión de Flota y Activos P2P**:
    - API externa `Consultar información embarcación` (para obtener el puerto de atraque de la embarcación y determinar la zona horaria oficial aplicable al cálculo exacto de la anticipación temporal — ver FR-004).
    - API externa `Asignar estado operativo` (notificación para liberar la embarcación a estado `Disponible` o su pase a `En Mantenimiento/Limpieza` si se reporta avería, orquestada de forma indirecta a través del caso de uso `Actualizar estado reserva`).
- **Módulo 3 – Liquidación, Seguros y Dispersión de Fondos**:
    - API externa `Recibir estado de reserva` (notificación del estado principal `Cancelado` junto con el sub-estado contractual determinado para que Módulo 3 ejecute la liquidación de reembolsos y penalidades, orquestada vía `Actualizar estado reserva`).
- **Casos de uso internos de Módulo 2**:
    - `Actualizar estado reserva` `(<<include>>)`: Para ejecutar la transición formal al estado principal terminal `Cancelado` con el sub-estado clasificado (`Flexible`, `Moderado`, `Tardío` o `Por Anfitrión`).

---

## User Scenarios & Testing

### User Story 1 - El Arrendatario solicita cancelar una reserva confirmada antes del inicio del servicio (Priority: P1)

Como Arrendatario, quiero solicitar la cancelación voluntaria de mi reserva previamente confirmada para desistir del viaje y que el sistema determine la clasificación de mi cancelación según el tiempo de anticipación respecto al zarpe, permitiendo a Módulo 3 tramitar el reembolso correspondiente.

***Why this priority***: Constituye el mecanismo contractual de salida unilateral para el cliente, activando de forma controlada la política de cancelaciones de la plataforma, clasificando la penalidad/reembolso según los tiempos pactados y devolviendo la disponibilidad del activo al inventario náutico.

***Independent Test***: Se prueba aislando reservas en estado "Confirmada", solicitando la cancelación en los tres umbrales temporales del proyecto (>72h, entre 72h y 24h, y <24h antes del zarpe, evaluados en la zona horaria del puerto de atraque). Se verifica el cálculo algorítmico interno de anticipación, la asignación directa del sub-estado correspondiente ("Flexible", "Moderado" o "Tardío"), la invocación a `(<<include>>)` "Actualizar estado reserva", la liberación del activo en Módulo 1 y la notificación a Módulo 3 sin que Módulo 2 realice cálculos monetarios.

***Acceptance Scenarios***:

1. **Scenario**: Cancelación voluntaria con más de 72 horas de anticipación (Flexible)
    - **Given** una reserva en estado principal "Confirmada" cuya hora pactada de inicio es en más de 72 horas (por ejemplo, 96 horas) en la zona horaria del puerto de atraque
    - **When** el Arrendatario titular solicita cancelarla
    - **Then** el sistema calcula la anticipación temporal (>72h), determina internamente la clasificación "Flexible", invoca la relación `(<<include>>)` con "Actualizar estado reserva" fijando el estado "Cancelado" con el sub-estado "Flexible", notifica a Módulo 1 para liberar la embarcación a estado "Disponible" y notifica a Módulo 3 para tramitar el reembolso total al cliente según su matriz de liquidación

2. **Scenario**: Cancelación voluntaria entre 72 y 24 horas de anticipación (Moderado)
    - **Given** una reserva en estado principal "Confirmada" cuya hora pactada de inicio es entre 24 y 72 horas (por ejemplo, 40 horas) en la zona horaria del puerto de atraque
    - **When** el Arrendatario titular solicita cancelarla
    - **Then** el sistema calcula la anticipación temporal (entre 24h y 72h), determina internamente la clasificación "Moderado", invoca la relación `(<<include>>)` con "Actualizar estado reserva" fijando el estado "Cancelado" con el sub-estado "Moderado" y notifica a Módulo 3 para que aplique la retención del 50% según la política financiera

3. **Scenario**: Cancelación voluntaria con menos de 24 horas de anticipación (Tardío)
    - **Given** una reserva en estado principal "Confirmada" cuya hora pactada de inicio es en menos de 24 horas (por ejemplo, 6 horas) en la zona horaria del puerto de atraque
    - **When** el Arrendatario titular solicita cancelarla
    - **Then** el sistema calcula la anticipación temporal (<24h), determina internamente la clasificación "Tardío", invoca la relación `(<<include>>)` con "Actualizar estado reserva" fijando el estado "Cancelado" con el sub-estado "Tardío" y notifica a Módulo 3 para que liquide la compensación completa al anfitrión

---

### User Story 2 - El Propietario solicita cancelar una reserva confirmada antes del inicio del servicio (Priority: P1)

Como Propietario de la embarcación, quiero cancelar una reserva confirmada antes del zarpe por inconvenientes de fuerza mayor logística o averías mecánicas, para liberar el compromiso contractual, registrar la causa y permitir que el sistema asigne la clasificación "Por Anfitrión" para reembolsar al cliente.

***Why this priority***: Es la contraparte de protección al consumidor que garantiza que, ante incumplimiento o indisponibilidad del anfitrión, la reserva quede formalmente cerrada con clasificación directa "Por Anfitrión" (sin aplicar franjas horarias de anticipación), el activo náutico se actualice adecuadamente y el Arrendatario reciba la devolución íntegra de su dinero gestionada por Módulo 3.

***Independent Test***: Se prueba ejecutando la cancelación por parte del Propietario sobre una reserva confirmada antes del check-in, independientemente del tiempo restante para el zarpe. Se valida que el sistema asigne directamente el sub-estado "Por Anfitrión" sin evaluar umbrales horarios, registre el motivo, invoque a `(<<include>>)` "Actualizar estado reserva", actualice el activo náutico en Módulo 1 de acuerdo a la causa (Disponible o En Mantenimiento) e instruya la liquidación a Módulo 3.

***Acceptance Scenarios***:

1. **Scenario**: Cancelación por Propietario por indisponibilidad logística (Embarcación liberada)
    - **Given** una reserva en estado principal "Confirmada" previa al zarpe
    - **When** el Propietario registrado solicita la cancelación indicando motivos de fuerza mayor logística
    - **Then** el sistema asigna directamente la clasificación "Por Anfitrión" sin evaluar la anticipación horaria, invoca la relación `(<<include>>)` con "Actualizar estado reserva" asentando "Cancelado" con sub-estado "Por Anfitrión", actualiza la embarcación a "Disponible" en Módulo 1 y notifica a Módulo 3 para el reembolso integral al turista

2. **Scenario**: Cancelación por Propietario por desperfecto mecánico (Embarcación a mantenimiento)
    - **Given** una reserva en estado principal "Confirmada" previa al zarpe
    - **When** el Propietario solicita la cancelación notificando una avería mecánica en el motor
    - **Then** el sistema asigna la clasificación "Por Anfitrión", invoca la relación `(<<include>>)` con "Actualizar estado reserva" asentando "Cancelado" con sub-estado "Por Anfitrión", y notifica a Módulo 1 para actualizar la embarcación a estado operativo "En Mantenimiento/Limpieza", bloqueando su oferta comercial

---

### User Story 3 - Rechazar cancelaciones sobre reservas no cancelables o por actores no legitimados (Priority: P1)

Como sistema, quiero denegar las solicitudes de cancelación sobre reservas en estados no permitidos o iniciadas por terceros no autorizados, para mantener la integridad de las reservas y evitar cancelaciones indebidas.

***Why this priority***: Preserva la consistencia de la máquina de estados, evita cancelaciones arbitrarias durante la navegación y ratifica que las reservas pendientes de pago no se cancelan activamente sino por expiración pasiva de su temporizador TTL de 15 minutos.

***Independent Test***: Se prueba emitiendo solicitudes de cancelación sobre reservas en estados "Pendiente de Pago", "En Navegación", "Completado" y "Expirado", así como desde cuentas de terceros no vinculadas a la reserva, verificando el rechazo estricto en el 100% de los intentos sin emitir clasificaciones ni alterar estados.

***Acceptance Scenarios***:

1. **Scenario**: Intento de cancelación sobre reserva en Pendiente de Pago
    - **Given** una reserva en estado principal "Pendiente de Pago" dentro de su temporizador TTL de 15 minutos
    - **When** el usuario intenta solicitar su cancelación
    - **Then** el sistema rechaza la solicitud informando que las reservas en pendiente de pago no admiten cancelación activa y deben esperar la expiración natural de su temporizador si se decide no pagar

2. **Scenario**: Intento de cancelación sobre servicio ya en navegación o completado
    - **Given** una reserva en estado "En Navegación" o "Completado"
    - **When** el Arrendatario o el Propietario intentan solicitar la cancelación
    - **Then** el sistema deniega la acción informando que el servicio ya fue iniciado o finalizado

3. **Scenario**: Intento de cancelación por un tercero no autorizado
    - **Given** una reserva en estado "Confirmada"
    - **When** un usuario que no es ni el Arrendatario titular ni el Propietario registrado solicita la cancelación
    - **Then** el sistema deniega la operación por falta de autorización

---

## Edge Cases

- **Resolución de límites exactos de anticipación (72h o 24h en punto)**:
    - Si la solicitud del Arrendatario se procesa exactamente en el límite de las 72 horas y 0 segundos (`anticipacion == 72h:00m:00s`), el sistema clasifica la cancelación como **Flexible** (inclusivo a favor del Arrendatario).
    - Si la solicitud se procesa exactamente en el límite de las 24 horas y 0 segundos (`anticipacion == 24h:00m:00s`), el sistema clasifica la cancelación como **Moderado** (inclusivo a favor del Arrendatario).
    - Para tiempos estrictamente inferiores a 24 horas (`anticipacion < 24h`), se clasifica como **Tardío**.
- **Inconsistencia o ausencia de hora pactada de inicio**:
    - Si el sistema no puede determinar la hora pactada de inicio de la reserva por datos corruptos o incompletos, DEBE rechazar el flujo con un error descriptivo en lugar de asumir una clasificación por defecto.
- **Huso horario del puerto de atraque**:
    - La anticipación temporal DEBE computarse utilizando la fecha y hora oficial del puerto donde está atracada la embarcación, obtenida mediante la API `Consultar información embarcación` de Módulo 1 (nunca la hora del dispositivo del usuario ni la del servidor central sin ajuste de huso).
- **Indisponibilidad o timeout de `Consultar información embarcación`**:
    - Si el sistema no puede obtener el puerto de atraque y su zona horaria al momento de procesar la cancelación, el sistema NO DEBE calcular la anticipación con una zona horaria asumida por defecto. DEBE detener temporalmente la solicitud y retornar un error de servicio no disponible para reintento.
- **Inadmisibilidad en reservas "Pendiente de Pago"**:
    - El estado "Pendiente de Pago" no es cancelable voluntariamente por ningún actor. La salida pasiva consiste en no realizar el pago y permitir que el temporizador TTL de 15 minutos expire automáticamente la reserva y libere el activo en Módulo 1.
- **Diferenciación estricta entre Cancelación Tardía (<24h) y No-Show**:
    - Ambas figuras conllevan una anticipación inferior a 24 horas respecto al zarpe, pero el "No-Show" es un caso de uso independiente (`Marcar inasistencia`) administrado exclusivamente por el Propietario tras cumplirse la ventana de tolerancia de 30 minutos en el muelle. Este caso de uso solo gestiona cancelaciones voluntarias explícitas previas al zarpe.
- **Condición de carrera entre Arrendatario y Propietario (Doble cancelación simultánea)**:
    - Si ambos actores envían la cancelación de la misma reserva exactamente en el mismo instante, el control de concurrencia de `Actualizar estado reserva` asegura que la primera transacción confirmada asiente el sub-estado correspondiente. La segunda solicitud es rechazada al encontrar la reserva en estado terminal `Cancelado`.
- **Cancelación concurrente con el inicio de navegación**:
    - Si el Arrendatario solicita cancelar mientras el Propietario registra el inicio de la navegación en el muelle, prevalece la primera transacción confirmada. Si se confirma primero el inicio de navegación, la cancelación es rechazada indicando que el viaje ya comenzó.
- **Prohibición absoluta de cálculo financiero en Módulo 2**:
    - Módulo 2 **no calcula importes a devolver, comisiones de retención ni montos de penalidad monetaria**. Módulo 2 calcula la anticipación temporal, asigna el sub-estado (`Flexible`, `Moderado`, `Tardío`, `Por Anfitrión`), asienta la transición de estado y delega en Módulo 3 toda la dispersión financiera de fondos.

---

## Requirements

### Functional Requirements

- **FR-001**: El sistema DEBE permitir solicitar la cancelación de una reserva si y solo si la reserva existe y se encuentra en estado principal `Confirmada` (previo al check-in o inicio formal de la navegación).
- **FR-002**: El sistema DEBE verificar y validar que el usuario solicitante sea unívocamente el Arrendatario titular o el Propietario registrado de la embarcación asociada a la reserva. Si el usuario no está legitimado, DEBE rechazar la solicitud con error de autorización.
- **FR-003**: El sistema DEBE excluir explícitamente de la opción de cancelación activa a las reservas que se encuentren en estado `Pendiente de Pago`, `En Navegación`, `Completado`, `Expirado` o previamente `Cancelado`.
- **FR-004**: El sistema DEBE consultar a la API externa `Consultar información embarcación` de Módulo 1 para obtener el puerto de atraque de la embarcación y determinar la zona horaria oficial del activo. Si la API de Módulo 1 no responde o falla, el sistema NO DEBE asumir una zona horaria por defecto y DEBE detener el flujo con error descriptivo.
- **FR-005**: Si el solicitante es el Arrendatario, el sistema DEBE calcular el tiempo de anticipación exacto (en horas y minutos) como la diferencia entre la fecha/hora de la solicitud de cancelación y la fecha/hora pactada de inicio de la reserva, bajo la zona horaria oficial del puerto de atraque.
- **FR-006**: Si el solicitante es el Arrendatario, el sistema DEBE clasificar automáticamente la cancelación aplicando las siguientes reglas de negocio temporales:
    - **Flexible**: Cuando la anticipación es mayor o igual a 72 horas (`anticipacion >= 72 horas`).
    - **Moderado**: Cuando la anticipación es mayor o igual a 24 horas y estrictamente menor a 72 horas (`24 horas <= anticipacion < 72 horas`).
    - **Tardío**: Cuando la anticipación es estrictamente menor a 24 horas (`anticipacion < 24 horas`).
- **FR-007**: Si el solicitante es el Propietario, el sistema DEBE asignar directamente la clasificación **Por Anfitrión**, sin evaluar el tiempo de anticipación ni aplicar franjas horarias.
- **FR-008**: Si la cancelación es solicitada por el Propietario, el sistema DEBE permitir registrar el motivo o justificación de la cancelación, identificando si la causa se debe a fuerza mayor logística o a una avería/desperfecto mecánico que requiera inhabilitar la embarcación.
- **FR-009**: Una vez determinada la clasificación contractual (`Flexible`, `Moderado`, `Tardío` o `Por Anfitrión`), el sistema DEBE invocar obligatoriamente el caso de uso interno subordinado `Actualizar estado reserva` mediante una relación `(<<include>>)`, solicitando la transición al estado principal terminal `Cancelado` asociando el sub-estado clasificado.
- **FR-010**: El sistema DEBE delegar en `Actualizar estado reserva` la notificación hacia la API externa `Asignar estado operativo` de Módulo 1 para actualizar el activo náutico:
    - Asignar `Disponible` si la cancelación fue realizada por el Arrendatario o por el Propietario sin reporte de avería (fuerza mayor logística).
    - Asignar `En Mantenimiento/Limpieza` si el Propietario canceló reportando daño mecánico o necesidad de reparación.
- **FR-011**: El sistema DEBE delegar en `Actualizar estado reserva` la notificación hacia la API externa `Recibir estado de reserva` de Módulo 3 enviando el estado principal `Cancelado`, el sub-estado contractual determinado (`Flexible`, `Moderado`, `Tardío` o `Por Anfitrión`), el actor solicitante y la marca de tiempo, para que Módulo 3 gestione la liquidación y dispersión financiera de fondos.
- **FR-012**: **REGLA DE NEGOCIO ESTRICTA (Sin cálculo financiero):** El sistema **NO DEBE en ningún caso calcular montos de devolución, deducciones de comisiones, penalidades monetarias ni ejecutar transferencias de dinero**. Módulo 2 actúa como orquestador de tiempos, reglas de anticipación y estados; la valoración económica y dispersión de dinero pertenece exclusivamente a Módulo 3.
- **FR-013**: El sistema DEBE guardar un registro claro y auditable de la cancelación (`CancellationEvent`), capturando: identificador del evento, identificador de la reserva, actor solicitante, fecha y hora de la solicitud, anticipación temporal calculada, sub-estado contractual resultante y la justificación/motivo si aplica.

---

## Key Entities

- **Reserva (`Reservation`)**: Entidad principal de Módulo 2 que cambia de estado de `Confirmada` a `Cancelado`, adoptando el sub-estado clasificado (`Flexible`, `Moderado`, `Tardío` o `Por Anfitrión`).
- **Evento de Cancelación (`CancellationEvent`)**: Registro auditable de dominio en Módulo 2. Atributos clave:
    - `id_evento`: Identificador único de auditoría.
    - `id_reserva`: Referencia a la reserva afectada.
    - `actor_solicitante`: `Arrendatario` o `Propietario`.
    - `fecha_hora_solicitud`: Timestamp oficial en la zona horaria del puerto.
    - `anticipacion_calculada`: Diferencia en horas y minutos respecto al zarpe.
    - `sub_estado_clasificado`: `Flexible`, `Moderado`, `Tardío` o `Por Anfitrión`.
    - `justificacion`: Motivo reportado por el Propietario (si aplica).
- **Clasificación de Cancelación**: Categoría de dominio resultante del cálculo de anticipación o rol del actor, persistida como sub-estado de la reserva y comunicada a Módulo 3.
- **Embarcación** *(entidad externa, propiedad de Módulo 1)*: Referenciada por identificador; su puerto de atraque (y zona horaria) se consulta vía `Consultar información embarcación` y su estado operativo se actualiza a `Disponible` o `En Mantenimiento/Limpieza` vía `Asignar estado operativo`.

---

## Success Criteria

### Measurable Outcomes

- **SC-001**: Cero (0%) cancelaciones permitidas sobre reservas en estado no cancelable (`Pendiente de Pago`, `En Navegación`, `Completado`, `Expirado` o `Cancelado`).
- **SC-002**: El 100% de las solicitudes válidas de cancelación del Arrendatario reciben la clasificación correcta según la franja horaria correspondiente (`Flexible` ≥ 72h, `Moderado` 24h a 72h, `Tardío` < 24h).
- **SC-003**: El 100% de las solicitudes de cancelación del Propietario reciben la clasificación `Por Anfitrión`, independientemente del tiempo restante para el zarpe.
- **SC-004**: Cero (0%) clasificaciones emitidas cuando alguna entrada obligatoria (hora pactada de zarpe, zona horaria o identidad del solicitante) esté ausente o sea inválida.
- **SC-005**: El 100% de las cancelaciones confirmadas invocan secuencialmente a `Actualizar estado reserva` y notifican la actualización de la embarcación en Módulo 1 (`Asignar estado operativo`) en menos de 1 segundo tras procesar la solicitud.
- **SC-006**: El 100% de los eventos de cancelación y sus sub-estados son notificados y confirmados por la API de Módulo 3 (`Recibir estado de reserva`) para la liquidación de fondos (0% de eventos perdidos silenciosamente).
- **SC-007**: Cero (0) cálculos de reembolsos, comisiones o penalidades monetarias realizados dentro de Módulo 2.
- **SC-008**: Cero (0%) cancelaciones autorizadas a usuarios que no sean el Arrendatario titular o el Propietario registrado de la reserva.
