### Test Case Specification: TC-CU01-005 — Degradación de cotizaciones
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU01-005

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar degradación de cotizaciones. Requisitos: FR-003, FR-004. Criterios: SC-001.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
La degradación se prueba a nivel de servicio con cotización simulada; no se comprueban HTTP 200 y los campos serializados quotation_available y estimated_total.

##### Dependencies & Assumptions
Ejecución V4 con dependencias controladas. Pruebas parciales de componentes: [lote](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpBatchEstimateAdapterTest.java); no demuestran el flujo completo de CU-01.

##### Preconditions & Default Setup
Catálogo M1 disponible y proveedor M3 simulado falla o no cotiza algunos IDs.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pasó | VesselSearchServiceTest#quotationUnavailableDegradesWithoutFailing (XML V4: passed). Aserta un barco, mapa de estimaciones vacío y etiqueta de contingencia con puertos simulados. |
| 2 | Ejecutar | Consultar catálogo. | Datos de las precondiciones | El plan prevé 200 con embarcaciones, quotation_available false y estimated_total null para IDs sin precio. | Pendiente | VesselSearchServiceTest#quotationUnavailableDegradesWithoutFailing (XML V4: passed). Aserta un barco, mapa de estimaciones vacío y etiqueta de contingencia con puertos simulados. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | VesselSearchServiceTest#quotationUnavailableDegradesWithoutFailing (XML V4: passed). Aserta un barco, mapa de estimaciones vacío y etiqueta de contingencia con puertos simulados. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
La visualización de etiquetas es UI; el adaptador lote actual cubre parcialmente la degradación.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: VesselSearchServiceTest#quotationUnavailableDegradesWithoutFailing. La degradación se prueba a nivel de servicio con cotización simulada; no se comprueban HTTP 200 y los campos serializados quotation_available y estimated_total.

#### Traceability
FR-003, FR-004 | SC-001 | [lote](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpBatchEstimateAdapterTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/application/service/VesselSearchServiceTest.java#quotationUnavailableDegradesWithoutFailing | In Process
