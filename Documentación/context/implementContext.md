# Contexto de Progreso de Implementación - Módulo 2 (SEA-SHARE)

**Fecha**: 2026-11-07  
**Módulo**: Módulo 2 - Operación de Reservas, Tiempos y Cancelaciones  
**Stack**: Java 21, Spring Boot 4.1.2, Maven, PostgreSQL, Flyway  
**Referencias**: `Documentación/technical-plan/plan.md` y los `plan.md` de cada caso de uso en `Documentación/features/`.

> **Criterio de esta bitácora**: solo se marca como implementado lo que existe en el código y está respaldado por pruebas o por el estado verificable del repositorio. La existencia de una tabla en Flyway, de una propiedad de configuración o de una clase sin uso **no** acredita por sí sola la implementación de un caso de uso.

---

## 1. Resumen del estado actual

- El repositorio implementa **únicamente el contexto `reservas`**. No existen los paquetes `disputa` ni `shared/contracts`.
- Funcionalidad operativa verificable:
  - Creación de reservas (`POST /api/v1/reservations`) con idempotencia y TTL de 15 minutos.
  - Motor interno de cambio de estado (`ChangeReservationStatusUseCase`), con matriz de transiciones, auditoría y escritura en outbox.
  - Consultas básicas de reservas (`GET /api/v1/reservations` y `GET /api/v1/reservations/{id}`).
  - Job programado de expiración por TTL para reservas en `INITIATED` y `PENDING_PAYMENT` (T-07, Hito H4).
  - Manejo global de errores `{ code, message }`, identidad temporal por headers, reloj inyectable y reglas base de ArchUnit.
- **No implementado**:
  - Consumidores AMQP de producción: la deduplicación por `event_id` solo tiene un consumidor de prueba en `src/test` (T-04); M3 es quien deduplica en producción (D-04).
  - Spring Security y Resilience4j (ShedLock sí está implementado desde el 2026-11-12; ver 4.11).
  - Contexto `disputa` y contrato interno `shared/contracts`.
  - Todos los CU salvo los avances parciales de CU-02, CU-08, CU-20 y CU-21. **Hito H4 completado (2026-11-09)**: CU-08 quedó completo como motor interno (comando, respuesta, auditoría completa, TTL doble, matriz de notificaciones M3/M1); **desde el 2026-11-12 la creación pasa por CU-08 (T-24, ver 4.11)**, por lo que ya no queda escritura de estado fuera del motor.
- **Hito H2 completado (2026-11-07)**: topología RabbitMQ (T-02), relay del outbox con publisher confirms y reintentos (T-03), publicación AMQP con `Message-Id` y prueba de deduplicación (T-04), filtro de correlation id HTTP→AMQP (T-22) y **ShedLock en TTL y relay (T-05, 2026-11-12)**. Detalle en la sección 4.6.
- La última ejecución registrada de la suite completa es del **2026-11-12** (`.\mvnw.cmd test`): **363 pruebas, 0 fallos, 0 errores, 0 omitidas** (eran 115 el 2026-11-07, 271 el 2026-11-10 y 332 antes de T-16a/T-16b).

---

## 2. Estado por fase del plan general

| Fase | CUs / alcance | Estado | Evidencia y brecha principal |
| :--- | :--- | :--- | :--- |
| Fase 1 — Setup | Infraestructura base | **Parcial (Hito H1 completado)** | Existen `pom.xml` con Boot 4.1.2/Java 21, `M2Application`, `docker-compose.yml` y `.gitignore`. Añadidos en el Hito H1: `Dockerfile` multi-stage, `README.md`, perfiles `dev/test/prod`, springdoc-openapi 3.2.0 y Checkstyle (reporte sin fallar). Verificado en vivo el 2026-11-09: `docker build` OK (imagen no-root uid 999), `GET /v3/api-docs` 200 con los 3 endpoints, arranque con `--spring.profiles.active=dev`. Siguen faltando Resilience4j y `maven-failsafe-plugin`. |
| Fase 2 — Foundational | Persistencia, errores, reloj, outbox, seguridad, mensajería, testing | **Parcial** | Implementados: errores globales, `ErrorCode`, Flyway V1 a V5, `ClockPort`, repositorio de reservas, idempotencia de entrada, escritura en outbox, reglas base de ArchUnit, Testcontainers y WireMock, **topología RabbitMQ (T-02), relay del outbox con publisher confirms y reintentos (T-03), publicación AMQP con `Message-Id` + deduplicación probada (T-04) y correlation id HTTP→AMQP (T-22)**. Faltan: seguridad, ShedLock, métricas/health y reglas ArchUnit completas. Los clientes HTTP salientes de M1 y M3 ya existen (Hito H3), pero siguen en modo `fake` por defecto. |
| Fase 3 — Adaptadores de lectura | CU-09, CU-10, CU-11, CU-12 | **En curso** | T-14 hecha: clientes HTTP reales de M1 para CU-09 y CU-10 con registro de consultas (ver 4.8); activos con `seashare.fleet.mode=http`, los *fake* siguen por defecto. T-15 hecha: clientes HTTP de M3 para CU-11 y CU-12 (ver 4.9), activos con `seashare.finance.mode=http`. |
| Fase 4 — Núcleo de dominio | CU-08 | **Completada (Hito H4, 2026-11-09)** | Contrato completo de CU-08 (T-06): comando con `actor_id`/`expected_version`/`mechanical_failure`/`anticipation_minutes`/`completion_notes`/`payment_reference`, respuesta con `version` y `events_enqueued`. Persistencia y V4 (T-09): `status_changed_at` y campos de cierre/pago. Auditoría completa (T-10): creación + transiciones con `actor_id`/`event_ids` e inmutabilidad probada. TTL doble (T-07): `INITIATED` y `PENDING_PAYMENT`. Matriz M3/M1 con guarda `m1_blocked` y destinos `AMQP` + `M1_HTTP` (T-08). Detalle en la sección 4.7. Queda fuera de esta fase: creación vía CU-08 (T-24, Fase 7) y payload final de CU-14 (T-12, Fase 5). |
| Fase 5 — Superficie Módulo 3 | CU-14, CU-15 | **Completada (Hito H5, integrada por PR #28)** | T-12: payload de CU-14 según UC07 de M3 (mapeador de estados y token de pago opcional). T-13: endpoint interno de consulta para CU-15, solo lectura; **desde el 2026-11-12 exige rol `FINANCE` y devuelve 403**. Detalle en el historial (entrada del Hito H5) y en 4.11. Los consumidores AMQP de producción son de M3. |
| Fase 6 — Vistas de consulta | CU-19, CU-20, CU-21 | **Completada en código (Hito H6, PR #29)** | CU-19, CU-20 y CU-21 existen. **Desde el 2026-11-12 exponen `status_badge` y el listado usa el mismo total que el detalle (ver 4.11).** |
| Fase 7 — Embudo de conversión | CU-01, CU-02, CU-03, CU-13 | **Parcial** | CU-01 (T-16a), CU-02 (con T-24) y CU-03 (T-16b) implementados el 2026-11-12. CU-13 no existe y sigue bloqueado por la decisión sobre la salida de `EXPIRED`. |
| Fase 8 — Operaciones en muelle | CU-04, CU-05, CU-06, CU-07 | **No iniciada** | Sin endpoints, servicios ni eventos de cancelación, inasistencia, check-in o check-out. |
| Fase 9 — Disputa de garantía | CU-16, CU-17, CU-18 | **No iniciada** | Las tablas `dispute_*` existen en V2, pero no hay contexto `disputa`, lógica, endpoints ni publicación de eventos. |
| Fase 10 — Polish | Transversal | **No iniciada** | Sin pruebas de carga, resiliencia, observabilidad operativa, cierre de `[NEEDS CLARIFICATION]` ni documentación de arranque. |

---

## 3. Estado por caso de uso

| CU | Estado | Detalle verificado |
| :--- | :--- | :--- |
| CU-01 — Buscar embarcaciones disponibles | **Implementado (Hito H7, T-16a)** | `GET /api/v1/vessels` con `start_date`, `end_date`, `passengers`, `type`, `q` y `page`; pagina de 20, filtros locales por tipo, capacidad y solapamiento, y cotización en lote con degradación controlada si M3 falla. Catálogo de M1 vía `FleetCatalogPort` (fake por defecto). |
| CU-02 — Iniciar reserva | **Implementado (código)** | `POST /api/v1/reservations`; valida fechas, capacidad, zona horaria y solapamiento; usa idempotencia de 60 s y fija TTL de 15 min. La creación pasa por CU-08 (T-24). Depende de adaptadores *fake* de flota por decisión de entorno aislado. |
| CU-03 — Iniciar pago | **Implementado (Hito H7, T-16b)** | `POST /api/v1/reservations/{id}/payments`: exige política aceptada, desglose de M3, disponibilidad de M1, bloqueo pesimista, control FCFS y transición a `PENDING_PAYMENT` sin reiniciar el TTL; persiste el desglose y publica el token en el evento `PENDING`. Reintento con tarjeta queda para CU-13. |
| CU-04 — Solicitar cancelación | **No implementado** | Sin endpoint ni clasificación de cancelación. |
| CU-05 — Marcar inasistencia | **No implementado** | Sin endpoint ni validación de la ventana de 30 min. |
| CU-06 — Marcar inicio de navegación | **No implementado** | Sin endpoint ni registro de check-in. |
| CU-07 — Marcar fin de navegación | **No implementado** | Sin endpoint ni registro de check-out. |
| CU-08 — Actualizar estado de reserva | **Parcial (motor completo desde el Hito H4)** | Motor interno con matriz de transiciones, auditoría inmutable, outbox y `@Version`. Contrato completo (T-06): comando con `expected_version`, `actor_id`, `mechanical_failure`, `anticipation_minutes`, `completion_notes`, `payment_reference` y respuesta con `version` + `events_enqueued` (puerto `ChangeReservationStatusUseCase`). Auditoría de creación y de cada transición con `actor_id`/`event_ids` (T-10). TTL desde `INITIATED` y `PENDING_PAYMENT` (T-07). Notificación a M1 (`M1_HTTP`) con guarda `m1_blocked` y matriz M3/M1 (T-08). **La creación pasa por CU-08 con `InitiateReservationUseCase.initiate` (T-24, 2026-11-12)**: la reserva, la auditoría `null → INITIATED` y `reservation.info.provided` se escriben en la misma transacción con el mismo `event_id`. El payload de los eventos hacia M3 (T-12) se cerró en el Hito H5. |
| CU-09 — Proveer información de embarcación | **Parcial** | Cliente HTTP real (`HttpFleetInfoAdapter`) con timeouts, un reintento y registro de consultas; se activa con `seashare.fleet.mode=http`. Contrato con M1 sin firmar. |
| CU-10 — Brindar información de estado operativo | **Parcial** | Cliente HTTP real (`HttpFleetOperationalStatusAdapter`) con registro de consultas; solo `AVAILABLE` es disponible y una respuesta ilegible da 503. Contrato con M1 sin firmar. |
| CU-11 — Proveer información de cotización | **Parcial** | Clientes HTTP reales de M3 para estimación individual y en lote (`HttpIndividualEstimateAdapter`, `HttpBatchEstimateAdapter`) con puertos, fakes y pruebas WireMock; se activan con `seashare.finance.mode=http`. Falta `base-url` de M3 y que algún caso de uso los llame (CU-01 usará el lote en H7). |
| CU-12 — Brindar cálculo total de la reserva | **Parcial** | Cliente HTTP real (`HttpCalculatedValueAdapter`) con reintento del 404 y tope total de 800 ms; se activa con `seashare.finance.mode=http`. Falta `base-url` de M3, la auditoría de CU-12 (FR-010) y que CU-03 lo llame (H7). |
| CU-13 — Confirmar pago | **No implementado** | Sin canal B activo ni canal A. |
| CU-14 — Recibir estado de reserva | **Parcial** | El relay publica las filas `AMQP` con `Message-Id` y publisher confirm (T-03/T-04) hacia `finance.reservation-status.v1`. Desde el Hito H4 el `StatusChangedEvent` transporta `actor`, `anticipation_minutes` y `completion_notes` listos para el payload. Hito H5 (T-12): el payload sigue el contrato UC07 de M3 con `reservation_id`, `status` (`PENDING`, `RESERVED`, `IN_NAVIGATION`, `COMPLETED`, `CANCELLED_FLEXIBLE`, `CANCELLED_MODERATE`, `CANCELLED_STRICT`, `CANCELLED_BY_HOST`), `status_changed_at` y `payment_token_ref` solo en `PENDING`. **CU-03 ya exige el token (T-16b).** [NEEDS CLARIFICATION] la spec de CU-14 (FR-002 a FR-004) pide actor, sub-estado, anticipación y notas en el payload y el plan no; se siguió el plan. El consumidor real es de M3. |
| CU-15 — Solicitar información de la reserva | **Implementado (Hito H5, T-13; integrado por PR #28)** | `GET /api/v1/internal/reservations/{reservation_id}`, solo lectura y sin dinero. **T-13 cerrado el 2026-11-12**: exige `X-User-Role: FINANCE` y devuelve 403 `PROFILE_NOT_AUTHORIZED` con cualquier otro perfil. [NEEDS CLARIFICATION] `damage_detected` y `cancellation_anticipation_hours` siguen nulos porque CU-07 y CU-04 aún no los persisten; se documenta el motivo y no se inventan valores. `scheduled_arrival` provisional y formato de `reservation_code` sin definir. Detalle en el historial. |
| CU-16 — Generar disputa de garantía | **No implementado** | Solo tablas en Flyway; sin lógica ni API. |
| CU-17 — Actualizar estado de disputa | **No implementado** | Solo tablas en Flyway; sin lógica ni API. |
| CU-18 — Recibir información de disputa | **No implementado** | Sin contexto `disputa` ni publicación AMQP. |
| CU-19 — Ver detalle de embarcación | **Implementado (Hito H6, T-11a)** | `GET /api/v1/vessels/{id}` y `/quotations` con datos de M1 y cotización individual de M3; `Cache-Control: no-store` y sin persistencia. |
| CU-20 — Ver mis reservas | **Completada en código (Hito H6)** | `GET /api/v1/reservations` con paginación y filtro `filter`; `pagination`, `applied_filter` y `status_badge` implementados. `total_price.amount` usa `total_amount` con respaldo en `estimated_total` (misma regla que CU-21, 2026-11-12). |
| CU-21 — Ver detalle de reserva | **Completada en código (Hito H6)** | `GET /api/v1/reservations/{id}` con validación de pertenencia, `status_badge` y datos operativos. Los rubros de `payment_summary` siguen nulos hasta que CU-03/CU-12 los persistan (H7). |
| CU-22 — Recibir información de estado operativo | **Parcial** | El relay despacha el destino `M1_HTTP` al puerto `FleetStatusNotificationPort` con adaptador fake (loguea y responde OK); sin HTTP real. Desde el Hito H4 la matriz de notificaciones (T-08) está implementada en `ModuleNotificationMatrix` siguiendo la tabla FR-002, con `OperationalStatusEvent` encolado a `M1_HTTP` y payload de 4 campos. Falta el cliente HTTP real (D-01). |

---

## 4. Implementación verificable

### 4.1 Fundaciones y configuración

- Proyecto Spring Boot 4.1.2 con Java 21 y Maven. Dependencias presentes: Web MVC, Validation, Data JPA, AMQP, Actuator, Flyway (`spring-boot-starter-flyway` + `flyway-database-postgresql`), PostgreSQL y ArchUnit para pruebas.
- Flyway administra el esquema con `spring.jpa.hibernate.ddl-auto: validate`. Existen las migraciones **V1 a V5**.
  - V1 crea `reservation`, `reservation_status_audit` y `outbox_message`, con `btree_gist`, restricción `EXCLUDE` de solapamiento e índice `ix_reservation_ttl`.
  - V2 crea tablas de funcionalidades aún no implementadas (`payment_attempt`, eventos de cancelación/inasistencia/check-in/check-out, `external_query_log`, `dispute_*`, `operational_failure`, `idempotency_key`, `shedlock`). **Estas tablas son estructura de datos, no funcionalidad implementada.**
  - V3 agrega campos de reserva y columnas de control del outbox.
  - V4 agrega `reservation.status_changed_at` y, en `reservation_status_audit`, `actor_id` y `event_ids`.
  - V5 agrega `returned_status` a `external_query_log`.
- `docker-compose.yml` define PostgreSQL en `localhost:5433` y RabbitMQ en `5672`/`15672`.
- Manejo global de errores con cuerpo `{ code, message }` y catálogo `ErrorCode`, incluyendo la traducción de errores internos a códigos públicos.
- Identidad temporal por headers `X-User-Id` y `X-User-Role`; no hay Spring Security.
- `ClockPort` como fuente única de tiempo en los servicios de aplicación.
- Reglas base de arquitectura por capas con ArchUnit.

### 4.2 Reservas: CU-02, CU-08, CU-20 y CU-21

- **CU-02**: `ReservationController`, `CreateReservationService`, `CreateReservationCommand`, `ReservationView`.
  - Valida rango de fechas, zona horaria del puerto, disponibilidad operativa, capacidad y solapamiento con estados bloqueantes.
  - Reclama la clave de idempotencia y devuelve la reserva original si la clave ya fue usada dentro de la ventana.
  - Crea la reserva en `INITIATED` y fija `ttl_expires_at` con `TtlPolicy`.
- **CU-08**: `ReservationStateMachine`, `ChangeReservationStatusService`, `StatusAuditPort` y `OutboxPort`.
  - Matriz de transiciones: `INITIATED → PENDING_PAYMENT | EXPIRED`; `PENDING_PAYMENT → RESERVED | EXPIRED | PAYMENT_FAILED`; `RESERVED → IN_NAVIGATION | CANCELLED`; `IN_NAVIGATION → COMPLETED`; estados finales sin salida.
  - Persiste el cambio con *optimistic locking* (`version`), registra auditoría y encola un evento en outbox para los estados configurados.
  - El mapeo de persistencia en `ReservationMapper.applyChanges` solo actualiza `status` y `cancellationSubtype`; los campos de CU-08 como horas reales, notas de finalización o referencia de pago no se persisten.
- **CU-20 / CU-21**: `ReservationQueryController` y `ReservationQueryService`.
  - Listado paginado por arrendatario o propietario, con límites `page >= 1` y `1 <= limit <= 50`.
  - Detalle con validación de existencia y pertenencia según rol.
  - Los contratos son básicos y no reproducen todos los campos ni envoltorios definidos en los planes de CU-20 y CU-21.

### 4.3 Scheduler de expiración por TTL (Parte B)

- `ExpireReservationsUseCase` y `ExpireReservationsService`:
  - Obtienen la hora con `ClockPort`.
  - Consultan IDs de reservas `INITIATED` con `ttl_expires_at <= now`, ordenadas por vencimiento y limitadas por lote.
  - Invocan `ChangeReservationStatusUseCase` con destino `EXPIRED`, actor `SYSTEM` y motivo de expiración.
  - Omiten sin error `VersionConflictException` e `InvalidTransitionException`; registran y continúan ante otras excepciones.
- `TtlExpiryJob` delega en el caso de uso; `SchedulerConfig` habilita el *scheduling* según `m2.scheduler.ttl.enabled`.
- Puerto y persistencia:
  - `ReservationRepositoryPort.findInitiatedExpiredBefore(Instant now, int batchSize)`.
  - Consulta JPQL en `ReservationJpaRepository` que aprovecha `ix_reservation_ttl`.
  - Implementación paginada en `ReservationRepositoryAdapter`.
- Configuración en `application.yaml`:
  - `m2.scheduler.ttl.enabled: true`
  - `m2.scheduler.ttl.fixed-delay-ms: 30000`
  - `m2.scheduler.ttl.batch-size: 100`
- Pruebas añadidas:
  - `ExpireReservationsServiceTest` (9 pruebas unitarias).
  - `TtlExpiryJobTest` (1 prueba unitaria).
  - `TtlExpiryIntegrationTest` (4 pruebas de integración con PostgreSQL real).

### 4.4 Idempotencia de creación (Parte A)

Los tests de idempotencia se conservaron y verificaron tras la migración de `IdempotencyAdapter` a Spring Data JPA:

- `IdempotencyAdapterTest`:
  - Clave nueva reclamada y misma clave rechazada dentro de la ventana.
  - Clave vencida reclamable de nuevo.
  - `complete` sobre una clave no reclamada falla con `IllegalStateException`.
- `IdempotencyClaimConcurrencyTest`:
  - Dos hilos compiten con `CountDownLatch` y commits reales mediante `TransactionTemplate`; exactamente uno obtiene la clave.
- La idempotencia implementada es **solo de entrada**; no existe deduplicación de eventos salientes en un relay.

### 4.5 Encolado de reservation.info.provided en Outbox (CU-02) (Parte C)

- **Objetivo**: Garantizar que al crearse una reserva en estado `INITIATED` mediante `CreateReservationService` (CU-02), se encole de forma transaccional el evento `reservation.info.provided` en la tabla `outbox_message` para su posterior publicación a Módulo 3.
- **Encolado en la Transacción de Creación**:
  - En `CreateReservationService.java`:
    - `@Transactional` a nivel de clase.
    - Persiste la reserva formalmente mediante `reservationRepositoryPort.create(...)` en estado inicial `INITIATED`.
    - Calcula el `eventId` determinista (`UUID.nameUUIDFromBytes(("reservation.info.provided:" + created.getId()).getBytes(StandardCharsets.UTF_8))`).
    - Encola el evento: `outboxPort.enqueue(new ReservationInfoProvidedEvent(...))`.
    - Asocia la reserva a la clave reclamada invocando `idempotencyPort.complete(normalizedKey, IDEMPOTENCY_ENDPOINT, created.getId())`.
  - El payload del evento contiene exactamente los 7 campos requeridos por Módulo 3 (`reservation_id`, `boat_id`, `start_date`, `end_date`, `passengers`, `owner_id`, `max_capacity`).
  - El nombre real del campo en el record es `vesselId` (`ReservationInfoProvidedEvent.java`), traducido a `boat_id` por `OutboxAdapter.java`.
- **Infraestructura de Outbox**:
  - Puerto `OutboxPort`, adaptador `OutboxAdapter` (serializa a JSON snake_case con Jackson 3 y construye `OutboxMessageEntity` con exchange `seashare.reservations`, routing key `reservation.info.provided`, destino `AMQP`), persistido con `OutboxMessageJpaRepository`.
- **Corrección de Persistencia en Idempotencia**:
  - En `IdempotencyKeyJpaRepository.java`: el método `complete(...)` se anotó con `@Modifying(flushAutomatically = true, clearAutomatically = true)` para evitar que `clear()` descarte las entidades pendientes del outbox antes del flush.
- **Estado del Relay de Mensajería**:
  - ~~El relay del outbox a RabbitMQ no existe todavía~~ **Actualizado (Hito H2, 2026-11-07)**: el relay existe y publica; ver sección 4.6.

### 4.6 Infraestructura de mensajería y observabilidad (Hito H2)

Ejecutado en cuatro tareas con un commit por tarea (ver historial, sección 7).

**T-02 — Topología RabbitMQ**
- `MessagingProperties` (`@ConfigurationProperties` `m2.messaging`): exchange `seashare.reservations`, DLX `seashare.reservations.dlx`, routing keys `reservation.status.changed` / `reservation.info.provided`, colas `finance.reservation-status.v1` / `finance.reservation-info.v1` y sus DLQs (`<cola>.dlq`). `OutboxAdapter` lee los mismos nombres para no divergir.
- `RabbitTopologyConfig`: bean `Declarables` con exchange topic durable, DLX directo,4 colas (las dos de M3 con argumentos `x-dead-letter-exchange`/`x-dead-letter-routing-key`) y4 bindings; `RabbitAdmin` los declara al abrir la primera conexión.
- `application.yaml` ya tenía host/credenciales de RabbitMQ (prod usa variables de entorno `SEASHARE_RABBIT_*`, sin secretos); `docker-compose.yml` traía `rabbitmq:3-management` en5672/15672 sin cambios.
- `pom.xml`: + `org.testcontainers:testcontainers-rabbitmq` **2.0.5** (versión gestionada por el `testcontainers-bom`2.0.5 ya importado; D-09 verificado en Maven Central).
- Pruebas: `RabbitTestContainers` (contenedor `rabbitmq:3-management` compartido por JVM, patrón de `PostgresTestContainers`) y `RabbitTopologyTest` (5 pruebas: exchange topic+DLX,4 colas con argumentos, bindings por routing key y DLQs ligadas al DLX, vía API de management).

**T-03 — Relay del outbox**
- Puerto de entrada: `PublishPendingOutboxUseCase` → `PublishPendingOutboxService`.
- `OutboxPort` ampliado con3 métodos: `findPending(Instant now, int batchSize)`, `markPublished(UUID messageId, Instant publishedAt)` (incluye la purga condicional de `payment_token_ref` → `{}` por D-09, sin cambio de esquema) y `markFailed(UUID messageId, int attempts, Instant nextAttemptAt, String lastError, boolean dead)`.
- Puertos nuevos de salida: `EventPublisherPort` (publicación AMQP con confirm; lanza `EventPublishException` sin ack) y `FleetStatusNotificationPort` con `FleetNotificationOutcome` (`DELIVERED`/`RETRYABLE_FAILURE`/`PERMANENT_FAILURE`).
- Nuevo DTO público `PendingOutboxMessage` (la entidad outbox es *package-private* en infraestructura).
- `PublishPendingOutboxService`: lee pendientes (`published_at IS NULL AND dead = false AND next_attempt_at <= now`, orden `next_attempt_at, created_at`), transacción por mensaje con `TransactionTemplate` (aislamiento), despacho por destino (`AMQP` → publica y marca tras confirm; `M1_HTTP` → llama al puerto, nunca HTTP), backoff1/5/25/125 s con jitter ±10 % (`BackoffPolicy`, `Random` inyectable), máximo5 intentos → `dead=true` + log de alerta, M1 4xx → dead directo (D-05),5xx/timeout → reintento. `ClockPort` siempre; nunca `Instant.now()`. No propaga excepciones.
- `BackoffPolicy`: componente con `@Value` (`max-attempts`, `backoff-initial-ms`, `backoff-multiplier`, `jitter-ratio`) y constructor de paquete con `Random` fijo para pruebas exactas.
- `OutboxRelayJob` (`adapter/in/scheduler`): `@Scheduled(fixedDelayString=m2.scheduler.outbox.fixed-delay-ms)`, `@ConditionalOnProperty(m2.scheduler.outbox.enabled)`, solo delega y atrapa todo. `SchedulerConfig` ahora habilita scheduling si `ttl` **o** `outbox` están activos (`@ConditionalOnExpression`).
- Adaptadores: `RabbitEventPublisherAdapter` (RabbitTemplate + `publisher-confirm-type: correlated` + `CorrelationData`, espera de confirm con timeout `m2.messaging.publish-timeout-ms`, `Message-Id` = `event_id`, `Content-Type` JSON, mensaje persistente) y `FakeFleetStatusNotificationAdapter` en `adapter/out/fleet/` (log + `DELIVERED`; el cliente HTTP real cambiará solo este adaptador).
- Configuración (`application.yaml`): `m2.scheduler.outbox.enabled: false` por defecto (evita que la suite publique sin querer; `true` en `application-dev.yaml` y `application-prod.yaml`), `fixed-delay-ms:5000`, `batch-size:50`, `max-attempts:5`, `backoff-initial-ms:1000`, `backoff-multiplier:5`, `jitter-ratio:0.1`; `spring.rabbitmq.publisher-confirm-type: correlated`.
- Pruebas: `BackoffPolicyTest` (3), `PublishPendingOutboxServiceTest` (10: backoff exacto,5º→dead,4xx→dead, reintentos M1, aislamiento, lote, destino desconocido), `OutboxRelayJobTest` (2), `OutboxAdapterTest` (+2 de purga D-09) e `OutboxRelayIntegrationTest` (3, con RabbitMQ real y Awaitility: publica+`published_at`, fallo simulado→reintento, M1_HTTP vía puerto fake).

**T-04 — Message-Id y deduplicación (versión mínima)**
- El publicador ya envía `Message-Id` = `event_id` (T-03). Sin consumidor de producción: `EventDeduplicationTest` registra un consumidor **solo de prueba** en `src/test` que descarta `event_id` ya vistos y publica el mismo evento dos veces → procesado una sola vez; asimismo verifica `Message-Id`, `Content-Type` y persistencia.

**T-22 — Correlation id (solo eso)**
- `shared/infrastructure/adapter/in/web/CorrelationIdFilter` (`@Component`, `@Order(HIGHEST_PRECEDENCE)`): lee/genera `X-Correlation-Id`, lo pone en MDC (`correlationId`), lo devuelve en la respuesta y lo limpia en `finally`.
- `RabbitEventPublisherAdapter` propaga el valor de MDC como header AMQP `X-Correlation-Id` **solo si hay contexto HTTP**; el relay corre sin MDC y no añade el header (sin cambio de esquema para guardarlo en outbox).
- Pruebas: `CorrelationIdFilterTest` (2: respeta el id entrante, genera UUID) y `RabbitEventPublisherAdapterTest` (3: headers + propagación desde MDC, sin MDC no hay header, nack → `EventPublishException`).
- Fuera de alcance de T-22 (siguen pendientes): métricas, health checks y la regla ArchUnit "sin dinero".

### 4.7 Núcleo de estados CU-08 — Hito H4 (2026-11-09)

Rama `feature/h4-nucleoDom` (nombre distinto al del enunciado, `feature/h4-nucleo-estados`; creada desde develop con H1+H2 integrados, decisión del usuario). Cinco commits, uno por tarea, más el commit de docs. Relay de H2 **existía** al empezar: T-08 solo encola `M1_HTTP` y el relay ya lo despacha al puerto `FleetStatusNotificationPort` (adaptador fake; el cliente HTTP real queda pendiente, D-01).

**Commits**: `ad015a1` T-06, `5c3482e` T-09, `9f7235c` T-10, `f7de0bd` T-07, `2ea01bb` T-08, más este commit de docs.

**T-06 — Contrato completo de CU-08**
- `ChangeReservationStatusCommand` ampliado con `actorId`, `expectedVersion`, `mechanicalFailure`, `anticipationMinutes`, `completionNotes`, `paymentReference`; conserva el constructor compacto de 5 args (TTL y tests existentes siguen compilando).
- Nuevo `ChangeReservationStatusResult` (`reservationId`, `status`, `cancellationSubtype`, `statusChangedAt`, `ttlExpiresAt`, `version`, `eventsEnqueued`); `ChangeReservationStatusUseCase.execute` ahora lo retorna (los llamadores que lo ignoran no cambian).
- Validaciones nuevas: `actor_id` obligatorio si actor ≠ SYSTEM (`INVALID_PARAMETERS`); `expected_version` distinto → `VERSION_CONFLICT`; subtipo obligatorio solo con `CANCELLED` y prohibido fuera; cancelación activa solo desde `RESERVED` con mensaje explicativo (spec L156-L159); `anticipation_minutes` obligatorio al cancelar RENTER y `mechanical_failure` al cancelar OWNER; `completion_notes` solo `COMPLETED` y `payment_reference` solo `RESERVED`; guarda de TTL en `RESERVED` con corte exacto (`now >= ttl` → `RESERVATION_EXPIRED`, plan L60/L213).
- `StatusChangedEvent` gana `actor`, `anticipationMinutes`, `completionNotes` (datos para el payload de T-12; **el payload hacia M3 no cambió**).

**T-09 — Persistencia + V4**
- Migración única `V4__status_changed_at_and_audit_tracking.sql`: `reservation.status_changed_at TIMESTAMPTZ` (backfill `created_at` → NOT NULL); `reservation_status_audit.actor_id UUID` y `reservation_status_audit.event_ids TEXT` (JSON array). Columnas de T-09 y T-10 en la misma V4; no hizo falta nada más (V1 ya tenía `completion_notes`, `payment_reference`, `payment_confirmed_at`, `actual_departure_at/arrival_at`, `m1_blocked`) → sin V5.
- `Reservation` (dominio) + `ReservationEntity` + `ReservationMapper`: `completionNotes`, `paymentReference`, `paymentConfirmedAt`, `statusChangedAt`, `m1Blocked` y round-trip de `actualDepartureAt/arrivalAt`. `applyChanges` solo escribe campos no nulos (no borra columnas existentes).
- Servicio: `RESERVED` fija `paymentConfirmedAt` y `paymentReference` si viene; `COMPLETED` fija `completionNotes`; toda transición fija `statusChangedAt`. Ningún importe se recalcula.
- **Horas reales**: el spec de CU-08 **no** dice que CU-08 las registre (los escenarios 4 y 5 solo piden cambio de estado y novedades; no hay mención de `actual_*`); se aplicó el caso 2 acordado: solo soporte de persistencia (entidad + mapper, columnas de V1), cubierto con test de mapper. Quedan pendientes para T-17c (CU-06, `checkin_event.actual_time`, V2 L52) y T-17d (CU-07, `checkout_event.actual_time`, V2 L60), que las recibirán del Propietario.

**T-10 — Auditoría completa**
- `StatusAuditEntry` + `actorId` + `eventIds`; `StatusAuditAdapter` serializa `event_ids` como JSON array; el servicio genera los `event_id` **antes** de auditar y reutiliza esos mismos ids en el outbox (misma transacción, FR-011).
- `CreateReservationService` (cambio mínimo autorizado): inyecta `StatusAuditPort` y registra la creación (`null → INITIATED`, actor `RENTER`, `actor_id` = renter, `event_ids = [reservation.info.provided]`). **No** se refactorizó CU-08 para la creación (eso es T-24).
- **Excepción documentada a la regla "sin JdbcTemplate en tests"** (autorizada por el usuario): nueva clase `ReservationStatusAuditImmutabilityTest` (`src/test/.../reservas/integration/`). El objeto bajo prueba es el trigger `trg_status_audit_immutable` de V1 (L80-L89) y no existe camino público para mutar la auditoría, por diseño. JdbcTemplate se usa **solo** para intentar `UPDATE` y `DELETE` sobre `reservation_status_audit`, cada intento en su propia transacción (sin `@Transactional` en la clase, porque en PostgreSQL el error del trigger aborta la transacción en curso); ambos fallan con `DataAccessException` y la integridad se verifica con un `UPDATE … WHERE` que exige los valores originales. Los datos se preparan solo por vías públicas (`CreateReservationUseCase` + `ChangeReservationStatusUseCase`). No se añadieron métodos a `StatusAuditPort` ni se abrió visibilidad de entidades/repos. ArchUnit sigue en verde.
- Efecto lateral: las reservas creadas ya no se pueden borrar (FK a `reservation_status_audit` + trigger inmutable). Dos tests existentes dejaron de borrar reservas en su limpieza (`CreateReservationValidationTest.tearDown`, `CreateReservationOutboxIntegrationTest.cleanUp`; siguen borrando outbox y claves de idempotencia). El registro de esa deuda queda aquí.

**T-07 — TTL desde PENDING_PAYMENT**
- Conflicto L7 resuelto con el spec de CU-08 (escenario 7, L80-L83: "Iniciada **o** Pendiente de Pago … Iniciada → Expirada y Pendiente de Pago → Expirada"): expiran **ambas**. JPQL ahora `status IN ('INITIATED','PENDING_PAYMENT')` con `ttl_expires_at <= now` (corte exacto); método renombrado `findExpiredBefore` en puerto, adaptador y servicio; motivo de expiración actualizado.
- Tests con reloj fijo: `expiresPendingPaymentPastTtl` (en `now` exacto expira, sin outbox), `pendingPaymentBeforeTtlIsNotExpired` (1 s antes no).
- **Sin cambios**: intervalo del job (30 s en `TtlExpiryJob` vs 5 s + `FOR UPDATE SKIP LOCKED` del plan, Anexo B L21) y ausencia de ShedLock (T-05 pendiente).

**T-08 — Matriz de notificaciones**
- Nuevo `application/service/ModuleNotificationMatrix` (puro): decide M3 (CU-14 FR-001: `PENDING_PAYMENT`, `RESERVED`, `IN_NAVIGATION`, `COMPLETED`, `CANCELLED`; no `INITIATED`/`EXPIRED`/`PAYMENT_FAILED` — D-03) y M1 según **CU-22 FR-002** (fuente de verdad): `RESERVED→RESERVED`, `IN_NAVIGATION`, `COMPLETED→MAINTENANCE_CLEANING`, `CANCELLED` condicional a `m1_blocked` → `AVAILABLE` o `MAINTENANCE_CLEANING` si `mechanical_failure`; nunca `INITIATED`/`PENDING_PAYMENT`/`EXPIRED`/`PAYMENT_FAILED`.
- Nuevos `OperationalStatusEvent` (nombre del spec CU-22 L143) y enum `OperationalStatus` (`AVAILABLE`, `RESERVED`, `IN_NAVIGATION`, `MAINTENANCE_CLEANING`, consistencia-m2-m1 L73-L76); `OutboxPort`/`OutboxAdapter.enqueue(OperationalStatusEvent)` → destino `M1_HTTP`, sin exchange/routing key, payload de exactamente 4 campos (`vessel_id`, `operational_status`, `occurred_at`, `event_id`), sin montos ni estado crudo.
- Guarda `m1_blocked` (plan L196): `Reservation.m1Blocked` en dominio/mapper; el servicio la pone en `true` al encolar `RESERVED` hacia M1; las liberaciones solo se encolan si estaba en `true` (SC-003).
- **D-03**: `PENDING_PAYMENT` sigue publicándose a M3 (cubierto con test). **Reporte**: el payload hoy lleva `"PENDING_PAYMENT"` (nombre interno), no `"PENDING"`; la traducción es T-12 (Anexo B L28), no se tocó el payload. L214 no contradice D-03 (habla de `EXPIRED`/`PAYMENT_FAILED`, que ya no se encolan).

**Pruebas y verificación**: baseline 115/0 (hubo que arrancar Docker Desktop y `docker compose up -d` para RabbitMQ de `CorrelationIdFilterTest`); verde tras cada tarea: 128/0 (T-06), 133/0 (T-09), 137/0 (T-10), 139/0 (T-07), **152/0 (T-08)**. Tests nuevos: escenarios 3-6 y 8 de CU-08, rechazo de cancelación desde `PENDING_PAYMENT`, `expected_version`, campos restringidos, guarda TTL (borde exacto), auditoría de creación, inmutabilidad del trigger, expiración desde `PENDING_PAYMENT`, matriz M1 y payload `M1_HTTP`.

**Estado de CU-08 y CU-22 tras H4**: CU-08 completo como motor interno (comando, respuesta, validaciones, auditoría con `actor_id`/`event_ids`, outbox AMQP + `M1_HTTP`, TTL doble, guarda `m1_blocked`); sigue **sin** creación vía CU-08 (T-24) y sin endpoint (no aplica). CU-22 queda con matriz de notificación y encolado implementados; falta el cliente HTTP real de M1 (adaptador `FakeFleetStatusNotificationAdapter` sigue por defecto, D-01).

**Pendientes tras H4**: T-24 (creación vía CU-08 + `Location`), T-12 (payload CU-14: `PENDING` vs `PENDING_PAYMENT`, `actor`/anticipación/novedades — `StatusChangedEvent` ya los transporta), adaptador HTTP real de M1, T-05 (ShedLock), `MoneyArchitectureTest`. Mención `ApplyReservationTransitionUseCase` corregida en `CU-08/plan.md` L26 (y `TransitionCommand` → `ChangeReservationStatusCommand` L36); las menciones en `implementContext.md` L60/L230 y `plan-trabajo-backend.md` L92 quedan fuera de esta sección por propiedad de frentes.

### 4.8 Clientes HTTP de M1 y registro de consultas: Hito H3 (2026-11-09)

Rama `feature/fase3-adaptadores-lectura` (creada desde develop; se le fusionó develop con H4 antes del registro de consultas). T-14 completa: clientes HTTP reales de M1 para CU-09 y CU-10 y registro de consultas (CU-09 FR-010, CU-10 FR-009).

**Commits**: `6665507`, `0792fd3`, `912e961`, `7778a7b` (adaptadores, configuracion y pruebas WireMock); `d685305`, `9c79976`, `1cb39e2`, `8cd8a11`, `a289741`, `9d47663` (registro de consultas); `48bb289` (CU-10 lanza 503 ante respuesta ilegible).

**Clientes HTTP (CU-09 y CU-10)**
- `RestClient` con `SimpleClientHttpRequestFactory`: connect timeout 100 ms y read timeout 300 ms. Un solo reintento inmediato ante timeout, IOException o 503; nunca ante 4xx. Sin cache ni valores por defecto.
- Errores: 404 `VESSEL_NOT_FOUND`; 400 `INVALID_UUID`; 401 o 403 `INTERNAL_ERROR`; timeout, conexion o 5xx `FLEET_SERVICE_UNAVAILABLE`; `port.timezone` ausente o invalida `HARBOR_TIMEZONE_UNAVAILABLE`.
- Los adaptadores falsos siguen por defecto (`seashare.fleet.mode=fake`); los HTTP se activan con `seashare.fleet.mode=http`.
- Decision tomada el 2026-11-09: en CU-10, cuerpo vacio, JSON ilegible o `operational_status` ausente lanzan `FLEET_SERVICE_UNAVAILABLE` (503), igual que CU-09. Solo un valor desconocido (por ejemplo `WEIRD_STATE`) da `false`, con fila `OK` y el valor crudo. Esto reemplaza la regla anterior de que "ausente es false".

**Registro de consultas**
- Migracion `V5__external_query_log_returned_status.sql`: agrega `returned_status VARCHAR(30)` a `external_query_log` (la tabla viene de V2).
- Puerto `ExternalQueryLogPort`, adaptador JPA con `Propagation.REQUIRES_NEW` (la fila sobrevive al rollback de la reserva) y una fila por consulta con el resultado final: `OK`, `TIMEOUT` o `ERROR`. Si guardar el registro falla, solo se escribe un aviso en el log y la consulta a M1 no cambia.

**Limites conocidos y dudas abiertas**
- `REQUIRES_NEW` abre una segunda conexion mientras `CreateReservationService` (que es `@Transactional`) mantiene la suya. Bajo concurrencia alta con el pool por defecto, el registro puede perderse por falta de conexiones. La solucion de fondo es sacar las llamadas a M1 de la transaccion de CU-02; no se hizo en este hito.
- Un 503 persistente tras el reintento se registra como `TIMEOUT`, aunque M1 si respondio. Nombre no exacto; duda para el grupo.
- Los metodos `get`, `exchange` y `register` estan duplicados en los dos adaptadores HTTP. Refactor pendiente.
- Contrato con M1 no firmado: rutas, header `X-Internal-Service-Token`, valores de `operational_status` y esquema de errores siguen por confirmar.

---

### 4.9 Clientes HTTP de M3: estimaciones y valor calculado, Hito H3 (2026-11-09)

Misma rama `feature/fase3-adaptadores-lectura`. T-15 completa: clientes HTTP de M3 para CU-11 (estimacion individual y en lote) y CU-12 (valor calculado de la reserva). Contratos de M3 usados: UC01 individual, UC01 lote y UC04.

**Commits**: `f30b5dc`, `4ab85ec`, `f5cbb5d`, `4330d7d` (CU-11); `af81673`, `3877e16`, `5518aa7`, `107a238` (CU-12).

**Estructura**
- Puertos `IndividualEstimatePort`, `BatchEstimatePort` y `CalculatedValuePort`; adaptadores `Http...` y `Fake...` en `infrastructure/adapter/out/finance`.
- `seashare.finance.mode`: `fake` por defecto, `http` activa los clientes reales. Tres `RestClient` con su `@Qualifier` y sin header `Authorization` (el README de contratos de M3 dice que su servicio no exige autenticacion).
- Timeouts: individual connect 100 ms y read 1000 ms; lote connect 150 ms y read 1000 ms; valor calculado connect 100 ms y read 1000 ms.

**Reglas por caso de uso**
- Individual: 400 `INVALID_PARAMETERS`; 401 o 403 `INTERNAL_ERROR`; el resto de fallos, timeout, cuerpo vacio o ilegible y precio invalido `QUOTATION_UNAVAILABLE`. Un reintento solo ante `ConnectException`, nunca ante timeout.
- Lote: bloques de 50 en secuencia y sin reintentos. Un bloque que falla deja sus ids como no disponibles y los demas siguen. Un id que M3 no devuelve o con total invalido tambien queda no disponible.
- Valor calculado: 400, 401 y 403 `INTERNAL_ERROR`; 404 se reintenta hasta 3 veces cada 300 ms con tope total de 800 ms medido con `ClockPort` (en la practica son 2 reintentos); el resto de fallos `CALCULATION_UNAVAILABLE`. Mismo reintento por socket que el individual.
- Dinero opaco: `BigDecimal` sin sumas ni redondeos. Decision 2026-11-09: en el valor calculado, alquiler y total se rechazan si son cero o negativos; seguro y deposito se rechazan solo si son negativos.

**Pruebas**: 25 de CU-11 y 23 de CU-12 con WireMock; suite completa en 234 pruebas, 0 fallos.

**Limites conocidos y dudas abiertas (no bloqueantes, sin confirmar con M3)**
- [NEEDS CLARIFICATION] `seashare.finance.base-url` no esta definida (puerto de M3 desconocido); hace falta antes de usar `mode=http`.
- Timeouts y SLA (800 ms individual y valor calculado, 1500 ms lote) son valores provisionales de M2. El ejemplo de M3 menciona un timeout de 1 s hacia Flota, igual a nuestro read timeout.
- Un evento `reservation.info.provided` duplicado puede invalidar el calculo ya hecho (UC03 hace upsert y el `Message-Id` es fijo por reserva). El riesgo se acepta; volver a pedir CU-12 recalcula.
- La auditoria de CU-12 (FR-010) no esta implementada: no se reutilizo `ExternalQueryLogPort`.
- Ningún caso de uso llama aun a los puertos nuevos: ~~CU-01 usara el lote y CU-03 el calculo (H7)~~ **Resuelto el 2026-11-12**: CU-01 usa `BatchEstimatePort` (T-16a) y CU-03 usa `CalculatedValuePort` (T-16b); quien consume la estimación individual sigue por definir.
- Metodos `post` y `exchange` duplicados entre los adaptadores de M3 y los de M1. Refactor pendiente.
- Huecos de pruebas: body del individual, ausencia de `Authorization` en individual y lote, y `X-Correlation-Id` del valor calculado.

---

### 4.10 Vistas de consulta CU-19, CU-20 y CU-21 — Hito H6 (2026-11-10)

Rama `feature/h6-vistas-consulta` (creada desde `develop`). Trabajo en paralelo con H5 (T-12/T-13); no se tocaron `OutboxAdapter`, `FinanceStatusMapper` ni `InternalReservationController` (este último aún no existe en `develop`).

**Decisiones cerradas con el usuario**

| ID | Decisión | Efecto |
|---|---|---|
| D1 | Reemplazar el cuerpo de `GET /api/v1/reservations` por el contrato de CU-20 | Nuevo envoltorio `pagination`, `applied_filter`, `user_role`, `view_title`, `reservations[]`; se actualizó `ReservationQueryTest` |
| D2 | Nuevo `ReservationDetailResponse` para CU-21 | `ReservationResponse` queda intacta para el `POST` de CU-02 |
| D3 | Puertos/adaptadores nuevos de solo lectura de flota | No se modificaron `FleetInfoPort`, `FleetInfo` ni los adaptadores HTTP de H3 |
| D4 | `total_days` calculado desde fechas; `quotation_id`/`expires_at` nulos | Sin tocar `HttpIndividualEstimateAdapter`; no se inventan identificadores |
| D5 | Fail-safe por elemento en listas | Si M1 falla, la lista responde 200 con datos de embarcación nulos |
| D6 | `payment_summary` solo con lo persistido | Sin sumas; rubros no persistidos salen `null` |
| D7 | `start_at`/`end_at` y `scheduled_arrival` con `departure_time` | Se documenta la falta de hora de llegada |
| D8 | `previous_trips` = `COUNT` de reservas `COMPLETED` del arrendatario en M2 | Excluye la reserva consultada; no es cálculo monetario |
| D9 | `renter.name = holder_name` | Documentado como provisional |
| D10 | No devolver `status_badge` | El front mapea textos/colores desde `status` y `cancellation_subtype`; desviación consciente de FR-008 (CU-20) y FR-015 (CU-21) |

**T-11b — CU-20 (commit `0358009`)**

- `ReservationQueryController.listReservations` acepta `filter` (`all`, `history`, `cancelled`) y devuelve `ReservationListResponse`.
- `ReservationListFilter` traduce el filtro a estados: `history` = `INITIATED`, `PENDING_PAYMENT`, `RESERVED`, `IN_NAVIGATION`, `COMPLETED`; `cancelled` = `CANCELLED`, `EXPIRED`, `PAYMENT_FAILED`.
- `ReservationRepositoryPort`/`ReservationRepositoryAdapter`/`ReservationJpaRepository` añaden consultas paginadas por estado, manteniendo las anteriores para compatibilidad.
- `ReservationListItemResponse` expone: `reservation_id`, `reservation_code`, `status`, `cancellation_subtype`, `vessel` (id, nombre, imagen, puerto), `dates` (inicio, fin, días, noches), `passengers`, `total_price` (monto persistido, moneda, `is_frozen`), `renter`, `owner`, `contextual_label`, `detail_url`, `created_at`.
- `ReservationMapper.toDomain` ahora mapea `reservation_code`; `ReservationView` incorpora código, partes y subestado.
- Flota: nuevo `VesselReadPort` con `getSummaries(ids)`; `HttpVesselReadAdapter` consulta en paralelo (4 hilos, presupuesto 700 ms), deduplica y omite elementos fallidos; `FakeVesselReadAdapter` cubre desarrollo y tests. Sin caché entre peticiones ni copia de datos de M1.
- `contextual_label` solo para `RENTER`: textos cualitativos por subestado de cancelación y "Reembolso gestionado por Módulo 3" en `COMPLETED`; no se afirma "sin disputa" porque H9 no existe.
- Paginación: `page >= 1`, `1 <= limit <= 50`, `limit` por defecto 10, orden `created_at DESC`, `total_pages` calculado sin montos.
- Tests: `ReservationQueryTest` (9) y `ReservationQueryServiceTest` (6).

**T-11c — CU-21 (commit `844a089`)**

- `ReservationQueryController.getReservation` devuelve `ReservationDetailResponse`; el método básico `getReservationDetail` se conserva para no romper a H5.
- `ReservationQueryService.getReservationDetailView` compone: ficha de embarcación, itinerario, partes, `payment_summary`, `available_actions`, `time_indicators` y `contextual_banner`.
- `ReservationDetailPolicy` (puro) implementa:
  - `RESERVED`/`IN_NAVIGATION`: tres rubros persistidos y total liquidado (o estimado literal si `total_amount` es nulo).
  - `COMPLETED`: oculta alquiler y seguro, mantiene depósito y nota de ventana de 24 h.
  - `CANCELLED(NO_SHOW)`: título "Compensación", rubros nulos y nota exacta de FR-014.
  - Acciones: `can_cancel` en `RESERVED`; `can_initiate_payment` en `INITIATED` para arrendatario; `can_mark_trip_start`/`can_mark_no_show` para propietario desde la hora de zarpe y tras 30 min; `can_mark_trip_end` en `IN_NAVIGATION`.
  - Contadores: TTL, minutos al zarpe, cortesía y ventana de disputa (24 h desde llegada real o último cambio de estado).
- `ReservationMapper`/`Reservation`/`ReservationView` exponen `rental_amount`, `insurance_amount`, `guarantee_deposit_amount`, `total_amount`, horas reales y `status_changed_at` (solo lectura).
- `previous_trips` (D8): `countCompletedByRenterExcluding`, excluyendo la reserva consultada.
- Flota: `VesselReadPort.getDetail` con parseo completo del contrato CU-09; si M1 falla, el detalle degrada a resumen o ficha mínima sin romper la vista.
- Tests: `ReservationDetailPolicyTest` (6), `ReservationQueryServiceTest` (9), `ReservationQueryTest` (12).

**T-11a — CU-19 (commit `388591a`)**

- `VesselDetailController`:
  - `GET /api/v1/vessels/{vessel_id}`: ficha técnica completa desde M1, con `Cache-Control: no-store` y sin persistencia.
  - `GET /api/v1/vessels/{vessel_id}/quotations?start_date&end_date&passengers`: revalida capacidad contra `FleetInfoPort` (409 `CAPACITY_EXCEEDED`) y cotiza con `IndividualEstimatePort`; monto y `warning` literales de M3, moneda COP constante.
- `VesselDetailService` (caso de uso `VesselDetailUseCase`) restringe el acceso a `RENTER`, valida UUID/fechas/pasajeros y conserva los códigos `VESSEL_NOT_FOUND` y `QUOTATION_UNAVAILABLE` de H3.
- `total_days` se calcula como días inclusivos (coherente con `duration_days` de M3); `quotation_id` y `quotation_expires_at` se devuelven `null` porque M3 no los entrega (D4).
- `VesselDetail`/`VesselDetailResponse` amplían la ficha con capacidad, eslora, camarotes, capitán, coordenadas y verificación del propietario.
- Tests: `VesselDetailServiceTest` (8), `HttpVesselReadAdapterTest` (3, WireMock) y `VesselDetailControllerTest` (7, MockMvc con adaptadores fake).
- **Suite completa del hito (2026-11-10)**: `.\mvnw.cmd test` → **273 pruebas, 0 fallos, 0 errores, 0 omitidas** (eran 234 al inicio de H6). Commits: T-11b `0358009`, T-11c `844a089`, T-11a `388591a`.

**Cambios de contrato HTTP**

- `GET /api/v1/reservations`: reemplazo del envoltorio (`items/page/limit/total_items` → `pagination/applied_filter/user_role/view_title/reservations[]`). Decisión D1.
- `GET /api/v1/reservations/{id}`: reemplazo por la estructura de CU-21. Decisión D2. `POST /api/v1/reservations` no cambia.
- Nuevos: `GET /api/v1/vessels/{id}` y `GET /api/v1/vessels/{id}/quotations`.

**Pendientes y deudas de H6**

1. `payment_summary` completo depende de que CU-03/CU-12 persistan `rental_amount`, `insurance_amount`, `guarantee_deposit_amount` y `total_amount` (H7). Hoy las columnas existen pero están nulas; no se suman ni se inventan.
2. El nombre del propietario y los datos de embarcación provienen de M1 en cada consulta (D3). No hay caché ni copia. Para listas de 50 se deduplica y se acota el presupuesto; falta un endpoint de M1 por lote de IDs.
3. `vessel.image_url` no existe en la operación individual de CU-09; se intenta `image_url` y, si no, `photo_url`; si tampoco, se devuelve `null`.
4. `departure_port.city` no existe en el contrato de M1: se devuelve `null`.
5. No se persiste hora de llegada programada; `scheduled_arrival` usa `end_date + departure_time` (D7).
6. `status_badge` no se expone (D10); queda cubierto por el front.
7. `courtesy_elapsed_minutes` en `CANCELLED(NO_SHOW)` no se persiste; se devuelve `null` (el ejemplo del plan mostraba 30).
8. `COMPLETED` no distingue "sin incidentes" porque el contexto de disputa (H9) no existe.
9. El nuevo `HttpVesselReadAdapter` no escribe `external_query_log` para las fichas; la revalidación de capacidad de CU-19 sí pasa por `FleetInfoPort`, que ya registra CU-09 FR-010.
10. ~~`seashare.finance.base-url` sigue sin definirse~~ **Resuelto el 2026-11-12**: `application.yaml` define `${SEASHARE_FINANCE_BASE_URL:http://localhost:8083}` y `application-prod.yaml` lo exige por variable de entorno.
11. Riesgo de solape con H5 si T-13 reutiliza `ReservationQueryService`/`ReservationView`: se conservaron los métodos previos y `ReservationResponse` para minimizarlo.

---

### 4.11 Remediación de la auditoría H1–H6 y cierre de T-24 (2026-11-12)

Trabajo posterior al merge de H6 (`1e49661`) para desbloquear la Fase 7. Los commits `fde70f9` (ShedLock), `853756b` (SKIP LOCKED), `fe9ef7b` (purga del token), `79c1cd4` (`MoneyArchitectureTest`), `fdd47bd` (`base-url` de M3), `79117fe` (Java 21 y docstring TTL) y `5c25b97` (`.idea`) ya estaban en la rama; esta entrada recoge la remediación adicional.

**T-24 — creación vía CU-08**
- Nuevo puerto `InitiateReservationUseCase` y comando `InitiateReservationCommand` (`NewReservation` + `maxCapacity`).
- `ChangeReservationStatusService.initiate` valida `ReservationStateMachine.validateCreation(INITIATED)`, persiste, encola `reservation.info.provided` y registra la auditoría `null → INITIATED` con el **mismo `event_id`** determinista, todo en la misma transacción.
- `CreateReservationService` ya **no** persiste, audita ni encola: valida, consulta M1 y delega en `CreateReservationWriter` (nueva frontera transaccional que reclama idempotencia, invoca `initiate` y completa la clave).
- `ReservationStateMachine.canTransition(null, ...)` sigue devolviendo `false`: la creación no se abre a la matriz genérica.
- Nueva regla ArchUnit `SingleWriterArchitectureTest`: ninguna clase de aplicación fuera de `ChangeReservationStatusService` puede llamar a `ReservationRepositoryPort.create`.

**Llamadas externas fuera de transacción**
- `CreateReservationService` deja de ser `@Transactional`: las consultas a M1 ocurren antes de abrir la transacción de escritura.
- El relay del outbox usa un *lease* (`OutboxPort.lease`, `m2.scheduler.outbox.lease-ms`, 30 s): reclama el mensaje en una transacción corta, publica o notifica **fuera** de la transacción y marca el resultado en otra transacción corta. Si el proceso cae, el mensaje vuelve a estar pendiente al vencer el lease; el receptor deduplica por `event_id`.

**T-13 — autorización de CU-15**
- El endpoint interno exige `X-User-Role: FINANCE`; cualquier otro perfil recibe 403 `PROFILE_NOT_AUTHORIZED`. Los campos que dependen de CU-04/CU-07 siguen nulos y documentados.

**Vistas CU-20/CU-21**
- `status_badge` (`text`, `semantic_color`, `description`) en listado y detalle, con mapeo puro en `StatusBadge` y cobertura de todos los estados y subtipos.
- El listado usa `total_amount` con respaldo en `estimated_total`, igual que el detalle. `estimated_total` sigue siendo el monto de creación en CU-02 y el que usará CU-01 para cotizar.

**CU-12 sin esperas**
- Se elimina el reintento con `Thread.sleep` del valor calculado: una sola llamada; un 404 devuelve `CALCULATION_UNAVAILABLE` de inmediato y el cliente reintenta más tarde. Se conserva un reintento inmediato solo ante conexión rechazada. Se retiran `max-404-retries`, `retry-delay-ms` y `max-total-ms`.

**Perfiles y secretos**
- `seashare.fleet.mode`, `seashare.finance.mode` y el token de M1 se leen de variables de entorno. En `dev`/`test` el modo por defecto es `fake` (M2 corre aislado); en `prod` deben declararse explícitamente (`SEASHARE_FLEET_MODE`, `SEASHARE_FINANCE_MODE`, `SEASHARE_FINANCE_BASE_URL` y `SEASHARE_FLEET_TOKEN` cuando el modo sea `http`) o la aplicación no arranca. El token de desarrollo ya no está en la configuración compartida.
- `FleetProperties` y `FinanceProperties` validan el modo y, en `http`, el destino (y el token en M1); al arrancar con fakes se registra un aviso.

**Pruebas de contenedores en Windows**
- `PostgresTestContainers` y `RabbitTestContainers` aceptan puertos fijos por entorno (`SEASHARE_TEST_POSTGRES_PORT`, `SEASHARE_TEST_RABBIT_PORT`, `SEASHARE_TEST_RABBIT_MANAGEMENT_PORT`) para equipos donde Windows reserva rangos efímeros. RabbitMQ amplía la espera de arranque a 3 minutos.

**Verificación**: `.\mvnw.cmd test` del 2026-11-12 con Docker operativo → **332 pruebas, 0 fallos, 0 errores, 0 omitidas**.

---

### 4.12 Embudo de conversión: T-16a (CU-01) y T-16b (CU-03) (2026-11-12)

**T-16a — búsqueda de embarcaciones**
- Puerto nuevo `FleetCatalogPort` (`search(query, page, size)`) con dos adaptadores: `FakeFleetCatalogAdapter` (25 embarcaciones deterministas, modo por defecto) y `HttpFleetCatalogAdapter` (`GET /api/v1/vessels`, contrato de CU-09 operación 2, `X-Internal-Service-Token`).
- `VesselSearchService`: valida rol `RENTER`, pagina en lotes fijos de 20, filtra localmente por `type`, `passengers` y solapamiento con reservas `PENDING_PAYMENT`/`RESERVED`/`IN_NAVIGATION`, y cotiza en lote solo los ids visibles.
- `VesselSearchController`: `GET /api/v1/vessels` con `Cache-Control: no-store`; fechas, pasajeros, tipo y página se validan antes de consultar M1 o M3 (400 `INVALID_PARAMETERS`).
- Degradación: si M3 no cotiza, la respuesta sigue siendo 200 con `quotation_available: false`, `estimated_total: null` y `context_label` de tarifas en actualización; si falla M1, 503 `FLEET_SERVICE_UNAVAILABLE`.
- `[NEEDS CLARIFICATION]`: el contrato de catálogo de M1 figura en el plan de CU-09 pero no está firmado; el fake es el comportamiento por defecto.

**T-16b — iniciar pago**
- `PaymentController` expone `POST /api/v1/reservations/{id}/payments` y valida la política de cancelación (`POLICY_NOT_ACCEPTED`) y los datos del medio de pago (`PAYMENT_DATA_INCOMPLETE`, incluido el formato de `payer_email`).
- `StartPaymentService` hace las lecturas externas **fuera** de la transacción (cálculo de M3 y disponibilidad de M1) y delega en `StartPaymentWriter`.
- `StartPaymentWriter` (una transacción): `SELECT ... FOR UPDATE` sobre la reserva, revalidación de estado y TTL, control de solapamiento FCFS (`CONCURRENCY_CONFLICT`), persistencia del desglose literal de M3 y transición a `PENDING_PAYMENT` por CU-08 con `payment_token_ref`. La violación de la restricción GiST (`23P01`) se traduce a 409.
- El TTL no se reinicia. El desglose persistido hace que CU-21 deje de mostrar `payment_summary` en nulos.
- Nuevos códigos de error: `POLICY_NOT_ACCEPTED` (400) y `PAYMENT_DATA_INCOMPLETE` (400).
- Pendiente: el reintento con otra tarjeta sobre `PENDING_PAYMENT` con cobro `REJECTED` requiere el estado de cobro de CU-13; `payment_method_type` y `payment_metadata` se validan pero no se persisten.

**Verificación**: `.\mvnw.cmd test` del 2026-11-12 → **363 pruebas, 0 fallos, 0 errores, 0 omitidas**.

---

## 5. Pruebas y verificación

- La suite ejecuta **363 pruebas** (eran 273 al cierre de H6, 234 tras el Hito H3, 115 tras el Hito H2, 84 antes y 75 el 2026-11-03).
- La última ejecución completa es del **2026-11-12** (`.\mvnw.cmd test` con Docker): **363 pruebas ejecutadas, 0 fallos, 0 errores, 0 omitidas**.
- Comandos de referencia:
  ```bash
  ./mvnw test
  ./mvnw test -Dtest=ExpireReservationsServiceTest,TtlExpiryJobTest,TtlExpiryIntegrationTest
  ./mvnw test "-Dtest=RabbitTopologyTest,OutboxRelayIntegrationTest,EventDeduplicationTest"   # Hito H2 (requiere Docker)
  ./mvnw test "-Dtest=PublishPendingOutboxServiceTest,BackoffPolicyTest,OutboxRelayJobTest"   # unitarios del relay
  ```
- En Windows: `.\mvnw.cmd test`.
- **Nota de verificación (2026-11-06)**: no se volvió a ejecutar la suite en esta actualización porque Docker/PostgreSQL no estaban disponibles. El estado se contrastó con el código fuente y con los reportes de Surefire existentes. Los reportes del 2026-11-02 corresponden a una ejecución anterior y no se contabilizan.
- **Nota de verificación (2026-11-07, Hito H1)**: suite completa en verde con Docker disponible: `.\mvnw.cmd test` → 84 pruebas, 0 fallos; `.\mvnw.cmd verify` → BUILD SUCCESS (incluye Checkstyle en modo reporte). Ver historial para el detalle.
- **Nota de verificación (2026-11-07, Hito H2)**: baseline en verde antes de empezar (84/0); `.\mvnw.cmd test` → **115 pruebas, 0 fallos**; `.\mvnw.cmd verify` → BUILD SUCCESS (115/0 + Checkstyle: 19 violaciones reportadas sin fallar). El contenedor RabbitMQ de Testcontainers arranca **una vez por corrida** (compartido por JVM, igual que PostgreSQL): **10 arranques confirmados en log** durante la sesión (1 por cada una de las10 corridas de Maven con tests de mensajería que cargaron el contexto completo);2 corridas intermedias fallaron al cargar el contexto por un error de configuración propio (`publisher-confirm-type`) y su arranque no quedó confirmado en log. Ver historial para el detalle.
- **Nota de verificación (2026-11-09, Hito H4 + limpieza post-auditoría)**: `.\mvnw.cmd test` → **152 pruebas, 0 fallos** con docker-compose **apagado** (dos corridas seguidas y una con `-Dsurefire.runOrder=reversealphabetical`, todas en verde tras el arreglo de `CorrelationIdFilterTest`); `.\mvnw.cmd verify` → BUILD SUCCESS. Arranques de contenedor por corrida: 1 PostgreSQL + 1 RabbitMQ (singleton por JVM).
- **Migración a Testcontainers (2026-11-07)**: la suite ya **no** necesita docker-compose, solo Docker corriendo; `docker-compose.yml` sigue siendo necesario para la app en desarrollo. Detalle y métricas en el historial. Desde el Hito H2 la suite también usa un contenedor **RabbitMQ** compartido.
- **EXCEPCIÓN HEREDADA — tests con JdbcTemplate o repos JPA (registrada 2026-11-09, sin modificar los tests)**: la regla de "tests de integración solo con puertos, casos de uso y MockMvc" tiene estas excepciones **heredadas de H1/H2** (preexistentes, no creadas en H4). Se migrarán a puertos cuando se toque cada área:
  - `PostgresSmokeTest` (L7): `JdbcTemplate` para verificar el esquema migrado por Flyway en el test de humo.
  - `EventDeduplicationTest` (L25): `JdbcTemplate` para limpiar filas de `outbox_message` antes/después del ensayo de deduplicación.
  - `CreateReservationOutboxIntegrationTest` (L19): `JdbcTemplate` para limpiar `outbox_message` de las reservas creadas.
  - `OutboxRelayIntegrationTest` (L20): `JdbcTemplate` para verificar el estado de las filas del relay (`published_at`, `attempts`, `dead`).
  - `CreateReservationValidationTest` (L17): `JdbcTemplate` para limpiar `idempotency_key` en el `tearDown`.
  - `ReservationPersistenceTest` (L28): inyecta `ReservationJpaRepository` directamente para probar bloqueo optimista sobre la entidad `ReservationEntity` (mismo paquete, visibilidad de paquete).
  - `OutboxAdapterTest` (L33): mockea `OutboxMessageJpaRepository` (test unitario del adaptador, no de integración).
  - **Excepción vigente y autorizada (H4)**: `ReservationStatusAuditImmutabilityTest` (L20) — `JdbcTemplate` solo para `UPDATE`/`DELETE` sobre `reservation_status_audit` (objeto bajo prueba: el trigger de V1; ver detalle en la sección 4.7).

---

## 6. Pendiente / deuda técnica

### 6.1 Integraciones externas

1. ~~Implementar los clientes HTTP reales de Módulo 1 para CU-09 y CU-10~~ **Hecho (Hito H3, T-14)**, ver 4.8. Falta retirar los adaptadores *fake* como comportamiento por defecto (hoy `seashare.fleet.mode=fake`) cuando M1 firme el contrato.
2. ~~Implementar los clientes de Módulo 3 para CU-11 y CU-12~~ **Hecho (Hito H3, T-15)**, ver 4.9. Falta definir `seashare.finance.base-url` (puerto de M3) y que algun caso de uso llame a los puertos nuevos.
3. Implementar CU-22 y la notificación de estado operativo hacia Módulo 1 por `M1_HTTP`.

### 6.2 Mensajería y outbox

4. ~~Definir la topología RabbitMQ (exchange, colas, *bindings*, DLQ y retención).~~ **Completado (Hito H2, T-02)**: exchange topic,2 colas de M3 con sus DLQ y bindings. **Pendiente**: la cola de disputa `finance.guarantee-dispute.v1` queda para T-18c (decisión: no crearla en T-02).
5. ~~Implementar el relay del outbox con *publisher confirms*, reintentos con *backoff* y marcado de publicación.~~ **Completado (Hito H2, T-03)**.
6. Implementar los **consumidores AMQP de producción** (el publicador y la deduplicación por `event_id` ya están: T-03/T-04; la deduplicación solo tiene consumidor de prueba en `src/test`). **Pendiente también**: republicar a DLQ desde el relay tras el5º fallo (decisión del Hito H2: por ahora solo `dead=true` + log de alerta, sin consumidores aún).
7. ~~Incorporar ShedLock y el bloqueo de jobs.~~ **Completado el 2026-11-12 (T-05)**: `shedlock-spring` 7.11.0 + provider JDBC sobre la tabla `shedlock` de V2; `TtlExpiryJob` y `OutboxRelayJob` usan `@SchedulerLock`, y el relay reclama con `FOR UPDATE SKIP LOCKED` y *lease* por mensaje.

### 6.3 Núcleo CU-08

8. ~~Completar el contrato interno de CU-08: `ApplyReservationTransitionUseCase`, `expected_version`, `actor_id`, `events_enqueued` y validación de versión.~~ **Completado en H4.**
9. ~~Implementar la expiración por TTL también desde `PENDING_PAYMENT`.~~ **Completado en H4 (T-07).**
10. ~~Implementar la matriz completa de notificaciones a Módulo 1 y Módulo 3, incluida la guarda `m1_blocked` y los destinos `AMQP`/`M1_HTTP`.~~ **Completado en H4 (T-08).**
11. ~~Persistir los campos de CU-08 que hoy no mapea `ReservationMapper.applyChanges`.~~ **Completado en H4 (T-09).**
12. ~~Verificar y completar la inmutabilidad de estados terminales y la auditoría.~~ **Completado en H4 (T-10).**
13. ~~Creación vía CU-08 (T-24).~~ **Completado el 2026-11-12**, ver 4.11.

### 6.4 Casos de uso y contextos faltantes

13. Implementar el contexto `disputa` (CU-16, CU-17, CU-18) y `shared/contracts/dispute/OpenDisputeUseCase`.
14. Implementar CU-04, CU-05, CU-06, CU-07, CU-13 y CU-22 (CU-01 y CU-03 quedaron en H7; CU-14 y CU-15 en H5; CU-19 en H6).
15. Completar los contratos de CU-20 y CU-21 según sus planes.
16. Completar el contrato de CU-02 y sustituir las dependencias *fake* por integraciones reales.

### 6.5 Infraestructura y calidad

17. ~~Añadir `Dockerfile`, `README.md` y perfiles `application-dev/test/prod`.~~ **Completado (Hito H1, 2026-11-07)**.
18. Incorporar springdoc/OpenAPI y verificación de estilo: **completado en el Hito H1** (springdoc 3.2.0 + Checkstyle en modo reporte). **Siguen pendientes**: Resilience4j y `maven-failsafe-plugin`.
19. ~~Incorporar Testcontainers, WireMock y Awaitility para pruebas de integración y contrato.~~ **Completado (Hito H1)**: dependencias añadidas, base de pruebas reutilizable y un test de humo con PostgreSQL en contenedor. **Migración posterior (2026-11-07)**: las 11 clases `@SpringBootTest` usan ya el contenedor y la suite no requiere docker-compose. WireMock se usará en T-14/T-15.
20. Implementar observabilidad con *correlation id* en HTTP/AMQP: **completado (Hito H2, T-22)** (filtro `X-Correlation-Id` en MDC/respuesta y header AMQP). **Siguen pendientes**: logging estructurado, métricas y health checks específicos; y la regla ArchUnit "sin dinero" (`MoneyArchitectureTest`).
21. Cerrar los `[NEEDS CLARIFICATION]` abiertos y ejecutar las fases de *polish* del plan general.

---

## 7. Historial de actualizaciones

- **2026-11-03**: se verificaron y consolidaron los tests de idempotencia (Parte A) y se implementó el scheduler de expiración por TTL (Parte B), con CU-02 integrado al TTL y 75 pruebas sin fallos.
- **2026-11-06**: se reescribió esta bitácora para reflejar el avance global contra el plan general, separar lo implementado de lo pendiente y eliminar justificaciones de diseño que no describían estado de implementación. No se modificó código en esta actualización.
- **2026-11-07 — Hito H1 (Fase 1: Setup y decisiones) completado** en la rama `feature/fase1-setupV2`, sin push. Commits (uno por tarea):
  - `71b9250` `docs: registrar estado de decisiones D-01..D-15 del plan de trabajo` (T-01).
  - `b9bdc3b` `chore: agregar Dockerfile, README y perfiles de configuracion` (T-19).
  - `cc718fb` `chore: agregar springdoc-openapi y verificacion de estilo` (T-20).
  - `efa2cf3` `chore: agregar Testcontainers, WireMock y Awaitility con test de humo` (T-21).

  **T-01**: se añadió la columna `Estado` a la matriz D-01..D-15 de `plan-trabajo-backend.md` y se registró la excepción de ubicación de ese documento (D-14). Estados: D-01, D-02, D-04 y D-05 *PENDIENTE de confirmar con M1*; D-03 y D-06 *PENDIENTE de confirmar con M3* (D-06 además con el nombre del rol `FINANCE` como NEEDS CLARIFICATION); el resto, *Adoptada por defecto*. Sin cambios de contenido en las decisiones.

  **T-19**: archivos nuevos `Dockerfile` (multi-stage: build `maven:3.9.9-eclipse-temurin-21`, runtime `eclipse-temurin:21-jre`, usuario no root `seashare` uid 999, puerto configurable vía `SERVER_PORT`, sin secretos), `.dockerignore`, `README.md` (requisitos, `docker-compose.yml` ya existente, arranque, tests, perfiles, identidad temporal por headers `X-User-Id`/`X-User-Role`, estado de M1/M3) y `src/main/resources/application-{dev,test,prod}.yaml` (extensión `.yaml` por D-11; solo deltas por entorno; prod usa variables de entorno `SEASHARE_*`, sin secretos). `ddl-auto=validate` sigue activo en todos los perfiles (heredado de `application.yaml`, ningún perfil lo sobreescribe). Verificado: `docker build` OK, imagen arranca como usuario no root, la app arranca con `--spring.profiles.active=dev` y con `--spring.profiles.active=test` (log: `The following 1 profile is active: "test"`), y la suite sigue en verde (83 pruebas en ese punto).

  **T-20**: `pom.xml` + `config/checkstyle/checkstyle.xml`. springdoc-openapi **3.2.0** verificado en Maven Central: su parent es `spring-boot-starter-parent` 4.1.2 (idéntico al del proyecto). `/v3/api-docs` responde OpenAPI 3.1.0 con los 3 endpoints actuales (`GET/POST /api/v1/reservations`, `GET /api/v1/reservations/{id}`). Estilo: **Checkstyle** (menor fricción que Spotless, que reformatearía el código existente); `maven-checkstyle-plugin` **3.6.0** + Checkstyle **14.4.0**, configurado con `failOnViolation=false` → reporta sin fallar (19 violaciones de deuda sobre el código actual, sin reformatear nada). Criterio `.\mvnw.cmd verify` en verde.

  **T-21**: `pom.xml` + `src/test/java/com/seashare/m2/shared/`. Versiones verificadas en Maven Central (D-09): `spring-boot-testcontainers` **4.1.2** (BOM del parent), `org.testcontainers:testcontainers-postgresql` **2.0.5** (en Testcontainers 2.x el artifactId `postgresql` pasó a `testcontainers-postgresql`; el BOM de Spring Boot 4.1.2 **no** gestiona `org.testcontainers`, así que se importa `testcontainers-bom` 2.0.5 en `dependencyManagement`), `org.wiremock:wiremock-standalone` **3.13.2** (última estable; 4.0.0 está en beta) y `org.awaitility:awaitility` **4.3.0**. Archivos nuevos: `PostgresTestContainers.java` (configuración reutilizable con contenedor PostgreSQL 16 compartido por JVM + `@ServiceConnection`) y `PostgresSmokeTest.java` (único test de humo: la app conecta al contenedor y Flyway aplica las migraciones; verificado con puerto efímero 52049, distinto del 5433 de docker-compose). Los tests existentes **no** se migraron a Testcontainers; RabbitMQ con Testcontainers queda para T-02.

  **Decisiones y pendientes**: se adoptó por defecto todo lo previsto en el plan; siguen abiertas las confirmaciones con M1 (D-01, D-02, D-04, D-05), con M3 (D-03, D-06) y el nombre del rol `FINANCE`. Pendientes de H1: ninguno. Deudas observadas: `CLAUDE.md` no existe en el repo (el plan lo cita); 19 violaciones de Checkstyle por documentar/corregir en una tarea futura; Resilience4j y `maven-failsafe-plugin` siguen ausentes (no son parte del H1).
- **2026-11-07 — Migración de tests de integración a Testcontainers** (rama `feature/fase1-setupV2`, sin push). Commit `c6e8ff2` `chore: migrar tests de integracion a Testcontainers` (10 clases + `README.md`).
  - **Qué se migró**: las **11** clases con `@SpringBootTest` (el enunciato indicaba 12; la decimotercera coincidencia es un comentario javadoc dentro de `PostgresTestContainers.java`). A cada una se le añadió `@Import(PostgresTestContainers.class)`. **No** se tocaron aserciones, lógica de tests ni código de producción.
  - **Estrategia elegida**: `@Import` sobre clase base abstracta — una anotación por clase, sin cambiar jerarquías ni firmas de las clases de prueba.
  - **Contextos de Spring**: la caché `DefaultContextCache` quedó con **`size = 6`** contextos, **`hitCount = 592`** reutilizaciones, **`missCount = 6`** y `failureCount = 0` (medido con `logging.level.org.springframework.test.context.cache=DEBUG`). Los 6 corresponden a configuraciones distintas (con/sin `@AutoConfigureMockMvc`, `properties = "m2.scheduler.ttl.enabled=false"`, `@MockitoBean`/`@MockitoSpyBean`); el resto de clases reutiliza contexto cacheado.
  - **Contenedor**: PostgreSQL 16 arranca **una sola vez** por corrida (log único `Container postgres:16-alpine started`; el otro `started in` es Ryuk) y se retira al terminar la JVM de tests.
  - **Tiempos de `.\mvnw.cmd test`**: **antes con docker-compose: 77.8 s** (baseline) → **después sin compose: 84.9 s** (primera corrida, incluye arranque de contenedor), **74.9 s** (segunda corrida seguida) y **75.6 s** (con `-Dsurefire.runOrder=reversealphabetical`). `.\mvnw.cmd verify`: 82.7 s, BUILD SUCCESS.
  - **Verificaciones**: (a) `docker compose down` **sin `-v`** (volumen conservado), puertos 5433 y 5672 cerrados y suite en verde **solo con Docker** (84/0); (b) segunda corrida seguida en verde y corrida con orden inverso en verde; (c) tiempos arriba; (d) `verify` en verde (84/0 + Checkstyle: 19 violaciones reportadas sin fallar). **Ningún test dependía de orden ni de datos de otro test.**
  - **Configuración**: `application-test.yaml` no define datasource y ningún test fija `spring.datasource.*` ni usa `@ActiveProfiles` → sin cambios; `@ServiceConnection` tiene prioridad sobre `application.yaml`. La app en modo `dev` sigue usando docker-compose.
  - **Concurrencia**: `IdempotencyClaimConcurrencyTest` (commits reales, sin `@Transactional`) y `IdempotencyAdapterTest` en verde con la BD nueva del contenedor en cada corrida.
  - **README**: sección «4. Tests» actualizada (solo requiere Docker; docker-compose sigue para desarrollo).
  - **Pendientes**: RabbitMQ con Testcontainers queda fuera (T-02). Al terminar la verificación se volvió a levantar `docker compose up -d` para dejar el entorno de desarrollo listo.
- **2026-11-07 — Hito H2 (Fase 2: Fundación de integración) completado** en la rama `feature/fase2-Foundational` (rama activa del repositorio al empezar; el enunciato citaba `feature/fase1-setup`), sin push. Commits (uno por tarea):
  - `979e962` `feat: declarar topologia RabbitMQ con exchange, colas y DLQ (T-02)`.
  - `45b68ec` `feat: implementar relay del outbox con publisher confirms y reintentos (T-03)`.
  - `a56aad8` `feat: probar Message-Id y deduplicacion de eventos con consumidor de prueba (T-04)`.
  - `6158bc8` `feat: agregar filtro de correlation id y propagacion a headers AMQP (T-22)`.
  - `docs: actualizar implementContext tras Hito H2` (este commit).

  **T-02**: `RabbitTopologyConfig` declara exchange topic `seashare.reservations`, DLX `seashare.reservations.dlx`, colas `finance.reservation-status.v1` y `finance.reservation-info.v1` (con `x-dead-letter-exchange`/`x-dead-letter-routing-key`), sus2 DLQs y4 bindings; los nombres viven en `application.yaml` (`m2.messaging.*`) vía `MessagingProperties`, que también consume `OutboxAdapter` para no divergir. `docker-compose.yml` ya traía `rabbitmq:3-management` (sin cambios). Dependencia nueva: `org.testcontainers:testcontainers-rabbitmq` **2.0.5** (D-09: existe en Maven Central y la gestiona el `testcontainers-bom`2.0.5 ya importado). Pruebas: `RabbitTestContainers` (contenedor compartido por JVM) + `RabbitTopologyTest` (5 pruebas contra broker real).

  **T-03**: `PublishPendingOutboxUseCase`/`PublishPendingOutboxService` con `ClockPort`, lote ordenado (`next_attempt_at, created_at`), transacción por mensaje (`TransactionTemplate`), despacho `AMQP`/`M1_HTTP`, backoff1/5/25/125 s con jitter ±10 % (`BackoffPolicy`, `Random` inyectable),5 intentos → `dead=true` + alerta,4xx de M1 → dead directo (D-05), nunca propaga excepciones. `OutboxPort` ampliado con `findPending`, `markPublished` (purga condicional de `payment_token_ref` → `{}` por D-09, sin cambio de esquema) y `markFailed`. Puertos nuevos: `EventPublisherPort`, `FleetStatusNotificationPort` (+ `FleetNotificationOutcome`), DTO `PendingOutboxMessage`. Adaptadores: `RabbitEventPublisherAdapter` (confirms con `CorrelationData`), `FakeFleetStatusNotificationAdapter` (log + OK) y `OutboxRelayJob` con `m2.scheduler.outbox.enabled` (default `false` en `application.yaml`, `true` en dev/prod; decisión para proteger la suite, que comparte BD entre contextos cacheados). `SchedulerConfig` pasa a habilitar scheduling si `ttl` u `outbox` están activos.

  **T-04**: el publicador envía `Message-Id` = `event_id`, `Content-Type` JSON y mensaje persistente. Deduplicación demostrada **solo** con consumidor de prueba en `src/test` (`EventDeduplicationTest`):2 copias del mismo evento →1 procesamiento. Sin consumidor de producción (decisión: la deduplicación es de M3, D-04).

  **T-22**: `CorrelationIdFilter` en `shared/infrastructure/adapter/in/web/` (lee/genera `X-Correlation-Id`, MDC, respuesta) y propagación como header AMQP desde MDC en el publicador (solo con contexto HTTP; el relay corre sin MDC). Sin métricas/health ni `MoneyArchitectureTest` (fuera del alcance de esta tarea).

  **Decisiones del hito** (confirmadas antes de codificar): (1) `m2.scheduler.outbox.enabled` = `false` por defecto en `application.yaml` para que la suite no publique sin querer; (2) purga condicional del payload con token ya en este hito (a `{}`, sin cambio de esquema); (3) jitter ±10 % con `Random` inyectable para poder verificar retardos exactos; (4) el relay **no** republica a DLQ tras el5º fallo (solo `dead=true` + log); (5) no se crea la cola de disputa en T-02 (queda para T-18c).

  **Versiones (D-09)**: `testcontainers-rabbitmq`2.0.5 (BOM ya importado), `spring-boot-starter-amqp`4.1.2 (parent), `publisher-confirm-type: correlated` (Spring AMQP4 renombró `correlate`→`CORRELATED`), Awaitility4.3.0 ya presente. `@ServiceConnection` sobre `RabbitMQContainer` soportado por `RabbitContainerConnectionDetailsFactory` en `spring-boot-amqp`4.1.2.

  **Pruebas y verificación**: baseline `.\mvnw.cmd test` =84/0 antes de empezar; verde tras cada tarea (89/0,109/0,110/0,115/0); `.\mvnw.cmd verify` final = **115 pruebas,0 fallos**, BUILD SUCCESS con Checkstyle (19 violaciones, reporte sin fallar). Contenedor RabbitMQ: **1 arranque por corrida** de Maven (compartido por JVM); **10 arranques confirmados en log** en la sesión (las2 corridas con fallo temprano de `publisher-confirm-type` no lo confirmaron). Contenedor PostgreSQL:1 por corrida. Sin `Thread.sleep` (Awaitility en los reintentos).

  **Pendientes dejados**: T-05 (ShedLock), T-12 (contrato de payload de CU-14), adaptador HTTP real de M1 (`FakeFleetStatusNotificationAdapter` → `Http*`), consumidores AMQP de producción, republicación a DLQ, cola de disputa (T-18c), `MoneyArchitectureTest` y métricas/health.


- **2026-11-09 — Hito H3 (Fase 3: Adaptadores de lectura)**, rama `feature/fase3-adaptadores-lectura`, subida a origin, con PR #26 fusionado en develop. Commits de T-14 (CU-09 y CU-10, clientes de M1): `6665507`, `0792fd3`, `912e961`, `7778a7b`, `229df4e` (merge), `d685305` (V5), `9c79976`, `1cb39e2`, `8cd8a11`, `a289741`, `9d47663`, `48bb289`, `a7d5a4f`. Commits de T-15: CU-11 `f30b5dc`, `4ab85ec`, `f5cbb5d`, `4330d7d`; CU-12 `af81673`, `3877e16`, `5518aa7`, `107a238`. Documentación: `6d6d72b` y `39d88b5`; comentario de `application.yaml`: `2927d9c`.

  **Decisiones**: no se envía `Authorization` a M3 (el README de contratos de M3 lo confirma); reintento por socket una vez y solo ante `ConnectException`; precio inválido = cotización no disponible; en CU-12, alquiler y total en cero se rechazan y seguro y depósito en cero se aceptan. Detalle en las secciones 4.8 y 4.9.

  **Pruebas y verificación**: `.\mvnw.cmd test` el 2026-11-09 = **234 pruebas, 0 fallos, 0 errores, 0 omitidas**, BUILD SUCCESS (antes 115).

  **Pendientes dejados**: `base-url` de M3, que algún caso de uso llame a los puertos de M3 (H7), auditoría de CU-12 (FR-010), refactor del código duplicado en los adaptadores de M1 y M3, y tests opcionales (body del individual, ausencia de `Authorization`, `X-Correlation-Id` del valor calculado).
- **2026-11-10 — Hito H5 (Fase 5: Superficie Módulo 3)**, rama `feature/fase5-superficie-m3` (desde develop `21c7f9d`), subida a origin, PR pendiente. Commits: `dad0e52` mapeador de estados internos a los nombres de M3 (UC07), `e567fae` token de pago opcional en el comando de CU-08, `3bdc4af` payload de estado de reserva según UC07, `4ec6110` endpoint interno de consulta de reserva (CU-15), `5a0b8f8` pruebas de CU-15.

  **T-12 (CU-14)**: `reservation.status.changed` sigue el contrato UC07 de M3 (exchange `seashare.reservations`, routing key `reservation.status.changed`, cola `finance.reservation-status.v1`). `FinanceStatusMapper` traduce `PENDING_PAYMENT` a `PENDING`; `RESERVED`, `IN_NAVIGATION` y `COMPLETED` igual; `CANCELLED` según subtipo: `FLEXIBLE` a `CANCELLED_FLEXIBLE`, `MODERATE` a `CANCELLED_MODERATE`, `LATE` y `NO_SHOW` a `CANCELLED_STRICT`, `BY_OWNER` a `CANCELLED_BY_HOST`. `INITIATED`, `EXPIRED`, `PAYMENT_FAILED` y `CANCELLED` sin subtipo lanzan `IllegalStateException` (la `ModuleNotificationMatrix` ya impide que lleguen). `ChangeReservationStatusCommand` (12 campos) y `StatusChangedEvent` (9 campos) ganan `paymentTokenRef` al final, con constructores de compatibilidad. El token no se guarda en la base: solo viaja en el payload cuando el status mapeado es `PENDING` y no está vacío; el servicio lo rechaza con `INVALID_PARAMETERS` si el destino no es `PENDING_PAYMENT`. El payload lleva solo `reservation_id`, `status`, `status_changed_at` y, en `PENDING`, `payment_token_ref`; ya no envía `cancellation_subtype`. La clave de idempotencia no viaja en el mensaje (M3 la deriva con `reservation_id` y `status_changed_at`).

  **T-13 (CU-15)**: `GET /api/v1/internal/reservations/{reservation_id}`, solo lectura, sin dinero y sin eventos. Lector JDBC con dos consultas (la reserva y la última fila de `reservation_status_audit` con `to_status = 'CANCELLED'`), en archivos nuevos y separados para no chocar con H6. Errores: 401 `UNAUTHENTICATED` sin headers, 400 `INVALID_UUID`, 404 `RESERVATION_NOT_FOUND`. `ttl_timer`: aplica solo en `INITIATED` y `PENDING_PAYMENT` con `ttl_expires_at`; `remaining_seconds` es `max(0, segundos hasta el vencimiento)` y 0 cuando no aplica. `duration_days` es fin menos inicio más 1 y `duration_nights` es días menos 1. `scheduled_departure` es `start_date` más `departure_time` en `harbor_timezone`; con zona nula o inválida, la zona y las dos fechas pactadas salen nulas. `cancellation_actor`: `CANCELLED` toma el actor de la auditoría, `EXPIRED` informa `SYSTEM_TTL` (provisional) y otro estado informa nulo.

  **Decisiones cerradas de T-13**: (1) sin chequeo de rol; basta `X-User-Id` UUID y `X-User-Role` no vacío, y no se devuelve 403 aunque el plan lo pide. (2) `damage_detected` siempre nulo hasta H8 (CU-07). (3) `cancellation_anticipation_hours` siempre nulo hasta que CU-04 escriba en `cancellation_event`; `NO_SHOW` sale con actor `OWNER` porque lo dispara el propietario y `SYSTEM_NO_SHOW` no existe en `ActorType`. (4) `reservation_code` se lee con una consulta nueva sin tocar `Reservation`. (5) Archivos nuevos y separados.

  **[NEEDS CLARIFICATION]**: la spec de CU-14 (FR-002, FR-003, FR-004) pide actor, sub-estado, anticipación y notas de cierre en el payload, y el plan (L40 a L47 y L124) no; se siguió el plan y UC07, y M3 obtiene esos datos por CU-15. No se sabe si M3 ignora o rechaza campos desconocidos. Falta definir la hora de llegada: `scheduled_arrival` usa `end_date` más `departure_time` (provisional). El formato de `reservation_code` no está definido (el generador da `RS-` más 8 caracteres y el plan muestra `#RS-4492`). El plan marca `cancellation_subtype` como pendiente de confirmar con M3 y no se verificó que M3 llame este endpoint. `created_at` y `updated_at` salen en UTC (el plan muestra -05:00; es diferencia de representación, no de instante). Una expirada con cobro aprobado tardío se publicaría como `CANCELLED_FLEXIBLE` (plan L62), pero la máquina de estados hoy no permite salir de `EXPIRED` (es de CU-13, H7).

  **Pruebas y verificación**: `FinanceStatusMapperTest` (13), `OutboxAdapterTest` (12, 7 nuevos), 1 prueba nueva en `ChangeReservationStatusServiceTest`, y las pruebas de CU-15 (servicio, controlador e integración). `.\mvnw.cmd test` el 2026-11-10 = **271 pruebas, 0 fallos, 0 errores**, BUILD SUCCESS.

  **Pendientes dejados**: al fusionarse H6 (mi compañero), pasar `reservation_code` al agregado `Reservation`; CU-03 debe hacer obligatorio el token de pago (H7); `payment_method_type` y `payment_metadata` quedan para H7; avisar al grupo del borrado de `.idea/modules.xml` y del cambio de `.idea/misc.xml` que entraron en develop con el PR #27 ([NEEDS CLARIFICATION] si fue intencional); la rama `feature/fase3-adaptadores-lectura` no se borra hasta avisar al grupo.

- **2026-11-12 — Remediación de la auditoría H1–H6 y cierre de T-24**, rama `feature/h7-embudo-conversion`. Detalle técnico en la sección 4.11. Resumen:

  **Correcciones de la auditoría que ya estaban en la rama**: ShedLock (T-05), `FOR UPDATE SKIP LOCKED` en el outbox, purga selectiva de `payment_token_ref`, `MoneyArchitectureTest`, `base-url` de M3 con fail-fast, `source/target` 21 y `.idea` fuera del repositorio.

  **Cambios de esta entrada**: creación vía CU-08 (T-24) con `InitiateReservationUseCase`; `CreateReservationWriter` como frontera transaccional; consultas a M1 fuera de la transacción de CU-02; *lease* en el relay del outbox; 403 `FINANCE` en CU-15; `status_badge` en CU-20 y CU-21; total unificado (`total_amount` con respaldo `estimated_total`); CU-12 sin `Thread.sleep` ni reintentos del 404; perfiles con modos y token por variable de entorno y arranque fallido si faltan en `prod`; puertos fijos opcionales en los contenedores de prueba para entornos con rangos reservados.

  **Pruebas y verificación**: `.\mvnw.cmd test` con Docker = **332 pruebas, 0 fallos, 0 errores, 0 omitidas**, BUILD SUCCESS. El entorno exige `SEASHARE_TEST_POSTGRES_PORT`, `SEASHARE_TEST_RABBIT_PORT` y `SEASHARE_TEST_RABBIT_MANAGEMENT_PORT` en esta máquina por los rangos de puertos reservados de Windows.

  **Pendientes tras esta entrada**: T-16c (CU-13) sigue bloqueada por la decisión de producto/M3 sobre la salida de `EXPIRED` (C1–C10 de `diseno-h7-previo.md`); la máquina de estados mantiene `EXPIRED` y `PAYMENT_FAILED` como terminales.

- **2026-11-12 — Hito H7 parcial: T-16a (CU-01) y T-16b (CU-03)**, rama `feature/h7-embudo-conversion`. Detalle en la sección 4.12.

  **T-16a**: puerto `FleetCatalogPort` con adaptador fake (25 embarcaciones, por defecto) y HTTP (contrato de CU-09 operación 2); `VesselSearchService` con paginación fija de 20, filtros locales por tipo, capacidad y solapamiento, y cotización en lote tolerante a fallos; `GET /api/v1/vessels` con validación sintáctica previa y `Cache-Control: no-store`.

  **T-16b**: `POST /api/v1/reservations/{id}/payments` con validación de política y datos de pago, cálculo de M3 y disponibilidad de M1 fuera de la transacción, `SELECT ... FOR UPDATE`, control FCFS, traducción de la violación GiST a 409, persistencia del desglose y transición a `PENDING_PAYMENT` con el token en el evento `PENDING`. Nuevos códigos `POLICY_NOT_ACCEPTED` y `PAYMENT_DATA_INCOMPLETE`.

  **Pruebas y verificación**: `.\mvnw.cmd test` con Docker = **363 pruebas, 0 fallos, 0 errores, 0 omitidas**, BUILD SUCCESS. Nuevas: `VesselSearchServiceTest` (7), `VesselSearchControllerTest` (7), `StartPaymentServiceTest` (8) y `PaymentIntegrationTest` (9, con PostgreSQL real). Se ajustó `ErrorCodeTest` al nuevo catálogo de 23 códigos.

  **Pendientes de H7**: solo T-16c (CU-13), bloqueada por la decisión sobre `EXPIRED`/`PAYMENT_FAILED` y por el contrato de consulta de estado de cobro con M3. El reintento de pago con otra tarjeta queda dentro de CU-13.
