# Auditoría H1–H6 — Backend M2

**Fecha:** 2026-11-12
**Rama auditada:** `feature/h7-embudo-conversion`
**HEAD:** `1e49661` (merge PR #29, H6)
**Alcance:** Hitos H1 a H6 del plan backend (`T-01`, `T-19`–`T-22`, `T-14`, `T-15`, `T-06`–`T-10`, `T-12`, `T-13`, `T-11a`–`T-11c`), con impacto declarado sobre las precondiciones de H7.
**Método:** lectura estática del código, migraciones, configuración y documentación; comandos git de solo lectura; compilación de producción y de tests. No se modificó código, no se hicieron commits y no se ejecutaron comandos destructivos.

**Clasificación usada en todo el informe**

| Etiqueta | Significado |
|---|---|
| **VERIFICADO** | Observado directamente en el código o en la salida de un comando de esta auditoría. |
| **DECLARADO** | Afirmado por la documentación del proyecto, sin comprobación de ejecución en esta auditoría. |
| **NO VERIFICABLE** | Requiere Docker, servicios externos o un entorno multi-réplica no disponible. |

---

## 1. Resumen ejecutivo

1. **Veredicto: NO-GO para iniciar H7 en su forma actual.** Hay bloqueantes funcionales y de integración, no solo ambientales.
2. H4 (núcleo CU-08) es la parte más sólida: máquina de estados, auditoría, outbox, TTL doble y matriz M3/M1 están implementados en código.
3. H2 está incompleto: **no existe ShedLock** (`T-05`) y falta `MoneyArchitectureTest` (`T-22`); el relay del outbox tampoco reclama filas de forma segura.
4. H5 está incompleto: el endpoint interno no valida rol `FINANCE` ni devuelve **403** (`T-13`), y publica campos obligatorios en `null`.
5. H6 está incompleto por decisión consciente: **no se expone `status_badge`** en listado ni detalle (`T-11b`/`T-11c`), y el listado usa `estimatedTotal` en lugar del total liquidado.
6. **`T-24` no está cumplido:** la creación sigue persistiendo con `reservationRepositoryPort.create(...)` y no pasa por CU-08 (`CreateReservationService.java:131`).
7. **CU-13 es inviable hoy:** `EXPIRED` y `PAYMENT_FAILED` son terminales y la cancelación solo se admite desde `RESERVED`; el cobro tardío `EXPIRED → CANCELLED_FLEXIBLE` no puede ocurrir.
8. La integración real con M3 no es operable en modo `http`: **falta `seashare.finance.base-url`** y los adaptadores de M1/M3 no cumplen del todo la política de reintentos.
9. La suite completa **no se pudo ejecutar** porque el daemon de Docker no está disponible; solo se verificó compilación. Los conteos 152/271/273 son **DECLARADOS**.
10. Reevaluar a **GO CON CONDICIONES** solo después de cerrar los bloqueantes de la sección 6 y de repetir `clean verify` y la suite en orden inverso con Docker operativo.

---

## 2. Estado del repositorio y de la suite

### 2.1 Repositorio

| Comprobación | Resultado | Evidencia |
|---|---|---|
| Rama actual | `feature/h7-embudo-conversion` | `git rev-parse --abbrev-ref HEAD` |
| Árbol de trabajo | Limpio (sin cambios locales) | `git status --porcelain` sin salida |
| HEAD | `1e49661 Merge pull request #29 ...` | `git log --oneline -8` |
| Integración H5 | Merge `7ca6719` (PR #28) ya contenido en HEAD | `git log --oneline -8` |
| Integración H6 | Merge `1e49661` (PR #29) ya contenido en HEAD | `git log --oneline -8` |
| Migraciones | V1 a V5; no existe V6 | `src/main/resources/db/migration/` |
| Archivos IDE versionados | `.idea/.gitignore`, `.iml`, `discord.xml`, `misc.xml`, `vcs.xml` | `git ls-files .idea` |
| `target/`, `CLAUDE.md`, `untitled` versionados | No encontrados | `git ls-files` filtrado |

La rama local `feature/fase5-superficie-m3` ya no existe; la integración de H5 queda probada por el merge `7ca6719`. No se encontraron artefactos de build ni archivos temporales versionados.

### 2.2 Suite de pruebas

| Intento | Resultado | Evidencia |
|---|---|---|
| `docker info` | **Falla**: no conecta con `npipe:////./pipe/dockerDesktopLinuxEngine` | Salida de `docker info` |
| `.\mvnw.cmd clean verify` | **No ejecutado**: bloqueado por Docker | Decisión de no inventar resultados |
| `.\mvnw.cmd test "-Dsurefire.runOrder=reversealphabetical"` | **No ejecutado**: bloqueado por Docker | Igual que el anterior |
| `.\mvnw.cmd clean test-compile -DskipTests` | **BUILD SUCCESS**, 26.759 s, 137 fuentes main y 49 fuentes test, `release 21` | Salida de la compilación del 2026-11-12 |

**Conteos de pruebas declarados por la documentación, no verificados aquí:**

| Documento | Conteo declarado | Observación |
|---|---|---|
| `implementContext.md:229` | 152 pruebas al cierre de H4 (2026-11-09) | DECLARADO |
| `implementContext.md:508` | 271 pruebas al cierre de H5 (2026-11-10) | DECLARADO |
| `implementContext.md:344` | 273 pruebas al cierre de H6 (2026-11-10) | DECLARADO |
| `implementContext.md:28` y `:371` | 271 pruebas como “última ejecución” | Inconsistente con `:344` (273) |

Inventario local: 49 archivos bajo `src/test/java`, de los cuales 2 son utilidades de Testcontainers (`PostgresTestContainers`, `RabbitTestContainers`) y 1 es el test de contexto (`M2ApplicationTests`). No se encontraron `@Disabled`, `@Ignore` ni `Thread.sleep` en código de pruebas. Varias pruebas usan `JdbcTemplate` directamente, en los casos ya documentados como excepción.

---

## 3. Hallazgos

| ID | Severidad | Clasificación | Hallazgo | Evidencia |
|---|---|---|---|---|
| F-01 | Alta | VERIFICADO | `T-24` incumplido: la creación persiste directamente con `Reservation.initiate(...)` y no invoca CU-08. El flujo queda fuera del “único escritor” y no puede validar la transición inicial con la máquina de estados. | `CreateReservationService.java:131`; `CreateReservationService.java:36` (`@Transactional`); ausencia de `ChangeReservationStatusUseCase` en el servicio de creación (grep sin coincidencias) |
| F-02 | Alta | VERIFICADO | CU-13 no puede aplicar la regla de cobro tardío: `EXPIRED` y `PAYMENT_FAILED` no tienen salidas, y toda cancelación exige origen `RESERVED`. El caso `EXPIRED → CANCELLED_FLEXIBLE` es imposible. | `ReservationStateMachine.java:22-23`; `ChangeReservationStatusService.java:159-165`; reconocido en `implementContext.md:506` |
| F-03 | Alta | VERIFICADO | `T-05` no implementado: no hay dependencia ni anotaciones de ShedLock; ambos jobs tienen solo un `TODO`. En multi-réplica, TTL y relay pueden ejecutarse duplicados. | `pom.xml` sin `shedlock`; `TtlExpiryJob.java:13-14`; `OutboxRelayJob.java:19`; `application-prod.yaml:25` |
| F-04 | Alta | VERIFICADO | `T-03` parcial: `findPending` no usa `FOR UPDATE SKIP LOCKED` ni reclamación/lease. Dos instancias pueden leer y publicar el mismo mensaje; la deduplicación de M3 mitiga el efecto, pero no garantiza la entrega única ni evita trabajo duplicado. | `OutboxMessageJpaRepository.java:14-21`; `PublishPendingOutboxService.java:77-87` |
| F-05 | Alta | VERIFICADO | `T-13` parcial: el endpoint interno no comprueba el rol `FINANCE` ni devuelve 403. `CallerIdentity` se recibe pero no se usa para autorizar. | `InternalReservationController.java:22-36` |
| F-06 | Media | VERIFICADO | El endpoint interno devuelve en `null` dos campos exigidos por el contrato: `cancellation_anticipation_hours` y `damage_detected`. | `InternalReservationResponse.java:129`; `InternalReservationResponse.java:134` |
| F-07 | Media | VERIFICADO | `T-11b` y `T-11c` no exponen `status_badge` en listado ni detalle. Es una desviación consciente registrada, pero incumple la aceptación literal de ambas tareas. | `ReservationListItemResponse.java:15-55`; `ReservationDetailResponse.java:14-54`; `implementContext.md:306` (D10) |
| F-08 | Media | VERIFICADO | `T-12` parcial: el payload solo lleva `reservation_id`, `status`, `status_changed_at` y el token opcional. No incluye `actor`, `cancellation_subtype`, `anticipation_minutes` ni `completion_notes`, que la spec de CU-14 pide y el plan decidió omitir. | `OutboxAdapter.java:42-50`; `implementContext.md:66` y `:506` |
| F-09 | Alta | VERIFICADO | `seashare.finance.base-url` no existe en `application.yaml`. En `mode=http`, `FinanceProperties.baseUrl` queda nulo y los clientes de M3 no son operables. | `application.yaml:64-74`; `FinanceProperties.java:12`; `FinanceHttpConfig.java:21,32,43`; `implementContext.md:363` |
| F-10 | Media | VERIFICADO | El adaptador de valor calculado usa `Thread.sleep` en producción para esperar entre reintentos. Bloquea el hilo de la petición y no es cancelable de forma cooperativa. | `HttpCalculatedValueAdapter.java:92-101` |
| F-11 | Media | VERIFICADO | `T-14` parcial: los adaptadores de flota solo reintentan `503` o `ResourceAccessException`; un `500`, `502` o `504` se trata como error permanente, pese a que la aceptación pide reintento ante 5xx o timeout. Un 503 persistente se registra como `TIMEOUT`. | `HttpFleetInfoAdapter.java:105-127`; `HttpFleetOperationalStatusAdapter.java:82-104` |
| F-12 | Media | VERIFICADO | `HttpVesselReadAdapter` no reintenta, no escribe `external_query_log`, no propaga correlation id y mide el presupuesto con `System.nanoTime()` en lugar de `ClockPort`. El fail-safe por elemento sí está implementado. | `HttpVesselReadAdapter.java:44,79,103-119,187-208` |
| F-13 | Media | VERIFICADO | `T-22` parcial: no existe `MoneyArchitectureTest` y `LayeredArchitectureTest` solo tiene tres reglas de capas. No hay regla automática que prohíba aritmética monetaria ni dependencias prohibidas adicionales. | `LayeredArchitectureTest.java:16-29`; búsqueda de `MoneyArchitectureTest` sin resultados |
| F-14 | Media | VERIFICADO | Purga de token incompleta: `markPublished` reemplaza todo el payload por `{}`, perdiendo el evento para auditoría/reintento; `markFailed(dead=true)` no purga el token. | `OutboxAdapter.java:131-137`; `OutboxAdapter.java:141-147` |
| F-15 | Media | VERIFICADO | El relay no propaga correlation id: el publicador AMQP solo añade el header si hay MDC, y el job no reconstruye ese contexto. Los eventos publicados por el relay salen sin trazabilidad de la petición origen. | `RabbitEventPublisherAdapter.java:55-58`; `OutboxRelayJob.java:33-39` |
| F-16 | Media | VERIFICADO | El `pom.xml` fija `source`/`target` 7 aunque el proyecto declara Java 21, y Checkstyle tiene `failOnViolation=false`. El build compila con `release 21`, pero la configuración es contradictoria y el estilo no bloquea el build. | `pom.xml:30`; `pom.xml:192-199`; `pom.xml:170-181` |
| F-17 | Media | VERIFICADO | Llamadas HTTP externas dentro de transacciones: la creación consulta M1 bajo `@Transactional` y la consulta de vistas llama a M1 dentro de una transacción `readOnly`. Aumenta el tiempo de retención de conexión y el riesgo de bloqueos. | `CreateReservationService.java:36,89`; `ReservationQueryService.java:33,191` |
| F-18 | Media | VERIFICADO | El listado usa `view.estimatedTotal()` para `total_price.amount`, mientras el detalle usa `totalAmount` con respaldo en `estimatedTotal`. Los dos contratos pueden mostrar montos distintos para la misma reserva. | `ReservationListItemResponse.java:138`; `ReservationDetailPolicy.java:49` |
| F-19 | Baja | VERIFICADO | `.idea/*` está versionado, en contra de la regla de no commitear archivos de IDE. | `git ls-files .idea` |
| F-20 | Baja | VERIFICADO | El docstring de `TtlExpiryJob` sigue diciendo “reservas en INITIATED”, aunque `T-07` ya expira también `PENDING_PAYMENT`. | `TtlExpiryJob.java:10`; `ReservationJpaRepository.java:40-41` |
| F-21 | Baja | VERIFICADO | No hay prueba de rollback del outbox cuando `FinanceStatusMapper` lanza por un estado no publicable. La cobertura existente valida el mapeo y los estados ilegales, pero no el efecto transaccional. | `FinanceStatusMapperTest` (inventario); ausencia de escenario de rollback en las pruebas revisadas |
| F-22 | Alta | NO VERIFICABLE | No se pudo ejecutar la suite completa ni la suite en orden inverso. Los resultados 152/271/273 son declaraciones documentales. | `docker info` falla; `clean verify` no ejecutado |
| F-23 | Media | NO VERIFICABLE | No se puede comprobar el comportamiento multi-réplica, la recuperación tras reinicio del broker, la latencia real de M1/M3 ni el contrato efectivo de los módulos externos. | Requiere Docker y servicios M1/M3 |

---

## 4. Matriz plan vs código por tarea

Estados: **Cumple** = aceptación satisfecha en código; **Parcial** = implementación presente con brechas; **No cumple** = ausente o contradictoria; **No verificable** = depende de ejecución.

### H1 — Setup y decisiones

| Tarea | Estado | Evidencia | Observación |
|---|---|---|---|
| T-01 Decisiones D-01..D-15 | **Cumple** | `plan-trabajo-backend.md:32-50` | Decisiones registradas y excepción de ubicación documentada. |
| T-19 Dockerfile, README y perfiles | **Cumple (código)** | `Dockerfile`; `README.md`; `application-dev.yaml`, `application-test.yaml`, `application-prod.yaml` | El arranque real no se verificó en esta auditoría. |
| T-20 springdoc y estilo | **Parcial** | `pom.xml:125-129`; `pom.xml:170-190`; `pom.xml:192-199` | springdoc presente; Checkstyle solo reporta y el compilador fija Java 7. |
| T-21 Testcontainers, WireMock, Awaitility | **Cumple (dependencias)** | `pom.xml:131-161`; `PostgresSmokeTest` | La ejecución con contenedores no se pudo verificar. |

### H2 — Fundación de integración

| Tarea | Estado | Evidencia | Observación |
|---|---|---|---|
| T-02 Topología RabbitMQ | **Cumple (código)** | `application.yaml:33-40`; `RabbitTopologyTest` | Sin ejecución verificada. |
| T-03 Relay del outbox | **Parcial** | `PublishPendingOutboxService.java:75-161`; `OutboxMessageJpaRepository.java:14-21`; `OutboxAdapter.java:131-147` | Confirms, backoff y `dead` existen; faltan reclamación segura, ShedLock y purga correcta del token. |
| T-04 Publicación y deduplicación | **Cumple (código)** | `RabbitEventPublisherAdapter.java:52`; `EventDeduplicationTest` | `Message-Id = event_id`; sin ejecución verificada. |
| T-05 ShedLock | **No cumple** | `pom.xml`; `TtlExpiryJob.java:13-14`; `OutboxRelayJob.java:19` | Dependencia y uso ausentes. |
| T-22 Observabilidad y ArchUnit | **Parcial** | Filtro de correlation id; `RabbitEventPublisherAdapter.java:55-58`; `LayeredArchitectureTest.java:16-29` | Falta `MoneyArchitectureTest`, métricas/health completos y propagación en el relay. |

### H3 — Integraciones M1/M3

| Tarea | Estado | Evidencia | Observación |
|---|---|---|---|
| T-14 Cliente HTTP de M1 | **Parcial** | `HttpFleetInfoAdapter.java:105-127`; `HttpFleetOperationalStatusAdapter.java:82-104` | Timeouts 100/300 ms y registro de consultas; reintento limitado a 503/timeout, no a todo 5xx. |
| T-15 Cliente HTTP de M3 | **Parcial** | `HttpIndividualEstimateAdapter.java:47-63`; `HttpBatchEstimateAdapter.java:78`; `HttpCalculatedValueAdapter.java:59-101` | Sin `base-url`; lote sin reintento; valor calculado con `Thread.sleep` y reintento de 404. |

### H4 — Núcleo de estados

| Tarea | Estado | Evidencia | Observación |
|---|---|---|---|
| T-06 Contrato completo de CU-08 | **Cumple** | `ChangeReservationStatusService.java:59-119`; `ChangeReservationStatusCommand`; `ChangeReservationStatusResult` | Comando y respuesta completos; validaciones y versión presentes. |
| T-07 TTL desde `PENDING_PAYMENT` | **Cumple** | `ReservationJpaRepository.java:40-41`; `ExpireReservationsService.java:44-61` | Ambos estados incluidos; solo el docstring de `TtlExpiryJob` quedó desactualizado. |
| T-08 Matriz de notificaciones M3/M1 | **Cumple** | `ModuleNotificationMatrix.java:33-59`; `ChangeReservationStatusService.java:70-94` | `m1_blocked` se activa al notificar `RESERVED`; destinos `AMQP` y `M1_HTTP`. |
| T-09 Persistencia de campos CU-08 | **Cumple** | `ReservationMapper.java`; migración V4 | `status_changed_at`, cierre, pago y horas reales mapeados. |
| T-10 Auditoría e inmutabilidad | **Cumple** | Trigger de V1; `ReservationStatusAuditImmutabilityTest`; `CreateReservationService.java:147-157` | Creación y transiciones auditadas; inmutabilidad probada por trigger. |

### H5 — Superficie para M3

| Tarea | Estado | Evidencia | Observación |
|---|---|---|---|
| T-12 Payload `reservation.status.changed` | **Parcial** | `FinanceStatusMapper.java:11-32`; `OutboxAdapter.java:42-50` | Nombres UC07, `Message-Id` y token solo en `PENDING`; faltan actor, subtipo, anticipación y notas que pide la spec. |
| T-13 Endpoint interno CU-15 | **Parcial** | `InternalReservationController.java:22-36`; `InternalReservationResponse.java:129,134` | 200 y solo lectura; sin 403 por rol y con campos obligatorios nulos. |

### H6 — Vistas de consulta

| Tarea | Estado | Evidencia | Observación |
|---|---|---|---|
| T-11a Detalle de embarcación CU-19 | **Cumple (código)** | `VesselDetailController.java:30-50`; `VesselDetailService`; `VesselDetailControllerTest` | `Cache-Control: no-store`, revalidación de capacidad y cotización; en `mode=http` depende del `base-url` faltante. |
| T-11b Mis reservas CU-20 | **Parcial** | `ReservationQueryController.java:27-38`; `ReservationListResponse.java:13-59`; `ReservationListItemResponse.java:15-55,138` | `filter`, paginación y `applied_filter` presentes; falta `status_badge` y el total no es el liquidado. |
| T-11c Detalle de reserva CU-21 | **Parcial** | `ReservationQueryController.java:40-55`; `ReservationDetailResponse.java:14-54`; `ReservationDetailPolicy.java:25-51` | Estructura completa y sin aritmética; falta `status_badge`; rubros nulos hasta que H7 los persista. |

---

## 5. Reglas transversales

| Regla | Resultado | Evidencia y comentario |
|---|---|---|
| **Único escritor de estado: CU-08** | **Violada** | La creación escribe el estado inicial directamente (`CreateReservationService.java:131`). TTL y cambios sí pasan por CU-08 (`ExpireReservationsService.java:29`). |
| **Sin aritmética monetaria en M2** | **Cumple (código)** | No se encontraron operaciones `add/subtract/multiply/divide/setScale` sobre `BigDecimal`; los montos se transportan y se muestran. Falta la regla ArchUnit que lo garantice (`MoneyArchitectureTest` ausente). |
| **Tiempo solo por `ClockPort`** | **Cumple con excepción justificada** | No se encontró `Instant.now()` ni `System.currentTimeMillis()` en producción. `HttpVesselReadAdapter.java:79` usa `System.nanoTime()` para un presupuesto monotónico, no para persistir tiempo. |
| **Sin caché de datos de flota** | **Cumple** | No hay `@Cacheable`, `CacheManager` ni cachés propias; M1 se consulta en cada vista. |
| **Idempotencia de creación** | **Cumple** | `CreateReservationService.java:81-87` reclama y recupera por clave; `IdempotencyAdapterTest` e `IdempotencyClaimConcurrencyTest` cubren el puerto. |
| **Correlation id HTTP → AMQP** | **Parcial** | El filtro HTTP y el publicador AMQP lo propagan si hay MDC (`RabbitEventPublisherAdapter.java:55-58`), pero el relay no reconstruye el contexto. |
| **Token de pago solo en `PENDING`** | **Cumple con deuda** | `OutboxAdapter.java:47-50` lo añade solo en `PENDING`; la purga al publicar borra todo el payload y no purga en `dead`. |
| **Llamadas externas fuera de transacción** | **Violada** | `CreateReservationService` y `ReservationQueryService` llaman a M1 dentro de la transacción (`:36/:89` y `:33/:191`). |
| **Outbox seguro en multi-réplica** | **Violada** | Sin `FOR UPDATE SKIP LOCKED`, sin lease y sin ShedLock (`OutboxMessageJpaRepository.java:14-21`). |
| **Catálogo de errores estable** | **Cumple** | `GlobalExceptionHandler` mapea `DomainException` a `{code,message}` y conserva códigos de `ErrorCode`; no se emite 403 porque no existe la comprobación de rol. |
| **Pruebas sin atajos** | **Parcial** | No hay `@Disabled`, `@Ignore` ni `Thread.sleep` en tests; los `JdbcTemplate` están en los casos documentados. La suite no se pudo ejecutar. |
| **ArchUnit de capas y dinero** | **Parcial** | Tres reglas de capas en `LayeredArchitectureTest`; no existe la regla de “sin dinero”. |

---

## 6. Precondiciones para iniciar H7

| # | Precondición | ¿Cumple? | Evidencia / acción necesaria |
|---|---|---|---|
| P-01 | Motor CU-08 completo (T-06 a T-10) | **SÍ (código)** | `ChangeReservationStatusService`, matriz, auditoría y TTL doble presentes; falta confirmación por suite. |
| P-02 | Creación pasa por CU-08 (T-24) | **NO** | `CreateReservationService.java:131` persiste directamente. |
| P-03 | Cobro tardío `EXPIRED → CANCELLED_FLEXIBLE` (CU-13) | **NO** | `ReservationStateMachine.java:22-23`; `ChangeReservationStatusService.java:159-165`. |
| P-04 | ShedLock en jobs (T-05) | **NO** | Sin dependencia ni uso; solo `TODO` en ambos jobs. |
| P-05 | Relay de outbox con reclamación segura | **NO** | `OutboxMessageJpaRepository.java:14-21` sin `SKIP LOCKED` ni lease. |
| P-06 | Endpoint interno con 403 `FINANCE` y campos completos (T-13) | **NO** | `InternalReservationController.java:22-36`; `InternalReservationResponse.java:129,134`. |
| P-07 | Payload de CU-14 conforme a la spec (T-12) | **NO** | `OutboxAdapter.java:42-50` omite actor, subtipo, anticipación y notas. |
| P-08 | `status_badge` en listado y detalle, o aceptación formal de la desviación | **NO** | `ReservationListItemResponse.java:15-55`; `ReservationDetailResponse.java:14-54`; D10 en `implementContext.md:306`. |
| P-09 | Integración M3 operable en `http` | **NO** | Falta `seashare.finance.base-url` (`application.yaml:64-74`). |
| P-10 | Reintentos y resiliencia de adaptadores M1/M3 | **NO** | `HttpFleetInfoAdapter.java:105-127`; `HttpCalculatedValueAdapter.java:92-101`; ver F-10 y F-11. |
| P-11 | Regla ArchUnit de “sin dinero” (T-22) | **NO** | `MoneyArchitectureTest` ausente. |
| P-12 | Suite completa verde y en orden inverso con Docker | **NO VERIFICABLE** | `docker info` falla; `clean verify` no ejecutado. |
| P-13 | Documentación sincronizada con HEAD | **NO** | `implementContext.md:27,41,72` contradice el código; ver sección 7. |

> **Lectura del veredicto:** P-02, P-03, P-04, P-06 y P-09 son bloqueantes funcionales u operativos. P-08 y P-12 pueden resolverse con una decisión formal y una ejecución reproducible, respectivamente, pero hoy no están satisfechas.

---

## 7. Documentos desactualizados o contradictorios

| Documento | Afirmación | Realidad verificada |
|---|---|---|
| `implementContext.md:27` | “Hito H2 completado” | `T-05` (ShedLock) y parte de `T-22` (`MoneyArchitectureTest`) no están implementados; la propia línea 37 lo reconoce. |
| `implementContext.md:41` | “CU-19 no existe” | `VesselDetailController.java` y `VesselDetailService` existen desde H6. |
| `implementContext.md:72` | “Faltan el filtro `filter` y la estructura exacta” | `filter`, `pagination` y `applied_filter` ya están en `ReservationQueryController` y `ReservationListResponse`. Solo falta `status_badge`. |
| `implementContext.md:40` y `:67` | H5 con “PR pendiente” | H5 está integrado por el merge `7ca6719` (PR #28). |
| `implementContext.md:28` y `:371` | “271 pruebas, última ejecución 2026-11-10” | `:344` declara 273 al cierre de H6; además, ninguna de las dos cifras es verificable sin Docker. |
| `implementContext.md:506` | Reconoce que `EXPIRED` no puede salir a `CANCELLED_FLEXIBLE` | Confirmado en código; debe dejar de ser una nota y convertirse en bloqueante de CU-13. |
| `implementContext.md:363` | “`seashare.finance.base-url` sigue sin definirse” | Confirmado; impide operar `mode=http`. |
| `TtlExpiryJob.java:10` | “expiración por TTL de reservas en INITIATED” | `T-07` expira también `PENDING_PAYMENT` (`ReservationJpaRepository.java:40-41`). |
| `pom.xml:170-171` | Checkstyle “SOLO reporta” | Correcto, pero contradice la expectativa de un build que verifique estilo; `failOnViolation=false` en `:179`. |

---

## 8. Elementos no verificables en esta auditoría

1. **Ejecución de la suite completa** (`clean verify`) y de la suite en orden inverso: Docker no está disponible.
2. **Conteos reales de pruebas** (152/271/273): solo existen como declaraciones documentales.
3. **Pruebas de integración con PostgreSQL y RabbitMQ**: requieren contenedores.
4. **Comportamiento multi-réplica** de TTL y relay: no hay entorno con dos instancias.
5. **Recuperación tras reinicio del broker** y reintentos reales de AMQP: requiere RabbitMQ.
6. **Contrato efectivo de M1 y M3**: no se dispone de servicios externos ni de esquemas firmados.
7. **Latencia real y timeouts** de M1/M3: no medidos.
8. **Arranque con `--spring.profiles.active=dev|prod`**: no ejecutado en esta auditoría.
9. **Migraciones aplicadas sobre una base limpia**: no ejecutadas; solo se verificó su existencia y orden V1–V5.
10. **Cobertura de código**: no se generó informe de cobertura.

---

## 9. Orden recomendado de corrección

Sin aplicar cambios en esta auditoría; el orden prioriza desbloquear H7 con el menor riesgo de retrabajo.

1. **Cerrar `T-24` (creación vía CU-08).** Es la deuda estructural que permite que cualquier flujo futuro escriba estados fuera del motor. Debe incluir la prueba `creationGoesThroughStatusUseCase` y mantener la auditoría de creación.
2. **Habilitar CU-13 y la transición tardía.** Definir y probar `EXPIRED → CANCELLED_FLEXIBLE` con subtipo `LATE`/`FLEXIBLE` según contrato, sin romper la inmutabilidad de los demás terminales. Es precondición de `T-16c`.
3. **Implementar `T-05` y la reclamación del outbox.** Añadir ShedLock a TTL y relay, y `FOR UPDATE SKIP LOCKED` o lease equivalente en `findPending`. Sin esto, H7 añade un job de polling sobre una base ya insegura en multi-réplica.
4. **Completar `T-13`.** Validar rol `FINANCE` y devolver 403; completar `cancellation_anticipation_hours` y `damage_detected` o documentar formalmente su ausencia con contrato alternativo.
5. **Definir `seashare.finance.base-url` y la política HTTP de M3.** Configurar el valor por entorno y añadir reintentos acotados sin `Thread.sleep` en el hilo de petición. Verificar `mode=http` con WireMock.
6. **Alinear `T-12` con la spec de CU-14 o formalizar la omisión.** Si M3 necesita actor, subtipo, anticipación y notas, incluirlos; si no, dejar la decisión por escrito y ajustar la spec.
7. **Resolver `status_badge` (T-11b/T-11c).** Implementarlo o registrar la aceptación de producto de D10; no dejarlo como deuda implícita.
8. **Añadir `MoneyArchitectureTest` y completar ArchUnit.** Debe cubrir aritmética monetaria, dependencias de capas y prohibición de escrituras de estado fuera de CU-08.
9. **Corregir la purga del token y la trazabilidad del relay.** No borrar todo el payload al publicar; purgar solo `payment_token_ref`; propagar correlation id desde el origen o generar uno por mensaje.
10. **Sanear configuración y documentación.** `source/target` 21, Checkstyle bloqueante o decisión explícita, `.idea` fuera del repositorio, docstring de TTL y cifras de suite sincronizadas.
11. **Repetir la verificación completa.** `.\mvnw.cmd clean verify` y `.\mvnw.cmd test "-Dsurefire.runOrder=reversealphabetical"` con Docker operativo; registrar fecha, conteo y resultado. Solo entonces reevaluar el veredicto.

---

**Fin del informe.** No se aplicaron correcciones, commits ni cambios de estado del repositorio.
