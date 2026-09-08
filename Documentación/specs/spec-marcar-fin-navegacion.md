# Feature Specification: Marcar Fin de la Navegación

**Módulo**: Módulo 2 (Operación de Reservas, Tiempos y Cancelaciones)  
**Created**: 2026-09-06  
**Primary Actor**: Propietario (Anfitrión al recibir la embarcación en muelle)  
**External Dependencies (APIs)**:
- **Módulo 1 (Gestión de Flota y Activos P2P)**: API externa `Asignar estado operativo` (cambio del estado del barco a `Disponible` si no hubo problemas, o a `En Mantenimiento/Limpieza` si se reportaron averías).
- **Módulo 3 (Liquidación, Seguros y Dispersión de Fondos)**: API externa `Recibir estado de reserva` (notificación del estado `Completado` junto con el sub-estado `Sin incidentes` o `Con incidentes` para que Módulo 3 libere el dinero al anfitrión o retenga el depósito de garantía).
- **Casos de uso internos de Módulo 2**: `Actualizar estado reserva` (`<<include>>`).

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - El Propietario registra la entrega del barco sin novedades (Priority: P1)

Al terminar el viaje y regresar al puerto, el Propietario revisa la embarcación junto al cliente. Si confirma que el barco y todos sus componentes se encuentran en perfecto estado, el Propietario registra el fin de la navegación seleccionando la opción 'Sin incidentes'. En ese instante, el sistema verifica que la reserva esté en estado 'En Navegación', cambia la reserva al estado 'Completado' con el sub-estado 'Sin incidentes' mediante la invocación a 'Actualizar estado reserva' (`<<include>>`), notifica al Módulo 1 para que vuelva a poner el barco como 'Disponible' y le avisa al Módulo 3 para que le entregue el pago al Propietario y le devuelva la garantía al turista.

**Why this priority**: Es el paso clave que le permite al cliente pasar de una reserva temporal a la confirmación de su viaje. Sin esta acción, el proceso se detiene y la reserva termina cancelándose automáticamente.

**Independent Test**: Se prueba con una reserva en estado "En Navegación", registrando el fin de viaje con la opción "Sin incidentes" por parte del Propietario. Se comprueba que la reserva pasa a "Completado" (sub-estado "Sin incidentes"), la embarcación se marca como "Disponible" en Módulo 1 y Módulo 3 recibe la notificación de cierre sin que Módulo 2 realice cálculos monetarios.

**Acceptance Scenarios**:

1. **Scenario**: Registro de entrega exitoso y sin problemas
    - **Given** una reserva en estado principal "En Navegación"
    - **When** el Propietario registrado reporta el fin de la navegación eligiendo la condición "Sin incidentes"
    - **Then** el sistema actualiza la reserva al estado "Completado" con el sub-estado "Sin incidentes", guarda la hora real de desembarque y solicita marcar el barco como "Disponible" en Módulo 1 y notificar el cierre a Módulo 3

2. **Scenario**: Regreso del barco después de la hora acordada
    - **Given** una reserva en estado "En Navegación" cuya hora de entrega ya venció
    - **When** el Propietario registra el fin de la navegación seleccionando la opción "Sin incidentes"
    - **Then** el sistema registra la hora exacta en la que regresó el barco, marca la reserva como "Completado" (sub-estado "Sin incidentes") y le envía al Módulo 3 la hora real de llegada para que este evalúe si aplica algún cobro por tiempo extra

---

### User Story 2 - El Propietario reporta problemas o daños al recibir el barco (Priority: P1)

Si al recibir el barco el Propietario nota daños en el casco, fallas en el motor, falta de equipo de seguridad o cualquier incumplimiento en la entrega, registra el fin de la navegación seleccionando la opción 'Con incidentes' e incluye una explicación de lo sucedido. En ese momento, el sistema cambia la reserva a 'Completado' con el sub-estado 'Con incidentes' mediante la invocación a 'Actualizar estado reserva' (`<<include>>`), le ordena al Módulo 1 poner el barco en 'En Mantenimiento/Limpieza' para que nadie más lo alquile de inmediato, y le avisa al Módulo 3 para que retenga el depósito de garantía e inicie la revisión del reclamo.

**Why this priority**: Protege la propiedad del anfitrión y la seguridad de la flota, evitando que un barco dañado se vuelva a alquilar y asegurando que la garantía no se le devuelva al turista antes de revisar los daños.

**Independent Test**: Se prueba registrando el fin de viaje de una reserva en "En Navegación" con la opción "Con incidentes" y adjuntando una descripción. Se verifica que la reserva pasa a "Completado" (sub-estado "Con incidentes"), Módulo 1 cambia el barco a "En Mantenimiento/Limpieza" y Módulo 3 recibe el aviso para congelar el depósito de garantía, garantizando que Módulo 2 no fije cobros ni montos de reparación.

**Acceptance Scenarios**:

1. **Scenario**: Registro de entrega con reporte de daños
    - **Given** una reserva en estado principal "En Navegación"
    - **When** el Propietario reporta el fin de navegación indicando "Con incidentes" y escribe el detalle de los problemas encontrados
    - **Then** el sistema actualiza la reserva a "Completado" con el sub-estado "Con incidentes", guarda el reporte de la novedad, le indica a Módulo 1 poner el barco en "En Mantenimiento/Limpieza" y le notifica a Módulo 3 la retención de la garantía

---

### User Story 3 - Rechazar el fin de navegación en reservas no válidas o por usuarios no autorizados (Priority: P2)

Si alguien intenta registrar el fin de la navegación sobre una reserva que no está en curso (por ejemplo, en estado "Confirmada", "Pendiente de Pago", "Cancelado" o ya "Completado"), o si la solicitud la realiza una persona diferente al Propietario del barco, el sistema rechaza la acción inmediatamente.

**Why this priority**: Evita errores en las reservas, cierres de viajes que no han iniciado y que personas ajenas modifiquen la información de los barcos.

**Independent Test**: Se prueba intentando registrar el fin de navegación con un usuario que no sea el Propietario o sobre reservas que no estén "En Navegación", comprobando que el sistema deniega la solicitud sin cambiar ningún dato.

**Acceptance Scenarios**:

1. **Scenario**: Intento de finalizar un viaje que no ha iniciado
    - **Given** una reserva en estado principal "Confirmada"
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
    - Al reportar el sub-estado "Con incidentes", el Módulo 2 **NO calcula el costo de arreglar el barco, no cobra penalidades ni descuenta dinero de la garantía**. El Módulo 2 solo guarda el reporte y le avisa al Módulo 3 para que su área financiera gestione los cobros o el seguro.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE permitir registrar el fin de la navegación (entrega del barco) si y solo si la reserva existe y se encuentra en estado principal "En Navegación".
- **FR-002**: El sistema DEBE validar de forma estricta que el usuario que realiza la solicitud sea el Propietario registrado de la embarcación.
- **FR-003**: El sistema DEBE solicitar al Propietario la selección obligatoria de una de las dos opciones de sub-estado al momento de la entrega:
     `Sin incidentes` (entrega normal de la embarcación sin ningún problema o daño).
    `Con incidentes` (presencia de daños, fallas en la embarcación o faltantes de equipo). 
- **FR-004**: Si el Propietario selecciona el sub-estado `Con incidentes`, el sistema DEBE exigir y guardar una descripción textual con los detalles de las novedades o daños encontrados.
- **FR-005**: Al confirmar la entrega, el sistema DEBE invocar el caso de uso subordinado "Actualizar estado reserva" (`<<include>>`), solicitando cambiar la reserva al estado principal  `Completado`  junto con el sub-estado seleccionado (`Sin incidentes` o `Con incidentes`).
- **FR-006**: El sistema DEBE guardar un registro de la entrega, incluyendo: identificador de la reserva, identificador del propietario, fecha y hora real de entrega, sub-estado elegido y las observaciones de incidentes si aplican.
- **FR-007**:  El sistema DEBE indicar a "Actualizar estado reserva" que llame a la API de Módulo 1 (`Asignar estado operativo`) para actualizar la embarcación:
    - Si el sub-estado es `Sin incidentes`: cambiar el barco a estado `Disponible`.
    - Si el sub-estado es `Con incidentes`: cambiar el barco a estado `En Mantenimiento/Limpieza` para inhabilitarlo temporalmente.
- **FR-008**:  El sistema DEBE indicar a "Actualizar estado reserva" que llame a la API de Módulo 3 (`Recibir estado de reserva`) para notificar el cierre del viaje:
    - Si el sub-estado es `Sin incidentes`: comunicar la entrega exitosa para que Módulo 3 libere el pago al anfitrión y le devuelva la garantía al cliente.
    - Si el sub-estado es `Con incidentes`: notificar el reporte de daños a Módulo 3 para que retenga la garantía e inicie el proceso de revisión.
- **FR-009**: **REGLA DE NEGOCIO ESTRICTA (Sin dinero):** El sistema **NO DEBE calcular costos de reparación, cobros por demora ni realizar devoluciones o retenciones de dinero**. La evaluación financiera le corresponde exclusivamente al Módulo 3.
- **FR-010**: Si la reserva se encuentra en cualquier estado diferente a "En Navegación", el sistema DEBE rechazar la solicitud e informar que el estado no es compatible.

---

### Key Entities

- **Reserva (`Reservation`)**: Entidad de dominio en Módulo 2 que cambia de estado "En Navegación" al estado "Completado", adoptando el sub-estado "Sin incidentes" o "Con incidentes".
- **Registro de Check-out (`CheckOutEvent`)**: Documento que guarda los datos de la entrega. Incluye: identificador del evento, identificador de la reserva, identificador del propietario, fecha/hora real de llegada, sub-estado de cierre y descripción de incidentes en caso de existir.
- **Embarcación**: Activo registrado en el Módulo 1 cuyo estado se actualiza a `Disponible` (si no hubo incidentes) o a `En Mantenimiento/Limpieza` (si hubo incidentes).

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Cero (0%) registros de fin de navegación permitidos sobre reservas que no estén en estado "En Navegación".
- **SC-002**: El 100% de los registros sin incidentes cambian la reserva a `Completado` (sub-estado `Sin incidentes`) y actualizan el barco como `Disponible` en Módulo 1 en menos de 1 segundo.
- **SC-003**: El 100% de los registros reportados con `Con incidentes` cambian el barco a `En Mantenimiento/Limpieza` en Módulo 1 y notifican la retención a Módulo 3 en menos de 1 segundo.
- **SC-004**: Cero (0) cobros, evaluaciones de daños o cálculo de dinero realizados dentro del Módulo 2.
- **SC-005**: El 100% de las entregas quedan registradas con la hora real de desembarque para auditorías.
- **SC-006**: Cero (0%) registros de entrega autorizados a personas diferentes al propietario del barco.