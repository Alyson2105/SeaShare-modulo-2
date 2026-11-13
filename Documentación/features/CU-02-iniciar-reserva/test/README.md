# CU-02 — Pruebas backend de iniciar reserva

Esta carpeta documenta pruebas backend de `POST /api/v1/reservations`; no sustituye pruebas frontend ni ejecución real.

**Estado global**: In Process. Los seis casos tienen evidencia parcial; quedan pasos y cobertura integral pendientes. Evaluado el 2026-11-13.

## Trazabilidad

| TC | Escenario / EC | FR | SC | Tipo | Test automatizado | Status |
|---|---|---|---|---|---|---|
| [TC-CU02-001](TC-CU02-001-creacion-exitosa-reserva-iniciada.md) | Creación exitosa | FR-001, FR-004, FR-011, FR-012, FR-013 | SC-001, SC-003 | Backend | [CreateReservationTest](../../../../src/test/java/com/seashare/m2/reservas/integration/CreateReservationTest.java); [CreateReservationServiceTest](../../../../src/test/java/com/seashare/m2/reservas/application/service/CreateReservationServiceTest.java); [CreateReservationOutboxIntegrationTest](../../../../src/test/java/com/seashare/m2/reservas/integration/CreateReservationOutboxIntegrationTest.java) | In Process |
| [TC-CU02-002](TC-CU02-002-validacion-datos-obligatorios.md) | Rechazo de datos inválidos | FR-006, FR-007, FR-008 | SC-001 | Backend | [CreateReservationValidationTest](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/in/web/CreateReservationValidationTest.java) | In Process |
| [TC-CU02-003](TC-CU02-003-carrera-fcfs-bloqueada.md) | Carrera FCFS | FR-014 | SC-002 | Backend | [PaymentIntegrationTest](../../../../src/test/java/com/seashare/m2/reservas/integration/PaymentIntegrationTest.java) (colisión secuencial; falta carrera simultánea) | In Process |
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
| FR-011 | Parcial | 001, 005 | Backend | Creación delegada a CU-08; integración y prueba unitaria ejecutadas |
| FR-012 | Parcial | 004 | Backend | TTL de respuesta y reloj unitario verificados; comparación persistida con reloj controlado pendiente |
| FR-013 | Parcial | 006 | Backend | Evento M3 y decisión de matriz verificados; ausencia de envío M1 en flujo integrado pendiente |
| FR-014 | Parcial | 003 | CU-03 | Pago y rechazo secuencial implementados y probados; carrera simultánea pendiente |

FR-002, FR-003, FR-005 y FR-009 requieren pruebas frontend o integración UI, fuera del alcance de esta carpeta. Los aspectos visuales de otros FR también quedan fuera.

## Criterios de éxito

| SC | Estado | Observación |
|---|---|---|
| SC-001 | Parcial | Pruebas ejecutadas sobre creación, TTL y decisión de notificación; no demuestran el 100 % ni el flujo integrado completo. |
| SC-002 | Parcial | Existe rechazo de solapamiento secuencial; falta prueba de competencia simultánea y demostración de cero sobreventas. |
| SC-003 | Parcial | TC-001 verifica conservación del monto; no demuestra ausencia global de aritmética ni presentación UI. |

## Checklist de ejecución

| TC | Documentado | Ejecutado | Resultado | Ejecutado por | Fecha | Observaciones |
|---|---|---|---|---|---|---|
| TC-CU02-001 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Creación y outbox verificados parcialmente; auditoría y ausencia de M1 en el flujo pendientes. |
| TC-CU02-002 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Se probaron algunas entradas y códigos; faltan variantes y comprobaciones de persistencia. |
| TC-CU02-003 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Colisión de pago secuencial probada; falta carrera simultánea. |
| TC-CU02-004 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | TTL de respuesta y reloj unitario verificados; falta contraste de fila persistida. |
| TC-CU02-005 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Mismo ID, una reserva y un evento verificados en pruebas complementarias; límite de 60 s no controlado. |
| TC-CU02-006 | Sí | Sí | Partial | Agente automatizado | 2026-11-13 | Evento AMQP y matriz verificados; falta ausencia de envío o bloqueo M1 en integración. |

## Brechas detectadas

Contexto de implementación: [implementContext](../../../context/implementContext.md).

1. CU-02 está parcialmente implementado y su contrato tiene aclaraciones pendientes, incluido `duration_days` y formato del monto.
3. La identidad utiliza headers temporales; Spring Security está pendiente.
4. Los adaptadores de flota usan fake por defecto.
6. Los requisitos de interfaz requieren pruebas frontend o integración UI.
7. El plan menciona FR-019, que no existe entre los 14 FR del spec.
8. La publicación efectiva del outbox por relay y sus servicios externos queda fuera de esta validación.
9. El formato telefónico exigido por el plan no está validado; solo se comprueba que no esté vacío y su longitud.
10. `plan.event.md` sección 4 indica 400 por capacidad excedida, mientras `plan.md` exige 409 CAPACITY_EXCEEDED; CreateReservationTest#rejectsPassengersOverCapacity verifica 409 con flota simulada.

## Convenciones de estado

- **Draft**: contrato insuficiente o inconsistente para cerrar el caso.
- **In Process**: contrato suficiente para ejecutarlo contra la implementación actual; aún no aprobado.
- **Approved**: caso aprobado tras el proceso correspondiente, nunca por esta validación documental.
- **Blocked**: depende de una funcionalidad pendiente.

## Ejecución automatizada — 2026-11-13

`sg docker -c 'bash ./mvnw -B clean test'`: salida 1 por ErrorCodeTest#noDuplicateCodes, sin atribuir ese fallo a CU-02. Las pruebas citadas usan las dependencias indicadas en cada caso; no acreditan servicios externos reales.

| Clase | Pruebas | Fallos | Errores | Omitidos | Resultado |
|---|---:|---:|---:|---:|---|
| [CreateReservationTest](../../../../src/test/java/com/seashare/m2/reservas/integration/CreateReservationTest.java) | 5 | 0 | 0 | 0 | Verde; cobertura según TC |
| [CreateReservationOutboxIntegrationTest](../../../../src/test/java/com/seashare/m2/reservas/integration/CreateReservationOutboxIntegrationTest.java) | 3 | 0 | 0 | 0 | Verde; cobertura según TC |
| [PaymentIntegrationTest](../../../../src/test/java/com/seashare/m2/reservas/integration/PaymentIntegrationTest.java) | 9 | 0 | 0 | 0 | Verde; cobertura según TC |
| [IdempotencyAdapterTest](../../../../src/test/java/com/seashare/m2/reservas/integration/IdempotencyAdapterTest.java) | 5 | 0 | 0 | 0 | Verde; cobertura según TC |
| [IdempotencyClaimConcurrencyTest](../../../../src/test/java/com/seashare/m2/reservas/integration/IdempotencyClaimConcurrencyTest.java) | 1 | 0 | 0 | 0 | Verde; cobertura según TC |
| [CreateReservationServiceTest](../../../../src/test/java/com/seashare/m2/reservas/application/service/CreateReservationServiceTest.java) | 4 | 0 | 0 | 0 | Verde; cobertura según TC |
| [ModuleOneNotificationMatrixTest](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java) | 7 | 0 | 0 | 0 | Verde; cobertura según TC |
| [TtlPolicyTest](../../../../src/test/java/com/seashare/m2/reservas/domain/service/TtlPolicyTest.java) | 3 | 0 | 0 | 0 | Verde; cobertura según TC |
| [CreateReservationValidationTest](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/in/web/CreateReservationValidationTest.java) | 13 | 0 | 0 | 0 | Verde; cobertura según TC |
