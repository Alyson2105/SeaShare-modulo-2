# Feature Specification: Iniciar Reserva

**Módulo**: Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones  
**Fecha de Creación**: 2026-09-08 (Actualizado: valida y cotiza sin persistir reserva)  
**Actores Primarios**: Arrendatario (Turista / Cliente)  
**Dependencias Externas (APIs)**:
- **Módulo 1 (Gestión de Flota y Activos P2P)**: API externa `Consultar información de embarcación` para validar la existencia del activo, su puerto GPS y su zona horaria antes de permitir la reserva. (Nota: En este paso NO se llama a `Asignar estado operativo`, la embarcación no se bloquea).
- **Casos de uso internos de Módulo 2**:
    - `CU-01 Buscar embarcaciones disponibles` `(<<extend>>)`: Caso de uso opcional que amplía este flujo si el usuario necesita buscar una embarcación antes de iniciar la reserva. La condición de extensión es que el Arrendatario inicie el flujo sin un identificador de embarcación preseleccionado.

---

## User Scenarios & Testing

### User Story 1 - Configurar y validar la intención de reserva sin persistirla (Priority: P1)

Como Arrendatario, quiero seleccionar una embarcación, ingresar las fechas de mi viaje y la cantidad de pasajeros para validar mi intención de viaje y ver el costo estimado antes de decidir si procedo a pagar.

***Why this priority***: Es el punto de entrada principal al embudo de conversión del marketplace. Sin este paso, el cliente no puede formalizar su intención de viaje ni conocer el presupuesto estimado aplicable a sus fechas.

***Independent Test***: Se prueba seleccionando una embarcación válida en Módulo 1, ingresando fechas futuras y una cantidad de pasajeros válida. Se verifica que el sistema consulte la cotización estimada a Módulo 3 (a través de `Proveer cotización de reserva`), muestre la advertencia obligatoria, sin persistir ninguna reserva y sin bloquear la disponibilidad del barco en Módulo 1.

***Acceptance Scenarios***:

1. **Scenario**: Creación exitosa de la reserva preliminar
    - **Given** un Arrendatario autenticado y una embarcación validada en Módulo 1
    - **When** el usuario selecciona las fechas, horas y pasajeros, y presiona "Reservar"
    - **Then** el sistema valida los parámetros, presenta la cotización estimada y deja los datos listos para el flujo de `Iniciar pago`, sin crear ni persistir ninguna reserva y sin bloquear el activo físico

2. **Scenario**: Fechas pasadas o inválidas rechazadas por el motor de cotización
    - **Given** un Arrendatario configurando una reserva
    - **When** ingresa fechas en el pasado o una fecha de fin anterior a la de inicio
    - **Then** la validación falla, el sistema rechaza la operación informando el error y la reserva no se crea en la base de datos

---


### Edge Cases

- **Condición de No-Bloqueo**: Este caso de uso **no aparta la embarcación ni persiste ninguna reserva**. Múltiples usuarios pueden configurar la intención de viaje para el mismo barco y las mismas fechas al mismo tiempo. El primero que complete el caso de uso `Iniciar pago` será quien se quede con el bloqueo del inventario.
- **Abandono tras configurar la intención**: Si el Arrendatario configura su viaje pero nunca avanza a pagar, no queda ningún registro persistido que limpiar. Como no se crea temporizador TTL ni se bloquea inventario, el abandono no afecta la operación.
- **Búsqueda previa opcional**: El flujo puede ser extendido por `CU-01 Buscar embarcaciones disponibles` si el usuario no tiene un identificador de embarcación preseleccionado. La condición es que el Arrendatario desee explorar opciones antes de reservar.

---

## Requirements

### Functional Requirements

- **FR-001**: El sistema DEBE permitir al Arrendatario ingresar el identificador de la embarcación, fechas/horas pactadas de inicio y fin, y la cantidad de pasajeros.
- **FR-002**: El sistema DEBE consultar a la API externa de Módulo 1 (`Consultar información de embarcación`) para validar que el identificador existe y obtener el puerto de atraque y su zona horaria oficial.
- **FR-005**: Si la solicitud de reserva es válida, el sistema DEBE entregar los parámetros consolidados y validados (embarcación, fechas/horas, pasajeros y cotización) al flujo de `Iniciar pago`, sin persistir ninguna reserva en este caso de uso.
- **FR-006**: El sistema **NO DEBE** invocar llamadas de bloqueo hacia Módulo 1 en esta etapa. Como no se persiste ninguna reserva, nada de lo actuado aquí debe alterar la disponibilidad operativa de la embarcación física.
- **FR-007**: El sistema **NO DEBE** iniciar el temporizador TTL de 15 minutos en este caso de uso. El control de tiempo límite se delega estrictamente al caso de uso posterior (`Iniciar pago`).

---

### Key Entities

- **Reserva (`Reservation`)**: Entidad de Módulo 2 que **NO** se crea ni persiste en este flujo. Los parámetros operativos validados (fechas, barco, pasajeros) se entregan de forma transitoria al flujo de `Iniciar pago`.

---

## Success Criteria

### Measurable Outcomes

- **SC-001**: Cero (0%) reservas persistidas como resultado de la ejecución de este caso de uso.
- **SC-002**: Cero (0%) bloqueos operativos enviados a Módulo 1 derivados de la ejecución de este caso de uso.
- **SC-004**: Cero (0) operaciones aritméticas, redondeos o cálculos de tarifas ejecutados internamente por el código de Módulo 2.