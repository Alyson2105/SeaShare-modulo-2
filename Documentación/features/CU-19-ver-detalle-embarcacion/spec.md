# Feature Specification: Ver detalle de embarcación

**Módulo**: Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones  
**Fecha de Creación**: 2026-09-28  
**Actores Primarios**: Arrendatario (Turista / Cliente)  
**Dependencias Externas (APIs)**: Ninguna directa (consume APIs externas a través de los casos de uso subordinados).
- **Casos de uso internos de Módulo 2**:
    - `Buscar embarcaciones disponibles` (`<<extend>>`): Este caso de uso se extiende desde la búsqueda cuando el usuario selecciona un activo específico del catálogo.
    - `Iniciar reserva` (`<<extend>>`): Este caso de uso extiende hacia la creación de la reserva cuando el usuario decide proceder con los datos ingresados.
    - `Proveer información embarcación` (`<<include>>`): Para obtener los datos técnicos del activo, puerto y capacidad máxima desde Módulo 1.
    - `Proveer información cotización de reserva` (`<<include>>`): Para obtener el monto total del viaje directamente desde Módulo 3.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Visualizar la ficha técnica y validar la capacidad máxima (Priority: P1)

Como Arrendatario que seleccionó una embarcación desde el catálogo, quiero ver su información detallada (capacidad máxima, puerto, servicios) e ingresar la cantidad de pasajeros y el rango de fechas de mi viaje, para que el sistema valide que mi grupo no supere el límite físico del activo.

**Why this priority**: Es el paso de validación física del inventario. Previene que los usuarios intenten cotizar o reservar un viaje para un número de personas que la embarcación no puede soportar legal ni operativamente.

**Independent Test**: Se prueba seleccionando una embarcación con capacidad máxima conocida (ej. 8 pasajeros) e ingresando valores válidos (ej. 5) e inválidos (ej. 10). Se verifica que el sistema invoque `Proveer información embarcación`, muestre los detalles técnicos y bloquee el avance si se excede la capacidad.

**Acceptance Scenarios**:

1. **Scenario**: Selección de pasajeros dentro de la capacidad permitida
    - **Given** una embarcación seleccionada cuya capacidad máxima es de 10 pasajeros (obtenida de Módulo 1)
    - **When** el Arrendatario ingresa un rango de fechas y selecciona 6 pasajeros
    - **Then** el sistema valida exitosamente la entrada, permite continuar en la vista de detalle y se prepara para solicitar la cotización

2. **Scenario**: Bloqueo por exceder la capacidad máxima de la embarcación
    - **Given** una embarcación seleccionada cuya capacidad máxima es de 6 pasajeros
    - **When** el Arrendatario intenta ingresar 8 pasajeros en el selector
    - **Then** el sistema bloquea la entrada o muestra un error inmediato indicando que se ha superado la capacidad máxima, impidiendo solicitar cotizaciones o iniciar la reserva

---

### User Story 2 - Visualizar el precio total del rango de fechas sin cálculos locales (Priority: P1)

Como Arrendatario, quiero ver el precio total exacto de mi viaje para el rango de fechas y pasajeros que ingresé, junto con la cantidad de días seleccionados, para tomar la decisión de avanzar hacia la reserva conociendo el costo dictaminado por Finanzas.

**Why this priority**: Cumple con la regla de oro de la plataforma: transparencia financiera delegada a Módulo 3. Permite al usuario conocer el presupuesto oficial sin que Módulo 2 asuma riesgos aritméticos.

**Independent Test**: Se prueba ingresando fechas y pasajeros válidos y verificando el payload devuelto por `Proveer información cotización de reserva`. Se corrobora que la interfaz de Módulo 2 muestre exclusivamente el monto total recibido y la cantidad de días, sin efectuar divisiones para mostrar "tarifas por noche".

**Acceptance Scenarios**:

1. **Scenario**: Visualización del total provisto por Módulo 3 más cantidad de días
    - **Given** un Arrendatario que seleccionó 3 días de viaje y 4 pasajeros para una embarcación válida
    - **When** el sistema invoca `Proveer información cotización de reserva` en modo individual
    - **Then** el sistema recibe el total exacto desde Módulo 3 (ej. 1,500,000 COP) y presenta en la interfaz el texto "Total: 1,500,000 COP por 3 días", sin calcular, dividir ni exhibir ninguna tarifa por noche

2. **Scenario**: Falla en la obtención de la cotización para el detalle
    - **Given** un Arrendatario consultando fechas específicas en el detalle de la embarcación
    - **When** el caso de uso `Proveer información cotización de reserva` retorna error por indisponibilidad de Módulo 3
    - **Then** el sistema mantiene la ficha técnica visible pero deshabilita el botón de continuar hacia la reserva, mostrando un aviso de "Cotización temporalmente no disponible"

---

## Edge Cases

- **Prohibición de cálculos aritméticos (Tarifa por noche)**: Módulo 2 no debe dividir el total provisto por Módulo 3 entre la cantidad de días para inventar un "precio por noche". Solo expone el total oficial y los días involucrados.
- **Cambio dinámico de parámetros**: Si el usuario altera las fechas o los pasajeros en la vista de detalle, el sistema debe re-evaluar la capacidad máxima e invocar nuevamente la cotización para actualizar el total en pantalla.
- **Inconsistencia de datos desde Módulo 1**: Si al invocar `Proveer información embarcación` el activo no existe o retorna datos corruptos (ej. capacidad máxima = 0), el sistema debe mostrar un error de "Embarcación no disponible" y abortar la visualización del detalle.
- **Expiración del Temporizador**: Si el temporizador regresivo de la cotización expira (ej. llega a 00:00), el sistema debe invalidar la cotización actual, bloquear la transición hacia el pago o reserva y requerir una re-cotización a Módulo 3.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE recibir el identificador de la embarcación seleccionada desde `Buscar embarcaciones disponibles` (`<<extend>>`).
- **FR-002**: El sistema DEBE invocar obligatoriamente a `Proveer información embarcación` (`<<include>>`) para obtener los datos técnicos oficiales (capacidad máxima, puerto, servicios) sin persistirlos localmente.
- **FR-003**: El sistema DEBE proveer selectores y un calendario interactivo con campos visuales independientes para que el Arrendatario ingrese la cantidad de pasajeros y el rango de fechas (fecha de inicio y fecha de fin) deseado.
- **FR-004**: El sistema DEBE validar de forma estricta que la cantidad de pasajeros ingresada sea menor o igual a la capacidad máxima técnica de la embarcación.
- **FR-005**: Al definir fechas y pasajeros válidos, el sistema DEBE invocar a `Proveer información cotización de reserva` (`<<include>>`) en modo individual para obtener el total del viaje.
- **FR-006**: **REGLA DE NEGOCIO ESTRICTA (Sin dinero)**: El sistema **NO DEBE** calcular divisiones, tarifas por noche, ni manipular el total entregado por Módulo 3. DEBE exponer visualmente el total íntegro junto con la cantidad de días del rango seleccionado.
- **FR-007**: El sistema DEBE habilitar la transición hacia `Iniciar reserva` (`<<extend>>`) única y exclusivamente si la capacidad es válida y se ha obtenido una cotización exitosa de Módulo 3.
- **FR-008**: El sistema DEBE proveer un enlace de navegación de retorno (ej. *"‹ Volver a resultados"*) que permita al Arrendatario regresar al catálogo de búsqueda sin perder los filtros previos.
- **FR-009**: El sistema DEBE renderizar la ficha técnica de la embarcación desglosando los atributos oficiales de Módulo 1 en componentes visuales estructurados: capacidad máxima, tipo de navegación/capitán, eslora en pies y número de camarotes.
- **FR-010**: El sistema DEBE mostrar una sección informativa con las amenidades y comodidades disponibles de la embarcación (ej. equipo de esnórquel, Wi-Fi, nevera, tablas de paddle, etc.) y los datos de validación del propietario.



### Key Entities

- **Selección Temporal de Viaje**: Objeto en memoria transitoria (no persistido) que almacena el identificador de la embarcación, el rango de fechas, la cantidad de pasajeros y el monto total cotizado, que será transferido a `Iniciar reserva` si el usuario decide avanzar.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Cero (0%) operaciones matemáticas de división o cálculo de tarifas por noche ejecutadas en Módulo 2.
- **SC-002**: El 100% de los intentos de ingresar pasajeros por encima de la capacidad máxima de la embarcación son bloqueados en la interfaz antes de solicitar cotización.
- **SC-003**: Cero (0%) persistencia en base de datos de Módulo 2 de los datos técnicos de la embarcación obtenidos desde Módulo 1.