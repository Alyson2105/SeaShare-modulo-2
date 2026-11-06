# contextom2

## Resumen del proyecto
- `com.seashare:m2` — Spring Boot 4.1.2, Java 21, Maven. Punto de entrada: `com.seashare.m2.M2Application`.
- Arquitectura hexagonal bajo `src/main/java/com/seashare/m2/reservas/`: `domain` → `application/{port/in,port/out,service,dto}` → `infrastructure/adapter/{in,out}`. `LayeredArchitectureTest` (ArchUnit) falla ante violaciones de capas.
- Implementado hoy: crear reserva (`POST /api/v1/reservations`), listado/detalle (`GET`), transiciones de estado mediante `ChangeReservationStatusUseCase` y el job de expiración por TTL. Todavía no existe un endpoint REST para cambios de estado.
- Flyway pre-crea tablas para muchos casos de uso aún no implementados (`payment_attempt`, eventos de cancelación/no-show/check-in/out, `dispute_*`, etc.). No asumir que una funcionalidad existe solo porque exista su tabla. Solo el contexto `reservas` está implementado; `disputa` y `shared/contracts` son diseño objetivo en `Documentación/technical-plan/plan.md`.
- Los adaptadores de flota son falsos por defecto (`seashare.fleet.mode=fake`, `matchIfMissing=true`); el cliente HTTP real del Módulo 1 no está implementado.
- El outbox actualmente solo inserta filas. No existe relay, publicador RabbitMQ ni listener, a pesar de la configuración AMQP.

## Comandos
- Windows: `.\mvnw.cmd ...`; Unix: `./mvnw ...`.
- Levantar primero la infraestructura: `docker compose up -d` (Postgres en `localhost:5433`, RabbitMQ en `5672`/`15672`). `docker compose down -v` borra los datos locales.
- Compilar: `.\mvnw.cmd clean package`; ejecutar: `.\mvnw.cmd spring-boot:run`; todos los tests: `.\mvnw.cmd test`.
- Una clase/método: `.\mvnw.cmd test -Dtest=CreateReservationTest` o `.\mvnw.cmd test -Dtest=ReservationTest#methodName`.
- Todo `@SpringBootTest` (contexto, web, persistencia, integración) necesita el Postgres real; no hay H2 ni Testcontainers. Los tests unitarios puros no lo necesitan.
- La mayoría de los tests de integración son `@Transactional` y hacen rollback; `IdempotencyClaimConcurrencyTest` confirma filas deliberadamente.

## Particularidades de ejecución y configuración
- La URL de la BD es `jdbc:postgresql://localhost:5433/seashare_m2` (5433, no 5432), usuario/contraseña `seashare`. Las credenciales en `application.yaml` son solo para desarrollo local.
- Flyway es dueño del esquema (`spring.jpa.hibernate.ddl-auto: validate`). Nunca editar un `V*.sql` ya aplicado: se rompe el checksum; agregar una nueva migración `V4__...sql`.
- El job de TTL corre cada 30 s por defecto. Desactivarlo en los tests que no lo necesiten: `@SpringBootTest(properties = "m2.scheduler.ttl.enabled=false")`.
- Obtener la hora desde `ClockPort.now()`, nunca `Instant.now()`, para poder simular el tiempo en tests.
- `ReservationRepositoryAdapter.save()` hoy persiste únicamente el estado y el subtipo de cancelación (`ReservationMapper.applyChanges`). Los nuevos campos mutables requieren mapeo explícito.
- Agregar un estado/transición exige cambios coordinados en `ReservationStatus`, `ReservationStateMachine`, restricciones CHECK/migraciones de BD y `ChangeReservationStatusService.OUTBOX_STATUSES` si debe emitir eventos.
- `Idempotency-Key` debe ser un UUID. `IdempotencyAdapter.tryClaim` usa un upsert nativo atómico; mantenerlo (insert simple + catch aborta la transacción de Postgres).
- La identidad web es temporal basada en headers: `X-User-Id` (UUID) y `X-User-Role`; no hay Spring Security. `POST` solo permite `RENTER`.
- El JSON de la API es snake_case; los errores son `{ "code", "message" }` a partir de `ErrorCode` (que mapea los internos `INVALID_STATE_TRANSITION`/`VERSION_CONFLICT` a códigos públicos 409).
- Spring Boot 4 usa Jackson 3: usar `tools.jackson.databind.*` para `ObjectMapper`; las anotaciones siguen en `com.fasterxml.jackson.annotation.*`. `AutoConfigureMockMvc` para tests está en `org.springframework.boot.webmvc.test.autoconfigure`.

## Convenciones y documentación
- Los identificadores de código, campos de API y mensajes de excepción van en inglés; los comentarios de código y la documentación van en español. Los nombres de tests mezclan español e inglés.
- Los specs son la fuente de verdad: `Documentación/features/CU-XX-*/spec.md` y `plan.md`; las reglas de negocio en `Documentación/context/sea-share.md`. `technical-plan/plan.md` describe la arquitectura objetivo, no necesariamente el estado actual.
- Regla de dinero: nunca calcular, redondear, sumar ni convertir importes; almacenar y transportar `BigDecimal` exactamente como se recibe.

## Contexto real del proyecto (fuente: `Documentación/`)

### Propósito, dominio y alcance
- **SEA-SHARE** es un marketplace P2P de alquiler náutico. Este repositorio implementa el **Módulo 2: Operación de Reservas, Tiempos y Cancelaciones**, dueño del ciclo de vida de la reserva y de sus ventanas temporales.
- Fronteras del dominio: **Módulo 1** (Gestión de Flota y Activos P2P) es la única fuente de verdad de la flota; Módulo 2 solo persiste referencias (`vessel_id`, `owner_id`) y nunca accede a su base de datos ni cachea sus datos. **Módulo 3** (Liquidación, Seguros y Dispersión de Fondos) es la autoridad única sobre el dinero; Módulo 2 nunca calcula, suma, redondea ni convierte importes.
- El alcance documentado son los casos de uso **CU-01 a CU-22** en `Documentación/features/`. Es un backend: la UI queda fuera del alcance según el plan general.
- El plan general propone un único servicio desplegable con dos contextos acotados, `reservas` y `disputa`; la bitácora de progreso solo certifica trabajo del contexto `reservas` (ver “Estado de implementación documentado”).
- La documentación de contexto, features, diagramas y templates se trata como **solo lectura**; los specs son la fuente de verdad y el flujo de trabajo es SDD: `spec.md` define el qué y `plan.md` el cómo (`Documentación/templates/sdd-guide.MD`). Ante un `[NEEDS CLARIFICATION]` abierto, no se debe implementar.

### Actores y lenguaje ubicuo
- Actores de negocio: **Arrendatario**, **Propietario** y **Admin** (este último solo para resolver disputas, sin introducir montos).
- En documentación de Finanzas, **“el sistema”** designa a Módulo 3; **“Sistema de Reservas y Operaciones”** designa a Módulo 2. Evitar “Módulo 3” dentro del vocabulario financiero.
- Usar **Arrendatario** y **Propietario**; `consistencia-m2-m3.md` descarta los términos “turista” y “anfitrión” para el vocabulario compartido.
- Convención del proyecto (D-23): identificadores de código y API en inglés; prosa y documentación en español.

### Reglas de negocio que gobiernan la implementación
- La reserva nace en `INITIATED` (“Iniciada”) y arranca un **TTL de 15 minutos**; el TTL no se reinicia al pasar a `PENDING_PAYMENT`. La reserva puede expirar desde `INITIATED` y, según CU-08, también desde `PENDING_PAYMENT`.
- Varias reservas en `INITIATED` pueden coexistir para el mismo barco y fechas. La exclusividad se resuelve al pasar a `PENDING_PAYMENT` con criterio **FCFS** y control atómico; el perdedor recibe HTTP 409. La restricción de base de datos impide solapamientos en `PENDING_PAYMENT`, `RESERVED` e `IN_NAVIGATION`.
- Solo al alcanzar `RESERVED` se bloquea el inventario en Módulo 1. `INITIATED` y `PENDING_PAYMENT` no notifican a Módulo 1; `EXPIRED` y `PAYMENT_FAILED` nunca lo hacen; `CANCELLED` solo si la reserva había llegado antes a `RESERVED`.
- La cancelación activa solo se admite desde `RESERVED` (no desde `INITIATED` ni `PENDING_PAYMENT`). Reglas: Flexible (>72 h) reembolso 100 %; Moderada (72–24 h) reembolso 50 %; Tardía/No-Show (<24 h) 100 % al Propietario; cancelación por el Propietario reembolso 100 % al Arrendatario.
- La inasistencia se puede marcar tras **30 minutos** de tolerancia desde la hora pactada.
- Estados principales: `INITIATED`, `PENDING_PAYMENT`, `RESERVED`, `IN_NAVIGATION`, `COMPLETED`, `CANCELLED`, `EXPIRED`, `PAYMENT_FAILED`. Subestados de cancelación: `FLEXIBLE`, `MODERATE`, `LATE`, `BY_OWNER`, `NO_SHOW`. Los cuatro estados finales son inmutables.
- Las ventanas temporales se evalúan en la **zona horaria del puerto** informada por Módulo 1, nunca en UTC del servidor ni en la del dispositivo. Si Módulo 1 no entrega una zona válida, la operación debe fallar de forma segura (no hay zona por defecto).
- El depósito de garantía es el 10 % de la tarifa base diaria, se retiene tras `COMPLETED` y se resuelve en una ventana fija de 24 h. Módulo 2 gestiona la disputa (`PENDING` / `REJECTED` / `ACCEPTED`); Módulo 3 ejecuta la consecuencia financiera usando sus propios registros. El depósito se entrega completo a una de las partes, nunca en parte.
- Toda notificación saliente relevante usa **outbox transaccional** e idempotencia por `event_id`; la entrega es *at-least-once* y el receptor deduplica. Los eventos hacia Módulo 3 no llevan montos ni instrucciones de pago.
- El contrato de error de las APIs es `{ code, message }`; los códigos y su mapeo HTTP están en el plan general y en `ErrorCode`.

### Integraciones y contratos documentados
- **Módulo 1**: lecturas REST de información de embarcación y estado operativo; notificación del estado operativo mediante outbox (ver contradicción sobre el transporte más abajo). No confundir **CU-10** (Módulo 2 pregunta a Módulo 1) con **CU-22** (Módulo 2 notifica a Módulo 1).
- **Módulo 3**: CU-11 consulta estimaciones en lote/individual; CU-12 solicita el valor calculado; CU-13 tiene el canal B activo (Módulo 2 consulta a Módulo 3) y el canal A apagado (endpoint de Módulo 3 hacia Módulo 2); CU-15 es una consulta de Módulo 3 hacia Módulo 2.
- **Eventos AMQP hacia Módulo 3**: exchange `seashare.reservations`; `reservation.status.changed` hacia `finance.reservation-status.v1` (CU-14) y `reservation.dispute.updated` hacia `finance.guarantee-dispute.v1` (CU-18).
- **CU-22 / Módulo 1**: el estado operativo se envía ya mapeado (`AVAILABLE`, `RESERVED`, `IN_NAVIGATION`, `MAINTENANCE_CLEANING`), nunca el estado crudo de la reserva. Los nombres de exchange, routing key y cola de Módulo 1 siguen pendientes de definir en `consistencia-m2-m1.md`.
- El plan general exige reintentos con backoff (máximo 5, 1/5/25/125 s), DLQ y alerta; solo se reintenta ante 5xx o timeout, nunca ante 4xx permanente.

### Estado de implementación documentado
- `Documentación/context/implementContext.md` (2026-11-03) certifica: recuperación/verificación de tests de idempotencia y scheduler de expiración TTL implementado, con CU-02 integrado al TTL y 75 pruebas sin fallos. Deja como deuda ShedLock (D-15) y la expiración de reservas en `PENDING_PAYMENT`.
- La nota de actualización del plan general (2026-10-25) fija el estado real del stack: Spring Boot 4.1.2, Java 21, Maven, **sin Spring Security**, identidad temporal por headers, Jackson 3 y Flyway con starter de PostgreSQL.
- La bitácora no registra el avance de todas las fases del plan general ni certifica la implementación de todos los CU; los specs y planes restantes describen alcance objetivo. La sección técnica de este archivo refleja además el estado verificado del código.

### Concordancias y contradicciones detectadas
- **Plan general vs. nota 2026-10-25**: el texto inicial del plan menciona Spring Boot 3.5.x, Spring Security/JWT, ShedLock, Testcontainers, WireMock y Resilience4j. La nota posterior corrige el stack a Spring Boot 4.1.2 y “sin Spring Security”, y pide verificar las versiones de las demás librerías. Prevalece la nota; coincide con la sección técnica de este archivo.
- **CU-08 vs. `implementContext.md`**: el spec de CU-08 exige expiración por TTL desde `INITIATED` **y** `PENDING_PAYMENT`; la bitácora documenta que la implementación actual solo barre `INITIATED` y deja `PENDING_PAYMENT` como pendiente. No se debe asumir que esa rama ya existe.
- **Migraciones**: la nota del plan dice que solo V1 y V2 están aplicadas; el repositorio ya contiene V3. Prevalece el estado del repositorio (V1–V3); las migraciones aplicadas no se editan.
- **Transporte de la notificación a Módulo 1**: `consistencia-m2-m1.md` lo define como AMQP/RabbitMQ; el plan general lo describe como outbox hacia `PATCH /internal/v1/vessels/{vessel_id}/status` por HTTP (`M1_HTTP`). La documentación no resuelve la discrepancia; no asumir un transporte sin confirmarlo.
- **Conteo de casos de uso**: el plan general alterna entre 21 y 22 CU; el índice real de `Documentación/features/` contiene CU-01 a CU-22. Tomar el índice como referencia.
- **Elementos planificados no presentes en el repo**: el plan general lista `Dockerfile`, `README.md` y perfiles `application-dev/test/prod`; el repositorio actual no los contiene. Son objetivos del plan, no estado actual.