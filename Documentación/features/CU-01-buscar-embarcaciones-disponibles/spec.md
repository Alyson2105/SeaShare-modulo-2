# Feature Specification: Buscar embarcaciones disponibles

**Módulo**: Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones  
**Fecha de Creación**: 2026-09-24 (Actualizado: 2026-09-28 por cambios de arquitectura)  
**Actores Primarios**: Arrendatario (Turista)  
**Dependencias Externas (APIs)**:
- **Módulo 1**: Módulo 2 NO consulta directamente a Módulo 1 desde este caso de uso; el acceso al catálogo pasa por el caso de uso interno `Proveer información de embarcación`, que es el único punto de integración con la API `Consultar información de embarcación` de Módulo 1.
- **Módulo 3**: integración indirecta, a través del caso de uso interno `CU-11 Proveer información cotización de reserva`.

- **Casos de uso internos de Módulo 2**:
    - `Proveer información de embarcación` (`<<include>>`): para obtener, en modo lote, los datos de catálogo de las embarcaciones recuperadas (nombre, tipo, puerto GPS, zona horaria) y mostrarlos en los resultados de búsqueda.
    - `CU-11 Proveer información cotización de reserva` (`<<include>>`): para obtener la estimación del costo en modo lote de las embarcaciones recuperadas y presentarla en los resultados de búsqueda.
    - `Ver detalle de embarcación` (`<<extend>>`): este caso de uso es extendido por la vista de detalle. La condición de extensión se cumple si el usuario desea seleccionar un activo específico del catálogo para ver su ficha técnica completa.

---

## User Scenarios & Testing

### User Story 1 - Búsqueda de embarcaciones por fechas y filtros (Priority: P1)

Como Arrendatario, quiero buscar embarcaciones disponibles utilizando fechas, cantidad de pasajeros o tipo de embarcación, para encontrar opciones que se ajusten a mis necesidades y visualizar su tarifa estimada.

***Why this priority***: Es el primer paso en la experiencia de usuario y permite la exploración del inventario. Sin este caso de uso, el usuario no podría encontrar embarcaciones para reservar.

***Independent Test***: Se prueba ingresando fechas futuras, una cantidad de pasajeros y el tipo de embarcacion. Se verifica que el sistema consulte disponibilidad en el catálogo (vía `Proveer información de embarcación`, modo lote) y utilice `CU-11 Proveer información cotización de reserva` para mostrar las opciones con tarifas estimadas calculadas por Módulo 3.

***Acceptance Scenarios***:

1. **Scenario**: Búsqueda exitosa con resultados disponibles
    - **Given** un Arrendatario que desea encontrar una embarcación
    - **When** el usuario ingresa fechas, tipo de embarcacion, cantidad de pasajeros y ejecuta la búsqueda
    - **Then** el sistema obtiene el inventario vía `Proveer información de embarcación` (modo lote), invoca `(<<include>>)` a "CU-11 Proveer información cotización de reserva" en su modalidad por lote y presenta los resultados con su costo estimado de referencia.

2. **Scenario**: Búsqueda sin resultados
    - **Given** un Arrendatario realizando una búsqueda
    - **When** ingresa fechas o filtros que no coinciden con ninguna embarcación disponible
    - **Then** el sistema informa que no hay embarcaciones disponibles para los criterios seleccionados.

---

### User Story 2 - Búsqueda sin filtros ingresados (Priority: P2)

Como Arrendatario, quiero poder ver el catálogo completo de embarcaciones disponibles sin tener que ingresar fechas ni cantidad de pasajeros, para explorar libremente las opciones antes de decidir cuándo y con cuántas personas viajar.

***Why this priority***: Permite la exploración casual del inventario, un patrón habitual en marketplaces de este tipo, sin obligar al usuario a comprometerse con datos del viaje desde el primer momento.

***Independent Test***: Se prueba ejecutando la búsqueda con los campos de fecha y pasajeros vacíos. Se verifica que el sistema devuelve todo el inventario disponible, sin aplicar ningún filtro de fecha o capacidad.

***Acceptance Scenarios***:

1. **Scenario**: Búsqueda sin ningún filtro ingresado
    - **Given** un Arrendatario que no ingresa fechas ni cantidad de pasajeros
    - **When** ejecuta la búsqueda
    - **Then** el sistema muestra TODO el inventario disponible, sin filtrar por fecha ni por capacidad, siguiendo el mismo flujo de catálogo (modo lote) + cotización (modo lote) que una búsqueda con filtros

---

### Edge Cases

- **Ausencia de Cotización Temporal**: Si Módulo 3 no puede proveer cotizaciones en lote debido a una caída del servicio, el sistema mostrará las embarcaciones disponibles pero indicará que el precio estimado no está disponible en este momento.
- **Búsqueda sin filtros**: Ver User Story 2. El sistema NO rechaza la búsqueda vacía; devuelve el catálogo completo.
- **Filtros con formato inválido**: Si el usuario ingresa un valor con formato incorrecto (texto donde se espera un número de pasajeros, un valor que no es una fecha válida en el campo de fecha), el sistema DEBE rechazar esa entrada específica e indicar el formato esperado, sin ejecutar la búsqueda. Esta validación es **únicamente de formato** — el sistema NO aplica aquí ninguna regla de negocio sobre las fechas (por ejemplo, no rechaza una fecha de fin anterior a la de inicio, ni una fecha ya pasada, ni un rango de fechas excesivamente largo); ese tipo de validación, si existe, se resuelve en un caso de uso posterior (como `Iniciar reserva`), no en la búsqueda.
- **Punto de Extensión Hacia Detalle**: Este caso de uso es el punto de partida que permite extender hacia `Ver detalle de embarcación` una vez que el usuario hace clic en una tarjeta del catálogo para avanzar en el embudo.

---

## Requirements

### Functional Requirements

- **FR-001**: El sistema DEBE permitir al Arrendatario ingresar criterios de búsqueda como fechas, cantidad de pasajeros y tipo de embarcación, todos ellos opcionales.
- **FR-002**: El sistema DEBE recuperar la lista de embarcaciones que coincidan con los criterios de búsqueda (incluyendo el tipo, cuando se especifique) invocando obligatoriamente `(<<include>>)` al caso de uso `Proveer información de embarcación` en modo lote. Módulo 2 NO accede directamente a Módulo 1 ni a su base de datos desde este caso de uso.
- **FR-003**: El sistema DEBE invocar obligatoriamente al caso de uso subordinado `CU-11 Proveer información cotización de reserva` `(<<include>>)` en su modo lote, transmitiendo los identificadores de las embarcaciones recuperadas.
- **FR-004**: El sistema DEBE mostrar los resultados al Arrendatario incluyendo la información básica de la embarcación —tales como nombre comercial, fotografía del activo, ubicación o puerto base, capacidad máxima de pasajeros y condiciones de navegación— junto con la tarifa estimada devuelta.
- **FR-005**: El sistema DEBE proveer un punto de extensión hacia `Ver detalle de embarcación` para cada resultado mostrado en el catálogo, interactuando mediante un botón de acción principal (`Reservar` o selección de tarjeta) que permita al usuario avanzar a la validación de capacidad.
- **FR-006**: El sistema DEBE proveer elementos de navegación superior y filtrado rápido en la interfaz, incluyendo una barra de búsqueda global por texto (*"Buscar barcos - marina, isla..."*), enlaces de acceso rápido (`Reservas`) y filtros rápidos por categoría (*Velero*, *Yate*, *Catamarán*).
- **FR-007**: El sistema DEBE mostrar de forma dinámica el contexto de los resultados en la interfaz, incluyendo una etiqueta descriptiva (ej. *"Populares esta semana"*) y un contador numérico total de elementos encontrados (ej. *24 resultados*).
- **FR-008**: El sistema DEBE paginar los resultados de búsqueda en lotes (*batches*) de **20 embarcaciones por página** en las consultas del catálogo, transmitiendo a `Proveer información de embarcación` y a `CU-11 Proveer información cotización de reserva` únicamente los identificadores del lote visible en la página actual.
- **FR-009**: Si el Arrendatario no ingresa fechas ni cantidad de pasajeros, el sistema DEBE ejecutar la búsqueda igualmente y devolver el catálogo completo disponible, sin aplicar filtro de fecha ni de capacidad.
- **FR-010**: Si alguno de los criterios ingresados tiene un formato inválido (no numérico donde se espera un número, no es una fecha válida donde se espera una fecha, o un tipo de embarcación que no corresponde a ninguna categoría del catálogo), el sistema DEBE rechazar la búsqueda e indicar el campo y el formato esperado, sin invocar a `Proveer información de embarcación` ni a `CU-11 Proveer información cotización de reserva`.
---

### Key Entities

- **Filtro de Búsqueda**: Objeto temporal en memoria que contiene los parámetros ingresados por el usuario (fechas, pasajeros, tipo de embarcación — todos opcionales).
- **Resultado de Búsqueda**: Lista de embarcaciones devueltas, cada una asociada a su cotización temporal de referencia.

---

## Success Criteria

### Measurable Outcomes

- **SC-001**: El 100% de los resultados presentados incluyen la tarifa estimada provista por Módulo 3 (salvo en caídas de servicio).
- **SC-002**: El tiempo de respuesta de la búsqueda, incluyendo la invocación en lote de cotizaciones, debe mantenerse por debajo de los límites de UX aceptables. [NEEDS CLARIFICATION: no hay un umbral de latencia numérico definido en la documentación del proyecto — tu propio plan.md ya lo marca como SLA pendiente para CU-01].
- **SC-003**: El 100% de las búsquedas sin filtros ingresados devuelven el catálogo completo disponible sin error.
- **SC-004**: El 100% de las entradas con formato inválido son rechazadas antes de invocar a Módulo 1 o a Módulo 3.
