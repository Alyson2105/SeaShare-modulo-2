# Feature Specification: Recibir Estado de Reserva

**Módulo**: Módulo 2 (Operación de Reservas, Tiempos y Cancelaciones)  
**Created**: 2026-09-06  
**Primary Actor / Disparador**: Invocación interna desde el caso de uso `Actualizar estado reserva` (al confirmarse cualquier cambio de estado en la reserva) / Módulo 3 (Gestión Liquidación) como consumidor externo  
**External Dependencies (APIs)**:
- **Módulo 3 (Liquidación, Seguros y Dispersión de Fondos)**: API externa de tesorería y liquidación (`Recibir estado de reserva` que atiende los avisos de cambio de estado para activar seguros, guardar o devolver el depósito de garantía, y entregar los pagos o reembolsos).

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Avisar en tiempo real al Módulo 3 sobre los cambios de estado del viaje (Priority: P1)

Cada vez que una reserva cambia de estado dentro del Módulo 2 (`Pendiente de Pago`, `Confirmada`, `En Navegación`, `Completado`, `Cancelado` o `Expirado`), este proceso envía una notificación inmediata al Módulo 3. Con esta información, el Módulo 3 maneja los cobros y seguros: activa el seguro del barco, guarda o devuelve la garantía y le entrega el dinero al anfitrión o al cliente según corresponda.

**Why this priority**: Es la comunicación indispensable entre la operación del viaje y el área de cobros. Sin estos avisos, el Módulo 3 no sabría cuándo activar los seguros ni cuándo entregar o devolver el dinero guardado.

**Independent Test**: Se prueba realizando cambios de estado en el Módulo 2 conectándolo a un simulador de Módulo 3. Se comprueba que Módulo 3 recibe la información exacta (código de reserva, estado principal, sub-estado y fecha/hora), verificando que Módulo 2 no envía ni calcula montos de dinero.

**Acceptance Scenarios**:

1. **Scenario**: Notificación de reserva Confirmada a Módulo 3
    - **Given** una reserva que pasa al estado "Confirmada"
    - **When** se guarda el cambio en Módulo 2
    - **Then** el sistema le avisa al Módulo 3 que la reserva está "Confirmada" para que guarde los fondos y active la póliza de seguro

2. **Scenario**: Notificación de inicio de viaje (En Navegación) a Módulo 3
    - **Given** una reserva que pasa al estado "En Navegación"
    - **When** se confirma la salida del barco en Módulo 2
    - **Then** el sistema le avisa al Módulo 3 que el barco está "En Navegación" para ratificar que el seguro está activo en el agua

3. **Scenario**: Notificación de viaje Completado sin problemas a Módulo 3
    - **Given** una reserva que termina en estado "Completado" con el sub-estado "Sin incidentes"
    - **When** el Propietario registra la entrega en Módulo 2
    - **Then** el sistema le notifica al Módulo 3 para que le entregue el dinero al anfitrión y le devuelva el depósito de garantía al cliente

4. **Scenario**: Notificación de reserva Cancelada con su motivo exacto a Módulo 3
    - **Given** una reserva que pasa a estado "Cancelado" con un sub-estado asignado (`Flexible`, `Moderado`, `Tardío`, `Por Anfitrión` o `Por Inasistencia`)
    - **When** se confirma la cancelación en Módulo 2
    - **Then** el sistema le informa a Módulo 3 el estado y el sub-estado de cancelación para que este calcule los reembolsos o penalidades que apliquen

---

### User Story 2 - Notificar de inmediato los reportes de daños para congelar la garantía (Priority: P1)

Si al recibir el barco el Propietario reporta daños o averías y la reserva finaliza en estado 'Completado' con el sub-estado 'Con incidentes', este proceso le envía un aviso prioritario al Módulo 3. Esta notificación le indica al Módulo 3 que debe retener de forma preventiva el depósito de garantía del cliente e iniciar la revisión del reclamo, sin que Módulo 2 evalúe el costo de los daños.

**Why this priority**: Evita que el Módulo 3 le devuelva el dinero de la garantía al cliente cuando el barco sufrió daños o pérdidas durante el viaje.

**Independent Test**: Se prueba simulando la entrega de un barco con la opción "Con incidentes" y comentarios de los daños. Se verifica que el mensaje enviado a Módulo 3 incluye el aviso de retención de garantía y los comentarios, asegurando que Módulo 2 no calcula valores a cobrar.

**Acceptance Scenarios**:

1. **Scenario**: Notificación de entrega con reporte de daños a Módulo 3
    - **Given** una reserva que pasa a "Completado" con sub-estado "Con incidentes" y la descripción de los problemas encontrados
    - **When** se registra la entrega en Módulo 2
    - **Then** el sistema le notifica al Módulo 3 el estado "Completado", el sub-estado "Con incidentes" y los comentarios para que retenga el depósito de garantía y gestione el reclamo

---

### User Story 3 - Reintentar el envío de mensajes si falla la conexión con Módulo 3 (Priority: P2)

Si al intentar enviar una notificación al Módulo 3 se pierde la conexión a internet, la respuesta tarda demasiado o el servidor falla, el sistema guarda la notificación en una lista de pendientes y la reintenta enviar automáticamente hasta que Módulo 3 la reciba con éxito.

**Why this priority**: Asegura que ningún aviso de cambio de estado se pierda por una falla temporal de red, evitando que los pagos o devoluciones de dinero queden trabados.

**Independent Test**: Se prueba simulando un corte de red cuando se intenta enviar un aviso a Módulo 3. Se comprueba que el sistema guarda el mensaje pendiente, lo reintenta cuando regresa la conexión y lo marca como entregado al recibir la confirmación de Módulo 3.

**Acceptance Scenarios**:

1. **Scenario**: Reintento exitoso tras una falla temporal de red
    - **Given** un cambio de estado confirmado en Módulo 2
    - **When** el primer intento de envío a Módulo 3 falla por desconexión
    - **Then** el sistema guarda el mensaje en pendientes, reintenta el envío y confirma la entrega una vez restablecida la conexión con Módulo 3

---

### Edge Cases

- **Evitar notificaciones duplicadas**:
    - Cada mensaje enviado a Módulo 3 lleva un código único y la hora exacta del evento, permitiendo que Módulo 3 identifique y descarte avisos repetidos en caso de reintentos por falla de red.
- **Respeto estricto del orden de los eventos**:
    - El sistema garantiza que las notificaciones de una misma reserva se entreguen a Módulo 3 en el orden exacto en que ocurrieron (por ejemplo, primero `Confirmada`, luego `En Navegación` y finalmente `Completado`), evitando errores en los cobros.
- **Prohibición de calcular o mover dinero en Módulo 2**:
    - Este caso de uso solo envía avisos sobre lo que sucede en el viaje; **NO calcula comisiones, penalidades, costos de seguro ni montos de devolución**. Módulo 2 informa QUÉ pasó y CUÁNDO pasó; Módulo 3 decide CUÁNTO dinero se entrega.
- **Falla prolongada en la conexión con Módulo 3**:
    - Si el Módulo 3 permanece caído tras varios reintentos, la notificación se queda guardada en estado "Pendiente de Entrega" y se genera una alerta para su revisión, garantizando que no se pierda ningún evento.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE ofrecer un mecanismo interno para recibir y enviar los avisos de cambio de estado generados desde el caso de uso `Actualizar estado reserva`.
- **FR-002**: El sistema DEBE construir la notificación para Módulo 3 incluyendo los siguientes datos: código de la reserva, código del cliente, código del barco, estado principal alcanzado (`Pendiente de Pago`, `Confirmada`, `En Navegación`, `Completado`, `Cancelado`, `Expirado`), sub-estado (si aplica), fecha/hora del evento y el proceso que lo causó.
- **FR-003**: Cuando la reserva pase a estado `Confirmada`, el sistema DEBE notificar a Módulo 3 para habilitar la activación de la póliza de seguro y la custodia del dinero.
- **FR-004**: Cuando la reserva pase a estado `En Navegación`, el sistema DEBE notificar a Módulo 3 para confirmar el inicio del viaje y la cobertura del seguro en el agua.
- **FR-005**: Cuando la reserva pase a estado `Completado` con sub-estado `Sin incidentes`, el sistema DEBE notificar a Módulo 3 para que entregue el pago al anfitrión y le devuelva la garantía al cliente.
- **FR-006**: 🔶 [PENDIENTE DE CONFIRMAR — Notificación de incidentes a Módulo 3]: Cuando la reserva pase a estado `Completado` con sub-estado `Con incidentes`, el sistema DEBE notificar a Módulo 3 enviando los detalles y comentarios de las averías para que Módulo 3 retenga la garantía e inicie la revisión. [FIN PENDIENTE]
- **FR-007**: Cuando la reserva pase a estado `Cancelado`, el sistema DEBE notificar a Módulo 3 el sub-estado de cancelación correspondiente (`Flexible`, `Moderado`, `Tardío`, `Por Anfitrión` o `Por Inasistencia`) para que Módulo 3 aplique las reglas de devolución y penalidades.
- **FR-008**: Cuando la reserva pase a estado `Expirado`, el sistema DEBE notificar a Módulo 3 para el cierre del intento de reserva y la liberación de cobros si existieron intentos en proceso.
- **FR-009**: El sistema DEBE contar con un mecanismo de envío garantizado que reintente entregar los avisos pendientes si ocurre una falla temporal de conexión con Módulo 3.
- **FR-010**: **REGLA DE NEGOCIO ESTRICTA (Sin dinero):** El sistema **NO DEBE calcular devoluciones, penalidades en dinero, costos de seguros ni realizar transferencias**. Toda la lógica de cobros y cuentas es responsabilidad exclusiva del Módulo 3.
- **FR-011**: El sistema DEBE guardar un registro de cada notificación enviada a Módulo 3, guardando: código del evento, código de la reserva, estado/sub-estado notificado, fecha/hora de envío y la confirmación de recepción entregada por Módulo 3.

---

### Key Entities

- **Evento de Estado (`ReservationStatusEvent`)**: Datos del mensaje enviado a Módulo 3. Incluye: código del evento, código de la reserva, código del cliente, código del barco, estado principal, sub-estado, fecha/hora del evento, aviso de incidentes y comentarios explicativos.
- **Reserva (`Reservation`)**: Entidad del Módulo 2 cuyos cambios de estado se notifican a Módulo 3.
- **Acuse de Recibo (`Module3Acknowledgment`)**: Respuesta enviada por Módulo 3 confirmando que recibió la notificación correctamente.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de los cambios de estado confirmados en `Actualizar estado reserva` envían la notificación al Módulo 3 en menos de 500 milisegundos.
- **SC-002**: Cero (0%) avisos de cambio de estado perdidos (el 100% de los eventos se reintentan hasta recibir confirmación de Módulo 3).
- **SC-003**: El 100% de los cierres de viaje con sub-estado `Con incidentes` envían el aviso de retención de garantía y los comentarios al Módulo 3.
- **SC-004**: Cero (0) cálculos de dinero, tarifas, comisiones o pagos realizados dentro de Módulo 2.
- **SC-005**: El 100% de las notificaciones enviadas incluyen un código único para evitar que Módulo 3 procese mensajes duplicados.
