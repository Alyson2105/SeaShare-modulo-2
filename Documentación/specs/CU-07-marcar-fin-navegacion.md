# Feature Specification: Marcar Fin de la Navegación

**Módulo**: Módulo 2 (Operación de Reservas, Tiempos y Cancelaciones)  
**Created**: 2026-09-06  
**Primary Actor**: Propietario (Anfitrión al recibir la embarcación en muelle)  
**External Dependencies (APIs)**:
- **Módulo 1 (Gestión de Flota y Activos P2P)**: API externa `Asignar estado operativo` (cambio del estado del barco a `Disponible` al cerrar la navegación).
- **Módulo 3 (Liquidación, Seguros y Dispersión de Fondos)**: API externa `Recibir estado de reserva` (notificación del estado `Completado` para que Módulo 3 libere el pago al anfitrión y devuelva la garantía, adjuntando el texto de novedades si existe).
- **Casos de uso internos de Módulo 2**: `Actualizar estado reserva` (`<<include>>`).

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - El Propietario registra el fin de la navegación (Priority: P1)

Al terminar el viaje y regresar al puerto, el Propietario revisa la embarcación junto al cliente y registra el fin de la navegación, pudiendo adjuntar un texto opcional de novedades si detectó algún daño o incumplimiento. En ese instante, el sistema verifica que la reserva esté en estado 'En Navegación', cambia la reserva al estado 'Completado' mediante la invocación a 'Actualizar estado reserva' (`<<include>>`), notifica al Módulo 1 para que vuelva a poner el barco como 'Disponible' y le avisa al Módulo 3 para que le entregue el pago al Propietario y le devuelva la garantía al turista.

**Why this priority**: Es el paso clave que le permite al cliente pasar de una reserva temporal a la confirmación de su viaje. Sin esta acción, el proceso se detiene y la reserva termina cancelándose automáticamente.

**Independent Test**: Se prueba con una reserva en estado "En Navegación", registrando el fin de viaje por parte del Propietario (con y sin texto de novedades). Se comprueba que la reserva pasa a "Completado", la embarcación se marca como "Disponible" en Módulo 1 y Módulo 3 recibe la notificación de cierre sin que Módulo 2 realice cálculos monetarios.

**Acceptance Scenarios**:

1. **Scenario**: Registro de entrega del barco
    - **Given** una reserva en estado principal "En Navegación"
    - **When** el Propietario registrado reporta el fin de la navegación
    - **Then** el sistema actualiza la reserva al estado "Completado", guarda la hora real de desembarque y solicita marcar el barco como "Disponible" en Módulo 1 y notificar el cierre a Módulo 3

2. **Scenario**: Regreso del barco después de la hora acordada
    - **Given** una reserva en estado "En Navegación" cuya hora de entrega ya venció
    - **When** el Propietario registra el fin de la navegación
    - **Then** el sistema registra la hora exacta en la que regresó el barco, marca la reserva como "Completado" y le envía al Módulo 3 la hora real de llegada para que este evalúe si aplica algún cobro por tiempo extra

3. **Scenario**: Registro de entrega con texto opcional de novedades
    - **Given** una reserva en estado principal "En Navegación"
    - **When** el Propietario reporta el fin de navegación y adjunta un texto con el detalle de las novedades encontradas (daños, faltantes o incumplimientos)
    - **Then** el sistema actualiza la reserva a "Completado", guarda el texto de la novedad como campo informativo, mantiene el barco como "Disponible" en Módulo 1 y adjunta el texto en la notificación de cierre a Módulo 3, sin que ello modifique el tratamiento del cierre

---

### User Story 2 - Rechazar el fin de navegación en reservas no válidas o por usuarios no autorizados (Priority: P2)

Si alguien intenta registrar el fin de la navegación sobre una reserva que no está en curso (por ejemplo, en estado "Reservada", "Pendiente de Pago", "Cancelado" o ya "Completado"), o si la solicitud la realiza una persona diferente al Propietario del barco, el sistema rechaza la acción inmediatamente.

**Why this priority**: Evita errores en las reservas, cierres de viajes que no han iniciado y que personas ajenas modifiquen la información de los barcos.

**Independent Test**: Se prueba intentando registrar el fin de navegación con un usuario que no sea el Propietario o sobre reservas que no estén "En Navegación", comprobando que el sistema deniega la solicitud sin cambiar ningún dato.

**Acceptance Scenarios**:

1. **Scenario**: Intento de finalizar un viaje que no ha iniciado
    - **Given** una reserva en estado principal "Reservada"
    - **When** el Propietario intenta marcar el fin de la navegación
    - **Then** el sistema rechaza la solicitud e informa que la reserva no ha iniciado su viaje

2. **Scenario**: Intento de registro por un usuario no autorizado
    - **Given** una reserva en estado principal "En Navegación"
    - **When** un usuario que no es el propietario registrado del barco intenta marcar el fin de la navegación
    - **Then** el sistema rechaza la solicitud por falta de permisos

---

### Edge Cases

- **Falta de señal o conexión a internet en el puerto**:
    - Si el Propietario presiona varias veces el botón de finalizar debido a una mala conexión en el muelle, el sistema procesa únicamente la primera solicitud y responde correctamente a los demás intentos sin duplicar registros ni enviar notificaciones repetidas a los Módulos 1 y 3.
- **Regreso antes o después de la hora pactada**:
    - *Regreso anticipado*: Si el cliente devuelve el barco antes de tiempo por decisión propia, el sistema guarda la hora real de llegada y libera el barco; Módulo 2 no devuelve dinero por las horas no usadas.
    - *Regreso con retraso*: Si el barco regresa con demora, Módulo 2 guarda la hora exacta de desembarque; cualquier cobro o penalidad por la demora la calcula exclusivamente el Módulo 3 al comparar la hora pactada contra la hora real de entrega.
- **Imposibilidad de cambiar una reserva ya completada**:
    - Una vez registrada la entrega del barco, la reserva queda cerrada de forma permanente; no se puede reabrir el viaje ni cambiar el reporte de incidentes guardado.
- **Prohibición de calcular costos de reparación o penalidades**:
    - Al adjuntar un texto de novedades, el Módulo 2 **NO calcula el costo de arreglar el barco, no cobra penalidades ni descuenta dinero de la garantía**. El Módulo 2 solo guarda el texto y lo informa a Módulo 3 para que su área financiera gestione los cobros o el seguro.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE permitir registrar el fin de la navegación (entrega del barco) si y solo si la reserva existe y se encuentra en estado principal "En Navegación".
- **FR-002**: El sistema DEBE validar de forma estricta que el usuario que realiza la solicitud sea el Propietario registrado de la embarcación.
- **FR-003**: El sistema DEBE permitir al Propietario adjuntar un texto opcional de novedades al momento de la entrega, con la descripción de los daños, fallas o faltantes encontrados, si los hay.
- **FR-004**: Al confirmar la entrega, el sistema DEBE invocar el caso de uso subordinado "CU-08 Actualizar estado reserva" (`<<include>>`), solicitando cambiar la reserva al estado principal `Completado` y adjuntando el texto de novedades si el Propietario lo proveyó.
- **FR-005**: El sistema DEBE guardar un registro de la entrega, incluyendo: identificador de la reserva, identificador del propietario, fecha y hora real de entrega y el texto de novedades si fue provisto.
- **FR-006**:  El sistema DEBE indicar a "CU-08 Actualizar estado reserva" que llame a la API de Módulo 1 (`Asignar estado operativo`) para actualizar la embarcación a estado `Disponible`.
- **FR-007**:  El sistema DEBE indicar a "CU-08 Actualizar estado reserva" que llame a la API de Módulo 3 (`Recibir estado de reserva`) para notificar el cierre del viaje, de modo que Módulo 3 libere el pago al anfitrión y le devuelva la garantía al cliente, adjuntando el texto de novedades si fue provisto.
- **FR-008**: **REGLA DE NEGOCIO ESTRICTA (Sin dinero):** El sistema **NO DEBE calcular costos de reparación, cobros por demora ni realizar devoluciones o retenciones de dinero**. La evaluación financiera le corresponde exclusivamente al Módulo 3.
- **FR-009**: Si la reserva se encuentra en cualquier estado diferente a "En Navegación", el sistema DEBE rechazar la solicitud e informar que el estado no es compatible.

---

### Key Entities

- **Reserva (`Reservation`)**: Entidad de dominio en Módulo 2 que cambia de estado "En Navegación" al estado "Completado".
- **Registro de Check-out (`CheckOutEvent`)**: Documento que guarda los datos de la entrega. Incluye: identificador del evento, identificador de la reserva, identificador del propietario, fecha/hora real de llegada y texto de novedades en caso de existir.
- **Embarcación**: Activo registrado en el Módulo 1 cuyo estado se actualiza a `Disponible` al cerrar la navegación.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Cero (0%) registros de fin de navegación permitidos sobre reservas que no estén en estado "En Navegación".
- **SC-002**: El 100% de los registros de fin de navegación cambian la reserva a `Completado` y actualizan el barco como `Disponible` en Módulo 1 en menos de 1 segundo.
- **SC-003**: Cero (0) cobros, evaluaciones de daños o cálculo de dinero realizados dentro del Módulo 2.
- **SC-004**: El 100% de las entregas quedan registradas con la hora real de desembarque para auditorías.
- **SC-005**: Cero (0%) registros de entrega autorizados a personas diferentes al propietario del barco.