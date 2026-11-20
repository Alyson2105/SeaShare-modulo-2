### Test Case Specification: TC-CU09-007 — Ficha completa y ubicación GPS
**Status**: Blocked
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU09-007

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P2

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar ficha completa y ubicación gps. Requisitos: FR-001, FR-004. Criterios: SC-005.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
El DTO y el adaptador ahora mapean registration_number según plan CU-09; falta verificar el contrato y la ficha completa con M1 real.

##### Dependencies & Assumptions
Ejecución con dependencias controladas. Pruebas de HttpVesselReadAdapterTest y VesselDetailControllerTest con proveedores simulados; su cobertura debe evaluarse por escenario.

##### Preconditions & Default Setup
Proveedor M1 entrega ficha completa, puerto y GPS.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | HttpVesselReadAdapterTest#mapsFullDetail (XML V4: passed). Aserta nombre, tipo, capacidad, puerto, coordenadas y zona de una ficha WireMock. Spec FR-004 exige matrícula; VesselDetail y su adaptador no exponen ese campo. La ruta de ficha completa requerida no está implementada. |
| 2 | Ejecutar | Solicitar ficha desde los consumidores CU-01 o CU-19. | Datos de las precondiciones | La especificación espera datos descriptivos y GPS completos para el consumidor. | Pendiente | HttpVesselReadAdapterTest#mapsFullDetail (XML V4: passed). Aserta nombre, tipo, capacidad, puerto, coordenadas y zona de una ficha WireMock. Spec FR-004 exige matrícula; VesselDetail y su adaptador no exponen ese campo. La ruta de ficha completa requerida no está implementada. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | HttpVesselReadAdapterTest#mapsFullDetail (XML V4: passed). Aserta nombre, tipo, capacidad, puerto, coordenadas y zona de una ficha WireMock. Spec FR-004 exige matrícula; VesselDetail y su adaptador no exponen ese campo. La ruta de ficha completa requerida no está implementada. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
La matrícula exigida por spec FR-004 y plan sección 3 no aparece en el DTO de detalle ni en el adaptador; la ruta completa requerida no existe.

FleetInfo expone propietario, capacidad y zona; los consumidores CU-01 y CU-19 existen y usan DTO de catálogo o detalle con GPS, pero falta matrícula.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Blocked; estado documental: Blocked; evidencia: HttpVesselReadAdapterTest#mapsFullDetail. Spec FR-004 exige matrícula; VesselDetail y su adaptador no exponen ese campo. La ruta de ficha completa requerida no está implementada.

#### Traceability
FR-001, FR-004 | SC-005 | Pruebas de detalle ejecutadas | Evidencia: src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/HttpVesselReadAdapterTest.java#mapsFullDetail; src/test/java/com/seashare/m2/reservas/infrastructure/adapter/in/web/VesselDetailControllerTest.java#vesselDetailReturnsFleetDataWithoutCache | Blocked

#### Evidencia local posterior a V4 — 2026-11-13
HttpVesselReadAdapterTest#mapsFullDetail verifica registration_number con fixture WireMock, y VesselDetailControllerTest verifica la respuesta local. Ambas clases pasaron en la suite completa de 642 pruebas; M1 real sigue aplazado. Veredicto técnico: Partial.
