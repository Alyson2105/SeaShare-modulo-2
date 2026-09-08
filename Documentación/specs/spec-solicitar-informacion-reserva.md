# Feature Specification: Solicitar Información de la Reserva

**Módulo**: Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones  
**Fecha de Creación**: 2026-09-08  
**Actores Primarios / Disparador**: Módulo 3 (Consumidor del API)  
**Dependencias Externas (APIs)**:
- **Módulo 3 – Liquidación, Seguros y Dispersión de Fondos**: Actúa como el sistema cliente que consume este *endpoint* de lectura para obtener los datos operativos de la reserva.
- **Casos de uso internos de Módulo 2**: Ninguno. Este caso de uso es una consulta de dominio de solo lectura. **NO invoca a "Actualizar estado reserva"**, no utiliza relaciones `(<<include>>)` ni `(<<extend>>)` y no muta la máquina de estados.

---

## User Scenarios & Testing

### User Story 1 - Suministrar información operativa en tiempo real a Módulo 3 (Priority: P1)

Como motor financiero (Módulo 3), necesito consultar los datos operativos actualizados de una reserva específica (estado, fechas, pasajeros y barco) para poder vincular mis operaciones de cobro, activación de seguros y custodia de fondos con la realidad operativa del alquiler[cite: 2].

***Why this priority***: Es el puente de lectura fundamental entre la operación y las finanzas. Sin este canal, Módulo 3 operaría a ciegas y no podría respaldar transacciones, activaciones de pólizas ni dispersiones.

***Independent Test***: Se prueba ejecutando consultas `GET` hacia Módulo 2 enviando identificadores válidos de reservas en estados activos (`Pendiente de Pago`, `Confirmada`, `En Navegación`). Se verifica que la respuesta estregue el *payload* completo en menos de 200 ms sin alterar el estado de la base de datos ni gatillar eventos secundarios.

***Acceptance Scenarios***:

1. **Scenario**: Consulta exitosa de una reserva activa
    - **Given** una reserva existente en estado `Confirmada` o `En Navegación`
    - **When** Módulo 3 solicita la información mediante su identificador
    - **Then** el sistema responde con el código 200 OK y entrega los datos operativos completos (fechas, pasajeros, embarcación, estado actual y marcas temporales) sin realizar cálculos financieros

2. **Scenario**: Consulta de reserva en ventana de pago (TTL)
    - **Given** una reserva en estado `Pendiente de Pago`
    - **When** Módulo 3 solicita la información de la reserva
    - **Then** el sistema entrega los datos, indicando el estado `Pendiente de Pago` y el tiempo exacto restante del temporizador de 15 minutos

---

### User Story 2 - Proveer datos de cierre e incidentes para liquidación final (Priority: P1)

Como motor financiero (Módulo 3), necesito obtener los detalles de cierre de una reserva (sub-estados de incidentes, justificaciones o anticipación de cancelación) para aplicar de manera autónoma mi matriz de liquidación, reembolsos y ejecución de garantías[cite: 2].

***Why this priority***: Permite a Módulo 3 saber matemáticamente cuánto dinero liberar, retener o penalizar al finalizar un contrato, basándose estrictamente en los hechos operativos reportados en muelle.

***Independent Test***: Se prueba consultando reservas en estados terminales (`Completado` y `Cancelado` con sus respectivos sub-estados). Se valida que Módulo 2 exponga el texto íntegro de los incidentes y las horas exactas de anticipación, sin deducir montos de dinero.

***Acceptance Scenarios***:

1. **Scenario**: Consulta de reserva completada para evaluar garantía
    - **Given** una reserva en estado `Completado` (ya sea `Sin incidentes` o `Con incidentes`)
    - **When** Módulo 3 solicita la información
    - **Then** el sistema devuelve los datos del check-out, incluyendo el texto descriptivo de novedades (si las hay), permitiendo a Módulo 3 decidir sobre el depósito de garantía

2. **Scenario**: Consulta de reserva cancelada para aplicar penalidades
    - **Given** una reserva en estado `Cancelado`
    - **When** Módulo 3 solicita la información
    - **Then** el sistema responde con el sub-estado (ej. `Moderado`), el actor responsable y las horas de anticipación, delegando el cálculo del reembolso a Módulo 3

---

### Edge Cases

- **Naturaleza Estrictamente Idempotente (Solo Lectura)**: Esta interfaz no produce efectos secundarios. No avanza el estado, no interfiere con el TTL, no llama a Módulo 1 ni dispara webhooks. Módulo 3 puede consultarla 1,000 veces seguidas obteniendo exactamente el mismo resultado sin corromper el sistema.
- **Sin Política de Reintentos Internos**: Dado que Módulo 2 actúa como servidor pasivo en este caso de uso, si hay un timeout de red, Módulo 2 simplemente cierra el hilo. La responsabilidad de reintentar la llamada recae 100% en el cliente (Módulo 3).
- **Prohibición de Cálculos Financieros**: Módulo 2 **NO** tasa económicamente los daños, no estima penalidades y no deduce comisiones[cite: 2]. Solo entrega hechos (horas, textos, estados).
- **Inmutabilidad en Estados Terminales**: Las consultas sobre reservas en estado `Completado`, `Cancelado` o `Expirado` siempre devolverán la misma fotografía histórica del cierre.

---

## Requirements

### Functional Requirements

- **FR-001**: El sistema DEBE exponer un *endpoint* de lectura síncrona dedicado a proveer información de la reserva a Módulo 3.
- **FR-002**: Si la reserva existe, el sistema DEBE retornar un payload estructurado que incluya obligatoriamente: identificador de la reserva, identificadores de arrendatario y embarcación, fechas/horas pactadas de zarpe y desembarque, cantidad de pasajeros, estado principal actual y referencia de la cotización original.
- **FR-003**: Si el estado es `Completado`, el sistema DEBE incluir el sub-estado (`Sin incidentes` / `Con incidentes`), la fecha/hora real de check-out y el texto literal de las novedades u observaciones registradas por el Propietario.
- **FR-004**: Si el estado es `Cancelado`, el sistema DEBE incluir el sub-estado correspondiente (`Flexible`, `Moderado`, `Tardío`, `Por Anfitrión`, `Por Inasistencia`), el actor que disparó la cancelación y las horas exactas de anticipación calculadas.
- **FR-005**: Si el estado es `Pendiente de Pago`, el sistema DEBE incluir la marca de tiempo exacta en la que expirará el temporizador TTL de 15 minutos.
- **FR-006**: **REGLA DE NEGOCIO ESTRICTA**: El sistema **NO DEBE** calcular ni incluir en la respuesta ningún valor monetario derivado de penalidades, reembolsos o tasación de daños. Toda valoración económica pertenece a Módulo 3.
- **FR-007**: El sistema DEBE garantizar que la ejecución de este caso de uso no genere escrituras en la base de datos ni notificaciones hacia componentes de Módulo 1.

---

### Key Entities

- **Reserva (Read-Only Representation)**: Estructura de datos consolidada (DTO) que expone el estado y los metadatos operativos del alquiler sin exponer la lógica interna de transición de estados de Módulo 2.

---

## Success Criteria

### Measurable Outcomes

- **SC-001**: El 100% de las consultas exitosas se resuelven y entregan el payload completo en un tiempo inferior a 200 milisegundos.
- **SC-002**: Cero (0%) alteraciones o mutaciones de estado en la base de datos derivadas de la ejecución de esta consulta.
- **SC-003**: Cero (0) valores financieros o monetarios calculados internamente por Módulo 2 dentro del payload de respuesta.