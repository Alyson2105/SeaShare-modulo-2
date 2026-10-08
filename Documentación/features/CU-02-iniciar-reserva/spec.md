# Feature Specification: Iniciar Reserva

**Módulo**: Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones  
**Fecha de Creación**: 2026-09-08 (Actualizado: 2026-09-28 por corrección UML de dependencias extend)  
**Actores Primarios**: Arrendatario (Turista / Cliente)  
**Dependencias Externas (APIs)**:
- **Módulo 1 (Gestión de Flota y Activos P2P)**: API externa `Consultar información de embarcación` para validación. En estado `Iniciada` NO se notifica bloqueo de inventario a Módulo 1; el bloqueo operativo se aplica únicamente al transicionar a `Pendiente de Pago` (vía `Iniciar pago`).
- **Casos de uso internos de Módulo 2**:
    - `Ver detalle de embarcación` (`<<extend>>`): Este caso de uso (`Iniciar reserva`) **es la extensión** que se ancla a `Ver detalle de embarcación`. Se activa cuando el Arrendatario decide iniciar el proceso de reserva.
    - `Buscar embarcaciones disponibles` (`<<extend>>`): Este caso de uso (`Iniciar reserva`) **es la extensión** que se ancla a `Buscar embarcaciones disponibles`. Se activa cuando el Arrendatario inicia la reserva directamente desde la tarjeta del catálogo.
    - `Actualizar estado reserva` (`<<include>>`): Para crear la reserva formalmente en estado `Iniciada` (persistida en base de datos) y encender el TTL de 15 minutos. En este estado NO se notifica bloqueo a Módulo 1.
    - `Brindar información de estado operativo` (`<<include>>`): Para verificar, vía la API de Módulo 1, que la embarcación figura como `Disponible` antes de crear la reserva.

---

## User Scenarios & Testing

### User Story 1 - Completar datos, crear la reserva en estado Iniciada y arrancar el TTL (Priority: P1)

Como Arrendatario, una vez validados los detalles de mi viaje, quiero **llenar mis datos personales obligatorios (nombre completo del titular y celular)** para que la reserva quede registrada y persistida en base de datos en estado `Iniciada`, encendiendo su ventana de 15 minutos. En este estado NO se bloquea el inventario en Módulo 1.

***Why this priority***: Es el punto de entrada principal a la persistencia del marketplace. La reserva nace en `Iniciada` con su TTL en curso; el bloqueo de inventario se aplica recién al iniciar el pago.

***Independent Test***: Se prueba accediendo desde el catálogo o desde el detalle de la embarcación. Se ingresan los datos y se verifica que el sistema llame a `Actualizar estado reserva`, creando la reserva persistida en estado `Iniciada`, iniciando el TTL, y que NO se bloquee el inventario en Módulo 1 en este estado.

***Acceptance Scenarios***:

1. **Scenario**: Creación exitosa de la reserva en estado Iniciada (persistida, sin bloqueo Módulo 1)
    - **Given** un Arrendatario que proviene del detalle de embarcación y completó sus datos obligatorios (nombre completo y celular)
    - **When** acciona la intención de reservar (confirma el checkout)
    - **Then** el sistema persiste la reserva en base de datos asociándola a ese nombre y contacto, invoca a `Actualizar estado reserva` (`<<include>>`) fijando el estado en `Iniciada`, enciende el TTL de 15 minutos y deja los datos listos para el pago. En este estado **NO se bloquea el inventario en Módulo 1**.

2. **Scenario**: Dos reservas en `Iniciada` compiten por las mismas fechas y la carrera se resuelve al pagar (First-Come First-Served → HTTP 409)
    - **Given** dos Arrendatarios con reservas en estado `Iniciada` para el mismo barco y las mismas fechas (ambas coexisten; en `Iniciada` no hay retención de inventario en Módulo 1)
    - **When** ambos ejecutan `Iniciar pago` para la misma embarcación y fechas
    - **Then** el sistema aplica control de concurrencia atómico bajo criterio First-Come First-Served (FCFS) al transicionar a `Pendiente de Pago`: solo la primera transacción confirmada gana el bloqueo en Módulo 1 y retorna **HTTP 409 Conflict** al segundo usuario indicando que las fechas acaban de ser reservadas.

3. **Scenario**: Abandono de la intención de reserva por falta de datos
    - **Given** un Arrendatario configurando su reserva
    - **When** intenta avanzar sin ingresar su nombre completo o sin proveer un número de celular válido
    - **Then** la validación falla, el sistema rechaza la operación informando el error, la reserva no se crea y el barco NO se bloquea.

---

### Edge Cases

- **Reserva persistida sin bloqueo de inventario en Iniciada**: Al confirmar el checkout, la reserva nace persistida en base de datos en estado `Iniciada` y arranca el TTL de 15 minutos. En este estado NO se bloquea el inventario en Módulo 1. Si el usuario abandona el flujo y el TTL vence, el sistema (vía motor de estados) expira pasivamente la reserva.
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
- **FR-010**: El sistema DEBE validar que las fechas sigan disponibles en Módulo 1 antes de proceder con la creación al presionar el botón de continuar, mediante la invocación `(<<include>>)` a `Brindar información de estado operativo`. Este chequeo es previo y no bloquea inventario: la garantía de exclusividad (FCFS) se resuelve de forma atómica en `Iniciar pago` al transicionar a `Pendiente de Pago`.
- **FR-011**: Si los datos son válidos y hay disponibilidad, el sistema DEBE invocar a `Actualizar estado reserva` (`<<include>>`) para persistir la reserva en base de datos en estado `Iniciada`.
- **FR-012**: Al asentar la reserva en `Iniciada`, el sistema DEBE iniciar el temporizador TTL de 15 minutos asociado a esa transacción.
- **FR-013**: En estado `Iniciada`, el sistema NO DEBE notificar bloqueo de inventario a Módulo 1. El aviso a Módulo 1 (`Asignar estado operativo` → `Reservado`) ocurre exclusivamente al transicionar la reserva a `Pendiente de Pago` mediante `Iniciar pago`.
- **FR-014**: Varias reservas en `Iniciada` pueden coexistir para la misma embarcación y fechas (sin retención en Módulo 1 durante `Iniciada`). Ante colisión concurrente de `Iniciar pago`, el sistema DEBE resolver por First-Come First-Served (FCFS) al transicionar a `Pendiente de Pago`, validando de forma atómica la disponibilidad en Módulo 1 y con bloqueo pesimista de fila: solo la primera transacción confirmada gana el bloqueo (`Reservado` en Módulo 1); al competidor perdedor se le retorna **HTTP 409 Conflict** indicando que las fechas acaban de ser tomadas por otro usuario.
---

### Key Entities

- **Reserva (`Reservation`)**: Entidad de Módulo 2 que se crea y persiste por primera vez en estado `Iniciada`, vinculada al nombre y celular capturados. Su creación detona el temporizador TTL de 15 minutos. En este estado NO se bloquea el inventario en Módulo 1; el bloqueo operativo se aplica al transicionar a `Pendiente de Pago`.

---

## Success Criteria

### Measurable Outcomes

- **SC-001**: El 100% de las acciones de registro válidas persisten la reserva en estado `Iniciada` y arrancan el TTL de 15 minutos, sin bloquear el inventario en Módulo 1 en este estado.
- **SC-002**: Cero (0%) sobreventas cuando dos usuarios compiten por la misma embarcación y fechas: a lo sumo una sola reserva alcanza `Pendiente de Pago`/`Reservada`; el competidor perdedor recibe HTTP 409 Conflict en `Iniciar pago`.
- **SC-003**: Cero (0) operaciones aritméticas o cálculos de tarifas ejecutados internamente por este caso de uso.
