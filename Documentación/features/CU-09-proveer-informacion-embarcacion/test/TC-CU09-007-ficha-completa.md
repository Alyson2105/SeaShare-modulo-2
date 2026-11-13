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
CU-09 es integración saliente; M1 se simula y el modo fake es el predeterminado. FleetInfo expone propietario, capacidad y zona; los consumidores CU-01 y CU-19 existen y usan DTO de catálogo o detalle con GPS, pero falta matrícula.

##### Dependencies & Assumptions
Ejecución con dependencias controladas. Pruebas relacionadas: Sin prueba de CU-09; su cobertura debe evaluarse por escenario.

##### Preconditions & Default Setup
Proveedor M1 entrega ficha completa, puerto y GPS.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | No ejecutado |
| 2 | Ejecutar | Solicitar ficha desde los consumidores CU-01 o CU-19. | Datos de las precondiciones | La especificación espera datos descriptivos y GPS completos para el consumidor. | Pendiente | Componente WireMock: HttpVesselReadAdapterTest#mapsFullDetail pasó para nombre, tipo, capacidad, puerto y GPS; VesselDetailControllerTest#vesselDetailReturnsFleetDataWithoutCache pasó con adaptador fake. Falta matrícula y validación integral con M1 real. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | No ejecutado |

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
Corrida 2026-11-13: Blocked. Evidencia parcial; consultar las comprobaciones pendientes del flujo.

#### Traceability
FR-001, FR-004 | SC-005 | Pruebas de detalle ejecutadas | Evidencia: src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/HttpVesselReadAdapterTest.java#mapsFullDetail; src/test/java/com/seashare/m2/reservas/infrastructure/adapter/in/web/VesselDetailControllerTest.java#vesselDetailReturnsFleetDataWithoutCache | Blocked
