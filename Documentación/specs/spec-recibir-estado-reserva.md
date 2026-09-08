# Feature Specification: Recibir Estado de Reserva (Notificación a Módulo 3)

**Módulo**: Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones  
**Fecha de Creación**: 2026-09-08 (Actualizado para excluir notificaciones del estado Borrador)  
**Actores Primarios / Disparador**: Sistema / Invocado internamente por el caso de uso `Actualizar estado reserva`.  
**Dependencias Externas (APIs)**:
- **Módulo 3 – Liquidación, Seguros y Dispersión de Fondos**: API externa receptora (`Recibir estado de reserva`), encargada de procesar las transiciones operativas para detonar la lógica financiera (activación de seguros, retenciones, cobros, dispersiones y reembolsos).

---

## User Scenarios & Testing

### User Story 1 - Notificar el inicio del ciclo de pago y posteriores transiciones (Priority: P1)

Como sistema (Módulo 2), quiero notificar a Módulo 3 cada vez que una reserva entra a la fase de pago (`Pendiente de Pago`) y en cada transición de estado posterior, para que Finanzas tenga visibilidad en tiempo real del ciclo de vida del alquiler y pueda ejecutar sus procesos de recaudo y cobertura.

***Why this priority***: Módulo 3 es ciego a la operación si Módulo 2 no le avisa. Esta notificación es el gatillo que permite a Módulo 3 saber cuándo debe esperar un pago, cuándo activar un seguro o cuándo devolver dinero.

***Independent Test***: Se prueba interceptando la salida HTTP desde `Actualizar estado reserva` hacia un *mock* de la API de Módulo 3. Se provoca el cambio de una reserva de `Borrador` a `Pendiente de Pago` (vía `Iniciar pago`) y se verifica que Módulo 2 emita un *payload* con el ID de la reserva, el nuevo estado y la marca de tiempo, sin enviar montos calculados. Se verifica también que NO se envíe nada al crear el `Borrador`.

***Acceptance Scenarios***:

1. **Scenario**: Notificación inicial al arrancar el proceso de pago
    - **Given** una reserva que acaba de pasar de `Borrador` a `Pendiente de Pago` debido a que el Arrendatario ejecutó `Iniciar pago`
    - **When** se consolida el nuevo estado en la base de datos
    - **Then** el sistema emite una notificación síncrona a Módulo 3 informando que la reserva identificada entró a `Pendiente de Pago`, activando el interés financiero sobre el contrato

2. **Scenario**: Omisión intencional de notificación para el estado Borrador
    - **Given** un Arrendatario que acaba de crear una reserva preliminar mediante `Iniciar reserva`
    - **When** la reserva se guarda internamente en estado `Borrador`
    - **Then** el sistema NO notifica a Módulo 3, manteniendo el registro como un borrador interno exclusivo de Módulo 2

3. **Scenario**: Notificaciones de ciclo de vida activo
    - **Given** una reserva que transiciona a `Confirmada`, `En Navegación`, o cualquier estado terminal (`Completado`, `Cancelado`, `Expirado`)
    - **When** se asienta el cambio en la máquina de estados
    - **Then** el sistema notifica el evento exacto a Módulo 3, incluyendo sub-estados si aplican (ej. `Cancelado` con sub-estado `Moderado`)

---

### User Story 2 - Asegurar la entrega de notificaciones ante caídas de red (Priority: P1)

Como sistema, quiero encolar y reintentar las notificaciones dirigidas a Módulo 3 si este no responde, para garantizar que ningún evento operativo (como un check-in o una cancelación) se pierda silenciosamente, manteniendo la consistencia eventual entre la operación y las finanzas.

***Why this priority***: Una notificación perdida significa que un seguro no se activó o que un anfitrión nunca recibió su dinero. La entrega garantizada (Event Delivery Guarantee) es innegociable en arquitecturas desacopladas.

***Independent Test***: Se simula una caída (HTTP 503 o timeout) en la API de Módulo 3. Se dispara un cambio de estado en Módulo 2. Se verifica que Módulo 2 guarde la transición exitosamente y encole el mensaje de notificación, reintentándolo periódicamente hasta recibir un HTTP 200 OK.

***Acceptance Scenarios***:

1. **Scenario**: Reintento automático por indisponibilidad de Módulo 3
    - **Given** una reserva que cambia a `En Navegación` pero la API de Módulo 3 está caída
    - **When** el sistema intenta enviar la notificación y recibe un error de conexión
    - **Then** el sistema marca el evento como "Pendiente de envío" y lo reintenta con una estrategia de respaldo progresivo (backoff) hasta que Módulo 3 confirme la recepción

---

### Edge Cases

- **Idempotencia en la Recepción**: Si Módulo 2 envía dos veces la misma notificación por un falso timeout de red, Módulo 3 debe ser capaz de procesarla de forma idempotente. Módulo 2 envía identificadores únicos por cada transición para facilitar esto.
- **Reporte de Incidentes**: Si la notificación es de un estado `Completado` con sub-estado `Con incidentes`, el *payload* debe incluir obligatoriamente el texto descriptivo de las averías para que Módulo 3 sustente la retención del depósito de garantía.
- **Prohibición de Cálculo Monetario**: Las notificaciones de estado son puramente operativas. **Módulo 2 JAMÁS incluye en el *payload* cálculos de penalidades, montos de reembolso o valoraciones de daños**[cite: 2]. Solo notifica el estado (ej. `Cancelado`), el sub-estado (ej. `Tardío`) y el actor responsable.

---

## Requirements

### Functional Requirements

- **FR-001**: El sistema DEBE enviar una petición a la API externa `Recibir estado de reserva` de Módulo 3 cada vez que el caso de uso `Actualizar estado reserva` consolide una transición de estado válida.
- **FR-002**: El sistema **NO DEBE** emitir ninguna notificación a Módulo 3 cuando una reserva sea creada o modificada en estado `Borrador`. La integración con Módulo 3 inicia estrictamente a partir del estado `Pendiente de Pago`.
- **FR-003**: El *payload* de la notificación DEBE contener obligatoriamente: identificador de la reserva, nuevo estado principal, sub-estado (si aplica), marca temporal exacta del evento (en formato ISO 8601) y actor que disparó el evento.
- **FR-004**: Si el estado es `Cancelado`, la notificación DEBE incluir el sub-estado (`Flexible`, `Moderado`, `Tardío`, `Por Anfitrión`, `Por Inasistencia`) y la anticipación temporal cronológica.
- **FR-005**: Si el estado es `Completado` con sub-estado `Con incidentes`, la notificación DEBE incluir el reporte textual de novedades ingresado por el Propietario.
- **FR-006**: El sistema DEBE implementar un mecanismo de entrega garantizada (cola de reintentos) para asegurar que las notificaciones alcancen Módulo 3 ante fallos temporales de red o timeouts.
- **FR-007**: **REGLA ESTRICTA**: El sistema **NO DEBE** calcular ni incluir datos financieros procesados en la notificación[cite: 2]. Finanzas es responsable de interpretar el estado operativo y traducir ese evento a dinero.

---

### Key Entities

- **Notificación de Transición (`StateTransitionEvent`)**: DTO (Data Transfer Object) de integración saliente que empaqueta los detalles del cambio de estado operativo para consumo de Módulo 3.

---

## Success Criteria

### Measurable Outcomes

- **SC-001**: Cero (0%) notificaciones enviadas a Módulo 3 relacionadas con el estado interno `Borrador`.
- **SC-002**: El 100% de los cambios desde `Pendiente de Pago` en adelante son notificados a Módulo 3 (0% de eventos perdidos gracias a la cola de reintentos).
- **SC-003**: El tiempo de emisión del primer intento de notificación no supera los 500 milisegundos tras la consolidación del estado en la base de datos local.
- **SC-004**: Cero (0) valores financieros o monetarios calculados incluidos en el cuerpo del mensaje enviado a Módulo 3[cite: 2].