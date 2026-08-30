# Feature Specification: Módulo 2 - Operación de Reservas, Tiempos y Cancelaciones

**Created**: 2026-08-30  
**Project**: SEA-SHARE (Plataforma P2P de Alquiler Náutico)  
**Module**: Módulo 2 (Operación de Reservas, Tiempos y Cancelaciones)  
**Status**: Approved / Ready for Design

---

## User Scenarios & Testing *(mandatory)*

<!--
  User stories prioritized as user journeys ordered by importance (P1 to P3).
  Each user story is independently testable and delivers standalone business value.
-->

### User Story 1 - Creación de Reserva con Bloqueo Temporal (TTL de 15 Minutos) y Confirmación de Pago (Priority: P1)

Como **Arrendatario (Turista)**,  
quiero seleccionar una embarcación disponible para una fecha y franja horaria específica e iniciar el proceso de reserva asegurando un bloqueo exclusivo temporal,  
para poder proceder con el pago sin riesgo de que otro usuario tome la misma embarcación mientras completo la transacción.

**Why this priority**: Es el flujo central (Core Journey) del negocio. Sin la capacidad de reservar, bloquear el inventario de forma segura y confirmar la reserva tras el pago, la plataforma no puede generar transacciones ni operar comercialmente.

**Independent Test**: Puede probarse de punta a punta iniciando una solicitud de reserva sobre una embarcación en estado "Disponible", validando que el inventario quede bloqueado por un temporizador de 15 minutos, y simulando la recepción de confirmación de pago desde el Módulo 3 para verificar que la reserva pase a estado "Confirmada" y la embarcación quede asegurada.

**Acceptance Scenarios**:

1. **Scenario**: Bloqueo temporal exitoso al iniciar reserva
   - **Given** que el Arrendatario consulta una embarcación con estado "Disponible" en el Módulo 1 para una fecha y franja horaria determinadas.
   - **When** el Arrendatario solicita iniciar la reserva para dicha franja horaria.
   - **Then** el sistema crea la reserva en estado "Pendiente de Pago", inicia un temporizador de bloqueo temporal (TTL) de 15 minutos exactos, notifica al Módulo 1 el cambio de estado de la embarcación a "Reservado" para impedir reservas concurrentes, y envía los datos de la reserva al Módulo 3 para habilitar la solicitud de pago.

2. **Scenario**: Confirmación exitosa de reserva por pago recibido dentro de la ventana TTL
   - **Given** una reserva en estado "Pendiente de Pago" con un TTL activo restante (dentro de los 15 minutos).
   - **When** el sistema recibe la confirmación de pago exitoso desde el Módulo 3.
   - **Then** el sistema cancela el temporizador TTL, transiciona el estado de la reserva a "Confirmada", emite un identificador único de reserva confirmada al Arrendatario y al Propietario, y mantiene el estado operativo en Módulo 1 asegurando el bloqueo para las fechas pactadas.

3. **Scenario**: Intento de reserva sobre embarcación no disponible o en franja solapada
   - **Given** una embarcación que se encuentra en estado "Reservado", "En Navegación" o "En Mantenimiento/Limpieza" en el Módulo 1 para la fecha/hora seleccionada.
   - **When** un Arrendatario intenta iniciar una reserva para esa misma fecha y franja horaria.
   - **Then** el sistema rechaza la solicitud de reserva informando que la embarcación no se encuentra disponible y no genera ningún bloqueo ni registro de pago.

---

### User Story 2 - Expiración Automática del Bloqueo Temporal (TTL) por Falta de Pago (Priority: P1)

Como **Sistema SEA-SHARE / Propietario**,  
quiero que el bloqueo temporal de una embarcación expire automáticamente al cumplirse los 15 minutos sin confirmación de pago,  
para liberar el inventario inmediatamente y permitir que otros potenciales clientes puedan reservarlo sin retener el activo indebidamente.

**Why this priority**: Evita el bloqueo malicioso o accidental del inventario náutico (denegación de servicio a nivel de producto), garantizando la máxima disponibilidad comercial de las embarcaciones para los Propietarios.

**Independent Test**: Puede probarse creando una reserva en estado "Pendiente de Pago" y dejando transcurrir el temporizador de 15 minutos sin enviar confirmación de pago; el sistema debe marcar automáticamente la reserva como "Expirada" y emitir la notificación al Módulo 1 para restituir la embarcación a estado "Disponible".

**Acceptance Scenarios**:

1. **Scenario**: Expiración del temporizador TTL a los 15 minutos sin pago
   - **Given** una reserva en estado "Pendiente de Pago" cuyo temporizador TTL de 15 minutos llega a cero sin haber recibido notificación de pago exitoso de Módulo 3.
   - **When** se cumple el límite de tiempo de 15:00 minutos.
   - **Then** el sistema transiciona automáticamente la reserva al estado "Expirada", notifica al Módulo 1 para cambiar el estado operativo de la embarcación a "Disponible", y notifica al Arrendatario que su tiempo de reserva ha caducado.

2. **Scenario**: Llegada extemporánea de confirmación de pago tras expiración del TTL
   - **Given** una reserva que ya ha transitado al estado "Expirada" tras vencerse su TTL de 15 minutos y cuyo activo ya fue liberado.
   - **When** el Módulo 3 envía una confirmación de pago tardía asociada a dicha reserva.
   - **Then** el sistema rechaza la confirmación de la reserva manteniendo el estado "Expirada", y responde inmediatamente al Módulo 3 con un evento de "Pago Extemporáneo Rechazado / Reserva Expirada" para que el Módulo 3 gestione la reversión o reembolso correspondiente.

---

### User Story 3 - Clasificación y Notificación de Cancelación Solicitada por el Arrendatario (Priority: P1)

Como **Arrendatario**,  
quiero solicitar la cancelación de mi reserva confirmada a través de la plataforma antes del inicio del viaje,  
para que el sistema clasifique formalmente el tipo de cancelación de acuerdo con la anticipación de mi solicitud y notifique al Módulo 3 y Módulo 1 para los trámites correspondientes.

**Why this priority**: Define las reglas de interacción y salida voluntaria del contrato de alquiler, determinando con precisión temporal el tipo de política aplicable sin intervenir en liquidaciones monetarias.

**Independent Test**: Puede probarse sobre reservas en estado "Confirmada", solicitando la cancelación en tres momentos temporales distintos respecto a la hora pactada de zarpe (>72h, entre 72h y 24h, y <24h), comprobando que el sistema asigne la categoría correcta ("Flexible", "Moderada" o "Tardía"), actualice el estado a "Cancelada por Arrendatario", notifique la clasificación al Módulo 3 y libere el activo en Módulo 1.

**Acceptance Scenarios**:

1. **Scenario**: Cancelación con anticipación mayor a 72 horas (Cancelación Flexible)
   - **Given** una reserva en estado "Confirmada" cuya hora pactada de inicio es $T_{inicio}$.
   - **When** el Arrendatario solicita cancelar la reserva en un momento $T_{cancel}$ tal que $(T_{inicio} - T_{cancel}) > 72\text{ horas}$.
   - **Then** el sistema clasifica la cancelación como "Cancelación Flexible", actualiza el estado de la reserva a "Cancelada por Arrendatario", notifica al Módulo 3 el identificador de la reserva y el tipo "Cancelación Flexible", y notifica al Módulo 1 para cambiar el estado de la embarcación a "Disponible".

2. **Scenario**: Cancelación con anticipación entre 24 y 72 horas inclusive (Cancelación Moderada)
   - **Given** una reserva en estado "Confirmada" cuya hora pactada de inicio es $T_{inicio}$.
   - **When** el Arrendatario solicita cancelar la reserva en un momento $T_{cancel}$ tal que $24\text{ horas} \le (T_{inicio} - T_{cancel}) \le 72\text{ horas}$.
   - **Then** el sistema clasifica la cancelación como "Cancelación Moderada", actualiza el estado de la reserva a "Cancelada por Arrendatario", notifica al Módulo 3 el identificador de reserva y el tipo "Cancelación Moderada", y notifica al Módulo 1 la liberación de la embarcación a "Disponible".

3. **Scenario**: Cancelación con anticipación menor a 24 horas (Cancelación Tardía)
   - **Given** una reserva en estado "Confirmada" cuya hora pactada de inicio es $T_{inicio}$.
   - **When** el Arrendatario solicita cancelar la reserva en un momento $T_{cancel}$ tal que $(T_{inicio} - T_{cancel}) < 24\text{ horas}$.
   - **Then** el sistema clasifica la cancelación como "Cancelación Tardía", actualiza el estado de la reserva a "Cancelada por Arrendatario", notifica al Módulo 3 el identificador de reserva y el tipo "Cancelación Tardía", y notifica al Módulo 1 la liberación de la embarcación a "Disponible".

---

### User Story 4 - Registro de Inasistencia (No-Show) por el Propietario tras Ventana de Tolerancia (Priority: P2)

Como **Propietario (Anfitrión)**,  
quiero marcar formalmente la inasistencia (No-Show) del Arrendatario una vez transcurrida la ventana de tolerancia de 30 minutos desde la hora pactada de zarpe,  
para dar por terminada la espera, liberar la embarcación y notificar el incidente al Módulo 3 para su debida liquidación.

**Why this priority**: Protege el tiempo del anfitrión en puerto cuando el turista no se presenta, formalizando el cierre de la reserva sin penalizar la disponibilidad operativa futura de la embarcación.

**Independent Test**: Puede probarse configurando una reserva confirmada con hora pactada de inicio $T_{inicio}$, validando que la opción "Marcar inasistencia" permanezca bloqueada antes de $T_{inicio} + 30\text{ minutos}$, y habilitada a partir de $T_{inicio} + 30\text{ minutos}$; al activarla, la reserva transiciona a "Cancelada por Inasistencia (No-Show)", se notifica a Módulo 3 como "No-Show" y a Módulo 1 como "Disponible".

**Acceptance Scenarios**:

1. **Scenario**: Intento de marcar inasistencia antes de cumplirse la ventana de tolerancia de 30 minutos
   - **Given** una reserva en estado "Confirmada" con hora de inicio pactada a las 10:00 AM.
   - **When** el Propietario intenta marcar inasistencia a las 10:20 AM (transcurridos solo 20 minutos).
   - **Then** el sistema rechaza la acción, informando que deben transcurrir al menos 30 minutos de tolerancia (habilitado a partir de las 10:30 AM) y mantiene la reserva en estado "Confirmada".

2. **Scenario**: Marcación exitosa de inasistencia tras los 30 minutos de tolerancia
   - **Given** una reserva en estado "Confirmada" con hora de inicio pactada a las 10:00 AM donde el Arrendatario no se ha presentado ni se ha efectuado check-in.
   - **When** el Propietario activa la acción "Marcar inasistencia" a las 10:31 AM (o posterior).
   - **Then** el sistema valida que han transcurrido $\ge 30\text{ minutos}$, transiciona el estado de la reserva a "Cancelada por Inasistencia (No-Show)", notifica al Módulo 3 la condición de "No-Show" junto con la marca temporal del evento, y notifica al Módulo 1 para restablecer la embarcación a estado "Disponible".

---

### User Story 5 - Check-in Manual de Zarpe y Finalización del Recorrido Náutico (Priority: P2)

Como **Propietario (Anfitrión)**,  
quiero registrar el check-in manual al momento del embarque e inicio de la navegación, y posteriormente registrar la finalización del viaje al retornar a puerto,  
para mantener el estado operativo de la reserva y de la embarcación sincronizados en tiempo real entre los módulos del sistema.

**Why this priority**: Asegura el control operativo del activo físico durante su navegación real, evitando confusiones de estado entre reservas activas, viajes en curso y finalizaciones.

**Independent Test**: Puede probarse sobre una reserva en estado "Confirmada" el día del viaje ejecutando la acción de Check-in para verificar la transición a "En Navegación" (y notificación a Módulo 1), y posteriormente ejecutando la acción de "Finalizar Viaje" para verificar la transición a "Completada" (notificando a Módulo 1 y Módulo 3).

**Acceptance Scenarios**:

1. **Scenario**: Check-in manual de inicio de navegación exitoso
   - **Given** una reserva en estado "Confirmada" en la fecha y rango horario acordados.
   - **When** el Propietario realiza el check-in manual confirmando el zarpe de la embarcación con los pasajeros.
   - **Then** el sistema cambia el estado de la reserva a "En Navegación", inhabilita las opciones de cancelación o inasistencia, y notifica al Módulo 1 para actualizar el estado de la embarcación a "En Navegación".

2. **Scenario**: Finalización exitosa del servicio náutico
   - **Given** una reserva en estado "En Navegación".
   - **When** el Propietario confirma el desembarque y retorno exitoso de la embarcación a puerto.
   - **Then** el sistema cambia el estado de la reserva a "Completada", notifica al Módulo 1 la liberación de la embarcación a estado "Disponible" (o "En Mantenimiento/Limpieza"), y notifica al Módulo 3 el cierre operativo del servicio para la liquidación final.

---

### User Story 6 - Cancelación Excepcional de Reserva por parte del Propietario (Fuerza Mayor) (Priority: P3)

Como **Propietario (Anfitrión)**,  
quiero solicitar la cancelación justificada de una reserva confirmada antes del zarpe por motivos de fuerza mayor (condiciones climáticas adversas, avería mecánica imprevista o mantenimiento urgente),  
para declarar la imposibilidad operativa del servicio y notificar oportunamente al Arrendatario, al Módulo 1 y al Módulo 3.

**Why this priority**: Maneja las excepciones operativas atribuibles a la embarcación o a seguridad náutica, asegurando que el sistema registre la causa anfitrión y desvincule al Arrendatario de penalidades.

**Independent Test**: Puede probarse sobre una reserva confirmada ejecutando la acción de cancelación por el Propietario seleccionando un motivo de fuerza mayor; el sistema debe cambiar el estado a "Cancelada por Propietario", clasificar el evento como "Cancelación por Anfitrión", notificar al Módulo 3 para reembolso total al turista y actualizar la embarcación en Módulo 1 (a "En Mantenimiento/Limpieza" o "Disponible").

**Acceptance Scenarios**:

1. **Scenario**: Cancelación justificada por el Propietario antes del inicio del viaje
   - **Given** una reserva en estado "Confirmada" previa a la realización del check-in.
   - **When** el Propietario solicita la cancelación de la reserva indicando un motivo operativo/fuerza mayor.
   - **Then** el sistema transiciona la reserva a "Cancelada por Propietario", clasifica la notificación hacia el Módulo 3 como "Cancelación por Anfitrión", notifica al Módulo 1 para registrar el estado del activo (ej. "En Mantenimiento/Limpieza"), y envía una alerta prioritaria de cancelación al Arrendatario.

---

### Edge Cases

- **Confirmación de pago concurrente con el segundo exacto de expiración del TTL (minuto 15:00:00)**: Si el evento de confirmación de pago del Módulo 3 entra exactamente en la milésima de segundo en que el temporizador TTL vence, el sistema debe resolver la condición de carrera de forma atómica: si la reserva ya cambió a estado "Expirada", el pago se rechaza y se notifica al Módulo 3 para reembolso; si el pago se procesó antes del cambio de estado, la reserva se confirma y se desactiva el vencimiento.
- **Intento de cancelación en los límites temporales exactos (72:00:00h y 24:00:00h)**:
  - Si la cancelación entra con exactamente 72 horas y 00 minutos antes del inicio ($T = 72\text{h}$), se clasifica como **Cancelación Moderada** (dado que Flexible requiere estrictamente $> 72\text{h}$).
  - Si la cancelación entra con exactamente 24 horas y 00 minutos antes del inicio ($T = 24\text{h}$), se clasifica como **Cancelación Moderada** (rango $24\text{h} \le \Delta T \le 72\text{h}$).
  - Si la cancelación entra con 23 horas, 59 minutos y 59 segundos ($\Delta T < 24\text{h}$), se clasifica estrictamente como **Cancelación Tardía**.
- **Reservas de último momento creadas con menos de 24 horas de anticipación**: Si un Arrendatario reserva y confirma un viaje cuyo zarpe es en 6 horas y posteriormente decide cancelar, el sistema aplica directamente la clasificación de **Cancelación Tardía** ($\Delta T < 24\text{h}$).
- **Intentos concurrentes de reserva sobre la misma embarcación**: Si dos Arrendatarios intentan iniciar una reserva sobre la misma embarcación y rango de horas simultáneamente, el sistema debe otorgar el bloqueo temporal (TTL) al primer solicitante procesado y rechazar inmediatamente la segunda solicitud indicando falta de disponibilidad.
- **Intento de cancelación posterior al Check-in**: Si el Arrendatario o Propietario intenta solicitar una cancelación ordinaria o marcar inasistencia cuando la reserva ya está en estado "En Navegación", el sistema debe rechazar la acción indicando que el servicio ya se encuentra en curso.
- **Intento de Check-in posterior al registro de No-Show**: Si el Propietario ya marcó "No-Show" (tras los 30 minutos de tolerancia), el sistema bloquea cualquier intento posterior de iniciar navegación para esa reserva.
- **Fallas de comunicación o indisponibilidad temporal de Módulo 1 o Módulo 3**: Si al actualizar un estado de reserva la comunicación con Módulo 1 o Módulo 3 falla, el sistema debe garantizar la persistencia del estado en Módulo 2 y reintentar la entrega del evento de estado hasta recibir confirmación de recepción, evitando desincronización de inventario.

---

## Requirements *(mandatory)*

### Functional Requirements

#### Gestión de Creación de Reserva y Control de Tiempos (TTL)
- **FR-001**: El sistema DEBE permitir al Arrendatario solicitar la creación de una reserva indicando: ID de la embarcación, fecha y hora pactada de inicio ($T_{inicio}$), fecha y hora pactada de finalización ($T_{fin}$), y número de pasajeros.
- **FR-002**: El sistema DEBE validar con el Módulo 1 que la embarcación seleccionada se encuentre en estado operativo "Disponible" para la ventana de tiempo solicitada antes de iniciar la reserva.
- **FR-003**: Al iniciar una reserva válida, el sistema DEBE registrar la reserva en estado "Pendiente de Pago" y activar un bloqueo temporal exclusivo con un Time-To-Live (TTL) estricto de **15 minutos**.
- **FR-004**: El sistema DEBE notificar inmediatamente al Módulo 1 el cambio de estado operativo de la embarcación a "Reservado" para impedir que otros usuarios reserven la misma franja de tiempo.
- **FR-005**: El sistema DEBE proveer la información de cotización y parámetros de tiempo de la reserva al Módulo 3 para habilitar la generación de la orden de pago.
- **FR-006**: Si el temporizador de 15 minutos expira sin haber recibido la confirmación de pago exitosa desde el Módulo 3, el sistema DEBE transicionar automáticamente la reserva a estado "Expirada".
- **FR-007**: Al transicionar a estado "Expirada", el sistema DEBE notificar inmediatamente al Módulo 1 para restablecer la embarcación al estado "Disponible".
- **FR-008**: Si se recibe una confirmación de pago para una reserva que ya se encuentra en estado "Expirada" o "Cancelada", el sistema DEBE rechazar la confirmación y emitir una notificación de error/rechazo extemporáneo al Módulo 3 para la reversión del pago.
- **FR-009**: Al recibir una confirmación de pago válida y oportuna (dentro de los 15 minutos del TTL) desde el Módulo 3, el sistema DEBE transicionar la reserva al estado "Confirmada" y desactivar el temporizador de expiración.

#### Ciclo Operativo de Navegación y Check-in
- **FR-010**: El sistema DEBE permitir al Propietario registrar el **Check-in Manual** de inicio de viaje en la fecha acordada de la reserva.
- **FR-011**: Al registrar el Check-in Manual, el sistema DEBE actualizar el estado de la reserva a "En Navegación" y notificar al Módulo 1 para actualizar el estado del activo náutico a "En Navegación".
- **FR-012**: El sistema DEBE permitir al Propietario registrar la **Finalización del Viaje** una vez concluido el desembarque en puerto.
- **FR-013**: Al registrar la Finalización del Viaje, el sistema DEBE transicionar la reserva al estado "Completada", notificar al Módulo 1 la liberación de la embarcación a estado "Disponible" (o mantenimiento según reporte), y notificar al Módulo 3 el cierre operativo para la liquidación final.

#### Clasificación de Cancelaciones y Reglas de Negocio
- **FR-014**: El sistema DEBE permitir al Arrendatario solicitar la cancelación de una reserva en estado "Confirmada" antes de la realización del Check-in.
- **FR-015**: Al recibir una solicitud de cancelación del Arrendatario, el sistema DEBE calcular el tiempo restante $\Delta T = (T_{inicio} - T_{solicitud})$ y clasificar el tipo de cancelación de forma unívoca bajo las siguientes reglas:
  - **Cancelación Flexible**: Si $\Delta T > 72\text{ horas}$.
  - **Cancelación Moderada**: Si $24\text{ horas} \le \Delta T \le 72\text{ horas}$.
  - **Cancelación Tardía**: Si $\Delta T < 24\text{ horas}$.
- **FR-016**: El sistema DEBE actualizar el estado de la reserva a "Cancelada por Arrendatario" y registrar la clasificación obtenida.
- **FR-017**: El sistema DEBE notificar al Módulo 3 el identificador de la reserva, la marca temporal del evento y el **Tipo de Cancelación** determinado (Flexible, Moderada o Tardía), delegando en el Módulo 3 cualquier cálculo de retención, reembolso o penalidad económica.
- **FR-018**: El sistema DEBE notificar al Módulo 1 la liberación inmediata de la embarcación a estado "Disponible" tras procesar una cancelación.
- **FR-019**: El sistema DEBE permitir al Propietario solicitar la cancelación justificada de una reserva confirmada por motivos de fuerza mayor o mantenimiento antes del Check-in.
- **FR-020**: Al procesar una cancelación por Propietario, el sistema DEBE transicionar la reserva al estado "Cancelada por Propietario", clasificar el evento como "Cancelación por Anfitrión", notificar al Módulo 3 dicha tipificación, y notificar al Módulo 1 el estado correspondiente de la embarcación.

#### Gestión de Inasistencias (No-Show)
- **FR-021**: El sistema DEBE proveer al Propietario la función de "Marcar Inasistencia" (No-Show) para una reserva en estado "Confirmada".
- **FR-022**: El sistema DEBE bloquear la ejecución de "Marcar Inasistencia" hasta que hayan transcurrido al menos **30 minutos de tolerancia** exactos después de la hora pactada de inicio ($T \ge T_{inicio} + 30\text{ minutos}$) y no se haya registrado el Check-in.
- **FR-023**: Al confirmarse la inasistencia tras la ventana de tolerancia, el sistema DEBE actualizar el estado de la reserva a "Cancelada por Inasistencia (No-Show)".
- **FR-024**: El sistema DEBE notificar al Módulo 3 el evento de inasistencia tipificado como "No-Show" junto con la hora de marcación para su posterior procesamiento de liquidación.
- **FR-025**: El sistema DEBE notificar al Módulo 1 para restablecer la embarcación a estado "Disponible" una vez marcado el No-Show.

#### Intercambio de Información y Trazabilidad
- **FR-026**: El sistema DEBE registrar en una bitácora inmutable cada cambio de estado de la reserva, registrando: identificador de reserva, estado previo, nuevo estado, marca temporal precisa (timestamp ISO 8601), actor responsable y motivo/clasificación asociada.
- **FR-027**: El sistema DEBE exponer el estado actual y los datos de itinerario de cualquier reserva a petición del Módulo 3 y del Módulo 1.

---

### Key Entities *(include if feature involves data)*

- **Reserva (`Reservation`)**: Entidad principal que representa el contrato de alquiler temporal de un activo náutico entre un Arrendatario y un Propietario.
  - *Atributos clave*: Identificador único (`UUID`), Identificador del Arrendatario (`RenterID`), Identificador del Propietario (`OwnerID`), Identificador de la Embarcación (`BoatID`), Fecha y Hora de Inicio Pactada (`StartTime`), Fecha y Hora de Fin Pactada (`EndTime`), Número de Pasajeros (`PassengerCount`), Estado Actual de la Reserva (`Status`: *Pendiente de Pago, Confirmada, En Navegación, Completada, Expirada, Cancelada por Arrendatario, Cancelada por Propietario, Cancelada por Inasistencia*), Marca temporal de creación (`CreatedAt`), Marca temporal de última actualización (`UpdatedAt`).

- **Bloqueo Temporal / TTL (`TemporalLock`)**: Entidad que gestiona el periodo de retención y exclusividad del inventario durante el proceso de compra.
  - *Atributos clave*: Identificador de la reserva asociada (`ReservationID`), Identificador de la embarcación (`BoatID`), Marca temporal de inicio de bloqueo (`LockedAt`), Marca temporal de vencimiento (`ExpiresAt` = `LockedAt + 15 min`), Estado del temporizador (`Active`, `Expired`, `Released`, `Consumed`).

- **Evento de Cancelación (`CancellationEvent`)**: Entidad que registra formalmente la terminación anticipada de una reserva y su categorización temporal.
  - *Atributos clave*: Identificador del evento (`EventID`), Identificador de la reserva (`ReservationID`), Actor solicitante (`RequestedBy`: *Arrendatario, Propietario, Sistema*), Marca temporal de la solicitud (`CancellationTimestamp`), Horas de anticipación respecto al inicio (`AnticipationHours`), Tipo de Cancelación Determinado (`CancellationType`: *Cancelación Flexible, Cancelación Moderada, Cancelación Tardía, Cancelación por Anfitrión, No-Show*), Motivo justificado (`Reason`).

- **Registro de Inasistencia / No-Show (`NoShowRecord`)**: Entidad que formaliza el reporte de incumplimiento de asistencia en puerto.
  - *Atributos clave*: Identificador de reserva (`ReservationID`), Identificador del Propietario que reporta (`OwnerID`), Hora pactada de zarpe (`ScheduledStartTime`), Hora de reporte del evento (`ReportedAt`), Minutos de tolerancia transcurridos (`ToleranceMinutesElapsed` $\ge 30$).

- **Bitácora de Transiciones de Estado (`ReservationStateLog`)**: Registro auditable de trazabilidad cronológica de la reserva.
  - *Atributos clave*: Identificador de log (`LogID`), Identificador de reserva (`ReservationID`), Estado anterior (`PreviousState`), Nuevo estado (`NewState`), Marca de tiempo (`Timestamp`), Actor o disparador (`TriggeredBy`).

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de las reservas en estado "Pendiente de Pago" que no reciban confirmación de pago transcurridos 15 minutos son liberadas y puestas en estado "Expirada" en menos de 5 segundos tras el vencimiento del TTL.
- **SC-002**: El 100% de las solicitudes de cancelación son clasificadas con precisión temporal (Flexible $>72\text{h}$, Moderada $24\text{h}-72\text{h}$, Tardía $<24\text{h}$) y notificadas al Módulo 3 en menos de 2 segundos desde la confirmación de la solicitud.
- **SC-003**: Cero (0%) sobreventas o solapamiento de reservas en una misma embarcación durante el mismo periodo horario garantizado por el bloqueo temporal atómico del Módulo 2.
- **SC-004**: El sistema bloquea el 100% de los intentos de marcación de inasistencia (No-Show) cuando el tiempo transcurrido desde la hora de inicio es inferior a los 30 minutos de tolerancia estipulados.
- **SC-005**: El 99.9% de los eventos de cambio de estado operativo ("Reservado", "Disponible", "En Navegación") se sincronizan exitosamente con el Módulo 1 en un tiempo inferior a 1 segundo.
- **SC-006**: El 100% de las transiciones de estado y clasificaciones de cancelación quedan auditadas en la bitácora inmutable con marca de tiempo UTC/ISO 8601.
- **SC-007**: El 95% de los Arrendatarios y Propietarios completan sus acciones de reserva, check-in o cancelación en su primer intento sin reportar errores de bloqueo de interfaz o ambigüedad en el estado de la reserva.
