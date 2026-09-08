}# Feature Specification: Proveer Información de Cotización de Reserva

**Módulo**: Módulo 2 (Operación de Reservas, Tiempos y Cancelaciones)  
**Created**: 2026-09-06  
**Primary Actor / Disparador**: Invocación interna desde el caso de uso `Iniciar reserva` (`<<include>>`) / Interfaz de integración con Módulo 3  
**External Dependencies (APIs)**:
- **Módulo 3 (Liquidación, Seguros y Dispersión de Fondos)**: API externa de tarifas y costos (`Proveer cotización de reserva` / `Solicitar cotización para reserva`). Es el único motor financiero de la plataforma: recibe los datos del barco, el horario y los pasajeros, y entrega el precio total detallado (alquiler, seguro obligatorio y depósito de garantía).

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consultar el costo oficial calculado por Módulo 3 como parte de crear la reserva (Priority: P1)

En cuanto el cliente escoge el barco, la fecha/hora de inicio y fin, y el número de pasajeros, 'Iniciar reserva' (`<<include>>`) le pide de inmediato el precio oficial a Módulo 3, sin pausa intermedia ni confirmación adicional del cliente. Si Módulo 3 responde con éxito, el sistema recibe el precio desglosado (alquiler del barco, seguro de accidentes por pasajero y depósito de garantía) y, en el mismo flujo, 'Iniciar reserva' continúa y crea la reserva en estado 'Pendiente de Pago' con ese precio ya asociado, lo que activa de inmediato el conteo de los 15 minutos de tolerancia para pagar. El precio se le muestra al cliente como parte de la reserva ya creada, no como un paso previo a decidir si reservar o no.

**Why this priority**: Es el paso indispensable para saber cuánto se le va a cobrar al cliente antes de que la reserva quede creada; sin este dato, 'Iniciar reserva' no puede pasar la reserva a 'Pendiente de Pago' con un precio válido asociado.

**Independent Test**: Se prueba conectando el sistema a un simulador de Módulo 3 que devuelva precios para distintos barcos, horas y cantidad de pasajeros. Se comprueba que Módulo 2 recibe los valores entregados por Módulo 3, sin hacer cálculos, cambios ni redondeos, y que la reserva queda creada en 'Pendiente de Pago' con ese mismo precio, sin ningún paso de confirmación intermedio.

**Acceptance Scenarios**:

1. **Scenario**: Obtención exitosa de la cotización y creación inmediata de la reserva
    - **Given** un barco disponible, un horario libre y una cantidad de pasajeros permitida
    - **When** el cliente escoge la fecha y hora, y 'Iniciar reserva' solicita la cotización a Módulo 3
    - **Then** el sistema recibe el detalle del costo (alquiler, seguro, garantía y total) y 'Iniciar reserva' crea de inmediato la reserva en 'Pendiente de Pago' con ese precio, sin esperar ninguna acción adicional del cliente

2. **Scenario**: Respeto total de los valores recibidos sin modificarlos
    - **Given** una respuesta de Módulo 3 con el desglose de precios
    - **When** el sistema recibe los datos de la cotización
    - **Then** el sistema guarda y muestra cada monto exactamente como lo envió Módulo 3, sin sumar, restar ni modificar ningún número en Módulo 2

---

### User Story 2 - Pedir una nueva cotización si el cliente cambia los datos del viaje antes de que la reserva se cree (Priority: P2)

Si el cliente cambia la fecha, la hora o el número de pasajeros mientras todavía está eligiendo los datos del viaje (antes de que 'Iniciar reserva' complete la creación de la reserva), cualquier cotización que se haya pedido con los datos anteriores deja de ser válida. El sistema la descarta y le pide a Módulo 3 una nueva cotización con los datos actualizados.

**Why this priority**: Asegura que el precio con el que se crea la reserva corresponda exactamente a los datos finales del viaje, evitando cobros incorrectos por cambios en el número de pasajeros (que cambia el costo del seguro) o por tarifas de fines de semana y festivos.

**Independent Test**: Se prueba pidiendo una cotización para 4 personas en un horario determinado, cambiando luego a 6 personas antes de que se cree la reserva. Se verifica que la primera cotización se descarta y se le pide a Módulo 3 un nuevo cálculo actualizado.

**Acceptance Scenarios**:

1. **Scenario**: Cambiar fechas, horas o pasajeros antes de crear la reserva pide un nuevo precio
    - **Given** el cliente está seleccionando los datos del viaje y aún no se ha creado la reserva
    - **When** el cliente cambia la fecha, la hora o la cantidad de pasajeros
    - **Then** el sistema descarta cualquier cotización pedida con los datos anteriores y solicita una nueva a Módulo 3 con los datos actualizados

---

### User Story 3 - Protección y bloqueo del proceso si Módulo 3 no responde o falla (Priority: P2)

Si la API de Módulo 3 se cae, tarda demasiado en responder o rechaza la consulta (por ejemplo, porque el barco no tiene precios configurados), el sistema detiene el proceso de reserva por seguridad y NO crea la reserva. De esta forma se evita que se creen reservas con precios en cero, vacíos o incorrectos.

**Why this priority**: Protege al negocio y al cliente, evitando que se generen reservas o contratos con precios erróneos debido a una falla en el sistema de cobros.

**Independent Test**: Se prueba simulando una falla de red o un error en la respuesta de Módulo 3. Se comprueba que el sistema detiene el proceso, no crea la reserva en Módulo 2 y le muestra al usuario un mensaje indicando que el servicio de precios no está disponible.

**Acceptance Scenarios**:

1. **Scenario**: Barco sin precios configurados en Módulo 3
    - **Given** una consulta de precios para un barco que no tiene tarifas configuradas en Módulo 3
    - **When** Módulo 3 responde con un error indicando que no se puede cotizar
    - **Then** el sistema detiene el proceso, no genera precios inventados, no crea la reserva y le informa al cliente que el barco no se puede cotizar en ese momento

2. **Scenario**: Falla o tiempo de espera agotado al consultar Módulo 3
    - **Given** una consulta de cotización enviada a Módulo 3 que no responde a tiempo
    - **When** se agota el tiempo máximo de espera
    - **Then** el sistema cancela la consulta por seguridad, no asume precios por defecto, no crea la reserva y notifica al cliente que el servicio de tarifas no está disponible temporalmente

---

### User Story 4 - Evitar reservas duplicadas cuando compiten dos solicitudes por el mismo horario o cliente (Priority: P1)

Como no hay pausa entre cotizar y crear la reserva, pueden llegar dos solicitudes casi al mismo tiempo que compitan por el mismo resultado: (a) dos clientes distintos intentando reservar el mismo barco en el mismo horario, o (b) el mismo cliente disparando la misma solicitud dos veces por un doble clic o un reintento de red. En ambos casos, el sistema debe garantizar que solo una reserva válida se cree, y rechazar de forma clara la segunda.

**Why this priority**: Sin esta protección se podría vender el mismo horario dos veces o duplicar cobros de tolerancia de 15 minutos para el mismo cliente, lo cual rompe la integridad del inventario y la confianza en la plataforma.

**Independent Test**: Se prueba disparando dos solicitudes casi simultáneas para el mismo barco y horario desde dos clientes distintos, y por separado, dos solicitudes idénticas del mismo cliente en el mismo instante. Se verifica que en ambos casos solo se crea una reserva y la segunda solicitud recibe un rechazo explícito.

**Acceptance Scenarios**:

1. **Scenario**: Dos clientes distintos compiten por el mismo barco y horario
    - **Given** dos clientes solicitando cotización para el mismo barco y el mismo horario casi al mismo tiempo
    - **When** ambas solicitudes reciben cotización de Módulo 3 y ambas intentan crear la reserva
    - **Then** solo la primera reserva que efectivamente se crea en Módulo 2 queda en pie; a la segunda se le rechaza la creación e informa que el horario dejó de estar disponible, aunque ya tuviera un precio cotizado

2. **Scenario**: El mismo cliente duplica su propia solicitud
    - **Given** un cliente cuya solicitud se dispara dos veces de forma casi simultánea (mismo barco, mismo horario, mismo cliente) por un doble clic o un reintento de red
    - **When** el sistema detecta ambas solicitudes
    - **Then** el sistema procesa una sola de ellas y no crea dos reservas duplicadas para el mismo cliente

---

### Edge Cases

- **Prohibición de calcular dinero en Módulo 2**:
    - El Módulo 2 **JAMÁS calcula, suma, aplica fórmulas, ni cobra comisiones o seguros**. Todos los montos mostrados provienen directamente del Módulo 3.
- **Detección de precios inválidos (Monto menor o igual a cero)**:
    - Si el Módulo 3 devuelve un precio total de cero (0) o negativo sin una razón o promoción válida, el sistema detecta la falla y detiene la creación de la reserva de forma preventiva.
- **Moneda de la reserva**:
    - El Módulo 2 mantiene la moneda que le indique Módulo 3 (por ejemplo, COP o USD) y la muestra tal cual en pantalla sin hacer conversiones de cambio.
- **Cambios rápidos y repetidos mientras se eligen los datos del viaje**:
    - Si el cliente cambia rápidamente los selectores de hora o pasajeros generando varias consultas seguidas (antes de que la reserva se cree), el sistema ignora las respuestas viejas y usa únicamente el precio correspondiente a la última opción elegida.
- **Dos clientes compitiendo por el mismo barco y horario**:
    - Ver User Story 4, Escenario 1. La disponibilidad se vuelve a confirmar justo antes de crear la reserva; ganar la cotización no garantiza ganar la reserva.
- **El mismo cliente duplica su propia solicitud**:
    - Ver User Story 4, Escenario 2. El sistema detecta solicitudes idénticas y casi simultáneas del mismo cliente para el mismo barco y horario, y procesa solo una.
- **Módulo 3 responde con datos incompletos**:
    - Si la respuesta de Módulo 3 llega sin alguno de los componentes esperados del precio (por ejemplo, sin el desglose del seguro o sin el depósito de garantía), el sistema lo trata como una falla de Módulo 3 (bloqueo preventivo) y NO completa el dato faltante con un valor supuesto ni con cero.
- **La cotización no compromete fondos en Módulo 3**:
    - Pedir una cotización es una consulta de solo lectura para Módulo 3: no reserva fondos, no genera cargos ni bloquea dinero. Si el proceso de "Iniciar reserva" falla después de recibir el precio (por ejemplo, por un error interno de Módulo 2), no hay nada que deshacer del lado de Módulo 3.
- **Fecha/hora del viaje inválida**:
    - [NEEDS CLARIFICATION: si el cliente envía una fecha/hora de inicio ya pasada, o una fecha de fin anterior a la de inicio, ¿esta interfaz debe rechazarlo ella misma antes de llamar a Módulo 3, o esa validación ya la hizo "Iniciar reserva" antes de invocar esta interfaz, asumiendo que los datos siempre llegan válidos?]

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE ofrecer una interfaz de consulta de costos que sea llamada obligatoriamente por el caso de uso `Iniciar reserva` (`<<include>>`).
- **FR-002**: El sistema DEBE recibir de la reserva los datos necesarios: código del barco, fecha/hora de inicio, fecha/hora de fin y cantidad de pasajeros.
- **FR-003**: El sistema DEBE consultar directamente la API de Módulo 3 (`Proveer cotización de reserva` / `Solicitar cotización para reserva`) enviando los datos del viaje, en cuanto el cliente termine de escoger dichos datos.
- **FR-004**: **REGLA ESTRICTA (Sin cálculos de dinero):** El sistema **NO DEBE en ningún caso calcular, estimar, sumar ni restar precios de alquiler**, seguros o garantías. Todo valor monetario DEBE ser entregado exclusivamente por Módulo 3.
- **FR-005**: El sistema DEBE guardar la respuesta de precios enviada por Módulo 3, incluyendo:
    - Código o token de referencia de la cotización.
    - Precio total del viaje.
    - Detalle informativo: precio del alquiler del barco, costo del seguro por pasajero y valor del depósito de garantía.
    - Moneda oficial del cobro.
- **FR-006**: El sistema DEBE entregar la cotización al proceso `Iniciar reserva`, quien la usa de inmediato para crear la reserva en estado "Pendiente de Pago" con ese precio ya asociado, sin esperar una confirmación adicional del cliente.
- **FR-007**: Si el cliente cambia la fecha, hora, barco o número de pasajeros antes de que la reserva quede creada, el sistema DEBE descartar la cotización anterior y pedir una nueva a Módulo 3.
- **FR-008**: Si Módulo 3 devuelve un error (barco sin precios o datos inválidos) o una respuesta incompleta (falta algún componente del precio), el sistema DEBE detener el proceso e impedir la creación de la reserva, sin completar el dato faltante con un valor supuesto.
- **FR-009**: Si la consulta a Módulo 3 falla por desconexión o demora, el sistema DEBE aplicar un bloqueo preventivo (*fail-safe*), cancelando la operación e informando la indisponibilidad del servicio de tarifas.
- **FR-010**: El sistema DEBE volver a confirmar la disponibilidad del barco y del horario justo antes de crear la reserva (no solo al pedir la cotización), para evitar que dos solicitudes casi simultáneas por el mismo barco y horario terminen creando dos reservas.
- **FR-011**: Si dos solicitudes compiten por el mismo barco y horario, el sistema DEBE permitir que solo la primera que efectivamente cree la reserva quede en pie, y DEBE rechazar la creación de la segunda, informando que el horario dejó de estar disponible.
- **FR-012**: El sistema DEBE detectar solicitudes duplicadas del mismo cliente para el mismo barco y horario ocurridas de forma casi simultánea (por ejemplo, por doble clic o reintento de red), y DEBE procesar solo una, sin crear reservas duplicadas.
- **FR-013**: **REGLA (Cotización sin compromiso de fondos):** El sistema DEBE tratar la cotización como una consulta de solo lectura frente a Módulo 3; ninguna falla posterior en Módulo 2 DEBE requerir "deshacer" nada del lado de Módulo 3.

---

### Key Entities

- **Reserva (`Reservation`)**: Entidad de Módulo 2 que se crea en estado "Pendiente de Pago" con el precio cotizado ya asociado; su unicidad por barco y horario se protege ante solicitudes concurrentes.
- **Solicitud de Cotización (`ReservationQuoteRequest`)**: Datos enviados a Módulo 3. Incluye: identificador del barco, fecha/hora inicio, fecha/hora fin, número de pasajeros e identificador del cliente.
- **Desglose de Cotización (`ReservationQuoteResponse`)**: Información entregada por Módulo 3. Incluye: código de cotización, monto total, costo de alquiler, seguro por pasajero, depósito de garantía y moneda.
- **Parámetros de Reserva (`ReservationParameters`)**: Datos seleccionados por el usuario que determinan el costo del viaje.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de los precios usados en la creación de reservas provienen directamente de Módulo 3, con cero (0%) cálculos matemáticos realizados en Módulo 2.
- **SC-002**: El sistema obtiene la cotización y crea la reserva en un solo flujo continuo, sin pasos de confirmación intermedios, dentro de un tiempo de respuesta aceptable [NEEDS CLARIFICATION: no hay un umbral de latencia definido en la documentación del proyecto — ¿existe un SLA objetivo para esta entrega?].
- **SC-003**: Cero (0%) reservas creadas en estado "Pendiente de Pago" sin una cotización válida de Módulo 3.
- **SC-004**: El 100% de los cambios en los datos del viaje, hechos antes de crear la reserva, descartan la cotización anterior y piden una nueva a Módulo 3.
- **SC-005**: El 100% de los errores o caídas en la API de Módulo 3 detienen el proceso de forma segura, evitando reservas con precios vacíos o erróneos.
- **SC-006**: Cero (0%) barcos vendidos dos veces para el mismo horario por solicitudes casi simultáneas de clientes distintos.
- **SC-007**: Cero (0%) reservas duplicadas creadas por doble clic o reintento de red del mismo cliente.