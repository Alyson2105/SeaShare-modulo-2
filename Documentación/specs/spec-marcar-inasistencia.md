# Feature Specification: Marcar Inasistencia

**Módulo**: Módulo 2 (Operación de Reservas, Tiempos y Cancelaciones)  
**Created**: 2026-09-06  
**Primary Actor**: Propietario (Anfitrión de la embarcación)  
**External Dependencies (APIs)**:
- **Módulo 1 (Gestión de Flota y Activos P2P)**: API externa `Consultar información embarcación` (para obtener el puerto de atraque de la embarcación y determinar la zona horaria oficial aplicable al cálculo de los 30 minutos de espera); y API externa `Asignar estado operativo` (liberación del barco a estado `Disponible`, orquestada indirectamente a través del caso de uso `Actualizar estado reserva`).
- **Módulo 3 (Liquidación, Seguros y Dispersión de Fondos)**: API externa `Recibir estado de reserva` (comunicación del nuevo estado principal `Cancelado` y sub-estado `Por Inasistencia`, orquestada indirectamente a través de `Actualizar estado reserva`, para que Módulo 3 entregue el pago de compensación al anfitrión).
- **Casos de uso internos de Módulo 2**: `Actualizar estado reserva` (`<<include>>`).

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - El Propietario reporta inasistencia del cliente tras esperar los 30 minutos de cortesía (Priority: P1)

Si pasan los 30 minutos de cortesia establecidos después de la hora acordada para la salida y el cliente no llega al muelle, el Propietario reporta que el cliente no se presentó (No-Show). En ese instante, el sistema verifica que ya pasaron los 30 minutos obligatorios, cambia la reserva al estado 'Cancelado' con el sub-estado 'Por Inasistencia' mediante la invocación a 'Actualizar estado reserva' (`<<include>>`), avisa al Módulo 1 para que el barco vuelva a quedar 'Disponible' y le notifica al Módulo 3 para que le entregue la compensación económica al Propietario según las políticas de la plataforma.

**Why this priority**: Protege el tiempo y la disponibilidad del anfitrión, permitiéndole liberar su barco para otros posibles alquileres y asegurando que reciba el pago por el tiempo de espera y la reserva perdida.

**Independent Test**: Se prueba con una reserva en estado "Confirmada" cuya hora de salida fue hace más de 30 minutos. El Propietario envía el reporte de inasistencia; se verifica que el sistema comprueba la hora, invoca a "Actualizar estado reserva" pasando el estado a "Cancelado" (sub-estado "Por Inasistencia"), guarda el registro y envía las notificaciones externas sin realizar cálculos de dinero.

**Acceptance Scenarios**:

1. **Scenario**: Inasistencia reportada correctamente tras pasar los 30 minutos de espera
    - **Given** una reserva en estado principal "Confirmada" cuya hora pactada de salida pasó hace 35 minutos
    - **When** el Propietario registrado del barco reporta la inasistencia del cliente
    - **Then** el sistema confirma que transcurrieron los 30 minutos de tolerancia, invoca "Actualizar estado reserva" para cambiar el estado a "Cancelado" con el sub-estado "Por Inasistencia", guarda el registro y coordina la liberación del barco en Módulo 1 y el aviso de pago a Módulo 3

2. **Scenario**: Inasistencia reportada justo al cumplirse los 30 minutos
    - **Given** una reserva en estado principal "Confirmada" cuya hora pactada de salida pasó hace exactamente 30 minutos
    - **When** el Propietario solicita marcar la inasistencia
    - **Then** el sistema acepta el reporte por haber alcanzado el tiempo mínimo de espera y procesa el cambio de estado de la reserva

---

### User Story 2 - Rechazar el reporte de inasistencia antes de cumplir los 30 minutos de espera (Priority: P1)

Si el Propietario intenta reportar la inasistencia del cliente antes de que pasen los 30 minutos de cortesía desde la hora de salida acordada, el sistema rechaza la solicitud de inmediato y le muestra cuántos minutos faltan para poder habilitar esa opción, protegiendo el derecho del cliente a llegar dentro del tiempo de tolerancia.

**Why this priority**: Garantiza un trato justo con el turista y evita que la reserva se cancele antes de tiempo durante el periodo oficial de espera.

**Independent Test**: Se prueba intentando reportar la inasistencia a los 10 o 20 minutos de la hora de salida de una reserva confirmada. Se verifica que el sistema rechaza la acción, mantiene la reserva como "Confirmada" y le indica al Propietario el tiempo exacto que le falta por esperar.

**Acceptance Scenarios**:

1. **Scenario**: Intento de reporte dentro del tiempo de espera de 30 minutos
    - **Given** una reserva en estado principal "Confirmada" cuya hora de salida acordada fue hace 18 minutos
    - **When** el Propietario intenta marcar inasistencia
    - **Then** el sistema rechaza la solicitud, informa que el tiempo de espera sigue activo indicando que faltan 12 minutos para habilitar el reporte, y NO cambia el estado de la reserva ni le notifica a otros módulos

2. **Scenario**: Intento de reporte antes de la hora de salida del viaje
    - **Given** una reserva en estado principal "Confirmada" cuya hora de salida es en el futuro
    - **When** el Propietario intenta marcar inasistencia
    - **Then** el sistema bloquea la acción informando que el viaje aún no ha comenzado

---

### User Story 3 - Rechazar el reporte de inasistencia en reservas no válidas o por usuarios no autorizados (Priority: P2)

Si se intenta marcar la inasistencia en una reserva que ya inició el viaje, que está pendiente de pago, cancelada, completada o vencida, o si la solicitud la realiza una persona diferente al Propietario del barco, el sistema rechaza la acción de inmediato.

**Why this priority**: Evita errores en el sistema, confusiones en viajes que ya salieron y que personas no autorizadas cancelen reservas ajenas.

**Independent Test**: Se prueba enviando solicitudes de inasistencia desde cuentas no autorizadas o sobre reservas que estén en estados como "En Navegación", "Completado" o "Pendiente de Pago", comprobando que el sistema rechaza el intento sin modificar los datos de la reserva.

**Acceptance Scenarios**:

1. **Scenario**: Intento de marcar inasistencia cuando el viaje ya inició
    - **Given** una reserva que ya cambió al estado "En Navegación" (el cliente ya abordó)
    - **When** el Propietario intenta marcar inasistencia
    - **Then** el sistema rechaza la solicitud e informa que el viaje ya comenzó y no se puede reportar una inasistencia

2. **Scenario**: Intento de marcar inasistencia por un usuario no autorizado
    - **Given** una reserva en estado principal "Confirmada" que cumple el tiempo de espera
    - **When** un usuario que no es el propietario registrado del barco intenta marcar la inasistencia
    - **Then** el sistema rechaza la solicitud por falta de permisos

---

### Edge Cases

- **Elección entre iniciar viaje con retraso o marcar inasistencia**: Pasados los 30 minutos de cortesía, si el cliente llega tarde (por ejemplo, al minuto 35), el Propietario decide si le permite abordar e iniciar el viaje o si marca la inasistencia:
    - Si el Propietario presiona "Marcar inicio de la navegación", la reserva pasa a "En Navegación" y ya no se podrá marcar inasistencia.
    - Si el Propietario presiona "Marcar inasistencia", la reserva pasa a "Cancelado" (sub-estado "Por Inasistencia") y el viaje queda cancelado de forma definitiva.
- **Zona horaria del puerto donde está el barco**: Los 30 minutos de espera se calculan según la hora real del puerto donde se encuentra el barco (obtenida mediante `Consultar información embarcación` en Módulo 1), evitando problemas si el teléfono del Propietario tiene una hora distinta.
- **Coincidencia entre cancelación del cliente y reporte de inasistencia**: Si el cliente cancela desde su aplicación al mismo tiempo que el Propietario reporta la inasistencia, el sistema procesa la primera solicitud que reciba y rechaza la segunda por encontrarse en un estado ya cerrado.
- **Reservas que no han sido pagadas**: Una reserva que está en "Pendiente de Pago" no aplica para reporte de inasistencia. Si el cliente no paga a tiempo, la reserva se cancela por tiempo límite agotado (TTL), no por inasistencia.
- **Prohibición de calcular dinero en Módulo 2**: El Módulo 2 **NO calcula pagos, compensaciones ni penalidades en dinero**. Su función termina al verificar el tiempo transcurrido, cambiar el estado e informar al Módulo 3 el sub-estado "Por Inasistencia" para que el Módulo 3 realice la entrega del dinero.
- **Falla al consultar la información del barco**: Si no se puede consultar el puerto y la zona horaria del barco en Módulo 1 al momento de verificar la hora, el sistema NO debe asumir una zona horaria por defecto. [NEEDS CLARIFICATION: ¿se reintenta la consulta, o se rechaza temporalmente la solicitud de inasistencia hasta poder resolver la zona horaria?]

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE permitir registrar la inasistencia (No-Show) de una reserva si y solo si la reserva existe y se encuentra en estado principal "Confirmada".
- **FR-002**: El sistema DEBE verificar y garantizar que el usuario que solicita marcar la inasistencia sea estrictamente el Propietario registrado de la embarcación.
- **FR-003**: El sistema DEBE validar que hayan transcurrido al menos treinta (30) minutos continuos desde la fecha y hora pactada de inicio de la reserva (`tiempo_actual >= fecha_hora_inicio + 30 minutos`).
- **FR-004**: La validación de la ventana de tolerancia de 30 minutos DEBE calcularse tomando como referencia la zona horaria del puerto donde opera la embarcación, obtenida mediante la API `Consultar información embarcación` de Módulo 1.
- **FR-005**: Si no han transcurrido los 30 minutos de tolerancia, el sistema DEBE rechazar la solicitud, calcular y mostrar al Propietario los minutos y segundos exactos que faltan de espera, y NO DEBE realizar cambios de estado ni enviar notificaciones.
- **FR-006**: Si la reserva se encuentra en cualquier estado diferente a "Confirmada" (incluyendo Pendiente de Pago, En Navegación, Completado, Expirado o Cancelado), el sistema DEBE rechazar la solicitud e informar la incompatibilidad del estado.
- **FR-007**: Al confirmar el reporte de inasistencia, el sistema DEBE invocar el caso de uso subordinado "Actualizar estado reserva" (`<<include>>`), solicitando cambiar el estado a `Cancelado` con el sub-estado de cancelación `Por Inasistencia` .
- **FR-008**: El sistema DEBE guardar un registro del evento de inasistencia, incluyendo: identificador de la reserva, identificador del propietario, fecha y hora del reporte, minutos de espera transcurridos y observaciones opcionales del anfitrión.
- **FR-009**: **REGLA DE NEGOCIO ESTRICTA (Sin dinero):** El sistema **NO DEBE calcular montos de compensación, devoluciones, comisiones ni realizar pagos bancarios**. El Módulo 2 solo verifica el tiempo y actualiza el estado; la entrega de dinero al anfitrión la realiza el Módulo 3 al recibir el aviso del sub-estado "Por Inasistencia".
- **FR-010**: El sistema DEBE indicar a "Actualizar estado reserva" que llame a la API de Módulo 1 (`Asignar estado operativo`) para liberar la embarcación al estado `Disponible`.
- **FR-011**: El sistema DEBE indicar a "Actualizar estado reserva" que llame a la API de Módulo 3 (`Recibir estado de reserva`) para comunicar el estado `Cancelado` y el sub-estado `Por Inasistencia`.
- **FR-012**: Si la API `Consultar información embarcación` de Módulo 1 no responde o no entrega la zona horaria, el sistema NO DEBE calcular la tolerancia con una zona horaria asumida por defecto. [NEEDS CLARIFICATION: política de reintento o rechazo temporal ante esta falla — ver Edge Cases].

---

### Key Entities

- **Reserva (`Reservation`)**: Entidad de dominio en Módulo 2. Atributos evaluados: identificador, identificador del propietario, identificador de la embarcación, fecha/hora de inicio, estado principal ("Confirmada" → "Cancelado"), sub-estado ("Por Inasistencia").
- **Registro de Inasistencia (`NoShowEvent`)**: Registro del evento para auditoría. Atributos: identificador del evento, identificador de la reserva, identificador del anfitrión, fecha/hora del reporte, minutos de espera observados y comentarios del anfitrión.
- **Embarcación**: Activo registrado en el Módulo 1 cuya zona horaria se consulta mediante `Consultar información embarcación`.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Cero (0%) reportes de inasistencia aceptados antes de cumplirse exactamente los 30 minutos de espera posteriores a la hora acordada de salida.
- **SC-002**: Cero (0%) reportes de inasistencia aceptados en reservas que estén en estado "En Navegación", "Completado", "Cancelado", "Expirado" o "Pendiente de Pago".
- **SC-003**: El 100% de los intentos realizados antes de tiempo muestran un mensaje con el tiempo exacto que resta para cumplir los 30 minutos de espera.
- **SC-004**: El 100% de los reportes válidos cambian la reserva a Cancelado (sub-estado Por Inasistencia) y notifican a Módulo 1 y Módulo 3 en menos de 1 segundo.
- **SC-005**: Cero (0) cálculos de dinero, penalidades o pagos realizados dentro del Módulo 2.
- **SC-006**: Cero (0%) solicitudes de inasistencia autorizadas a personas diferentes al propietario del barco.