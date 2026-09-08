# Feature Specification: Iniciar Reserva (v2)

**Módulo**: Módulo 2 (Operación de Reservas, Tiempos y Cancelaciones)  
**Created**: 2026-09-06  
**Primary Actor**: Arrendatario (Turista que reserva la embarcación)  
**External Dependencies (APIs)**:
- **Módulo 1 (Gestión de Flota y Activos P2P)**:
  - `Proveer información de embarcación`: API externa de consulta para validar atributos, capacidad y especificaciones técnicas de la embarcación seleccionada.
  - `Brindar información estado operativo`: API externa de consulta síncrona para corroborar que la embarcación se encuentre físicamente en estado `Disponible` (y no en `Reservado`, `En Navegación` o `En Mantenimiento/Limpieza`).
  - `Asignar estado operativo`: API externa de actualización consumida indirectamente a través del caso de uso `Actualizar estado reserva` para cambiar la embarcación a `Reservado` en el momento de creación de la reserva.
- **Módulo 3 (Liquidación, Seguros y Dispersión de Fondos)**:
  - `Proveer cotización de reserva`: API externa financiera consumida para obtener el desglose tarifario estimado (tarifa base, seguro por pasajero y depósito de garantía informativo) sin que Módulo 2 efectúe cálculos monetarios.
  - `Confirmar pago`: Punto de entrada de integración asíncrono donde Módulo 3 notifica el resultado de cobro de la pasarela de pagos para consolidar la reserva en estado `Confirmada` dentro del TTL de 15 minutos.
- **Casos de uso internos de Módulo 2**:
  - `Actualizar estado reserva` (`<<include>>`): Para gobernar la transición inicial `Creación → Pendiente de Pago`, el inicio del temporizador TTL de 15 minutos y las posteriores transiciones hacia `Confirmada` o `Expirado`.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - El Arrendatario selecciona una embarcación disponible, valida el tiempo y registra la reserva en Pendiente de Pago (Priority: P1)

El Arrendatario navega por el marketplace, selecciona una embarcación y define el periodo de tiempo deseado (fecha/hora de inicio y fecha/hora de finalización). El sistema verifica mediante la API de Módulo 1 (`Proveer información de embarcación` y `Brindar información estado operativo`) que la embarcación existe y está operativa. Adicionalmente, el sistema valida que no existan traslapes horarios con otras reservas activas en Módulo 2. Cumplidas las validaciones, el sistema invoca a "Actualizar estado reserva" para crear la reserva en el estado inicial "Pendiente de Pago", iniciando de inmediato el temporizador de bloqueo temporal (TTL) de 15 minutos e instruyendo a Módulo 1 fijar la embarcación en "Reservado".

**Why this priority**: Es el núcleo operacional fundacional del marketplace. Sin la capacidad de originar reservas y bloquear temporalmente el activo náutico, no existe ciclo de vida comercial.

**Independent Test**: Se puede probar aislando el flujo con mocks de Módulo 1 y Módulo 3: enviando una solicitud con periodo válido sobre una embarcación en estado operativo "Disponible", verificando que la reserva se persiste en "Pendiente de Pago", el temporizador TTL de 15 minutos se activa y Módulo 1 recibe la instrucción de fijar la embarcación en "Reservado".

**Acceptance Scenarios**:

1. **Scenario**: Reserva creada exitosamente con embarcación disponible y fechas válidas
   - **Given** un Arrendatario autenticado y una embarcación cuyo estado operativo es "Disponible" según Módulo 1
   - **When** el Arrendatario envía una solicitud de reserva con fechas y horas válidas sin traslapes
   - **Then** el sistema invoca "Actualizar estado reserva", persiste la reserva en estado inicial "Pendiente de Pago", inicia el temporizador TTL de 15 minutos e instruye a Módulo 1 (`Asignar estado operativo`) actualizar la embarcación a "Reservado"

2. **Scenario**: Rechazo por embarcación no operativa según Módulo 1
   - **Given** una embarcación que se encuentra en "Reservado", "En Navegación" o "En Mantenimiento/Limpieza" según la API `Brindar información estado operativo` de Módulo 1
   - **When** el Arrendatario intenta iniciar la reserva sobre dicha embarcación
   - **Then** el sistema deniega la creación de la reserva, informa el motivo de no disponibilidad y no realiza bloqueos de calendario ni transiciones de estado

3. **Scenario**: Rechazo por traslape con otra reserva activa en Módulo 2
   - **Given** una embarcación disponible en Módulo 1 pero con una reserva existente en Módulo 2 en estado "Pendiente de Pago" o "Confirmada" para el mismo intervalo horario
   - **When** el Arrendatario intenta reservar ese mismo rango horario
   - **Then** el sistema rechaza la solicitud informando que el horario solicitado entra en conflicto con una reserva preexistente

---

### User Story 2 - El Arrendatario visualiza la cotización oficial provista por Módulo 3 antes de formalizar la reserva (Priority: P1)

Durante la configuración de fechas y cantidad de pasajeros, el sistema consulta de forma síncrona a la API externa de Módulo 3 (`Proveer cotización de reserva`) para obtener la cotización oficial estimada del alquiler. El sistema muestra de manera transparente al Arrendatario el desglose (tarifa por duración, costo de póliza de seguro por pasajero y valor del depósito de garantía) antes de que el usuario confirme la creación de la reserva y active el bloqueo de 15 minutos.

**Why this priority**: Asegura transparencia financiera al turista y garantiza el cumplimiento estricto de la regla arquitectónica: Módulo 2 jamás calcula tarifas ni dinero; toda cotización emana de Módulo 3.

**Independent Test**: Se prueba simulando la consulta con diferentes duraciones y pasajeros; se comprueba que el sistema consume `Proveer cotización de reserva` de Módulo 3, presenta los valores exactamente como fueron devueltos sin redondearlos ni alterarlos, y detiene el flujo si la API de cotización no responde.

**Acceptance Scenarios**:

1. **Scenario**: Presentación exitosa de cotización calculada por Módulo 3
   - **Given** un rango de fechas y número de pasajeros válidos seleccionados por el Arrendatario
   - **When** el sistema solicita la cotización preliminar a Módulo 3 (`Proveer cotización de reserva`)
   - **Then** el sistema recibe el desglose tarifario desde Módulo 3 y se lo exhibe íntegramente al Arrendatario previo a la confirmación de la reserva

2. **Scenario**: Falla o timeout en la API de cotización de Módulo 3
   - **Given** que la API `Proveer cotización de reserva` de Módulo 3 no responde o reporta error transitorio
   - **When** el Arrendatario solicita cotizar o continuar con la reserva
   - **Then** el sistema aborta de forma segura la creación de la reserva, informa de la indisponibilidad temporal del motor financiero y no genera registros provisionales ni bloqueos en Módulo 1

---

### User Story 3 - Gobierno del desenlace inicial: Confirmación de Pago vs Expiración de TTL (Priority: P1)

Habiéndose registrado la reserva en estado "Pendiente de Pago", el sistema gobierna los dos únicos desenlaces válidos de la ventana de 15 minutos:
1. Si Módulo 3 notifica la aprobación del cobro (`Confirmar pago`) dentro de los 15 minutos, el temporizador TTL se desactiva y la reserva transiciona a "Confirmada" a través de "Actualizar estado reserva".
2. Si expiran los 15 minutos continuos sin recibir confirmación de pago, el temporizador del sistema transiciona automáticamente la reserva a "Expirado" a través de "Actualizar estado reserva", lo que dispara la liberación de la embarcación a "Disponible" en Módulo 1.

**Why this priority**: Regula la liberación garantizada del inventario crítico náutico, impidiendo que una embarcación permanezca bloqueada indefinidamente por usuarios que abandonan el proceso de compra.

**Independent Test**: Se prueba en dos etapas:
a) Recibir el evento `Confirmar pago` de Módulo 3 al minuto 5 -> verificar cancelación de TTL y paso a "Confirmada".
b) Dejar transcurrir los 15 minutos sin evento de pago -> verificar que el sistema transiciona a "Expirado" y que Módulo 1 recibe la liberación a "Disponible".

**Acceptance Scenarios**:

1. **Scenario**: Confirmación de pago dentro de la ventana de 15 minutos
   - **Given** una reserva en estado "Pendiente de Pago" con tiempo remanente en su temporizador de 15 minutos
   - **When** Módulo 3 invoca `Confirmar pago` reportando transacción aprobada
   - **Then** el sistema cancela el temporizador TTL, invoca "Actualizar estado reserva" para transicionar la reserva a "Confirmada" y mantiene la embarcación en "Reservado" en Módulo 1

2. **Scenario**: Expiración automática por vencimiento del temporizador de 15 minutos sin pago
   - **Given** una reserva en estado "Pendiente de Pago" creada hace 15 minutos sin confirmación de pago
   - **When** el temporizador TTL alcanza el segundo 900 (15 minutos exactos)
   - **Then** el sistema invoca "Actualizar estado reserva" fijando el estado en "Expirado" y disparando la llamada síncrona a Módulo 1 (`Asignar estado operativo`) para restituir la embarcación a "Disponible"

---

### Edge Cases

- **Indisponibilidad o timeout de la API de Módulo 1**: Si al consultar `Proveer información de embarcación` o `Brindar información estado operativo` la API de Módulo 1 arroja timeout o no está disponible, el sistema debe abortar el proceso inmediatamente sin persistir reservas huérfanas y notificar al turista.
- **Concurrencia de múltiples solicitudes sobre la misma embarcación**: Si dos arrendatarios confirman en el mismo segundo una reserva para la misma embarcación en fechas coincidentes, el control de concurrencia atómico garantiza que únicamente la primera transacción persista en `Pendiente de Pago` y bloquee la embarcación en Módulo 1; la segunda solicitud es rechazada de forma inmediata por conflicto de inventario.
- **Validaciones de rango horario (Lógica temporal en Módulo 2)**:
  - Fecha/hora de inicio debe ser posterior a la fecha/hora actual del sistema.
  - Fecha/hora de finalización debe ser estrictamente posterior a la fecha/hora de inicio.
  - La duración debe respetar la política mínima de alquiler náutico establecida por la plataforma.
- **Inadmisibilidad de cancelación activa en "Pendiente de Pago"**: Una reserva recién nacida en estado `Pendiente de Pago` NO admite la invocación de `Solicitar cancelación`. Si el Arrendatario desiste de contratar, basta con abandonar el flujo y permitir que el temporizador TTL de 15 minutos expire naturalmente.
- **Condición de carrera al segundo 900 (Pago simultáneo a la expiración)**: Si la confirmación de pago de Módulo 3 se procesa cuando la reserva ya fue asentada como `Expirado` y liberada en Módulo 1, el sistema rechaza la confirmación y solicita formalmente a Módulo 3 la reversión automática de los fondos.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE permitir al Arrendatario seleccionar una embarcación y definir el rango horario de reserva (fecha/hora de inicio y fecha/hora de finalización).
- **FR-002**: El sistema DEBE consumir la API externa `Proveer información de embarcación` de Módulo 1 para validar que la embarcación existe y obtener sus especificaciones operativas.
- **FR-003**: El sistema DEBE consumir la API externa `Brindar información estado operativo` de Módulo 1 para corroborar que la embarcación se encuentra en estado `Disponible` previo a cualquier intento de persistencia.
- **FR-004**: El sistema DEBE verificar internamente que el periodo horario solicitado no presente solapamientos con ninguna otra reserva existente en estado `Pendiente de Pago` o `Confirmada` para la misma embarcación.
- **FR-005**: El sistema DEBE consumir la API externa `Proveer cotización de reserva` de Módulo 3 para obtener y exhibir la cotización preliminar estimada antes de la formalización de la reserva por parte del Arrendatario.
- **FR-006**: **REGLA DE NEGOCIO ESTRICTA (Sin cálculo financiero):** El sistema **NO DEBE calcular importes de alquiler, tarifas dinámicas, comisiones, coberturas de seguro ni valores de garantía**. Toda valoración monetaria es generada y gobernada exclusivamente por Módulo 3.
- **FR-007**: Al confirmar el Arrendatario la solicitud de reserva, el sistema DEBE invocar obligatoriamente el caso de uso subordinado `Actualizar estado reserva` (`<<include>>`) para asentar la transición inicial `Creación → Pendiente de Pago`.
- **FR-008**: Al persistirse la reserva en estado inicial `Pendiente de Pago`, el sistema DEBE iniciar de inmediato un temporizador de bloqueo temporal (TTL) de quince (15) minutos continuos.
- **FR-009**: Como consecuencia de la transición inicial a `Pendiente de Pago`, el sistema DEBE delegar en `Actualizar estado reserva` la invocación síncrona a la API externa `Asignar estado operativo` de Módulo 1 para actualizar el estado de la embarcación a `Reservado`.
- **FR-010**: El sistema DEBE coordinar con el caso de uso de integración `Confirmar pago` (consumido desde Módulo 3) para que, ante una transacción aprobada dentro del TTL de 15 minutos, se cancele el temporizador TTL y se delegue en `Actualizar estado reserva` la transición a estado `Confirmada`.
- **FR-011**: Si transcurren los quince (15) minutos del TTL sin confirmación de pago aprobada, el sistema DEBE disparar de forma automática la transición al estado terminal `Expirado` a través de `Actualizar estado reserva`, garantizando la restitución del activo a estado `Disponible` en Módulo 1.
- **FR-012**: Las reservas en estado `Pendiente de Pago` NO son cancelables mediante el caso de uso `Solicitar cancelación`; concluyen pasivamente por la expiración del temporizador TTL si el usuario decide no pagar.
- **FR-013**: El sistema DEBE registrar un asiento auditable de la creación de la reserva, capturando: identificador de la reserva, identificador del arrendatario, identificador de la embarcación, fechas pactadas, estampa temporal de inicio y fecha/hora de expiración del TTL.

---

### Key Entities

- **Reserva (`Reservation`)**: Entidad principal de Módulo 2. Atributos funcionales: identificador único, identificador del arrendatario, referencia a la embarcación (Módulo 1), fecha/hora pactada de inicio, fecha/hora pactada de fin, estado principal inicial (`Pendiente de Pago`), fecha/hora exacta de expiración del TTL de 15 minutos.
- **Embarcación**: Activo físico gobernado por Módulo 1, consultado vía `Proveer información de embarcación` y `Brindar información estado operativo`, y actualizado a `Reservado` mediante `Asignar estado operativo`.
- **Cotización Estimada de Alquiler**: Estructura de datos generada exclusivamente por Módulo 3 que detalla tarifa base estimada, seguro por pasajero y depósito de garantía informativo.
- **Temporizador TTL de Bloqueo**: Mecanismo temporal estricto de quince (15) minutos de duración que gobierna la ventana de pago antes de la expiración automática del activo.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Cero (0%) reservas creadas sobre embarcaciones que no tengan estado operativo `Disponible` certificado por Módulo 1.
- **SC-002**: Cero (0%) sobreventas o solapamientos de calendario en una misma embarcación para el mismo rango horario.
- **SC-003**: El 100% de las reservas creadas en `Pendiente de Pago` inician el temporizador TTL de 15 minutos y disparan la actualización de la embarcación a `Reservado` en Módulo 1 en menos de 1 segundo tras la confirmación del turista.
- **SC-004**: El 100% de las reservas no pagadas al cumplirse los 15 minutos transicionan a `Expirado` y liberan la embarcación a `Disponible` en Módulo 1 en menos de 3 segundos tras el vencimiento del temporizador.
- **SC-005**: Cero (0) cálculos monetarios, tarifas o cargos financieros calculados o alterados dentro de Módulo 2.
- **SC-006**: El 100% de las cotizaciones mostradas al Arrendatario son obtenidas de forma fidedigna y directa desde la API `Proveer cotización de reserva` de Módulo 3.
