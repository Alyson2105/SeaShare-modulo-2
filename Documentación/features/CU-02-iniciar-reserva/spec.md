# Feature Specification: Iniciar Reserva

**Módulo**: Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones  
**Fecha de Creación**: 2026-09-08 (Actualizado: al presionar Reservar crea la reserva en Iniciada e inicia el TTL)  
**Actores Primarios**: Arrendatario (Turista / Cliente)  
**Dependencias Externas (APIs)**:
- **Módulo 1 (Gestión de Flota y Activos P2P)**: API externa `Consultar información de embarcación` para validar la existencia del activo, su puerto GPS y su zona horaria antes de permitir la reserva. (Nota: En este paso NO se llama a `Asignar estado operativo`, la embarcación no se bloquea).
- **Casos de uso internos de Módulo 2**:
    - `CU-01 Buscar embarcaciones disponibles` `(<<extend>>)`: Caso de uso opcional que amplía este flujo si el usuario necesita buscar una embarcación antes de iniciar la reserva. La condición de extensión es que el Arrendatario inicie el flujo sin un identificador de embarcación preseleccionado.
    - `Actualizar estado reserva` `(<<include>>)`: Para crear la reserva en estado `Iniciada` e iniciar el TTL de 15 minutos al presionar "Reservar".

---

## User Scenarios & Testing

### User Story 1 - Crear la reserva en estado Iniciada al presionar Reservar (Priority: P1)

Como Arrendatario, quiero presionar "Reservar" y comenzar a llenar los datos de mi viaje (embarcación, fechas) para que la reserva quede registrada en estado `Iniciada`, iniciando su ventana de 15 minutos, y ver el costo estimado antes de decidir si procedo a pagar.

***Why this priority***: Es el punto de entrada principal al embudo de conversión del marketplace. Sin este paso, el cliente no puede formalizar su intención de viaje ni conocer el presupuesto estimado aplicable a sus fechas.

***Independent Test***: Se prueba seleccionando una embarcación válida en Módulo 1, ingresando fechas futuras. Se verifica que el sistema consulte la cotización estimada a Módulo 3 (a través de `Proveer cotización de reserva`), muestre la advertencia obligatoria, creando la reserva en estado `Iniciada` con su TTL en curso y sin bloquear la disponibilidad del barco en Módulo 1.

***Acceptance Scenarios***:

1. **Scenario**: Creación exitosa de la reserva preliminar
    - **Given** un Arrendatario autenticado y una embarcación validada en Módulo 1
    - **When** el usuario selecciona las fechas y presiona "Reservar"
    - **Then** el sistema crea la reserva en estado `Iniciada` a través de `Actualizar estado reserva` (`<<include>>`), enciende el TTL de 15 minutos, presenta la cotización estimada y deja los datos listos para el flujo de `Iniciar pago`, sin bloquear el activo físico

2. **Scenario**: Fechas pasadas o inválidas rechazadas por el motor de cotización
    - **Given** un Arrendatario configurando una reserva
    - **When** ingresa fechas en el pasado o una fecha de fin anterior a la de inicio
    - **Then** la validación falla, el sistema rechaza la operación informando el error y la reserva no se crea en la base de datos

---


### Edge Cases

- **Condición de No-Bloqueo**: Este caso de uso **no aparta la embarcación**. Múltiples usuarios pueden tener reservas en estado `Iniciada` para el mismo barco y las mismas fechas al mismo tiempo, sin bloqueo. El primero que complete el caso de uso `Iniciar pago` (transición a `Pendiente de Pago`) será quien se quede con el bloqueo del inventario.
- **Abandono del formulario antes del pago**: Si el Arrendatario abandona el llenado de datos antes de llegar al pago, la reserva permanece en estado `Iniciada` con su TTL en curso. Qué ocurre al vencer el TTL en este estado no está definido en ningún spec [ver Duda D-01 en la lista de dudas al final de esta tarea].
- **Búsqueda previa opcional**: El flujo puede ser extendido por `CU-01 Buscar embarcaciones disponibles` si el usuario no tiene un identificador de embarcación preseleccionado. La condición es que el Arrendatario desee explorar opciones antes de reservar.

---

## Requirements

### Functional Requirements

- **FR-001**: El sistema DEBE permitir al Arrendatario ingresar el identificador de la embarcación y las fechas pactadas de inicio y fin.
- **FR-002**: El sistema DEBE consultar a la API externa de Módulo 1 (`Consultar información de embarcación`) para validar que el identificador existe y obtener el puerto de atraque y su zona horaria oficial.
- **FR-005**: Si la solicitud de reserva es válida, el sistema DEBE invocar a `Actualizar estado reserva` (`<<include>>`) para crear la reserva en estado `Iniciada` e iniciar el TTL de 15 minutos, y entregar los parámetros consolidados y validados (embarcación, fechas y cotización) al flujo de `Iniciar pago`.
- **FR-006**: El sistema **NO DEBE** invocar llamadas de bloqueo hacia Módulo 1 en esta etapa. Como no se persiste ninguna reserva, nada de lo actuado aquí debe alterar la disponibilidad operativa de la embarcación física.
- **FR-007**: Al presionar "Reservar" y comenzar el llenado de datos, el sistema DEBE iniciar el temporizador TTL de 15 minutos asociado a la reserva recién creada en estado `Iniciada`.

---

### Key Entities

- **Reserva (`Reservation`)**: Entidad de Módulo 2 que se crea y persiste por primera vez en estado `Iniciada` en este flujo (con su TTL en curso). Sus parámetros alimentan el flujo de `Iniciar pago`. La embarcación no se bloquea en esta etapa.

---

## Success Criteria

### Measurable Outcomes

- **SC-001**: El 100% de las acciones "Reservar" válidas crean la reserva en estado `Iniciada` con su TTL de 15 minutos en curso.
- **SC-002**: Cero (0%) bloqueos operativos enviados a Módulo 1 derivados de la ejecución de este caso de uso.
- **SC-004**: Cero (0) operaciones aritméticas, redondeos o cálculos de tarifas ejecutados internamente por el código de Módulo 2.