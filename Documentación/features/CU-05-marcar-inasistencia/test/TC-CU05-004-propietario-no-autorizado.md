### Test Case Specification: TC-CU05-004 — Rechazo de solicitante ajeno
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU05-004

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P2

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar rechazo de solicitante ajeno. Requisitos: FR-002. Criterios: SC-006.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
La integración comprueba rechazo de arrendatario y propietario ajeno; usa cabeceras de identidad y no comprueba JWT ni efectos posteriores persistidos.

##### Dependencies & Assumptions
Evidencia de la corrida con dependencias controladas. Pruebas parciales de componentes: La prueba citada cubre solo parte del escenario; no demuestra el flujo completo de CU-05.

##### Preconditions & Default Setup
Reserva RESERVED cumplida la tolerancia; solicitante distinto del propietario registrado.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pasó | NoShowIntegrationTest#renterAndForeignOwnerAreForbiddenAndUnknownIs404 (XML V4: passed). Aserta 403 y PROFILE_NOT_AUTHORIZED para ambos solicitantes; no consulta la reserva ni los eventos tras el rechazo. |
| 2 | Ejecutar | Solicitar el reporte. | Datos de las precondiciones | Según el plan, 403 PROFILE_NOT_AUTHORIZED; reserva sin cambios. | Pendiente | NoShowIntegrationTest#renterAndForeignOwnerAreForbiddenAndUnknownIs404 (XML V4: passed). Aserta 403 y PROFILE_NOT_AUTHORIZED para ambos solicitantes; no consulta la reserva ni los eventos tras el rechazo. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | NoShowIntegrationTest#renterAndForeignOwnerAreForbiddenAndUnknownIs404 (XML V4: passed). Aserta 403 y PROFILE_NOT_AUTHORIZED para ambos solicitantes; no consulta la reserva ni los eventos tras el rechazo. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
JWT es contrato propuesto; no hay autenticación completa implementada.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: NoShowIntegrationTest#renterAndForeignOwnerAreForbiddenAndUnknownIs404. La integración comprueba rechazo de arrendatario y propietario ajeno; usa cabeceras de identidad y no comprueba JWT ni efectos posteriores persistidos.

#### Traceability
FR-002 | SC-006 | Evidencia: src/test/java/com/seashare/m2/reservas/integration/NoShowIntegrationTest.java#renterAndForeignOwnerAreForbiddenAndUnknownIs404 | In Process
