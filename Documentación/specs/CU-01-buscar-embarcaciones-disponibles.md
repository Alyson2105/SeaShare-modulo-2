# Feature Specification: Buscar embarcaciones disponibles

**Módulo**: Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones  
**Fecha de Creación**: 2026-09-24  
**Actores Primarios**: Arrendatario (Turista / Cliente)  
**Dependencias Externas (APIs)**: 
**Modulo 1** :API Externa `Consultar información de embarcación` para poder mostrar todo el catálogo de embarcaciones disponibles, su puerto GPS y su zona horario antes de permitir realizar la reserva.
**Módulo 3** API Externa `Proveer información cotización de reserva` `(<<include>>)`: Para obtener la estimación del costo en lote y presentarla en los resultados de búsqueda.
- **Casos de uso internos de Módulo 2**:
    - `CU-02 Iniciar reserva`: Este caso de uso extiende `(<<extend>>)` a Iniciar reserva. La condición de extensión se cumple si el usuario desea buscar y seleccionar una embarcación antes de formalizar la intención de reserva.

---

## User Scenarios & Testing

### User Story 1 - Búsqueda de embarcaciones por fechas y filtros (Priority: P1)

Como Arrendatario, quiero buscar embarcaciones disponibles utilizando fechas, cantidad de pasajeros y filtros, para encontrar opciones que se ajusten a mis necesidades y visualizar su tarifa estimada.

***Why this priority***: Es el primer paso en la experiencia de usuario y permite la exploración del inventario. Sin este caso de uso, el usuario no podría encontrar embarcaciones para reservar.

***Independent Test***: Se prueba ingresando fechas futuras y una cantidad de pasajeros. Se verifica que el sistema consulte disponibilidad en el catálogo y utilice `CU-11 Proveer información cotización de reserva` para mostrar las opciones con tarifas estimadas calculadas por Módulo 3.

***Acceptance Scenarios***:

1. **Scenario**: Búsqueda exitosa con resultados disponibles
    - **Given** un Arrendatario que desea encontrar una embarcación
    - **When** el usuario ingresa fechas, horas, cantidad de pasajeros y ejecuta la búsqueda
    - **Then** el sistema obtiene el inventario, invoca `(<<include>>)` a "CU-11 Proveer información cotización de reserva" en su modalidad por lote y presenta los resultados con su costo estimado de referencia.

2. **Scenario**: Búsqueda sin resultados
    - **Given** un Arrendatario realizando una búsqueda
    - **When** ingresa fechas o filtros que no coinciden con ninguna embarcación disponible
    - **Then** el sistema informa que no hay embarcaciones disponibles para los criterios seleccionados.

---

### Edge Cases

- **Ausencia de Cotización Temporal**: Si Módulo 3 no puede proveer cotizaciones en lote debido a una caída del servicio, el sistema mostrará las embarcaciones disponibles pero indicará que el precio estimado no está disponible en este momento.
- **Punto de Extensión Hacia Iniciar Reserva**: Este caso de uso es opcionalmente invocado si el Arrendatario entra al proceso de `CU-02 Iniciar reserva` sin haber preseleccionado una embarcación.

---

## Requirements

### Functional Requirements

- **FR-001**: El sistema DEBE permitir al Arrendatario ingresar criterios de búsqueda como fechas, horas y cantidad de pasajeros.
- **FR-002**: El sistema DEBE recuperar la lista de embarcaciones que coincidan con los criterios de búsqueda (a través del catálogo/Módulo 1 de manera implícita u otra fuente de verdad de listados).
- **FR-003**: El sistema DEBE invocar obligatoriamente al caso de uso subordinado `CU-11 Proveer información cotización de reserva` `(<<include>>)` en su modo lote, transmitiendo los identificadores de las embarcaciones recuperadas.
- **FR-004**: El sistema DEBE mostrar los resultados al Arrendatario incluyendo la información básica de la embarcación y la tarifa estimada devuelta.

---

### Key Entities

- **Filtro de Búsqueda**: Objeto temporal en memoria que contiene los parámetros ingresados por el usuario.
- **Resultado de Búsqueda**: Lista de embarcaciones devueltas, cada una asociada a su cotización temporal de referencia.

---

## Success Criteria

### Measurable Outcomes

- **SC-001**: El 100% de los resultados presentados incluyen la tarifa estimada provista por Módulo 3 (salvo en caídas de servicio).
- **SC-002**: El tiempo de respuesta de la búsqueda, incluyendo la invocación en lote de cotizaciones, debe mantenerse por debajo de los límites de UX aceptables.
