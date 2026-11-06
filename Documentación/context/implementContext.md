# Contexto de Progreso de Implementación - Módulo 2 (SEA-SHARE)

**Fecha**: 2026-11-06  
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
  - Job programado de expiración por TTL para reservas en `INITIATED`.
  - Manejo global de errores `{ code, message }`, identidad temporal por headers, reloj inyectable y reglas base de ArchUnit.
- **No implementado**:
  - Integración HTTP real con Módulo 1 y Módulo 3; solo existen adaptadores *fake* para flota.
  - Topología RabbitMQ, relay del outbox, publicador con *publisher confirms*, consumidores y DLQ.
  - ShedLock, Spring Security, OpenAPI, Resilience4j, Testcontainers, WireMock y Awaitility.
  - Contexto `disputa` y contrato interno `shared/contracts`.
  - Todos los CU salvo los avances parciales de CU-02, CU-08, CU-20 y CU-21.
- La última ejecución registrada de la suite completa es del **2026-11-03 00:16**: **75 pruebas, 0 fallos, 0 errores**.

---

## 2. Estado por fase del plan general

| Fase | CUs / alcance | Estado | Evidencia y brecha principal |
| :--- | :--- | :--- | :--- |
| Fase 1 — Setup | Infraestructura base | **Parcial** | Existen `pom.xml` con Boot 4.1.2/Java 21, `M2Application`, `docker-compose.yml` y `.gitignore`. Faltan `Dockerfile`, `README.md`, perfiles `dev/test/prod`, springdoc, Resilience4j, Checkstyle/Spotless y `maven-failsafe-plugin`. |
| Fase 2 — Foundational | Persistencia, errores, reloj, outbox, seguridad, mensajería, testing | **Parcial** | Implementados: errores globales, `ErrorCode`, Flyway V1–V3, `ClockPort`, repositorio de reservas, idempotencia de entrada, escritura en outbox y reglas base de ArchUnit. Faltan: seguridad, topología RabbitMQ, relay y publicación, clientes HTTP salientes, observabilidad con *correlation id*, deduplicación de eventos salientes, Testcontainers/WireMock y reglas ArchUnit completas. |
| Fase 3 — Adaptadores de lectura | CU-09, CU-10, CU-11, CU-12 | **No iniciada** | Solo existen los puertos `FleetInfoPort` y `FleetOperationalStatusPort` con adaptadores *fake*. No hay clientes reales ni CU-09/10/11/12. |
| Fase 4 — Núcleo de dominio | CU-08 | **Parcial** | Existe el motor de transiciones, auditoría, outbox y TTL para `INITIATED`. Falta el contrato completo de CU-08, la expiración desde `PENDING_PAYMENT`, `expected_version`/`actor_id`, `events_enqueued`, la guarda `m1_blocked` y los destinos `AMQP` + `M1_HTTP`. |
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
| CU-08 — Actualizar estado de reserva | **Parcial** | Motor interno con matriz de transiciones, auditoría inmutable, outbox y `@Version`. No implementa el contrato completo (`ApplyReservationTransitionUseCase`, `expected_version`, `actor_id`, `events_enqueued`), ni la notificación a M1 (`M1_HTTP`), ni la guarda `m1_blocked`, ni la expiración desde `PENDING_PAYMENT`. |
| CU-09 — Proveer información de embarcación | **No implementado** | Solo puerto de salida y adaptador *fake*. |
| CU-10 — Brindar información de estado operativo | **No implementado** | Solo puerto de salida y adaptador *fake*. |
| CU-11 — Proveer información de cotización | **No implementado** | Sin cliente de Módulo 3 ni endpoint. |
| CU-12 — Brindar cálculo total de la reserva | **No implementado** | Sin cliente de Módulo 3 ni endpoint. |
| CU-13 — Confirmar pago | **No implementado** | Sin canal B activo ni canal A. |
| CU-14 — Recibir estado de reserva | **No implementado** | El outbox inserta filas `AMQP`, pero no existe relay, publicador ni consumidor. |
| CU-15 — Solicitar información de la reserva | **No implementado** | Sin endpoint interno para Módulo 3. |
| CU-16 — Generar disputa de garantía | **No implementado** | Solo tablas en Flyway; sin lógica ni API. |
| CU-17 — Actualizar estado de disputa | **No implementado** | Solo tablas en Flyway; sin lógica ni API. |
| CU-18 — Recibir información de disputa | **No implementado** | Sin contexto `disputa` ni publicación AMQP. |
| CU-19 — Ver detalle de embarcación | **No implementado** | Sin endpoint ni integración con CU-09/CU-11. |
| CU-20 — Ver mis reservas | **Parcial** | `GET /api/v1/reservations` con paginación (`page`, `limit`) y filtrado por rol `RENTER`/`OWNER`. Faltan el filtro `filter` y la estructura exacta del contrato (`pagination`, `applied_filter`, `status_badge`). |
| CU-21 — Ver detalle de reserva | **Parcial** | `GET /api/v1/reservations/{id}` con validación de pertenencia. Faltan campos y estructura del contrato (`status_badge`, datos operativos completos). |
| CU-22 — Recibir información de estado operativo | **No implementado** | Sin eventos ni notificación hacia Módulo 1. |

---

## 4. Implementación verificable

### 4.1 Fundaciones y configuración

- Proyecto Spring Boot 4.1.2 con Java 21 y Maven. Dependencias presentes: Web MVC, Validation, Data JPA, AMQP, Actuator, Flyway (`spring-boot-starter-flyway` + `flyway-database-postgresql`), PostgreSQL y ArchUnit para pruebas.
- Flyway administra el esquema con `spring.jpa.hibernate.ddl-auto: validate`. Existen las migraciones **V1, V2 y V3**.
  - V1 crea `reservation`, `reservation_status_audit` y `outbox_message`, con `btree_gist`, restricción `EXCLUDE` de solapamiento e índice `ix_reservation_ttl`.
  - V2 crea tablas de funcionalidades aún no implementadas (`payment_attempt`, eventos de cancelación/inasistencia/check-in/check-out, `external_query_log`, `dispute_*`, `operational_failure`, `idempotency_key`, `shedlock`). **Estas tablas son estructura de datos, no funcionalidad implementada.**
  - V3 agrega campos de reserva y columnas de control del outbox.
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
  - El relay del outbox a RabbitMQ no existe todavía y los eventos quedan persistidos en la tabla `outbox_message`.

---

## 5. Pruebas y verificación

- La suite contiene **75 métodos `@Test` distribuidos en 18 archivos**.
- La última ejecución completa registrada en `target/surefire-reports` es del **2026-11-03 00:16**: **75 pruebas ejecutadas, 0 fallos, 0 errores, 0 omitidas**.
- Comandos de referencia:
  ```bash
  ./mvnw test
  ./mvnw test -Dtest=ExpireReservationsServiceTest,TtlExpiryJobTest,TtlExpiryIntegrationTest
  ```
- En Windows: `.\mvnw.cmd test`.
- **Nota de verificación (2026-11-06)**: no se volvió a ejecutar la suite en esta actualización porque Docker/PostgreSQL no estaban disponibles. El estado se contrastó con el código fuente y con los reportes de Surefire existentes. Los reportes del 2026-11-02 corresponden a una ejecución anterior y no se contabilizan.

---

## 6. Pendiente / deuda técnica

### 6.1 Integraciones externas

1. Implementar los clientes HTTP reales de Módulo 1 para CU-09 y CU-10, con timeouts, reintento rápido y *fail-safe*; retirar los adaptadores *fake* como comportamiento por defecto.
2. Implementar los clientes de Módulo 3 para CU-11 y CU-12.
3. Implementar CU-22 y la notificación de estado operativo hacia Módulo 1 por `M1_HTTP`.

### 6.2 Mensajería y outbox

4. Definir la topología RabbitMQ (exchange, colas, *bindings*, DLQ y retención).
5. Implementar el relay del outbox con *publisher confirms*, reintentos con *backoff* y marcado de publicación.
6. Implementar el publicador y los consumidores AMQP, incluida la deduplicación por `event_id`.
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

17. Añadir `Dockerfile`, `README.md` y perfiles `application-dev/test/prod`.
18. Incorporar springdoc/OpenAPI, Resilience4j y verificación de estilo.
19. Incorporar Testcontainers, WireMock y Awaitility para pruebas de integración y contrato.
20. Implementar observabilidad con *correlation id* en HTTP/AMQP, logging estructurado y métricas.
21. Cerrar los `[NEEDS CLARIFICATION]` abiertos y ejecutar las fases de *polish* del plan general.

---

## 7. Historial de actualizaciones

- **2026-11-03**: se verificaron y consolidaron los tests de idempotencia (Parte A) y se implementó el scheduler de expiración por TTL (Parte B), con CU-02 integrado al TTL y 75 pruebas sin fallos.
- **2026-11-06**: se reescribió esta bitácora para reflejar el avance global contra el plan general, separar lo implementado de lo pendiente y eliminar justificaciones de diseño que no describían estado de implementación. No se modificó código en esta actualización.
