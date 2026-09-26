# Feature Specification: Brindar Información de Estado Operativo

**Módulo**: Módulo 2 (Operación de Reservas, Tiempos y Cancelaciones)  
**Created**: 2026-09-06  
**Primary Actor / Disparador**: Módulo 1 (Gestión de Flota) / Invocación interna durante las validaciones de disponibilidad de la embarcación.  
**External Dependencies (APIs)**:
- **Módulo 1 (Gestión de Flota y Activos P2P)**: API externa de inventario náutico que provee el estado operativo real de la embarcación (`Disponible`, `Reservado`, `En Navegación`, `En Mantenimiento/Limpieza`).

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consultar si una embarcación está disponible para alquilar (Priority: P1)

Cuando el sistema necesita saber si un barco se puede alquilar, consulta a la API de Módulo 1 para obtener su estado en ese mismo instante. Si el Módulo 1 responde que el barco está "Disponible", el sistema confirma que la embarcación se puede reservar. Si el barco se encuentra en cualquier otro estado (`Reservado`, `En Navegación` o `En Mantenimiento/Limpieza`), el sistema rechaza la solicitud e informa la razón por la que no se puede alquilar en ese momento.

**Why this priority**: Es la verificación obligatoria que evita alquilar un barco que ya está ocupado, averiado o en mantenimiento, previniendo sobreventas.

**Independent Test**: Se prueba conectando el sistema a un simulador de Módulo 1 que responda con cada uno de los cuatro estados posibles. Se verifica que el sistema únicamente permita avanzar con la reserva cuando la respuesta sea "Disponible".

**Acceptance Scenarios**:

1. **Scenario**: Embarcación disponible para alquiler
    - **Given** un identificador de embarcación registrado en la plataforma
    - **When** el sistema consulta su estado operativo y Módulo 1 responde "Disponible"
    - **Then** el sistema confirma que el barco está libre y permite continuar con el proceso de reserva

2. **Scenario**: Embarcación ocupada o en taller
    - **Given** una embarcación cuyo estado en Módulo 1 es "En Navegación" o "En Mantenimiento/Limpieza"
    - **When** se consulta su estado operativo
    - **Then** el sistema informa que el barco no se puede alquilar y muestra el motivo exacto de su indisponibilidad

3. **Scenario**: Embarcación apartada por otro pago en curso
    - **Given** una embarcación que figura en estado "Reservado" en Módulo 1
    - **When** se consulta su estado operativo
    - **Then** el sistema indica que el barco está bloqueado temporalmente por otro proceso y detiene la nueva solicitud

---

### User Story 2 - Protección ante fallas o caídas del Módulo 1 (Priority: P2)

Si al consultar la API del Módulo 1 el servicio no responde, falla o tarda demasiado tiempo en contestar, el sistema asume por seguridad que el barco NO está disponible. De esta forma se evita permitir la reserva de un barco cuyo estado real se desconoce.

**Why this priority**: Evita que un cliente alquile y pague un barco que podría estar en mal estado o dañado sin que el sistema lo supiera a causa de una falla de red.

**Independent Test**: Se prueba desconectando la red o simulando una demora en la respuesta de Módulo 1; se comprueba que el sistema bloquea el alquiler preventivamente y le muestra al usuario un mensaje indicando que el servicio de flota no está disponible.

**Acceptance Scenarios**:

1. **Scenario**: Tiempo de espera agotado o error en Módulo 1
    - **Given** una consulta de disponibilidad para una embarcación
    - **When** la API de Módulo 1 no responde en el tiempo límite o devuelve un error interno
    - **Then** el sistema rechaza la solicitud por seguridad, informa que no se pudo verificar la flota y no permite crear la reserva

---

### User Story 3 - Rechazo de consultas sobre barcos inexistentes (Priority: P2)

Si la consulta se realiza enviando un identificador de embarcación inválido, mal escrito o que no existe en Módulo 1, el sistema detecta el error mediante la respuesta de Módulo 1 y rechaza la operación de inmediato.

**Why this priority**: Impide que datos erróneos o modificados afecten el funcionamiento de la plataforma.

**Independent Test**: Se prueba enviando códigos de barco inventados o vacíos, comprobando que Módulo 1 responde que el recurso no existe y que el sistema rechaza la consulta sin fallar.

**Acceptance Scenarios**:

1. **Scenario**: Embarcación no encontrada
    - **Given** una solicitud con un código de barco que no existe en Módulo 1
    - **When** se consulta el estado operativo y Módulo 1 responde que no lo encontró
    - **Then** el sistema informa que la embarcación no está registrada y detiene la consulta

---

### Edge Cases

- **Barcos en mantenimiento con fecha estimada de entrega**:
    - Aunque Módulo 1 indique cuándo terminará la reparación de un barco, este caso de uso reporta únicamente su estado actual ("En Mantenimiento/Limpieza"), dejando claro que hoy no está disponible.
- **Comunicación estricta por API (sin acceso a base de datos)**:
    - Módulo 2 **NO lee directamente las tablas del Módulo 1**. Toda la información se solicita mediante mensajes HTTP/API.
- **Prohibición absoluta de cálculos de dinero**:
    - Este caso de uso solo verifica si el barco se puede usar o no; **NO calcula precios, no aplica tarifas ni solicita depósitos de garantía**.
- **Información siempre en tiempo real**:
    - Para evitar que dos personas alquilen el mismo barco al mismo tiempo, la consulta se realiza en el instante exacto en que se solicita, sin guardar datos antiguos en memoria cache.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE ofrecer una interfaz interna que reciba el código o identificador del barco a consultar.
- **FR-002**: El sistema DEBE consultar directamente la API externa de Módulo 1 para conocer el estado operativo en tiempo real.
- **FR-003**: El sistema DEBE reconocer los cuatro estados operativos oficiales que maneja Módulo 1:
    - `Disponible`: Libre para ser alquilado.
    - `Reservado`: Bloqueado por un proceso de pago o reserva previa.
    - `En Navegación`: En el agua ejecutando un viaje.
    - `En Mantenimiento/Limpieza`: Inhabilitado por reparación, revisión o aseo.
- **FR-004**: El sistema DEBE confirmar que el barco es **Apto para Reserva** si y solo si Módulo 1 devuelve el estado `Disponible`.
- **FR-005**: Si Módulo 1 devuelve `Reservado`, `En Navegación` o `En Mantenimiento/Limpieza`, el sistema DEBE marcar la embarcación como **No Apta para Reserva** y comunicar el motivo correspondiente.
- **FR-006**: Si la consulta a Módulo 1 falla por desconexión o demora, el sistema DEBE aplicar un bloqueo preventivo (*fail-safe*), declarando el barco como **No Apto para Reserva** y cancelando el proceso.
- **FR-007**: El sistema DEBE comunicarse con Módulo 1 exclusivamente a través de su API externa, sin conectarse jamás a la base de datos de dicho módulo.
- **FR-008**: **REGLA DE NEGOCIO ESTRICTA (Sin dinero):** El sistema **NO DEBE consultar precios, calcular tarifas ni manejar cobros**. Su función es únicamente verificar la disponibilidad física del barco.
- **FR-009**: El sistema DEBE guardar un registro simple de cada consulta realizada durante una reserva, guardando el código del barco, el estado devuelto por Módulo 1 y la hora exacta de la consulta.

---

### Key Entities

- **Consulta de Estado Operativo (`OperationalStatusQuery`)**: Datos intercambiados internamente en Módulo 2. Incluye: identificador de la embarcación, fecha/hora de la consulta, estado devuelto (`Disponible`, `Reservado`, `En Navegación`, `En Mantenimiento/Limpieza`) y la respuesta final de si se puede alquilar o no.
- **Embarcación**: Activo registrado en el Módulo 1 sobre el cual únicamente se verifica su disponibilidad.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de las consultas de disponibilidad a la API de Módulo 1 se responden en menos de 300 milisegundos en condiciones normales.
- **SC-002**: Cero (0%) reservas permitidas en barcos que figuren como `Reservado`, `En Navegación` o `En Mantenimiento/Limpieza` en Módulo 1.
- **SC-003**: El 100% de los errores de conexión con Módulo 1 bloquean la reserva de forma preventiva para evitar alquileres sin verificación.
- **SC-004**: Cero (0) accesos a la base de datos de Módulo 1 o cálculos de precios dentro de este caso de uso.
- **SC-005**: El 100% de las consultas sobre barcos inexistentes devuelven un mensaje claro sin detener el funcionamiento general del sistema.