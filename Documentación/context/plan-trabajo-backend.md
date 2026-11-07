# Plan de trabajo auditado: backend Módulo 2 (M2)

**Versión**: 2 (corrección de la v1; registro de cambios al final)
**Fecha de auditoría**: 2026-11-06
**Rama**: feature/fase1-setup (salida de `git branch --show-current`; ahead 1 respecto a origin, ver A.4)
**Alcance**: CU-01 a CU-22 (rango de `Documentación/features/`; ver sección L, inconsistencia 1)
**Entrega**: sábado 7 de noviembre de 2026
**Responsables**: por asignar

> Convención de este documento: `[NEEDS CLARIFICATION]` marca un dato que los documentos no cierran. Las rutas son relativas a la raíz del repositorio. "L" seguido de número indica línea del archivo citado.
> Lectura: se leyeron completos `contexto_m2.md`, `implementContext.md`, `technical-plan/plan.md`, `features/CU-22-*/spec.md` y `plan.md`, `consistencia-m2-m1.md`, `CU-02/plan.event.md` (no leído, ver M). El resto de specs se consultó solo por búsqueda puntual y se marca en M.

---

## A. Estado real auditado

### A.1 Tabla por CU

Documentación: existencia verificada con listado de `Documentación\features\`. Código: solo se marca si existe clase o endpoint. Estado según `implementContext.md` (L = línea en ese archivo, sección 3, líneas 52-73).

| CU | Documentación (existencia) | Código (evidencia) | Tests (evidencia) | Estado en implementContext.md | Estado real | Coincide |
|---|---|---|---|---|---|---|
| CU-01 Buscar embarcaciones | spec.md, plan.md | Sin endpoint de búsqueda. Controladores existentes: `ReservationController.java` L21-31 y `ReservationQueryController.java` L18-56 | Ninguno | L52 No implementado | PENDIENTE | Sí |
| CU-02 Iniciar reserva | spec.md, plan.md, plan.event.md | `ReservationController.java` L31 (`@PostMapping`); `CreateReservationService.java` L129 (encola evento; entró en commit `96e7568`, A.4) | `CreateReservationTest.java` L45; `CreateReservationOutboxIntegrationTest.java` L48 | L53 Parcial | PARCIAL | Sí |
| CU-03 Iniciar pago | spec.md, plan.md | Sin endpoint ni transición a PENDING_PAYMENT por CU-03 | Ninguno | L54 No implementado | PENDIENTE | Sí |
| CU-04 Solicitar cancelación | spec.md, plan.md | Sin clase. Solo tabla `cancellation_event` en `V2__remaining_tables.sql` L28 | Ninguno | L55 No implementado | PENDIENTE | Sí |
| CU-05 Marcar inasistencia | spec.md, plan.md | Sin clase. Solo tabla `noshow_event` en V2 L39 | Ninguno | L56 No implementado | PENDIENTE | Sí |
| CU-06 Marcar inicio navegación | spec.md, plan.md | Sin clase. Solo tabla `checkin_event` en V2 L48 | Ninguno | L57 No implementado | PENDIENTE | Sí |
| CU-07 Marcar fin navegación | spec.md, plan.md | Sin clase. Solo tabla `checkout_event` en V2 L56 | Ninguno | L58 No implementado | PENDIENTE | Sí |
| CU-08 Actualizar estado | spec.md, plan.md | `ChangeReservationStatusService.java` L28 (`@Service`); `ReservationStateMachine.java` L11-42 | `ReservationStateMachineTest.java`; `ChangeReservationStatusServiceTest` (integration) | L59 Parcial | PARCIAL | Sí |
| CU-09 Información de embarcación | spec.md, plan.md | Solo puerto `FleetInfoPort` y `FakeFleetInfoAdapter.java` L13 | Ninguno específico | L60 No implementado | PENDIENTE (solo fake) | Sí |
| CU-10 Estado operativo (M1 → M2) | spec.md, plan.md | Solo puerto `FleetOperationalStatusPort` y `FakeFleetOperationalStatusAdapter.java` L11 | Ninguno | L61 No implementado | PENDIENTE (solo fake) | Sí |
| CU-11 Cotización (M3) | spec.md, plan.external.individual.md, plan.external.lote.md (no hay plan.md) | Sin cliente M3 | Ninguno | L62 No implementado | PENDIENTE | Sí |
| CU-12 Cálculo total (M3) | spec.md, plan.md | Sin cliente M3 | Ninguno | L63 No implementado | PENDIENTE | Sí |
| CU-13 Confirmar pago | spec.md, plan.md, plan.external.md | Sin código. Solo tabla `payment_attempt` (V2 L14) | Ninguno | L64 No implementado | PENDIENTE | Sí |
| CU-14 Recibir estado (publicar a M3) | spec.md, plan.md | Se encola en outbox: `OutboxAdapter.java` L30 (`enqueue(StatusChangedEvent)`). No hay relay ni publicador (ver A.2) | Ninguno de publicación | L65 No implementado | PENDIENTE | Sí |
| CU-15 Consulta de M3 a M2 | spec.md, plan.md | Sin endpoint interno | Ninguno | L66 No implementado | PENDIENTE | Sí |
| CU-16 Generar disputa | spec.md, plan.md | Solo tablas `dispute_guarantee` y `dispute_claim` (V2 L89, L104) | Ninguno | L67 No implementado | PENDIENTE | Sí |
| CU-17 Actualizar disputa | spec.md, plan.md | Solo tabla `admin_review` (V2 L114) | Ninguno | L68 No implementado | PENDIENTE | Sí |
| CU-18 Recibir info de disputa | spec.md, plan.md | Sin contexto `disputa` (no hay carpeta en `src/main/java/com/seashare/m2/`) | Ninguno | L69 No implementado | PENDIENTE | Sí |
| CU-19 Ver detalle embarcación | spec.md, plan.md | Sin endpoint | Ninguno | L70 No implementado | PENDIENTE | Sí |
| CU-20 Ver mis reservas | spec.md, plan.md | `ReservationQueryController.java` L43 (`@GetMapping`) | `ReservationQueryTest.java` | L71 Parcial | PARCIAL | Sí |
| CU-21 Ver detalle de reserva | spec.md, plan.md | `ReservationQueryController.java` L56 (`@GetMapping("/{id}")`) | `ReservationQueryTest.java` | L72 Parcial | PARCIAL | Sí |
| CU-22 Notificar estado operativo a M1 | spec.md, plan.md, `consistencia-m2-m1.md` (raíz de context) | Sin publicador ni cola hacia M1 | Ninguno | L73 No implementado | PENDIENTE | Sí |

Nota: la asociación clase ↔ CU de CU-02, CU-08, CU-20 y CU-21 proviene de `implementContext.md` L94, L98, L102; en el código no hay comentario "CU-xx" en esas clases.

### A.2 Piezas transversales

| Pieza | Estado | Ruta que lo prueba |
|---|---|---|
| Migraciones | V1, V2 y V3 existen | `src/main/resources/db/migration/V1__init.sql`, `V2__remaining_tables.sql`, `V3__reservation_fields_and_outbox.sql` |
| Job de expiración TTL | Implementado solo para INITIATED | `TtlExpiryJob.java`, `SchedulerConfig.java` (`infrastructure/adapter/in/scheduler`), `ExpireReservationsService.java` L44-46 |
| Adaptadores fake de flota | Activos por defecto | `FakeFleetInfoAdapter.java` L13, `FakeFleetOperationalStatusAdapter.java` L11 (`matchIfMissing = true`) |
| Manejo de errores | Implementado | `GlobalExceptionHandler.java`; `shared/domain/ErrorCode.java` L8-28 |
| Outbox (escritura) | Solo inserta filas | `OutboxAdapter.java` L17 (EXCHANGE), L30, L57; `OutboxMessageEntity.java` |
| Relay outbox → RabbitMQ | NO EXISTE | Búsqueda en `src/`: sin resultados para `RabbitTemplate`, `AmqpTemplate`, `RabbitListener`, `relay`, `publisherConfirm`, `OutboxRelay` |
| Publicador/consumidores AMQP | NO EXISTEN | Mismo resultado anterior |
| Idempotencia de entrada | Implementado | `IdempotencyAdapter.java` L28 (`tryClaim`) |
| Reloj | Implementado | `shared/domain/ClockPort.java` L7; `SystemClockAdapter.java` L20 |
| Reglas ArchUnit | Existe clase de prueba | `src/test/java/com/seashare/m2/reservas/architecture/LayeredArchitectureTest.java`; `pom.xml` L97 (`archunit-junit5`) |

### A.3 Git (salida literal)

```
git branch --show-current
feature/fase1-setup

git --no-pager status -sb
## feature/fase1-setup...origin/feature/fase1-setup [ahead 1]
 M .idea/misc.xml
 D .idea/modules.xml
?? CLAUDE.md
?? "Documentaci\303\263n/context/plan-trabajo-backend.md"
?? [linea de carpeta ignorada por instruccion]
?? untitled/

git --no-pager log --oneline -10
ca1ef9b feat: agregar CALCULATION_UNAVAILABLE (503) al catalogo de errores
1972540 fix: resolución de conflictos por implementContext.md
0547b18 docs: Creación de documento de contexto general de módulo 2. Modificación del implementContext
19d6118 docs: alinear contratos M2-M3 (boat_id, COMPLETED/REJECTED, UTC)
a9a3afd fix: truncar el reloj a milisegundos para estabilizar status_changed_at
96e7568 feat: encolar reservation.info.provided en el outbox al crear la reserva
7aab976 fix: vaciar el contexto de persistencia solo despues de hacer flush en la consulta de idempotencia
ae4da11 feat(reservas): implementar scheduler de expiración de reservas por TTL (Parte B)
ad1b4aa test(reservas): verificar y consolidar tests de idempotencia recuperados (Parte A)
02b17ff doc: creación contexto de progreso de implementación
```

Esta auditoría no modificó ningún archivo. La línea de carpeta ignorada se omite del documento; la salida completa aparece en el chat.

### A.4 Verificaciones de la Fase 1 (salida literal, v2)

- **0.2** `git --no-pager log --oneline -S "ReservationInfoProvidedEvent" -- src` → `96e7568 feat: encolar reservation.info.provided en el outbox al crear la reserva`. Esa es la fecha de entrada del encolado de CU-02.
- **0.3** Conteo de constantes en `shared/domain/ErrorCode.java` L8-L28 con patrón `^\s+[A-Z_]+\("[A-Z_]+", \d+\)`: **21**. Lista: INVALID_PARAMETERS 400; INVALID_UUID 400; UNAUTHENTICATED 401; PROFILE_NOT_AUTHORIZED 403; RESERVATION_NOT_OWNED 403; RESERVATION_NOT_FOUND 404; VESSEL_NOT_FOUND 404; INVALID_RESERVATION_STATE 409; CONCURRENT_STATE_CHANGE 409; CONCURRENCY_CONFLICT 409; CAPACITY_EXCEEDED 409; VESSEL_NOT_AVAILABLE 409; VESSEL_CURRENTLY_IN_NAVIGATION 409; RESERVATION_EXPIRED 409; RESERVATION_EXPIRED_REVERSION_REQUIRED 409; DISPUTE_ALREADY_RESOLVED 409; INTERNAL_ERROR 500; FLEET_SERVICE_UNAVAILABLE 503; CALCULATION_UNAVAILABLE 503; HARBOR_TIMEZONE_UNAVAILABLE 503; QUOTATION_UNAVAILABLE 503.
- **0.4** `IdempotencyKeyJpaRepository.java` L24: `@Modifying(flushAutomatically = true, clearAutomatically = true)`, sobre el método `complete` (L25-L26). Coincide con `implementContext.md` L155.
- **0.5** `.\mvnw.cmd test` con Docker activo (`docker info`: 29.9.2; contenedores `seashare-m2-postgres` y `seashare-m2-rabbitmq` en estado Up). Resultado: **Tests run: 83, Failures: 0, Errors: 0, Skipped: 0; BUILD SUCCESS**.
- La cifra de 75 pruebas de `implementContext.md` L163 está desactualizada respecto de 0.5 (ver L, inconsistencia 6).

---

## B. Afirmaciones por verificar

| Id | Afirmación | Veredicto | Evidencia |
|---|---|---|---|
| a | Hay máquina de estados y catálogo de 20 códigos de error | **PARCIAL**. Máquina de estados: CONFIRMADA. Catálogo: NO CONFIRMADA, son **21** constantes (conteo real de 0.3, A.4) | Máquina: `ReservationStateMachine.java` L11-42. Catálogo: `shared/domain/ErrorCode.java` L8-28 (21 constantes contadas con patrón; las notas de "20" no coinciden) |
| b | Hay CU-02, CU-08, CU-20 y CU-21 en código | **CONFIRMADA** | CU-02: `ReservationController.java` L31, `CreateReservationService.java` L129. CU-08: `ChangeReservationStatusService.java` L28. CU-20: `ReservationQueryController.java` L43. CU-21: `ReservationQueryController.java` L56 |
| c | El job de expiración solo cubre INITIATED | **CONFIRMADA** | `ReservationJpaRepository.java` L36 (`r.status = 'INITIATED'`); `ExpireReservationsService.java` L46 y L44 (comentario). `implementContext.md` L111, L116. Falta PENDING_PAYMENT según `contexto_m2.md` L77 (cita de spec CU-08, no leído directamente) |
| d | NO existe relay del outbox a RabbitMQ | **CONFIRMADA** | Búsqueda en `src/` sin resultados para `RabbitTemplate`, `AmqpTemplate`, `RabbitListener`, `relay`. `OutboxAdapter.java` L30 y L57 solo encolan. `contexto_m2.md` L9 y `implementContext.md` L157 coinciden |
| e | M1 está simulado con clases fake | **CONFIRMADA** | `FakeFleetInfoAdapter.java` L13 y `FakeFleetOperationalStatusAdapter.java` L11 (`matchIfMissing = true`). `implementContext.md` L22 |
| f | Traducción de estados a M3 existe o no existe | **NO EXISTE EN CÓDIGO; SÍ EN DOCUMENTO**. Mapeo documentado: `PENDING_PAYMENT` → `PENDING`; `CANCELLED` con subtipo: `FLEXIBLE` → `CANCELLED_FLEXIBLE`, `MODERATE` → `CANCELLED_MODERATE`, `LATE` → `CANCELLED_STRICT`, `BY_OWNER` → `CANCELLED_BY_HOST`, `NO_SHOW` → `CANCELLED_STRICT`. Pregunta resuelta por el documento: **sí**, LATE y NO_SHOW van a CANCELLED_STRICT | Documento: `features/CU-14-recibir-estado-reserva/plan.md` L53 (PENDING), L57-61 (CANCELLED_*). Código: sin resultados en `src/` para `CANCELLED_(STRICT\|FLEXIBLE\|MODERATE\|BY_HOST)`. Conflicto sobre PENDING_PAYMENT: ver sección L, inconsistencia 4 |
| g | CU-02 encola `reservation.info.provided` en el outbox | **CONFIRMADA** | `CreateReservationService.java` L129 (`outboxPort.enqueue(new ReservationInfoProvidedEvent(...))`); `OutboxAdapter.java` L57. Entró en el historial en commit `96e7568` (`git log -S`, A.4). Destino AMQP según `implementContext.md` L153 (no verificado en código) |
| h | M1 se notifica por REST (PATCH) o por AMQP | **NO RESUELTO (contradicción en documentos)**. No se elige transporte | AMQP: `consistencia-m2-m1.md` L9 y L166; `features/CU-22-*/plan.md` L7 y L48 (Protocolo AMQP 0-9-1, publish a exchange). PATCH: `technical-plan/plan.md` L229 (`PATCH /internal/v1/vessels/{vessel_id}/status`, destino `M1_HTTP`) y `features/CU-22-*/plan.md` L279 ("el último salto es HTTP PATCH") |

---

## C. Plan por fases

Fuente de las fases: `technical-plan/plan.md` L349-L459 (Phase 1 a Phase 10) y `implementContext.md` L33-L44 (estado por fase). Orden: `technical-plan/plan.md` L477-L486.

| Fase | Contenido | Estado real | Dependencia que justifica el orden |
|---|---|---|---|
| 1 Setup | `plan.md` L353-L362 | Parcial (`implementContext.md` L35) | Sin dependencias (`plan.md` L477) |
| 2 Fundación | `plan.md` L370-L384 | Parcial (`implementContext.md` L36) | Depende de Fase 1 (`plan.md` L478); bloquea a todas las CU |
| 3 Lectura M1/M3 (CU-09..12) | `plan.md` L391-L393 | No iniciada (`implementContext.md` L37) | Depende de Fase 2 (`plan.md` L479) |
| 4 Núcleo CU-08 | `plan.md` L401-L403 | Parcial (`implementContext.md` L38) | Depende de Fase 2 (`plan.md` L480) |
| 5 Superficie M3 (CU-14, CU-15) | `plan.md` L411-L413 | No iniciada (`implementContext.md` L39) | Depende de Fases 2 y 4 (`plan.md` L481) |
| 6 Vistas (CU-19, 20, 21) | `plan.md` L421-L423 | Parcial (`implementContext.md` L40) | Depende de Fases 2 y 3 (`plan.md` L482) |
| 7 Embudo (CU-01, 02, 03, 13) | `plan.md` L431-L433 | Parcial (`implementContext.md` L41) | Depende de Fases 3, 4, 5 y 6 (`plan.md` L483) |
| 8 Muelle (CU-06, 07, 05, 04) | `plan.md` L441-L443 | No iniciada (`implementContext.md` L42) | Depende de Fases 3, 4, 5 y 6 (`plan.md` L484) |
| 9 Disputa (CU-16, 17, 18) | `plan.md` L451-L453 | No iniciada (`implementContext.md` L43) | Depende de Fase 2 y de CU-07 (`plan.md` L485) |
| 10 Polish | `plan.md` L461-L469 | No iniciada (`implementContext.md` L44) | Depende de las CU deseadas (`plan.md` L486) |

Pendientes de la bitácora que no están en las fases de `plan.md` con su mismo nombre: `implementContext.md` L177-L211 (deuda técnica 1 a 21). Se usan como base de la tabla D.

---

## D. Tabla de tareas

### Definición de terminado (común a todas las tareas)

Una tarea está terminada cuando se cumplen todos estos puntos:

1. Compila: `.\mvnw.cmd -q compile`.
2. Tests de la tarea en verde. Los tests propios se completan en Fase 2 (ver criterio en la tabla).
3. Suite completa en verde: `.\mvnw.cmd test` (último resultado registrado: 83 tests, BUILD SUCCESS, A.4).
4. `git --no-pager status --short` solo muestra las rutas esperadas de la tarea.
5. Commit con rutas explícitas (`git add <ruta> <ruta>`). Nunca `git add .` ni `git add -A`.
6. Mensaje de commit en español con prefijo `feat:`, `fix:`, `chore:` o `docs:`.

Fuente: instrucción del usuario para la v2 (no es un archivo del repositorio). Los documentos no definen esta lista. Ver H.

Notas de columnas:
- "Tamaño" (S, M, L) es estimación relativa de este documento; no viene de ningún archivo. Úsese como orientación.
- Donde la tarea depende de una decisión abierta, el criterio de aceptación es `[NEEDS CLARIFICATION]`.
- Antes de iniciar cualquier tarea con CU, leer `spec.md` y `plan.md` de ese CU (no leídos todavía, ver M).

| ID | Fase | Nombre corto | CU o sección (ruta) | Archivos o paquetes que toca | Depende de | Tamaño | Responsable | Criterio de aceptación | Imports o dependencias a revisar (Boot 4) | Commit sugerido |
|---|---|---|---|---|---|---|---|---|---|---|
| T-01 | 1 | Cerrar las decisiones del grupo (ver sección "Decisiones del grupo") | Sección nueva "Decisiones del grupo" | Solo documentos | Ninguna | S | por asignar | Ver Definición de terminado + [NEEDS CLARIFICATION: tests propios, se completan en Fase 2] | No aplica | `docs: registrar decisiones del grupo` |
| T-02 | 2 | Topología RabbitMQ (M3 primero; M1 tras D-1 y D-2) | `technical-plan/plan.md` L374 (T015); `technical-plan/plan.md` L223 (exchange M3) | Nueva, esperada según `technical-plan/plan.md` L67 y L374; usa `OutboxAdapter.java` L17 (EXCHANGE) | T-01 (solo para la parte M1) | M | por asignar | Ver Definición de terminado + [NEEDS CLARIFICATION: tests propios, se completan en Fase 2] | `spring-boot-starter-amqp` (`pom.xml` L40) | `feat: definir topologia rabbitmq para integracion` |
| T-03 | 2 | Relay del outbox con publisher confirms | `implementContext.md` L186 (deuda 5); `technical-plan/plan.md` L375-L376 (T016, T017) | Nueva, esperada según `implementContext.md` L186; reutiliza `OutboxAdapter.java`, `OutboxMessageEntity.java`, V3 L26-L27 (`dead`, `last_error`) | T-02; D-5 (4xx) para la política de DLQ | L | por asignar | Ver Definición de terminado + [NEEDS CLARIFICATION: tests propios, se completan en Fase 2]. Mínimo documentado: backoff 1/5/25/125 s y 5 intentos (`technical-plan/plan.md` L299) | Publisher confirms: revisar API de Spring AMQP para Boot 4 | `feat: relay del outbox con publisher confirms` |
| T-04 | 2 | Publicador y consumidores AMQP con deduplicación | `implementContext.md` L187 (deuda 6) | Nueva, esperada según `implementContext.md` L187 | T-03; D-1, D-4 | M | por asignar | Ver Definición de terminado + [NEEDS CLARIFICATION: tests propios, se completan en Fase 2] | Manual ack: revisar API para Boot 4 | `feat: publicador y consumidores amqp` |
| T-05 | 2 | Incorporar ShedLock | `implementContext.md` L188 (deuda 7); `technical-plan/plan.md` L240 | `pom.xml` (no tiene dependencia); tabla `shedlock` en `V2__remaining_tables.sql` L145 | D-9 (versión) | S | por asignar | Ver Definición de terminado + [NEEDS CLARIFICATION: tests propios, se completan en Fase 2] | ShedLock: versión sin confirmar (`CLAUDE.md`, sección Dudas abiertas, punto 2) | `chore: agregar shedlock para jobs` |
| T-06 | 4 | Contrato completo de CU-08 | `implementContext.md` L192 (deuda 8); `implementContext.md` L59 | `ChangeReservationStatusService.java`, `ReservationStateMachine.java`, `ChangeReservationStatusCommand` (en `application/dto/`) | Ninguna | L | por asignar | Ver [Ficha T-06](#ficha-t-06) | No aplica | `feat: contrato completo de cambio de estado` |
| T-07 | 4 | TTL también desde PENDING_PAYMENT | `implementContext.md` L193 (deuda 9); `ReservationJpaRepository.java` L36 | `ReservationJpaRepository.java`, `ReservationRepositoryAdapter.java` L111, `ExpireReservationsService.java` | T-06; D-3 (solo si el spec de CU-08 lo confirma) | M | por asignar | Ver [Ficha T-07](#ficha-t-07) | No aplica | `feat: expirar reservas en pending_payment por ttl` |
| T-08 | 4 | Matriz de notificaciones M1/M3 con guarda `m1_blocked` | `implementContext.md` L194 (deuda 10); `ChangeReservationStatusService.java` L30-L36 | `ChangeReservationStatusService.java`, columna `m1_blocked` en V1/V3 | T-06; D-1; D-3 | L | por asignar | Ver [Ficha T-08](#ficha-t-08) | No aplica | `feat: matriz de notificaciones a modulos externos` |
| T-09 | 4 | Persistir campos de CU-08 no mapeados | `implementContext.md` L195 (deuda 11); `implementContext.md` L101 | `ReservationMapper.applyChanges` (`infrastructure/adapter/out/persistence/reservation`); `ReservationEntity.java` ya tiene campos (L54-L119) | T-06 | M | por asignar | Ver [Ficha T-09](#ficha-t-09) | No aplica. Regla "Sin dinero": `BigDecimal` solo se almacena (`contexto_m2.md` L34) | `feat: mapear campos de cambio de estado` |
| T-10 | 4 | Inmutabilidad de terminales y auditoría completa | `implementContext.md` L196 (deuda 12) | `ReservationStatusAuditEntity.java`; trigger en V1 | T-06 | M | por asignar | Ver [Ficha T-10](#ficha-t-10) | No aplica | `test: inmutabilidad de estados terminales` |
| T-11a | 6 | CU-19 contrato y endpoint | `implementContext.md` L70 (CU-19 no existe); L201 (deuda 14); `features/CU-19-*/` (spec y plan, no leídos) | Nueva, sin ruta definida en documentos: [NEEDS CLARIFICATION] | T-14 y T-15 (`technical-plan/plan.md` L423); D-13 (seguridad e identidad) | M | por asignar | Ver Definición de terminado + [NEEDS CLARIFICATION: tests propios, se completan en Fase 2] | No aplica | `feat: detalle de embarcacion` |
| T-11b | 6 | CU-20 contrato completo | `implementContext.md` L71 (Parcial; faltan `filter`, `pagination`, `applied_filter`, `status_badge`); `features/CU-20-*/` (no leído) | `ReservationQueryController.java` L43; `ReservationQueryService.java`; `ReservationResponse.java` | Ninguna | M | por asignar | Ver Definición de terminado + [NEEDS CLARIFICATION: tests propios, se completan en Fase 2] | `OffsetDateTime` en `ReservationResponse.java` L49 vs `Instant` en entidad (sección G) | `feat: completar contrato de mis reservas` |
| T-11c | 6 | CU-21 contrato completo | `implementContext.md` L72 (Parcial; faltan `status_badge` y datos operativos); `features/CU-21-*/` (no leído) | `ReservationQueryController.java` L56; `ReservationQueryService.java`; `ReservationResponse.java` | T-11b (mismo DTO); T-15 si el detalle incluye datos de cotización (no verificado) | M | por asignar | Ver Definición de terminado + [NEEDS CLARIFICATION: tests propios, se completan en Fase 2] | `OffsetDateTime` en `ReservationResponse.java` L49 vs `Instant` (sección G) | `feat: completar detalle de reserva` |
| T-12 | 5 | CU-14 publicar estado a M3 | `features/CU-14-*/plan.md` L18-L62; `implementContext.md` L193 y L201 | Usa T-03 y T-04; `OutboxAdapter.java` | T-03, T-04, T-06; D-3 (PENDING_PAYMENT) | L | por asignar | Ver [Ficha T-12](#ficha-t-12) | AMQP: revisar API Boot 4 | `feat: publicar estado de reserva a modulo 3` |
| T-13 | 5 | CU-15 endpoint interno para M3 | `features/CU-15-*/plan.md`; `technical-plan/plan.md` L233 (C7) | Nueva, esperada según `technical-plan/plan.md` L233 | D-6 (identidad de servicio); D-13 (seguridad) | M | por asignar | Ver Definición de terminado + [NEEDS CLARIFICATION: tests propios, se completan en Fase 2] | Identidad de servicio (M3): no definida (`technical-plan/plan.md` L73) | `feat: endpoint interno de consulta para modulo 3` |
| T-14 | 3 | Cliente HTTP real de M1 (CU-09 y CU-10) | `implementContext.md` L179 (deuda 1); `technical-plan/plan.md` L293-L296 (timeouts) | Nueva, esperada según `technical-plan/plan.md` L133; reemplaza comportamiento de `FakeFleet*Adapter.java` | D-9 (Resilience4j) | L | por asignar | Ver Definición de terminado + [NEEDS CLARIFICATION: tests propios, se completan en Fase 2] | Resilience4j: no está en `pom.xml`; versión por verificar | `feat: cliente http de modulo 1` |
| T-15 | 3 | Clientes M3 para CU-11 y CU-12 | `implementContext.md` L180 (deuda 2); `technical-plan/plan.md` L134 | Nueva, esperada según `technical-plan/plan.md` L134 | D-7 (SLA); D-9 (Resilience4j) | L | por asignar | Ver Definición de terminado + [NEEDS CLARIFICATION: tests propios, se completan en Fase 2] | Resilience4j: versión por verificar | `feat: clientes http de modulo 3` |
| T-16a | 7 | CU-01 búsqueda con cotización en lote | `features/CU-01-*/` (no leído); `technical-plan/plan.md` L393 (CU-01 depende del modo lote de CU-11); `technical-plan/plan.md` L431-L433 | Nueva, sin ruta definida en documentos: [NEEDS CLARIFICATION] | T-15 (lote de CU-11); T-14 (`technical-plan/plan.md` L431 no lo nombra: [NEEDS CLARIFICATION]) | L | por asignar | Ver Definición de terminado + [NEEDS CLARIFICATION: tests propios, se completan en Fase 2] | No aplica | `feat: buscar embarcaciones disponibles` |
| T-16b | 7 | CU-03 iniciar pago | `features/CU-03-*/` (no leído); `technical-plan/plan.md` L393 (CU-03 depende de la liquidación final de CU-12); `technical-plan/plan.md` L431-L433 | Nueva, sin ruta definida en documentos: [NEEDS CLARIFICATION] | T-15 (CU-12); T-06; T-11c (caso base, `technical-plan/plan.md` L423); D-3 (PENDING) | L | por asignar | Ver Definición de terminado + [NEEDS CLARIFICATION: tests propios, se completan en Fase 2] | Sin Spring Security; identidad por headers (`contexto_m2.md` L27) | `feat: iniciar pago de reserva` |
| T-16c | 7 | CU-13 confirmar pago (canal B) | `features/CU-13-*/plan.md` L25, L226, L260; `contexto_m2.md` L65 (canal B activo) | Nueva, sin ruta definida en documentos: [NEEDS CLARIFICATION] | T-15 (cliente M3); T-06; D-6 (canal A usa identidad de servicio) | L | por asignar | Ver Definición de terminado + [NEEDS CLARIFICATION: tests propios, se completan en Fase 2] | No aplica | `feat: confirmar pago desde modulo 3` |
| T-17a | 8 | CU-04 solicitar cancelación | `features/CU-04-*/` (no leído); `technical-plan/plan.md` L443 (depende de la zona horaria de CU-09) | Nueva, sin ruta definida en documentos; tabla `cancellation_event` (V2 L28) | T-14 (zona horaria); T-06; T-12 (aviso a M1 y M3) | L | por asignar | Ver Definición de terminado + [NEEDS CLARIFICATION: tests propios, se completan en Fase 2] | No aplica | `feat: solicitar cancelacion` |
| T-17b | 8 | CU-05 marcar inasistencia | `features/CU-05-*/` (no leído); `technical-plan/plan.md` L443 | Nueva; tabla `noshow_event` (V2 L39) | T-14; T-06; T-12 | M | por asignar | Ver Definición de terminado + [NEEDS CLARIFICATION: tests propios, se completan en Fase 2] | No aplica | `feat: marcar inasistencia` |
| T-17c | 8 | CU-06 marcar inicio de navegación | `features/CU-06-*/` (no leído); `consistencia-m2-m1.md` L28 (aviso IN_NAVIGATION a M1) | Nueva; tabla `checkin_event` (V2 L48) | T-06; T-08; T-12 | M | por asignar | Ver Definición de terminado + [NEEDS CLARIFICATION: tests propios, se completan en Fase 2] | No aplica | `feat: marcar inicio de navegacion` |
| T-17d | 8 | CU-07 marcar fin de navegación | `features/CU-07-*/` (no leído); `consistencia-m2-m1.md` L29 (señal MAINTENANCE_CLEANING) | Nueva; tabla `checkout_event` (V2 L56) | T-06; T-08; T-12 | M | por asignar | Ver Definición de terminado + [NEEDS CLARIFICATION: tests propios, se completan en Fase 2] | No aplica | `feat: marcar fin de navegacion` |
| T-18a | 9 | CU-16 generar disputa de garantía | `features/CU-16-*/` (no leído); `technical-plan/plan.md` L138-L153; tablas `dispute_guarantee` (V2 L89) y `dispute_claim` (V2 L104) | Nueva, contexto `disputa` (`technical-plan/plan.md` L138) | T-17d (`technical-plan/plan.md` L485); T-02; D-8 (ventana 24 h) | L | por asignar | Ver Definición de terminado + [NEEDS CLARIFICATION: tests propios, se completan en Fase 2] | No aplica | `feat: generar disputa de garantia` |
| T-18b | 9 | CU-17 actualizar estado de disputa | `features/CU-17-*/` (no leído); tabla `admin_review` (V2 L114) | Nueva, contexto `disputa` | T-18a; D-8 | M | por asignar | Ver Definición de terminado + [NEEDS CLARIFICATION: tests propios, se completan en Fase 2] | No aplica | `feat: actualizar estado de disputa` |
| T-18c | 9 | CU-18 publicar información de disputa a M3 | `features/CU-18-*/` (no leído); `technical-plan/plan.md` L453 | Nueva, `shared/contracts/dispute/OpenDisputeUseCase` (`technical-plan/plan.md` L144-L145) | T-18a; T-04 | M | por asignar | Ver Definición de terminado + [NEEDS CLARIFICATION: tests propios, se completan en Fase 2] | AMQP: revisar API Boot 4 | `feat: publicar disputa a modulo 3` |
| T-19 | 10 | Dockerfile, README y perfiles dev/test/prod | `implementContext.md` L207 (deuda 17); `technical-plan/plan.md` L104-L108, L157-L158 | Nueva: `Dockerfile`, `README.md`, `application-dev.yml`, `application-test.yml`, `application-prod.yml` (según `technical-plan/plan.md`). Nota: el repo tiene `application.yaml`, no `.yml` (`src/main/resources/application.yaml`) | D-11 (nombres de archivo) | M | por asignar | Ver Definición de terminado + [NEEDS CLARIFICATION: tests propios, se completan en Fase 2] | Nombre de archivos: `application.yaml` vs `application.yml` | `chore: dockerfile, readme y perfiles` |
| T-20 | 10 | springdoc, Resilience4j y verificación de estilo | `implementContext.md` L208 (deuda 18); `technical-plan/plan.md` L361 (T009) | `pom.xml` | D-9 (versiones) | M | por asignar | Ver Definición de terminado + [NEEDS CLARIFICATION: tests propios, se completan en Fase 2] | springdoc, Resilience4j: no están en `pom.xml`; verificar compatibilidad Boot 4 | `chore: springdoc, resilience4j y estilo` |
| T-21 | 10 | Testcontainers, WireMock, Awaitility | `implementContext.md` L209 (deuda 19); `contexto_m2.md` L16 | `pom.xml` | D-9 (versiones) | M | por asignar | Ver Definición de terminado + [NEEDS CLARIFICATION: tests propios, se completan en Fase 2] | Verificar compatibilidad Boot 4. `contexto_m2.md` L16 dice que no hay Testcontainers hoy | `chore: dependencias de testing de integracion` |
| T-22 | 10 | Observabilidad: correlation id, logs, métricas | `implementContext.md` L210 (deuda 20); `technical-plan/plan.md` L380 (T021) | Clases transversales nuevas (no hay ruta definida) | Ninguna | M | por asignar | Ver Definición de terminado + [NEEDS CLARIFICATION: tests propios, se completan en Fase 2] | Actuator ya en `pom.xml` L36 | `feat: observabilidad con correlation id` |
| T-23 | 10 | Cierre de NEEDS CLARIFICATION y polish | `implementContext.md` L211 (deuda 21); `technical-plan/plan.md` L463-L469 | Varios | Todas; D-10 (número de CU) | L | por asignar | Ver Definición de terminado + [NEEDS CLARIFICATION: tests propios, se completan en Fase 2] | No aplica | `docs: cerrar necesidades de clarificacion` |

---

## Fichas de tareas

Fichas de la Fase 2 para el lote [CU-02, CU-08, CU-14]. Cada ficha cubre una fila de la tabla D. Las citas de spec y plan son líneas de los archivos leídos completos en esta fase. "PROPUESTA" marca nombres que no están en ningún documento.

Lote leído completo: `features/CU-02-iniciar-reserva/spec.md`, `plan.md`, `plan.event.md`; `features/CU-08-actualizar-estado-reserva/spec.md`, `plan.md`; `features/CU-14-recibir-estado-reserva/spec.md`, `plan.md`. Código leído: ver M.

#### Ficha T-06

- **Objetivo**: convertir el motor de cambio de estado en el puerto de aplicación del plan, con comando completo, validación por subtipo y respuesta con versión y eventos encolados.
- **CU y spec**: CU-08. Spec `features/CU-08-actualizar-estado-reserva/spec.md` (FR-001 a FR-011, L176-L195). Plan `features/CU-08-actualizar-estado-reserva/plan.md` (puerto L22-L50; matriz L63-L75; respuesta L81-L95).
- **Depende de**: ninguna fila. **Bloqueada por**: ninguna decisión directa. El actor se valida en cada CU invocador (`plan.md` L183).
- **Archivos**:
  - Existe: `application/port/in/ChangeReservationStatusUseCase.java` (método `void execute(ChangeReservationStatusCommand)`).
  - Existe: `application/dto/ChangeReservationStatusCommand.java` L9-L15 (5 campos).
  - Existe: `application/service/ChangeReservationStatusService.java` L28-L106.
  - Existe: `domain/state/ReservationStateMachine.java` L11-L42.
  - PROPUESTA: nombre `ApplyReservationTransitionUseCase`. Lo usa `plan.md` L22 y L26, pero no existe en el código.
  - Capa: application (puerto in y servicio). Modelo vecino: `application/service/ExpireReservationsService.java` L51 (llama al puerto con actor `SYSTEM`).
- **Contrato del comando** (copiado de `plan.md` L38-L52): `reservation_id`, `target_status`, `cancellation_subtype` (obligatorio si `CANCELLED`), `actor` (`RENTER | OWNER | SYSTEM`), `actor_id` (obligatorio si no es `SYSTEM`), `reason`, `mechanical_failure` (obligatorio si cancela `OWNER`), `anticipation_minutes` (obligatorio si cancela `RENTER`), `completion_notes` (solo `COMPLETED`), `payment_reference` (solo `RESERVED`), `expected_version` (opcional).
  - Código actual: 5 campos (`ChangeReservationStatusCommand.java` L10-L14). Faltan `actor_id`, `mechanical_failure`, `anticipation_minutes`, `completion_notes`, `payment_reference`, `expected_version`.
- **Contrato de respuesta** (`plan.md` L86-L94): `reservation_id`, `status`, `cancellation_subtype`, `status_changed_at`, `ttl_expires_at`, `version`, `events_enqueued`. Código: `void execute` (`ChangeReservationStatusUseCase.java`). No devuelve nada.
- **Tablas**: `reservation` (V1 L11-L57), `reservation_status_audit` (V1 L67-L76), `outbox_message` (V1 L94-L107). Faltan en V1 `status_changed_at` (plan L203), `actor_id` y `event_ids` (plan L204). Ver ficha T-10.
- **Reglas**:
  - Sin dinero: `plan.md` L7 (punto 7) y `spec.md` L194 (FR-010). El comando no tiene montos (L9-L15).
  - CU-08 único escritor: `spec.md` L176 (FR-001). Incumplido por `CreateReservationService.java` L125 (ver ficha CU-02, PROPUESTA T-24).
  - Hexagonal: `ReservationStateMachine.java` solo importa tipos de dominio (L3-L9). ArchUnit: `src/test/java/com/seashare/m2/reservas/architecture/LayeredArchitectureTest.java`.
  - Jackson 3: no aplica en la capa de aplicación. El plan define el comando en snake_case (`plan.md` L38), pero el código usa camelCase interno.
  - Migraciones: no editar V1, V2 ni V3 (`contexto_m2.md` L21). Columnas faltantes van en V4 (`[NEEDS CLARIFICATION]`, ver K).
- **Errores**:
  - `INVALID_STATE_TRANSITION` lanzado por `InvalidTransitionException` (`CODE` L8) → `ErrorCode.fromInternal` → `INVALID_RESERVATION_STATE` 409 (`ErrorCode.java` L58-L60).
  - `VERSION_CONFLICT` lanzado por `VersionConflictException` (`CODE` L7) → `CONCURRENT_STATE_CHANGE` 409 (`ErrorCode.java` L61-L63).
  - `RESERVATION_NOT_FOUND` lanzado por `ReservationNotFoundException` (`CODE` L9) → 404 (`ErrorCode.java` L13).
  - `RESERVATION_EXPIRED` 409 existe en `ErrorCode.java` L21 pero ningún código de `src/main` lo lanza. El plan lo exige para `RESERVED` con TTL vencido (`plan.md` L167).
  - `PERSISTENCE_ERROR` (`plan.md` L168) no existe en `ErrorCode.java` L8-L28. `[NEEDS CLARIFICATION]`.
- **Escenarios de aceptación** (copiados de `spec.md`):
  - Escenario 3 (L60-L63): "**Given** una reserva en estado principal "Pendiente de Pago" / **When** el caso de uso `Confirmar pago` pide actualizar a estado "Reservada" / **Then** el sistema verifica que el cambio es válido, actualiza el estado principal a "Reservada", guarda la hora del cambio."
  - Escenario 6 (L75-L78): "**Given** una reserva en estado principal "Reservada" / **When** `Solicitar cancelación` o `Marcar inasistencia` piden el cambio, aportando el tipo o motivo correspondiente / **Then** el sistema actualiza el estado principal a "Cancelada" y asigna el sub-estado que corresponda."
  - Escenario 7 (L80-L83): "**Then** el sistema actualiza el estado principal a "Expirada" de forma segura y completa (transiciones válidas: `Iniciada → Expirada` y `Pendiente de Pago → Expirada`)."
  - Escenario 8 (L85-L88): "**Then** el sistema actualiza el estado principal a "Pago Fallido", guardando el motivo de rechazo."
  - US4 escenario 1 (L156-L159): "**Then** el sistema rechaza la solicitud explicando que solo las reservas en estado `Reservada` admiten cancelación activa."
  - FR-006 (L190): "la segunda recibe un conflicto y se rechaza (HTTP 409) sin sobrescribir el estado consolidado".
- **Tests**:
  - Existentes: `integration/ChangeReservationStatusServiceTest.java` (`validChangeSavesStatusAndAudit` L106, `invalidChangeWritesNothing` L120, `versionConflictRaisesVERSION_CONFLICT` L135, `failureRollsEverythingBack` L149, `actorNotAllowedForSubtypeIsRejected` L166). `domain/state/ReservationStateMachineTest.java` (L17, L25, L33, L44, L50).
  - Propuestos (un método por escenario):
    - Escenario 3: `reservedFromPendingPaymentWithPaymentReference` en `ChangeReservationStatusServiceTest`. Comando: `.\mvnw.cmd test "-Dtest=ChangeReservationStatusServiceTest"`.
    - Escenario 6: `cancelledByRenterStoresSubtypeAndAnticipation`. Mismo comando.
    - Escenario 7: `pendingPaymentExpiresByTtl` en `ExpireReservationsServiceTest`. Comando: `.\mvnw.cmd test "-Dtest=ExpireReservationsServiceTest"`.
    - Escenario 8: `pendingPaymentToPaymentFailedStoresReason` en `ChangeReservationStatusServiceTest`.
    - US4: `cancelFromPendingPaymentIsRejected` en `ReservationStateMachineTest`. Comando: `.\mvnw.cmd test "-Dtest=ReservationStateMachineTest"`.
    - FR-006: ya cubierto por `versionConflictRaisesVERSION_CONFLICT`.
  - Criterio de terminado: ver Definición de terminado.
- **Riesgos e imports (Boot 4)**:
  - `@Transactional` desde `org.springframework.transaction.annotation` (`ChangeReservationStatusService.java` L19).
  - `OptimisticLockingFailureException` desde `org.springframework.dao` (`ReservationRepositoryAdapter.java` L12).
  - El conflicto de versión se detecta en `saveAndFlush` (`ReservationRepositoryAdapter.java` L62), no al cargar la reserva.
  - El plan pide guarda de TTL en `RESERVED` (`plan.md` L213), que el spec no define (ver L).
- **Dependencias sin confirmar**: ninguna para T-06.

#### Ficha T-07

- **Objetivo**: el barrido de expiración también expira reservas en `PENDING_PAYMENT`.
- **CU y spec**: CU-08. `spec.md` L80-L83 (escenario 7) y L184-L185 (FR-003). Plan: `plan.md` L69, L71 (filas de la matriz) y L197 (job).
- **Depende de**: T-06. **Bloqueada por**: ninguna decisión que impida empezar. El spec confirma la expiración desde `PENDING_PAYMENT` (`spec.md` L80-L83). D-3 solo afecta si esa expiración se publica a M3.
- **Archivos**:
  - Existe: `infrastructure/adapter/out/persistence/reservation/ReservationJpaRepository.java` L36-L37 (JPQL con `r.status = 'INITIATED'`).
  - Existe: `infrastructure/adapter/out/persistence/reservation/ReservationRepositoryAdapter.java` L109-L114.
  - Existe: `application/port/out/ReservationRepositoryPort.java` L29-L33 (`findInitiatedExpiredBefore`).
  - Existe: `application/service/ExpireReservationsService.java` L44-L72.
  - Existe: `infrastructure/adapter/in/scheduler/TtlExpiryJob.java` L27 (`fixedDelayString = "${m2.scheduler.ttl.fixed-delay-ms:30000}"`).
  - PROPUESTA: nombre del método del puerto para estados múltiples. No definido en documentos.
  - Capa: application (puerto out y servicio) e infrastructure (JPQL y adaptador).
- **Contrato**: no hay endpoint ni evento nuevo. Índice `ix_reservation_ttl (status, ttl_expires_at)` en V1 L61.
- **Reglas**:
  - CU-08 único escritor: `ExpireReservationsService.java` L51 usa `ChangeReservationStatusUseCase`.
  - Reloj: `ExpireReservationsService.java` L45 usa `ClockPort`. Prohibido `Instant.now()` (`contexto_m2.md` L23).
  - Sin dinero: no aplica.
  - Migraciones: no aplica.
- **Errores**: ninguno nuevo.
- **Escenarios de aceptación** (`spec.md`):
  - L80-L83 (copiado): "**Given** una reserva en estado principal "Iniciada" o "Pendiente de Pago" cuyo temporizador TTL de 15 minutos ya venció sin confirmación de pago / **When** el temporizador interno del sistema dispara el cambio / **Then** el sistema actualiza el estado principal a "Expirada" de forma segura y completa".
  - Nota de notificación (L81-L83): "No se dispara `CU-22`: `Expirada` no se notifica a Módulo 1 bajo ninguna circunstancia".
- **Tests**:
  - Existentes: `ExpireReservationsServiceTest` (9 pruebas según `implementContext.md` L124), `TtlExpiryIntegrationTest` (4, L126), `TtlExpiryJobTest` (1, L125).
  - Propuestos:
    - `expiresPendingPaymentPastTtl` en `TtlExpiryIntegrationTest` (PostgreSQL real). Comando: `.\mvnw.cmd test "-Dtest=TtlExpiryIntegrationTest"`.
    - `pendingPaymentBeforeTtlIsNotExpired` en `ExpireReservationsServiceTest`. Comando: `.\mvnw.cmd test "-Dtest=ExpireReservationsServiceTest"`.
    - `expiredPendingPaymentWritesNoOutbox` en `ChangeReservationStatusServiceTest`.
- **Riesgos e imports (Boot 4)**:
  - Sin ShedLock: `TtlExpiryJob.java` L13 (TODO D-15). `implementContext.md` L188 (deuda 7). Con varias réplicas, dos instancias pueden expirar lo mismo; `ExpireReservationsService` tolera conflictos (L59-L62).
  - Intervalo: 30 s en código (`TtlExpiryJob.java` L27). El plan propone 5 s con `FOR UPDATE SKIP LOCKED` (`plan.md` L197). `[NEEDS CLARIFICATION]`.
  - `@Scheduled` desde `org.springframework.scheduling.annotation`.
- **Dependencias sin confirmar**: ShedLock. `[NEEDS CLARIFICATION]`: versión compatible con Boot 4.1.2. Verificar en Maven Central antes de agregar (`CLAUDE.md`, sección Dudas abiertas, punto 2). Ver D-9.

#### Ficha T-08

- **Objetivo**: en la misma transacción del cambio de estado, encolar los eventos hacia M1 y M3 según la tabla del spec, con la guarda `m1_blocked`.
- **CU y spec**: CU-08 `spec.md` L37-L46 (tabla de flujos alternos) y L191 (FR-007). Fuente de verdad del mapeo a M1: CU-22 `spec.md` L107-L116 (FR-002).
- **Depende de**: T-06. D-1 (transporte de M1), D-2 (nombres de M1), D-3 (PENDING_PAYMENT a M3), D-15 (fase de CU-22).
- **Archivos**:
  - Existe: `application/service/ChangeReservationStatusService.java` L30-L36 (`OUTBOX_STATUSES`) y L77-L85 (encolado).
  - Existe: `application/dto/StatusChangedEvent.java` L9-L15.
  - Existe: `application/port/out/OutboxPort.java` L8-L10.
  - Existe: `infrastructure/adapter/out/persistence/reservation/ReservationEntity.java` L111-L112 (`m1Blocked`) y L339-L344.
  - Existe: columna `m1_blocked` en V1 L38.
  - PROPUESTA: evento hacia M1. Nombre `OperationalStatusEvent` según `CU-22/spec.md` L143 (Key Entities). No existe en código.
  - Capa: application (matriz) e infrastructure (adaptador outbox). Modelo vecino: `infrastructure/adapter/out/messaging/outbox/OutboxAdapter.java` L30-L54.
- **Contrato hacia M1** (`CU-22/plan.md`):
  - Payload `vessel_id`, `operational_status`, `occurred_at`, `event_id` (L112-L116).
  - Headers `Message-Id` = `event_id` y `Content-Type` (L57-L58).
  - Destino: AMQP (L7, L48) o `M1_HTTP` (`technical-plan/plan.md` L229, L296). Ver D-1.
  - Exchange, routing key y cola: pendientes (`CU-22/plan.md` L64-L66). Ver D-2.
  - Estados operativos: `AVAILABLE`, `RESERVED`, `IN_NAVIGATION`, `MAINTENANCE_CLEANING` (`CU-22/plan.md` L132-L135).
- **Contrato hacia M3**: se cubre en la ficha T-12. Aquí solo se encola.
- **Reglas**:
  - Sin dinero: `CU-22/spec.md` L124 (FR-007) y L93. Payload sin montos.
  - CU-08 único escritor: el encolado ocurre dentro de `ChangeReservationStatusService` (L77).
  - Transacción única: `CU-22/spec.md` L122 (FR-005) y `plan.md` L194.
  - Guarda `m1_blocked`: `CU-08/plan.md` L196 y SC-003 en `CU-08/plan.md` L239. **Hallazgo**: `m1_blocked` solo se escribe como `false` (`ReservationMapper.java` L54 y `ReservationMapperTest.java` L58). Ningún código lo pone en `true`. `applyChanges` solo copia `status` y subtipo (`ReservationMapper.java` L60-L62).
  - Hexagonal: la matriz debe quedar en application, sin Spring.
  - Migraciones: `outbox_message.destination` acepta `AMQP` y `M1_HTTP` (V1 L106).
- **Errores**: ninguno HTTP. Si el outbox falla, la transición se revierte (`ChangeReservationStatusServiceTest.failureRollsEverythingBack`, L149-L150).
- **Escenarios de aceptación**:
  - CU-08 `spec.md` L37-L46: fila `Reservada`: "Sí (con sub-estado si aplica) | Sí | Notificación a Módulo 1 con estado operativo mapeado `Reservado`". Fila `Iniciada`: "No | No". Fila `Pendiente de Pago`: "Sí | No".
  - CU-22 `spec.md` L32-L35 (escenario 1, copiado): "**Then** el sistema encola la notificación a Módulo 1 con el estado operativo `Reservado` para esa embarcación".
  - CU-22 `spec.md` L42-L45 (escenario 3, copiado): "**Then** el sistema **NO** encola ninguna notificación a Módulo 1".
- **Tests propuestos**:
  - `enqueuesReservedToModuleOneOnPaymentConfirmed` (en `ChangeReservationStatusServiceTest`). Comando: `.\mvnw.cmd test "-Dtest=ChangeReservationStatusServiceTest"`.
  - `initiatedAndPendingPaymentDoNotEnqueueModuleOne`.
  - `cancelledBeforeReservedDoesNotEnqueueModuleOne`.
  - `cancelledByOwnerWithMechanicalFailureEnqueuesMaintenance`.
  - PROPUESTA: clase `ModuleOneNotificationMatrixTest`. Nombre no definido en documentos. Comando: `.\mvnw.cmd test "-Dtest=ModuleOneNotificationMatrixTest"`.
- **Riesgos e imports (Boot 4)**:
  - D-1 y D-2 deciden el adaptador. Hasta decidir, el encolado puede usar el destino ya existente (`DESTINATION_AMQP` en `OutboxAdapter.java` L20).
  - Guarda `m1_blocked` sin escritura: si se activa, se deben poner `true` al encolar `RESERVED` y leer la guarda al encolar liberaciones. `[NEEDS CLARIFICATION]` si la escritura va en la misma transacción.
  - `OutboxAdapter.java` L30-L54 incluye `reservation_id`, `status`, `cancellation_subtype`, `status_changed_at`. No incluye `vessel_id`, que el payload de M1 necesita. `[NEEDS CLARIFICATION]` de dónde sale.
- **Dependencias sin confirmar**: ninguna nueva para T-08.

#### Ficha T-09

- **Objetivo**: persistir los campos de CU-08 que hoy no se mapean (cierre, pago, horas reales, novedades).
- **CU y spec**: CU-08. `spec.md` L60-L63 (escenario 3: "guarda la hora del cambio"), L70-L73 (escenario 5: novedades), L193 (FR-009). Plan: `plan.md` L36-L50 (comando) y L203 (modelo mínimo).
- **Depende de**: T-06.
- **Archivos**:
  - Existe: `infrastructure/adapter/out/persistence/reservation/ReservationMapper.java` L60-L62 (`applyChanges`).
  - Existe: `ReservationEntity.java` L97 (`actualDepartureAt`), L100 (`actualArrivalAt`), L103 (`completionNotes`), L106 (`paymentReference`), L109 (`paymentConfirmedAt`).
  - Existe: columnas en V1 L33-L37 y L35-L36.
  - Faltan en V1: `status_changed_at` (plan L203). Nueva migración V4 (PROPUESTA: nombre no definido). `[NEEDS CLARIFICATION]`.
  - Capa: infrastructure (mapper) y domain (`Reservation.java` L13-L30 no tiene estos campos).
- **Contrato**: sin endpoint. Los valores vienen del comando de T-06.
- **Reglas**:
  - Sin dinero: los campos `rental_amount`, `insurance_amount`, `guarantee_deposit_amount`, `total_amount` existen en `ReservationEntity.java` L75-L87. No deben calcularse ni sumarse (`contexto_m2.md` L34).
  - CU-08 único escritor: `ReservationMapper.applyChanges` solo lo llama `ReservationRepositoryAdapter.save` (L59).
  - Migraciones: V1, V2 y V3 intocables (`contexto_m2.md` L21). Cambios en V4.
- **Errores**: ninguno.
- **Escenarios de aceptación**:
  - Escenario 5 (`spec.md` L70-L73, copiado): "**Then** el sistema actualiza el estado principal a "Completada" y guarda el texto de novedades si el Propietario lo proveyó."
  - FR-009 (L193, copiado): "el sistema DEBE adjuntarlo como campo informativo en la notificación de `Completada` a Módulo 3, sin que ello modifique el tratamiento del cierre."
- **Tests propuestos**:
  - `applyChangesPersistsCompletionNotes` en `ReservationMapperTest`. Comando: `.\mvnw.cmd test "-Dtest=ReservationMapperTest"`.
  - `persistsPaymentReferenceOnReserved` en `ReservationPersistenceTest`. Comando: `.\mvnw.cmd test "-Dtest=ReservationPersistenceTest"`.
  - `persistsActualDepartureAndArrival` en `ReservationPersistenceTest`.
- **Riesgos e imports (Boot 4)**:
  - `departure_time` es NOT NULL desde V3 (L12). Los tests de creación ya lo cumplen.
  - `LocalTime` ↔ `TIME` y `Instant` ↔ `TIMESTAMPTZ`: el mapeo existe en `ReservationEntity.java` L60 y L97.
  - `[NEEDS CLARIFICATION]` si `completion_notes` es obligatorio en `COMPLETED`.
- **Dependencias sin confirmar**: ninguna.

#### Ficha T-10

- **Objetivo**: auditoría completa por transición (incluida creación) y estados terminales inmutables verificados por prueba.
- **CU y spec**: CU-08. `spec.md` L178-L180 (FR-002, estados terminales), L168 (edge case), L156-L159 (US4). Plan: `plan.md` L198 (auditoría), L204 (campos), L325 (D-19).
- **Depende de**: T-06.
- **Archivos**:
  - Existe: `domain/state/ReservationStateMachine.java` L20-L23 (salidas vacías en estados terminales).
  - Existe: `infrastructure/adapter/out/persistence/audit/StatusAuditAdapter.java` L17-L27 (solo `repository.save`).
  - Existe: `infrastructure/adapter/out/persistence/audit/ReservationStatusAuditEntity.java` L25-L50.
  - Existe: `application/dto/StatusAuditEntry.java` (campos actuales).
  - Existe: V1 L67-L76 (tabla) y L80-L89 (trigger `prevent_audit_mutation`).
  - Faltan: `actor_id` y `event_ids` (plan L204). Nueva migración V4. `[NEEDS CLARIFICATION]`.
- **Contrato**: sin endpoint.
- **Reglas**:
  - Inmutabilidad: trigger en V1 L87-L89. Única vía de escritura: `StatusAuditAdapter.record`.
  - Sin dinero: no aplica.
  - Migraciones: V1 intocable.
- **Errores**: `INVALID_STATE_TRANSITION` lanzado por `ReservationStateMachine.validate` (L35-L37) → `INVALID_RESERVATION_STATE` 409.
- **Escenarios de aceptación**:
  - `spec.md` L178-L180 (copiado): "`Completada`, `Cancelada`, `Expirada` y `Pago Fallido` son definitivos."
  - `spec.md` L156-L159 (copiado): "**Then** el sistema rechaza la solicitud explicando que solo las reservas en estado `Reservada` admiten cancelación activa".
- **Tests propuestos**:
  - Existentes: `ReservationStateMachineTest.finalStatesHaveNoExit` (L33) y `ReservationTest.initiatedToCompletedThrowsInvalidTransition` (L17).
  - Propuesto: `auditRowCannotBeUpdated`. No existe test del trigger (búsqueda sin resultados). Clase PROPUESTA: `StatusAuditImmutabilityTest`. Comando: `.\mvnw.cmd test "-Dtest=StatusAuditImmutabilityTest"`.
  - Propuesto: `creationWritesAuditRow`, que hoy no ocurre (`CreateReservationService.java` no llama a `StatusAuditPort`).
- **Riesgos e imports (Boot 4)**:
  - El trigger necesita PostgreSQL real (`contexto_m2.md` L16).
  - Plan L198 pide auditoría de la creación. El spec no lo pide explícitamente (`spec.md` L53, "No se invocan CU-14 ni CU-22"). `[NEEDS CLARIFICATION]`.
- **Dependencias sin confirmar**: ninguna.

#### Ficha T-12

- **Objetivo**: publicar `reservation.status.changed` a M3 con el contrato de CU-14, estados que M3 reconoce y sin montos.
- **CU y spec**: CU-14. `spec.md` L25-L28 (escenario 1), L30-L33 (escenario 2), L47-L50 (reintento), L66 (FR-001), L67 (FR-002), L68 (FR-003), L69 (FR-004), L70 (FR-005), L71 (FR-006). Plan: `plan.md` L23-L27 (destino), L33-L36 (headers), L38-L47 (payload), L51-L63 (mapeo), L119-L130 (reglas).
- **Depende de**: T-03 (relay), T-04 (publicador), T-06, D-3. **Bloquea**: T-16b (CU-03 emite `PENDING`), T-17a..T-17d, T-18c.
- **Archivos**:
  - Existe: `application/dto/StatusChangedEvent.java` L9-L15.
  - Existe: `infrastructure/adapter/out/messaging/outbox/OutboxAdapter.java` L30-L54 (payload actual en L31-L38).
  - PROPUESTA: mapeador de estado interno a estado M3 (`PENDING_PAYMENT` → `PENDING`, `CANCELLED` + subtipo → `CANCELLED_*`). Nombre no definido. Capa: infrastructure/adapter/out/messaging.
  - Modelo vecino: `OutboxAdapter.enqueue(ReservationInfoProvidedEvent)` L57-L82 (snake_case con Jackson 3).
- **Contrato** (`plan.md` L23-L63):
  - Exchange `seashare.reservations` (topic). Routing key `reservation.status.changed`. Cola `finance.reservation-status.v1`.
  - Headers: `Message-Id` (UUID), `Content-Type` (`application/json`).
  - Payload: `reservation_id`, `status` (`PENDING`, `RESERVED`, `IN_NAVIGATION`, `COMPLETED`, `CANCELLED_FLEXIBLE`, `CANCELLED_MODERATE`, `CANCELLED_STRICT`, `CANCELLED_BY_HOST`), `status_changed_at`, `payment_token_ref` (solo `PENDING`), `payment_method_type` (opcional), `payment_metadata` (opcional, solo `PENDING`).
  - Brechas código-contrato:
    - `OutboxAdapter.java` L33 y L35 envían nombres internos (`PENDING_PAYMENT`, `CANCELLED`). No hay mapeo.
    - No hay `Message-Id` (no hay relay).
    - No hay `payment_token_ref`. El token viene de CU-03 (`plan.md` L315, D-09).
- **Reglas**:
  - Sin dinero: `plan.md` L124; `spec.md` L71 (FR-006) y L87 (SC-003).
  - CU-08 único escritor: `spec.md` L66 (FR-001, "cada vez que el caso de uso `Actualizar estado reserva` consolide una transición").
  - Outbox transaccional: `spec.md` L66 ("vía outbox"); `plan.md` L119.
  - Jackson 3: `OutboxAdapter.java` L7-L8 (`tools.jackson.databind`).
  - Anotaciones: `com.fasterxml.jackson.annotation` (`CreateReservationRequest.java` L3).
- **Errores**: no HTTP. Si falta `payment_token_ref` en `PENDING`, CU-03 responde 400 y no cambia estado (`plan.md` L122). `[NEEDS CLARIFICATION]` código de error (no está en el catálogo).
- **Escenarios de aceptación**:
  - Escenario 1 (`spec.md` L25-L28, copiado): "**Then** el sistema emite, por la cola de entrega garantizada (primer intento asíncrono), la notificación a Módulo 3 informando que la reserva identificada entró a `Pendiente de Pago`".
  - Escenario 2 (L30-L33): "**Then** el sistema notifica el evento exacto a Módulo 3, incluyendo sub-estados si aplican (ej. `Cancelada` con sub-estado `Moderado`)".
  - FR-002 (L67): "el *payload* ... DEBE contener obligatoriamente: ... sub-estado (si aplica) ... y actor que disparó el evento."
  - FR-003 (L68): "Si el estado es `Cancelada`, la notificación DEBE incluir el sub-estado ... y la anticipación temporal cronológica."
  - FR-004 (L69): "la notificación de `Completada` DEBE incluirlo como campo informativo".
- **Tests propuestos**:
  - Existente: `OutboxAdapterTest.enqueuesStatusChangedEventSuccessfully` (L94). Solo asserta la routing key (L114).
  - Propuesto: `statusChangedUsesFinanceStatusNames`. Comando: `.\mvnw.cmd test "-Dtest=OutboxAdapterTest"`.
  - Propuesto: `cancelledPayloadIncludesSubtypeAndAnticipation`.
  - Propuesto: `completedPayloadIncludesCompletionNotesWhenProvided`.
  - Propuesto: `payloadHasNoAmountFields`.
  - Propuesto: `pendingPaymentPayloadHasTokenRef`, que depende de T-16b.
- **Riesgos e imports (Boot 4)**:
  - Publisher confirms y manual ack: `[NEEDS CLARIFICATION]`. Nombres de propiedades de Spring AMQP para Boot 4 no verificados. Starter presente en `pom.xml` L40.
  - Reintentos de `PENDING` generan un `Message-Id` nuevo (`plan.md` L19 y L123).
  - Contradicción con el plan de CU-08 (ver L).
- **Dependencias sin confirmar**: ninguna nueva para T-12. Versiones de Spring AMQP: no se cambian.

#### Ficha T-24 (PROPUESTA)

- **Objetivo**: completar el contrato de CU-02 y corregir la creación para que pase por CU-08.
- **Estado**: CU-02 no tiene fila en la tabla D. `implementContext.md` L203 (deuda 16) sí lo menciona. Se propone T-24. `[NEEDS CLARIFICATION]`: confirmar si se agrega como fila nueva.
- **CU y spec**: CU-02. `spec.md` L28-L41 (escenarios 1 a 3), L57-L70 (FR-001 a FR-014). Plan: `plan.md` L19-L101 (endpoint), L162-L164 (errores), L176-L192 (trazabilidad).
- **Depende de**: T-14 (cliente real de M1, para que `FLEET_SERVICE_UNAVAILABLE` y `VESSEL_NOT_FOUND` tengan origen real). D-13 (identidad). D-14.
- **Archivos**:
  - Existe: `infrastructure/adapter/in/web/ReservationController.java` L31-L65 (`@ResponseStatus(CREATED)`; no devuelve `ResponseEntity`, por lo que no hay header `Location`).
  - Existe: `application/service/CreateReservationService.java` L125 (llama directamente a `reservationRepositoryPort.create`).
  - Existe: `infrastructure/adapter/in/web/CreateReservationRequest.java` L15-L47.
  - Existe: `infrastructure/adapter/in/web/ReservationResponse.java` L14-L53.
  - Existe: `infrastructure/adapter/in/web/GlobalExceptionHandler.java` L27-L34.
  - Existe: `infrastructure/adapter/out/fleet/FakeFleetInfoAdapter.java` L13-L20 (nunca lanza error).
  - Capa: infrastructure (in y out) y application.
- **Contrato** (`plan.md`):
  - `POST /api/v1/reservations` (L24).
  - Headers: `Idempotency-Key` UUID obligatorio (L35). `Authorization` JWT (L33): ver L (decisión de auth).
  - Body: `vessel_id`, `start_date`, `end_date`, `passengers`, `departure_time`, `estimated_total`, `holder` con `full_name`, `phone`, `email` (L43-L55).
  - Respuesta 201 (L74-L93): `reservation_id`, `status` = `INITIATED`, `vessel_id`, `start_date`, `end_date`, `passengers`, `holder`, `estimated_total`, `currency` = `COP`, `departure_time`, `ttl_minutes` = 15, `ttl_expires_at`, `created_at`.
  - Header de respuesta `Location: /api/v1/reservations/{reservation_id}` (L101). No implementado.
  - Errores (L158-L164): 400 `INVALID_PARAMETERS`; 401 `UNAUTHENTICATED`; 403 `PROFILE_NOT_AUTHORIZED`; 404 `VESSEL_NOT_FOUND`; 409 `VESSEL_NOT_AVAILABLE`; 409 `CAPACITY_EXCEEDED`; 503 `FLEET_SERVICE_UNAVAILABLE`.
- **Reglas**:
  - CU-08 único escritor: `spec.md` L176 (FR-001 de CU-08) y `CU-08/spec.md` L53. Incumplido por `CreateReservationService.java` L125.
  - Sin dinero: `CreateReservationService` y `ReservationController.java` L60 aceptan `estimated_total` del cliente. `spec.md` L60 dice que el valor viene de Módulo 3 (ver L).
  - Hexagonal: `ReservationController` → `CreateReservationUseCase` (`ReservationController.java` L4 y L25). Dominio sin Spring: `Reservation.initiate` (`Reservation.java` L116).
  - Jackson 3: DTOs con `com.fasterxml.jackson.annotation` (`CreateReservationRequest.java` L3). Serialización del outbox con `tools.jackson` (`OutboxAdapter.java` L7-L8).
  - Migraciones: V1 `reservation` L11-L57; V3 L8 (`harbor_timezone`) y L12 (`departure_time` NOT NULL). No tocar.
- **Errores**:
  - `INVALID_PARAMETERS`: validación (`CreateReservationRequest.java` L17-L47) y fecha inválida (`CreateReservationService.java` L69-L71).
  - `INVALID_UUID`: `ReservationController.java` L44-L46.
  - `UNAUTHENTICATED`: `CallerIdentityArgumentResolver.java` L31-L32.
  - `PROFILE_NOT_AUTHORIZED`: `ReservationController.java` L38-L39.
  - `VESSEL_NOT_AVAILABLE`: `CreateReservationService.java` L94-L96 y L102-L104.
  - `CAPACITY_EXCEEDED`: L98-L100.
  - `HARBOR_TIMEZONE_UNAVAILABLE`: L85-L92 (no está en la tabla de errores del plan).
  - `VESSEL_NOT_FOUND` y `FLEET_SERVICE_UNAVAILABLE`: existen en `ErrorCode.java` L14 y L25, pero ningún código de `src/main` los lanza.
- **Escenarios de aceptación** (`spec.md`, copiados):
  - Escenario 1 (L28-L31): "**Then** el sistema persiste la reserva en base de datos asociándola a ese nombre y contacto, invoca a `Actualizar estado reserva` (`<<include>>`) fijando el estado en `Iniciada`, enciende el TTL de 15 minutos ... En este estado **NO se bloquea el inventario en Módulo 1**."
  - Escenario 2 (L33-L36): "**Then** el sistema aplica control de concurrencia atómico bajo criterio First-Come First-Served (FCFS) al transicionar a `Pendiente de Pago` ... retorna **HTTP 409 Conflict** al segundo usuario". Este escenario es de CU-03: ver T-16b.
  - Escenario 3 (L38-L41): "**Then** la validación falla, el sistema rechaza la operación informando el error, la reserva no se crea y el barco NO se bloquea."
- **Tests propuestos**:
  - Escenario 1: existente `CreateReservationTest.createsReservationInInitiated` (L111). Propuesto: `creationGoesThroughStatusUseCase` (verifica que `ChangeReservationStatusUseCase` se invoca). Comando: `.\mvnw.cmd test "-Dtest=CreateReservationTest"`.
  - Escenario 3: propuesto `missingHolderNameIs400NoReservation` y `missingHolderPhoneIs400NoReservation` en `CreateReservationValidationTest`. Comando: `.\mvnw.cmd test "-Dtest=CreateReservationValidationTest"`.
  - Propuesto: `createsLocationHeader` en `CreateReservationTest`.
- **Riesgos e imports (Boot 4)**:
  - `@AutoConfigureMockMvc` desde `org.springframework.boot.webmvc.test.autoconfigure` (`contexto_m2.md` L29).
  - Idempotencia: `IdempotencyAdapter.java` L21-L22 tiene dos `[NEEDS CLARIFICATION]` sobre la misma clave con otro cuerpo y la expiración.
  - Mensajes de error: el plan (L158) muestra "Completa el nombre del titular ..." y el código devuelve el mensaje por defecto del campo (`GlobalExceptionHandler.java` L29-L31).
- **Dependencias sin confirmar**: ninguna para T-24 directamente.

## Decisiones del grupo

Esta sección reemplaza la antigua tarea T-01 como lista de decisiones. Ninguna opción está elegida. Cada decisión necesita respuesta del equipo antes de la tarea que bloquea.

| ID | Pregunta | Opciones tal como aparecen en los documentos (archivo y línea) | Tareas que bloquea | Consecuencia de cada opción |
|---|---|---|---|---|
| D-1 | ¿Cómo llega la notificación de estado operativo a M1 (CU-22)? | (a) AMQP: `context/consistencia-m2-m1.md` L9 y L166; `features/CU-22-*/plan.md` L7 y L48. (b) PATCH HTTP `M1_HTTP`: `technical-plan/plan.md` L229 (C3), L296 y L197 (destino `M1_HTTP`); `features/CU-22-*/plan.md` L279 | T-02 (parte M1), T-04, T-08, T-17a, T-17b, T-17c, T-17d | (a) El outbox publica en RabbitMQ (T-03, T-04); M1 debe tener consumidor y cola (D-2). (b) Hace falta cliente HTTP con timeouts de `technical-plan/plan.md` L296 (connect 100 ms, read 300 ms) y el relay distingue 5xx de 4xx. |
| D-2 | ¿Qué nombres usan exchange, routing key y cola de M1? | Pendiente de acordar con M1: `context/consistencia-m2-m1.md` L45-L47 y `features/CU-22-*/plan.md` L64-L66. Convención de referencia (contrato M3): `seashare.<dominio>`, `<entidad>.<evento>`, `<consumidor>.<evento>.v1` (mismas líneas) | T-02 (parte M1), T-04 | Sin nombres no se puede declarar la topología de M1. Si se adopta la convención de M3, el documento lo marca como referencia, no como acuerdo. |
| D-3 | ¿Se publica PENDING_PAYMENT a M3? | (a) No se publica: `features/CU-08-*/plan.md` L214. (b) Sí se publica como `PENDING`: `features/CU-14-*/spec.md` L66; `features/CU-14-*/plan.md` L18 y L53. Estado actual del código: se publica (`ChangeReservationStatusService.java` L31, `OUTBOX_STATUSES`) | T-07 (solo si cambia el alcance del TTL), T-08, T-12, T-16b | (a) Cambiar `OUTBOX_STATUSES` (`ChangeReservationStatusService.java` L30-L36) y corregir CU-14 spec L66 (solo lectura). (b) El código ya coincide con CU-14; M3 recibe `PENDING` en cada reintento de pago (`features/CU-14-*/plan.md` L19). |
| D-4 | ¿Hay garantía de orden entre eventos de la misma embarcación? | (a) No hay garantía; M1 trata cada `event_id` como idempotente: `features/CU-22-*/plan.md` L236; `context/consistencia-m2-m1.md` L161. (b) Garantía de orden: marcado `[NEEDS CLARIFICATION]` en `features/CU-22-*/spec.md` L94; los documentos no describen mecanismo | T-04 | (a) Sin cambio de diseño; el consumidor deduplica por `event_id`. (b) Requiere mecanismo de secuencia que los documentos no definen. |
| D-5 | ¿Qué códigos 4xx de M1 son permanentes? | (a) Todo 4xx es permanente (sin reintento): `features/CU-22-*/spec.md` L91; `context/consistencia-m2-m1.md` L159. (b) Lista acordada con M1: `features/CU-22-*/plan.md` L281 y `context/consistencia-m2-m1.md` L168 (pendiente) | T-03 | (a) Política simple, sin acuerdo previo. (b) Requiere lista de códigos y pruebas por código. |
| D-6 | ¿Qué mecanismo de identidad usa M3 al llamar a M2 (CU-13 canal A, CU-15)? | Los documentos no fijan mecanismo: `technical-plan/plan.md` L73 y L522 (item 1). Identidad de servicio para M3: `technical-plan/plan.md` L371 (T012). Se usan headers temporales: `context/contexto_m2.md` L27 | T-13, T-16c | Sin definir, el endpoint de M3 se apoya en headers `X-User-Id` y `X-User-Role` sin autenticación real. Definir mecanismo abre una decisión fuera de los documentos (ver D-13). |
| D-7 | ¿Qué SLA de CU-11 y CU-12 se fija? | Placeholders: `technical-plan/plan.md` L46 ("p. ej. 800 ms", "p. ej. 1500 ms"). Timeout documentado para M3: read 1000 ms (`technical-plan/plan.md` L297-L298). Límite de lote de M3: 50 (`technical-plan/plan.md` L522 item 2) | T-15 (criterio), T-16a (lote) | Con los timeouts de L297-L298 hay valor usable. Un SLA distinto cambia los timeouts de T-15. |
| D-8 | ¿La disputa admite varios reclamos o uno solo? ¿La política de reintento del job diferido? | `technical-plan/plan.md` L524 (item 3): reclamo múltiple o editable abierto; ventana de 24 h fija. Política de reintento: `[NEEDS CLARIFICATION]` en `features/CU-16-*/spec.md` (citado en `technical-plan/plan.md` L524) | T-18a, T-18b | Un reclamo: `dispute_claim` con `UNIQUE(dispute_id)` (`technical-plan/plan.md` L195). Varios: cambia la tabla o la restricción. |
| D-9 | ¿Qué versión de cada librería usar con Boot 4 (ShedLock, Resilience4j, springdoc, Testcontainers, WireMock, Awaitility)? | Verificar en Maven Central antes de agregar: `CLAUDE.md` (sección Dudas abiertas, punto 2); `context/contexto_m2.md` L76; `technical-plan/plan.md` L547-L549. Hoy no están en `pom.xml` (L7-L107) excepto ArchUnit (L97) | T-05, T-14, T-15, T-20, T-21 | Sin versión verificada no se agrega la dependencia. Si no hay versión compatible con Boot 4.1.2, el documento no define alternativa. |
| D-10 | ¿El alcance es 21 o 22 CU? | 22: `context/contexto_m2.md` L41 y L80; carpeta `features/CU-22-*/` existe. 21: `CLAUDE.md` L98 (rango CU-01..15, 19..21 y CU-16..18); `technical-plan/plan.md` L12 (dice 22 y luego enumera 21) | T-23 | Con 22, CU-22 necesita tarea (ver D-15). Con 21, se quita CU-22 del alcance; el documento no dice eso. |
| D-11 | ¿Se usan archivos `.yml` (plan) o `.yaml` (repo) para perfiles? | Plan: `technical-plan/plan.md` L156-L158 (`application.yml`, `application-dev.yml`, `application-test.yml`). Repo: `src/main/resources/application.yaml` (listado de `src/`). | T-19 | Mantener `.yaml`: cambia el texto del plan. Renombrar: cambia el archivo existente y su commit. |
| D-12 | ¿Qué es imprescindible y qué es deseable? | Los documentos fijan prioridad por fase: P1 para Fases 3 a 7 y P2 para Fases 8 y 9 (`technical-plan/plan.md` L389, L399, L409, L419, L429, L439, L449). Fase 10 sin prioridad (L459). Propuesta en la sección I.1 | Camino mínimo (I.1) | Aceptar la propuesta o cambiar niveles. |
| D-13 | ¿Qué seguridad aplica antes de la Fase 2? | (a) Headers temporales, sin Spring Security: `technical-plan/plan.md` L545-L546 (nota 2026-10-25); `context/contexto_m2.md` L27. (b) OAuth2/JWT: `technical-plan/plan.md` L27 (Spring Security OAuth2) y L328 (D-22, JWT). Los dos coexisten sin resolver. `technical-plan/plan.md` L73 y L522 (item 1) marcan la falta de mecanismo como bloqueante | T-11a, T-13, T-16c, y los endpoints de T-11b, T-11c, T-16a, T-16b, T-17a..d | (a) Identidad temporal, sin protección real. (b) Requiere dependencias y configuración que no están en `pom.xml`. |
| D-14 | ¿Dónde vive este documento? | `technical-plan/plan.md` L532: `context/`, `features/`, `diagrams/`, `templates/` son de solo lectura, con excepciones autorizadas por fecha. La instrucción de la v2 pide editar solo este archivo en `Documentación\context\`. | T-01 (commit de documentos), T-23 | Si se mantiene en `context/`, la regla de solo lectura exige excepción registrada. Si se mueve, el usuario debe indicar carpeta. |
| D-15 | ¿En qué fase va CU-22? | `technical-plan/plan.md` L349-L459 (Phase 1 a 10) no asigna CU-22 a ninguna fase. CU-22 aparece en `technical-plan/plan.md` L229 (C3) y L433 (FR-002 de CU-22, en Fase 7). Hay tarea de CU-22 solo implícita en T-08 (`context/implementContext.md` L194) | T-08, T-04, T-03 | (a) Asignarla a una fase existente (decisión del equipo). (b) Crear fase nueva. (c) Dejarla dentro de T-08 sin fase propia. |

### Qué puede empezar YA

No depende de ninguna decisión del grupo:

| Tarea | Motivo |
|---|---|
| T-02 (solo parte M3) | Exchange de M3 está definido: `technical-plan/plan.md` L223 |
| T-06 | Sin decisión bloqueante en la tabla |
| T-09 | Sin decisión bloqueante |
| T-10 | Sin decisión bloqueante |
| T-11b | Sin decisión bloqueante (leer spec de CU-20 antes, Fase 2) |
| T-22 | Sin decisión bloqueante |
| Verificación de versiones (D-9) | Solo consulta a Maven Central; no cambia archivos |

Bloqueos por decisión:

| Decisión | Tareas bloqueadas |
|---|---|
| D-1 | T-02 (M1), T-04, T-08, T-17a..d |
| D-2 | T-02 (M1), T-04 |
| D-3 | T-08, T-12, T-16b |
| D-4 | T-04 |
| D-5 | T-03 |
| D-6 | T-13, T-16c |
| D-7 | T-15 (criterio), T-16a |
| D-8 | T-18a, T-18b |
| D-9 | T-05, T-14, T-15, T-20, T-21 |
| D-10 | T-23 |
| D-11 | T-19 |
| D-13 | T-11a, T-13, T-16c |
| D-14 | T-01, T-23 |
| D-15 | T-08 (ubicación de CU-22) |

## E. Mapa de dependencias y camino crítico

Dependencias confirmadas por documento:

- Fase 2 bloquea a todas las CU: `technical-plan/plan.md` L478.
- CU-14 se dispara desde la máquina de estados: `technical-plan/plan.md` L413.
- CU-19 requiere la cotización individual de M3 (Fase 3): `technical-plan/plan.md` L423.
- CU-01, 02, 03 y 13 requieren Fases 3, 4, 5 y 6: `technical-plan/plan.md` L483.
- CU-04 y 05 requieren la zona horaria por puerto (CU-09): `technical-plan/plan.md` L443.
- Fase 9 depende de CU-07: `technical-plan/plan.md` L485.
- CU-22 es incluido por CU-08 y solo se dispara desde él: `features/CU-22-*/spec.md` L6.

Camino crítico (orden sugerido por dependencias documentadas; las decisiones D-1 a D-14 pueden bloquear cada paso):

T-01 (decisiones) → T-02 → T-03 → T-04 → T-12 (CU-14) → T-16a / T-16b / T-16c (embudo) → T-17a / T-17b / T-17c / T-17d (muelle) → T-18a (disputa, CU-16) → T-18b y T-18c → T-23.

Paralelo posible: T-05, T-06, T-09, T-10, T-14 y T-15 no dependen de la topología RabbitMQ (T-02). T-14 y T-15 dependen solo de D-9 (versiones): las lecturas de CU-09 a CU-12 son REST GET (`technical-plan/plan.md` L227-L231, C1, C2, C4, C5), no el transporte de CU-22.

Fuente de la ruta crítica: `technical-plan/plan.md` L477-L503.

---

## F. Reparto en paralelo

Bloques que pueden hacerse a la vez (sin tocar los mismos archivos):

- **Bloque 1 (documentación)**: T-01. Solo documentos.
- **Bloque 2 (infraestructura de mensajería)**: T-02, T-03, T-04. Tocan configuración AMQP y `OutboxAdapter.java`.
- **Bloque 3 (núcleo de estado)**: T-06, T-07, T-08, T-09, T-10. Tocan `ChangeReservationStatusService.java` y `ReservationStateMachine.java`.
- **Bloque 4 (integraciones de lectura)**: T-14, T-15. Tocan clientes nuevos y `FakeFleet*Adapter.java`.
- **Bloque 5 (vistas)**: T-11a, T-11b, T-11c. T-11b y T-11c tocan `ReservationQueryController.java` y `ReservationQueryService.java`; deben ir en serie entre sí. T-11a es nueva y puede ir en paralelo.
- **Bloque 7 (casos de uso de embudo y muelle)**: T-16a, T-16b, T-16c, T-17a, T-17b, T-17c, T-17d. Son CU distintos; comparten `ChangeReservationStatusService.java` (solo lectura para ellos) y no deben editarlo a la vez.
- **Bloque 8 (disputa)**: T-18a, T-18b, T-18c. Van en serie: T-18b y T-18c dependen de T-18a.
- **Bloque 6 (dependencias y configuración)**: T-05, T-19, T-20, T-21. Tocan `pom.xml` o archivos de configuración (ver archivos compartidos).

Archivos compartidos (no editar a la vez):

| Archivo | Tareas que lo tocan | Coordinación |
|---|---|---|
| `pom.xml` | T-05, T-20, T-21 | Una persona por vez; un commit por tarea |
| `src/main/resources/application.yaml` | T-19, T-02 | Una persona por vez |
| `ChangeReservationStatusService.java` | T-06, T-08, T-12 | Bloque 3 primero; T-12 después |
| `OutboxAdapter.java` | T-03, T-12 | T-03 primero |
| `ReservationMapper.java` (persistencia) | T-09 | Solo T-09 |

---

## G. Trampas de JPA con ddl-auto=validate

Solo las encontradas en código o documentos:

1. **Mezcla de tipos de tiempo para `timestamptz`**: la entidad usa `Instant` (`ReservationEntity.java` L94-L119, `OutboxMessageEntity.java` L40-L49, `ReservationStatusAuditEntity.java` L50), pero `IdempotencyKeyEntity.java` L21 usa `OffsetDateTime`. La respuesta web usa `OffsetDateTime` (`ReservationResponse.java` L49-L52). Revisar que cada entidad coincida con la columna `timestamptz` de V1/V2.
2. **`@EmbeddedId`**: `IdempotencyKeyEntity.java` L14. Revisar que la clave compuesta coincida con `UNIQUE(key, endpoint)` de `idempotency_key` (`technical-plan/plan.md` L199).
3. **`@Version`**: `ReservationEntity.java` L121. Solo `reservation` lo tiene en código. `technical-plan/plan.md` L194 pide `version` también en `dispute_guarantee`. Revisar al crear entidades de disputa.
4. **`LocalTime` para `departure_time`**: `ReservationEntity.java` L60 y V3 L12 (`SET NOT NULL`). Si la columna pasa a NOT NULL, la entidad no debe permitir `null`.
5. **`BigDecimal`**: `ReservationEntity.java` L75-L87 (columnas `numeric`). No sumar ni redondear (`contexto_m2.md` L34).
6. **`LocalDate` y rango**: `ReservationRepositoryPort.java` L22 y `ReservationJpaRepository.java` L24-L25; la restricción `EXCLUDE` de V1 usa `daterange`. Revisar consultas de solapamiento.

---

## H. Reglas de trabajo en equipo (escritas en archivos)

- GitFlow: rama principal `develop`; trabajo en `feature/<nombre>`; rama actual `feature/fase1-setup` (`CLAUDE.md` L144).
- Commits en español con prefijos `feat:`, `chore:`, `fix:`, `docs:` (`CLAUDE.md` L147).
- Un commit por tarea del plan (`CLAUDE.md` L147).
- Flujo: commit → `git push -u origin <rama>` → Pull Request a `develop` (`CLAUDE.md` L148).
- Stash viejo: nunca `git stash pop` (`CLAUDE.md` L146).
- Comandos destructivos (`git reset --hard`, `git clean`, `git stash drop`, `docker volume rm`, `docker compose down -v`): avisar y esperar confirmación (`CLAUDE.md` L149-L150).
- Revisar el diff antes de dar por terminada una tarea mecánica (`CLAUDE.md` L154).
- Archivos de solo lectura: `Documentación/context/`, `features/`, `diagrams/`, `templates/` (`technical-plan/plan.md` L532; `contexto_m2.md` L43).
- Migraciones aplicadas no se editan; cambios van en V4 o posterior (`contexto_m2.md` L21; `CLAUDE.md` L87).
- Archivos que no se commitean: `[NEEDS CLARIFICATION]`. No hay regla escrita en los documentos leídos.
- Regla de este equipo (fuente: instrucción del usuario para la v2; no es un archivo del repositorio): no commitear `.idea`, `CLAUDE.md`, `target/`, `untitled/` ni la carpeta `docs` (cualquier nivel).
- Ubicación de este documento: vive en `Documentación\context\`. `technical-plan/plan.md` L532 describe esa carpeta como de solo lectura. Ubicación por decidir (D-14): `[NEEDS CLARIFICATION]`.

---

## I. Qué se puede dejar fuera si no hay tiempo

Los documentos no fijan prioridades de tareas individuales. Lo que sí dicen:

- Fase 10 (polish) se marca como fase final: `technical-plan/plan.md` L459-L469.
- El plan dice que las tareas técnicas detalladas de cada CU se definen después: `technical-plan/plan.md` L395, L405, L415, L425, L435, L445, L455.
- Hueco conocido: CU-22 está no implementado (`implementContext.md` L73).

Clasificación de imprescindible / deseable: ver I.1 (propuesta, pendiente de D-12).

### I.1 Camino mínimo propuesto para el sábado

> PROPUESTA. El equipo debe confirmarla (D-12). No es decisión.
> Criterio: lo mínimo para que el sistema compile, los tests pasen y el flujo funcione de punta a punta según `technical-plan/plan.md` L433 (flujo "de la búsqueda al pago confirmado"). Donde los documentos no alcanzan, se marca `[NEEDS CLARIFICATION]`.

| Nivel | Qué incluye | Fuente del criterio | Tareas |
|---|---|---|---|
| Imprescindible | Fases 3, 4, 5 y 6 y embudo de Fase 7 (todas marcadas P1) | `technical-plan/plan.md` L389, L399, L409, L419, L429 (P1); L433 (flujo de punta a punta); L393 (CU-09..12 desbloquean CU-01 y CU-03) | T-02 (M3), T-03, T-04 (si CU-14 lo usa), T-06, T-07, T-11a, T-11b, T-11c, T-12, T-13, T-14, T-15, T-16a, T-16b, T-16c |
| Imprescindible (dependencia de D-15) | Notificación a M1 (CU-22): sin fase asignada en el plan | `technical-plan/plan.md` L229 (C3); ver D-15 | T-08 (parte M1), T-02 (M1) |
| Deseable | Fases 8 y 9 (P2): cancelación, inasistencia, check-in y check-out, y disputa | `technical-plan/plan.md` L439 y L449 (P2); L441-L443 (valor de negocio: ciclo operativo) | T-17a, T-17b, T-17c, T-17d, T-18a, T-18b, T-18c |
| Deseable | Infra de entrega: Dockerfile, README, perfiles, springdoc, Resilience4j, estilo | `technical-plan/plan.md` L361 (T009), L104-L108 (Dockerfile, README). Sin prioridad en documentos: `[NEEDS CLARIFICATION]` | T-19, T-20 |
| Deseable | Pruebas de integración con contenedores | `technical-plan/plan.md` L31 (Testcontainers, WireMock, Awaitility). `context/contexto_m2.md` L16 indica que hoy no se usa Testcontainers. Sin prioridad: `[NEEDS CLARIFICATION]` | T-21 |
| Deseable | Observabilidad y polish | `technical-plan/plan.md` L380 (T021); L459-L469 (Fase 10, sin prioridad) | T-22, T-23 |
| Hueco conocido | Si no alcanza el tiempo: CU-04 a CU-07 y CU-16 a CU-18 (Fases 8 y 9). Quedan documentados como P2. Sin plan de demo para ellas. | `technical-plan/plan.md` L439, L449 | T-17a..d, T-18a..c |
| Hueco conocido | Seguridad real (mecanismo de autenticación). Hoy solo headers temporales | `technical-plan/plan.md` L522 (item 1); D-13 | Sin tarea propia |

---

## J. Decisiones cerradas (solo las escritas en documentos)

| Decisión | Archivo y línea |
|---|---|
| Sin Spring Security; identidad por headers `X-User-Id` y `X-User-Role` | `CLAUDE.md` (sección Decisiones confirmadas); `contexto_m2.md` L27; `technical-plan/plan.md` L545 |
| Estado inicial llamado `INITIATED` | `CLAUDE.md` (sección Decisiones confirmadas); `technical-plan/plan.md` L551 |
| Identificadores en inglés; prosa en español | `technical-plan/plan.md` L329 (D-23); `contexto_m2.md` L49 |
| Formato de error `{ code, message }` | `contexto_m2.md` L28, L61 |
| Spring Boot 4.1.2, Java 21, Jackson 3 | `contexto_m2.md` L4, L29; `technical-plan/plan.md` L547 |
| Migraciones: nunca editar aplicadas; cambios en nueva migración | `contexto_m2.md` L21; `technical-plan/plan.md` L550 |
| Regla "sin dinero" | `contexto_m2.md` L34; `technical-plan/plan.md` L50 |
| Exchange de M3: `seashare.reservations` (topic) | `technical-plan/plan.md` L223; código `OutboxAdapter.java` L17 |
| Notificación a M1 no lleva montos | `features/CU-22-*/spec.md` L93, L120 (FR-007) |
| Reintentos: 5 intentos, backoff 1/5/25/125 s, solo ante 5xx o timeout | `technical-plan/plan.md` L67; `features/CU-22-*/spec.md` L123 (FR-006) |
| Los eventos de M1 no incluyen estado crudo de la reserva | `features/CU-22-*/spec.md` L110-L116 (FR-002) |
| Expiración por TTL: 15 min | `contexto_m2.md` L52; `implementContext.md` L97 |
| Zona horaria del puerto (no UTC del servidor) | `contexto_m2.md` L58; `technical-plan/plan.md` L55 |

Nota: `technical-plan/plan.md` L522 marca la seguridad como bloqueante (ver K).

---

## K. Preguntas abiertas [NEEDS CLARIFICATION]

1. **Transporte de notificación a M1 (AMQP o PATCH HTTP)**: determina si T-04 usa cola o cliente HTTP, y si T-02 declara topología para M1. Ver D-1.
2. **Nombres de exchange, routing key y cola de M1**: sin estos nombres no se puede implementar T-02 para M1. Fuente: `consistencia-m2-m1.md` L45-L47; `features/CU-22-*/plan.md` L64-L66.
3. **Garantías de orden entre eventos de la misma embarcación**: afecta diseño del consumidor (T-04). Fuente: `features/CU-22-*/spec.md` L94; `consistencia-m2-m1.md` L161.
4. **Códigos 4xx permanentes de M1**: afecta la política de DLQ (T-03). Fuente: `features/CU-22-*/plan.md` L281.
5. **PENDING_PAYMENT hacia M3**: `features/CU-08-*/plan.md` L214 dice que no se publica; `features/CU-14-*/spec.md` L66 y `features/CU-14-*/plan.md` L18 dicen que sí; el código lo publica (`ChangeReservationStatusService.java` L31). Afecta T-08 y T-12.
6. **Mecanismo de autenticación para identidad de servicio de M3**: bloquea T-13. Fuente: `technical-plan/plan.md` L73; L522 (item 1).
7. **SLA de CU-11 y CU-12**: afecta timeouts en T-15. Fuente: `technical-plan/plan.md` L46; L522 (item 2).
8. **Ventana de 24 h y reclamo múltiple en disputa**: afecta T-18a y T-18b. Ver D-8.
9. **Versiones compatibles con Boot 4 de ShedLock, Resilience4j, springdoc, Testcontainers, WireMock, ArchUnit (ya en `pom.xml`)**: afecta T-05, T-20, T-21. Fuente: `CLAUDE.md` (sección Dudas abiertas, punto 2); `technical-plan/plan.md` L547-L549.
10. **Número de CU**: 21 o 22. Afecta alcance de T-23. Fuente: sección L, inconsistencia 1.
11. **Criterios de aceptación de casi todas las tareas**: los documentos no los definen. Afecta todas las filas de D.
12. **Nombres de archivos de configuración (`application.yaml` vs `application.yml`)**: afecta T-19. Fuente: `technical-plan/plan.md` L156-L158; `src/main/resources/application.yaml`.
13. **Estimación de tamaño (S, M, L) y responsables**: no existen en documentos; son orientación de este documento. Afecta planificación.
14. **Fase de CU-22**: no tiene fase en `technical-plan/plan.md`. Ver D-15.
15. **Ubicación de este documento** (carpeta de solo lectura): ver D-14.
16. **Mecanismo de identidad de M3 y seguridad previa a Fase 2**: ver D-6 y D-13.
17. **Prioridades en Fase 10 y en tareas de infra**: no hay prioridad en documentos. Ver D-12 e I.1.
18. **CU-02 sin fila en la tabla D**: `implementContext.md` L203 (deuda 16) lo menciona. ¿Se agrega como fila nueva T-24 (ficha PROPUESTA)? Fuente: ficha T-24.
19. **Origen de `estimated_total`**: `features/CU-02-*/spec.md` L60 dice que lo entrega Módulo 3. `plan.md` L49 y `ReservationController.java` L60 lo reciben del cliente. ¿Cuál es la fuente válida? Ver ficha T-24.
20. **Autenticación de la creación de reserva**: `CU-02/plan.md` L33 pide JWT Bearer. El repositorio usa headers `X-User-Id` y `X-User-Role` (`CallerIdentityArgumentResolver.java` L28-L29). Ver D-13.
21. **Departure time en CU-02**: `CU-02/plan.md` L48 lo hace obligatorio. `CU-02/spec.md` no lo menciona. ¿Se agrega al spec o se deja solo en plan? Ver ficha T-24.
22. **CU-08 sobre la creación**: `CU-08/spec.md` L53 y `CU-02/plan.md` L13 dicen que la creación pasa por CU-08. El código crea directamente (`CreateReservationService.java` L125). ¿Se corrige el código o el spec? Ver ficha T-24.
23. **PENDING_PAYMENT hacia M3 dentro de CU-08**: `CU-08/plan.md` L145 dice "Sí"; L214 dice "no se notifica". ¿Cuál vale? Esto cierra D-3. Ver ficha T-12 y L.
24. **Guarda TTL en RESERVED**: `CU-08/plan.md` L213 la pone en CU-08. `CU-08/spec.md` no la define. ¿Se acepta como decisión del plan o se pide al spec? Ver ficha T-06.
25. **Creación sin auditoría**: `CU-08/plan.md` L198 incluye la creación en la auditoría. El código no audita la creación. ¿Es requisito? Ver ficha T-10.
26. **Campos de CU-14 faltantes** (`actor`, anticipación, novedades): `CU-14/spec.md` L67-L69 los exige. `CU-14/plan.md` L40-L47 no los incluye. Ver ficha T-12.
27. **Valores de `status` hacia M3**: `CU-14/plan.md` L51-L63 usa `PENDING` y `CANCELLED_*`. El código envía nombres internos (`OutboxAdapter.java` L33-L35). Ver ficha T-12.
28. **Estado `CANCELLED_FLEXIBLE` por cobro tardío**: el spec lo exige (`CU-14/spec.md` L31, L66). En código no existe ningún camino que lo produzca (búsqueda sin resultados en `src/main`). Ver ficha T-12.

Todas las decisiones están en la sección "Decisiones del grupo" (D-1 a D-15). K lista las preguntas sin repetir opciones.

---

## L. Inconsistencias

1. **Rango de CU**:
   - `CLAUDE.md` L98: `reservas` (CU-01..15, CU-19..21) y `disputa` (CU-16..18).
   - `Documentación/features/CU-22-recibir-informacion-estado-operativo/` existe (listado de carpeta).
   - `technical-plan/plan.md` L12: "22 casos de uso (CU-01 a CU-18 operativos y CU-19 a CU-21 de vista de consulta)".
   - `contexto_m2.md` L80: el plan alterna entre 21 y 22.
   - Decisión tomada por la instrucción del usuario: el alcance es CU-01 a CU-22.
2. **Transporte de notificación a M1 (AMQP vs PATCH)**:
   - AMQP: `consistencia-m2-m1.md` L9 y L166; `features/CU-22-*/plan.md` L7 y L48.
   - PATCH: `technical-plan/plan.md` L229; `features/CU-22-*/plan.md` L279 (dentro del mismo archivo que dice AMQP).
   - `contexto_m2.md` L79 reconoce la contradicción.
3. **Número de migraciones aplicadas**:
   - `CLAUDE.md` L83-L85 y `technical-plan/plan.md` L550: V1 y V2.
   - Repositorio: V1, V2, V3 (`src/main/resources/db/migration/`).
   - `contexto_m2.md` L78 reconoce V3. No hay V4 en `Documentación/` ni en `src/main/`.
4. **Estado PENDING_PAYMENT hacia M3**:
   - `features/CU-08-*/plan.md` L214: no se notifica.
   - `features/CU-14-*/spec.md` L66 y `features/CU-14-*/plan.md` L53 (mapea a `PENDING`): sí se notifica.
   - Código: `ChangeReservationStatusService.java` L31 incluye PENDING_PAYMENT en `OUTBOX_STATUSES`.
5. **Conteo de códigos de error**:
   - Afirmación de la nota: 20.
   - `shared/domain/ErrorCode.java` L8-L28: 21 constantes (conteo de 0.3, A.4).
   - `technical-plan/plan.md` L261-L283 lista códigos con otra numeración (no es fuente de conteo).
6. **Conteo de pruebas**:
   - `implementContext.md` L163: 75 métodos `@Test` (cifra de la última ejecución registrada, 2026-11-03).
   - `.\mvnw.cmd test` del 2026-11-06 (A.4, 0.5): 83 tests, 0 fallos, BUILD SUCCESS. Esta cifra es la verificada hoy.
   - Búsqueda de la cadena `@Test` en `src/test/java`: 83 líneas. Coincide con el conteo de Maven.
7. **Expiración TTL**:
   - `contexto_m2.md` L77 y `implementContext.md` L111/L116: solo INITIATED.
   - `contexto_m2.md` L52: la reserva puede expirar desde INITIATED y desde PENDING_PAYMENT según CU-08.
8. **CU-22 interno**: `features/CU-22-*/plan.md` L7 y L48 (AMQP) vs L279 (PATCH).
9. **Tipo de tiempo en entidades**: `ReservationEntity` usa `Instant`; `ReservationResponse` usa `OffsetDateTime` (`ReservationResponse.java` L49). No es contradicción de documentos; es riesgo técnico (sección G).
10. **CU-22 sin fase**: `technical-plan/plan.md` L349-L459 (Phase 1 a 10) no incluye CU-22. Aparece en L229 (C3) y L433. Ver D-15.
11. **Número de CU, dentro del mismo archivo**: `technical-plan/plan.md` L12 dice "22 casos de uso" y enumera CU-01 a CU-18 más CU-19 a CU-21 (21 CU). Ver D-10.
12. **Estado de CLAUDE.md**: `CLAUDE.md` L159 dice "Hecho: setup, V1 y V2 aplicadas". El repositorio tiene V3 (`src/main/resources/db/migration/`). El usuario indicó que la sección "Estado actual" de CLAUDE.md está desactualizada; no se usa como evidencia.
13. **Número de migraciones en `technical-plan/plan.md`**: L550 dice "V1 y V2" y L87 de `CLAUDE.md` dice "(V3, V4...)"; `contexto_m2.md` L78 reconoce V3. Ya cubierto en el punto 3.

Lote CU-02, CU-08, CU-14 (Fase 2):

14. **FR de CU-02 citados que no existen**: `CU-02/plan.md` L35 (FR-019), L163 (FR-018) y `CU-02/plan.event.md` L80 (FR-015). `CU-02/spec.md` solo define FR-001 a FR-014 (L57-L70).
15. **Autenticación de CU-02**: `CU-02/plan.md` L33 (JWT Bearer) vs `context/contexto_m2.md` L27 (headers temporales, sin Spring Security) y `CallerIdentityArgumentResolver.java` L28-L29.
16. **Origen de `estimated_total`**: `CU-02/spec.md` L60 (lo entrega Módulo 3) vs `CU-02/plan.md` L49 (lo envía el cliente y se guarda tal cual) y `ReservationController.java` L60 (código del cliente).
17. **Creación fuera de CU-08**: `CU-02/spec.md` L31 y `CU-02/plan.md` L13 (invoca CU-08) vs `CU-08/spec.md` L53 y L176 (único escritor) vs código `CreateReservationService.java` L125.
18. **`departure_time`**: `CU-02/plan.md` L48 (obligatorio) vs `CU-02/spec.md` (no lo menciona).
19. **Header `Location`**: `CU-02/plan.md` L101 (obligatorio en 201) vs `ReservationController.java` L31-L32 (no lo devuelve). Brecha de código, no de documento.
20. **Término "turista"**: `CU-02/spec.md` L5 ("Arrendatario (Turista / Cliente)") vs `context/contexto_m2.md` L48 (descarta "turista" en el vocabulario compartido).
21. **PENDING_PAYMENT hacia M3 dentro de CU-08**: `CU-08/plan.md` L145 (Sí) vs `CU-08/plan.md` L214 (no se notifica). A su vez, `CU-08/spec.md` L40 y L192 (FR-008) dicen Sí, y `CU-14/spec.md` L66 y `CU-14/plan.md` L18 dicen Sí.
22. **Intervalo y bloqueo del job TTL**: `CU-08/plan.md` L197 (cada 5 s, `FOR UPDATE SKIP LOCKED`) vs `TtlExpiryJob.java` L27 (30000 ms por defecto) y `ReservationJpaRepository.java` L36 (sin bloqueo).
23. **Código de error `PERSISTENCE_ERROR`**: `CU-08/plan.md` L168 vs `shared/domain/ErrorCode.java` L8-L28 (no existe).
24. **Campos de auditoría y reserva**: `CU-08/plan.md` L203 (`status_changed_at`), L204 (`actor_id`, `event_ids`) y L205 (`outbox_message.status`) vs V1 L11-L57 (sin `status_changed_at`), V1 L67-L76 (sin `actor_id` ni `event_ids`) y V1 L94-L107 (sin `status`).
25. **Auditoría de la creación**: `CU-08/plan.md` L198 ("incluida la creación") vs `CreateReservationService.java` (no llama a `StatusAuditPort`).
26. **Guarda TTL en RESERVED**: `CU-08/plan.md` L213 (decidido dentro de CU-08) vs `CU-08/spec.md` (no define expiración en RESERVED; escenario 3, L60-L63).
27. **Párrafo duplicado**: `CU-08/plan.md` L197 repite tres veces la misma frase sobre el adaptador de CU-13. Error editorial.
28. **Campos de CU-14**: `CU-14/spec.md` L67 (actor en el payload) y L68 (anticipación en `Cancelada`) vs `CU-14/plan.md` L40-L47 (sin actor ni anticipación) y L124 (M3 deduce la anticipación del estado).
29. **Novedades en `Completada`**: `CU-14/spec.md` L69 (FR-004) vs `CU-14/plan.md` L40-L47 y L109-L115 (sin campo de novedades).
30. **Valores de `status` hacia M3**: `CU-14/plan.md` L51-L63 (`PENDING`, `CANCELLED_*`) vs `OutboxAdapter.java` L33 y L35 (nombres internos `PENDING_PAYMENT`, `CANCELLED`).
31. **Nombre del evento**: `CU-14/spec.md` L77 (`StateTransitionEvent`) vs `technical-plan/plan.md` L342 (`ReservationStatusEvent`).
32. **Cobro tardío**: `CU-14/spec.md` L31 y L66 (publica `CANCELLED_FLEXIBLE`) vs código sin ninguna ruta que lo genere (`src/main`, búsqueda sin resultados).
33. **`Message-Id`**: `CU-14/plan.md` L35 (obligatorio) vs código (`OutboxAdapter.java` no fija headers; no hay relay).
34. **Mapeo de `m1_blocked`**: `features/CU-08-*/plan.md` L196 (`m1_blocked` controla la liberación) vs código (`m1_blocked` solo se escribe como `false`: `ReservationMapper.java` L54; no hay escritura a `true`).

---

## M. No verificado

- **Specs y planes de CU-01 a CU-21**: no leídos de forma completa. Solo existencia (listado de carpeta) y búsqueda puntual. Fase 2 los lee. Antes de cerrar T-11a..c, T-13, T-16a..c, T-17a..d y T-18a..c, leer `spec.md` y `plan.md` del CU correspondiente.
- **`features/CU-02-*/plan.event.md`**: no leído; solo existencia.
- **`features/CU-11-*/plan.external.*`**: nombres según listado y `technical-plan/plan.md` L230.
- **`context/sea-share.md`, `context/consistencia-m2-m3.md`**: solo búsqueda puntual.
- **`templates/`**: no leído.
- **`src/main/resources/application.yaml`**: no leído completo; `implementContext.md` L119-L122 describe su configuración.
- **`pom.xml`**: solo lista de `artifactId` (L7-L107). No se verificó la versión de cada dependencia.
- **Tests**: ejecutados el 2026-11-06 con Docker activo (A.4, 0.5): 83 tests, BUILD SUCCESS. `implementContext.md` L171 (Docker no disponible) queda superado por esta ejecución.
- **Tests de cada tarea futura**: no existen para las tareas nuevas. Se completan en Fase 2 (ver Definición de terminado).
- **Clases de persistencia y web**: no leídas completas; solo búsqueda de patrones.
- **Carpeta docs**: ignorada por instrucción. No leída, no citada como fuente. Búsqueda de nombre `docs` a cualquier nivel: ver salida en la sección de verificación del chat.
- **`Documentación/diagrams/`**: imagen; no leída.
- **Conteo exacto de pruebas**: ver inconsistencia 6.
- **Presencia de `ReservationQueryController` y `ReservationController` como CU-20/21 y CU-02**: confirmada por endpoints; la asociación con el número de CU viene de `implementContext.md`.
- **Lectura de specs y planes de CU-01 a CU-21 (Fase 2)**: pendiente, salvo CU-02, CU-08 y CU-14 (lote de esta pasada). CU-01, CU-03 a CU-07, CU-09 a CU-13, CU-15 a CU-21 siguen sin leer completos.
- **CU-02, CU-08 y CU-14 leídos completos (spec y plan)**: `features/CU-02-iniciar-reserva/spec.md`, `plan.md`, `plan.event.md`; `features/CU-08-actualizar-estado-reserva/spec.md`, `plan.md`; `features/CU-14-recibir-estado-reserva/spec.md`, `plan.md`.
- **Código del lote leído**: completo: `CreateReservationService.java`, `ReservationController.java`, `CreateReservationRequest.java`, `CreateReservationCommand.java`, `ReservationInfoProvidedEvent.java`, `OutboxAdapter.java`, `FakeFleetInfoAdapter.java`, `IdempotencyAdapter.java`, `ReservationResponse.java`, `GlobalExceptionHandler.java`, `Reservation.java`, `ChangeReservationStatusCommand.java`, `ReservationRepositoryAdapter.java`, `StatusChangedEvent.java`, `StatusAuditAdapter.java`, V1 y V3, `CallerIdentityArgumentResolver.java`, `ReservationJpaRepository.java`. Ya leídos en fases anteriores: `ChangeReservationStatusService.java`, `ReservationStateMachine.java`, `ExpireReservationsService.java`, `ErrorCode.java`.
- **Tests del lote**: solo nombres de métodos y líneas (`grep`), no cuerpo completo. Ficha T-24 y T-06 dependen de leer el cuerpo antes de implementar.
- **Tests de integración no ejecutados en esta pasada**: la ejecución completa de `.\mvnw.cmd test` está en A.4 (83 tests, BUILD SUCCESS, fase anterior).

