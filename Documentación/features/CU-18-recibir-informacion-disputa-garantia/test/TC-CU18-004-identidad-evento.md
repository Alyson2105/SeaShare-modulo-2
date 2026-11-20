### Test Case Specification: TC-CU18-004 — Identificador y reenvío
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU18-004

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar identificador y reenvío. Requisitos: FR-004, FR-005. Criterios: SC-003, SC-004.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
CU-18 spec FR-005 exige reintentar hasta confirmar; su plan sección 6.10 limita a cinco intentos y alarma. El paso de un fallo seguido de éxito es evaluable en ambos contratos; el agotamiento conserva esa divergencia y el reenvío específico de disputa sigue sin prueba.

##### Dependencies & Assumptions
La evidencia identifica las dependencias usadas; un broker de prueba no demuestra consumo por M3.

##### Preconditions & Default Setup
Evento final con event_key y Message-Id según plan; broker simulado.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | DisputeEventMapperTest#eventIdIsDeterministicPerDisputeAndStatus (XML V4: passed). Aserta que dos conversiones del mismo estado tienen el UUID esperado; OutboxRelayIntegrationTest#falloSimuladoReintentaHastaPublicar verifica un reintento genérico, no la identidad de un reenvío de disputa. |
| 2 | Ejecutar | Fallar publicación y reintentar observando identificadores. | Datos de las precondiciones | Se conserva la identidad del evento y se marca publicado tras confirmación del broker. | Pendiente | DisputeEventMapperTest#eventIdIsDeterministicPerDisputeAndStatus (XML V4: passed). Aserta que dos conversiones del mismo estado tienen el UUID esperado; OutboxRelayIntegrationTest#falloSimuladoReintentaHastaPublicar verifica un reintento genérico, no la identidad de un reenvío de disputa. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | DisputeEventMapperTest#eventIdIsDeterministicPerDisputeAndStatus (XML V4: passed). Aserta que dos conversiones del mismo estado tienen el UUID esperado; OutboxRelayIntegrationTest#falloSimuladoReintentaHastaPublicar verifica un reintento genérico, no la identidad de un reenvío de disputa. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
La deduplicación efectiva corresponde a M3; no se demuestra con publisher confirm.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: DisputeEventMapperTest#eventIdIsDeterministicPerDisputeAndStatus. CU-18 spec FR-005 exige reintentar hasta confirmar; su plan sección 6.10 limita a cinco intentos y alarma. El paso de un fallo seguido de éxito es evaluable en ambos contratos; el agotamiento conserva esa divergencia y el reenvío específico de disputa sigue sin prueba.

#### Traceability
FR-004, FR-005 | SC-003, SC-004 | Evidencia: src/test/java/com/seashare/m2/disputa/application/service/DisputeEventMapperTest.java#eventIdIsDeterministicPerDisputeAndStatus; src/test/java/com/seashare/m2/disputa/integration/DisputePublishingIntegrationTest.java#acceptedIsPublishedAsCompletedWithoutAmounts; src/test/java/com/seashare/m2/reservas/integration/OutboxRelayIntegrationTest.java#falloSimuladoReintentaHastaPublicar | In Process
