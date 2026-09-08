# Feature Specification: Actualizar Estado de Reserva

**Módulo**: Módulo 2 (Operación de Reservas, Tiempos y Cancelaciones)  
**Created**: 2026-09-06  
**Primary Actor**: Sistema / Se llama internamente (Es el motor que usan los demás casos de uso de Módulo 2: `Iniciar reserva`, `Confirmar pago`, `Marcar inicio de la navegación`, `Marcar fin de navegacion`, `Solicitar cancelación`, `Marcar inasistencia`, y el temporizador de expiración TTL de 15 minutos)  
**External Dependencies (APIs)**:
- **Módulo 1 (Gestión de Flota y Activos P2P)**: API externa (`Asignar estado operativo`) para mantener sincronizado el estado operativo de la embarcación (`Disponible`, `Reservado`, `En Navegación`, `En Mantenimiento/Limpieza`).
- **Módulo 3 (Liquidación, Seguros y Dispersión de Fondos)**: API externa (`Recibir estado de reserva`) a la que Módulo 2 le avisa cada vez que la reserva cambia de estado o sub-estado (incluyendo cuando hay que reportar un incidente para gestionar el depósito de garantía y la liquidación).

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Ser el único lugar donde se aplican los cambios de estado válidos de la reserva (Priority: P1)

Cualquier caso de uso de Módulo 2 que necesite registrar el nacimiento de una reserva o cambiar su estado tiene que pasar obligatoriamente por "Actualizar estado reserva" (`<<include>>`: `Iniciar reserva`, `Confirmar pago`, `Marcar inicio de la navegación`, `Marcar fin de navegacion`, `Solicitar cancelación`, `Marcar inasistencia`, o el temporizador TTL de 15 minutos). El sistema verifica que el cambio pedido cumpla con las reglas de estados, asigna el estado principal y el sub-estado que corresponda, guarda el cambio de forma segura, registra la hora exacta y deja guardado el historial completo de la reserva.

**Why this priority**: Es la pieza central que mantiene todo consistente en el mundo de las reservas. Tener un único motor de cambios evita estados inconsistentes, problemas cuando dos cosas pasan al mismo tiempo, y datos dañados en el ciclo de vida del alquiler.

**Independent Test**: Se puede probar aislando el motor de cambios de estado, preparando reservas en cada estado posible y pidiendo cambios permitidos (por ejemplo, `Creación` → `Pendiente de Pago`, `Pendiente de Pago` → `Confirmada`, `Confirmada` → `En Navegación`, `En Navegación` → `Completado` con sub-estados, `Confirmada` → `Cancelado` con sub-estados, `Pendiente de Pago` → `Expirado`), y verificando que el nuevo estado y sub-estado quedan guardados correctamente con fecha y motivo.

**Acceptance Scenarios**:

1. **Scenario**: Creación y primer registro de la reserva en Pendiente de Pago
    - **Given** una embarcación disponible según Módulo 1 y una solicitud de reserva válida
    - **When** el caso de uso "Iniciar reserva" pide la transición inicial
    - **Then** el sistema crea la reserva con estado principal "Pendiente de Pago" y le avisa a Módulo 1 (`Asignar estado operativo`) para poner la embarcación en "Reservado"

2. **Scenario**: Confirmación de la reserva tras aprobarse el pago
    - **Given** una reserva en estado principal "Pendiente de Pago"
    - **When** el caso de uso "Confirmar pago" pide actualizar a estado "Confirmada"
    - **Then** el sistema verifica que el cambio es válido, actualiza el estado principal a "Confirmada", guarda la hora del cambio y empieza a avisarle a los módulos externos

3. **Scenario**: Inicio del servicio de navegación (Check-in)
    - **Given** una reserva en estado principal "Confirmada"
    - **When** el caso de uso "Marcar inicio de la navegación" pide el cambio de estado
    - **Then** el sistema verifica el cambio y actualiza el estado principal de la reserva a "En Navegación"

4. **Scenario**: Cierre exitoso del servicio sin problemas (Check-out)
    - **Given** una reserva en estado principal "En Navegación"
    - **When** el caso de uso "Marcar fin de navegacion" avisa que el servicio terminó sin novedades
    - **Then** el sistema actualiza el estado principal a "Completado" con el sub-estado "Sin incidentes"

5. **Scenario**: Cierre del servicio con novedades o averías
    - **Given** una reserva en estado principal "En Navegación"
    - **When** el caso de uso "Marcar fin de navegacion" avisa que el servicio terminó con daños o incidencias
    - **Then** el sistema actualiza el estado principal a "Completado" con el sub-estado "Con incidentes"

6. **Scenario**: Cancelación voluntaria de una reserva confirmada, o por inasistencia
    - **Given** una reserva en estado principal "Confirmada"
    - **When** "Solicitar cancelación" o "Marcar inasistencia" piden el cambio, aportando el tipo o motivo correspondiente
    - **Then** el sistema actualiza el estado principal a "Cancelado" y asigna el sub-estado que corresponda ("Flexible", "Moderado", "Tardío", "Por Anfitrión" o "Por Inasistencia")

7. **Scenario**: Expiración automática al cumplirse los 15 minutos
    - **Given** una reserva en estado principal "Pendiente de Pago" cuyo temporizador TTL de 15 minutos ya venció sin que se confirmara el pago
    - **When** el temporizador interno del sistema dispara el cambio
    - **Then** el sistema actualiza el estado principal a "Expirado" de forma segura y completa

---

### User Story 2 - Mantener sincronizado el estado operativo de la embarcación en Módulo 1 (Priority: P1)

En los momentos clave del ciclo de vida de la reserva, el sistema le avisa de inmediato a la API `Asignar estado operativo` de Módulo 1 para actualizar el estado operativo de la embarcación (`Disponible`, `Reservado`, `En Navegación`, `En Mantenimiento/Limpieza`), asegurando que lo que se ve en el muelle coincida exactamente con lo que se muestra en la plataforma.

**Why this priority**: Si el estado de la embarcación no se actualiza en Módulo 1, el barco podría figurar disponible estando en altamar, o quedar bloqueado después de una cancelación o expiración, generando sobreventas o pérdidas para el negocio.

**Independent Test**: Se puede probar simulando la API `Asignar estado operativo` de Módulo 1, provocando cambios de estado de reserva y verificando que Módulo 1 recibe el aviso con el identificador correcto del barco y el nuevo estado operativo exacto.

**Acceptance Scenarios**:

1. **Scenario**: Al nacer la reserva, la embarcación queda bloqueada como "Reservado"
    - **Given** una embarcación en estado operativo "Disponible" en Módulo 1
    - **When** la reserva se crea exitosamente y pasa al estado inicial "Pendiente de Pago"
    - **Then** el sistema llama a la API `Asignar estado operativo` de Módulo 1 enviando el estado "Reservado" para esa embarcación

2. **Scenario**: Al iniciar la navegación, la embarcación pasa a "En Navegación"
    - **Given** una reserva que pasa al estado principal "En Navegación"
    - **When** se guarda ese cambio en Módulo 2
    - **Then** el sistema llama a la API `Asignar estado operativo` de Módulo 1 enviando el estado operativo "En Navegación" para la embarcación asociada

3. **Scenario**: Un cierre normal, una cancelación o una expiración liberan la embarcación a "Disponible"
    - **Given** una reserva que pasa a "Completado" con sub-estado "Sin incidentes", a "Cancelado" o a "Expirado"
    - **When** se procesa el cambio de estado
    - **Then** el sistema llama a la API `Asignar estado operativo` de Módulo 1 enviando el estado operativo "Disponible" para devolver la embarcación al inventario disponible

4. **Scenario**: Un cierre con reporte de avería envía la embarcación a mantenimiento
    - **Given** una reserva que pasa a "Completado" con sub-estado "Con incidentes" (o una cancelación por anfitrión que reporta que el barco quedó inhabilitado)
    - **When** se guarda el cambio de estado
    - **Then** el sistema llama a la API `Asignar estado operativo` de Módulo 1 enviando el estado operativo "En Mantenimiento/Limpieza" para bloquear temporalmente el barco hasta que lo revisen

---

### User Story 3 - Avisarle a Módulo 3 sobre los cambios de estado y los incidentes, para la liquidación y las garantías (Priority: P1)

Cada vez que cambia el estado o el sub-estado de una reserva, el sistema DEBE avisarle a la API externa de Módulo 3 (`Recibir estado de reserva`). Con esto, Módulo 3 puede activar a tiempo los seguros del barco, empezar a entregarle el dinero al anfitrión, retener o devolver a tiempo los depósitos de garantía, y procesar los reembolsos — todo esto respetando la regla estricta de que Módulo 2 jamás calcula ni mueve dinero.

**Why this priority**: Módulo 3 necesita enterarse en tiempo real de lo que pasa en Módulo 2 para poder mover el dinero correctamente. Sin esta comunicación, las liquidaciones y garantías quedarían retenidas para siempre o se liberarían por error.

**Independent Test**: Se puede probar simulando la API `Recibir estado de reserva` de Módulo 3, verificando que ante cada cambio en Módulo 2 se envíe un mensaje con el identificador de la reserva, el estado principal, el sub-estado y la hora exacta, sin ningún dato de cálculo de dinero.

**Acceptance Scenarios**:

1. **Scenario**: Aviso de reserva confirmada, para activar la cobertura y la custodia
    - **Given** una reserva que pasa al estado principal "Confirmada"
    - **When** se guarda ese cambio en Módulo 2
    - **Then** el sistema llama a la API `Recibir estado de reserva` de Módulo 3 con el estado "Confirmada" para activar la póliza de seguro y la retención de la garantía

2. **Scenario**: Aviso de servicio completado sin incidentes, para entregar el dinero y devolver la garantía
    - **Given** una reserva que pasa a "Completado" con sub-estado "Sin incidentes"
    - **When** se procesa el cambio de estado
    - **Then** el sistema le avisa a Módulo 3 para que empiece a entregarle el dinero al anfitrión y le devuelva el depósito de garantía al arrendatario

3. **Scenario**: Aviso de servicio completado con incidentes, para retener la garantía
    - **Given** una reserva que pasa a "Completado" con sub-estado "Con incidentes"
    - **When** se procesa el cambio de estado
    - **Then** el sistema le avisa a Módulo 3 que hubo incidentes, para que retenga el depósito de garantía y gestione el reclamo por su cuenta

4. **Scenario**: Aviso de cancelación o inasistencia, para aplicar penalidades y reembolsos
    - **Given** una reserva que pasa a "Cancelado" con un sub-estado específico ("Flexible", "Moderado", "Tardío", "Por Anfitrión", "Por Inasistencia")
    - **When** se registra la cancelación
    - **Then** el sistema le avisa a Módulo 3 el estado "Cancelado" junto con su sub-estado (el "Por Inasistencia" lo asignó directamente Módulo 2; los otros cuatro los recibió antes desde Módulo 3 mediante "Solicitar tipo de cancelación") para que Módulo 3 aplique su matriz de liquidación y reembolsos

---

### User Story 4 - Rechazar por completo los cambios no permitidos, y mantener intactos los estados finales (Priority: P2)

Si llega un pedido de cambio de estado que no está permitido, si se intenta cancelar una reserva que sigue en `Pendiente de Pago`, o si se intenta modificar una reserva que ya llegó a un estado final (`Completado`, `Cancelado` o `Expirado`), el sistema tiene que rechazar la solicitud sin excepciones, sin guardar cambios inconsistentes ni hacer llamadas innecesarias a otros sistemas.

**Why this priority**: Evita que se dañen los datos por reintentos de red desfasados, eventos que llegan desordenados, o intentos maliciosos de modificar contratos que ya se cerraron.

**Independent Test**: Se prueba enviando a propósito cambios de estado no permitidos (por ejemplo, `Pendiente de Pago` → `Cancelado`, `Expirado` → `Confirmada`, `Completado` → `Cancelado`, `En Navegación` → `Pendiente de Pago`) y comprobando que el sistema devuelve un rechazo claro, no cambia la reserva y no genera ningún tráfico hacia Módulo 1 ni Módulo 3.

**Acceptance Scenarios**:

1. **Scenario**: Intento de cancelar directamente una reserva en Pendiente de Pago
    - **Given** una reserva en estado principal "Pendiente de Pago"
    - **When** llega una solicitud de cancelación voluntaria
    - **Then** el sistema rechaza la solicitud explicando que las reservas pendientes de pago no admiten cancelación directa, y que deben esperar a que el TTL de 15 minutos expire por sí solo si se decide no pagar

2. **Scenario**: Intento de confirmar el pago fuera de tiempo, sobre una reserva expirada
    - **Given** una reserva en estado principal "Expirado"
    - **When** llega una solicitud de confirmación de pago
    - **Then** el sistema rechaza la solicitud explicando que es un estado final e irreversible, y no cambia la reserva

3. **Scenario**: Intento de cancelar un servicio ya completado o en navegación
    - **Given** una reserva en estado principal "Completado" o "En Navegación"
    - **When** se pide cancelarla
    - **Then** el sistema rechaza la operación explicando que el estado actual no admite cancelación

---

### Edge Cases

- **"Pendiente de Pago" no se puede cancelar por voluntad propia**: Si el Arrendatario ya no quiere seguir con una reserva que está en `Pendiente de Pago`, no existe ninguna acción ni botón de cancelación anticipada. La reserva se libera solo de forma pasiva, cuando vence el temporizador TTL de 15 minutos, pasando a `Expirado` y liberando la embarcación en Módulo 1.
- **Cuando el TTL vence casi al mismo tiempo que llega la confirmación de pago**: Si el aviso de pago confirmado de Módulo 3 llega justo cuando se cumplen los 15 minutos, el sistema tiene que resolver esa carrera de forma segura: si la expiración se guarda primero, la reserva pasa a `Expirado`, se libera la embarcación y se rechaza la confirmación, pidiéndole a Módulo 3 que haga el reembolso; si la confirmación entra antes de que se guarde la expiración, la reserva pasa a `Confirmada` y se apaga el temporizador.
- **Cuando coinciden una cancelación de reserva confirmada y un reporte de inasistencia**: Si al mismo tiempo llegan una solicitud de cancelación voluntaria y un reporte de No-Show, el control de concurrencia se asegura de que la primera que se procese sea la que quede como estado final; la segunda se rechaza porque la reserva ya está en un estado terminal.
- **Falla pasajera de conexión con Módulo 1 o Módulo 3**: Si el estado ya se guardó bien en Módulo 2 pero la llamada a la API `Asignar estado operativo` de Módulo 1 o `Recibir estado de reserva` de Módulo 3 falla por timeout o problema de red, el sistema DEBE guardar ese aviso en una cola pendiente y reintentarlo automáticamente hasta que se entregue con éxito (0% de eventos perdidos).
- **Avisos repetidos (idempotencia)**: Si se pide un cambio hacia un estado y sub-estado en el que la reserva ya se encuentra (por ejemplo, un reintento de aviso de pago ya aprobado), el sistema responde como si hubiera funcionado, pero sin volver a llamar a los sistemas externos ni duplicar el historial.
- **Los estados finales no se pueden tocar nunca más**: Los estados `Completado`, `Cancelado` y `Expirado` (con cualquiera de sus sub-estados) son definitivos; ningún actor ni evento puede modificarlos una vez que quedaron guardados.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE ser el único lugar donde se actualiza el estado y sub-estado de cualquier reserva en Módulo 2; todos los demás casos de uso operativos tienen que llamarlo obligatoriamente mediante relaciones `<<include>>`.
- **FR-002**: El sistema DEBE seguir esta lista oficial de estados de la reserva:
    - 🔶 [PENDIENTE DE CONFIRMAR — Estados Principales de la Reserva]: `Pendiente de Pago`, `Confirmada`, `En Navegación`, `Completado`, `Cancelado`, `Expirado`. El estado inicial de toda reserva al crearse es siempre `Pendiente de Pago` (el cambio desde "no existe" hacia `Pendiente de Pago` lo dispara únicamente "Iniciar reserva"). `Pendiente de Pago`, `En Navegación` y `Expirado` no tienen sub-estados. [FIN PENDIENTE]
    - 🔶 [PENDIENTE DE CONFIRMAR — Sub-estados de Cancelación]: Solo aplican cuando el estado principal es `Cancelado`. Los valores permitidos son: `Flexible`, `Moderado`, `Tardío`, `Por Anfitrión`, `Por Inasistencia`. Los primeros cuatro (`Flexible`, `Moderado`, `Tardío`, `Por Anfitrión`) los **calcula Módulo 3**: dentro de "Solicitar cancelación", Módulo 2 le manda a Módulo 3 (mediante "Solicitar tipo de cancelación") el tiempo de anticipación y quién pidió la cancelación, y Módulo 3 aplica sus reglas y devuelve la clasificación — Módulo 2 no calcula esta categoría, solo la recibe y la guarda como sub-estado. El quinto valor (`Por Inasistencia`) es la única excepción: lo asigna **directamente Módulo 2** dentro de "Marcar inasistencia", sin preguntarle a Módulo 3, porque no depende de un tiempo variable sino de la regla fija de 30 minutos de tolerancia. [FIN PENDIENTE]
    - 🔶 [PENDIENTE DE CONFIRMAR — Sub-estados de Finalización]: Solo aplican cuando el estado principal es `Completado`. Los valores permitidos son: `Sin incidentes`, `Con incidentes`. [FIN PENDIENTE]
    - 🔶 [PENDIENTE DE CONFIRMAR — Estados terminales]: `Completado`, `Cancelado` y `Expirado` (con cualquiera de sus sub-estados). Ninguno de estos estados admite más cambios después, bajo ninguna circunstancia. [FIN PENDIENTE]
- **FR-003**: El sistema DEBE verificar de forma estricta que el cambio de estado pedido sea uno de los permitidos:
    - `Creación → Pendiente de Pago`: única forma de entrar a la máquina de estados, disparada solo por "Iniciar reserva".
    - Desde `Pendiente de Pago` solo se puede pasar a: `Confirmada`, `Expirado`. (Las reservas en `Pendiente de Pago` NO admiten cancelación directa desde "Solicitar cancelación"; si no se completa el pago, la reserva termina únicamente por la expiración pasiva del TTL).
    - Desde `Confirmada` solo se puede pasar a: `En Navegación`, `Cancelado`.
    - Desde `En Navegación` solo se puede pasar a: `Completado`.
    - Ningún cambio está permitido desde los estados terminales `Completado`, `Cancelado` o `Expirado`.
- **FR-004**: Si el cambio pedido es válido, el sistema DEBE actualizar y guardar de forma segura y completa el estado principal de la Reserva y, cuando aplique, su sub-estado.
- **FR-005**: Si el cambio pedido no es válido o rompe las reglas de la máquina de estados, el sistema DEBE rechazar la solicitud, no tocar el registro de la reserva y NO avisarle a ningún sistema externo.
- **FR-006**: El sistema DEBE tener un control que evite que dos solicitudes de actualización sobre la misma reserva choquen al mismo tiempo: solo una gana, y las siguientes se evalúan contra el estado ya actualizado.
- **FR-007**: 🔶 [PENDIENTE DE CONFIRMAR — Sincronización operativa con Módulo 1]: El sistema DEBE llamar a la API externa `Asignar estado operativo` de Módulo 1 para actualizar el estado operativo de la embarcación según estas reglas:
    - Cuando la reserva se crea (primer cambio a `Pendiente de Pago`): llamar a `Asignar estado operativo` en Módulo 1 para poner la embarcación en `Reservado`.
    - Cuando la reserva pasa a `En Navegación`: actualizar la embarcación a `En Navegación`.
    - Cuando la reserva pasa a `Completado` (con sub-estado `Sin incidentes`), `Cancelado` o `Expirado`: actualizar la embarcación a `Disponible`.
    - Cuando la reserva pasa a `Completado` con sub-estado `Con incidentes`, o se reporta que el barco quedó inhabilitado: actualizar la embarcación a `En Mantenimiento/Limpieza`. [FIN PENDIENTE]
- **FR-008**: El sistema DEBE llamar a la API externa de Módulo 3 (`Recibir estado de reserva`) ante cada cambio de estado, enviando el identificador de la reserva, el nuevo estado principal, el sub-estado (si aplica), la fecha/hora exacta del cambio y qué lo causó.
- **FR-009**: 🔶 [PENDIENTE DE CONFIRMAR — Aviso de incidentes a Módulo 3]: Si el resultado es `Completado` con sub-estado `Con incidentes`, el sistema DEBE avisarle explícitamente a Módulo 3 sobre esa novedad, para que ese módulo retenga el depósito de garantía de forma preventiva y gestione el reclamo financiero. [FIN PENDIENTE]
- **FR-010**: **REGLA DE NEGOCIO ESTRICTA (Sin cálculo financiero):** El sistema **NO DEBE calcular montos de reembolso, porcentajes de penalidad, comisiones de la plataforma, costos de seguros ni hacer transferencias de dinero**. La responsabilidad de Módulo 2 se limita a manejar el tiempo, gobernar la máquina de estados, y enviar/recibir las clasificaciones de cancelación hacia y desde Módulo 3 (excepto `Por Inasistencia`, que Módulo 2 asigna directamente); todo cálculo de dinero, y la clasificación por tiempo de las cancelaciones voluntarias, es responsabilidad exclusiva de Módulo 3.
- **FR-011**: El sistema DEBE registrar en orden cronológico cada cambio de estado exitoso, guardando: identificador de la reserva, estado principal anterior, sub-estado anterior, nuevo estado principal, nuevo sub-estado, caso de uso que lo pidió, actor o sistema que lo causó y la hora exacta.
- **FR-012**: El sistema DEBE garantizar que los avisos a Módulo 1 y Módulo 3 siempre lleguen, incluso después de fallas de red pasajeras, guardando los eventos pendientes y reintentándolos.

---

### Key Entities

- **Reserva (`Reservation`)**: Entidad principal de Módulo 2. Datos clave: identificador único, identificador del arrendatario, identificador de la embarcación (referencia a Módulo 1), estado principal actual, sub-estado actual, fecha/hora pactada de inicio, fecha/hora pactada de fin, versión de concurrencia.
- **Historial de Cambios de Estado (`ReservationStatusAudit`)**: Registro del historial de vida de la reserva. Datos: identificador del evento, identificador de la reserva, estado principal anterior, sub-estado anterior, nuevo estado principal, nuevo sub-estado, caso de uso de origen, actor o disparador, hora del cambio, notas/metadata de contexto.
- **Embarcación**: Barco físico cuya existencia y estado operativo (`Disponible`, `Reservado`, `En Navegación`, `En Mantenimiento/Limpieza`) se manejan en Módulo 1 y se referencian desde Módulo 2.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Cero (0%) cambios de estado no permitidos o fuera de la máquina de estados guardados en la base de datos de Módulo 2.
- **SC-002**: El 100% de los cambios de estado confirmados disparan los avisos hacia la API `Asignar estado operativo` de Módulo 1 y la API `Recibir estado de reserva` de Módulo 3 en menos de 500 milisegundos desde que se guardan internamente.
- **SC-003**: Cero (0%) casos donde una reserva figure en creación/pendiente, en navegación o completada y el barco real en Módulo 1 muestre un estado operativo que no coincide.
- **SC-004**: El 100% de los cambios de estado y sub-estado son avisados y confirmados por Módulo 3 (0% de eventos de sincronización financiera perdidos en silencio).
- **SC-005**: Cero (0) operaciones de cálculo de dinero, cobro, reembolso o retención de fondos hechas dentro de Módulo 2.
- **SC-006**: El 100% de los cambios de estado quedan registrados de forma permanente en el historial, con su hora exacta y quién los causó.
