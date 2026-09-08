# Feature Specification: Iniciar Reserva

**Módulo**: Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones  
**Fecha de Creación**: 2026-09-08 (Actualizado para arquitectura de estado Borrador)  
**Actores Primarios**: Arrendatario (Turista / Cliente)  
**Dependencias Externas (APIs)**:
- **Módulo 1 (Gestión de Flota y Activos P2P)**: API externa `Consultar información de embarcación` para validar la existencia del activo, su puerto GPS y su zona horaria antes de permitir la reserva. (Nota: En este paso NO se llama a `Asignar estado operativo`, la embarcación no se bloquea).
- **Casos de uso internos de Módulo 2**:
    - `Proveer cotización de reserva` `(<<include>>)`: Para obtener la estimación del costo basada en las fechas seleccionadas.
    - `Actualizar estado reserva` `(<<include>>)`: Para registrar el nacimiento de la reserva en estado `Borrador`.

---

## User Scenarios & Testing

### User Story 1 - Configurar y registrar la intención de reserva en estado Borrador (Priority: P1)

Como Arrendatario, quiero seleccionar una embarcación, ingresar las fechas de mi viaje y la cantidad de pasajeros para crear una reserva preliminar y ver el costo estimado antes de decidir si procedo a pagar.

***Why this priority***: Es el punto de entrada principal al embudo de conversión del marketplace. Sin este paso, el cliente no puede formalizar su intención de viaje ni conocer el presupuesto estimado aplicable a sus fechas.

***Independent Test***: Se prueba seleccionando una embarcación válida en Módulo 1, ingresando fechas futuras y una cantidad de pasajeros válida. Se verifica que el sistema consulte la cotización estimada a Módulo 3 (a través de `Proveer cotización de reserva`), muestre la advertencia obligatoria, y guarde la reserva en estado `Borrador` sin bloquear la disponibilidad del barco en Módulo 1.

***Acceptance Scenarios***:

1. **Scenario**: Creación exitosa de la reserva preliminar
    - **Given** un Arrendatario autenticado y una embarcación validada en Módulo 1
    - **When** el usuario selecciona las fechas, horas y pasajeros, y presiona "Reservar"
    - **Then** el sistema invoca `(<<include>>)` a "Proveer cotización de reserva" obteniendo el valor estimado, muestra la advertencia obligatoria ("Valor estimado..."), e invoca `(<<include>>)` a "Actualizar estado reserva" para crear la reserva en estado `Borrador` sin bloquear el activo físico

2. **Scenario**: Fechas pasadas o inválidas rechazadas por el motor de cotización
    - **Given** un Arrendatario configurando una reserva
    - **When** ingresa fechas en el pasado o una fecha de fin anterior a la de inicio
    - **Then** la validación delegada en la cotización falla, el sistema rechaza la operación informando el error y la reserva no se crea en la base de datos

---

### User Story 2 - Mostrar la cotización estimada con la advertencia obligatoria de Finanzas (Priority: P1)

Como sistema, quiero asegurar que el Arrendatario reciba la cotización estimada exacta devuelta por el caso de uso subordinado, acompañada siempre del texto de advertencia obligatorio dictado por Módulo 3, para gestionar correctamente sus expectativas financieras antes del cobro final.

***Why this priority***: Módulo 2 no calcula dinero ni asume cargos ocultos. Al estar en la etapa de `Borrador`, el total devuelto es solo un estimado. Omitir la advertencia generaría problemas legales y reclamos cuando en el paso de pago se sumen el seguro y la garantía.

***Independent Test***: Se simula la creación de la reserva y se intercepta la interfaz gráfica para comprobar que el texto "Valor estimado. No incluye cargos adicionales ni depósito de seguridad" se renderiza exactamente como fue entregado por la integración, sin alteraciones.

***Acceptance Scenarios***:

1. **Scenario**: Visualización de la cotización estimada
    - **Given** un Arrendatario que acaba de configurar sus fechas y pasajeros
    - **When** el sistema recibe la respuesta de `(<<include>>)` "Proveer cotización de reserva"
    - **Then** el sistema presenta el monto en pantalla junto con la advertencia obligatoria íntegra, sin sumar ni calcular ningún valor adicional por su cuenta

---

### Edge Cases

- **Condición de No-Bloqueo**: A diferencia de versiones anteriores, crear una reserva en este caso de uso **no aparta la embarcación**. Múltiples usuarios pueden crear reservas en estado `Borrador` para el mismo barco y las mismas fechas al mismo tiempo. El primero que decida avanzar al caso de uso `Iniciar pago` será quien se quede con el bloqueo del inventario.
- **Abandono de la Reserva en Borrador**: Si el Arrendatario crea la reserva (estado `Borrador`) pero nunca avanza a pagar, el registro quedará inactivo. Como no tiene temporizador TTL ni bloquea inventario, el sistema puede limpiar estos registros pasivamente mediante rutinas de mantenimiento sin afectar la operación.
- **Indisponibilidad de la cotización**: Si `Proveer cotización de reserva` retorna un error porque Módulo 3 está caído o el barco no tiene tarifas configuradas, el sistema DEBE detener la creación de la reserva y mostrar un error controlado, prohibiendo la creación de un `Borrador` sin precio de referencia.

---

## Requirements

### Functional Requirements

- **FR-001**: El sistema DEBE permitir al Arrendatario ingresar el identificador de la embarcación, fechas/horas pactadas de inicio y fin, y la cantidad de pasajeros.
- **FR-002**: El sistema DEBE consultar a la API externa de Módulo 1 (`Consultar información de embarcación`) para validar que el identificador existe y obtener el puerto de atraque y su zona horaria oficial.
- **FR-003**: El sistema DEBE invocar obligatoriamente al caso de uso subordinado `Proveer cotización de reserva` `(<<include>>)` en su modo individual, transmitiéndole las fechas y la embarcación.
- **FR-004**: El sistema DEBE capturar el precio estimado devuelto y exhibir obligatoriamente en la interfaz de usuario el siguiente texto inmutable asociado a la cotización: *"Valor estimado. No incluye cargos adicionales ni depósito de seguridad"*.
- **FR-005**: Si la solicitud de reserva es válida y se obtuvo la cotización, el sistema DEBE invocar al caso de uso orquestador `Actualizar estado reserva` `(<<include>>)` solicitando la creación de la reserva en el estado principal `Borrador`.
- **FR-006**: El sistema **NO DEBE** invocar llamadas de bloqueo hacia Módulo 1 en esta etapa. El estado `Borrador` no debe alterar la disponibilidad operativa de la embarcación física.
- **FR-007**: El sistema **NO DEBE** iniciar el temporizador TTL de 15 minutos en este caso de uso. El control de tiempo límite se delega estrictamente al caso de uso posterior (`Iniciar pago`).
- **FR-008**: **REGLA DE NEGOCIO ESTRICTA (Sin cálculo financiero):** El sistema **NO DEBE** manipular el valor devuelto por la cotización, no debe intentar sumar seguros, comisiones ni depósitos por su cuenta, ni realizar validaciones de reglas tarifarias. Toda la matemática y validación de fechas a nivel de costo pertenece a Módulo 3.

---

### Key Entities

- **Reserva (`Reservation`)**: Entidad de Módulo 2 que se crea y persiste por primera vez en estado `Borrador` tras este flujo, conteniendo los parámetros operativos (fechas, barco, pasajeros) y la cotización estimada de referencia.

---

## Success Criteria

### Measurable Outcomes

- **SC-001**: El 100% de las reservas recién creadas nacen exclusivamente en estado `Borrador`.
- **SC-002**: Cero (0%) bloqueos operativos enviados a Módulo 1 derivados de la ejecución de este caso de uso.
- **SC-003**: El 100% de las cotizaciones mostradas en este paso despliegan la advertencia textual requerida por Módulo 3.
- **SC-004**: Cero (0) operaciones aritméticas, redondeos o cálculos de tarifas ejecutados internamente por el código de Módulo 2.