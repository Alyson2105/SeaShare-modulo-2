# Feature Specification: Actualizar Estado de Reserva

**Módulo**: Módulo 2 (Operación de Reservas, Tiempos y Cancelaciones)  
**Created**: 2026-09-08 (Actualizado: integración del ciclo de pago e inicio en 'Iniciada')  
**Primary Actor**: Sistema / Se llama internamente (Es el motor que usan los demás casos de uso de Módulo 2: `Iniciar reserva`, `Iniciar pago`, `Confirmar pago`, `Marcar inicio de la navegación`, `Marcar fin de navegacion`, `Solicitar cancelación`, `Marcar inasistencia`, y el temporizador de expiración TTL de 15 minutos)  
**External Dependencies (APIs)**:
- **Módulo 1 (Gestión de Flota y Activos P2P)**: API externa (`Asignar estado operativo`) para mantener sincronizado el estado operativo de la embarcación (`Disponible`, `Reservado`, `En Navegación`, `En Mantenimiento/Limpieza`).
- **Módulo 3 (Liquidación, Seguros y Dispersión de Fondos)**: API externa (`Recibir estado de reserva`) a la que Módulo 2 le avisa cada vez que la reserva cambia de estado o sub-estado (incluyendo cuando hay que reportar un incidente para gestionar el depósito de garantía y la liquidación).

**Casos de uso internos de Módulo 2**:
- `Recibir Estado de Reserva` (`CU-14`, `<<include>>`): notificación asíncrona a Módulo 3. Ver flujos alternos por estado destino para su condición de disparo.
- `Recibir información de estado operativo` (`CU-22`, `<<include>>`): notificación asíncrona a Módulo 1 con el estado operativo de la embarcación **ya mapeado**, según las reglas definidas en CU-22. Ver flujos alternos por estado destino para su condición de disparo. El fallo o reintento de una de estas notificaciones es independiente de la otra.

> **Nota conceptual**: Aunque los `<<include>>` hacia CU-14 y CU-22 son **condicionales según el estado destino** de la transición, siguen siendo relaciones `<<include>>` y **no** `<<extend>>`. Dentro de cada flujo alterno donde aplican, la notificación es **obligatoria y automática** (no depende de una decisión del actor ni constituye un punto de extensión opcional). El diagrama de casos de uso puede mostrar la relación de forma genérica; la condición de ejecución se documenta explícitamente en los flujos alternos de este spec.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Ser el único lugar donde se aplican los cambios de estado válidos de la reserva (Priority: P1)

Cualquier caso de uso de Módulo 2 que necesite crear una reserva en su estado inicial (`Iniciada`) o cambiar su estado tiene que pasar obligatoriamente por "CU-08 Actualizar estado reserva" (`<<include>>`). El sistema verifica que el cambio pedido cumpla con las reglas de estados, asigna el estado principal y el sub-estado que corresponda, guarda el cambio de forma segura, registra la hora exacta y deja guardado el historial completo de la reserva.

**Why this priority**: Es la pieza central que mantiene todo consistente en el mundo de las reservas. Tener un único lugar para aplicar cambios evita estados inconsistentes, problemas de concurrencia y corrupción de datos en el ciclo de vida del alquiler.

**Independent Test**: Se puede probar aislando el motor de cambios de estado, preparando reservas en cada estado posible y pidiendo cambios permitidos, verificando que el nuevo estado/sub-estado quedan guardados correctamente con fecha y motivo.

#### Flujo principal (genérico)
1. **Solicitud**: Algún caso de uso transaccional solicita actualizar el estado de una reserva (o crearla en `Iniciada`).
2. **Validación**: El motor valida que la transición es permitida según la máquina de estados (FR-003).
3. **Aplicación**: Se actualiza el estado principal (y sub-estado si aplica) de forma atómica, con control de concurrencia (FR-006).
4. **Persistencia / auditoría**: Se guarda el cambio y su historial.
5. **Notificaciones externas (condicionales por estado destino)**: Se invocan los `<<include>>` hacia `CU-14` y/o `CU-22` **únicamente** si corresponden al estado destino, según la tabla de flujos alternos. Dentro del flujo alterno donde aplique, la notificación es obligatoria y automática.

#### Flujos alternos según el estado destino

| Estado destino | Include a `Recibir Estado de Reserva` (`CU-14`, Módulo 3) | Include a `Recibir información estado operativo` (`CU-22`, Módulo 1) | Notas |
|---|---|---|---|
| `Iniciada` (creación) | No | No | Sin bloqueo operativo. Sin notificación a Módulo 3 (integración con M3 inicia a partir de `Pendiente de Pago` según CU-14). |
| `Pendiente de Pago` | Sí | No | Integración con Módulo 3 inicia en `Pendiente de Pago`. No se notifica a Módulo 1: el bloqueo operativo ocurre al entrar en `Reservada` (ver CU-22 FR-002). |
| `Reservada` | Sí (con sub-estado si aplica) | Sí | Notificación a Módulo 1 con estado operativo mapeado `Reservado` según CU-22. |
| `En Navegación` | Sí | Sí | Notificación a Módulo 1 con estado operativo mapeado `En Navegación` según CU-22. |
| `Completada` | Sí | Sí | Notificación a Módulo 1: señal para pasar a Mantenimiento/Limpieza (gestión interna de Módulo 1). Ver CU-22 FR-002. |
| `Cancelada` | Sí (con sub-estado correspondiente: `Flexible`, `Moderado`, `Tardío`, `Por Propietario`, `Por Inasistencia`) | Condicional | Solo se notifica a Módulo 1 si la reserva ya había llegado previamente a `Reservada` (Módulo 1 ya la tenía marcada como ocupada). Si la cancelación ocurre antes de `Reservada` (Iniciada/Pendiente de pago), **no** se notifica. La señal es de liberación a `Disponible`, salvo que la cancelación esté asociada a una avería mecánica reportada por el Propietario, en cuyo caso es hacia Mantenimiento/Limpieza. Ver CU-22 FR-002. |
| `Expirada` | No (según reglas vigentes) | No | No se notifica a Módulo 1 bajo ninguna circunstancia. Ver CU-22 FR-002. |
| `Pago Fallido` | No (según reglas vigentes) | No | No se notifica a Módulo 1 bajo ninguna circunstancia. Ver CU-22 FR-002. |

**Acceptance Scenarios** (relevantes):

1. **Scenario**: Creación y primer registro de la reserva en estado Iniciada
    - **Given** un Arrendatario que presiona "Reservar" y comienza a llenar los datos del viaje (embarcación, fechas)
    - **When** el caso de uso `Iniciar reserva` pide la creación inicial
    - **Then** el sistema crea la reserva con estado principal "Iniciada" como único punto de entrada a la máquina de estados, enciende el temporizador TTL de 15 minutos. **No** se invocan `CU-14` ni `CU-22`.

2. **Scenario**: Transición a Pendiente de Pago sin notificación a Módulo 1
    - **Given** una reserva en estado "Iniciada" con su TTL en curso
    - **When** el caso de uso orquestador `Iniciar pago` invoca este motor para proceder al cobro
    - **Then** el sistema registra la reserva en estado "Pendiente de Pago" (el TTL sigue corriendo desde `Iniciada`, no se reinicia). Se invoca `CU-14` (`<<include>>`, obligatorio y automático). **No** se invoca `CU-22`: el bloqueo operativo en Módulo 1 se produce al entrar en `Reservada`, una vez confirmado el pago (ver CU-22 FR-002).

3. **Scenario**: Confirmación de la reserva tras aprobarse el pago
    - **Given** una reserva en estado principal "Pendiente de Pago"
    - **When** el caso de uso `Confirmar pago` pide actualizar a estado "Reservada"
    - **Then** el sistema verifica que el cambio es válido, actualiza el estado principal a "Reservada", guarda la hora del cambio. Se invocan `CU-14` y `CU-22` (`<<include>>`, obligatorios y automáticos en este flujo alterno).

4. **Scenario**: Inicio del servicio de navegación (Check-in)
    - **Given** una reserva en estado principal "Reservada"
    - **When** el caso de uso `Marcar inicio de la navegación` pide el cambio de estado
    - **Then** el sistema verifica el cambio y actualiza el estado principal de la reserva a "En Navegación". Se invocan `CU-14` y `CU-22`.

5. **Scenario**: Cierre del servicio (Check-out)
    - **Given** una reserva en estado principal "En Navegación"
    - **When** el caso de uso `Marcar fin de navegacion` avisa que el servicio terminó
    - **Then** el sistema actualiza el estado principal a "Completada" y guarda el texto de novedades si el Propietario lo proveyó. Se invoca `CU-14`. Para `CU-22` se aplica CU-22 FR-002: Módulo 2 notifica `Completada`, lo que genera en Módulo 1 una señal hacia Mantenimiento/Limpieza; la transición final a `Disponible` la gestiona Módulo 1 internamente.

6. **Scenario**: Cancelación voluntaria de una reserva en estado Reservada, o por inasistencia
    - **Given** una reserva en estado principal "Reservada"
    - **When** `Solicitar cancelación` o `Marcar inasistencia` piden el cambio, aportando el tipo o motivo correspondiente
    - **Then** el sistema actualiza el estado principal a "Cancelada" y asigna el sub-estado que corresponda (`Flexible`, `Moderado`, `Tardío`, `Por Propietario` o `Por Inasistencia`). Se invoca `CU-14` con sub-estado. Para `CU-22` se aplica CU-22 FR-002: la señal es de liberación a `Disponible`, salvo que la cancelación esté asociada a una avería mecánica reportada por el Propietario, en cuyo caso es hacia Mantenimiento/Limpieza; en ambos casos solo se notifica si la reserva ya había llegado a `Reservada`.

7. **Scenario**: Expiración automática al cumplirse los 15 minutos
    - **Given** una reserva en estado principal "Iniciada" o "Pendiente de Pago" cuyo temporizador TTL de 15 minutos ya venció sin confirmación de pago
    - **When** el temporizador interno del sistema dispara el cambio
    - **Then** el sistema actualiza el estado principal a "Expirada" de forma segura y completa (transiciones válidas: `Iniciada → Expirada` y `Pendiente de Pago → Expirada`). No se dispara `CU-22`: `Expirada` no se notifica a Módulo 1 bajo ninguna circunstancia (ver CU-22 FR-002), porque la reserva nunca alcanzó `Reservada` y por tanto nunca se bloqueó el inventario en Módulo 1. Cualquier notificación a Módulo 3 queda sujeta a lo definido en `CU-14`.

8. **Scenario**: Notificación de pago fallido o rechazado definitivo desde CU-13
    - **Given** una reserva en estado principal "Pendiente de Pago"
    - **When** el caso de uso `Confirmar pago` (CU-13) procesa un resultado de pago rechazado o fallido definitivo
    - **Then** el sistema actualiza el estado principal a "Pago Fallido", guardando el motivo de rechazo. No se libera la embarcación en Módulo 1, porque nunca se notificó su bloqueo: `Pago Fallido` no se notifica a Módulo 1 bajo ninguna circunstancia (ver CU-22 FR-002). Cualquier notificación a Módulo 3 queda sujeta a lo definido en `CU-14`.

---

### User Story 2 - Mantener sincronizado el estado operativo de la embarcación en Módulo 1 (Priority: P1)

En los momentos clave del ciclo de vida de la reserva (desde que el pago se confirma y la reserva alcanza `Reservada`), el sistema le avisa de inmediato a la API `Asignar estado operativo` de Módulo 1 para actualizar el estado operativo de la embarcación (`Disponible`, `Reservado`, `En Navegación`, `En Mantenimiento/Limpieza`). El mapeo exacto de cada estado de reserva al estado operativo notificado está definido en **CU-22 FR-002**, que es su **única fuente de verdad**.

**Why this priority**: Asegura que el inventario físico en el muelle coincida exactamente con los compromisos de pago y navegación en la plataforma.

**Independent Test**: Se puede probar simulando la API `Asignar estado operativo` de Módulo 1, provocando cambios de estado de reserva y verificando que Módulo 1 recibe el aviso correcto en los estados pertinentes: ningún aviso al nacer en `Iniciada` ni al entrar en `Pendiente de Pago`, y aviso de bloqueo al entrar en `Reservada`.

**Acceptance Scenarios**:

1. **Scenario**: Al confirmar el pago, la embarcación queda bloqueada como "Reservado"
    - **Given** una reserva que pasa de "Pendiente de Pago" a "Reservada" (al confirmarse el pago)
    - **When** se procesa la actualización de estado en este motor
    - **Then** el sistema llama a la API `Asignar estado operativo` de Módulo 1 enviando el estado "Reservado" para esa embarcación

2. **Scenario**: Al iniciar la navegación, la embarcación pasa a "En Navegación"
    - **Given** una reserva que pasa al estado principal "En Navegación"
    - **When** se guarda ese cambio
    - **Then** el sistema llama a la API `Asignar estado operativo` de Módulo 1 enviando el estado operativo "En Navegación"

3. **Scenario**: El cierre normal libera hacia Mantenimiento/Limpieza, la cancelación ordinaria libera a "Disponible" y la expiración o el pago fallido no notifican nada
    - **Given** una reserva que pasa a "Completada", a "Cancelada" (sin avería), a "Pago Fallido" o a "Expirada" desde "Pendiente de Pago"
    - **When** se procesa el cambio de estado
    - **Then** para "Completada" el sistema llama a la API `Asignar estado operativo` de Módulo 1 con la señal que lleva la embarcación a "En Mantenimiento/Limpieza" (la transición final a "Disponible" la gestiona Módulo 1 internamente), y para "Cancelada" sin avería la llama con la señal de liberación a "Disponible"; en cambio, para "Pago Fallido" y para "Expirada" desde "Pendiente de Pago" el sistema NO llama a Módulo 1, porque la reserva nunca alcanzó "Reservada" y por tanto nunca tuvo bloqueado el inventario en Módulo 1 (ver CU-22 FR-002)

4. **Scenario**: Expiración desde Iniciada no notifica a Módulo 1
    - **Given** una reserva en estado "Iniciada" que expira por vencimiento del TTL pasando a "Expirada"
    - **When** se procesa el cambio de estado
    - **Then** el sistema NO llama a la API `Asignar estado operativo` de Módulo 1 (nunca hubo bloqueo previo en Módulo 1; notificarlo liberaría indebidamente el bloqueo de otra reserva vigente)

5. **Scenario**: Cancelación por avería reportada por el Propietario pasa la embarcación a "En Mantenimiento/Limpieza"
    - **Given** una reserva en estado "Reservada" cancelada por el Propietario reportando avería o desperfecto mecánico
    - **When** se procesa la transición a "Cancelada" con sub-estado "Por Propietario"
    - **Then** el sistema llama a la API `Asignar estado operativo` de Módulo 1 enviando el estado operativo "En Mantenimiento/Limpieza"

---

### User Story 3 - Avisarle a Módulo 3 sobre los cambios de estado y los incidentes (Priority: P1)

Cada vez que cambia el estado o sub-estado de una reserva a partir de su consolidación, el sistema DEBE avisarle a la API externa de Módulo 3 (`Recibir estado de reserva`).

**Why this priority**: Módulo 3 necesita enterarse en tiempo real de lo que pasa para activar seguros, retener o devolver depósitos de garantía, y procesar reembolsos.

**Independent Test**: Se prueba simulando eventos de transición y validando que las peticiones salientes hacia Módulo 3 incluyan los datos correctos del estado y sub-estado.

**Acceptance Scenarios**:

1. **Scenario**: Aviso exitoso de transición a Módulo 3
    - **Given** una reserva que experimenta un cambio de estado válido gestionado por este motor
    - **When** el cambio se consolida en base de datos
    - **Then** el sistema emite una notificación hacia la API externa `Recibir estado de reserva` de Módulo 3

---

### User Story 4 - Rechazar por completo los cambios no permitidos y mantener intactos los estados finales (Priority: P2)

Si llega un pedido de cambio de estado que no está permitido por la máquina de estados, el sistema tiene que rechazar la solicitud sin excepciones.

**Why this priority**: Protege la integridad transaccional de la plataforma.

**Independent Test**: Se prueba enviando comandos de transición prohibidos (ej. cancelar una reserva en `Pendiente de Pago`) y verificando que el motor los rechaza.

**Acceptance Scenarios**:

1. **Scenario**: Intento de cancelar una reserva en Pendiente de Pago
    - **Given** una reserva en estado principal "Pendiente de Pago"
    - **When** llega una solicitud de cancelación voluntaria
    - **Then** el sistema rechaza la solicitud explicando que solo las reservas en estado `Reservada` admiten cancelación activa; la reserva en Pendiente de Pago debe expirar pasivamente por TTL.

---

### Edge Cases

- **Sin bloqueo antes del pago (Condición de carrera pre-pago)**: Dado que en estado `Iniciada` no hay retención de la embarcación, es posible que dos Arrendatarios distintos tengan reservas en `Iniciada` para el mismo barco y las mismas fechas simultáneamente. El primer usuario que complete `Iniciar pago` transicionará su reserva a `Pendiente de Pago` y ganará la exclusividad **interna de Módulo 2** (registros de disponibilidad de Módulo 2 y bloqueo pesimista de fila), que es la que evita la doble reserva. Como en `Pendiente de Pago` no se notifica a Módulo 1, el estado operativo en Módulo 1 todavía refleja la embarcación como disponible; el bloqueo en Módulo 1 se materializa únicamente cuando la reserva alcanza `Reservada`, tras la confirmación del pago.
- **"Pendiente de Pago" no se puede cancelar por voluntad propia**: La reserva se libera solo de forma pasiva cuando vence el temporizador TTL de 15 minutos.
- **Falla pasajera de conexión con Módulo 1 o 3**: Mecanismo de cola y reintentos (0% de eventos perdidos). Política de reintento: máximo 5 intentos por evento (primer intento inmediato), backoff exponencial con jitter de 1 s, 5 s, 25 s y 125 s entre intentos; solo se reintenta ante 5xx o timeout, nunca ante un 4xx permanente; tras el quinto fallo el evento se deriva a la cola de letras muertas (DLQ) y se dispara una alerta.
- **Los estados finales no se pueden tocar nunca más**: `Completada`, `Cancelada`, `Expirada` y `Pago Fallido` son definitivos.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE ser el único lugar donde se actualiza el estado y sub-estado de cualquier reserva en Módulo 2, invocado obligatoriamente mediante relaciones `<<include>>` por los casos de uso transaccionales (incluyendo `Iniciar pago`).
- **FR-002**: El sistema DEBE seguir esta lista oficial de estados de la reserva:
    - **Estados Principales de la Reserva**: `Iniciada`, `Pendiente de Pago`, `Reservada`, `En Navegación`, `Completada`, `Cancelada`, `Expirada`, `Pago Fallido`. El estado inicial de toda reserva al crearse es siempre `Iniciada` (disparado únicamente por `Iniciar reserva` al presionar "Reservar"). `Iniciada`, `Pendiente de Pago`, `En Navegación`, `Expirada` y `Pago Fallido` no tienen sub-estados.
    - **Sub-estados de Cancelación**: `Flexible`, `Moderado`, `Tardío`, `Por Propietario`, `Por Inasistencia`.
    - **Estados terminales**: `Completada`, `Cancelada`, `Expirada` y `Pago Fallido`.
- **FR-003**: El sistema DEBE verificar de forma estricta que el cambio de estado pedido sea uno de los permitidos:
    - `Creación → Iniciada`: única forma de entrar a la máquina de estados, disparada por `Iniciar reserva` al presionar "Reservar".
    - `Iniciada → Pendiente de Pago`: disparada por `Iniciar pago`.
    - `Iniciada → Expirada`: ocurre cuando el TTL de 15 minutos vence sin que se haya iniciado el pago.
    - `Pendiente de Pago → Reservada`, `Expirada` o `Pago Fallido`.
    - `Reservada → En Navegación` o `Cancelada`.
    - `En Navegación → Completada`.
- **FR-004**: Si el cambio pedido es válido, el sistema DEBE actualizar y guardar de forma atómica el estado principal.
- **FR-005**: Si el cambio pedido no es válido, el sistema DEBE rechazar la solicitud.
- **FR-006**: El sistema DEBE tener control de concurrencia para evitar que dos solicitudes de actualización sobre la misma reserva choquen. La estrategia es el bloqueo optimista con columna de versión (`@Version`): ante dos escrituras concurrentes sobre la misma reserva, la segunda recibe un conflicto y se rechaza (HTTP 409) sin sobrescribir el estado consolidado. La validación atómica de disponibilidad del inventario en `Iniciar pago` (CU-03) utiliza bloqueo pesimista de fila.
- **FR-007**: El sistema DEBE sincronizar el estado operativo con Módulo 1 conforme al mapeo definido en la tabla de **FR-002 de `CU-22` (Recibir información estado operativo)**, que es la **única fuente de verdad** de esa correspondencia. Este spec **no** mantiene una copia propia del mapeo: cualquier detalle (condiciones, excepciones y estados operativos enviados) debe consultarse en `CU-22` FR-002. En particular, y conforme a esa tabla: el bloqueo operativo en Módulo 1 (estado `Reservado`) ocurre **únicamente al entrar en `Reservada`** (pago confirmado); en `Iniciada` y en `Pendiente de Pago` no se notifica nada a Módulo 1 (el TTL de 15 minutos y el estado de pago pendiente se gestionan íntegramente dentro de Módulo 2); y `Expirada` y `Pago Fallido` no se notifican bajo ninguna circunstancia.
- **FR-008**: El sistema DEBE llamar a la API externa de Módulo 3 (`Recibir estado de reserva`) ante cada cambio de estado a partir de `Pendiente de Pago` (inclusive).
- **FR-009**: **Texto de novedades en el cierre**: Si el cierre de la navegación incluye el texto opcional de novedades provisto por el Propietario, el sistema DEBE adjuntarlo como campo informativo en la notificación de `Completada` a Módulo 3, sin que ello modifique el tratamiento del cierre.
- **FR-010**: **REGLA DE NEGOCIO ESTRICTA (Sin cálculo financiero):** Módulo 2 **NO DEBE** calcular montos ni hacer transferencias de dinero. Toda valoración económica es de Módulo 3.
- **FR-011**: Cada evento saliente hacia Módulo 3 DEBE portar un identificador único de evento (`eventId`, UUID) generado en la misma transacción local que la transición de estado (patrón outbox). La entrega es *at-least-once*; la deduplicación de eventos duplicados corresponde a Módulo 3 mediante ese `eventId`.

---

### Key Entities

- **Reserva (`Reservation`)**: Entidad principal.
- **Historial de Cambios de Estado (`ReservationStatusAudit`)**: Registro del historial de vida.
- **Embarcación**: Barco físico referenciado externamente en Módulo 1.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Cero (0%) cambios de estado no permitidos.
- **SC-002**: El tiempo de emisión del primer intento de aviso a APIs externas no supera los 500 milisegundos tras la consolidación del estado en la base de datos local.
- **SC-003**: Cero (0%) embarcaciones bloqueadas físicamente en Módulo 1 sin que exista una reserva en estado `Reservada` o `En Navegación` que respalde el bloqueo (Módulo 1 solo conoce las reservas que alcanzaron `Reservada`).
