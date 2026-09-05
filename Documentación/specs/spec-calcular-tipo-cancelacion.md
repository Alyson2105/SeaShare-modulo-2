# Feature Specification: Calcular Tipo de Cancelación

**Módulo**: Módulo 2 – Gestión de Reserva  
**Creado**: 2026-09-04  
**Actor primario**: Ninguno (caso de uso interno, invocado siempre como `<<include>>` por "Solicitar cancelación"; nunca lo inicia un usuario directamente)  
**Precondición**: "Solicitar cancelación" ya validó que la reserva existe y está en estado cancelable, e identificó al actor solicitante (Arrendatario o Propietario). Este caso de uso recibe esa información como entrada y devuelve una clasificación; no realiza ninguna transición de estado ni notificación.  
**Dependencias externas (APIs)**:

- Módulo 3 – Gestión de Liquidación: `Solicitar tipo de cancelación` (API externa de Módulo 3; no lleva spec propio de Módulo 2 — se referencia como dependencia en el FR). Módulo 2 envía la clasificación determinada; Módulo 3 la usa internamente para sus cálculos de reembolso/penalidad. Módulo 2 nunca recibe ni maneja montos económicos.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - [Clasificar cancelación del Arrendatario según anticipación] (Priority: P1)

El sistema recibe como entrada el identificador de la reserva, el actor solicitante (Arrendatario) y la hora pactada de inicio. Con esa información, calcula cuánto tiempo resta hasta el inicio del servicio y determina la clasificación correspondiente: Flexible (más de 72 horas de anticipación), Moderada (entre 24 y 72 horas de anticipación) o Tardía (menos de 24 horas de anticipación). Devuelve esa clasificación a "Solicitar cancelación" para que continúe el flujo.

**Why this priority**: Es la lógica central de todo el flujo de cancelación del Arrendatario. Sin esta clasificación, "Solicitar cancelación" no tiene qué comunicarle a Módulo 3, y la política de reembolsos del proyecto no puede aplicarse.

**Independent Test**: Se puede probar de forma aislada invocando este caso de uso directamente con tres conjuntos de entrada distintos (anticipación mayor a 72h, anticipación entre 24h y 72h, anticipación menor a 24h) y verificando en cada caso que la clasificación devuelta es la correcta, sin necesidad de que exista un flujo de reserva activo.

**Acceptance Scenarios**:

1. **Scenario**: Cancelación con más de 72 horas de anticipación → Flexible
   - **Given** la hora pactada de inicio de la reserva es conocida, el actor solicitante es el Arrendatario, y la solicitud de cancelación se realiza con más de 72 horas de anticipación respecto a esa hora de inicio
   - **When** el sistema evalúa la anticipación
   - **Then** el sistema determina la clasificación "Flexible" y la devuelve como resultado a "Solicitar cancelación"

2. **Scenario**: Cancelación entre 24 y 72 horas de anticipación → Moderada
   - **Given** la hora pactada de inicio de la reserva es conocida, el actor solicitante es el Arrendatario, y la solicitud de cancelación se realiza con entre 24 y 72 horas de anticipación respecto a esa hora de inicio
   - **When** el sistema evalúa la anticipación
   - **Then** el sistema determina la clasificación "Moderada" y la devuelve como resultado a "Solicitar cancelación"

3. **Scenario**: Cancelación con menos de 24 horas de anticipación → Tardía
   - **Given** la hora pactada de inicio de la reserva es conocida, el actor solicitante es el Arrendatario, y la solicitud de cancelación se realiza con menos de 24 horas de anticipación respecto a esa hora de inicio
   - **When** el sistema evalúa la anticipación
   - **Then** el sistema determina la clasificación "Tardía" y la devuelve como resultado a "Solicitar cancelación"

---

### User Story 2 - [Clasificar cancelación del Propietario como "Cancelación por Anfitrión"] (Priority: P1)

El sistema recibe como entrada el identificador de la reserva y el actor solicitante (Propietario). Sin evaluar el tiempo de anticipación, determina directamente la clasificación "Cancelación por Anfitrión" y la devuelve a "Solicitar cancelación".

**Why this priority**: El Propietario tiene una ruta de clasificación completamente distinta a la del Arrendatario: no aplican las franjas horarias. Si el sistema aplica por error la lógica de 72h/24h a una cancelación del Propietario, Módulo 3 calcularía erróneamente el reembolso al Arrendatario. Tiene la misma prioridad que US1 porque ambas rutas deben existir en el MVP. [NEEDS CLARIFICATION: política exacta de reembolso al Arrendatario cuando el Propietario cancela — propuesta: 100% de reembolso sin importar el tiempo restante; pendiente de confirmación con Módulo 3]

**Independent Test**: Se puede probar de forma aislada invocando este caso de uso con actor solicitante = Propietario (independientemente del tiempo de anticipación) y verificando que la clasificación devuelta siempre es "Cancelación por Anfitrión", sin importar cuánto falta para el inicio.

**Acceptance Scenarios**:

1. **Scenario**: Cancelación por Propietario → siempre "Cancelación por Anfitrión"
   - **Given** el actor solicitante identificado es el Propietario y la reserva se encuentra en estado cancelable
   - **When** el sistema evalúa quién solicita la cancelación
   - **Then** el sistema determina la clasificación "Cancelación por Anfitrión" sin evaluar el tiempo de anticipación, y la devuelve como resultado a "Solicitar cancelación"

---

### Edge Cases

- **Hora de inicio de la reserva no disponible o inválida**: si el sistema no puede determinar la hora pactada de inicio (dato ausente o inconsistente), no puede calcular la anticipación. El sistema DEBE devolver un error descriptivo a "Solicitar cancelación" para que rechace el flujo, en lugar de asumir una clasificación por defecto. [NEEDS CLARIFICATION: ¿qué estado de la reserva garantiza que la hora de inicio esté siempre presente? Resolver cuando se especifique la máquina de estados completa]
- **Anticipación exactamente en el límite (72h o 24h en punto)**: el sistema DEBE resolver sin ambigüedad en cuál franja cae cada límite exacto. [NEEDS CLARIFICATION: ¿72h exactas se clasifica como Flexible o Moderada? ¿24h exactas como Moderada o Tardía? Propuesta: los límites son inclusivos en la franja más favorable al Arrendatario, es decir, exactamente 72h → Flexible; exactamente 24h → Moderada]
- **Solicitud de clasificación con actor solicitante no reconocido**: si el valor del actor solicitante no es ni Arrendatario ni Propietario, el sistema DEBE devolver un error a "Solicitar cancelación" y no emitir ninguna clasificación.
- **Falla en la comunicación con Módulo 3** (`Solicitar tipo de cancelación`): si la API de Módulo 3 no responde o devuelve un error, el sistema DEBE informar la falla a "Solicitar cancelación" para que detenga el flujo, sin quedar ningún evento de cancelación en estado inconsistente. [NEEDS CLARIFICATION: SLA de respuesta esperado de Módulo 3 y política de reintentos — el mecanismo concreto es una decisión de diseño técnico que se define en la fase de arquitectura]

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE recibir como entradas obligatorias para ejecutar este caso de uso: identificador de la reserva, actor solicitante (Arrendatario o Propietario) y hora pactada de inicio de la reserva. Si alguna de estas entradas está ausente o es inválida, DEBE devolver un error descriptivo a "Solicitar cancelación" y no emitir clasificación alguna.
- **FR-002**: Si el actor solicitante es el Arrendatario, el sistema DEBE calcular el tiempo de anticipación como la diferencia entre el momento de la solicitud de cancelación y la hora pactada de inicio de la reserva.
- **FR-003**: Con base en el tiempo de anticipación calculado (FR-002), el sistema DEBE determinar la clasificación aplicando las siguientes reglas, en este orden de evaluación:
  - Más de 72 horas de anticipación → **Flexible**
  - Entre 24 y 72 horas de anticipación (inclusive) → **Moderada** [NEEDS CLARIFICATION: tratamiento exacto de los límites; ver edge case correspondiente]
  - Menos de 24 horas de anticipación → **Tardía**
- **FR-004**: Si el actor solicitante es el Propietario, el sistema DEBE asignar directamente la clasificación **Cancelación por Anfitrión**, sin evaluar el tiempo de anticipación.
- **FR-005**: El sistema DEBE invocar la API externa de Módulo 3 (`Solicitar tipo de cancelación`), transmitiéndole la clasificación determinada. Módulo 3 usará esa clasificación para sus cálculos internos de reembolso o penalidad; Módulo 2 no recibe ni procesa montos económicos como resultado de esta invocación. [NEEDS CLARIFICATION: formato/payload exacto del contrato de integración con Módulo 3 — se documentará en el contrato de integración aparte, no en este spec]
- **FR-006**: El sistema DEBE devolver la clasificación determinada como resultado a "Solicitar cancelación" para que continúe el flujo (transición de estado, notificación a Módulo 1, etc.). Este caso de uso no realiza ninguna transición de estado ni notificación propia.
- **FR-007**: Si la API de Módulo 3 falla o no responde, el sistema DEBE propagar el error a "Solicitar cancelación" de forma que el flujo completo se detenga y no quede ningún evento de cancelación en estado inconsistente. [NEEDS CLARIFICATION: SLA de Módulo 3 y mecanismo de garantía de entrega — decisión de diseño técnico, no se modela aquí]

### Key Entities

- **Reserva** *(entidad de Módulo 2, referenciada)*: este caso de uso la lee para obtener la hora pactada de inicio. No la modifica ni transiciona; eso lo hace "Solicitar cancelación".
- **Clasificación de Cancelación**: resultado de dominio que produce este caso de uso. Valores posibles: Flexible, Moderada, Tardía, Cancelación por Anfitrión. No es una entidad persistida por este caso de uso; es el valor que se comunica a Módulo 3 y se devuelve a "Solicitar cancelación".

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de las solicitudes de cancelación del Arrendatario reciben una clasificación correcta según la franja de anticipación correspondiente (Flexible / Moderada / Tardía).
- **SC-002**: El 100% de las solicitudes de cancelación del Propietario reciben la clasificación "Cancelación por Anfitrión", independientemente del tiempo de anticipación.
- **SC-003**: Cero (0%) clasificaciones emitidas cuando alguna entrada obligatoria está ausente o es inválida.
- **SC-004**: La clasificación es determinada y comunicada a Módulo 3 en menos de [NEEDS CLARIFICATION: tiempo objetivo — propuesta: 1 segundo desde que se recibe la solicitud de clasificación] desde que "Solicitar cancelación" invoca este caso de uso.
- **SC-005**: Cero (0%) eventos en los que una falla de la API de Módulo 3 resulte en una clasificación parcialmente registrada o un estado inconsistente en Módulo 2.
