# Feature Specification: Actualizar Estado de Reserva

**Módulo**: Módulo 2 (Operación de Reservas, Tiempos y Cancelaciones)  
**Created**: 2026-09-06  
**Primary Actor**: Sistema / Invocación interna (Orquestador de casos de uso de Módulo 2: `Iniciar reserva`, `Confirmar pago`, `Marcar inicio de la navegación`, `Marcar fin de navegacion`, `Solicitar cancelación`, `Marcar inasistencia`, y el temporizador de expiración TTL de 15 minutos)  
**External Dependencies (APIs)**:
- **Módulo 1 (Gestión de Flota y Activos P2P)**: API externa (`Asignar estado operativo`) para la sincronización del estado operativo de la embarcación (`Disponible`, `Reservado`, `En Navegación`, `En Mantenimiento/Limpieza`).
- **Módulo 3 (Liquidación, Seguros y Dispersión de Fondos)**: API externa (`Recibir estado de reserva`) hacia la cual Módulo 2 notifica las transiciones de estado y los sub-estados operativos (incluyendo el reporte de incidentes para gestión del depósito de garantía y liquidación).

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Centralizar y ejecutar transiciones válidas en la máquina de estados de la reserva (Priority: P1)

Cualquier caso de uso del ciclo de vida de Módulo 2 que requiera registrar el nacimiento o modificar el estado de una reserva invoca de forma obligatoria a "Actualizar estado reserva" (`<<include>>`: `Iniciar reserva`, `Confirmar pago`, `Marcar inicio de la navegación`, `Marcar fin de navegacion`, `Solicitar cancelación`, `Marcar inasistencia`, o el temporizador TTL de 15 minutos). El sistema valida que la transición solicitada cumpla con la máquina de estados formal, asigna el estado principal y el sub-estado correspondiente, persiste el cambio de manera atómica, registra la marca de tiempo exacta y preserva la trazabilidad histórica de la reserva.

**Why this priority**: Es el núcleo central de consistencia del dominio de reservas. Centralizar las transiciones en un motor único previene estados inconsistentes, condiciones de carrera concurrentes y corrupciones en el ciclo de vida del alquiler.

**Independent Test**: Se puede probar aislando el motor de transiciones, preparando reservas en cada estado principal admisible y solicitando transiciones legales (p. ej. `Creación` → `Pendiente de Pago`, `Pendiente de Pago` → `Confirmada`, `Confirmada` → `En Navegación`, `En Navegación` → `Completado` con sub-estados, `Confirmada` → `Cancelado` con sub-estados, `Pendiente de Pago` → `Expirado`), verificando que el nuevo estado y sub-estado quedan persistidos atómicamente con fecha y causa.

**Acceptance Scenarios**:

1. **Scenario**: Creación y registro inicial de reserva en Pendiente de Pago
    - **Given** una embarcación disponible según Módulo 1 y una solicitud de reserva válida
    - **When** el caso de uso "Iniciar reserva" invoca la transición inicial
    - **Then** el sistema crea la reserva con estado principal "Pendiente de Pago" e invoca a Módulo 1 (`Asignar estado operativo`) para fijar la embarcación en "Reservado"

2. **Scenario**: Confirmación de reserva tras aprobación de pago
    - **Given** una reserva en estado principal "Pendiente de Pago"
    - **When** el caso de uso "Confirmar pago" invoca la actualización a estado "Confirmada"
    - **Then** el sistema valida que la transición es legal, actualiza el estado principal a "Confirmada", almacena la marca temporal del cambio e inicia la notificación hacia los módulos externos

3. **Scenario**: Inicio del servicio de navegación (Check-in)
    - **Given** una reserva en estado principal "Confirmada"
    - **When** el caso de uso "Marcar inicio de la navegación" solicita la transición de estado
    - **Then** el sistema valida la transición y actualiza el estado principal de la reserva a "En Navegación"

4. **Scenario**: Cierre exitoso del servicio sin novedades (Check-out)
    - **Given** una reserva en estado principal "En Navegación"
    - **When** el caso de uso "Marcar fin de navegacion" reporta el cierre del servicio indicando ausencia de novedades
    - **Then** el sistema actualiza el estado principal a "Completado" con el sub-estado "Sin incidentes"

5. **Scenario**: Cierre del servicio con novedades o averías
    - **Given** una reserva en estado principal "En Navegación"
    - **When** el caso de uso "Marcar fin de navegacion" reporta el cierre del servicio notificando daños o incidencias
    - **Then** el sistema actualiza el estado principal a "Completado" con el sub-estado "Con incidentes"

6. **Scenario**: Cancelación voluntaria de reserva confirmada o por inasistencia
    - **Given** una reserva en estado principal "Confirmada"
    - **When** "Solicitar cancelación" o "Marcar inasistencia" invocan la actualización aportando la tipificación externa o causal correspondiente
    - **Then** el sistema actualiza el estado principal a "Cancelado" y asigna el sub-estado correspondiente ("Flexible", "Moderado", "Tardío", "Por Anfitrión" o "Por Inasistencia")

7. **Scenario**: Expiración automática por vencimiento del temporizador de 15 minutos
    - **Given** una reserva en estado principal "Pendiente de Pago" cuyo temporizador TTL de 15 minutos ha vencido sin confirmación de pago
    - **When** el temporizador interno del sistema dispara la actualización
    - **Then** el sistema actualiza el estado principal a "Expirado" de forma atómica

---

### User Story 2 - Sincronizar el estado operativo de la embarcación en Módulo 1 (Priority: P1)

En los hitos determinantes del ciclo de vida de la reserva, el sistema debe notificar de manera síncrona a la API externa `Asignar estado operativo` de Módulo 1 para actualizar el estado operativo de la embarcación (`Disponible`, `Reservado`, `En Navegación`, `En Mantenimiento/Limpieza`), asegurando que el inventario físico en muelle coincida exactamente con la disponibilidad del marketplace.

**Why this priority**: Si el estado de la embarcación no se actualiza en Módulo 1, el activo podría figurar disponible estando en altamar o reservado temporalmente, o quedar bloqueado tras cancelaciones y expiraciones, generando sobreventas o pérdidas operativas.

**Independent Test**: Se puede probar mediante un mock de la API `Asignar estado operativo` de Módulo 1, disparando transiciones de reserva y verificando que Módulo 1 recibe los llamados con el identificador del activo y el nuevo estado operativo exacto.

**Acceptance Scenarios**:

1. **Scenario**: Nacimiento de la reserva bloquea la embarcación como "Reservado"
    - **Given** una embarcación en estado operativo "Disponible" en Módulo 1
    - **When** la reserva se crea exitosamente pasando al estado inicial "Pendiente de Pago"
    - **Then** el sistema invoca la API externa `Asignar estado operativo` de Módulo 1 enviando el estado "Reservado" para esa embarcación

2. **Scenario**: Navegación iniciada actualiza la embarcación a "En Navegación"
    - **Given** una reserva que transiciona al estado principal "En Navegación"
    - **When** se completa la persistencia del estado en Módulo 2
    - **Then** el sistema invoca la API externa `Asignar estado operativo` de Módulo 1 enviando el estado operativo "En Navegación" para la embarcación asociada

3. **Scenario**: Culminación regular, cancelación o expiración libera la embarcación a "Disponible"
    - **Given** una reserva que transiciona a "Completado" con sub-estado "Sin incidentes", a "Cancelado" o a "Expirado"
    - **When** se procesa la actualización de estado
    - **Then** el sistema invoca la API externa `Asignar estado operativo` de Módulo 1 enviando el estado operativo "Disponible" para reintegrar la embarcación al inventario activo

4. **Scenario**: Culminación con reporte de avería o mantenimiento solicitado
    - **Given** una reserva que transiciona a "Completado" con sub-estado "Con incidentes" (o cancelación por anfitrión que reporta inhabilitación)
    - **When** se asienta la actualización de estado
    - **Then** el sistema invoca la API externa `Asignar estado operativo` de Módulo 1 enviando el estado operativo "En Mantenimiento/Limpieza" para bloquear temporalmente el activo hasta su inspección

---

### User Story 3 - Notificar transiciones y reporte de incidentes a Módulo 3 para liquidación y garantías (Priority: P1)

Cada cambio de estado y sub-estado en una reserva debe ser empujado a la API externa de Módulo 3 (`Recibir estado de reserva`). Esto permite que el motor financiero gobierne oportunamente la activación de seguros náuticos, el inicio de la dispersión de fondos al anfitrión, la retención preventiva o liberación de depósitos de garantía y el procesamiento de reembolsos, cumpliendo con la regla estricta de que Módulo 2 jamás calcula ni transfiere dinero.

**Why this priority**: Módulo 3 depende en tiempo real de los eventos operativos de Módulo 2 para ejecutar los movimientos financieros. Sin esta comunicación, las liquidaciones y garantías quedarían retenidas indefinidamente o liberadas erróneamente.

**Independent Test**: Se puede probar utilizando un mock de la API `Recibir estado de reserva` de Módulo 3, validando que ante cada transición en Módulo 2 se envíe un mensaje que incluya el identificador de reserva, estado principal, sub-estado y marca de tiempo, sin contener ningún atributo de cálculo monetario.

**Acceptance Scenarios**:

1. **Scenario**: Notificación de reserva confirmada para cobertura y custodia
    - **Given** una reserva que transiciona a estado principal "Confirmada"
    - **When** se asienta la transición en Módulo 2
    - **Then** el sistema invoca la API `Recibir estado de reserva` de Módulo 3 con el estado "Confirmada" para activar la póliza de seguro y la retención de garantía

2. **Scenario**: Notificación de servicio completado sin incidentes para dispersión y devolución de garantía
    - **Given** una reserva que transiciona a "Completado" con sub-estado "Sin incidentes"
    - **When** se procesa la actualización de estado
    - **Then** el sistema notifica a Módulo 3 para que inicie la dispersión de fondos al anfitrión y la liberación del depósito de garantía al arrendatario

3. **Scenario**: Notificación de servicio completado con incidentes para retención de garantía
    - **Given** una reserva que transiciona a "Completado" con sub-estado "Con incidentes"
    - **When** se procesa la actualización de estado
    - **Then** el sistema notifica a Módulo 3 la existencia de incidentes para que Módulo 3 retenga el depósito de garantía y gestione el reclamo financiero de forma autónoma

4. **Scenario**: Notificación de cancelación o inasistencia para ejecución de penalidades y reembolsos
    - **Given** una reserva que transiciona a "Cancelado" con un sub-estado específico ("Flexible", "Moderado", "Tardío", "Por Anfitrión", "Por Inasistencia")
    - **When** se registra la cancelación
    - **Then** el sistema notifica a Módulo 3 el estado "Cancelado" junto con su sub-estado (el de `Por Inasistencia` fue asignado directamente por Módulo 2; los otros cuatro fueron recibidos previamente desde Módulo 3 a través de "Solicitar tipo de cancelación") para que Módulo 3 aplique su matriz de liquidación y reembolsos

---

### User Story 4 - Rechazar estrictamente transiciones ilegales y preservar la inmutabilidad de estados terminales (Priority: P2)

Si se recibe una petición de cambio de estado incompatible con la máquina de estados formal, si se intenta cancelar una reserva que aún está en `Pendiente de Pago`, o si se intenta alterar una reserva que ya alcanzó un estado terminal (`Completado`, `Cancelado` o `Expirado`), el sistema debe denegar de forma estricta la solicitud, impidiendo modificaciones inconsistentes en persistencia y llamadas colaterales a sistemas externos.

**Why this priority**: Previene la corrupción de datos ante reintentos desfasados de red, eventos concurrentes desordenados o solicitudes maliciosas que pretendan alterar contratos ya concluidos o interrumpir flujos de pago protegidos por TTL.

**Independent Test**: Se prueba enviando intencionalmente transiciones ilegales (p. ej. `Pendiente de Pago` → `Cancelado`, `Expirado` → `Confirmada`, `Completado` → `Cancelado`, `En Navegación` → `Pendiente de Pago`) y comprobando que el sistema retorna una denegación formal, no altera el registro de la reserva y no genera tráfico hacia Módulo 1 ni Módulo 3.

**Acceptance Scenarios**:

1. **Scenario**: Intento de cancelación directa sobre reserva en Pendiente de Pago
    - **Given** una reserva en estado principal "Pendiente de Pago"
    - **When** se recibe una solicitud de cancelación voluntaria
    - **Then** el sistema rechaza la solicitud indicando que las reservas pendientes de pago no admiten cancelación directa y deben esperar la expiración natural del TTL de 15 minutos si se desiste de pagar

2. **Scenario**: Intento de confirmación extemporánea sobre reserva expirada
    - **Given** una reserva en estado principal "Expirado"
    - **When** se recibe una solicitud de confirmación de pago
    - **Then** el sistema rechaza la solicitud indicando estado terminal irreversible y no altera la reserva

3. **Scenario**: Intento de cancelación sobre servicio completado o en navegación
    - **Given** una reserva en estado principal "Completado" o "En Navegación"
    - **When** se solicita su cancelación
    - **Then** el sistema rechaza la operación indicando que el estado actual no admite cancelación

---

### Edge Cases

- **"Pendiente de Pago" no es cancelable voluntariamente**: Si el Arrendatario desiste de contratar una reserva que está en `Pendiente de Pago`, no existe endpoint ni acción de cancelación anticipada. La reserva se libera de forma pasiva y garantizada únicamente cuando expira el temporizador TTL de 15 minutos, pasando a `Expirado` y liberando la embarcación en Módulo 1.
- **Condición de carrera entre expiración del TTL de 15 minutos y confirmación de pago**: Si el evento de confirmación de pago de Módulo 3 coincide con el vencimiento del temporizador de 15 minutos, el sistema debe resolver la concurrencia de forma atómica: si la expiración se consolida primero, la reserva pasa a `Expirado`, se libera la embarcación y se rechaza la confirmación instruyendo a Módulo 3 el reembolso; si la confirmación entra antes de la persistencia de la expiración, la reserva transiciona a `Confirmada` y el temporizador se desactiva.
- **Concurrencia entre cancelación de reserva confirmada e inasistencia**: Si coinciden temporalmente una solicitud de cancelación voluntaria y un reporte de No-Show, el control de concurrencia optimista asegura que la primera transacción aceptada defina el estado final; la segunda es denegada por encontrarse la reserva en estado terminal.
- **Falla transitoria de conectividad con Módulo 1 o Módulo 3**: Si la persistencia del estado en Módulo 2 es exitosa pero la llamada a la API externa `Asignar estado operativo` de Módulo 1 o `Recibir estado de reserva` de Módulo 3 falla por timeout o error de red, el sistema DEBE encolar el evento pendiente de entrega y activar un mecanismo de reintentos automáticos garantizados para asegurar consistencia eventual (0% eventos perdidos).
- **Notificaciones duplicadas e idempotencia**: Si se solicita una transición hacia un estado y sub-estado en el que la reserva ya se encuentra actualmente (p. ej. reintento de webhook de pago ya aprobado), el sistema responde con confirmación exitosa de forma idempotente sin disparar dobles llamadas externas ni duplicar registros de auditoría.
- **Inmutabilidad absoluta de estados terminales**: Los estados `Completado`, `Cancelado` y `Expirado` (con cualquiera de sus sub-estados) son definitivos; ninguna entidad, actor ni evento puede modificar su estado una vez asentado.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE actuar como el motor central y exclusivo para actualizar el estado y sub-estado de cualquier reserva en Módulo 2, siendo invocado obligatoriamente por los casos de uso operativos mediante relaciones `<<include>>`.
- **FR-002**: El sistema DEBE gobernar la máquina de estados de la reserva bajo el siguiente catálogo formal:
    - 🔶 [PENDIENTE DE CONFIRMAR — Estados Principales de la Reserva]: `Pendiente de Pago`, `Confirmada`, `En Navegación`, `Completado`, `Cancelado`, `Expirado`. El estado inicial de toda reserva al crearse es obligatoriamente `Pendiente de Pago` (transición desde el estado inexistente hacia `Pendiente de Pago`, disparada exclusivamente por "Iniciar reserva"). `Pendiente de Pago`, `En Navegación` y `Expirado` no tienen sub-estados. [FIN PENDIENTE]
    - 🔶 [PENDIENTE DE CONFIRMAR — Sub-estados de Cancelación]: Aplican exclusivamente cuando el estado principal es `Cancelado`. Los valores admitidos son: `Flexible`, `Moderado`, `Tardío`, `Por Anfitrión`, `Por Inasistencia`. Los primeros cuatro (`Flexible`, `Moderado`, `Tardío`, `Por Anfitrión`) son **calculados por Módulo 3**: dentro de "Solicitar cancelación", Módulo 2 le envía a Módulo 3 (mediante "Solicitar tipo de cancelación") el tiempo de anticipación y el actor solicitante, y Módulo 3 aplica sus reglas de negocio y devuelve la clasificación — Módulo 2 no calcula esta categoría, solo la recibe y la persiste como sub-estado. El quinto valor (`Por Inasistencia`) es la única excepción: lo asigna **directamente Módulo 2** dentro de "Marcar inasistencia", sin consultar a Módulo 3, porque no depende de una ventana de tiempo variable sino de la regla fija de 30 minutos de tolerancia. [FIN PENDIENTE]
    - 🔶 [PENDIENTE DE CONFIRMAR — Sub-estados de Finalización]: Aplican exclusivamente cuando el estado principal es `Completado`. Los valores admitidos son: `Sin incidentes`, `Con incidentes`. [FIN PENDIENTE]
    - 🔶 [PENDIENTE DE CONFIRMAR — Estados terminales]: `Completado`, `Cancelado` y `Expirado` (con cualquiera de sus sub-estados). Ninguno de estos estados admite transiciones posteriores bajo ninguna circunstancia. [FIN PENDIENTE]
- **FR-003**: El sistema DEBE validar de forma estricta que la transición solicitada cumpla con las rutas legales permitidas:
    - `Creación → Pendiente de Pago`: Única vía de entrada a la máquina de estados, disparada exclusivamente por el caso de uso "Iniciar reserva".
    - Desde `Pendiente de Pago` solo se permite transicionar a: `Confirmada`, `Expirado`. (Las reservas en `Pendiente de Pago` NO admiten cancelación directa a través de "Solicitar cancelación"; si no se completa el pago, la reserva concluye únicamente por expiración pasiva del temporizador TTL).
    - Desde `Confirmada` solo se permite transicionar a: `En Navegación`, `Cancelado`.
    - Desde `En Navegación` solo se permite transicionar a: `Completado`.
    - Ninguna transición está permitida desde los estados terminales `Completado`, `Cancelado` o `Expirado`.
- **FR-004**: Si la transición solicitada es legal, el sistema DEBE actualizar y persistir de forma atómica en el registro de la Reserva su estado principal y, cuando aplique, su sub-estado correspondiente.
- **FR-005**: Si la transición solicitada es ilegal o viola las reglas de la máquina de estados, el sistema DEBE rechazar la solicitud, abstenerse de modificar el registro de la reserva y NO emitir notificaciones a sistemas externos.
- **FR-006**: El sistema DEBE aplicar control de concurrencia atómico para garantizar que, ante solicitudes simultáneas de actualización sobre la misma reserva, solo una transición gane y las subsecuentes se evalúen contra el estado actualizado.
- **FR-007**: 🔶 [PENDIENTE DE CONFIRMAR — Sincronización operativa con Módulo 1]: El sistema DEBE invocar la API externa `Asignar estado operativo` de Módulo 1 para actualizar el estado operativo de la embarcación según las siguientes reglas:
    - Cuando la reserva se crea (transición inicial a `Pendiente de Pago`): invocar `Asignar estado operativo` en Módulo 1 para actualizar la embarcación a `Reservado`.
    - Cuando la reserva pasa a `En Navegación`: actualizar la embarcación a `En Navegación`.
    - Cuando la reserva pasa a `Completado` (con sub-estado `Sin incidentes`), `Cancelado` o `Expirado`: actualizar la embarcación a `Disponible`.
    - Cuando la reserva pasa a `Completado` con sub-estado `Con incidentes` o se reporte inhabilitación física: actualizar la embarcación a `En Mantenimiento/Limpieza`. [FIN PENDIENTE]
- **FR-008**: El sistema DEBE invocar la API externa de Módulo 3 (`Recibir estado de reserva`) ante cada cambio de estado, transmitiendo el identificador de la reserva, el nuevo estado principal, el sub-estado (si aplica), la fecha/hora exacta de la transición y la causa o evento disparador.
- **FR-009**: 🔶 [PENDIENTE DE CONFIRMAR — Notificación de incidentes a Módulo 3]: Si el estado resultante es `Completado` con sub-estado `Con incidentes`, el sistema DEBE reportar explícitamente la condición de novedad a Módulo 3 para que dicho módulo gestione la retención preventiva del depósito de garantía y la tramitación del reclamo financiero. [FIN PENDIENTE]
- **FR-010**: **REGLA DE NEGOCIO ESTRICTA (Sin cálculo financiero):** El sistema **NO DEBE calcular montos de reembolso, porcentajes de penalidad, comisiones de plataforma, costos de seguros ni efectuar transferencias monetarias**. La responsabilidad de Módulo 2 se restringe a evaluar el tiempo, gobernar la máquina de estados y transmitir/recibir clasificaciones de cancelación hacia y desde Módulo 3 (excepto `Por Inasistencia`, que Módulo 2 asigna directamente); todo cálculo monetario, y la clasificación por tiempo de las cancelaciones voluntarias, es competencia exclusiva de Módulo 3.
- **FR-011**: El sistema DEBE registrar un asiento de auditoría cronológico por cada transición de estado exitosa, documentando: identificador de la reserva, estado principal anterior, sub-estado anterior, nuevo estado principal, nuevo sub-estado, caso de uso solicitante, actor o sistema causante y marca temporal exacta.
- **FR-012**: El sistema DEBE garantizar la entrega eventual y la no pérdida de notificaciones hacia Módulo 1 y Módulo 3 ante fallos transitorios de red mediante registro de eventos pendientes y reintentos.

---

### Key Entities

- **Reserva (`Reservation`)**: Entidad central de Módulo 2. Atributos funcionales clave: identificador único, identificador del arrendatario, identificador de la embarcación (referencia a Módulo 1), estado principal actual, sub-estado actual, fecha/hora pactada de inicio, fecha/hora pactada de fin, versión de concurrencia.
- **Historial de Transiciones de Estado (`ReservationStatusAudit`)**: Registro de auditoría del ciclo de vida. Atributos: identificador del evento de auditoría, identificador de la reserva, estado principal previo, sub-estado previo, nuevo estado principal, nuevo sub-estado, caso de uso origen, actor o disparador, marca temporal del cambio, observaciones/metadata contextual.
- **Embarcación**: Activo náutico cuya existencia física y estado operativo (`Disponible`, `Reservado`, `En Navegación`, `En Mantenimiento/Limpieza`) son gobernados en Módulo 1 y referenciados externamente en Módulo 2.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Cero (0%) transiciones de estado ilegales o no contempladas en la máquina de estados admitidas en la base de datos de Módulo 2.
- **SC-002**: El 100% de las transiciones de estado confirmadas disparan las notificaciones hacia la API `Asignar estado operativo` de Módulo 1 y la API `Recibir estado de reserva` de Módulo 3 en menos de 500 milisegundos desde la persistencia interna.
- **SC-003**: Cero (0%) discrepancias donde una reserva figure en creación/pendiente, en navegación o completada y el activo físico en Módulo 1 muestre un estado operativo contradictorio.
- **SC-004**: El 100% de los cambios de estado y sub-estado son notificados y confirmados por Módulo 3 (0% de eventos de sincronización financiera perdidos silenciosamente).
- **SC-005**: Cero (0) operaciones de cálculo monetario, cobro, reembolso o retención de dinero ejecutadas dentro de Módulo 2.
- **SC-006**: El 100% de las transiciones de estado quedan registradas de manera inmutable en el historial de auditoría con su estampa de tiempo y causante.
