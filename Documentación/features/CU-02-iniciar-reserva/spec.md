# Feature Specification: Iniciar Reserva

**Módulo**: Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones  
**Fecha de Creación**: 2026-09-08 (Actualizado: 2026-09-28 por corrección UML de dependencias extend)  
**Actores Primarios**: Arrendatario (Turista / Cliente)  
**Dependencias Externas (APIs)**:
- **Módulo 1 (Gestión de Flota)**: API externa `Consultar información de embarcación` para validación, y de forma indirecta (vía Actualizar estado reserva), la API `Asignar estado operativo` para aplicar el bloqueo físico de la embarcación durante los 15 minutos del TTL.
- **Casos de uso internos de Módulo 2**:
    - `Ver detalle de embarcación` (`<<extend>>`): Este caso de uso (`Iniciar reserva`) **es la extensión** que se ancla a `Ver detalle de embarcación`. Se activa cuando el Arrendatario decide iniciar el proceso de reserva.
    - `Buscar embarcaciones disponibles` (`<<extend>>`): Este caso de uso (`Iniciar reserva`) **es la extensión** que se ancla a `Buscar embarcaciones disponibles`. Se activa cuando el Arrendatario inicia la reserva directamente desde la tarjeta del catálogo.
    - `Actualizar estado reserva` (`<<include>>`): Para crear la reserva formalmente en estado `Iniciada`, encender el TTL de 15 minutos y notificar el bloqueo a Módulo 1.

---

## User Scenarios & Testing

### User Story 1 - Completar datos, crear la reserva en estado Iniciada y apartar la embarcación (Priority: P1)

Como Arrendatario, una vez validados los detalles de mi viaje, quiero **llenar mis datos personales obligatorios (nombre completo del titular y celular)** para que la reserva quede registrada en estado `Iniciada`, encendiendo su ventana de 15 minutos y apartando temporalmente la embarcación para que nadie más la pueda tomar mientras yo decido si procedo a pagar.

***Why this priority***: Es el punto de entrada principal a la persistencia del marketplace y el mecanismo que protege la disponibilidad del inventario para el usuario mientras completa su transacción.

***Independent Test***: Se prueba accediendo desde el catálogo o desde el detalle de la embarcación. Se ingresan los datos y se verifica que el sistema llame a `Actualizar estado reserva`, creando la reserva en estado `Iniciada`, iniciando el TTL y validando que Módulo 1 bloquee el activo.

***Acceptance Scenarios***:

1. **Scenario**: Creación exitosa de la reserva preliminar y bloqueo de inventario
    - **Given** un Arrendatario que proviene del detalle de embarcación y completó sus datos obligatorios (nombre completo y celular)
    - **When** acciona la intención de reservar
    - **Then** el sistema persiste la reserva asociándola a ese nombre y contacto, invoca a `Actualizar estado reserva` (`<<include>>`) fijando el estado en `Iniciada`, enciende el TTL de 15 minutos, **bloquea la disponibilidad de la embarcación en Módulo 1**, y deja los datos listos para el pago.

2. **Scenario**: Colisión por concurrencia al intentar apartar el barco (Control de sobreventa)
    - **Given** dos Arrendatarios intentando crear una reserva para el mismo barco y las mismas fechas al mismo tiempo
    - **When** ambos envían sus datos simultáneamente
    - **Then** el sistema aplica control de concurrencia atómico, permite que solo la primera transacción entre a `Iniciada` (ganando el bloqueo de 15 minutos) y rechaza la segunda informando que las fechas ya no están disponibles.

3. **Scenario**: Abandono de la intención de reserva por falta de datos
    - **Given** un Arrendatario configurando su reserva
    - **When** intenta avanzar sin ingresar su nombre completo o sin proveer un número de celular válido
    - **Then** la validación falla, el sistema rechaza la operación informando el error, la reserva no se crea y el barco NO se bloquea.

---

### Edge Cases

- **Bloqueo Temporal Garantizado**: Durante los 15 minutos del TTL, la embarcación está fuera del mercado para las fechas seleccionadas. Si el usuario abandona el flujo y el TTL vence, el sistema (vía motor de estados) expira pasivamente la reserva y notifica a Módulo 1 que vuelva a liberar la embarcación.
- **Desacople de validaciones iniciales**: Al ser una extensión, asume que la embarcación seleccionada proviene de un flujo válido previo (desde la vista de detalle ).
- **Cancelación mediante Modal**: Si el usuario presiona el botón de cerrar ("X") en la esquina superior derecha del modal de confirmación, el sistema debe abortar el proceso de inicio de reserva, limpiar el formulario y devolver al usuario a la pantalla anterior sin aplicar ningún bloqueo en Módulo 1.

---

## Requirements

### Functional Requirements

- **FR-001**: El sistema DEBE recibir los parámetros de la embarcación, fechas, pasajeros y montos provenientes de los casos base a los que extiende (`Buscar embarcaciones disponibles` o `Ver detalle de embarcación`).
- **FR-002**: El sistema DEBE presentar la interfaz de captura de datos mediante una ventana modal superpuesta con el título "CONFIRMA LOS DATOS DE TU RESERVA".
- **FR-003**: El sistema DEBE mostrar en la parte superior del modal una tarjeta de resumen que incluya la fotografía, nombre y ubicación de la embarcación, junto con las fechas, número de noches y cantidad de pasajeros seleccionados.
- **FR-004**: El sistema DEBE presentar la información financiera mostrando únicamente la "Tarifa base" y el subtotal de las noches seleccionadas (ej. "$250 × 2 noches" resultando en "$500"), acompañada directamente debajo por la nota aclaratoria: "Monto previo. El valor final y vinculante se confirma en el paso de pago."
- **FR-005**: El sistema DEBE mostrar un banner informativo (color amarillo) que advierta al usuario sobre el efecto de su acción: "Al continuar, tu reserva queda Iniciada y arranca tu ventana de 15 minutos."
- **FR-006**: El sistema DEBE proveer un formulario ("Datos de contacto del titular") para capturar obligatoriamente el "Nombre completo del titular" y el "Celular de contacto".
- **FR-007**: El sistema DEBE proveer en el mismo formulario un campo de texto opcional para capturar el "Correo electrónico" del titular.
- **FR-008**: El sistema DEBE validar visualmente el formulario en caso de datos faltantes o incorrectos, mostrando un banner de error general (ej. "Completa el nombre del titular y el celular de contacto para continuar."), marcando los bordes de los campos afectados en rojo y desplegando mensajes de ayuda específicos debajo de cada input (ej. "Ingresa el nombre del titular de la reserva.").
- **FR-009**: El sistema DEBE incluir un botón de acción principal en la parte inferior del modal con el texto "Continuar a pagar", el cual debe estar deshabilitado visualmente si existen errores de validación en el formulario.
- **FR-010**: El sistema DEBE validar de forma atómica que las fechas sigan disponibles en Módulo 1 antes de proceder con la creación al presionar el botón de continuar.
- **FR-011**: Si los datos son válidos y hay disponibilidad, el sistema DEBE invocar a `Actualizar estado reserva` (`<<include>>`) para persistir la reserva en estado `Iniciada`.
- **FR-012**: Al asentar la reserva en `Iniciada`, el sistema DEBE iniciar el temporizador TTL de 15 minutos asociado a esa transacción.
- **FR-013**: Como efecto directo de pasar a `Iniciada`, el sistema DEBE asegurar (a través de `Actualizar estado reserva`) que se invoque a Módulo 1 para asignar el estado operativo de bloqueo temporal (`Reservado`) a la embarcación física.
---

### Key Entities

- **Reserva (`Reservation`)**: Entidad de Módulo 2 que se crea y persiste por primera vez en estado `Iniciada`, vinculada al nombre y celular capturados. Su creación detona el temporizador TTL y el bloqueo físico en Módulo 1, garantizando la exclusividad del inventario de cara al flujo de pago.

---

## Success Criteria

### Measurable Outcomes

- **SC-001**: El 100% de las acciones de registro válidas crean la reserva en estado `Iniciada`, arrancan el TTL y bloquean la embarcación en Módulo 1.
- **SC-002**: Cero (0%) sobreventas cuando dos usuarios intentan iniciar una reserva sobre el mismo inventario en el mismo instante.
- **SC-003**: Cero (0) operaciones aritméticas o cálculos de tarifas ejecutados internamente por este caso de uso.