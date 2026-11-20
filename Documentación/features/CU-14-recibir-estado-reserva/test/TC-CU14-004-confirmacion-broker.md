### Test Case Specification: TC-CU14-004 — Confirmación del broker
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU14-004

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar confirmación del broker. Requisitos: FR-005. Criterios: SC-001.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
EventDeduplicationTest verifica Message-Id en otro caso; no demuestra consumo por M3 real ni todos los cambios publicables de SC-001.

##### Dependencies & Assumptions
Ejecución con dependencias controladas cuando corresponda. Pruebas relacionadas: [relay](../../../../src/test/java/com/seashare/m2/reservas/application/service/PublishPendingOutboxServiceTest.java); [integracion](../../../../src/test/java/com/seashare/m2/reservas/integration/OutboxRelayIntegrationTest.java); su cobertura debe evaluarse por escenario.

##### Preconditions & Default Setup
Mensaje AMQP pendiente y publicador simulado confirma recepción.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | OutboxRelayIntegrationTest#publicaEnRabbitYMarcaPublishedAt (XML V4: passed). Aserta una publicación procesada, published_at persistido y al menos un mensaje en cola RabbitMQ. EventDeduplicationTest verifica Message-Id en otro caso; no demuestra consumo por M3 real ni todos los cambios publicables de SC-001. |
| 2 | Ejecutar | Ejecutar relay e inspeccionar estado del outbox. | Datos de las precondiciones | Se marca publicado tras confirmación; se conserva event_id como Message-Id. | Pendiente | OutboxRelayIntegrationTest#publicaEnRabbitYMarcaPublishedAt (XML V4: passed). Aserta una publicación procesada, published_at persistido y al menos un mensaje en cola RabbitMQ. EventDeduplicationTest verifica Message-Id en otro caso; no demuestra consumo por M3 real ni todos los cambios publicables de SC-001. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | OutboxRelayIntegrationTest#publicaEnRabbitYMarcaPublishedAt (XML V4: passed). Aserta una publicación procesada, published_at persistido y al menos un mensaje en cola RabbitMQ. EventDeduplicationTest verifica Message-Id en otro caso; no demuestra consumo por M3 real ni todos los cambios publicables de SC-001. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
RabbitMQ y PostgreSQL se ejecutaron en Testcontainers; el consumidor real de M3 no se probó.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: OutboxRelayIntegrationTest#publicaEnRabbitYMarcaPublishedAt. EventDeduplicationTest verifica Message-Id en otro caso; no demuestra consumo por M3 real ni todos los cambios publicables de SC-001.

#### Traceability
FR-005 | SC-001 | [relay](../../../../src/test/java/com/seashare/m2/reservas/application/service/PublishPendingOutboxServiceTest.java); [integracion](../../../../src/test/java/com/seashare/m2/reservas/integration/OutboxRelayIntegrationTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/integration/OutboxRelayIntegrationTest.java#publicaEnRabbitYMarcaPublishedAt; src/test/java/com/seashare/m2/reservas/integration/EventDeduplicationTest.java#publicarElMismoEventoDosVecesSeProcesaUnaSolaVez | In Process
