# Feature Specification: Recibir Información de Estado Operativo (Notificación a Módulo 1)

**Módulo**: Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones  
**Código de Caso de Uso**: CU-22  
**Fecha de Creación**: 2026-10-21  
**Actores Primarios / Disparador**: Sistema / Invocado internamente por el caso de uso `Actualizar estado reserva` (`CU-08`) mediante relación `<<include>>`.  
**Dependencias Externas (APIs)**:
- **Módulo 1 (Gestión de Flota y Activos P2P)**: API externa receptora (`Asignar estado operativo`), encargada de materializar en su inventario el estado operativo de la embarcación (`Disponible`, `Reservado`, `En Navegación`, `En Mantenimiento/Limpieza`).
- **Casos de uso internos de Módulo 2**:
    - `Actualizar estado reserva` (`CU-08`, `<<include>>`): único disparador de este caso de uso. Se invoca de forma **independiente** del `<<include>>` hacia `Recibir Estado de Reserva` (`CU-14`, notificación a Módulo 3): el fallo de una de las dos notificaciones no invalida ni impide la otra.

> **Nota de alcance (sentidos opuestos, no confundir)**: este caso de uso es el camino **Módulo 2 → Módulo 1** de la sincronización del estado operativo. **NO** es el caso de uso `Brindar información de estado operativo` (`CU-10`), que es el camino **Módulo 1 → Módulo 2** (allí Módulo 2 *pregunta* el estado operativo para validar disponibilidad). Ambos hablan del mismo dominio —el estado operativo de la embarcación— pero con direcciones, disparadores, contratos y objetivos opuestos.

> **Nota de caso de uso hermano (no fusionar)**: `Recibir Estado de Reserva` (`CU-14`) notifica a **Módulo 3** el estado de la **reserva** tal cual. Este caso de uso notifica a **Módulo 1** el **estado operativo de la embarcación ya mapeado**, que es una entidad distinta. Cada uno tiene su propia tabla de mapeo de estados y puede evolucionar de forma independiente; comparten el mecanismo técnico de entrega, no el contrato de negocio.

> **Nota de fuente de verdad (mapeo estado de reserva → estado operativo notificado a Módulo 1)**: la tabla de **FR-002 de este spec es la ÚNICA fuente de verdad** de esa correspondencia. Ningún otro documento del proyecto (spec, plan técnico, contrato de integración o matriz de trazabilidad) debe mantener una copia propia del mapeo con granularidad o reglas distintas: cuando un documento necesite describirlo, debe **referenciar esta tabla** (por ejemplo, `CU-08` FR-007 remite a esta tabla en lugar de replicarla). Cualquier divergencia entre esta tabla y otro documento se resuelve a favor de esta tabla.

---

## User Scenarios & Testing

### User Story 1 - Traducir el cambio de estado de la reserva y avisar el estado operativo a Módulo 1 (Priority: P1)

Como sistema (Módulo 2), quiero avisar a Módulo 1 el **estado operativo resultante** de la embarcación cada vez que una reserva cambia de estado, para que el inventario físico de la flota refleje exactamente los compromisos de navegación y los bloqueos de la plataforma.

***Why this priority***: Módulo 1 es la fuente de verdad del inventario náutico y no participa de la transacción local de Módulo 2. Sin este aviso, una reserva ya confirmada puede dejar la embarcación figurando como libre en el muelle y ser alquilada por otro Arrendatario.

***Independent Test***: Se prueba interceptando la salida de eventos desde `Actualizar estado reserva` hacia un doble de la API de Módulo 1. Se provocan una transición a `Reservada` y otra a `En Navegación`, y se verifica que se emiten dos avisos cuyo *payload* contiene el identificador de la embarcación, el estado operativo **ya mapeado** (`Reservado`, `En Navegación`) y la marca de tiempo del evento, sin ningún estado crudo de reserva ni dato monetario.

***Acceptance Scenarios***:

1. **Scenario**: La reserva pasa a `Reservada` y Módulo 1 recibe el estado operativo `Reservado`
    - **Given** una reserva que transiciona al estado principal `Reservada`
    - **When** el caso de uso `Actualizar estado reserva` consolida el cambio
    - **Then** el sistema encola la notificación a Módulo 1 con el estado operativo `Reservado` para esa embarcación

2. **Scenario**: La reserva pasa a `En Navegación` y Módulo 1 recibe el estado operativo `En Navegación`
    - **Given** una reserva que transiciona al estado principal `En Navegación`
    - **When** el cambio queda asentado en la base de datos local
    - **Then** el sistema encola la notificación a Módulo 1 con el estado operativo `En Navegación` para esa embarcación

3. **Scenario**: La reserva nace en `Iniciada` y no se notifica nada a Módulo 1
    - **Given** una reserva recién creada en el estado principal `Iniciada`
    - **When** el caso de uso `Actualizar estado reserva` consolida la creación
    - **Then** el sistema **NO** encola ninguna notificación a Módulo 1 (todavía no hay bloqueo operativo que comunicar)

---

### User Story 2 - Asegurar la entrega del aviso a Módulo 1 ante caídas de red (Priority: P1)

Como sistema, quiero encolar y reintentar la notificación dirigida a Módulo 1 si este no responde, para que ningún cambio de estado operativo se pierda silenciosamente y el inventario de la flota no quede desalineado respecto de las reservas vivas.

***Why this priority***: Una notificación perdida rompe la consistencia entre la operación y la flota: una reserva viva seguiría apareciendo como libre en el inventario de Módulo 1 y podría ser reasignada a otro Arrendatario. La entrega garantizada (Event Delivery Guarantee) es innegociable en arquitecturas desacopladas.

***Independent Test***: Se simula una caída (HTTP 503 o timeout) en la API de Módulo 1, se dispara un cambio de estado en Módulo 2 y se verifica que la transición quede guardada, que el evento quede en la cola de entrega y que se reintente según FR-006 (máximo 5 intentos con backoff exponencial y jitter), derivando a DLQ con alerta si se agotan los intentos.

***Acceptance Scenarios***:

1. **Scenario**: Reintento automático por indisponibilidad de Módulo 1
    - **Given** una reserva que cambia a `En Navegación` mientras la API de Módulo 1 está caída
    - **When** el sistema intenta entregar la notificación y recibe un error de conexión
    - **Then** el sistema marca el evento como "Pendiente de envío" y lo reintenta con una estrategia de respaldo progresivo (*backoff*) hasta que Módulo 1 confirme la recepción

2. **Scenario**: Agotamiento de reintentos y derivación a DLQ
    - **Given** un evento que ya acumula cinco intentos fallidos por indisponibilidad de Módulo 1
    - **When** el quinto intento termina en error
    - **Then** el sistema deriva el evento a la cola de letras muertas (DLQ) y dispara una alerta, sin descartar el evento ni alterar el estado ya consolidado de la reserva

---

### User Story 3 - Permitir que Módulo 1 procese duplicados sin efectos repetidos (Priority: P2)

Como sistema, quiero que cada aviso a Módulo 1 lleve un identificador único por transición, para que Módulo 1 pueda descartar entregas duplicadas (por ejemplo, ante un falso timeout de red) sin volver a aplicar el mismo estado operativo.

***Why this priority***: La entrega es *at-least-once*: sin un identificador de evento, un reintento podría reprocesar una transición ya aplicada y generar efectos inconsistentes en el inventario de la flota.

***Independent Test***: Se fuerza la reentrega del mismo evento a Módulo 1 y se verifica que el identificador del evento es idéntico al del primer envío y que es único por transición, de modo que el receptor pueda deduplicar.

***Acceptance Scenarios***:

1. **Scenario**: Reentrega del mismo evento por falso timeout
    - **Given** una notificación ya entregada a Módulo 1 que se vuelve a encolar por un falso timeout de red
    - **When** Módulo 1 la recibe por segunda vez
    - **Then** el evento llega con el mismo identificador único de transición, lo que permite a Módulo 1 procesarlo de forma idempotente y sin efectos repetidos

---

### Edge Cases

- **Falso timeout de red**: si Módulo 2 entrega dos veces la misma notificación por un falso timeout, Módulo 1 debe poder procesarla de forma idempotente. Módulo 2 envía un identificador único por cada transición para facilitarlo (FR-004 y FR-005).
- **Error permanente (4xx) de Módulo 1**: si Módulo 1 rechaza el aviso con un error 4xx (por ejemplo, embarcación inexistente o estado operativo no reconocido), el evento **no se reintenta** y se escala directamente a DLQ con alerta, porque reintentar un rechazo permanente no aporta resultado.
- **Sin respuesta síncrona**: este caso de uso no espera una respuesta de negocio de Módulo 1 ni revierte la transición local de la reserva. La entrega se considera consumada con la confirmación del broker, no con un acuse de negocio del receptor.
- **Prohibición absoluta de información financiera**: la notificación es puramente operativa. **Módulo 2 JAMÁS incluye montos, tarifas, penalidades, valores de reembolso ni datos de liquidación o pago**. Solo el identificador de la embarcación, el estado operativo resultante y los metadatos técnicos del evento.
- **Entrega fuera de orden**: si dos notificaciones correspondientes a dos transiciones consecutivas de la misma Reserva llegan en orden inverso al de las transiciones, cada una debe ser tratada por Módulo 1 como una transición idempotente identificada por su propio identificador de evento. `[NEEDS CLARIFICATION: el contrato no define garantías de orden entre eventos de la misma embarcación]`.
- **Conflicto de doble reserva**: **no aplica** a este caso de uso. La prevención de la doble reserva la resuelve Módulo 2 internamente antes de confirmar la reserva, usando sus propios registros de disponibilidad y el bloqueo de inventario; este caso de uso solo informa un hecho ya decidido.
- **Manejo de rechazo del receptor**: **no aplica**. No hay un flujo de "rechazo de la notificación" observable por el usuario final; un rechazo permanente se escala a DLQ con alerta para el equipo de integración.

---

## Requirements

### Functional Requirements

- **FR-001**: El sistema DEBE encolar una notificación para la API externa de Módulo 1 (`Asignar estado operativo`) cada vez que el caso de uso `Actualizar estado reserva` (`CU-08`) consolide una transición de estado de la reserva cuya tabla de mapeo (FR-002) indique que se notifica.
- **FR-002**: El sistema DEBE enviar el **estado operativo de la embarcación ya mapeado**, nunca el estado crudo de la reserva. La tabla de mapeo vigente es:

    | Estado de la reserva | ¿Se notifica a Módulo 1? | Estado operativo enviado | Detalle |
    |---|---|---|---|
    | Iniciada | No | — | Sin bloqueo operativo. |
    | Pendiente de pago | No | — | Estado transitorio gestionado íntegramente dentro de Módulo 2 (TTL de 15 minutos en curso y espera del resultado del cobro). Módulo 1 no participa de este estado: el bloqueo operativo en Módulo 1 ocurre únicamente al llegar a `Reservada`. |
    | Reservada | Sí | Reservado | Bloqueo operativo de la embarcación. Es el único momento en que Módulo 1 recibe el estado `Reservado`, y ocurre una vez confirmado el pago. |
    | En navegación | Sí | En Navegación | Embarcación en uso. |
    | Completada | Sí | Señal de que debe pasar a Mantenimiento/Limpieza | Al completarse la navegación, la embarcación requiere limpieza antes de volver a estar disponible. Módulo 2 notifica "Completada" y es Módulo 1 quien gestiona internamente la transición a Mantenimiento/Limpieza y, después, a Disponible, según su propio proceso. |
    | Cancelada | Condicional | Señal de liberación — por defecto `Disponible`; salvo que la cancelación esté asociada a una avería mecánica reportada por el Propietario, en cuyo caso la señal es hacia `Mantenimiento/Limpieza` (igual que en el flujo de `Completada`) | Solo se notifica si la reserva ya había llegado previamente al estado Reservada (es decir, si Módulo 1 ya había sido notificado y tiene la embarcación marcada como ocupada). Esta condición aplica **igual** para la liberación normal y para la liberación con avería mecánica. Si la cancelación ocurre antes de llegar a Reservada (por ejemplo, durante Iniciada o Pendiente de pago), no se notifica a Módulo 1, ya que nunca llegó a marcarla como ocupada. |
    | Expirada | No | — | No se notifica a Módulo 1 bajo ninguna circunstancia. Como la reserva nunca alcanzó `Reservada`, Módulo 1 nunca la tuvo bloqueada y no hay nada que liberar. |
    | Pago Fallido | No | — | No se notifica a Módulo 1 bajo ninguna circunstancia. Como la reserva nunca alcanzó `Reservada`, Módulo 1 nunca la tuvo bloqueada y no hay nada que liberar. |

> **Esta tabla (FR-002) es la única fuente de verdad** del mapeo estado de la reserva → estado operativo notificado a Módulo 1. Cualquier otro documento del proyecto que necesite describir este mapeo debe **referenciar esta tabla** en lugar de mantener una copia propia con su propia granularidad (ver `CU-08` FR-007, que remite a esta tabla).

- **FR-003**: El *payload* de la notificación DEBE contener obligatoriamente: identificador de la embarcación, estado operativo resultante (ya mapeado, según FR-002) y marca temporal exacta del evento en formato ISO 8601.
- **FR-004**: El *payload* DEBE incluir un identificador único por transición (`eventId`, UUID), de modo que Módulo 1 pueda deduplicar entregas repetidas y procesar el evento de forma idempotente.
- **FR-005**: El identificador del evento DEBE generarse en la misma transacción local que consolida la transición de estado (patrón *outbox*), de forma que la transición y su notificación se confirmen juntas y el evento nunca se pierda silenciosamente.
- **FR-006**: El sistema DEBE implementar un mecanismo de entrega garantizada (cola de reintentos) para asegurar que las notificaciones alcancen Módulo 1 ante fallos temporales de red o timeouts. Esta cola gobierna tanto el camino feliz (primer intento) como el de fallo (reintentos con backoff); **no existe envío síncrono directo**. Política de reintento: máximo 5 intentos por evento (primer intento inmediato), backoff exponencial con jitter de 1 s, 5 s, 25 s y 125 s, solo ante 5xx o timeout (nunca ante un 4xx permanente); tras el quinto fallo, derivación a DLQ y alerta.
- **FR-007**: **REGLA ESTRICTA**: El sistema **NO DEBE** calcular, incluir ni transmitir montos, tarifas, penalidades, valores de reembolso ni ningún dato de liquidación o pago en la notificación. La notificación es puramente operativa; Módulo 1 interpreta el estado operativo y nada más.
- **FR-008**: El sistema **NO DEBE** esperar una respuesta de negocio de Módulo 1 ni alterar la transición local de la reserva en función del resultado de la notificación. La consistencia es eventual y se alcanza por reintento, no por reversión.
- **FR-009**: El sistema **NO DEBE** validar ni resolver conflictos de doble reserva en el marco de esta notificación. Esa prevención es responsabilidad interna de Módulo 2 y ocurre antes de confirmar la reserva.

---

### Decisiones de negocio resueltas

No quedan inconsistencias abiertas en este spec. Las dos que estaban pendientes quedaron resueltas por decisión única de negocio (2026-10-22):

1. **Fila `Pendiente de pago` — RESUELTA (opción a)**: prevalece la regla de la tabla de FR-002: Módulo 1 **no** se notifica mientras la reserva está en `Pendiente de pago`. El bloqueo operativo en Módulo 1 (estado `Reservado`) ocurre **únicamente al entrar en `Reservada`**, es decir, una vez confirmado el pago. El TTL de 15 minutos y el propio estado `Pendiente de pago` se gestionan íntegramente dentro de Módulo 2, sin involucrar a Módulo 1 en ningún momento.
    - **Justificación de coherencia**: esta decisión es coherente con la regla ya vigente de que `Expirada` y `Pago Fallido` **no** se notifican a Módulo 1. Como Módulo 1 nunca se entera de que una reserva estaba en curso durante `Pendiente de pago`, no hay nada que liberar si esa reserva termina en `Expirada` o en `Pago Fallido`. La alternativa (notificar desde `Pendiente de pago`) obligaría además a notificar esos dos estados para liberar la embarcación, contradiciendo la regla ya establecida.
    - **Documentos corregidos en cascada**: `CU-08` FR-007 y sus escenarios, `CU-08` plan (contrato con Módulo 1), `context/consistencia-m2-m3.md` §2 (relato, diagrama, tabla de estados y vocabulario), `CU-03` plan/spec, `CU-02` plan/spec, `CU-04` plan/spec (condición "solo si ya había llegado a Reservada" y excepción de avería mecánica), `CU-07` plan/spec (señal de `Completada` hacia Mantenimiento/Limpieza).
2. **Granularidad del mapeo — RESUELTA**: la tabla de **FR-002 de este spec es la fuente de verdad única** del mapeo estado de la reserva → estado operativo notificado a Módulo 1. `CU-08` FR-007 deja de mantener una tabla propia y **remite a esta tabla**; el resto de documentos que describen el mapeo hacen lo mismo.

---

### Key Entities

- **Notificación de Estado Operativo (`OperationalStatusEvent`)**: DTO (Data Transfer Object) de integración saliente que empaqueta la transición de estado ya traducida al dominio de flota, para consumo por Módulo 1. Contiene: identificador de la embarcación, estado operativo resultante, marca temporal del evento (ISO 8601) e identificador único de transición.
- **Estado Operativo de Embarcación (`OperationalStatus`)**: conjunto de cuatro estados que administra Módulo 1 (`Disponible`, `Reservado`, `En Navegación`, `En Mantenimiento/Limpieza`). Es una **entidad distinta** del estado de la reserva; este caso de uso solo transmite su valor.
- **Embarcación**: activo registrado en Módulo 1, referenciado en Módulo 2 únicamente por su identificador (Módulo 2 no accede a su base de datos ni persiste sus datos técnicos).

---

## Success Criteria

### Measurable Outcomes

- **SC-001**: El 100% de las transiciones de reserva que la tabla de FR-002 marca como notificables son notificadas a Módulo 1 (0% de eventos perdidos gracias a la cola de reintentos).
- **SC-002**: El tiempo de emisión del primer intento de notificación no supera los 500 milisegundos tras la consolidación del estado en la base de datos local.
- **SC-003**: Cero (0) valores financieros, monetarios o de liquidación incluidos en el cuerpo del mensaje enviado a Módulo 1.
- **SC-004**: El 100% de los eventos emitidos lleva un identificador único por transición, de modo que la entrega duplicada sea detectable y deduplicable por el receptor.
- **SC-005**: Cero (0) transiciones de estado notificadas con el estado crudo de la reserva en lugar del estado operativo mapeado.
