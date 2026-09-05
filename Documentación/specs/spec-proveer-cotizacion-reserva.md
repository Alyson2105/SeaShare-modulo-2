# Feature Specification: Proveer Información Cotización de Reserva

**Módulo**: Módulo 2 – Gestión de Reserva  
**Creado**: 2026-09-05  
**Actor primario**: Arrendatario (indirecto, a través de "Establecer tiempo reserva" / "Iniciar reserva", que lo invocan vía `<<include>>`)  
**Dependencias externas (APIs)**:

- Módulo 3 – Liquidación, Seguros y Dispersión de Fondos (`Solicitar cotización para reserva`: API externa que recibe embarcación + ventana temporal y devuelve el monto a cobrar; internamente calcula tarifa base, tarifas dinámicas, seguro náutico y depósito de garantía de forma opaca para Módulo 2).

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - [Obtener cotización consolidada para una reserva solicitada] (Priority: P1)

Una vez que el caso de uso "Establecer tiempo reserva" valida exitosamente la ventana temporal solicitada para una embarcación, el sistema solicita a la API de Módulo 3 (`Solicitar cotización para reserva`) la cotización correspondiente al periodo y embarcación elegidos, recibiendo el valor a cobrar y poniéndolo a disposición del flujo de reserva para que el Arrendatario conozca el costo antes de pagar.

**Why this priority**: Es la funcionalidad medular del caso de uso. Sin la obtención de la cotización, el Arrendatario no puede conocer el costo de su alquiler ni se puede originar el proceso de cobro en Módulo 3.

**Independent Test**: Se puede probar aislando el caso de uso mediante un mock de la API externa de Módulo 3 (`Solicitar cotización para reserva`), enviando un identificador de embarcación y una ventana temporal válida, y verificando que el sistema retorna la información de cotización estructurada sin alterar ni recalcular ningún monto.

**Acceptance Scenarios**:

1. **Scenario**: Obtención exitosa de cotización para ventana válida
   - **Given** "Establecer tiempo reserva" ha validado satisfactoriamente la ventana temporal (fecha/hora inicio y fin) para una embarcación existente
   - **When** el sistema invoca "Proveer información cotización de reserva"
   - **Then** el sistema consume la API de Módulo 3 (`Solicitar cotización para reserva`), recibe el monto a cobrar y entrega la cotización a "Iniciar reserva" para su asociación a la reserva y presentación al Arrendatario

2. **Scenario**: Cotización con componentes informativos provistos por Módulo 3
   - **Given** Módulo 3 responde a la cotización con el monto total y el desglose de conceptos (tarifa base calculada, seguro náutico por pasajero y depósito de garantía)
   - **When** el sistema procesa la respuesta de Módulo 3
   - **Then** el sistema preserva íntegros los conceptos informativos provistos por Módulo 3 y los pone a disposición de "Iniciar reserva" sin realizar sumas, restas ni modificaciones sobre dichos valores

---

### User Story 2 - [Manejo de indisponibilidad o rechazo de cotización por Módulo 3] (Priority: P2)

Si la API de Módulo 3 no está disponible (timeout, caída del servicio) o rechaza la solicitud de cotización (por ejemplo, porque la embarcación no tiene reglas tarifarias configuradas o los parámetros son rechazados por liquidación), el sistema debe capturar el evento, impedir la creación o avance de la reserva con datos financieros ausentes e informar al Arrendatario el motivo de la imposibilidad de cotizar.

**Why this priority**: Evita que se creen reservas con montos nulos, corruptos o asumidos por defecto, garantizando la consistencia financiera y la integridad de la plataforma ante contingencias en la API externa.

**Independent Test**: Se puede probar simulando respuestas de error HTTP (4xx, 5xx) o timeout en la API de Módulo 3 y verificando que el sistema bloquea el flujo hacia "Iniciar reserva", no genera cotizaciones ficticias y retorna un mensaje de error controlado.

**Acceptance Scenarios**:

1. **Scenario**: Rechazo de cotización por falta de tarifa en Módulo 3
   - **Given** una embarcación que no cuenta con esquema tarifario activo en Módulo 3 para el rango solicitado
   - **When** el sistema solicita la cotización a Módulo 3
   - **Then** Módulo 3 responde con error de tarifación, el sistema detiene el flujo de cotización y notifica a "Iniciar reserva" que la embarcación no puede ser cotizada actualmente, impidiendo la confirmación de la reserva

2. **Scenario**: Timeout o indisponibilidad en la API de Módulo 3
   - **Given** la API de Módulo 3 no responde dentro del tiempo límite establecido
   - **When** se ejecuta la solicitud de cotización
   - **Then** el sistema interrumpe la espera, no asume ningún valor por defecto y notifica el fallo de comunicación al flujo de reserva para que el Arrendatario intente más tarde

---

### User Story 3 - [Revalidación de cotización ante cambios de parámetros] (Priority: P2)

Si el Arrendatario decide modificar las fechas, horas o la embarcación antes de formalizar la reserva, cualquier cotización previamente obtenida queda invalidada y el sistema debe solicitar de forma obligatoria una nueva cotización a Módulo 3.

**Why this priority**: Garantiza que el precio presentado y cobrado coincida exactamente con las condiciones finales del servicio náutico, evitando discrepancias causadas por tarifas dinámicas (fines de semana, temporada, variación de horas).

**Independent Test**: Se puede probar solicitando una cotización inicial para un rango T1, modificando el rango a T2 sobre la misma embarcación, y comprobando que la cotización asociada a T1 queda anulada y se genera una nueva llamada a Módulo 3 con T2.

**Acceptance Scenarios**:

1. **Scenario**: Modificación de ventana horaria invalida cotización anterior
   - **Given** una cotización previa obtenida para una ventana temporal T1
   - **When** el Arrendatario modifica la fecha u hora en "Establecer tiempo reserva", definiendo una nueva ventana T2
   - **Then** el sistema descarta la cotización de T1 y realiza una nueva invocación a la API `Solicitar cotización para reserva` de Módulo 3 con los parámetros de T2

---

### Edge Cases

- **Módulo 3 devuelve monto cero (0) o negativo**: Módulo 2 no interpreta reglas comerciales, pero por integridad de negocio náutico una reserva comercial no puede cotizarse en valores negativos. ¿El sistema debe rechazar cotizaciones con monto total menor o igual a cero como anomalía del proveedor tarifario? [NEEDS CLARIFICATION: ¿existen reservas gratuitas/promocionales legítimas autorizadas por Módulo 3, o todo valor <= 0 debe ser rechazado como error?]
- **Tiempo de vigencia de la cotización**: ¿Tiene la cotización entregada por Módulo 3 un periodo de validez propio (timestamp de caducidad de oferta tarifaria, p. ej. 10 minutos) independiente del TTL de 15 minutos de la reserva? [NEEDS CLARIFICATION: si una cotización vence mientras el usuario evalúa la pantalla antes de iniciar reserva, ¿debe refrescarse automáticamente o se mantiene hasta que se persista la reserva?]
- **Moneda de la transacción**: ¿Módulo 3 devuelve siempre la cotización en una moneda base única (p. ej. COP, USD) o puede responder en múltiples divisas según el puerto o el Arrendatario? [NEEDS CLARIFICATION: manejo de divisas en la respuesta de cotización]
- **Concurrencia de solicitudes de cotización por un mismo usuario**: si el Arrendatario ajusta repetidamente el selector de horas generando ráfagas de solicitudes, ¿el sistema descarta las respuestas obsoletas garantizando que solo la última cotización solicitada sea entregada?

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE recibir del caso de uso invocador ("Establecer tiempo reserva", vía `<<include>>`) el identificador de la embarcación y la ventana temporal validada (fecha/hora de inicio y fecha/hora de fin).
- **FR-002**: El sistema DEBE invocar la API externa de Módulo 3 (`Solicitar cotización para reserva`) enviando el identificador de la embarcación y la ventana temporal validada.
- **FR-003**: El sistema **NO DEBE bajo ninguna circunstancia calcular, estimar ni recalcular montos de tarifas**, ajustes de temporada, recargos de fin de semana, seguros náuticos ni depósitos de garantía; todo valor monetario DEBE ser provisto exclusivamente por Módulo 3.
- **FR-004**: El sistema DEBE recibir de Módulo 3 la respuesta de cotización, que contiene el monto total a cobrar y, si están presentes, los conceptos informativos desglosados (tarifa base, seguro náutico, depósito de garantía).
- **FR-005**: El sistema DEBE estructurar la información de cotización recibida y retornarla al flujo de reserva ("Establecer tiempo reserva" / "Iniciar reserva") para su almacenamiento provisional y posterior presentación al Arrendatario (FR-006 de `spec-iniciar-reserva.md`).
- **FR-006**: Si la API de Módulo 3 responde con un error de negocio (p. ej., "embarcación sin tarifa configurada", "rango no cotizable"), el sistema DEBE detener el flujo, abstenerse de emitir cotización y devolver el motivo de rechazo al caso de uso invocador.
- **FR-007**: Si la API de Módulo 3 no responde dentro de un tiempo de espera máximo [NEEDS CLARIFICATION: timeout para cotización, p. ej. 3000 ms], el sistema DEBE interrumpir la llamada y notificar al invocador el fallo de comunicación externa, sin permitir avanzar a la creación de la reserva.
- **FR-008**: Si los parámetros de la ventana temporal o la embarcación son modificados por el Arrendatario antes de formalizar la reserva, el sistema DEBE invalidar cualquier cotización previamente recibida y solicitar una nueva cotización a Módulo 3.
- **FR-009**: El sistema DEBE asegurar que la información de cotización provista quede vinculada unívocamente a los parámetros de la ventana temporal para la cual fue calculada, impidiendo que una cotización de un horario sea aplicada a otro.

### Key Entities

- **Cotización de Reserva (`ReservationQuote`)**: Estructura de datos temporal que encapsula la respuesta financiera provista por Módulo 3. Atributos clave: id_cotización (si lo emite Módulo 3), embarcación_id, fecha_hora_inicio, fecha_hora_fin, monto_total, desglose_informativo (tarifa_base, costo_seguro, monto_deposito_garantía), timestamp_emisión, vigencia_hasta [NEEDS CLARIFICATION].
- **Ventana Temporal (`ReservationWindow`)**: Entidad validada por "Establecer tiempo reserva" (fecha/hora inicio y fin) que sirve como insumo obligatorio para la cotización.
- **Embarcación**: Activo referenciado por identificador único (UUID), cuyos atributos físicos y operativos pertenecen a Módulo 1.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de las cotizaciones provistas a "Iniciar reserva" provienen directamente de la API `Solicitar cotización para reserva` de Módulo 3, con cero (0%) operaciones de cálculo o manipulación de tarifas en Módulo 2.
- **SC-002**: El sistema entrega la información de cotización al caso de uso invocador en menos de 2 segundos desde la solicitud a Módulo 3 (asumiendo latencia de API externa dentro de SLA).
- **SC-003**: Cero (0%) reservas confirmadas, persistidas o enviadas a pasarela de pago sin una cotización válida y vigente emitida por Módulo 3.
- **SC-004**: El 100% de los errores y timeouts de la API de Módulo 3 son capturados y notificados limpiamente al Arrendatario, impidiendo la emisión de cotizaciones nulas o inconsistentes.
- **SC-005**: El 100% de las modificaciones de ventana horaria previas a la reserva provocan la anulación de la cotización anterior y la obtención de una nueva cotización coherente con los nuevos parámetros.
