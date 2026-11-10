# CU-02 — Pruebas backend de iniciar reserva

Esta carpeta documenta pruebas backend de `POST /api/v1/reservations`; no sustituye pruebas frontend ni ejecución real.

**Estado global**: Draft. Existen funcionalidades parciales, dependencias pendientes y requisitos UI fuera del alcance backend.

## Trazabilidad

| TC | Escenario / EC | FR | SC | Tipo | Test automatizado | Status |
|---|---|---|---|---|---|---|
| [TC-CU02-001](TC-CU02-001-creacion-exitosa-reserva-iniciada.md) | Creación exitosa | FR-001, FR-004, FR-011, FR-012, FR-013 | SC-001, SC-003 | Backend | [CreateReservationTest](../../../../src/test/java/com/seashare/m2/reservas/integration/CreateReservationTest.java); [CreateReservationServiceTest](../../../../src/test/java/com/seashare/m2/reservas/application/service/CreateReservationServiceTest.java); [CreateReservationOutboxIntegrationTest](../../../../src/test/java/com/seashare/m2/reservas/integration/CreateReservationOutboxIntegrationTest.java) | In Process |
| [TC-CU02-002](TC-CU02-002-validacion-datos-obligatorios.md) | Rechazo de datos inválidos | FR-006, FR-007, FR-008 | SC-001 | Backend | [CreateReservationValidationTest](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/in/web/CreateReservationValidationTest.java) | In Process |
| [TC-CU02-003](TC-CU02-003-carrera-fcfs-bloqueada.md) | Carrera FCFS | FR-014 | SC-002 | Backend futuro | Sin test ejecutable de CU-03 | Blocked |
| [TC-CU02-004](TC-CU02-004-ttl-15-minutos.md) | Cálculo y persistencia TTL | FR-012 | SC-001 | Backend | [TtlPolicyTest](../../../../src/test/java/com/seashare/m2/reservas/domain/service/TtlPolicyTest.java); [CreateReservationTest](../../../../src/test/java/com/seashare/m2/reservas/integration/CreateReservationTest.java) | In Process |
| [TC-CU02-005](TC-CU02-005-idempotencia-clave-60s.md) | Repetición idempotente | FR-011 | SC-001 | Backend | [IdempotencyAdapterTest](../../../../src/test/java/com/seashare/m2/reservas/integration/IdempotencyAdapterTest.java); [IdempotencyClaimConcurrencyTest](../../../../src/test/java/com/seashare/m2/reservas/integration/IdempotencyClaimConcurrencyTest.java); [CreateReservationOutboxIntegrationTest](../../../../src/test/java/com/seashare/m2/reservas/integration/CreateReservationOutboxIntegrationTest.java) | In Process |
| [TC-CU02-006](TC-CU02-006-evento-m3-sin-bloqueo-m1.md) | Evento M3 y ausencia de bloqueo M1 | FR-013 | SC-001 | Backend | [CreateReservationOutboxIntegrationTest](../../../../src/test/java/com/seashare/m2/reservas/integration/CreateReservationOutboxIntegrationTest.java); [ModuleOneNotificationMatrixTest](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java) | In Process |

## Cobertura de requisitos

| FR | Cobertura | TC | Alcance | Motivo / Observación |
|---|---|---|---|---|
| FR-001 | Parcial | 001 | Backend | Recepción de parámetros |
| FR-002 | Sin cobertura backend | — | UI | Modal requiere pruebas frontend |
| FR-003 | Sin cobertura backend | — | UI | Tarjeta requiere pruebas frontend |
| FR-004 | Parcial | 001 | Backend/UI | Monto conservado; presentación y duration_days pendientes de aclaración |
| FR-005 | Sin cobertura backend | — | UI | Banner requiere pruebas frontend |
| FR-006 | Parcial | 002 | Backend/UI | Nombre y teléfono no vacíos; formato telefónico y formulario pendientes |
| FR-007 | Parcial | 002 | Backend/UI | Email opcional validado; presentación UI fuera |
| FR-008 | Parcial | 002 | Backend/UI | Rechazo backend; validación visual fuera |
| FR-009 | Sin cobertura backend | — | UI | Botón requiere pruebas frontend |
| FR-010 | Parcial | 001 | Backend | Consulta disponible; adaptadores fake por defecto |
| FR-011 | Parcial | 001, 005 | Backend | Creación directa; integración CU-08 T-24 pendiente |
| FR-012 | Cubierto documentalmente | 004 | Backend | TTL de creación |
| FR-013 | Cubierto documentalmente | 006 | Backend | Sin notificación M1 al iniciar |
| FR-014 | Bloqueado — pertenece a la transición de CU-03 | 003 | CU-03 | FCFS pendiente |

FR-002, FR-003, FR-005 y FR-009 requieren pruebas frontend o integración UI, fuera del alcance de esta carpeta. Los aspectos visuales de otros FR también quedan fuera.

## Criterios de éxito

| SC | Estado | Observación |
|---|---|---|
| SC-001 | Cobertura documental parcial | TC-001, 002, 004, 005 y 006; sin ejecución ni demostración estadística del 100 %. |
| SC-002 | Bloqueado | TC-003 depende de CU-03. |
| SC-003 | Parcial | TC-001 verifica conservación del monto; no demuestra ausencia global de aritmética ni presentación UI. |

## Checklist de ejecución

| TC | Documentado | Ejecutado | Resultado | Ejecutado por | Fecha | Observaciones |
|---|---|---|---|---|---|---|
| TC-CU02-001 | Sí | No | Pendiente | — | — | Sin ejecutar |
| TC-CU02-002 | Sí | No | Pendiente | — | — | Sin ejecutar |
| TC-CU02-003 | Sí | No | Pendiente | — | — | Bloqueado por CU-03 |
| TC-CU02-004 | Sí | No | Pendiente | — | — | Sin ejecutar |
| TC-CU02-005 | Sí | No | Pendiente | — | — | Sin ejecutar |
| TC-CU02-006 | Sí | No | Pendiente | — | — | Sin ejecutar |

## Brechas detectadas

Contexto de implementación: [implementContext](../../../context/implementContext.md).

1. CU-02 está parcialmente implementado y su contrato tiene aclaraciones pendientes, incluido `duration_days` y formato del monto.
2. CU-03 y FCFS con bloqueo pesimista están pendientes.
3. La identidad utiliza headers temporales; Spring Security está pendiente.
4. Los adaptadores de flota usan fake por defecto.
5. T-24: CU-02 aún no crea mediante CU-08.
6. Los requisitos de interfaz requieren pruebas frontend o integración UI.
7. El plan menciona FR-019, que no existe entre los 14 FR del spec.
8. La publicación efectiva del outbox por relay y sus servicios externos queda fuera de esta validación.
9. El formato telefónico exigido por el plan no está validado; solo se comprueba que no esté vacío y su longitud.

## Convenciones de estado

- **Draft**: contrato insuficiente o inconsistente para cerrar el caso.
- **In Process**: contrato suficiente para ejecutarlo contra la implementación actual; aún no aprobado.
- **Approved**: caso aprobado tras el proceso correspondiente, nunca por esta validación documental.
- **Blocked**: depende de una funcionalidad pendiente.
