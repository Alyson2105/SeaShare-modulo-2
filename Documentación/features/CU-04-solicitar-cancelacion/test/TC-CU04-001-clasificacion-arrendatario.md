### Test Case Specification: TC-CU04-001 — Clasificación por anticipación
**Status**: Blocked
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU04-001

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar clasificación por anticipación. Trazabilidad: FR-005, FR-006, FR-007, FR-008, FR-011; SC-002.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
CU-04 y el endpoint propuesto aún no están implementados. Los resultados son contractuales, no observados. El plan especifica truncar la anticipación a minutos antes de comparar; sin endpoint no se verifica.

##### Dependencies & Assumptions
Ejecución futura con CU-04 implementado y dependencias controladas. Pruebas parciales de componentes: [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); no demuestran la orquestación completa.

##### Preconditions & Default Setup
Reserva RESERVED de arrendatario titular, hora de zarpe conocida y zona horaria oficial del puerto. Reloj controlado.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar reserva, reloj y efectos iniciales. | Precondiciones | Entorno aislado disponible. | Pendiente | No ejecutado |
| 2 | Ejecutar | Enviar solicitudes independientes con anticipación de 72 h exactas, un segundo antes de 72 h, 24 h exactas, un segundo antes de 24 h y un minuto antes del zarpe. | Datos de las precondiciones | Clasificaciones respectivas FLEXIBLE, MODERATE, MODERATE, LATE y LATE; cada reserva cambia a CANCELLED. | Pendiente | ChangeReservationStatusServiceTest#cancelledByRenterStoresSubtypeAndAnticipation pasó: integración del motor acepta FLEXIBLE y anticipación suministrados; no calcula umbrales. |
| 3 | Comprobar | Inspeccionar respuesta y persistencia correspondientes. | Reserva y mensajes del escenario | Se cumplen los resultados del paso 2. | Pendiente | Sin publicación real |

##### Section 2: Secondary Flow / Error Handling
No aplica otro comportamiento principal; los límites se indican abajo.

##### Edge Cases & Boundary Conditions
El plan especifica truncar la anticipación a minutos antes de comparar; sin endpoint no se verifica.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Ejecutado el 2026-11-13: Blocked; la evidencia solo cubre el componente compartido, el flujo está bloqueado.

#### Traceability
FR-005, FR-006, FR-007, FR-008, FR-011 | SC-002 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java#cancelledByRenterStoresSubtypeAndAnticipation | Blocked
