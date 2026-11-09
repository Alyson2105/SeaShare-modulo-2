# Plan de trabajo técnico y ejecutable: backend Módulo 2 (M2)

**Versión**: 3 (reestructura operativa de la v2; evidencia de auditoría en el Anexo)
**Fecha de auditoría**: 2026-11-06
**Rama**: feature/fase1-setup (ahead 1 respecto a origin)
**Entrega**: sábado 7 de noviembre de 2026
**Responsables**: por asignar (ver sección 4)

> Convenciones: `[NEEDS CLARIFICATION]` = dato que los documentos no cierran. "L" = línea del archivo citado. Las tareas CU-xx marcadas **[PRE-LECTURA]** requieren leer `spec.md` y `plan.md` del CU antes de codificar; su criterio es provisional hasta esa lectura.

---

## 1. Objetivo y alcance completo

Implementar el backend completo del Módulo 2 en Spring Boot 4.1.2, Java 21, Maven, arquitectura hexagonal, paquete `com.seashare.m2`. Alcance: **CU-01 a CU-22, sin recortes**.

- **Core de estados** (CU-08 único escritor) y reglas de reserva (CU-02, CU-03, CU-04..CU-07, CU-13).
- **Integraciones**: M1 (CU-09, CU-10, CU-22 por HTTP PATCH), M3 (CU-11, CU-12, CU-13 canal B, CU-14, CU-15, CU-18), RabbitMQ (topología, outbox, relay, publisher confirms, DLQ).
- **Vistas y disputas**: CU-19..CU-21 (consulta), CU-16..CU-18 (contexto `disputa`).
- **Calidad**: pruebas con PostgreSQL real, reglas ArchUnit, observabilidad, Dockerfile.

Criterio de cierre global: `.\mvnw.cmd clean package` en verde, `.\mvnw.cmd test` en verde, y la aplicación arranca con `ddl-auto=validate`.

---

## 2. Matriz de decisiones con propuesta (D-01 a D-15)

Cada fila trae la recomendación técnica para destrabar hoy. Se adopta por defecto si no hay objeción en 1 hora (regla de proceso del equipo, no sale de los documentos).

| ID | Pregunta / conflicto | Recomendación por defecto | Destraba | Estado |
|---|---|---|---|---|
| D-01 | Transporte de la notificación a M1 (CU-22): AMQP o PATCH HTTP | **PATCH HTTP con destino `M1_HTTP`**. M1 solo expone REST (`features/CU-22-*/plan.md` L279; `technical-plan/plan.md` L229). Descarta AMQP, que exige un consumidor de M1 que no está documentado | T-02 (parte M1), T-04 (parte M1), T-08, T-17a..d | Adoptada por defecto, PENDIENTE de confirmar con M1 |
| D-02 | Exchange, routing key y cola de M1 | Sin efecto mientras D-01 = HTTP. Si M1 exige AMQP, usar la convención de M3 (`consistencia-m2-m1.md` L45-L47) y acordarla con M1 | T-02 (parte M1) | Adoptada por defecto, PENDIENTE de confirmar con M1 |
| D-03 | ¿Se publica `PENDING_PAYMENT` a M3? | **Sí, como `PENDING`**. Lo exige CU-14 (`features/CU-14-*/spec.md` L66; `plan.md` L18, L53) y el código ya lo hace (`ChangeReservationStatusService.java` L31). La excepción es `features/CU-08-*/plan.md` L214: corregirla | T-08, T-12, T-16b | Adoptada por defecto, PENDIENTE de confirmar con M3 |
| D-04 | ¿Hay orden garantizado entre eventos de la misma embarcación? | **No**. M1 trata cada `event_id` como idempotente (`features/CU-22-*/plan.md` L236). El consumidor deduplica | T-04 | Adoptada por defecto, PENDIENTE de confirmar con M1 |
| D-05 | ¿Qué 4xx de M1 son permanentes? | **Todo 4xx es permanente**: no reintenta, va a DLQ con alerta (`features/CU-22-*/spec.md` L91) | T-03 | Adoptada por defecto, PENDIENTE de confirmar con M1 |
| D-06 | Identidad de M3 al llamar a M2 (CU-13 canal A, CU-15) | **Headers temporales** `X-User-Id` y `X-User-Role` con rol de servicio `FINANCE` (rol nuevo, no está en los documentos: `[NEEDS CLARIFICATION]` del nombre). Auth real queda para Fase 10 | T-13, T-16c | Adoptada por defecto, PENDIENTE de confirmar con M3; nombre del rol `FINANCE`: NEEDS CLARIFICATION |
| D-07 | SLA de CU-11 y CU-12 | Usar **read timeout 1000 ms** de `technical-plan/plan.md` L297-L298. Los placeholders de L46 se reemplazan por ese valor hasta que negocio decida | T-15, T-16a | Adoptada por defecto |
| D-08 | Disputa: ¿uno o varios reclamos? | **Un reclamo** por disputa (`technical-plan/plan.md` L195, `UNIQUE(dispute_id)`) | T-18a, T-18b | Adoptada por defecto |
| D-09 | Versiones de librerías con Boot 4 (ShedLock, Resilience4j, springdoc, Testcontainers, WireMock, Awaitility) | **Verificar en Maven Central antes de agregar cada una**. Preferir lo nativo de Spring Framework 7 (`@Retryable`) para reintentos, según `CLAUDE.md` (Dudas abiertas) | T-05, T-14, T-15, T-20, T-21 | Adoptada por defecto |
| D-10 | Alcance: 21 o 22 CU | **22 CU**. CU-22 existe en `Documentación/features/` y `contexto_m2.md` L41 lo incluye | Alcance global | Adoptada por defecto |
| D-11 | Perfiles `.yml` (plan) o `.yaml` (repo) | **Mantener `.yaml`**: `src/main/resources/application.yaml` ya existe. Ajustar el texto del plan | T-19 | Adoptada por defecto |
| D-12 | Prioridad de fases | **Orden de fases 1 a 10 completo** (`technical-plan/plan.md` L477-L503). P1 (fases 3-7) primero; P2 (fases 8-9) en el mismo ciclo, después | Roadmap (sección 3) | Adoptada por defecto |
| D-13 | Seguridad antes de Fase 2 | **Headers temporales sin Spring Security** (`context/contexto_m2.md` L27; `technical-plan/plan.md` L545). El plan original con JWT (L27, L328) queda como referencia | T-11a, T-13, T-16c, endpoints | Adoptada por defecto |
| D-14 | Ubicación de este documento | **Mantener en `Documentación\context\`**. Registrar la excepción de solo lectura de `technical-plan/plan.md` L532 | T-01 | Adoptada por defecto (excepción de ubicación registrada, ver nota) |
| D-15 | Fase de CU-22 (no asignado en `plan.md`) | **Fase 4, dentro de T-08**. CU-22 es `<<include>>` de CU-08 (`features/CU-22-*/spec.md` L6) | T-08 | Adoptada por defecto |

> **Registro de excepción (D-14)**: este documento permanece en `Documentación/context/` pese a la regla de solo lectura de `technical-plan/plan.md` L532. Excepción autorizada y registrada el2026-11-07 en T-01.
>
> **Estado de la matriz (T-01, 2026-11-07)**: todas las decisiones quedan adoptadas por defecto. D-01, D-02, D-04 y D-05 pendientes de confirmar con M1; D-03 y D-06 pendientes de confirmar con M3 (D-06 además con el nombre del rol `FINANCE` como NEEDS CLARIFICATION). El resto no afecta a otros equipos.

---

## 3. Roadmap por fases

Convención por tarea: archivos a tocar (ruta real o **nueva**, con paquete propuesto), tests (método → comando), criterio de aceptación.

**Definición de terminado (aplica a todas)**: `.\mvnw.cmd -q compile` sin errores; tests de la tarea en verde; `.\mvnw.cmd test` completo en verde; `git status` solo con las rutas de la tarea; commit con rutas explícitas (nunca `git add .`); mensaje en español con `feat:`, `fix:`, `chore:` o `docs:`.

**Lectura previa de CU**: las tareas con **[PRE-LECTURA]** empiezan leyendo `spec.md` y `plan.md` de su CU. Los CU ya leídos son CU-02, CU-08, CU-14 y CU-22.

### Hito H1 · Fase 1 · Setup y decisiones

| ID | Tarea | CU | Archivos | Tests | Aceptación | Depende |
|---|---|---|---|---|---|---|
| T-01 | Registrar D-01..D-15 y excepción de ubicación | Todos | `Documentación/context/plan-trabajo-backend.md` | Ninguno (documental) | Decisiones aprobadas por el equipo o adoptadas por defecto (sección 2) | — |
| T-19 | Dockerfile multi-stage, README de arranque, perfiles de configuración | Todos | `Dockerfile` (nuevo), `README.md` (nuevo), `src/main/resources/application-dev.yaml` (nuevo, D-11), `application-test.yaml`, `application-prod.yaml` | Arranque con `--spring.profiles.active=dev` (manual) | Imagen construye; app arranca con `ddl-auto=validate` | T-01 |
| T-20 | springdoc-openapi y verificación de estilo (Checkstyle o Spotless) | Todos | `pom.xml`, configuración de plugin | `.\mvnw.cmd verify` | Versiones verificadas (D-09); build pasa | T-01 |
| T-21 | Dependencias de test: Testcontainers, WireMock, Awaitility | Todos | `pom.xml`, `src/test/java/com/seashare/m2/shared/` (base de pruebas nueva) | Test de humo con contenedor PostgreSQL | Contenedor levanta en `.\mvnw.cmd test` | T-01 |

### Hito H2 · Fase 2 · Fundación de integración

| ID | Tarea | CU | Archivos | Tests | Aceptación | Depende |
|---|---|---|---|---|---|---|
| T-02 | Topología RabbitMQ: exchange `seashare.reservations` (topic), colas `finance.reservation-status.v1`, `finance.reservation-info.v1`, `finance.guarantee-dispute.v1`, DLQ, bindings | CU-02, CU-14, CU-18 (M3); CU-22 (M1 si D-01 cambia) | Nueva `shared`/`infrastructure/adapter/out/messaging/config/RabbitTopologyConfig.java` (paquete propuesto); `application.yaml` | `RabbitTopologyTest` → `.\mvnw.cmd test "-Dtest=RabbitTopologyTest"` | Exchange, colas y bindings declarados al arrancar (Testcontainers) | T-21 |
| T-03 | Relay del outbox: publicación con publisher confirms, marca `published_at` tras ack, backoff 1/5/25/125 s, 5 intentos, `dead=true` y DLQ | CU-02, CU-08, CU-14, CU-22 | Nueva `infrastructure/adapter/out/messaging/outbox/OutboxRelayJob.java` (paquete propuesto); reutiliza `OutboxAdapter.java` L30-L82, `OutboxMessageEntity.java`; V3 L26-L27 (`dead`, `last_error`) | `OutboxRelayJobTest` (publica y marca); `OutboxRelayRetryTest` (backoff y DLQ) | Mensaje publicado y `published_at` no nulo tras confirm; tras 5 fallos `dead=true` | T-02 |
| T-04 | Consumidor/publicador AMQP y deduplicación por `event_id` | CU-02, CU-14, CU-18, CU-22 | Nueva `infrastructure/adapter/out/messaging/` (publicador y consumidor de prueba) | `OutboxRelayIntegrationTest` (cubre la publicación que el plan llamaba `OutboxPublisherIntegrationTest`); `EventDeduplicationTest` | Un mensaje duplicado se procesa una vez | T-03, D-04 |
| T-05 | ShedLock para jobs multi-réplica | CU-08 (TTL), CU-16, CU-17 (jobs) | `pom.xml`; tabla `shedlock` ya existe en V2 L145 | `ShedLockTtlJobTest` | Dos instancias no ejecutan el job a la vez | T-20, D-09 |
| T-22 | Observabilidad (correlation id HTTP y AMQP, MDC, métricas, health checks) y reglas ArchUnit de capas y "sin dinero" | Todos | `shared/` (filtro de correlation id, nuevo); `LayeredArchitectureTest.java` (existente); `moneyRulesArchTest` (nuevo, `src/test/java/com/seashare/m2/reservas/architecture/`) | `LayeredArchitectureTest`, `MoneyArchitectureTest` | Correlation id en respuesta y en headers AMQP; ArchUnit en verde | T-03 |

### Hito H3 · Fase 3 · Lectura de integraciones (M1 y M3)

| ID | Tarea | CU | Archivos | Tests | Aceptación | Depende |
|---|---|---|---|---|---|---|
| T-14 | Cliente HTTP real de M1: información de embarcación (CU-09) y estado operativo (CU-10). Reemplaza `FakeFleet*Adapter.java` como default | CU-09, CU-10 **[PRE-LECTURA]** | Nuevo `infrastructure/adapter/out/fleet/HttpFleetInfoAdapter.java` y `HttpFleetOperationalStatusAdapter.java` (paquete propuesto); `FakeFleet*Adapter.java` L13 y L11 (pasa a perfil test) | `HttpFleetClientTest` con WireMock; `FleetUnavailableTest` (503 → `FLEET_SERVICE_UNAVAILABLE`) | Timeouts connect 100 ms, read 300 ms; un reintento solo ante 5xx o timeout; sin fallback a caché | T-21, D-09 |
| T-15 | Cliente HTTP de M3: estimación en lote e individual (CU-11) y cálculo total (CU-12) | CU-11, CU-12 **[PRE-LECTURA]** | Nuevo `infrastructure/adapter/out/finance/HttpFinanceQuoteAdapter.java` (paquete propuesto) | `FinanceQuoteClientTest` con WireMock | Read timeout 1000 ms (D-07); sin cálculo local de montos | T-21, D-09 |

### Hito H4 · Fase 4 · Núcleo de estados (CU-08 único escritor)

| ID | Tarea | CU | Archivos | Tests | Aceptación | Depende |
|---|---|---|---|---|---|---|
| T-06 | Contrato completo de CU-08: puerto `ApplyReservationTransitionUseCase`, comando con `actor_id`, `expected_version`, `mechanical_failure`, `anticipation_minutes`, `completion_notes`, `payment_reference`; respuesta con `version` y `events_enqueued` | CU-08 | `application/port/in/ChangeReservationStatusUseCase.java` (existe, L1-L8); `application/dto/ChangeReservationStatusCommand.java` (existe, L9-L15); `application/service/ChangeReservationStatusService.java` (existe, L28-L106) | `ChangeReservationStatusServiceTest` (existente: L106, L120, L135, L149, L166); `ReservationStateMachineTest` (L17-L50); nuevos: `reservedFromPendingPaymentWithPaymentReference`, `cancelledByRenterStoresSubtypeAndAnticipation`, `pendingPaymentToPaymentFailedStoresReason`, `cancelFromPendingPaymentIsRejected` | Todos los escenarios de `CU-08/spec.md` L60-L88 y L156-L159 verdes | T-01 |
| T-07 | TTL también desde `PENDING_PAYMENT` | CU-08 (escenario 7) | `infrastructure/adapter/out/persistence/reservation/ReservationJpaRepository.java` L36-L37 (JPQL hoy solo `INITIATED`); `ReservationRepositoryAdapter.java` L109-L114; `application/port/out/ReservationRepositoryPort.java` L29-L33; `application/service/ExpireReservationsService.java` L44-L72 | `expiresPendingPaymentPastTtl` (`TtlExpiryIntegrationTest`); `pendingPaymentBeforeTtlIsNotExpired` (`ExpireReservationsServiceTest`) | Reservas en `INITIATED` y `PENDING_PAYMENT` vencidas pasan a `EXPIRED` sin outbox | T-06, T-05 |
| T-08 | Matriz de notificaciones: encolar a M3 (CU-14) y M1 (CU-22) según la tabla de `CU-22/spec.md` L110-L116; guarda `m1_blocked` que se pone en `true` al encolar `RESERVED` | CU-08, **CU-22** (D-15) | `ChangeReservationStatusService.java` L30-L36 (`OUTBOX_STATUSES`) y L77-L85; `ReservationMapper.java` L54 y L60-L62; nueva `application/service/ModuleNotificationMatrix.java` (propuesta); `OperationalStatusEvent` (nombre en `CU-22/spec.md` L143) | `ModuleOneNotificationMatrixTest` (nuevo); `enqueuesReservedToModuleOneOnPaymentConfirmed`; `initiatedAndPendingPaymentDoNotEnqueueModuleOne`; `cancelledBeforeReservedDoesNotEnqueueModuleOne`; `cancelledByOwnerWithMechanicalFailureEnqueuesMaintenance` | Eventos correctos por transición; `m1_blocked = true` tras `RESERVED`; sin evento si la transición falla | T-06, T-03, D-01, D-03 |
| T-09 | Persistir campos de CU-08 no mapeados: cierre, pago, horas reales, novedades | CU-08 | `ReservationMapper.java` L60-L62; `ReservationEntity.java` L97, L100, L103, L106, L109 (campos existentes); nueva migración **V4** para `status_changed_at` (V1 no lo tiene) | `applyChangesPersistsCompletionNotes` (`ReservationMapperTest`); `persistsPaymentReferenceOnReserved` (`ReservationPersistenceTest`) | Campos persistidos; ningún importe recalculado | T-06 |
| T-10 | Auditoría completa (incluye creación) e inmutabilidad verificada | CU-08 | `StatusAuditAdapter.java` L17-L27; `ReservationStatusAuditEntity.java` L25-L50; V4 para `actor_id` y `event_ids`; trigger V1 L80-L89 | `StatusAuditImmutabilityTest` (nuevo); `creationWritesAuditRow` | UPDATE/DELETE en `reservation_status_audit` falla; cada transición tiene fila | T-06, T-09 |

### Hito H5 · Fase 5 · Superficie para M3

| ID | Tarea | CU | Archivos | Tests | Aceptación | Depende |
|---|---|---|---|---|---|---|
| T-12 | Publicar `reservation.status.changed` con el contrato de CU-14: `status` como `PENDING`, `RESERVED`, `IN_NAVIGATION`, `COMPLETED`, `CANCELLED_*`; `Message-Id`; `payment_token_ref` solo en `PENDING`; `actor`, anticipación y novedades | CU-14 **[leído]** | `OutboxAdapter.java` L30-L54 (cambiar nombres internos por contrato); nuevo mapeador `FinanceStatusMapper` (propuesto) en `infrastructure/adapter/out/messaging/` | `OutboxAdapterTest.statusChangedUsesFinanceStatusNames`; `cancelledPayloadIncludesSubtypeAndAnticipation`; `completedPayloadIncludesCompletionNotesWhenProvided`; `payloadHasNoAmountFields` | Payload coincide con `CU-14/plan.md` L38-L47 y L51-L63; sin campos monetarios | T-03, T-04, T-06, D-03 |
| T-13 | Endpoint interno de consulta para M3 | CU-15 **[PRE-LECTURA]** | Nuevo `infrastructure/adapter/in/web/InternalReservationController.java`; `GET /api/v1/internal/reservations/{id}` (`technical-plan/plan.md` L233) | `InternalReservationControllerTest` (MockMvc); `financeIdentityRequired` | 200 con datos de reserva; 403 sin rol `FINANCE` (D-06) | T-06, D-06, D-13 |

### Hito H6 · Fase 6 · Vistas de consulta

| ID | Tarea | CU | Archivos | Tests | Aceptación | Depende |
|---|---|---|---|---|---|---|
| T-11a | Detalle de embarcación: datos de M1 y cotización individual de M3 | CU-19 **[PRE-LECTURA]** | Nuevo `infrastructure/adapter/in/web/VesselDetailController.java`; usa T-14 y T-15 | `VesselDetailControllerTest` (WireMock) | Respuesta con datos de M1 y cotización; sin datos de flota persistidos | T-14, T-15, D-13 |
| T-11b | Mis reservas: filtro `filter`, `pagination`, `applied_filter`, `status_badge` | CU-20 **[PRE-LECTURA]** | `ReservationQueryController.java` L43; `ReservationQueryService.java`; `ReservationResponse.java` L14-L53 | `ReservationQueryTest` (existente); nuevos: `listsWithAppliedFilter`, `statusBadgeMatchesStatus` | Contrato del spec de CU-20 cumplido; paginación `page >= 1`, `1 <= limit <= 50` | T-06 |
| T-11c | Detalle de reserva: `status_badge` y datos operativos | CU-21 **[PRE-LECTURA]** | `ReservationQueryController.java` L56; `ReservationQueryService.java`; `ReservationResponse.java` | `ReservationQueryTest`; `detailIncludesOperationalData` | Contrato de CU-21 cumplido; pertenencia validada | T-11b, T-09 |

### Hito H7 · Fase 7 · Embudo de conversión

| ID | Tarea | CU | Archivos | Tests | Aceptación | Depende |
|---|---|---|---|---|---|---|
| T-24 | Completar CU-02: `Location` en 201; creación pasa por CU-08 (`CreateReservationService.java` L125); `estimated_total` desde M3 o cliente (`[NEEDS CLARIFICATION]`, Anexo B, L16); `departure_time` en spec | CU-02 | `ReservationController.java` L31-L65 (cambiar a `ResponseEntity`); `CreateReservationService.java` L125; `CreateReservationRequest.java` L15-L47; `ReservationResponse.java` L14-L53; `CallerIdentityArgumentResolver.java` L28-L32 | `createsLocationHeader`; `creationGoesThroughStatusUseCase`; `missingHolderNameIs400NoReservation`; `missingHolderPhoneIs400NoReservation` (`CreateReservationValidationTest`) | Contrato de `CU-02/plan.md` L24-L101 y L158-L164; ningún código de creación fuera de CU-08 | T-14, T-06 |
| T-16a | Búsqueda de embarcaciones con cotización en lote | CU-01 **[PRE-LECTURA]** | Nuevo `infrastructure/adapter/in/web/VesselSearchController.java`; `application/service/VesselSearchService.java` | `VesselSearchServiceTest`; `VesselSearchControllerTest` | Catálogo con cotización por lote (T-15); sin cálculo local de montos | T-15, T-14 |
| T-16b | Iniciar pago: transición a `PENDING_PAYMENT`, FCFS con bloqueo pesimista sobre `vessel_id`, `payment_token_ref` obligatorio, publicación `PENDING` | CU-03 **[PRE-LECTURA]** (CU-03 usa CU-12 final: `technical-plan/plan.md` L393) | Nuevo `infrastructure/adapter/in/web/PaymentController.java`; `application/service/StartPaymentService.java`; `ReservationJpaRepository.java` (consulta con `PESSIMISTIC_WRITE`) | `StartPaymentServiceTest`; `fcfsSecondRequesterGets409` (`PaymentConcurrencyTest`) | 409 `CONCURRENCY_CONFLICT` al perdedor (`CU-02/spec.md` L33-L36); `PENDING` publicado | T-15, T-06, T-12, D-03 |
| T-16c | Confirmar pago (CU-13): canal B por job de consulta a M3 (cada 5 s, `CU-13/plan.md` L226); canal A apagado | CU-13 **[PRE-LECTURA]** | Nuevo `infrastructure/adapter/out/finance/PaymentStatusPollingJob.java` (job, ShedLock de T-05); `infrastructure/adapter/in/web/PaymentConfirmationController.java` (canal A, apagado por configuración) | `PaymentPollingJobTest`; `approvedAfterTtlPublishesCancelledFlexible` (`CU-13/plan.md` L25) | `APPROVED` con TTL vigente → `RESERVED`; cobro tardío → `CANCELLED_FLEXIBLE` + alerta | T-15, T-06, T-05, D-06 |

### Hito H8 · Fase 8 · Operaciones en muelle

| ID | Tarea | CU | Archivos | Tests | Aceptación | Depende |
|---|---|---|---|---|---|---|
| T-17a | Solicitar cancelación: clasificación Flexible (>72 h, 100 %), Moderada (72-24 h, 50 %), Tardía (<24 h) y por Propietario | CU-04 **[PRE-LECTURA]** | Nuevo `infrastructure/adapter/in/web/CancellationController.java`; `application/service/RequestCancellationService.java`; tabla `cancellation_event` (V2 L28); `domain/service/CancellationClassifier.java` (propuesto) | `CancellationClassifierTest` (bordes 72 h y 24 h exactos); `RequestCancellationServiceTest` | Clasificación exacta en los bordes; sin montos calculados | T-14 (zona horaria), T-06, T-12 |
| T-17b | Marcar inasistencia con tolerancia de 30 min | CU-05 **[PRE-LECTURA]** | Nuevo `NoShowController.java`; tabla `noshow_event` (V2 L39) | `NoShowWindowPolicyTest` (exacto 30 min) | No se acepta antes de 30 min | T-14, T-06, T-12 |
| T-17c | Marcar inicio de navegación (check-in) | CU-06 **[PRE-LECTURA]** | Nuevo `CheckInController.java`; tabla `checkin_event` (V2 L48) | `CheckInServiceTest`; `checkInNotifiesModuleOne` | Transición a `IN_NAVIGATION`; aviso `IN_NAVIGATION` a M1 (`consistencia-m2-m1.md` L28) | T-06, T-08, T-12 |
| T-17d | Marcar fin de navegación (check-out) | CU-07 **[PRE-LECTURA]** | Nuevo `CheckOutController.java`; tabla `checkout_event` (V2 L56) | `CheckOutServiceTest`; `checkOutSignalsMaintenanceCleaning` | Transición a `COMPLETED`; señal `MAINTENANCE_CLEANING` a M1 (`consistencia-m2-m1.md` L29) | T-06, T-08, T-12 |

### Hito H9 · Fase 9 · Disputa de garantía

| ID | Tarea | CU | Archivos | Tests | Aceptación | Depende |
|---|---|---|---|---|---|---|
| T-18a | Generar disputa de garantía en contexto `disputa`; contrato `OpenDisputeUseCase` en `shared/contracts/dispute/` | CU-16 **[PRE-LECTURA]** | Nuevo paquete `com.seashare.m2.disputa` (`domain/`, `application/`, `infrastructure/`); `shared/contracts/dispute/OpenDisputeUseCase.java` (nuevo); tablas `dispute_guarantee` (V2 L89) y `dispute_claim` (V2 L104) | `OpenDisputeServiceTest`; `OneClaimPerDisputeTest` (D-08); `OpenDisputeIntegrationTest` | Solo se abre desde `COMPLETED`; un reclamo por disputa | T-04, T-17d |
| T-18b | Actualizar estado de disputa por Admin (`PENDING`, `REJECTED`, `ACCEPTED`) con ventana fija de 24 h y job de cierre | CU-17 **[PRE-LECTURA]** | Nuevo `AdminDisputeController.java`; tabla `admin_review` (V2 L114); `DisputeWindowCloseJob.java` (con ShedLock T-05) | `DisputeStateMachineTest`; `DisputeWindowCloseJobTest`; `DISPUTE_ALREADY_RESOLVED` en terminales | Estados terminales inmutables; ventana de 24 h exacta | T-18a, T-05 |
| T-18c | Publicar información de disputa a M3 (solo estados finales, sin montos, con identificador único) | CU-18 **[PRE-LECTURA]** | Nuevo publicador en `disputa/infrastructure/adapter/out/messaging/`; routing key `reservation.dispute.updated` (`consistencia-m2-m3.md` §5, `CU-18/plan.md` L453) | `DisputePublishingTest` (cero mensajes en `PENDING`); `disputePayloadHasNoAmounts` | Publica solo `REJECTED`/`ACCEPTED`; reintento hasta confirmación | T-18a, T-04 |

### Hito H10 · Fase 10 · Cierre

| ID | Tarea | CU | Archivos | Tests | Aceptación | Depende |
|---|---|---|---|---|---|---|
| T-23 | Cierre: resolver NEEDS CLARIFICATION restantes, pruebas de resiliencia (caída de M1 y M3, reinicio con TTL en curso, reinicio del broker), carga y documentación de API | Todos | `README.md`; `src/test/java/com/seashare/m2/resilience/` (nuevo) | `ResilienceRestartTest`; `BrokerRestartTest` | Objetivos de latencia de `technical-plan/plan.md` L37-L46 medidos | Todas |

### Tabla resumen de IDs

| Hito | IDs |
|---|---|
| H1 | T-01, T-19, T-20, T-21 |
| H2 | T-02, T-03, T-04, T-05, T-22 |
| H3 | T-14, T-15 |
| H4 | T-06, T-07, T-08, T-09, T-10 |
| H5 | T-12, T-13 |
| H6 | T-11a, T-11b, T-11c |
| H7 | T-24, T-16a, T-16b, T-16c |
| H8 | T-17a, T-17b, T-17c, T-17d |
| H9 | T-18a, T-18b, T-18c |
| H10 | T-23 |

---

## 4. Paralelización y reparto por frentes

Cada frente es dueño de sus archivos. Un archivo compartido tiene un solo dueño por vez y un commit por tarea.

| Frente | Tareas | Archivos que posee | Dependencias de otros frentes |
|---|---|---|---|
| **A. Core de estados y dominio** | T-06, T-07, T-08, T-09, T-10, T-24 | `ChangeReservationStatusService.java`, `ExpireReservationsService.java`, `ReservationStateMachine.java`, `ReservationMapper.java`, `StatusAuditAdapter.java`, V4 | Mensajería (T-03, T-04) para el encolado real; Integraciones (T-14) para CU-02 |
| **B. Mensajería y outbox** | T-02, T-03, T-04, T-05, T-12, T-22 (parte AMQP) | `OutboxAdapter.java`, `OutboxRelayJob.java`, `FinanceStatusMapper`, `RabbitTopologyConfig.java`, publicadores AMQP | Core (T-06) para el payload; D-01 y D-03 |
| **C. Vistas y HTTP** | T-11a, T-11b, T-11c, T-13, T-16a, T-16b, T-16c, T-17a..d, T-18a..c (controladores) | `ReservationQueryController.java`, `ReservationQueryService.java`, `ReservationResponse.java`, controladores nuevos por CU | Core (T-06); Integraciones (T-14, T-15); Mensajería (T-12) |
| **D. Integraciones HTTP M1 y M3** (frente adicional, recomendado) | T-14, T-15, T-16c (job de polling) | `infrastructure/adapter/out/fleet/`, `infrastructure/adapter/out/finance/`, `FakeFleet*Adapter.java` (pasan a perfil test) | Solo D-07, D-09 |

**Archivos compartidos** (coordinar antes de editar):

| Archivo | Frente dueño | Quién más lo toca | Regla |
|---|---|---|---|
| `pom.xml` | Integraciones (D) | Mensajería (B), Fundación (T-20, T-21) | Un commit por tarea; avisar antes |
| `src/main/resources/application.yaml` | Mensajería (B) | Todos | Cambios pequeños, un commit por tarea |
| `ReservationEntity.java` | Core (A) | Vistas (C) | Solo A modifica; C pide el cambio |
| `V4__*.sql` (nueva migración) | Core (A) | Mensajería (B) para `outbox_message` | Una sola migración por ciclo; V1, V2 y V3 intocables (`contexto_m2.md` L21) |

Orden de integración: cada frente integra en su rama de tarea; los cambios al core (A) se integran primero.

---

## 5. Reglas de git y convención de commits

- Trabajar en `feature/<nombre>` sobre `develop` (`CLAUDE.md` L144). Un commit por tarea (`CLAUDE.md` L147).
- Commits en español: `feat:`, `fix:`, `chore:`, `docs:`.
- Flujo: commit → `git push -u origin <rama>` → Pull Request a `develop` (`CLAUDE.md` L148).
- Nunca `git add .` ni `git add -A`. Siempre rutas explícitas.
- Nunca `git stash pop` (stash viejo, `CLAUDE.md` L146).
- No commitear `.idea`, `CLAUDE.md`, `target/`, `untitled/` ni la carpeta `docs` (cualquier nivel). Regla de este equipo, fuente: instrucción del usuario.
- Comandos destructivos (`git reset --hard`, `git clean`, `docker compose down -v`): pedir confirmación primero (`CLAUDE.md` L149-L150).

---

## Anexo A. Auditoría por CU (evidencia)

| CU | Documentación | Código | Tests | Estado real |
|---|---|---|---|---|
| CU-01 | spec, plan | Ninguno | Ninguno | PENDIENTE |
| CU-02 | spec, plan, plan.event | `ReservationController.java` L31; `CreateReservationService.java` L125, L129 | `CreateReservationTest` L45; `CreateReservationOutboxIntegrationTest` L48 | PARCIAL |
| CU-03 | spec, plan | Ninguno | Ninguno | PENDIENTE |
| CU-04 | spec, plan | Solo tabla `cancellation_event` (V2 L28) | Ninguno | PENDIENTE |
| CU-05 | spec, plan | Solo tabla `noshow_event` (V2 L39) | Ninguno | PENDIENTE |
| CU-06 | spec, plan | Solo tabla `checkin_event` (V2 L48) | Ninguno | PENDIENTE |
| CU-07 | spec, plan | Solo tabla `checkout_event` (V2 L56) | Ninguno | PENDIENTE |
| CU-08 | spec, plan | `ChangeReservationStatusService.java` L28; `ReservationStateMachine.java` L11-L42 | `ReservationStateMachineTest`; `ChangeReservationStatusServiceTest` | PARCIAL |
| CU-09 | spec, plan | `FakeFleetInfoAdapter.java` L13 (fake) | Ninguno | PENDIENTE (solo fake) |
| CU-10 | spec, plan | `FakeFleetOperationalStatusAdapter.java` L11 (fake) | Ninguno | PENDIENTE (solo fake) |
| CU-11 | spec, plan.external.lote, plan.external.individual | Ninguno | Ninguno | PENDIENTE |
| CU-12 | spec, plan | Ninguno | Ninguno | PENDIENTE |
| CU-13 | spec, plan, plan.external | Solo tabla `payment_attempt` (V2 L14) | Ninguno | PENDIENTE |
| CU-14 | spec, plan | `OutboxAdapter.java` L30 (encola) | `OutboxAdapterTest` L94 | PENDIENTE (sin publicación) |
| CU-15 | spec, plan | Ninguno | Ninguno | PENDIENTE |
| CU-16 | spec, plan | Solo tablas `dispute_guarantee` (V2 L89), `dispute_claim` (V2 L104) | Ninguno | PENDIENTE |
| CU-17 | spec, plan | Solo tabla `admin_review` (V2 L114) | Ninguno | PENDIENTE |
| CU-18 | spec, plan | Ninguno (sin contexto `disputa`) | Ninguno | PENDIENTE |
| CU-19 | spec, plan | Ninguno | Ninguno | PENDIENTE |
| CU-20 | spec, plan | `ReservationQueryController.java` L43 | `ReservationQueryTest` | PARCIAL |
| CU-21 | spec, plan | `ReservationQueryController.java` L56 | `ReservationQueryTest` | PARCIAL |
| CU-22 | spec, plan, consistencia-m2-m1.md | Ninguno | Ninguno | PENDIENTE |

**Piezas transversales**:
- Migraciones: `V1__init.sql`, `V2__remaining_tables.sql`, `V3__reservation_fields_and_outbox.sql`. No existe V4.
- Relay outbox a RabbitMQ: **no existe** (sin resultados en `src/` para `RabbitTemplate`, `AmqpTemplate`, `RabbitListener`, `relay`).
- Job TTL: `TtlExpiryJob.java` L27 (30000 ms por defecto); sin ShedLock (TODO en L13).
- Fakes de flota activos por defecto: `FakeFleetInfoAdapter.java` L13, `FakeFleetOperationalStatusAdapter.java` L11.
- Idempotencia de entrada: `IdempotencyAdapter.java` L28.
- Reloj: `shared/domain/ClockPort.java` L7; `SystemClockAdapter.java` L20.
- Errores: `GlobalExceptionHandler.java`; `shared/domain/ErrorCode.java` L8-L28.

**Verificaciones del entorno (2026-11-06)**:
- Rama `feature/fase1-setup`, ahead 1. Último commit de CU-02: `96e7568` (`git log -S`).
- `ErrorCode`: 21 constantes (L8-L28). El "20" de la nota no coincide.
- `IdempotencyKeyJpaRepository.java` L24: `@Modifying(flushAutomatically = true, clearAutomatically = true)`.
- `.\mvnw.cmd test` con Docker: 83 tests, 0 fallos, BUILD SUCCESS.

**Afirmaciones de la auditoría (veredictos)**:
- a) Máquina de estados: CONFIRMADA. Catálogo de "20" códigos: NO CONFIRMADA (son 21).
- b) CU-02, CU-08, CU-20, CU-21 en código: CONFIRMADA.
- c) Job de expiración solo INITIATED: CONFIRMADA (`ReservationJpaRepository.java` L36).
- d) Sin relay del outbox: CONFIRMADA.
- e) M1 simulado con fakes: CONFIRMADA.
- f) Traducción a M3: existe en documento (`CU-14/plan.md` L53, L57-L61), no en código. LATE y NO_SHOW van a `CANCELLED_STRICT` (L59, L61).
- g) CU-02 encola `reservation.info.provided`: CONFIRMADA (`CreateReservationService.java` L129; `OutboxAdapter.java` L57).
- h) Transporte de M1: NO RESUELTO en documentos; ver D-01.

---

## Anexo B. Inconsistencias entre documentos y código

| ID | Contradicción | Lados |
|---|---|---|
| L1 | Rango de CU: 22 vs 21 | `CLAUDE.md` L98 vs `contexto_m2.md` L41, L80; `technical-plan/plan.md` L12. Resuelto por D-10 |
| L2 | Transporte de M1: AMQP vs PATCH | `consistencia-m2-m1.md` L9, L166; `CU-22/plan.md` L7, L48 vs `technical-plan/plan.md` L229; `CU-22/plan.md` L279. Resuelto por D-01 |
| L3 | Migraciones aplicadas: V1 y V2 vs V3 en repo | `CLAUDE.md` L83-L85; `technical-plan/plan.md` L550 vs `db/migration/`; `contexto_m2.md` L78 |
| L4 | PENDING_PAYMENT a M3 | `CU-08/plan.md` L145 y L214; `CU-08/spec.md` L40, L192; `CU-14/spec.md` L66; `CU-14/plan.md` L18, L53; código `ChangeReservationStatusService.java` L31. Resuelto por D-03 |
| L5 | Conteo de códigos de error | Nota: 20; `ErrorCode.java` L8-L28: 21 |
| L6 | Conteo de pruebas | `implementContext.md` L163: 75 (2026-11-03); Maven 2026-11-06: 83 |
| L7 | Expiración TTL | `contexto_m2.md` L77 y `implementContext.md` L111, L116 (solo INITIATED) vs `contexto_m2.md` L52 (INITIATED y PENDING_PAYMENT) |
| L8 | CU-22 con dos transportes | `CU-22/plan.md` L7, L48 vs L279 (mismo archivo) |
| L9 | Tipo de tiempo | `ReservationEntity` usa `Instant`; `ReservationResponse` usa `OffsetDateTime` (L49) |
| L10 | CU-22 sin fase | `technical-plan/plan.md` L349-L459. Resuelto por D-15 |
| L11 | Número de CU dentro del mismo plan | `technical-plan/plan.md` L12 |
| L12 | CLAUDE.md "Estado actual" desactualizado | `CLAUDE.md` L159 (V1 y V2). No usado como evidencia |
| L13 | Migraciones en `technical-plan/plan.md` | L550 (V1 y V2) vs `CLAUDE.md` L87 (V3, V4) |
| L14 | FR de CU-02 inexistentes | `CU-02/plan.md` L35 (FR-019), L163 (FR-018); `plan.event.md` L80 (FR-015). El spec solo define FR-001 a FR-014 |
| L15 | Autenticación de CU-02 | `CU-02/plan.md` L33 (JWT) vs `contexto_m2.md` L27 y `CallerIdentityArgumentResolver.java` L28-L29 (headers). Resuelto por D-13 |
| L16 | Origen de `estimated_total` | `CU-02/spec.md` L60 (M3) vs `CU-02/plan.md` L49 y `ReservationController.java` L60 (cliente). Pendiente K-19 |
| L17 | Creación fuera de CU-08 | `CU-02/spec.md` L31; `CU-02/plan.md` L13; `CU-08/spec.md` L53, L176 vs `CreateReservationService.java` L125 |
| L18 | `departure_time` | `CU-02/plan.md` L48 (obligatorio); `CU-02/spec.md` no lo menciona |
| L19 | Header `Location` | `CU-02/plan.md` L101 vs `ReservationController.java` L31-L32 (no lo devuelve) |
| L20 | Término "turista" | `CU-02/spec.md` L5 vs `contexto_m2.md` L48 |
| L21 | Intervalo y bloqueo del job TTL | `CU-08/plan.md` L197 (5 s, `FOR UPDATE SKIP LOCKED`) vs `TtlExpiryJob.java` L27 (30 s) y `ReservationJpaRepository.java` L36 (sin bloqueo) |
| L22 | `PERSISTENCE_ERROR` | `CU-08/plan.md` L168 vs `ErrorCode.java` (no existe) |
| L23 | Campos de auditoría y reserva | `CU-08/plan.md` L203-L205 vs V1 L11-L57, L67-L76, L94-L107 |
| L24 | Auditoría de la creación | `CU-08/plan.md` L198 vs `CreateReservationService.java` (sin `StatusAuditPort`) |
| L25 | Guarda TTL en RESERVED | `CU-08/plan.md` L213 vs `CU-08/spec.md` (no la define) |
| L26 | Párrafo repetido | `CU-08/plan.md` L197 (editorial) |
| L27 | Campos de CU-14 | `CU-14/spec.md` L67-L69 (actor, anticipación, novedades) vs `CU-14/plan.md` L40-L47, L124 |
| L28 | Valores de `status` hacia M3 | `CU-14/plan.md` L51-L63 vs `OutboxAdapter.java` L33, L35 |
| L29 | Nombre del evento | `CU-14/spec.md` L77 (`StateTransitionEvent`) vs `technical-plan/plan.md` L342 (`ReservationStatusEvent`) |
| L30 | `CANCELLED_FLEXIBLE` por cobro tardío | `CU-14/spec.md` L31, L66 vs código (ninguna ruta lo genera) |
| L31 | `Message-Id` | `CU-14/plan.md` L35 vs código (sin headers, sin relay) |
| L32 | `m1_blocked` | `CU-08/plan.md` L196 (controla liberaciones) vs código (solo se escribe `false`: `ReservationMapper.java` L54) |

---

## Anexo C. Trampas de JPA y ddl-auto=validate

1. **Tipos de tiempo mezclados**: `Instant` en `ReservationEntity.java` L94-L119, `OutboxMessageEntity.java` L40-L49, `ReservationStatusAuditEntity.java` L50; `OffsetDateTime` en `IdempotencyKeyEntity.java` L21 y `ReservationResponse.java` L49-L52. Verificar cada columna `TIMESTAMPTZ` (V1, V2).
2. **`@EmbeddedId`**: `IdempotencyKeyEntity.java` L14. La clave compuesta debe coincidir con `UNIQUE(key, endpoint)` (`technical-plan/plan.md` L199).
3. **`@Version`**: `ReservationEntity.java` L121, solo en `reservation`. `dispute_guarantee` también requiere `version` (`technical-plan/plan.md` L194).
4. **`LocalTime` y NOT NULL**: `ReservationEntity.java` L60 y V3 L12 (`departure_time SET NOT NULL`). La entidad no admite `null`.
5. **`BigDecimal`**: `ReservationEntity.java` L75-L87 (`NUMERIC(18,4)`). Solo almacenar, nunca operar (`contexto_m2.md` L34).
6. **Rango de fechas**: `ReservationJpaRepository.java` L20 (solapamiento inclusivo, `<=` y `>=`); V1 L53-L56 (`EXCLUDE` con `daterange`, solo `PENDING_PAYMENT`, `RESERVED`, `IN_NAVIGATION`).
7. **Auditoría inmutable**: trigger en V1 L80-L89. Cualquier cambio de esquema va en V4.

---

## Anexo D. Pendientes de lectura y límites de esta versión

- Specs y planes de CU-01, CU-03 a CU-07, CU-09 a CU-13, CU-15 a CU-21: pendientes de lectura (marcados **[PRE-LECTURA]**). Sus criterios son provisionales.
- Tests del lote: leídos solo por nombre de método y línea. Las fichas detalladas de T-06, T-07, T-08, T-09, T-10, T-12 y T-24 de la v2 no se conservan textualmente; su contenido relevante quedó en las tablas de la sección 3 y en los Anexos A y B.
- `features/CU-02-*/plan.event.md` leído en la fase 2.
- `context/sea-share.md`, `context/consistencia-m2-m3.md`, `templates/`, `diagrams/`: no leídos completos.
- `pom.xml`: solo lista de `artifactId`; sin verificar versiones (D-09).
- Plazo: el alcance completo se mantiene por decisión del usuario. El cronograma por hito depende del equipo.
