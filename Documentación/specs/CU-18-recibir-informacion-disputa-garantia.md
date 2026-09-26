# Feature Specification: Recibir Información de Disputa de Garantía

**Módulo**: Módulo 2 (Operación de Reservas, Tiempos y Cancelaciones)  
**Fecha de Creación**: 2026-09-26  
**Actores Primarios**: Módulo 3 (Gestión Liquidación), como receptor de la información publicada por Módulo 2.  
**Dependencias Externas (APIs)**:
- **Módulo 3 – Gestión Liquidación**: sistema cliente que consume los mensajes de la cola para decidir internamente el destino del depósito. No participa en la creación ni en la actualización del estado de la disputa.
- **Casos de uso internos de Módulo 2**: invocado tras cada transición a estado final (RECHAZADA o ACEPTADA) gestionada por `Generar disputa de garantía` (`CU-16`, cierre automático) y `Actualizar estado de disputa de garantía` (`CU-17`, resolución del Admin).

> **Nota de alcance**: este caso de uso reemplaza lo que originalmente se planteó como "Solicitar información de disputa de garantía". No es un caso adicional: es el mismo caso renombrado y con la dirección de la relación invertida — en vez de que Módulo 3 consulte activamente, es Módulo 2 quien publica o envía la información y Módulo 3 la recibe.
>
> **Nota de dominio**: los estados **PENDIENTE**, **RECHAZADA** y **ACEPTADA** (en mayúscula) pertenecen al objeto **Disputa de garantía**, que es distinto al estado de la reserva (`Completada`, `Reservada`, `Pendiente de Pago`, etc.).

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Publicar los estados finales de la disputa para Módulo 3 (Priority: P1)

Cuando la disputa alcanza un estado final (RECHAZADA por vencimiento automático o por resolución del Admin, o ACEPTADA por resolución del Admin), Módulo 2 publica un mensaje en la cola con únicamente ese estado y los identificadores necesarios para relacionarlo con la reserva y con la disputa correspondiente. El estado PENDIENTE es interno de Módulo 2 y nunca genera mensaje hacia Módulo 3. Módulo 3 consume el mensaje y decide internamente, con sus propios registros, el destino del depósito.

**Why this priority**: Es el único puente por el cual Módulo 3 se entera del resultado de la disputa. Sin esta publicación, el depósito quedaría congelado indefinidamente del lado financiero.

**Independent Test**: Se prueba provocando transiciones a estado final en disputas (cierre por vencimiento, resolución ACEPTADA y RECHAZADA) contra un consumidor simulado de la cola, y verificando que por cada estado final se publica exactamente un mensaje con el estado correcto y los identificadores, sin montos ni instrucciones de pago, y que la creación en PENDIENTE no genera ningún mensaje.

**Acceptance Scenarios**:

1. **Scenario**: Publicación al cerrar en RECHAZADA
   - **Given** una disputa que transiciona a RECHAZADA (por vencimiento automático o por resolución del Admin)
   - **When** se consolida el cambio de estado
   - **Then** Módulo 2 publica en la cola un mensaje con el estado RECHAZADA y los identificadores de la disputa y de la reserva, para que Módulo 3 solicite internamente el reembolso total del depósito al Arrendatario

2. **Scenario**: Publicación al resolver en ACEPTADA
   - **Given** una disputa que transiciona a ACEPTADA por resolución del Admin
   - **When** se consolida el cambio de estado
   - **Then** Módulo 2 publica en la cola un mensaje con el estado ACEPTADA y los identificadores, para que Módulo 3 solicite internamente la liquidación total del depósito al Propietario

### User Story 2 - Entrega garantizada e idempotente por cola de mensajes (Priority: P1)

El envío se realiza por cola de mensajes (Módulo 2 publica, Módulo 3 consume), no como consulta síncrona de solicitud/respuesta. Cada mensaje incluye un identificador único de evento para que, si se duplica o reenvía por la cola, Módulo 3 descarte los ya procesados sin reconsultar el estado completo. Si la publicación falla, Módulo 2 la reintenta hasta confirmarla en el broker.

**Why this priority**: Sin garantía de entrega e idempotencia, un mensaje perdido dejaría el depósito sin resolver y un duplicado podría provocar doble contabilización del lado financiero.

**Independent Test**: Se prueba simulando una caída del broker al publicar y mensajes duplicados hacia el consumidor simulado, verificando que Módulo 2 reintenta hasta publicar y que los duplicados (mismo identificador de evento) son descartables sin efectos adicionales.

**Acceptance Scenarios**:

1. **Scenario**: Reintento hasta publicar en el broker
   - **Given** un cambio de estado de disputa consolidado en Módulo 2 con el broker no disponible
   - **When** falla el primer intento de publicación
   - **Then** Módulo 2 reintenta la publicación hasta confirmarla en el broker, sin perder el evento

2. **Scenario**: Mensaje duplicado descartable por Módulo 3
   - **Given** un mensaje ya consumido por Módulo 3
   - **When** el mismo mensaje (mismo identificador único de evento) llega de nuevo por reenvío de la cola
   - **Then** Módulo 3 puede descartarlo como duplicado sin necesidad de volver a consultar el estado completo (la deduplicación la ejecuta Módulo 3 con el identificador provisto)

---

### Edge Cases

- **Contenido prohibido en el mensaje**: el mensaje **NO DEBE contener** monto del depósito, monto a reembolsar, monto a liquidar, porcentaje, instrucción de pago, referencia de pasarela ni ninguna decisión técnica de transferencia. Todo lo resuelve Módulo 3 con sus propios registros internos.
- **Orden de consumo**: si Módulo 3 consume mensajes desordenados, el identificador único de evento (que incluye número de versión incremental) le permite ordenar y aplicar solo el más reciente. [NEEDS CLARIFICATION: garantías de orden del broker y estrategia de versionado — se define en el contrato de integración, no en este spec]
- **Detalles de infraestructura de la cola** (broker, tópicos, retención, DLQ, particionado): [NEEDS CLARIFICATION: se definen en el contrato de integración con Módulo 3, no en este spec].
- **Consecuencias en Módulo 3** (a título informativo; la lógica vive en Módulo 3): RECHAZADA → solicita el reembolso total del depósito al Arrendatario; ACEPTADA → solicita la liquidación total del depósito al Propietario. (PENDIENTE es interno de Módulo 2 y nunca se envía a Módulo 3, por lo que no forma parte de este contrato.)

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE publicar un mensaje en la cola únicamente cuando la disputa alcance un estado final (RECHAZADA o ACEPTADA), con Módulo 2 como publicador y Módulo 3 como consumidor. El estado PENDIENTE es interno de Módulo 2 y NO genera ningún mensaje. NO es una consulta síncrona de solicitud/respuesta.
- **FR-002**: El mensaje DEBE contener únicamente: el estado de la disputa (uno de RECHAZADA, ACEPTADA) y los identificadores necesarios para relacionarlo con la reserva (identificador de reserva) y con la disputa correspondiente (identificador de disputa).
- **FR-003**: El mensaje NO DEBE contener: monto del depósito, monto a reembolsar, monto a liquidar, porcentaje, instrucción de pago, referencia de pasarela ni ninguna decisión técnica de transferencia.
- **FR-004**: Cada mensaje DEBE incluir un identificador único de evento (identificador de disputa + estado + eventId o número de versión incremental), de forma que Módulo 3 pueda descartar duplicados o reenvíos sin reconsultar el estado completo.
- **FR-005**: Si la publicación en el broker falla, el sistema DEBE reintentarla hasta confirmarla, sin perder el evento.
- **FR-006**: **REGLA DE NEGOCIO ESTRICTA (Sin dinero):** Módulo 2 **NO DEBE calcular, sugerir ni incluir** en el mensaje ningún valor monetario ni instrucción financiera. El destino del depósito lo decide Módulo 3 internamente a partir del estado recibido.

### Key Entities

- **Evento de disputa de garantía**: mensaje publicado en la cola por cada transición a estado final. Atributos clave: identificador único del evento (disputa + estado + eventId/versión), identificador de la disputa, identificador de la reserva, estado (RECHAZADA / ACEPTADA), marca temporal del cambio.
- **Disputa de garantía** *(objeto de dominio referenciado; vive en `CU-16`/`CU-17`)*: se referencia por sus identificadores para relacionar cada mensaje con la disputa y la reserva correspondientes.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de las transiciones a RECHAZADA o ACEPTADA generan exactamente un (1) mensaje publicado con el estado correcto y los identificadores correspondientes, y cero (0%) creaciones en PENDIENTE generan mensajes.
- **SC-002**: Cero (0) mensajes con montos, instrucciones de pago o referencias de pasarela emitidos por Módulo 2.
- **SC-003**: El 100% de los mensajes publicados incluyen identificador único de evento apto para deduplicación por Módulo 3.
- **SC-004**: Cero (0%) eventos de cambio de estado perdidos por fallos de publicación (reintento hasta confirmación en el broker).

---

## Dudas abiertas de este spec

- **D-01**: Garantías de orden del broker y estrategia de versionado del eventId — se define en el contrato de integración.
- **D-02**: Detalles de infraestructura de la cola (broker, tópicos, retención, DLQ) — se definen en el contrato de integración.
