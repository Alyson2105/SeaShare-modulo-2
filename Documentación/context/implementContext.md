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
  - Integración HTTP real con Módulo 3 (CU-11 y CU-12); con Módulo 1 existen clientes HTTP reales para CU-09 y CU-10 (activos con `seashare.fleet.mode=http`) y adaptadores *fake* por defecto; falta el cliente de aviso de estado (CU-22) (incluido el nuevo `FakeFleetStatusNotificationAdapter` de notificación a M1).
  - Consumidores AMQP de producción: la deduplicación por `event_id` solo tiene un consumidor de prueba en `src/test` (T-04); M3 es quien deduplica en producción (D-04).
  - ShedLock, Spring Security y Resilience4j. (springdoc/OpenAPI, Testcontainers, WireMock y Awaitility sí están desde el Hito H1; ver historial.)
  - Contexto `disputa` y contrato interno `shared/contracts`.
  - Todos los CU salvo los avances parciales de CU-02, CU-08, CU-20 y CU-21. **Hito H4 completado (2026-11-09)**: CU-08 quedó completo como motor interno (comando, respuesta, auditoría completa, TTL doble, matriz de notificaciones M3/M1); siguen pendientes de CU-08 solo la creación vía CU-08 (T-24) y el payload final de CU-14 (T-12).
- **Hito H2 completado (2026-11-07)**: topología RabbitMQ (T-02), relay del outbox con publisher confirms y reintentos (T-03), publicación AMQP con `Message-Id` y prueba de deduplicación (T-04) y filtro de correlation id HTTP→AMQP (T-22). Detalle en la sección 4.6.
- La última ejecución registrada de la suite completa es del **2026-11-09** (`.\mvnw.cmd test`): **234 pruebas, 0 fallos, 0 errores, 0 omitidas** (eran 115 el 2026-11-07, tras el Hito H2).

---

## 2. Estado por fase del plan general

| Fase | CUs / alcance | Estado | Evidencia y brecha principal |
| :--- | :--- | :--- | :--- |
| Fase 1 — Setup | Infraestructura base | **Parcial (Hito H1 completado)** | Existen `pom.xml` con Boot 4.1.2/Java 21, `M2Application`, `docker-compose.yml` y `.gitignore`. Añadidos en el Hito H1: `Dockerfile` multi-stage, `README.md`, perfiles `dev/test/prod`, springdoc-openapi 3.2.0 y Checkstyle (reporte sin fallar). Verificado en vivo el 2026-11-09: `docker build` OK (imagen no-root uid 999), `GET /v3/api-docs` 200 con los 3 endpoints, arranque con `--spring.profiles.active=dev`. Siguen faltando Resilience4j y `maven-failsafe-plugin`. |
| Fase 2 — Foundational | Persistencia, errores, reloj, outbox, seguridad, mensajería, testing | **Parcial** | Implementados: errores globales, `ErrorCode`, Flyway V1 a V5, `ClockPort`, repositorio de reservas, idempotencia de entrada, escritura en outbox, reglas base de ArchUnit, Testcontainers y WireMock, **topología RabbitMQ (T-02), relay del outbox con publisher confirms y reintentos (T-03), publicación AMQP con `Message-Id` + deduplicación probada (T-04) y correlation id HTTP→AMQP (T-22)**. Faltan: seguridad, ShedLock, métricas/health y reglas ArchUnit completas. Los clientes HTTP salientes de M1 y M3 ya existen (Hito H3), pero siguen en modo `fake` por defecto. |
| Fase 3 — Adaptadores de lectura | CU-09, CU-10, CU-11, CU-12 | **En curso** | T-14 hecha: clientes HTTP reales de M1 para CU-09 y CU-10 con registro de consultas (ver 4.8); activos con `seashare.fleet.mode=http`, los *fake* siguen por defecto. T-15 hecha: clientes HTTP de M3 para CU-11 y CU-12 (ver 4.9), activos con `seashare.finance.mode=http`. |
| Fase 4 — Núcleo de dominio | CU-08 | **Completada (Hito H4, 2026-11-09)** | Contrato completo de CU-08 (T-06): comando con `actor_id`/`expected_version`/`mechanical_failure`/`anticipation_minutes`/`completion_notes`/`payment_reference`, respuesta con `version` y `events_enqueued`. Persistencia y V4 (T-09): `status_changed_at` y campos de cierre/pago. Auditoría completa (T-10): creación + transiciones con `actor_id`/`event_ids` e inmutabilidad probada. TTL doble (T-07): `INITIATED` y `PENDING_PAYMENT`. Matriz M3/M1 con guarda `m1_blocked` y destinos `AMQP` + `M1_HTTP` (T-08). Detalle en la sección 4.7. Queda fuera de esta fase: creación vía CU-08 (T-24, Fase 7) y payload final de CU-14 (T-12, Fase 5). |
| Fase 5 — Superficie Módulo 3 | CU-14, CU-15 | **No iniciada** | No hay consumidores, publicadores ni endpoint interno de consulta para Módulo 3. |
| Fase 6 — Vistas de consulta | CU-19, CU-20, CU-21 | **Parcial** | CU-20 y CU-21 tienen endpoints básicos de listado y detalle. CU-19 no existe y los contratos de CU-20/21 están simplificados. |
| Fase 7 — Embudo de conversión | CU-01, CU-02, CU-03, CU-13 | **Parcial** | Solo CU-02 tiene una implementación básica. CU-01, CU-03 y CU-13 no existen. |
| Fase 8 — Operaciones en muelle | CU-04, CU-05, CU-06, CU-07 | **No iniciada** | Sin endpoints, servicios ni eventos de cancelación, inasistencia, check-in o check-out. |
| Fase 9 — Disputa de garantía | CU-16, CU-17, CU-18 | **No iniciada** | Las tablas `dispute_*` existen en V2, pero no hay contexto `disputa`, lógica, endpoints ni publicación de eventos. |
| Fase 10 — Polish | Transversal | **No iniciada** | Sin pruebas de carga, resiliencia, observabilidad operativa, cierre de `[NEEDS CLARIFICATION]` ni documentación de arranque. |

---

## 3. Estado por caso de uso

| CU | Estado | Detalle verificado |
| :--- | :--- | :--- |
| CU-01 — Buscar embarcaciones disponibles | **No implementado** | No hay endpoint de búsqueda ni integración de catálogo/cotización. |
| CU-02 — Iniciar reserva | **Parcial** | `POST /api/v1/reservations`; valida fechas, capacidad, zona horaria y solapamiento; usa idempotencia de 60 s y fija TTL de 15 min. Depende de adaptadores *fake* de flota y su contrato no cubre la totalidad del plan. |
| CU-03 — Iniciar pago | **No implementado** | No hay endpoint, bloqueo pesimista ni transición a `PENDING_PAYMENT`. |
| CU-04 — Solicitar cancelación | **No implementado** | Sin endpoint ni clasificación de cancelación. |
| CU-05 — Marcar inasistencia | **No implementado** | Sin endpoint ni validación de la ventana de 30 min. |
| CU-06 — Marcar inicio de navegación | **No implementado** | Sin endpoint ni registro de check-in. |
| CU-07 — Marcar fin de navegación | **No implementado** | Sin endpoint ni registro de check-out. |
| CU-08 — Actualizar estado de reserva | **Parcial (motor completo desde el Hito H4)** | Motor interno con matriz de transiciones, auditoría inmutable, outbox y `@Version`. Contrato completo (T-06): comando con `expected_version`, `actor_id`, `mechanical_failure`, `anticipation_minutes`, `completion_notes`, `payment_reference` y respuesta con `version` + `events_enqueued` (puerto `ChangeReservationStatusUseCase`). Auditoría de creación y de cada transición con `actor_id`/`event_ids` (T-10). TTL desde `INITIATED` y `PENDING_PAYMENT` (T-07). Notificación a M1 (`M1_HTTP`) con guarda `m1_blocked` y matriz M3/M1 (T-08). Pendientes: la creación de reservas aún no pasa por CU-08 (T-24) y el payload de los eventos hacia M3 (T-12). |
| CU-09 — Proveer información de embarcación | **Parcial** | Cliente HTTP real (`HttpFleetInfoAdapter`) con timeouts, un reintento y registro de consultas; se activa con `seashare.fleet.mode=http`. Contrato con M1 sin firmar. |
| CU-10 — Brindar información de estado operativo | **Parcial** | Cliente HTTP real (`HttpFleetOperationalStatusAdapter`) con registro de consultas; solo `AVAILABLE` es disponible y una respuesta ilegible da 503. Contrato con M1 sin firmar. |
| CU-11 — Proveer información de cotización | **Parcial** | Clientes HTTP reales de M3 para estimación individual y en lote (`HttpIndividualEstimateAdapter`, `HttpBatchEstimateAdapter`) con puertos, fakes y pruebas WireMock; se activan con `seashare.finance.mode=http`. Falta `base-url` de M3 y que algún caso de uso los llame (CU-01 usará el lote en H7). |
| CU-12 — Brindar cálculo total de la reserva | **Parcial** | Cliente HTTP real (`HttpCalculatedValueAdapter`) con reintento del 404 y tope total de 800 ms; se activa con `seashare.finance.mode=http`. Falta `base-url` de M3, la auditoría de CU-12 (FR-010) y que CU-03 lo llame (H7). |
| CU-13 — Confirmar pago | **No implementado** | Sin canal B activo ni canal A. |
| CU-14 — Recibir estado de reserva | **Parcial** | El relay publica las filas `AMQP` con `Message-Id` y publisher confirm (T-03/T-04) hacia `finance.reservation-status.v1`. Desde el Hito H4 el `StatusChangedEvent` transporta `actor`, `anticipation_minutes` y `completion_notes` listos para el payload. Falta el contrato de payload de T-12 (nombres `PENDING`/`CANCELLED_*`, `payment_token_ref`); el consumidor real es de M3. |
| CU-15 — Solicitar información de la reserva | **No implementado** | Sin endpoint interno para Módulo 3. |
| CU-16 — Generar disputa de garantía | **No implementado** | Solo tablas en Flyway; sin lógica ni API. |
| CU-17 — Actualizar estado de disputa | **No implementado** | Solo tablas en Flyway; sin lógica ni API. |
| CU-18 — Recibir información de disputa | **No implementado** | Sin contexto `disputa` ni publicación AMQP. |
| CU-19 — Ver detalle de embarcación | **No implementado** | Sin endpoint ni integración con CU-09/CU-11. |
| CU-20 — Ver mis reservas | **Parcial** | `GET /api/v1/reservations` con paginación (`page`, `limit`) y filtrado por rol `RENTER`/`OWNER`. Faltan el filtro `filter` y la estructura exacta del contrato (`pagination`, `applied_filter`, `status_badge`). |
| CU-21 — Ver detalle de reserva | **Parcial** | `GET /api/v1/reservations/{id}` con validación de pertenencia. Faltan campos y estructura del contrato (`status_badge`, datos operativos completos). |
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
- Ningun caso de uso llama aun a los puertos nuevos: CU-01 usara el lote y CU-03 el calculo (H7); quien consume el individual esta por definir.
- Metodos `post` y `exchange` duplicados entre los adaptadores de M3 y los de M1. Refactor pendiente.
- Huecos de pruebas: body del individual, ausencia de `Authorization` en individual y lote, y `X-Correlation-Id` del valor calculado.

---

## 5. Pruebas y verificación

- La suite ejecuta **234 pruebas** (eran 115 tras el Hito H2, 84 antes y 75 el 2026-11-03).
- La última ejecución completa es del **2026-11-09** (`.\mvnw.cmd test`): **234 pruebas ejecutadas, 0 fallos, 0 errores, 0 omitidas**.
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
7. Incorporar ShedLock y el bloqueo de jobs. La tabla `shedlock` existe en V2, pero la dependencia y su uso **no** están implementados.

### 6.3 Núcleo CU-08

8. Completar el contrato interno de CU-08: `ApplyReservationTransitionUseCase`, `expected_version`, `actor_id`, `events_enqueued` y validación de versión.
9. Implementar la expiración por TTL también desde `PENDING_PAYMENT`.
10. Implementar la matriz completa de notificaciones a Módulo 1 y Módulo 3, incluida la guarda `m1_blocked` y los destinos `AMQP`/`M1_HTTP`.
11. Persistir los campos de CU-08 que hoy no mapea `ReservationMapper.applyChanges` (horas reales, notas de finalización, referencia de pago, etcétera).
12. Verificar y completar la inmutabilidad de estados terminales y la auditoría con todos los campos del plan.

### 6.4 Casos de uso y contextos faltantes

13. Implementar el contexto `disputa` (CU-16, CU-17, CU-18) y `shared/contracts/dispute/OpenDisputeUseCase`.
14. Implementar CU-01, CU-03, CU-04, CU-05, CU-06, CU-07, CU-13, CU-14, CU-15, CU-19 y CU-22.
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


- **2026-11-09 — Hito H3 (Fase 3: Adaptadores de lectura)**, rama `feature/fase3-adaptadores-lectura`, subida a origin, sin PR abierto. Commits de T-14 (CU-09 y CU-10, clientes de M1): `6665507`, `0792fd3`, `912e961`, `7778a7b`, `229df4e` (merge), `d685305` (V5), `9c79976`, `1cb39e2`, `8cd8a11`, `a289741`, `9d47663`, `48bb289`, `a7d5a4f`. Commits de T-15: CU-11 `f30b5dc`, `4ab85ec`, `f5cbb5d`, `4330d7d`; CU-12 `af81673`, `3877e16`, `5518aa7`, `107a238`. Documentación: `6d6d72b` y `39d88b5`; comentario de `application.yaml`: `2927d9c`.

  **Decisiones**: no se envía `Authorization` a M3 (el README de contratos de M3 lo confirma); reintento por socket una vez y solo ante `ConnectException`; precio inválido = cotización no disponible; en CU-12, alquiler y total en cero se rechazan y seguro y depósito en cero se aceptan. Detalle en las secciones 4.8 y 4.9.

  **Pruebas y verificación**: `.\mvnw.cmd test` el 2026-11-09 = **234 pruebas, 0 fallos, 0 errores, 0 omitidas**, BUILD SUCCESS (antes 115).

  **Pendientes dejados**: `base-url` de M3, que algún caso de uso llame a los puertos de M3 (H7), auditoría de CU-12 (FR-010), refactor del código duplicado en los adaptadores de M1 y M3, y tests opcionales (body del individual, ausencia de `Authorization`, `X-Correlation-Id` del valor calculado).