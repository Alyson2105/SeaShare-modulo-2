# Feature Specification: Solicitar Cancelación

**Módulo**: Módulo 2 – Gestión de Reserva
**Creado**: 2026-09-04
**Actor primario**: Arrendatario y Propietario (ambos actúan sobre el mismo caso de uso en el diagrama; no son dos burbujas separadas)
**Precondición**: Debe existir una reserva en estado cancelable. NO existe restricción temporal de "recién creada" — la relación `<<extend>>` con "Iniciar reserva" fue descartada; este caso de uso es 100% independiente.
**Dependencias externas (APIs)**:

- Módulo 1 – Gestión de Embarcación: notificación de liberación del activo tras la cancelación (contrato de integración aparte).
- Módulo 3 – Gestión de Liquidación: consumo indirecto vía "Calcular tipo de cancelación" → (`Solicitar tipo de cancelación`, API externa de Módulo 3; no le corresponde spec propio, solo se referencia como dependencia en el FR).

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - [El Arrendatario solicita cancelación de su reserva] (Priority: P1)

El Arrendatario solicita cancelar una reserva en estado cancelable. El sistema clasifica la cancelación según la anticipación respecto a la hora pactada de zarpe (reglas 72h/24h del proyecto), notifica la clasificación a Módulo 3 para el cálculo de reembolsos/penalidades y libera la embarcación.

**Why this priority**: Es la vía de salida voluntaria del contrato por el lado del cliente y dispara toda la lógica de reembolsos que protege tanto al Arrendatario como al Propietario. Sin esto, no existe política de cancelación.

**Independent Test**: Se puede probar de forma aislada sobre reservas en estado "Confirmada" solicitando cancelación en tres momentos distintos respecto a la hora de inicio (más de 72h, entre 24h y 72h, y menos de 24h de anticipación) y verificando que el sistema clasifica, notifica a Módulo 3 el tipo determinado, transiciona el estado y libera la embarcación en Módulo 1.

**Acceptance Scenarios**:

1. **Scenario**: Cancelación Flexible (anticipación mayor a 72 horas)
   - **Given** una reserva en estado cancelable con una hora pactada de inicio conocida
   - **When** el Arrendatario solicita cancelarla con más de 72 horas de anticipación respecto a la hora pactada de inicio
   - **Then** el sistema clasifica la cancelación como "Cancelación Flexible", notifica la clasificación a Módulo 3 para que aplique el reembolso del 100% (regla del proyecto; el cálculo y los montos los ejecuta Módulo 3), transiciona la reserva a "Cancelada por Arrendatario" y notifica a Módulo 1 la liberación de la embarcación

2. **Scenario**: Cancelación Moderada (anticipación entre 24 y 72 horas)
   - **Given** una reserva en estado cancelable con una hora pactada de inicio conocida
   - **When** el Arrendatario solicita cancelarla con una anticipación de entre 24 y 72 horas respecto a la hora pactada de inicio (ambos límites incluidos en esta franja)
   - **Then** el sistema clasifica la cancelación como "Cancelación Moderada", notifica la clasificación a Módulo 3 para que aplique la penalidad del 50% (regla del proyecto; cálculo en Módulo 3), transiciona la reserva a "Cancelada por Arrendatario" y notifica a Módulo 1 la liberación de la embarcación

3. **Scenario**: Cancelación Tardía (anticipación menor a 24 horas)
   - **Given** una reserva en estado cancelable con una hora pactada de inicio conocida
   - **When** el Arrendatario solicita cancelarla con menos de 24 horas de anticipación respecto a la hora pactada de inicio
   - **Then** el sistema clasifica la cancelación como "Cancelación Tardía", notifica la clasificación a Módulo 3 para que cobre el 100% como compensación al anfitrión (regla del proyecto, sin reembolso), transiciona la reserva a "Cancelada por Arrendatario" y notifica a Módulo 1 la liberación de la embarcación

### User Story 2 - [El Propietario solicita cancelación de una reserva confirmada] (Priority: P1)

El Propietario solicita cancelar una reserva en estado cancelable (antes del check-in). El sistema transiciona la reserva, notifica a Módulo 3 la clasificación de "Cancelación por Anfitrión" para gestionar el reembolso al Arrendatario y libera o reasigna el activo en Módulo 1.

**Why this priority**: No es un "nice to have": si el anfitrión cancela, el Arrendatario depende del sistema para no perder su dinero y para ser reubicado/reembolsado de forma garantizada. Es la contraparte de protección al cliente de US1. Aunque la política exacta de reembolso aún está por definir, el flujo de notificación y cambio de estado debe existir en el MVP. [NEEDS CLARIFICATION: política de reembolso al Arrendatario cuando cancela el Propietario — propuesta: 100% de reembolso sin importar el tiempo restante; está pendiente de confirmación]

**Independent Test**: Se puede probar sobre una reserva en estado "Confirmada" previa al check-in solicitando la cancelación por el Propietario, y verificando que la reserva transiciona a "Cancelada por Propietario", que la notificación a Módulo 3 se envía tipificada como "Cancelación por Anfitrión" y que el activo se libera o pasa a mantenimiento según el flujo.

**Acceptance Scenarios**:

1. **Scenario**: Cancelación por Propietario antes del check-in
   - **Given** una reserva en estado cancelable previa a la realización del check-in
   - **When** el Propietario solicita cancelar la reserva
   - **Then** el sistema transiciona la reserva a "Cancelada por Propietario", notifica a Módulo 3 la clasificación "Cancelación por Anfitrión" para que determine y aplique el reembolso al Arrendatario, y notifica a Módulo 1 el estado que debe tomar la embarcación [NEEDS CLARIFICATION: ¿"Disponible" o "En Mantenimiento/Limpieza"? — depende del motivo de la cancelación]

---

### Edge Cases

- **Límites temporales exactos (72 horas y 24 horas)**: si la anticipación es de exactamente 72 horas, se clasifica como **Moderada** (Flexible exige estrictamente más de 72 horas). Si la anticipación es de exactamente 24 horas, también es **Moderada** (el rango 24h-72h incluye ambos límites). Si la anticipación es menor a 24 horas, es **Tardía** de forma estricta.
- **No-Show = Tardía**: la regla del proyecto agrupa "Tardía/No-Show <24h" con 0% de reembolso. En nuestro diagrama, el No-Show es un caso de uso distinto ("Marcar inasistencia"); aquí solo aplica la franja Tardía para cancelaciones voluntarias. La equivalencia de liquidación la resuelve Módulo 3.
- **Cancelar una reserva que ya no es cancelable** (En Navegación, Completada, Expirada o ya Cancelada): el sistema DEBE rechazar la solicitud con el motivo correspondiente y NO notificar ni transicionar nada. [NEEDS CLARIFICATION: conjunto exacto de estados cancelables — ¿"Pendiente de Pago" (TTL activo) es cancelable? Propuesta: sí; cancela el flujo de pago, libera el activo y notifica a Módulo 3 para evitar el cobro]
- **Doble cancelación simultánea**: dos actores intentan cancelar la misma reserva al mismo tiempo; el sistema DEBE garantizar que solo una transición de estado se aplique (la segunda recibe "reserva ya no está cancelable"), quedando la reserva con un único estado final de cancelación.
- **Cancelación vs. confirmación de pago concurrente**: si una cancelación del Arrendatario llega mientras Módulo 3 confirma el pago (dentro del TTL), el sistema DEBE resolver el conflicto de forma que solo uno de los dos eventos determine el resultado final; el otro evento se rechaza con la notificación correspondiente a Módulo 3 [NEEDS CLARIFICATION: cuál de los dos eventos debe prevalecer].
- **Cancelación posterior al check-in / durante navegación**: se rechaza siempre; el servicio ya está en curso.
- **Motivo de cancelación del Propietario** (fuerza mayor, avería, mantenimiento): se debe capturar para la notificación a Módulo 3, pero cualquier penalidad o consecuencia reputacional/operativa para el Propietario está **fuera del alcance de Módulo 2** (se documenta como nota; no se modela aquí).

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE permitir solicitar la cancelación de una reserva si y solo si la reserva existe y se encuentra en un estado cancelable. [NEEDS CLARIFICATION: conjunto exacto de estados cancelables, incluido si "Pendiente de Pago" es cancelable]
- **FR-002**: Si la reserva NO está en estado cancelable, el sistema DEBE rechazar la solicitud informando el motivo (p. ej. "el servicio ya está en curso", "la reserva ya fue cancelada o expirada") y NO DEBE notificar a Módulo 1 ni a Módulo 3.
- **FR-003**: El sistema DEBE identificar y registrar quién inicia la cancelación (actor solicitante: Arrendatario o Propietario).
- **FR-004**: El sistema DEBE invocar el caso de uso "Calcular tipo de cancelación" (delegación obligatoria, `<<include>>` según el diagrama; el detalle interno de su clasificación se especifica en su propio spec) pasándole como entrada explícita: identificador de la reserva, **actor solicitante (Arrendatario o Propietario)** y hora pactada de inicio. El tipo resultante depende de la anticipación Y de quién inicia la cancelación, no solo del tiempo restante.
- **FR-005**: Cuando el actor solicitante es el **Arrendatario**, el sistema DEBE clasificar la cancelación de forma unívoca según las reglas del proyecto: **Flexible** si la anticipación es mayor a 72 horas; **Moderada** si la anticipación está entre 24 y 72 horas (ambos límites incluidos); **Tardía** si la anticipación es menor a 24 horas. Los montos (100% reembolso / 50% penalidad / 0% reembolso) los aplica y calcula Módulo 3.
- **FR-006**: Cuando el actor solicitante es el **Propietario**, el sistema DEBE clasificar la notificación hacia Módulo 3 como "Cancelación por Anfitrión" y NO DEBE aplicar las franjas 72h/24h del Arrendatario. [NEEDS CLARIFICATION: política de reembolso al Arrendatario cuando cancela el Propietario — propuesta: 100% sin importar el tiempo restante]
- **FR-007**: El sistema DEBE notificar a Módulo 3, para cada cancelación procesada, los datos necesarios para su liquidación (identificador de la reserva, marca temporal del evento, actor solicitante y clasificación/tipo de cancelación determinada). La matriz de liquidación (comisión, seguro, depósito, penalidades) pertenece a Módulo 3; Módulo 2 solo asegura el envío de estos datos. [NEEDS CLARIFICATION: formato/payload exacto del contrato de integración con Módulo 3 — se documentará en el contrato de integración aparte, no en este spec]
- **FR-008**: Al procesar una cancelación, el sistema DEBE transicionar la reserva según el actor: "Cancelada por Arrendatario" o "Cancelada por Propietario". [NEEDS CLARIFICATION: nombres exactos y máquina de estados completa de una reserva]
- **FR-009**: El sistema DEBE notificar a Módulo 1 la liberación de la embarcación tras la cancelación, con el estado operativo resultante [NEEDS CLARIFICATION: en cancelación por Arrendatario se libera a "Disponible"; en cancelación por Propietario, ¿"Disponible" o "En Mantenimiento/Limpieza"?].
- **FR-010**: Consecuencias adicionales para el Propietario (reputación, restricción de publicación, retenciones) NO son responsabilidad de Módulo 2. Si existieran, se gestionan en otro módulo/contexto; Módulo 2 solo registra el evento.
- **FR-011**: El sistema DEBE garantizar que Módulo 1 y Módulo 3 se enteren de cada cancelación procesada, de forma que ningún evento se pierda silenciosamente. [NEEDS CLARIFICATION: el mecanismo concreto para lograr esta garantía —reintentos, colas, confirmaciones— es una decisión de diseño técnico que se define en la fase de arquitectura, no en este spec]

### Key Entities

- **Reserva** *(entidad de Módulo 2, referenciada)*: caso de uso la lee para validar el estado cancelable y la transiciona al estado de cancelación resultante. No crea reservas.
- **Evento de Cancelación**: registro de dominio que documenta la terminación anticipada de una reserva. Atributos clave: identificador del evento, identificador de la reserva, actor solicitante (Arrendatario / Propietario), marca temporal de la solicitud, horas de anticipación respecto al inicio, clasificación determinada (Flexible / Moderada / Tardía / Cancelación por Anfitrión), motivo justificado (aplica para Propietario).
- **Embarcación** *(entidad externa, propiedad de Módulo 1)*: Módulo 2 solo la referencia por id y notifica el estado operativo resultante tras la cancelación (liberación).

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de las cancelaciones solicitadas por el Arrendatario son clasificadas con precisión temporal (Flexible >72h, Moderada 24h-72h, Tardía <24h) y notificadas a Módulo 3 con el tipo correcto en menos de 2 segundos desde la confirmación de la solicitud.
- **SC-002**: Cero (0%) cancelaciones procesadas sobre reservas en un estado no cancelable.
- **SC-003**: El 100% de las cancelaciones de Propietario se notifican a Módulo 3 como "Cancelación por Anfitrión", garantizando que el Arrendatario reciba su reembolso (el monto exacto depende de la política pendiente de clarificación).
- **SC-004**: El 100% de las cancelaciones procesadas resultan en la liberación/actualización del estado operativo en Módulo 1 (sin embarcaciones que queden "Reservadas" tras una cancelación).
- **SC-005**: El 100% de los eventos de cancelación llegan a confirmarse ante Módulo 1 y Módulo 3 (0% de eventos perdidos silenciosamente).
