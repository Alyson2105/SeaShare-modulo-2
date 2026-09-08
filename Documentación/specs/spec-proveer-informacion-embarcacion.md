# Feature Specification: Proveer Información de Embarcación

**Módulo**: Módulo 2 (Operación de Reservas, Tiempos y Cancelaciones)  
**Created**: 2026-09-06  
**Primary Actor / Disparador**: Invocación interna desde el caso de uso `Iniciar reserva` (`<<include>>`) / Interfaz de integración con Módulo 1  
**External Dependencies (APIs)**:
- **Módulo 1 (Gestión de Flota y Activos P2P)**: API externa de catálogo de barcos (fuente única de verdad para conocer los datos del barco: código UUID, nombre, matrícula, tipo, número máximo de pasajeros, puerto de origen con GPS, servicios incluidos y dueño).

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consultar los datos del barco y verificar que no se supere la capacidad de pasajeros (Priority: P1)

Al comenzar el proceso de reserva, el caso de uso 'Iniciar reserva' (`<<include>>`) consulta a esta interfaz para obtener la información técnica del barco que el cliente quiere alquilar. El sistema consulta a la API de Módulo 1 enviando el código del barco. El Módulo 1 responde con los datos oficiales (nombre, tipo de barco, número máximo de personas permitidas, puerto de salida, servicios y dueño). En ese instante, el sistema comprueba que el número de pasajeros ingresado por el cliente no supere el límite permitido por el barco y le entrega los datos al proceso de reserva.

**Why this priority**: Es el paso obligatorio para garantizar la seguridad del viaje y asegurar que la reserva se realice sobre un barco registrado y sin exceso de personas.

**Independent Test**: Se prueba conectando el sistema a un simulador de Módulo 1 que responda con datos de diferentes barcos (lanchas, yates, catamaranes) y límites de personas. Se comprueba que el sistema entrega los datos correctamente y bloquea la reserva si el cliente intenta viajar con más personas de las permitidas.

**Acceptance Scenarios**:

1. **Scenario**: Obtención de datos exitosa con un número de pasajeros permitido
    - **Given** un barco registrado en Módulo 1 con capacidad máxima para 8 personas
    - **When** el cliente solicita la reserva para 6 personas
    - **Then** el sistema confirma que no se supera el límite y le entrega al proceso "Iniciar reserva" los datos del barco (nombre, matrícula, tipo, puerto GPS, servicios y dueño)

2. **Scenario**: Rechazo por intentar viajar con más personas de las permitidas
    - **Given** un barco registrado en Módulo 1 con capacidad máxima para 5 personas
    - **When** el cliente intenta hacer la reserva para 7 personas
    - **Then** el sistema rechaza la solicitud, informa que se supera la capacidad máxima del barco y detiene el proceso de reserva

3. **Scenario**: Barco no encontrado en el sistema de flota
    - **Given** una solicitud de reserva con un código de barco que no existe
    - **When** el sistema consulta a la API de Módulo 1
    - **Then** el sistema informa que el barco no está registrado en la flota y cancela la solicitud de inmediato

---

### User Story 2 - Obtener la ubicación exacta del puerto para calcular la hora local de la reserva (Priority: P2)

El sistema toma de los datos enviados por Módulo 1 la ubicación del puerto donde está amarrado el barco (coordenadas GPS y ciudad/puerto). Esta información se utiliza en las reglas de tiempo del Módulo 2 para saber la zona horaria del lugar, asegurando que el tiempo de espera (30 minutos) y las reglas de cancelación (72 horas y 24 horas) se calculen según la hora real del puerto donde está el barco.

**Why this priority**: Evita confusiones y errores con las horas cuando un cliente hace una reserva desde un país o ciudad con un horario distinto al del puerto donde sale el barco.

**Independent Test**: Se prueba consultando barcos ubicados en puertos con diferentes zonas horarias y verificando que el sistema identifica correctamente la hora local del puerto de salida.

**Acceptance Scenarios**:

1. **Scenario**: Suministro de puerto de salida y ubicación GPS
    - **Given** un barco con su puerto de salida registrado en Módulo 1
    - **When** se consulta la información del barco
    - **Then** el sistema entrega la ubicación del puerto y sus coordenadas para configurar las horas y reglas del viaje

---

### User Story 3 - Bloqueo de la reserva si el servicio de catálogo no responde (Priority: P2)

Si al consultar el Módulo 1 el servicio no responde, falla o tarda demasiado tiempo en contestar, el sistema detiene el proceso de reserva por seguridad. De esta forma se evita crear reservas con información incompleta o sin haber verificado si el número de pasajeros es seguro.

**Why this priority**: Evita que se guarden reservas a ciegas o con datos faltantes cuando falla la conexión con el catálogo de barcos.

**Independent Test**: Se prueba simulando una falla de conexión o una demora excesiva en la API de Módulo 1. Se verifica que el sistema cancela la consulta de forma segura y le muestra al usuario un mensaje indicando que el servicio de barcos no está disponible.

**Acceptance Scenarios**:

1. **Scenario**: Falla de conexión con el catálogo de Módulo 1
    - **Given** una consulta de información de un barco en proceso
    - **When** la API de Módulo 1 no responde en el tiempo máximo esperado
    - **Then** el sistema detiene la operación por seguridad y notifica que el servicio de flota no está accesible en ese momento

---

### Edge Cases

- **Prohibición de duplicar datos de barcos en Módulo 2**:
    - El Módulo 2 **NO guarda ni copia los detalles técnicos de los barcos en su base de datos**. Solo guarda los códigos de identificación (`embarcacion_id` y `propietario_id`), respetando que Módulo 1 es el único dueño de la información de la flota.
- **Prohibición de calcular precios o dinero en Módulo 2**:
    - Este caso de uso solo entrega información física y descriptiva del barco (tamaño, motor, servicios incluidos, capacidad, capitán); **NO maneja tarifas, no calcula precios por hora ni cobra dinero**. Todos los temas de precios provienen del Módulo 3.
- **Información siempre actualizada en tiempo real**:
    - Si el propietario cambia los servicios o datos del barco en Módulo 1, Módulo 2 siempre obtiene la información más reciente al hacer la consulta en tiempo real, evitando malentendidos con los clientes.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE ofrecer una interfaz interna para consultar los datos de los barcos, la cual DEBE ser llamada obligatoriamente por el caso de uso `Iniciar reserva` (`<<include>>`).
- **FR-002**: El sistema DEBE recibir como datos obligatorios: el código del barco y el número de pasajeros que viajarán.
- **FR-003**: El sistema DEBE consultar directamente la API externa de Módulo 1 para obtener la información oficial del barco seleccionado.
- **FR-004**: El sistema DEBE recibir y procesar los siguientes datos enviados por Módulo 1:
    - Código único del barco.
    - Nombre comercial y matrícula legal.
    - Tipo de barco (Lancha, Yate, Catamarán, etc.).
    - Capacidad máxima de pasajeros permitida.
    - Puerto de salida (coordenadas GPS y nombre del puerto).
    - Servicios e insumos incluidos (ej. Capitán, Combustible).
    - Código del propietario del barco.
- **FR-005**: El sistema DEBE verificar de forma estricta que la cantidad de pasajeros de la reserva sea menor o igual a la capacidad máxima permitida por el barco.
- **FR-006**: Si la cantidad de pasajeros supera el límite del barco, el sistema DEBE rechazar la solicitud, mostrar un aviso de exceso de personas y detener la reserva.
- **FR-007**: Si el barco no existe en Módulo 1, el sistema DEBE rechazar la solicitud e informar que el barco no fue encontrado.
- **FR-008**: Si la consulta a Módulo 1 falla por desconexión o tiempo de espera agotado, el sistema DEBE aplicar un bloqueo preventivo (*fail-safe*), cancelando el proceso e informando la falla de conexión.
- **FR-009**: **REGLA DE NEGOCIO ESTRICTA (Sin dinero):** El sistema **NO DEBE manejar precios, ni calcular cotizaciones, comisiones o garantías**. Su función es únicamente descriptiva y de control de capacidad física.
- **FR-010**: El sistema DEBE guardar un registro simple de la consulta realizada para mantener la trazabilidad del proceso de reserva.

---

### Key Entities

- **Consulta de Información (`VesselInfoQuery`)**: Datos intercambiados en la consulta. Incluye: código del barco, número de pasajeros solicitados y fecha/hora de la consulta.
- **Detalle del Barco (`VesselInfoResponse`)**: Información entregada por Módulo 1. Incluye: código del barco, nombre, matrícula, tipo, capacidad máxima de personas, puerto GPS, servicios incluidos y código del propietario.
- **Embarcación**: Activo registrado y administrado exclusivamente por el Módulo 1.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de las consultas de información técnica a Módulo 1 se responden en menos de 300 milisegundos en condiciones normales.
- **SC-002**: Cero (0%) reservas permitidas donde el número de personas supere la capacidad máxima oficial del barco.
- **SC-003**: Cero (0) accesos directos a la base de datos de Módulo 1 o cálculos de precios realizados dentro de Módulo 2.
- **SC-004**: El 100% de las fallas de conexión con Módulo 1 resultan en un rechazo preventivo seguro (*fail-safe*).
- **SC-005**: El 100% de las reservas válidas obtienen la ubicación del puerto de Módulo 1 para determinar la hora local del viaje.