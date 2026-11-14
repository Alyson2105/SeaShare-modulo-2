### Test Case Specification: TC-CU21-004 — Total estimado persistido
**Status**: Draft
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU21-004

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar total estimado persistido. Requisitos: FR-003, FR-004. Criterios: SC-002.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
spec.md FR-004 exige valores de M3 al pago; plan.md §2 admite estimación previa para INITIATED. El caso no fija estado y pide estimated_total como cadena; el plan define total_paid numérico o nulo.

##### Dependencies & Assumptions
Ejecución con dependencias controladas. Pruebas relacionadas: [consulta](../../../../src/test/java/com/seashare/m2/reservas/integration/ReservationQueryTest.java); su cobertura debe evaluarse por escenario.

##### Preconditions & Default Setup
Reserva propia con estimated_total conocido.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | ReservationQueryTest#detailIncludesContractFields (XML V4: passed). Aserta total_paid nulo para RESERVED sin liquidación, no un valor numérico. spec.md FR-004 exige valores de M3 al pago; plan.md §2 admite estimación previa para INITIATED. El caso no fija estado y pide estimated_total como cadena; el plan define total_paid numérico o nulo. |
| 2 | Ejecutar | Consultar detalle y comparar el total con el valor persistido. | Datos de las precondiciones | estimated_total se devuelve como cadena decimal del valor guardado sin cálculo de devolución. | Pendiente | ReservationQueryTest#detailIncludesContractFields (XML V4: passed). Aserta total_paid nulo para RESERVED sin liquidación, no un valor numérico. spec.md FR-004 exige valores de M3 al pago; plan.md §2 admite estimación previa para INITIATED. El caso no fija estado y pide estimated_total como cadena; el plan define total_paid numérico o nulo. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | ReservationQueryTest#detailIncludesContractFields (XML V4: passed). Aserta total_paid nulo para RESERVED sin liquidación, no un valor numérico. spec.md FR-004 exige valores de M3 al pago; plan.md §2 admite estimación previa para INITIATED. El caso no fija estado y pide estimated_total como cadena; el plan define total_paid numérico o nulo. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
El paso exige estimated_total como cadena; plan.md describe payment_summary.total_paid numérico y la política usa monto liquidado cuando existe.

Es estimación recibida del cliente, no demuestra desglose oficial de M3.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Draft; estado documental: Draft; evidencia: ReservationQueryTest#detailIncludesContractFields. spec.md FR-004 exige valores de M3 al pago; plan.md §2 admite estimación previa para INITIATED. El caso no fija estado y pide estimated_total como cadena; el plan define total_paid numérico o nulo.

#### Traceability
FR-003, FR-004 | SC-002 | [consulta](../../../../src/test/java/com/seashare/m2/reservas/integration/ReservationQueryTest.java)  | src/test/java/com/seashare/m2/reservas/integration/ReservationQueryTest.java#detailIncludesContractFields | Draft
