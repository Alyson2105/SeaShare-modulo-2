# Feature Specification: Actualizar Estado de Reserva

**Módulo**: Módulo 2 (Operación de Reservas, Tiempos y Cancelaciones)  
**Created**: 2026-09-08 (Actualizado con estado Borrador)  
**Primary Actor**: Sistema / Se llama internamente (Es el motor que usan los demás casos de uso de Módulo 2: `Iniciar reserva`, `Iniciar pago`, `Confirmar pago`, `Marcar inicio de la navegación`, `Marcar fin de navegacion`, `Solicitar cancelación`, `Marcar inasistencia`, y el temporizador de expiración TTL de 15 minutos)  
**External Dependencies (APIs)**:
- **Módulo 1 (Gestión de Flota y Activos P2P)**: API externa (`Asignar estado operativo`) para mantener sincronizado el estado operativo de la embarcación (`Disponible`, `Reservado`, `En Navegación`, `En Mantenimiento/Limpieza`).
- **Módulo 3 (Liquidación, Seguros y Dispersión de Fondos)**: API externa (`Recibir estado de reserva`) a la que Módulo 2 le avisa cada vez que la reserva cambia de estado o sub-estado (incluyendo cuando hay que reportar un incidente para gestionar el depósito de garantía y la liquidación).

---

## User Scenarios & Testing

### User Story 1 - Ser el único lugar donde se aplican los cambios de estado válidos de la reserva (Priority: P1)

Cualquier caso de uso de Módulo 2 que necesite registrar el nacimiento de una reserva o cambiar su estado tiene que pasar obligatoriamente por "Actualizar estado reserva" (`<<include>>`). El sistema verifica que el cambio pedido cumpla con las reglas de estados, asigna el estado principal y el sub-estado que corresponda, guarda el cambio de forma segura, registra la hora exacta y deja guardado el historial completo de la reserva.

***Why this priority***: Es la pieza central que mantiene todo consistente en el mundo de las reservas. Tener un único motor de cambios evita estados inconsistentes, problemas cuando dos cosas pasan al mismo tiempo, y datos dañados en el ciclo de vida del alquiler.

***Independent Test***: Se puede probar aislando el motor de cambios de estado, preparando reservas en cada estado posible y pidiendo cambios permitidos (por ejemplo, `Creación` → `Borrador`, `Borrador` → `Pendiente de Pago`, `Pendiente de Pago` → `Confirmada`, `Confirmada` → `En Navegación`, `En Navegación` → `Completado` con sub-estados, `Confirmada` → `Cancelado`), y verificando que el nuevo estado y sub-estado quedan guardados correctamente con fecha y motivo.

***Acceptance Scenarios***:

1. **Scenario**: Creación y primer registro de la reserva en estado Borrador
    - **Given** una embarcación disponible según Módulo 1 y una solicitud de configuración de fechas y pasajeros válida
    - **When** el caso de uso "Iniciar reserva" pide la transición inicial
    - **Then** el sistema crea la reserva con estado principal "Borrador" (sin bloquear aún la embarcación en Módulo 1)

2. **Scenario**: Transición a Pendiente de Pago e inicio del bloqueo operativo
    - **Given** una reserva en estado "Borrador"
    - **When** el caso de uso "Iniciar pago" pide la transición de estado porque el usuario decidió proceder al cobro
    - **Then** el sistema actualiza el estado a "Pendiente de Pago", enciende el temporizador TTL de 15 minutos y le avisa a Módulo 1 (`Asignar estado operativo`) para poner la embarcación en "Reservado"[cite: 2]

3. **Scenario**: Confirmación de la reserva tras aprobarse el pago
    - **Given** una reserva en estado principal "Pendiente de Pago"
    - **When** el caso de uso "Confirmar pago" pide actualizar a estado "Confirmada"
    - **Then** el sistema verifica que el cambio es válido, actualiza el estado principal a "Confirmada", guarda la hora del cambio y empieza a avisarle a los módulos externos

4. **Scenario**: Inicio del servicio de navegación (Check-in)
    - **Given** una reserva en estado principal "Confirmada"
    - **When** el caso de uso "Marcar inicio de la navegación" pide el cambio de estado
    - **Then** el sistema verifica el cambio y actualiza el estado principal de la reserva a "En Navegación"

5. **Scenario**: Cierre exitoso del servicio sin problemas (Check-out)
    - **Given** una reserva en estado principal "En Navegación"
    - **When** el caso de uso "Marcar fin de navegacion" avisa que el servicio terminó sin novedades
    - **Then** el sistema actualiza el estado principal a "Completado" con el sub-estado "Sin incidentes"

6. **Scenario**: Cierre del servicio con novedades o averías
    - **Given** una reserva en estado principal "En Navegación"
    - **When** el caso de uso "Marcar fin de navegacion" avisa que el servicio terminó con daños o incidencias
    - **Then** el sistema actualiza el estado principal a "Completado" con el sub-estado "Con incidentes"

7. **Scenario**: Cancelación voluntaria de una reserva confirmada, o por inasistencia
    - **Given** una reserva en estado principal "Confirmada"
    - **When** "Solicitar cancelación" o "Marcar inasistencia" piden el cambio, aportando el tipo o motivo correspondiente
    - **Then** el sistema actualiza el estado principal a "Cancelado" y asigna el sub-estado que corresponda ("Flexible", "Moderado", "Tardío", "Por Anfitrión" o "Por Inasistencia")

8. **Scenario**: Expiración automática al cumplirse los 15 minutos
    - **Given** una reserva en estado principal "Pendiente de Pago" cuyo temporizador TTL de 15 minutos ya venció sin que se confirmara el pago
    - **When** el temporizador interno del sistema dispara el cambio
    - **Then** el sistema actualiza el estado principal a "Expirado" de forma segura y completa

---

### User Story 2 - Mantener sincronizado el estado operativo de la embarcación en Módulo 1 (Priority: P1)

En los momentos clave del ciclo de vida de la reserva (a partir de que hay intención real de pago), el sistema le avisa de inmediato a la API `Asignar estado operativo` de Módulo 1 para actualizar el estado operativo de la embarcación (`Disponible`, `Reservado`, `En Navegación`, `En Mantenimiento/Limpieza`).

***Why this priority***: Asegura que el inventario físico en el muelle coincida exactamente con los compromisos de pago y navegación en la plataforma.

***Independent Test***: Se puede probar simulando la API `Asignar estado operativo` de Módulo 1, provocando cambios de estado de reserva y verificando que Módulo 1 recibe el aviso correcto en los estados pertinentes, y que se omite intencionalmente cuando nace en `Borrador`.

***Acceptance Scenarios***:

1. **Scenario**: Al iniciar el pago, la embarcación queda bloqueada como "Reservado"
    - **Given** una reserva que pasa de "Borrador" a "Pendiente de Pago" (al dispararse "Iniciar pago")
    - **When** se procesa la actualización de estado
    - **Then** el sistema llama a la API `Asignar estado operativo` de Módulo 1 enviando el estado "Reservado" para esa embarcación

2. **Scenario**: Al iniciar la navegación, la embarcación pasa a "En Navegación"
    - **Given** una reserva que pasa al estado principal "En Navegación"
    - **When** se guarda ese cambio en Módulo 2
    - **Then** el sistema llama a la API `Asignar estado operativo` de Módulo 1 enviando el estado operativo "En Navegación"

3. **Scenario**: Un cierre normal, cancelación o expiración liberan la embarcación a "Disponible"
    - **Given** una reserva que pasa a "Completado" (Sin incidentes), a "Cancelado" o a "Expirado"
    - **When** se procesa el cambio de estado
    - **Then** el sistema llama a la API `Asignar estado operativo` de Módulo 1 enviando el estado operativo "Disponible"

4. **Scenario**: Un cierre con avería envía la embarcación a mantenimiento
    - **Given** una reserva que pasa a "Completado" (Con incidentes) o cancelación por anfitrión reportando avería
    - **When** se guarda el cambio de estado
    - **Then** el sistema llama a la API de Módulo 1 enviando el estado operativo "En Mantenimiento/Limpieza"

---

### User Story 3 - Avisarle a Módulo 3 sobre los cambios de estado y los incidentes (Priority: P1)

Cada vez que cambia el estado o sub-estado de una reserva a partir de su consolidación, el sistema DEBE avisarle a la API externa de Módulo 3 (`Recibir estado de reserva`).

***Why this priority***: Módulo 3 necesita enterarse en tiempo real de lo que pasa para activar seguros, retener o devolver depósitos de garantía, y procesar reembolsos.

*(Los Acceptance Scenarios de esta US se mantienen intactos respecto a los avisos de Confirmación, Cierre, Cancelación y Penalidades).*

---

### User Story 4 - Rechazar por completo los cambios no permitidos y mantener intactos los estados finales (Priority: P2)

Si llega un pedido de cambio de estado que no está permitido, el sistema tiene que rechazar la solicitud sin excepciones.

***Acceptance Scenarios***:

1. **Scenario**: Intento de cancelar una reserva en Borrador o Pendiente de Pago
    - **Given** una reserva en estado principal "Borrador" o "Pendiente de Pago"
    - **When** llega una solicitud de cancelación voluntaria
    - **Then** el sistema rechaza la solicitud explicando que solo las reservas Confirmadas admiten cancelación. Si está en Pendiente de Pago, debe expirar pasivamente; si está en Borrador, simplemente se descarta/sobrescribe sin costo.

*(Los demás Acceptance Scenarios sobre estados finales y completados se mantienen idénticos).*

---

### Edge Cases

- **"Borrador" no bloquea inventario (Condición de carrera pre-pago)**: Dado que el estado `Borrador` no retiene la embarcación, es posible que dos Arrendatarios distintos tengan una reserva en `Borrador` para el mismo barco y las mismas fechas simultáneamente. El primer usuario que haga clic en `Iniciar pago` disparará el paso a `Pendiente de Pago` y ganará el bloqueo en Módulo 1 (`Reservado`). Si el segundo usuario intenta `Iniciar pago` después, Módulo 2 validará la disponibilidad en Módulo 1, descubrirá que ya está reservado por el primero y rechazará el paso a `Pendiente de Pago`.
- **"Pendiente de Pago" no se puede cancelar por voluntad propia**: La reserva se libera solo de forma pasiva cuando vence el temporizador TTL de 15 minutos[cite: 2].
- **Falla pasajera de conexión con Módulo 1 o 3**: Mecanismo de cola y reintentos (0% de eventos perdidos).
- **Los estados finales no se pueden tocar nunca más**: `Completado`, `Cancelado` y `Expirado` son definitivos.

---

## Requirements

### Functional Requirements

- **FR-001**: El sistema DEBE ser el único lugar donde se actualiza el estado y sub-estado de cualquier reserva en Módulo 2, invocado obligatoriamente mediante relaciones `<<include>>`.
- **FR-002**: El sistema DEBE seguir esta lista oficial de estados de la reserva:
    - **Estados Principales de la Reserva**: `Borrador`, `Pendiente de Pago`, `Confirmada`, `En Navegación`, `Completado`, `Cancelado`, `Expirado`. El estado inicial de toda reserva al crearse es siempre `Borrador` (disparado únicamente por "Iniciar reserva"). `Borrador`, `Pendiente de Pago`, `En Navegación` y `Expirado` no tienen sub-estados.
    - **Sub-estados de Cancelación**: `Flexible`, `Moderado`, `Tardío`, `Por Anfitrión`, `Por Inasistencia`.
    - **Sub-estados de Finalización**: `Sin incidentes`, `Con incidentes`.
    - **Estados terminales**: `Completado`, `Cancelado` y `Expirado`.
- **FR-003**: El sistema DEBE verificar de forma estricta que el cambio de estado pedido sea uno de los permitidos:
    - `Creación → Borrador`: única forma de entrar a la máquina de estados, disparada por "Iniciar reserva".
    - `Borrador → Pendiente de Pago`: disparado por "Iniciar pago".
    - `Borrador → Expirado` (o borrado pasivo/limpieza de carritos abandonados).
    - `Pendiente de Pago → Confirmada` o `Expirado`.
    - `Confirmada → En Navegación` o `Cancelado`.
    - `En Navegación → Completado`.
- **FR-004**: Si el cambio pedido es válido, el sistema DEBE actualizar y guardar de forma atómica el estado principal.
- **FR-005**: Si el cambio pedido no es válido, el sistema DEBE rechazar la solicitud.
- **FR-006**: El sistema DEBE tener control de concurrencia para evitar que dos solicitudes de actualización sobre la misma reserva choquen.
- **FR-007**: El sistema DEBE sincronizar el estado operativo con Módulo 1 bajo las siguientes reglas:
    - `Creación a Borrador`: NO se notifica bloqueo a Módulo 1.
    - `Borrador a Pendiente de Pago`: llamar a `Asignar estado operativo` (poner en `Reservado`).
    - Pasa a `En Navegación`: actualizar a `En Navegación`.
    - Pasa a `Completado` (Sin incidentes), `Cancelado` o `Expirado`: actualizar a `Disponible`.
    - Pasa a `Completado` (Con incidentes): actualizar a `En Mantenimiento/Limpieza`.
- **FR-008**: El sistema DEBE llamar a la API externa de Módulo 3 (`Recibir estado de reserva`) ante cada cambio de estado posterior al Borrador.
- **FR-009**: **Aviso de incidentes a Módulo 3**: Si el estado es `Completado` con sub-estado `Con incidentes`, el sistema DEBE avisarle explícitamente a Módulo 3.
- **FR-010**: **REGLA DE NEGOCIO ESTRICTA (Sin cálculo financiero):** Módulo 2 **NO DEBE** calcular montos ni hacer transferencias de dinero. Toda valoración económica es de Módulo 3.

---

### Key Entities

- **Reserva (`Reservation`)**: Entidad principal.
- **Historial de Cambios de Estado (`ReservationStatusAudit`)**: Registro del historial de vida.
- **Embarcación**: Barco físico referenciado externamente en Módulo 1.

---

## Success Criteria

### Measurable Outcomes

- **SC-001**: Cero (0%) cambios de estado no permitidos.
- **SC-002**: El 100% de los cambios disparan avisos a APIs externas en < 500 ms.
- **SC-003**: Cero (0%) embarcaciones bloqueadas físicamente en Módulo 1 por culpa de reservas que solo están en estado `Borrador`.