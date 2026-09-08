# Feature Specification: Solicitar Cancelación

**Módulo**: Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones  
**Fecha de Creación**: 2026-09-06  
**Actores Primarios**: Arrendatario y Propietario (ambos interactúan directamente con este caso de uso en la plataforma)  
**Dependencias Externas (APIs)**:
- **Módulo 1 – Gestión de Flota y Activos P2P**: API externa `Asignar estado operativo` (notificación para liberar la embarcación a estado `Disponible` o su pase a `En Mantenimiento/Limpieza` si se reporta avería, orquestada de forma indirecta a través del caso de uso `Actualizar estado reserva`).
- **Módulo 3 – Liquidación, Seguros y Dispersión de Fondos**:
    - Consumo indirecto a través del caso de uso subordinado `Solicitar tipo de cancelación` para consultar a Módulo 3 la clasificación contractual de la cancelación (`Flexible`, `Moderado`, `Tardío`, `Por Anfitrión`).
    - API externa `Recibir estado de reserva` (notificación del estado `Cancelado` junto con su sub-estado para que Módulo 3 ejecute la liquidación de reembolsos y penalidades, orquestada vía `Actualizar estado reserva`).
- **Casos de uso internos de Módulo 2**:
    - `Solicitar tipo de cancelación` `(<<include>>)`: Para delegar a Módulo 3 la determinación de la categoría o tipo de cancelación.
    - `Actualizar estado reserva` `(<<include>>)`: Para ejecutar la transición formal al estado principal terminal `Cancelado` con el sub-estado correspondiente.

---

## User Scenarios & Testing

### User Story 1 - El Arrendatario solicita cancelar una reserva confirmada antes del inicio del servicio (Priority: P1)

Como Arrendatario, quiero solicitar la cancelación voluntaria de mi reserva previamente confirmada para desistir del viaje y recibir el reembolso correspondiente de acuerdo con la anticipación de mi solicitud.

***Why this priority***: Constituye el mecanismo contractual de salida unilateral para el cliente, activando de forma controlada la política de cancelaciones de la plataforma y devolviendo la disponibilidad del activo al inventario.

***Independent Test***: Se prueba aislando reservas en estado "Confirmada", solicitando la cancelación en los tres umbrales temporales del proyecto (>72h, entre 72h y 24h, y <24h antes del zarpe). Se verifica la invocación secuencial a `(<<include>>)` "Solicitar tipo de cancelación" y `(<<include>>)` "Actualizar estado reserva", la asignación del sub-estado correspondiente, la liberación del activo en Módulo 1 y la notificación a Módulo 3 sin que Módulo 2 realice cálculos monetarios.

***Acceptance Scenarios***:

1. **Scenario**: Cancelación voluntaria con más de 72 horas de anticipación (Flexible)
    - **Given** una reserva en estado principal "Confirmada" cuya hora pactada de inicio es en 96 horas
    - **When** el Arrendatario titular solicita cancelarla
    - **Then** el sistema invoca la relación `(<<include>>)` con "Solicitar tipo de cancelación", recibe la clasificación "Flexible" desde Módulo 3, invoca la relación `(<<include>>)` con "Actualizar estado reserva" fijando el estado "Cancelado" con el sub-estado "Flexible", notifica a Módulo 1 para liberar la embarcación a estado "Disponible" y notifica a Módulo 3 para tramitar el reembolso total al cliente

2. **Scenario**: Cancelación voluntaria entre 72 y 24 horas de anticipación (Moderado)
    - **Given** una reserva en estado principal "Confirmada" cuya hora pactada de inicio es en 40 horas
    - **When** el Arrendatario titular solicita cancelarla
    - **Then** el sistema invoca la relación `(<<include>>)` con "Solicitar tipo de cancelación", recibe la clasificación "Moderado" desde Módulo 3, invoca la relación `(<<include>>)` con "Actualizar estado reserva" fijando el estado "Cancelado" con el sub-estado "Moderado" y notifica a Módulo 3 para aplicar la retención del 50% según la política financiera

3. **Scenario**: Cancelación voluntaria con menos de 24 horas de anticipación (Tardío)
    - **Given** una reserva en estado principal "Confirmada" cuya hora pactada de inicio es en 6 horas
    - **When** el Arrendatario titular solicita cancelarla
    - **Then** el sistema invoca la relación `(<<include>>)` con "Solicitar tipo de cancelación", recibe la clasificación "Tardío" desde Módulo 3, invoca la relación `(<<include>>)` con "Actualizar estado reserva" fijando el estado "Cancelado" con el sub-estado "Tardío" y notifica a Módulo 3 para que liquide la compensación completa al anfitrión

---

### User Story 2 - El Propietario solicita cancelar una reserva confirmada antes del inicio del servicio (Priority: P1)

Como Propietario de la embarcación, quiero cancelar una reserva confirmada antes del zarpe por inconvenientes de fuerza mayor o averías mecánicas, para liberar el compromiso contractual y permitir que el sistema reembolse al cliente.

***Why this priority***: Es la contraparte de protección al consumidor que garantiza que, ante incumplimiento o indisponibilidad del anfitrión, la reserva quede formalmente cerrada y el Arrendatario reciba la devolución oportuna de su dinero gestionada por Módulo 3.

***Independent Test***: Se prueba ejecutando la cancelación por parte del Propietario sobre una reserva confirmada antes del check-in, validando que el flujo invoque secuencialmente a `(<<include>>)` "Solicitar tipo de cancelación" y `(<<include>>)` "Actualizar estado reserva", asigne el sub-estado "Por Anfitrión", actualice el activo en Módulo 1 de acuerdo al motivo e instruya la liquidación a Módulo 3.

***Acceptance Scenarios***:

1. **Scenario**: Cancelación por Propietario por indisponibilidad logística (Embarcación liberada)
    - **Given** una reserva en estado principal "Confirmada" previa al zarpe
    - **When** el Propietario registrado solicita la cancelación indicando motivos de fuerza mayor logística
    - **Then** el sistema tramita la clasificación "Por Anfitrión" vía `(<<include>>)` "Solicitar tipo de cancelación", invoca la relación `(<<include>>)` con "Actualizar estado reserva" asentando "Cancelado" con el sub-estado "Por Anfitrión", actualiza la embarcación a "Disponible" en Módulo 1 y notifica a Módulo 3 para el reembolso integral al turista

2. **Scenario**: Cancelación por Propietario por desperfecto mecánico (Embarcación a mantenimiento)
    - **Given** una reserva en estado principal "Confirmada" previa al zarpe
    - **When** el Propietario solicita la cancelación notificando una avería mecánica en el motor
    - **Then** el sistema tramita el sub-estado "Por Anfitrión", invoca la relación `(<<include>>)` con "Actualizar estado reserva" y notifica a Módulo 1 para actualizar la embarcación a estado operativo "En Mantenimiento/Limpieza", bloqueando su oferta comercial

---

### User Story 3 - Rechazar cancelaciones sobre reservas no cancelables o por actores no legitimados (Priority: P1)

Como sistema, quiero denegar las solicitudes de cancelación sobre reservas en estados no permitidos o iniciadas por terceros no autorizados, para mantener la integridad de las reservas y evitar cancelaciones indebidas.

***Why this priority***: Preserva la consistencia de la máquina de estados, evita cancelaciones arbitrarias durante la navegación y ratifica que las reservas pendientes de pago no se cancelan activamente sino por expiración pasiva de su temporizador TTL.

***Independent Test***: Se prueba emitiendo solicitudes de cancelación sobre reservas en estados "Pendiente de Pago", "En Navegación" y "Completado", y desde cuentas de terceros no vinculadas a la reserva, verificando el rechazo estricto en el 100% de los intentos.

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
    - **When** un usuario que no es ni el Arrendatario ni el Propietario solicita la cancelación
    - **Then** el sistema deniega la operación por falta de autorización

---

### Edge Cases

- **Inadmisibilidad en reservas "Pendiente de Pago"**: Como regla de arquitectura consolidada, el estado "Pendiente de Pago" no es cancelable voluntariamente. La salida pasiva consiste en no realizar el pago y permitir que el temporizador TTL de 15 minutos expire automáticamente la reserva y libere el activo.
- **Diferenciación estricta entre Cancelación Tardía (<24h) y No-Show**: Ambas figuras conllevan una anticipación inferior a 24 horas, pero el "No-Show" es un caso de uso independiente (`Marcar inasistencia`) administrado exclusivamente por el Propietario tras 30 minutos de tolerancia en el muelle. Este caso de uso solo gestiona cancelaciones voluntarias explícitas.
- **Condición de carrera entre Arrendatario y Propietario (Doble cancelación simultánea)**: Si ambos actores envían la cancelación de la misma reserva exactamente en el mismo instante, el control de concurrencia de `Actualizar estado reserva` asegura que la primera solicitud procesada guarde el sub-estado. La segunda solicitud es rechazada al encontrar la reserva en estado terminal `Cancelado`.
- **Cancelación concurrente con el inicio de navegación**: Si el Arrendatario solicita cancelar mientras el Propietario registra el inicio de la navegación en el muelle, prevalece la primera transacción confirmada. Si se registra primero el inicio de navegación, la cancelación es rechazada indicando que el viaje ya comenzó.
- **Falla o interrupción en la API de Módulo 3 durante "Solicitar tipo de cancelación"**: Si Módulo 3 no responde a la solicitud de tipificación, el sistema no asienta la cancelación de forma incompleta; detiene el flujo, mantiene la reserva en estado "Confirmada" y notifica al usuario el inconveniente temporal para su reintento.
- **Prohibición absoluta de cálculo financiero en Módulo 2**: Módulo 2 **no calcula importes a devolver, comisiones de retención ni montos de penalidad monetaria**. Módulo 2 solo registra la anticipación temporal, consulta la categoría a Módulo 3, asienta el nuevo estado y delega en Módulo 3 toda la dispersión financiera de fondos.

---

## Requirements

### Functional Requirements

- **FR-001**: El sistema DEBE permitir solicitar la cancelación de una reserva si y solo si la reserva existe y se encuentra en estado principal "Confirmada" (previo al check-in o inicio de la navegación).
- **FR-002**: El sistema DEBE verificar y validar que el usuario solicitante sea unívocamente el Arrendatario titular o el Propietario registrado de la embarcación asociada a la reserva.
- **FR-003**: El sistema DEBE excluir explícitamente de la opción de cancelación a las reservas que se encuentren en estado `Pendiente de Pago`, `En Navegación`, `Completado`, `Expirado` o previamente `Cancelado`.
- **FR-004**: El sistema DEBE calcular las horas y minutos exactos de anticipación entre la fecha/hora de la solicitud de cancelación y la fecha/hora pactada de inicio de la reserva, tomando como base la zona horaria oficial del puerto de atraque de la embarcación.
- **FR-005**: El sistema DEBE invocar obligatoriamente el caso de uso subordinado `Solicitar tipo de cancelación` mediante una relación `(<<include>>)`, enviando el identificador de la reserva, el actor solicitante (Arrendatario o Propietario) y la anticipación calculada.
- **FR-006**: El sistema DEBE registrar la clasificación contractual de la cancelación retornada por Módulo 3 a través de `Solicitar tipo de cancelación`:
    - 🔶 [PENDIENTE DE CONFIRMAR — Sub-estados de Cancelación]: `Flexible`, `Moderado`, `Tardío` o `Por Anfitrión` [FIN PENDIENTE].
- **FR-007**: Si la cancelación es solicitada por el Propietario, el sistema DEBE permitir registrar el motivo o justificación de la cancelación, identificando si la causa se debe a una avería o mantenimiento que requiera inhabilitar la embarcación.
- **FR-008**: Al recibir la clasificación, el sistema DEBE invocar obligatoriamente el caso de uso subordinado `Actualizar estado reserva` mediante una relación `(<<include>>)`, solicitando la transición al estado principal terminal 🔶 [PENDIENTE DE CONFIRMAR — Estados Principales de la Reserva] `Cancelado` [FIN PENDIENTE], asociando el sub-estado obtenido de Módulo 3.
- **FR-009**: El sistema DEBE delegar en `Actualizar estado reserva` la notificación hacia la API externa `Asignar estado operativo` de Módulo 1 para actualizar el activo:
    - Asignar `Disponible` si la cancelación fue realizada por el Arrendatario o por el Propietario sin reporte de avería.
    - Asignar `En Mantenimiento/Limpieza` si el Propietario canceló reportando daño mecánico o necesidad de reparación.
- **FR-010**: El sistema DEBE delegar en `Actualizar estado reserva` la notificación hacia la API externa `Recibir estado de reserva` de Módulo 3 enviando el estado `Cancelado`, el sub-estado correspondiente y la marca de tiempo para que Módulo 3 gestione la liquidación y compensación de fondos.
- **FR-011**: **REGLA DE NEGOCIO ESTRICTA (Sin cálculo financiero):** El sistema **NO DEBE en ningún caso calcular montos de devolución, deducciones de comisiones, penalidades monetarias ni ejecutar transferencias de dinero**. Módulo 2 actúa como orquestador de tiempos y estados; la valoración económica y dispersión de dinero pertenece exclusivamente a Módulo 3.
- **FR-012**: El sistema DEBE guardar un registro claro y auditable de la cancelación, capturando: identificador de la reserva, actor solicitante, fecha y hora de la solicitud, anticipación temporal calculada, sub-estado contractual resultante y la justificación si aplica.

---

### Key Entities

- **Reserva (`Reservation`)**: Entidad de Módulo 2 que cambia de estado principal de "Confirmada" a "Cancelado", adoptando el sub-estado asignado por Módulo 3.
- **Evento de Cancelación (`CancellationEvent`)**: Registro auditable de dominio. Atributos clave: identificador del evento, identificador de la reserva, actor solicitante (Arrendatario / Propietario), marca de tiempo de la solicitud, horas de anticipación respecto al zarpe, clasificación contractual devuelta por Módulo 3 y motivo/justificación del anfitrión.
- **Embarcación**: Activo náutico administrado por Módulo 1, cuyo estado operativo se actualiza a `Disponible` o `En Mantenimiento/Limpieza` tras la cancelación.

---

## Success Criteria

### Measurable Outcomes

- **SC-001**: Cero (0%) cancelaciones permitidas sobre reservas en estado `Pendiente de Pago`, `En Navegación`, `Completado` o `Expirado`.
- **SC-002**: El 100% de las solicitudes válidas de cancelación ejecutan secuencialmente la llamada a `Solicitar tipo de cancelación` y posteriormente a `Actualizar estado reserva`.
- **SC-003**: El 100% de las cancelaciones confirmadas notifican la liberación o pase a mantenimiento de la embarcación en Módulo 1 (`Asignar estado operativo`) en menos de 1 segundo tras procesar la solicitud.
- **SC-004**: El 100% de los eventos de cancelación y sus sub-estados son notificados y confirmados por la API de Módulo 3 para la liquidación de fondos (0% de eventos perdidos silenciosamente).
- **SC-005**: Cero (0) cálculos de reembolsos, comisiones o penalidades monetarias realizados dentro de Módulo 2.
- **SC-006**: Cero (0%) cancelaciones autorizadas a usuarios que no sean el arrendatario titular o el propietario registrado de la reserva.