# Feature Specification: Marcar Inasistencia

**Módulo**: Módulo 2 – Gestión de Reserva  
**Creado**: 2026-09-05  
**Actor primario**: Propietario (Anfitrión de la embarcación)  
**Dependencias externas (APIs)**:

- Módulo 1 – Gestión de Embarcación (liberación del activo a estado "Disponible", gestionada a través de "Actualizar estado reserva").
- Módulo 3 – Liquidación, Seguros y Dispersión de Fondos (comunicación de la clasificación "No-Show" para que Módulo 3 ejecute la liquidación de la compensación del 100% al anfitrión según las reglas financieras del proyecto).

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - [El Propietario reporta inasistencia del Arrendatario tras la ventana de tolerancia] (Priority: P1)

Habiendo transcurrido los 30 minutos de tolerancia posteriores a la hora pactada de inicio del servicio sin que el Arrendatario se haya presentado en el muelle de embarque, el Propietario registra la inasistencia (No-Show). El sistema valida el cumplimiento estricto del tiempo, transiciona la reserva al estado terminal "No-Show", libera la embarcación en Módulo 1 para que vuelva a estar disponible y notifica la clasificación a Módulo 3 para que aplique la compensación correspondiente al anfitrión.

**Why this priority**: Protege el tiempo y la operación comercial del Propietario, permitiéndole liberar legal y operativamente su embarcación en el puerto y asegurar su derecho a la compensación estipulada en las políticas del marketplace.

**Independent Test**: Se puede probar preparando una reserva en estado "Confirmada" con fecha/hora de inicio ya superada por al menos 30 minutos, ejecutando la solicitud de marcar inasistencia por parte del Propietario autenticado, y verificando que el sistema actualiza el estado a "No-Show" mediante "Actualizar estado reserva", libera el activo en Módulo 1 y notifica el evento a Módulo 3 sin calcular valores monetarios.

**Acceptance Scenarios**:

1. **Scenario**: Inasistencia registrada válidamente cumplidos los 30 minutos de tolerancia
   - **Given** una reserva en estado "Confirmada" cuya fecha/hora pactada de zarpe fue hace 31 minutos o más
   - **When** el Propietario de la embarcación solicita marcar inasistencia
   - **Then** el sistema valida que transcurrió la ventana de tolerancia de 30 minutos, invoca "Actualizar estado reserva" para fijar el estado en "No-Show", registra el evento de inasistencia, notifica a Módulo 3 la clasificación "No-Show" para la compensación del 100% y notifica a Módulo 1 la liberación de la embarcación a estado "Disponible"

2. **Scenario**: Inasistencia registrada en el límite exacto de la tolerancia
   - **Given** una reserva en estado "Confirmada" cuya fecha/hora de zarpe fue hace exactamente 30 minutos y 0 segundos
   - **When** el Propietario solicita marcar inasistencia
   - **Then** el sistema admite la solicitud por haberse alcanzado el umbral reglamentario de tolerancia y procesa el No-Show

---

### User Story 2 - [Rechazar reporte de inasistencia anticipado dentro de la ventana de tolerancia] (Priority: P2)

Si el Propietario intenta reportar inasistencia antes de que transcurran los 30 minutos de cortesía desde la hora pactada de inicio, el sistema rechaza la solicitud de manera inmediata, informando el tiempo restante que debe esperar para garantizar el derecho de presentación del Arrendatario.

**Why this priority**: Garantiza la equidad contractual protegiendo al Arrendatario de cancelaciones arbitrarias durante el periodo de tolerancia oficial establecido por la plataforma.

**Independent Test**: Se puede probar intentando marcar inasistencia sobre una reserva confirmada a los 10 o 25 minutos posteriores a la hora pactada de inicio, verificando que el sistema rechaza la operación, no cambia el estado y muestra al usuario los minutos restantes para habilitar la opción.

**Acceptance Scenarios**:

1. **Scenario**: Intento de marcar inasistencia antes de los 30 minutos
   - **Given** una reserva en estado "Confirmada" cuya hora pactada de inicio transcurrió hace solo 15 minutos
   - **When** el Propietario intenta marcar inasistencia
   - **Then** el sistema rechaza la solicitud, informa que la ventana de tolerancia de 30 minutos sigue activa e indica que restan 15 minutos para poder reportar el No-Show, sin modificar el estado de la reserva ni notificar a Módulo 3

2. **Scenario**: Intento de marcar inasistencia previo a la hora de inicio de la reserva
   - **Given** una reserva en estado "Confirmada" cuya hora de inicio pactada aún no ha llegado (está en el futuro)
   - **When** el Propietario intenta marcar inasistencia
   - **Then** el sistema bloquea y rechaza la acción indicando que el servicio aún no ha comenzado

---

### User Story 3 - [Rechazar reporte de inasistencia sobre reservas en estados incompatibles] (Priority: P2)

Si el Propietario intenta marcar inasistencia sobre una reserva que ya inició navegación, fue cancelada previamente o ya culminó, el sistema debe denegar la operación sin alterar el registro.

**Why this priority**: Evita inconsistencias de dominio y fraudes operativos, asegurando que un servicio en curso o cancelado no pueda ser alterado retroactivamente como No-Show.

**Independent Test**: Se puede probar ejecutando la acción sobre reservas en estados "En Navegación", "Cancelada por Arrendatario", "Finalizada" y "Expirada", comprobando que en el 100% de los casos la acción es rechazada.

**Acceptance Scenarios**:

1. **Scenario**: Intento de marcar inasistencia cuando el servicio ya inició navegación
   - **Given** una reserva en estado "En Navegación" (el check-in ya fue efectuado)
   - **When** el Propietario intenta marcar inasistencia
   - **Then** el sistema rechaza la operación informando que el servicio ya fue iniciado

2. **Scenario**: Intento de marcar inasistencia en reserva ya cancelada
   - **Given** una reserva que fue previamente transicionada a "Cancelada por Arrendatario"
   - **When** el Propietario intenta marcar inasistencia
   - **Then** el sistema rechaza la solicitud informando que la reserva ya fue cancelada con anterioridad

---

### Edge Cases

- **Exclusión mutua entre "Marcar inasistencia" y "Marcar inicio de la navegación"**:
  - Ambas acciones compiten una vez transcurridos los 30 minutos (el cliente puede llegar al minuto 35 y el anfitrión optar por iniciar el viaje en lugar de penalizarlo).
  - Si el Propietario pulsa "Marcar inicio de la navegación", la reserva pasa a "En Navegación" y la opción de "Marcar inasistencia" queda inhabilitada de forma irreversible.
  - Si el Propietario pulsa "Marcar inasistencia", la reserva pasa a "No-Show" y queda inhabilitada de forma irreversible para iniciar navegación.
- **Validación del huso horario del puerto**:
  - La fecha/hora actual del sistema debe cotejarse contra la zona horaria oficial del puerto de atraque de la embarcación (GPS provisto por Módulo 1), evitando desfasajes si el Propietario o los servidores operan en husos horarios diferentes.
- **Acción manual vs. automatización de No-Show**:
  - En el diagrama de casos de uso, "Marcar inasistencia" está vinculado al actor humano `Propietario`. ¿Debe existir un proceso automático de fondo que declare No-Show si transcurre un plazo excesivo (p. ej. 3 horas) sin reporte del Propietario ni inicio de navegación? [NEEDS CLARIFICATION: ¿el No-Show es estrictamente manual a criterio del Propietario o existe un cierre automático por inactividad prolongada?]
- **Evidencia o justificación de inasistencia**:
  - ¿Debe el Propietario registrar notas obligatorias, fotografías del muelle o confirmación bajo declaración jurada al marcar inasistencia? [NEEDS CLARIFICATION: ¿se exigen campos de justificación o basta con la confirmación de la acción en la interfaz?]
- **Autorización de actor**:
  - Únicamente el Propietario legítimo de la embarcación asociada a la reserva tiene autorización para ejecutar esta acción; cualquier intento de otro usuario debe ser rechazado como no autorizado.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE permitir al Propietario marcar la inasistencia (No-Show) de una reserva si y solo si la reserva existe y se encuentra en estado "Confirmada".
- **FR-002**: El sistema DEBE validar que el usuario que ejecuta la acción sea el Propietario registrado de la embarcación asociada a la reserva.
- **FR-003**: El sistema DEBE validar que hayan transcurrido al menos treinta (30) minutos continuos desde la fecha y hora pactada de inicio de la reserva (`timestamp_actual >= fecha_hora_inicio + 30 minutos`).
- **FR-004**: La validación temporal de la ventana de tolerancia DEBE calcularse tomando como referencia el huso horario oficial del puerto de atraque de la embarcación.
- **FR-005**: Si no han transcurrido los 30 minutos de tolerancia, el sistema DEBE rechazar la solicitud, indicando el tiempo remanente antes de poder marcar la inasistencia, y NO DEBE realizar ninguna transición ni notificación externa.
- **FR-006**: Si la reserva se encuentra en cualquier estado distinto a "Confirmada" (p. ej. "En Navegación", "Cancelada por Arrendatario", "Finalizada", "Expirada"), el sistema DEBE denegar la solicitud informando el estado incompatible.
- **FR-007**: Al validar satisfactoriamente el reporte de inasistencia, el sistema DEBE invocar el caso de uso "Actualizar estado reserva" (vía `<<include>>`) para transicionar el estado de la reserva a "No-Show".
- **FR-008**: El sistema DEBE registrar un registro auditable del evento de inasistencia, capturando el identificador de la reserva, el identificador del Propietario, la marca temporal exacta del reporte, los minutos de tolerancia transcurridos y notas explicativas si aplican [NEEDS CLARIFICATION].
- **FR-009**: El sistema DEBE notificar a Módulo 3 la clasificación del evento como "No-Show" para que Módulo 3 gestione la liquidación de la compensación del 100% al anfitrión estipulada en la política del proyecto.
- **FR-010**: El sistema **NO DEBE calcular penalidades, comisiones, reembolsos ni ejecutar transferencias o dispersiones de dinero**; su alcance se limita a validar la regla temporal de tolerancia, registrar el estado y reportar la clasificación "No-Show" a Módulo 3.
- **FR-011**: Al completarse la transición a "No-Show", el sistema DEBE garantizar que la embarcación quede liberada a estado operativo "Disponible" en Módulo 1 (a través de los efectos colaterales de "Actualizar estado reserva").

### Key Entities

- **Reserva (`Reservation`)**: Entidad de Módulo 2 que transiciona a estado "No-Show".
- **Registro de Inasistencia (`NoShowEvent`)**: Registro de auditoría del dominio. Atributos clave: id_evento, reserva_id, propietario_id, timestamp_reporte, fecha_hora_pactada_inicio, minutos_tolerancia_observados, comentarios_anfitrión.
- **Embarcación**: Activo náutico cuya propiedad y estado operativo son administrados en Módulo 1.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Cero (0%) reportes de inasistencia procesados antes de cumplirse los 30 minutos reglamentarios de tolerancia posteriores a la hora pactada de inicio.
- **SC-002**: Cero (0%) reportes de inasistencia admitidos sobre reservas que ya hayan iniciado navegación ("En Navegación") o que estén canceladas o finalizadas.
- **SC-003**: El 100% de los reportes válidos de inasistencia transicionan la reserva a "No-Show" y notifican a Módulo 1 y Módulo 3 en menos de 2 segundos.
- **SC-004**: Cero (0%) operaciones de cálculo de dinero, montos de penalidad o transferencias financieras generadas dentro del flujo de Módulo 2.
- **SC-005**: El 100% de los intentos prematuros de reporte presentan un mensaje claro que informa con exactitud cuántos minutos restan para que expire la ventana de tolerancia.
