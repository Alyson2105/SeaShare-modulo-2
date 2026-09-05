# Feature Specification: Actualizar Estado de Reserva

**Módulo**: Módulo 2 – Gestión de Reserva  
**Creado**: 2026-09-05  
**Actor primario / Disparador**: Invocación interna desde casos de uso de Módulo 2 (`Solicitar cancelación`, `Confirmar pago`, `Marcar inasistencia`, `Marcar inicio de la navegación`, `Marcar fin de navegacion`, temporizador de expiración TTL)  
**Dependencias externas (APIs)**:

- Módulo 1 – Gestión de Embarcación (`Asignar estado operativo`: API externa de escritura para sincronizar el estado físico/operativo de la embarcación en el inventario náutico).
- Módulo 3 – Liquidación, Seguros y Dispersión de Fondos (`Recibir el estado de la reserva`: API externa hacia la cual Módulo 2 empuja el estado actualizado de la reserva en cada transición para gobernar la custodia de fondos, seguros y dispersión).

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - [Ejecutar transiciones válidas en el ciclo de vida de la reserva] (Priority: P1)

Cualquier caso de uso del ciclo de vida de Módulo 2 que requiera cambiar el estado de una reserva invoca a "Actualizar estado reserva". El sistema valida que la transición solicitada cumpla con la máquina de estados formal, actualiza el estado en persistencia, registra la marca de tiempo del cambio y el origen de la transición, y asegura la integridad del registro.

**Why this priority**: Es el núcleo central de consistencia del dominio de reservas. Centralizar las transiciones en un único caso de uso previene inconsistencias de estado, carreras concurrentes y estados corruptos en la plataforma.

**Independent Test**: Se puede probar preparando reservas en cada uno de los estados posibles y solicitando transiciones válidas (p. ej. de "Pendiente de Pago" a "Confirmada", de "Confirmada" a "En Navegación", de "En Navegación" a "Finalizada"), verificando que el nuevo estado se persiste de forma atómica junto con su marca temporal.

**Acceptance Scenarios**:

1. **Scenario**: Transición estándar de reserva pendiente a confirmada
   - **Given** una reserva en estado "Pendiente de Pago"
   - **When** "Confirmar pago" invoca la actualización a estado "Confirmada"
   - **Then** el sistema valida que la transición es admisible, actualiza el estado de la reserva a "Confirmada", almacena la marca temporal y dispara las sincronizaciones con Módulo 1 y Módulo 3

2. **Scenario**: Transición por expiración automática de TTL
   - **Given** una reserva en estado "Pendiente de Pago" cuyo temporizador de 15 minutos ha vencido sin pago confirmado
   - **When** el temporizador interno dispara la actualización de estado
   - **Then** el sistema valida y actualiza el estado de la reserva a "Expirada" y dispara la liberación del activo en Módulo 1

3. **Scenario**: Transición a estados terminales por cancelación o inasistencia
   - **Given** una reserva en estado "Confirmada"
   - **When** se solicita transición a "Cancelada por Arrendatario", "Cancelada por Propietario" o "No-Show"
   - **Then** el sistema actualiza el estado de la reserva al estado terminal solicitado y registra el actor y motivo causante

---

### User Story 2 - [Sincronizar el estado operativo de la embarcación en Módulo 1] (Priority: P1)

Cada vez que una reserva cambia de estado, el sistema debe mapear y actualizar inmediatamente el estado operativo de la embarcación física en Módulo 1 mediante la API `Asignar estado operativo`, garantizando que la disponibilidad real del inventario se refleje en toda la plataforma.

**Why this priority**: Si el estado operativo de la embarcación no se actualiza en Módulo 1, el activo podría figurar como "Disponible" mientras está en navegación o quedar bloqueado como "Reservado" tras una cancelación o expiración, provocando sobreventas o pérdida de ingresos comerciales.

**Independent Test**: Se puede probar mockeando la API `Asignar estado operativo` de Módulo 1 y ejecutando transiciones de reserva hacia "Confirmada", "En Navegación", "Cancelada" y "Finalizada", validando que Módulo 1 recibe la llamada con los estados "Reservado", "En Navegación", "Disponible" y "Disponible", respectivamente.

**Acceptance Scenarios**:

1. **Scenario**: Confirmación de reserva bloquea embarcación como "Reservado"
   - **Given** una embarcación en estado operativo "Disponible" en Módulo 1
   - **When** la reserva asociada transiciona exitosamente a "Confirmada"
   - **Then** el sistema invoca la API `Asignar estado operativo` de Módulo 1 enviando el estado "Reservado" para esa embarcación

2. **Scenario**: Inicio de navegación actualiza embarcación a "En Navegación"
   - **Given** una reserva en estado "Confirmada" cuya embarcación está "Reservado" en Módulo 1
   - **When** la reserva transiciona a estado "En Navegación"
   - **Then** el sistema invoca la API `Asignar estado operativo` de Módulo 1 enviando el estado "En Navegación"

3. **Scenario**: Cancelación, expiración, No-Show o fin de navegación libera embarcación a "Disponible"
   - **Given** una reserva que transiciona a "Expirada", "Cancelada por Arrendatario", "No-Show" o "Finalizada"
   - **When** se ejecuta la actualización del estado de la reserva
   - **Then** el sistema invoca la API `Asignar estado operativo` de Módulo 1 asignando el estado "Disponible" (salvo indicación explícita de paso a mantenimiento)

---

### User Story 3 - [Notificar y empujar el nuevo estado de reserva a Módulo 3] (Priority: P1)

Cada cambio de estado en una reserva debe ser notificado de manera activa a la API externa de Módulo 3 (`Recibir el estado de la reserva`) para que el sistema financiero y de liquidación ajuste las garantías, active seguros, libere depósitos o procese las liquidaciones y dispersiones de fondos correspondientes.

**Why this priority**: Módulo 3 depende en tiempo real de los hitos operativos de la reserva para controlar la retención y dispersión de fondos. Sin esta notificación, el dinero quedaría retenido o dispersado fuera de tiempo.

**Independent Test**: Se puede probar mediante un mock de la API `Recibir el estado de la reserva` de Módulo 3, comprobando que cada transición de estado en Módulo 2 envía una carga útil que contiene el identificador de la reserva, el nuevo estado, la marca temporal del cambio y el contexto del evento.

**Acceptance Scenarios**:

1. **Scenario**: Notificación de reserva confirmada a Módulo 3
   - **Given** una reserva que pasa a estado "Confirmada"
   - **When** se completa la persistencia del estado en Módulo 2
   - **Then** el sistema invoca la API `Recibir el estado de la reserva` de Módulo 3 comunicando el estado "Confirmada" para activar la custodia de fondos y seguro náutico

2. **Scenario**: Notificación de fin de servicio náutico a Módulo 3
   - **Given** una reserva que transiciona a "Finalizada"
   - **When** se asienta la finalización en Módulo 2
   - **Then** el sistema notifica a Módulo 3 el estado "Finalizada" para que Módulo 3 inicie la matriz de liquidación, pago al anfitrión y liberación del depósito de garantía

---

### User Story 4 - [Rechazar transiciones de estado inválidas o extemporáneas] (Priority: P2)

Si un caso de uso o evento externo solicita una transición no permitida por la máquina de estados (por ejemplo, intentar pasar una reserva "Expirada" a "Confirmada", o una reserva "Finalizada" a "Cancelada"), el sistema rechaza la solicitud de manera estricta, protegiendo la inmutabilidad de los estados terminales.

**Why this priority**: Evita inconsistencias de datos y ataques lógicos o condiciones de carrera que puedan reactivar reservas finalizadas o cancelar servicios que ya concluyeron.

**Independent Test**: Se puede probar enviando solicitudes de transición ilegales (p. ej. Expirada → Confirmada, Finalizada → Cancelada, No-Show → En Navegación) y verificando que el sistema retorna un error de transición inválida y no modifica la base de datos ni invoca APIs externas.

**Acceptance Scenarios**:

1. **Scenario**: Intento de confirmación sobre reserva ya expirada
   - **Given** una reserva en estado "Expirada"
   - **When** se solicita la transición a estado "Confirmada"
   - **Then** el sistema rechaza la transición indicando que el estado actual "Expirada" es terminal y no admite confirmación

2. **Scenario**: Intento de cancelación sobre reserva ya finalizada
   - **Given** una reserva en estado "Finalizada"
   - **When** se solicita transición a estado "Cancelada"
   - **Then** el sistema rechaza la solicitud indicando que un servicio finalizado no es cancelable

---

### Edge Cases

- **Máquina de Estados Formal de la Reserva**:
  - Estados posibles: `Pendiente de Pago`, `Confirmada`, `En Navegación`, `Finalizada`, `Cancelada por Arrendatario`, `Cancelada por Propietario`, `No-Show`, `Expirada`, `Pago Fallido`.
  - Estados terminales (no admiten ninguna transición posterior): `Finalizada`, `Cancelada por Arrendatario`, `Cancelada por Propietario`, `No-Show`, `Expirada`, `Pago Fallido`.
- **Falla de comunicación con Módulo 1 (`Asignar estado operativo`)**:
  - Si el estado de la reserva se actualiza en Módulo 2 pero Módulo 1 no responde a la actualización del activo, ¿cómo se garantiza la sincronización?
  - *Regla*: el sistema DEBE garantizar consistencia eventual mediante reintentos automáticos con registro de eventos pendientes, evitando que la falla externa aborte la consistencia interna de la reserva [NEEDS CLARIFICATION: política de reintentos y alertas ante desincronización persistente con Módulo 1].
- **Falla de comunicación con Módulo 3 (`Recibir el estado de la reserva`)**:
  - Si la llamada a Módulo 3 falla durante una transición crítica (p. ej. "Finalizada" o "Cancelada"), el sistema DEBE encolar el evento y reintentar su entrega para asegurar que Módulo 3 reciba la señal de liquidación (0% eventos perdidos silenciosamente, ver SC-005 de `spec-solicitar-cancelacion.md`).
- **Condición de carrera entre dos transiciones casi simultáneas**:
  - Por ejemplo, cancelación por el Arrendatario y reporte de No-Show por el Propietario en el mismo segundo.
  - El sistema DEBE aplicar control de concurrencia optimista (versionado de la entidad Reserva) para asegurar que solo una transición gane y la siguiente sea evaluada contra el estado recién asentado.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE proveer un mecanismo centralizado para actualizar el estado de una reserva, invocado obligatoriamente por cualquier caso de uso que modifique el ciclo de vida de la reserva (`<<include>>`).
- **FR-002**: El sistema DEBE validar de forma estricta que la transición solicitada sea legal de acuerdo con la máquina de estados formal:
  - De `Pendiente de Pago` solo se puede transicionar a: `Confirmada`, `Expirada`, `Pago Fallido`, `Cancelada por Arrendatario`.
  - De `Confirmada` solo se puede transicionar a: `En Navegación`, `Cancelada por Arrendatario`, `Cancelada por Propietario`, `No-Show`.
  - De `En Navegación` solo se puede transicionar a: `Finalizada`.
  - Los estados `Finalizada`, `Cancelada por Arrendatario`, `Cancelada por Propietario`, `No-Show`, `Expirada` y `Pago Fallido` son terminales y NO DEBEN permitir transiciones ulteriores.
- **FR-003**: Si la transición solicitada no es legal, el sistema DEBE rechazar la actualización, generar un error descriptivo y abstenerse de alterar la reserva o invocar sistemas externos.
- **FR-004**: Al admitir una transición válida, el sistema DEBE persistir de forma atómica el nuevo estado en la entidad Reserva, registrando el timestamp exacto del cambio y el origen/motivo de la actualización.
- **FR-005**: El sistema DEBE invocar la API externa de Módulo 1 (`Asignar estado operativo`) con el identificador de la embarcación y el estado operativo correspondiente:
  - Para transición a `Confirmada`: asignar estado `Reservado`.
  - Para transición a `En Navegación`: asignar estado `En Navegación`.
  - Para transiciones a `Expirada`, `Cancelada por Arrendatario`, `No-Show` o `Finalizada`: asignar estado `Disponible` [NEEDS CLARIFICATION: si tras cancelación por propietario o avería reportada al fin de navegación se asigna `En Mantenimiento/Limpieza`].
- **FR-006**: El sistema DEBE invocar la API externa de Módulo 3 (`Recibir el estado de la reserva`) enviando el identificador de la reserva, el nuevo estado alcanzado, la fecha/hora de transición y el tipo o metadata del evento si aplica.
- **FR-007**: El sistema DEBE aplicar control de concurrencia atómico para garantizar que, ante múltiples solicitudes simultáneas de actualización sobre la misma reserva, solo una transición prevalezca y la otra sea rechazada o reevaluada.
- **FR-008**: Si la notificación a Módulo 1 o a Módulo 3 experimenta una falla transitoria de conectividad, el sistema DEBE registrar el evento para reintento garantizado, impidiendo que el evento de cambio de estado se pierda silenciosamente.
- **FR-009**: El sistema DEBE mantener un registro de auditoría con el histórico cronológico de todos los cambios de estado experimentados por cada reserva durante su ciclo de vida.
- **FR-010**: El sistema **NO DEBE en ningún momento calcular reembolsos, dispersiones, comisiones ni valores monetarios**; su responsabilidad se restringe al gobierno del estado y a la comunicación de los cambios a Módulo 1 y Módulo 3.

### Key Entities

- **Reserva (`Reservation`)**: Entidad que almacena el estado actual (`estado`), identificador único, versión de concurrencia y relaciones con Arrendatario y Embarcación.
- **Registro Histórico de Transición de Estado (`ReservationStatusAudit`)**: Registro de auditoría. Atributos: id_auditoría, reserva_id, estado_anterior, estado_nuevo, disparador/caso_de_uso_origen, actor_responsable, timestamp_transicion, motivo.
- **Embarcación**: Activo náutico gobernado físicamente por Módulo 1.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Cero (0%) transiciones de estado ilegales o no contempladas en la máquina de estados formal admitidas en la base de datos de Módulo 2.
- **SC-002**: El 100% de las transiciones de estado exitosas disparan la invocación hacia la API `Asignar estado operativo` de Módulo 1 y la API `Recibir el estado de la reserva` de Módulo 3 en menos de 500 milisegundos desde la persistencia de la reserva.
- **SC-003**: Cero (0%) discrepancias operativas donde una reserva esté confirmada o en navegación y la embarcación figure en un estado incompatible en Módulo 1.
- **SC-004**: El 100% de los eventos de cambio de estado son entregados y confirmados ante Módulo 3 (0% de eventos de estado perdidos).
- **SC-005**: El 100% de las transiciones de estado quedan documentadas en el registro de auditoría con fecha, hora, estado anterior, estado nuevo y caso de uso causante.
