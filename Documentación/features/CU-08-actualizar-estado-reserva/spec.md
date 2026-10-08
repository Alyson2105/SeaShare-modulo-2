# Feature Specification: Actualizar Estado de Reserva

**Módulo**: Módulo 2 (Operación de Reservas, Tiempos y Cancelaciones)  
**Created**: 2026-09-08 (Actualizado: integración del ciclo de pago e inicio en 'Iniciada')  
**Primary Actor**: Sistema / Se llama internamente (Es el motor que usan los demás casos de uso de Módulo 2: `Iniciar reserva`, `Iniciar pago`, `Confirmar pago`, `Marcar inicio de la navegación`, `Marcar fin de navegacion`, `Solicitar cancelación`, `Marcar inasistencia`, y el temporizador de expiración TTL de 15 minutos)  
**External Dependencies (APIs)**:
- **Módulo 1 (Gestión de Flota y Activos P2P)**: API externa (`Asignar estado operativo`) para mantener sincronizado el estado operativo de la embarcación (`Disponible`, `Reservado`, `En Navegación`, `En Mantenimiento/Limpieza`).
- **Módulo 3 (Liquidación, Seguros y Dispersión de Fondos)**: API externa (`Recibir estado de reserva`) a la que Módulo 2 le avisa cada vez que la reserva cambia de estado o sub-estado (incluyendo cuando hay que reportar un incidente para gestionar el depósito de garantía y la liquidación).

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Ser el único lugar donde se aplican los cambios de estado válidos de la reserva (Priority: P1)

Cualquier caso de uso de Módulo 2 que necesite crear una reserva en su estado inicial (`Iniciada`) o cambiar su estado tiene que pasar obligatoriamente por "CU-08 Actualizar estado reserva" (`<<include>>`). El sistema verifica que el cambio pedido cumpla con las reglas de estados, asigna el estado principal y el sub-estado que corresponda, guarda el cambio de forma segura, registra la hora exacta y deja guardado el historial completo de la reserva.

**Why this priority**: Es la pieza central que mantiene todo consistente en el mundo de las reservas. Tener un único motor de cambios evita estados inconsistentes, problemas cuando dos cosas pasan al mismo tiempo, y datos dañados en el ciclo de vida del alquiler.

**Independent Test**: Se puede probar aislando el motor de cambios de estado, preparando reservas en cada estado posible y pidiendo cambios permitidos (por ejemplo, `Creación` → `Iniciada`, `Iniciada` → `Pendiente de Pago`, `Pendiente de Pago` → `Reservada`, `Reservada` → `En Navegación`, `En Navegación` → `Completada`, `Reservada` → `Cancelada`), y verificando que el nuevo estado y sub-estado quedan guardados correctamente con fecha y motivo.

**Acceptance Scenarios**:

1. **Scenario**: Creación y primer registro de la reserva en estado Iniciada
    - **Given** un Arrendatario que presiona "Reservar" y comienza a llenar los datos del viaje (embarcación, fechas)
    - **When** el caso de uso `Iniciar reserva` pide la creación inicial
    - **Then** el sistema crea la reserva con estado principal "Iniciada" como único punto de entrada a la máquina de estados, enciende el temporizador TTL de 15 minutos y no notifica bloqueo a Módulo 1

2. **Scenario**: Transición a Pendiente de Pago e inicio del bloqueo operativo vía Iniciar Pago
    - **Given** una reserva en estado "Iniciada" con su TTL en curso
    - **When** el caso de uso orquestador `Iniciar pago` invoca este motor para proceder al cobro
    - **Then** el sistema registra la reserva en estado "Pendiente de Pago" (el TTL sigue corriendo desde `Iniciada`, no se reinicia) y le avisa a Módulo 1 (`Asignar estado operativo`) para poner la embarcación en "Reservado"

3. **Scenario**: Confirmación de la reserva tras aprobarse el pago
    - **Given** una reserva en estado principal "Pendiente de Pago"
    - **When** el caso de uso `Confirmar pago` pide actualizar a estado "Reservada"
    - **Then** el sistema verifica que el cambio es válido, actualiza el estado principal a "Reservada", guarda la hora del cambio y avisa a los módulos externos

4. **Scenario**: Inicio del servicio de navegación (Check-in)
    - **Given** una reserva en estado principal "Reservada"
    - **When** el caso de uso `Marcar inicio de la navegación` pide el cambio de estado
    - **Then** el sistema verifica el cambio y actualiza el estado principal de la reserva a "En Navegación"

5. **Scenario**: Cierre del servicio (Check-out)
    - **Given** una reserva en estado principal "En Navegación"
    - **When** el caso de uso `Marcar fin de navegacion` avisa que el servicio terminó
    - **Then** el sistema actualiza el estado principal a "Completada" y guarda el texto de novedades si el Propietario lo proveyó

6. **Scenario**: Cancelación voluntaria de una reserva en estado Reservada, o por inasistencia
    - **Given** una reserva en estado principal "Reservada"
    - **When** `Solicitar cancelación` o `Marcar inasistencia` piden el cambio, aportando el tipo o motivo correspondiente
    - **Then** el sistema actualiza el estado principal a "Cancelada" y asigna el sub-estado que corresponda (`Flexible`, `Moderado`, `Tardío`, `Por Propietario` o `Por Inasistencia`)

7. **Scenario**: Expiración automática al cumplirse los 15 minutos
    - **Given** una reserva en estado principal "Iniciada" o "Pendiente de Pago" cuyo temporizador TTL de 15 minutos ya venció sin confirmación de pago
    - **When** el temporizador interno del sistema dispara el cambio
    - **Then** el sistema actualiza el estado principal a "Expirada" de forma segura y completa (transiciones válidas: `Iniciada → Expirada` y `Pendiente de Pago → Expirada`)

8. **Scenario**: Notificación de pago fallido o rechazado definitivo desde CU-13
    - **Given** una reserva en estado principal "Pendiente de Pago"
    - **When** el caso de uso `Confirmar pago` (CU-13) procesa un resultado de pago rechazado o fallido definitivo
    - **Then** el sistema actualiza el estado principal a "Pago Fallido", guardando el motivo de rechazo y liberando la embarcación en Módulo 1

---

### User Story 2 - Mantener sincronizado el estado operativo de la embarcación en Módulo 1 (Priority: P1)

En los momentos clave del ciclo de vida de la reserva (a partir de que hay intención real de pago), el sistema le avisa de inmediato a la API `Asignar estado operativo` de Módulo 1 para actualizar el estado operativo de la embarcación (`Disponible`, `Reservado`, `En Navegación`, `En Mantenimiento/Limpieza`).

**Why this priority**: Asegura que el inventario físico en el muelle coincida exactamente con los compromisos de pago y navegación en la plataforma.

**Independent Test**: Se puede probar simulando la API `Asignar estado operativo` de Módulo 1, provocando cambios de estado de reserva y verificando que Módulo 1 recibe el aviso correcto en los estados pertinentes: ningún aviso al nacer en `Iniciada`, y aviso de bloqueo al entrar a `Pendiente de Pago`.

**Acceptance Scenarios**:

1. **Scenario**: Al iniciar el pago, la embarcación queda bloqueada como "Reservado"
    - **Given** una reserva que pasa de "Iniciada" a "Pendiente de Pago" (al dispararse `Iniciar pago`)
    - **When** se procesa la actualización de estado en este motor
    - **Then** el sistema llama a la API `Asignar estado operativo` de Módulo 1 enviando el estado "Reservado" para esa embarcación

2. **Scenario**: Al iniciar la navegación, la embarcación pasa a "En Navegación"
    - **Given** una reserva que pasa al estado principal "En Navegación"
    - **When** se guarda ese cambio
    - **Then** el sistema llama a la API `Asignar estado operativo` de Módulo 1 enviando el estado operativo "En Navegación"

3. **Scenario**: Un cierre normal, cancelación ordinaria, expiración desde Pendiente de Pago o pago fallido liberan la embarcación a "Disponible"
    - **Given** una reserva que pasa a "Completada", a "Cancelada" (sin avería), a "Pago Fallido" o a "Expirada" desde "Pendiente de Pago"
    - **When** se procesa el cambio de estado
    - **Then** el sistema llama a la API `Asignar estado operativo` de Módulo 1 enviando el estado operativo "Disponible"

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

- **Sin bloqueo antes del pago (Condición de carrera pre-pago)**: Dado que en estado `Iniciada` no hay retención de la embarcación, es posible que dos Arrendatarios distintos tengan reservas en `Iniciada` para el mismo barco y las mismas fechas simultáneamente. El primer usuario que complete `Iniciar pago` transicionará su reserva a `Pendiente de Pago` y ganará el bloqueo en Módulo 1 (`Reservado`). Si el segundo usuario intenta `Iniciar pago` después, Módulo 2 validará la disponibilidad en Módulo 1, descubrirá que ya está reservado por el primero y rechazará la transición a `Pendiente de Pago`.
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
- **FR-007**: El sistema DEBE sincronizar el estado operativo con Módulo 1 bajo las siguientes reglas:
    - `Creación a Iniciada`: NO se notifica bloqueo a Módulo 1.
    - `Iniciada a Pendiente de Pago`: llamar a `Asignar estado operativo` (poner en `Reservado`).
    - Pasa a `En Navegación`: actualizar a `En Navegación`.
    - Pasa a `Expirada` desde `Pendiente de Pago`, o pasa a `Completada`, `Pago Fallido` o `Cancelada` (sin reporte de avería): actualizar a `Disponible`.
    - `Iniciada a Expirada`: NO se notifica a Módulo 1 (nunca hubo bloqueo en Módulo 1; notificarlo liberaría erróneamente el bloqueo de otra reserva vigente, provocando doble booking).
    - Pasa a `Cancelada` con reporte de avería o desperfecto mecánico por parte del Propietario: actualizar a `En Mantenimiento/Limpieza` (inhabilitando temporalmente el activo).
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
- **SC-003**: Cero (0%) embarcaciones bloqueadas físicamente en Módulo 1 sin que exista una reserva en estado `Pendiente de Pago`, `Reservada` o `En Navegación` que respalde el bloqueo.
