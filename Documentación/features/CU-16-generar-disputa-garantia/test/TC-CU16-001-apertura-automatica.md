### Test Case Specification: TC-CU16-001 — Apertura automática
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU16-001

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar apertura automática. Requisitos: FR-001, FR-003, FR-004, FR-005. Criterios: SC-001.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
H10 conecta CU-07 con la apertura de disputa. La apertura directa comprueba PENDING y 24 h, pero no prueba la disputa persistida ni los campos HTTP en el cierre integrado. OpenDisputeService registra el historial directamente, sin invocar CU-17 como exige FR-004.

##### Dependencies & Assumptions
La evidencia V4 y sus límites se indican en Comments.

##### Preconditions & Default Setup
Reserva IN_NAVIGATION y cierre CU-07 válido; reloj controlado.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | OpenDisputeIntegrationTest#opensPendingDisputeWith24HourWindow (XML V4: passed). Comprueba PENDING, reserva, inicio y fin de ventana y propietario persistido en apertura directa. EndNavigationServiceTest#completedCheckoutOpensDisputeWithStatusChangedAtAndOwner verifica el comando y el ID y vencimiento devueltos por un mock; NavigationEndDisputeRollbackTest#disputeFailureLeavesReservationInNavigation verifica 500, estado IN_NAVIGATION y cero checkout ante fallo. No comprueban ID, ventana o evento de disputa en el cierre HTTP exitoso. |
| 2 | Ejecutar | Finalizar navegación e inspeccionar disputa asociada. | Datos de las precondiciones | Se crea exactamente una disputa PENDING por el sistema mediante CU-17 con vencimiento 24 horas después. | Pendiente | OpenDisputeIntegrationTest#opensPendingDisputeWith24HourWindow (XML V4: passed). Comprueba PENDING, reserva, inicio y fin de ventana y propietario persistido en apertura directa. EndNavigationServiceTest#completedCheckoutOpensDisputeWithStatusChangedAtAndOwner verifica el comando y el ID y vencimiento devueltos por un mock; NavigationEndDisputeRollbackTest#disputeFailureLeavesReservationInNavigation verifica 500, estado IN_NAVIGATION y cero checkout ante fallo. No comprueban ID, ventana o evento de disputa en el cierre HTTP exitoso. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | OpenDisputeIntegrationTest#opensPendingDisputeWith24HourWindow (XML V4: passed). Comprueba PENDING, reserva, inicio y fin de ventana y propietario persistido en apertura directa. EndNavigationServiceTest#completedCheckoutOpensDisputeWithStatusChangedAtAndOwner verifica el comando y el ID y vencimiento devueltos por un mock; NavigationEndDisputeRollbackTest#disputeFailureLeavesReservationInNavigation verifica 500, estado IN_NAVIGATION y cero checkout ante fallo. No comprueban ID, ventana o evento de disputa en el cierre HTTP exitoso. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
El estado de disputa es distinto del estado de reserva; la retención financiera corresponde a M3.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: OpenDisputeIntegrationTest#opensPendingDisputeWith24HourWindow. H10 conecta CU-07 con la apertura de disputa. La apertura directa comprueba PENDING y 24 h, pero no prueba la disputa persistida ni los campos HTTP en el cierre integrado. OpenDisputeService registra el historial directamente, sin invocar CU-17 como exige FR-004.

#### Traceability
FR-001, FR-003, FR-004, FR-005 | SC-001 | src/test/java/com/seashare/m2/disputa/integration/OpenDisputeIntegrationTest.java#opensPendingDisputeWith24HourWindow; src/test/java/com/seashare/m2/reservas/application/service/EndNavigationServiceTest.java#completedCheckoutOpensDisputeWithStatusChangedAtAndOwner; src/test/java/com/seashare/m2/reservas/integration/NavigationEndDisputeRollbackTest.java#disputeFailureLeavesReservationInNavigation | In Process
