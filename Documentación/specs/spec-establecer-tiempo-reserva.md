# Feature Specification: Establecer Tiempo Reserva

**Módulo**: Módulo 2 – Gestión de Reserva
**Creado**: 2026-09-04
**Actor primario**: Arrendatario (a través de "Iniciar reserva", que lo incluye)
**Dependencias externas (APIs)**:

- Ninguna directa. Recibe datos de tarifas de Módulo 3 de forma indirecta vía "Proveer información cotización de reserva" (documentado en su propio spec).

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - [Validar el periodo de reserva solicitado] (Priority: P1)

El sistema recibe del Arrendatario la ventana temporal solicitada (fecha/hora de inicio y fin) y valida que sea un periodo coherente: el inicio debe ser anterior al fin, la duración debe estar dentro de los límites permitidos y la ventana no debe estar en el pasado.

**Why this priority**: Sin una validación básica del periodo no existe ninguna reserva válida. Es el primer filtro del caso de uso y cualquier reserva creada sobre un periodo incoherente es un dato corrupto.

**Independent Test**: Se puede probar enviando a "Establecer tiempo reserva" combos de fechas (inicio anterior a fin, inicio igual a fin, inicio posterior a fin, ventana en el pasado, duración extrema) y verificando que cada caso se acepta o rechaza según la regla correspondiente.

**Acceptance Scenarios**:

1. **Scenario**: Periodo válido
   - **Given** el Arrendatario solicita un periodo con fecha/hora de inicio anterior a la fecha/hora de fin y dentro de [NEEDS CLARIFICATION: duración mínima y máxima de una reserva]
   - **When** el sistema valida el periodo
   - **Then** el sistema acepta el periodo como válido y lo devuelve como el rango temporal registrado para la reserva

2. **Scenario**: Fecha de inicio posterior a la fecha de fin
   - **Given** el Arrendatario solicita un periodo donde fecha/hora de inicio es posterior a fecha/hora de fin (o ambas son iguales, duración cero)
   - **When** el sistema valida el periodo
   - **Then** el sistema rechaza el periodo con el motivo "fecha de inicio debe ser anterior a fecha de fin" y NO se crea la reserva (FR-005 de `spec-iniciar-reserva.md`)

3. **Scenario**: Ventana en el pasado
   - **Given** el Arrendatario solicita un periodo cuyo inicio ya transcurrió [NEEDS CLARIFICATION: ¿se bloquea estrictamente, o se permite una tolerancia para reservas inmediatas/último momento?]
   - **When** el sistema valida el periodo
   - **Then** el sistema rechaza el periodo indicando que la ventana no puede comenzar en el pasado (o la aplica según la tolerancia definida)

4. **Scenario**: Duración fuera de los límites permitidos
   - **Given** una duración por debajo de la mínima o por encima de la máxima [NEEDS CLARIFICATION: valores de duración mínima/máxima de una reserva]
   - **When** el sistema valida el periodo
   - **Then** el sistema rechaza el periodo con el motivo correspondiente (duración menor a la mínima / mayor a la máxima)

### User Story 2 - [Evitar traslape con reservas existentes de la misma embarcación] (Priority: P1)

El sistema verifica que la ventana temporal solicitada no se traslape con ninguna otra reserva ya registrada para la misma embarcación en Módulo 2, para garantizar que el activo no se comprometa dos veces en el mismo rango horario.

**Why this priority**: Es la validación crítica de disponibilidad temporal del inventario. Permitir un traslape implicaría sobreventa, uno de los errores más graves del negocio náutico.

**Independent Test**: Se puede probar creando reservas previas en distintos rangos sobre la misma embarcación y solicitando nuevas ventanas que: (a) no se traslapen (se aceptan), (b) se traslapen parcial o totalmente (se rechazan), y (c) toquen exactamente la frontera (según la convención de extremos definida).

**Acceptance Scenarios**:

1. **Scenario**: Ventana sin traslape
   - **Given** una embarcación con reservas existentes en Módulo 2 en ventanas que no interceptan la solicitada
   - **When** el Arrendatario solicita un periodo sin superposición con las existentes
   - **Then** el sistema acepta el periodo y lo marca como disponible para esa embarcación

2. **Scenario**: Ventana con traslape total o parcial
   - **Given** una embarcación con una reserva existente en Módulo 2 que cubre total o parcialmente la ventana solicitada
   - **When** el Arrendatario solicita ese periodo
   - **Then** el sistema rechaza el periodo indicando que la embarcación no está disponible en ese rango (traslape con reserva existente) y NO se crea la reserva

3. **Scenario**: Ventana adyacente en la frontera exacta
   - **Given** una reserva existente que termina exactamente cuando comienza la ventana solicitada (fecha/hora de fin de una = fecha/hora de inicio de la otra)
   - **When** el Arrendatario solicita ese periodo
   - **Then** [NEEDS CLARIFICATION: ¿se considera traslape o es un límite válido sin superposición? Propuesta: usar semántica de intervalo medio-abierto [inicio, fin), donde un fin que coincide con un inicio NO se considera traslape, salvo que exista margen de limpieza]

### User Story 3 - [Respetar margen de limpieza/traslado entre reservas consecutivas] (Priority: P2)

Si la plataforma define un margen de tiempo para limpieza/traslado entre reservas consecutivas de la misma embarcación, el sistema debe considerarlo como no disponible dentro de la validación de traslape.

**Why this priority**: Evita que el activo se comprometa en un horario donde aún no está operativamente habilitado para el siguiente cliente (limpieza, repostaje, traslado a puerto). Es un incremento de calidad sobre la sobreventa básica (US2).

**Independent Test**: Se puede probar configurando un margen de limpieza y solicitando una reserva que comienza dentro del margen posterior al fin de otra reserva; la solicitud debe rechazarse solo si el margen existe y es aplicable.

**Acceptance Scenarios**:

1. **Scenario**: Margen de limpieza violado
   - **Given** un margen de limpieza configurado de N minutos entre reservas de la misma embarcación [NEEDS CLARIFICATION: ¿existe margen de limpieza/traslado en el negocio? ¿Cuál es su duración? ¿Aplica a fin→inicio consecutivo, o también a otras combinaciones de estados como mantenimiento?]
   - **When** el Arrendatario solicita una ventana cuyo inicio cae dentro de los N minutos posteriores al fin de otra reserva
   - **Then** el sistema rechaza el periodo indicando que debe respetarse el margen de limpieza entre reservas consecutivas

2. **Scenario**: Margen de limpieza respetado
   - **Given** un margen de limpieza configurado de N minutos
   - **When** el Arrendatario solicita una ventana cuyo inicio es posterior al fin de la reserva anterior más los N minutos de margen
   - **Then** el sistema acepta el periodo

### User Story 4 - [Resolver solicitudes simultáneas sobre la misma ventana sin doble asignación] (Priority: P2)

Dos o más Arrendatarios pueden intentar reservar la misma embarcación en el mismo rango horario casi simultáneamente. El sistema debe resolver el conflicto de forma atómica para que solo una solicitud gane la ventana y las demás sean rechazadas, sin reservas traslapadas en persistencia.

**Why this priority**: Sin una garantía atómica, la validación de traslape (US2) falla bajo concurrencia real: dos transacciones pueden leer el inventario "libre" antes de que cualquiera persista. Es el error residual de sobreventa más difícil de detectar.

**Independent Test**: Se puede probar disparando dos solicitudes concurrentes para la misma embarcación y ventana (load/race test) y verificando que exactamente una se persiste y la otra se rechaza, sin estados traslapados.

**Acceptance Scenarios**:

1. **Scenario**: Dos solicitudes simultáneas, un solo ganador
   - **Given** dos Arrendatarios solicitan la misma ventana sobre la misma embarcación al mismo tiempo
   - **When** ambas solicitudes se procesan concurrentemente
   - **Then** exactamente una solicitud se acepta (su reserva se persiste posteriormente en "Iniciar reserva") y la otra se rechaza con motivo de no disponibilidad, quedando en la base de datos una sola reserva para esa ventana

2. **Scenario**: Conflicto por solicitudes casi simultáneas
   - **Given** dos Arrendatarios solicitan la misma ventana sobre la misma embarcación casi al mismo tiempo
   - **When** ambas solicitudes intentan registrarse
   - **Then** el sistema garantiza que solo una se registre exitosamente; la otra es rechazada con motivo de no disponibilidad, sin importar el orden exacto de llegada

---

### Edge Cases

- **Frontera exacta entre reservas consecutivas**: si la reserva A termina exactamente a las 17:00 y la B solicita comenzar a las 17:00, ¿traslape o límite válido? [NEEDS CLARIFICATION: semántica de intervalos — propuesta: medio-abierta [inicio, fin), sin traslape en frontera coincidente, salvo margen de limpieza]
- **Condición de carrera (US4)**: dos solicitudes simultáneas sobre la misma ventana. [NEEDS CLARIFICATION: lock optimista vs. pesimista]
- **Margen de limpieza**: ¿existe, cuánto dura, y qué combinaciones de estados lo disparan (fin→inicio, mantenimiento→inicio)? [NEEDS CLARIFICATION]
- **Duración mínima/máxima**: sin definir aún. [NEEDS CLARIFICATION]
- **Ventana en el pasado**: ¿tolerancia para reservas inmediatas/último momento? [NEEDS CLARIFICATION]
- **Zona horaria**: el puerto de la embarcación y el cliente pueden estar en husos distintos. ¿La ventana se define en hora del puerto, del cliente o UTC? [NEEDS CLARIFICATION]
- **Reservas temporales no confirmadas (pendientes de pago)**: ¿una reserva en estado "Pendiente de Pago" (bloqueo TTL activo) cuenta como traslape para ventanas nuevas, o la ventana solo se bloquea al confirmar el pago? [NEEDS CLARIFICATION] — Propuesta: el bloqueo TTL de 15 min retiene la ventana temporalmente y cualquier solicitud nueva que la traslape se rechaza mientras el bloqueo esté activo.
- **Consulta del estado operativo vs. traslape**: la disponibilidad temporal (traslape contra reservas de Módulo 2) es responsabilidad de este caso de uso; el estado operativo actual de la embarcación (Disponible/Reservado/En Navegación/En Mantenimiento) lo consulta "Iniciar reserva" a Módulo 1 (FR-003 de `spec-iniciar-reserva.md`). El traslape NO reemplaza la validación contra Módulo 1, ni viceversa. [NEEDS CLARIFICATION: si surgen conflictos entre ambos (p. ej. Módulo 1 reporta "Reservado" pero no hay reserva en Módulo 2), ¿cuál manda?]
- **Reintento tras fracaso por concurrencia**: si la validación falla por condición de carrera, ¿el sistema reintenta automáticamente la validación o debe el Arrendatario volver a solicitar? [NEEDS CLARIFICATION]

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE recibir la ventana temporal solicitada compuesta por fecha/hora de inicio y fecha/hora de fin.
- **FR-002**: El sistema DEBE validar que la fecha/hora de inicio sea estrictamente anterior a la fecha/hora de fin.
- **FR-003**: El sistema DEBE validar que la duración del periodo esté dentro de los límites [NEEDS CLARIFICATION: duración mínima y máxima de una reserva].
- **FR-004**: El sistema DEBE validar que la ventana no inicie en el pasado [NEEDS CLARIFICATION: tolerancia para reservas inmediatas].
- **FR-005**: El sistema DEBE verificar que la ventana solicitada NO se traslape con ninguna reserva existente de la misma embarcación registrada en Módulo 2.
- **FR-006**: Si la ventana se traslapa con una reserva existente, el sistema DEBE rechazarla devolviendo el motivo "embarcación no disponible en el rango solicitado" (este motivo es el que "Iniciar reserva" muestra al Arrendatario, FR-005 de `spec-iniciar-reserva.md`).
- **FR-007**: Si existe un margen de limpieza/traslado configurado para la embarcación, el sistema DEBE tratarlo como tiempo no disponible dentro de la validación de traslape [NEEDS CLARIFICATION: duración del margen y combinaciones de estados que lo aplican].
- **FR-008**: El sistema DEBE validar el traslape de forma atómica, garantizando que solo una solicitud por ventana y embarcación pueda registrar (o retener) el rango temporal bajo concurrencia [NEEDS CLARIFICATION: lock optimista vs. pesimista].
- **FR-009**: Si la concurrencia provoca un conflicto al momento de registrar la ventana, el sistema DEBE rechazar la solicitud perdedora con el mismo motivo de no disponibilidad [NEEDS CLARIFICATION: ¿reintento automático o intervención del usuario?].
- **FR-010**: Tras validar exitosamente la ventana, el sistema DEBE invocar el caso de uso "Proveer información cotización de reserva" para calcular el costo del periodo (delegación obligatoria, `<<include>>` según el diagrama; el detalle interno de ese cálculo se especifica en su propio spec). La presentación de ese monto al Arrendatario la realiza "Iniciar reserva" (FR-006 de `spec-iniciar-reserva.md`), que solo lo muestra, no lo recalcula.
- **FR-011**: El sistema DEBE devolver al caso de uso "Iniciar reserva" el rango temporal validado y admitido, para que sea él quien persista la reserva e inicie el TTL de 15 minutos (el trigger lo documenta `spec-iniciar-reserva.md`, FR-007).

### Key Entities

- **Ventana Temporal / Periodo Reserva (`ReservationWindow`)**: Fecha/hora de inicio y fin pactadas. Se valida y se persiste como parte de la entidad Reserva; no tiene identidad propia más allá de su rango. Representación propuesta: intervalo [inicio, fin) [NEEDS CLARIFICATION: semántica de extremos].
- **Reserva** *(entidad de Módulo 2, referenciada)*: este caso de uso lee las reservas existentes para el chequeo de traslape y entrega la ventana validada para su persistencia. No crea ni modifica reservas.
- **Embarcación** *(entidad externa, propiedad de Módulo 1)*: se referencia por id; el traslape se valida contra el inventario de reservas de Módulo 2, no contra el estado operativo de Módulo 1 (ver Edge Cases).

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de las solicitudes con traslape (parcial, total o por margen de limpieza) son detectadas y rechazadas con el motivo correcto antes de persistir cualquier reserva.
- **SC-002**: Cero (0%) reservas traslapadas persistidas en la base de datos de Módulo 2 bajo cualquier nivel de concurrencia (se complementa con SC-006 de `spec-iniciar-reserva.md`).
- **SC-003**: Bajo solicitudes concurrentes sobre la misma ventana, exactamente una (1) solicitud gana; el 100% de las restantes se rechazan sin estados intermedios ambiguos ni reservas fantasma.
- **SC-004**: La validación completa de la ventana (reglas FR-002 a FR-009) se resuelve en menos de [NEEDS CLARIFICATION: tiempo objetivo, p. ej. 2 segundos], sin impacto perceptible en el flujo de "Iniciar reserva".
- **SC-005**: El 99% de las ventanas rechazadas presentan al Arrendatario un motivo claro y accionable (traslape, duración, margen, pasado), sin mensajes genéricos.