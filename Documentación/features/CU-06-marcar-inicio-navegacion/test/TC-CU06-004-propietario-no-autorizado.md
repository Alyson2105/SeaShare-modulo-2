### Test Case Specification: TC-CU06-004 — Autorización del propietario
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU06-004

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar autorización del propietario. Requisitos: FR-002. Criterios: SC-006.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
La integración verifica dos rechazos con identidad por cabeceras; no prueba JWT ni consulta efectos persistidos después del rechazo.

##### Dependencies & Assumptions
Evidencia de la corrida con dependencias controladas. Pruebas parciales de componentes: La prueba citada cubre solo parte del escenario; no demuestra el flujo completo de CU-06.

##### Preconditions & Default Setup
Reserva RESERVED y solicitante distinto del propietario registrado.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pasó | NavigationStartIntegrationTest#renterAndForeignOwnerAreForbiddenAndUnknownIs404 (XML V4: passed). Aserta 403 PROFILE_NOT_AUTHORIZED para arrendatario y otro propietario; no consulta estado posterior. |
| 2 | Ejecutar | Solicitar inicio. | Datos de las precondiciones | Según el plan, 403 PROFILE_NOT_AUTHORIZED; sin transición. | Pendiente | NavigationStartIntegrationTest#renterAndForeignOwnerAreForbiddenAndUnknownIs404 (XML V4: passed). Aserta 403 PROFILE_NOT_AUTHORIZED para arrendatario y otro propietario; no consulta estado posterior. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | NavigationStartIntegrationTest#renterAndForeignOwnerAreForbiddenAndUnknownIs404 (XML V4: passed). Aserta 403 PROFILE_NOT_AUTHORIZED para arrendatario y otro propietario; no consulta estado posterior. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
Autenticación JWT descrita en el plan aún no implementada.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: NavigationStartIntegrationTest#renterAndForeignOwnerAreForbiddenAndUnknownIs404. La integración verifica dos rechazos con identidad por cabeceras; no prueba JWT ni consulta efectos persistidos después del rechazo.

#### Traceability
FR-002 | SC-006 | Evidencia: src/test/java/com/seashare/m2/reservas/integration/NavigationStartIntegrationTest.java#renterAndForeignOwnerAreForbiddenAndUnknownIs404 | In Process
