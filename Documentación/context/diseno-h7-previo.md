# Diseño previo H7 — T-24 y salida de `EXPIRED`

**Fecha:** 2026-11-12  
**Rama de referencia:** `feature/h7-embudo-conversion` (HEAD `1e49661`)  
**Modo:** lectura estática; no se modifica `src/`.  
**Objetivo:** desbloquear las dos decisiones que impiden implementar H7: (D1) creación vía CU-08/T-24 y (D2) salida de `EXPIRED` para el cobro tardío de CU-13.

> **Naturaleza del documento:** propuesta de diseño. No crea código, migraciones ni commits.

---

## 0. Resumen ejecutivo

- **D1:** la creación debe entrar al motor CU-08 mediante un **punto de entrada de creación** (`initiate`), no mediante el `execute` genérico de cambio de estado. CU-02 conserva validaciones, idempotencia y cálculo del TTL; CU-08 conserva la escritura de estado, la auditoría y el outbox.
- **D1:** el evento `reservation.info.provided` y la fila de auditoría `null → INITIATED` deben escribirse con el **mismo `event_id`** y dentro de la **misma transacción** que la reserva. La opción de pre-persistir una reserva y luego “transicionarla” a `INITIATED` se rechaza por alto riesgo y por violar el “único escritor”.
- **D2:** el contrato financiero necesita publicar `CANCELLED_FLEXIBLE`; con el código actual eso solo se consigue con estado `CANCELLED` + subtipo `FLEXIBLE`. La transición exacta sería `EXPIRED → CANCELLED` con subtipo `FLEXIBLE`, actor `SYSTEM` y causa explícita `LATE_PAYMENT`.
- **D2:** la spec de CU-08 declara `Expirada` terminal e inmutable (`CU-08/spec.md:168`), y el plan de CU-13 dice que la reserva “permanece Expirada” (`CU-13/plan.md:25`). Por tanto, la transición real exige una **excepción formal y acotada**, no abrir la matriz genérica.
- **D2:** la recomendación es una **política de dominio dedicada** que no toca `ReservationStateMachine.canTransition`, no agrega `EXPIRED → CANCELLED` a la matriz y no rompe `finalStatesHaveNoExit`. Si M3 exige que M2 siga en `EXPIRED`, la alternativa es un evento financiero sin cambio de estado.
- **D2:** `m1_blocked` permanece `false`, no se envía nada a M1 y el job de TTL no requiere cambios; el evento a M3 se publica como `CANCELLED_FLEXIBLE` y la auditoría registra la fila `EXPIRED → CANCELLED`.

---

# Decisión 1 — T-24: creación vía CU-08

## 1.1 Contexto verificado

### Flujo actual de creación

- `CreateReservationService.java:36` ejecuta todo bajo `@Transactional`.
- `CreateReservationService.java:79-87` normaliza y reclama la clave de idempotencia; si ya existe, devuelve la reserva original sin crear otra.
- `CreateReservationService.java:112` calcula `ttlExpiresAt` con `TtlPolicy` a partir de un único `now`.
- `CreateReservationService.java:131` persiste directamente con `reservationRepositoryPort.create(Reservation.initiate(newReservation))`.
- `CreateReservationService.java:134-145` genera un `event_id` determinístico y encola `reservation.info.provided`.
- `CreateReservationService.java:147-157` escribe la auditoría de creación `null → INITIATED` con `event_ids = [eventId]`.
- `CreateReservationService.java:159` completa la idempotencia con el id de la reserva.

### Motor CU-08 actual

- `ChangeReservationStatusService.java:63-64` solo trabaja sobre una reserva **ya existente**, buscada por `reservationId`.
- `ChangeReservationStatusService.java:76` aplica la transición con `reservation.changeStatus(...)`.
- `ChangeReservationStatusService.java:96-106` escribe la auditoría de la transición.
- `ChangeReservationStatusService.java:108-128` encola los eventos M3/M1 de la transición.
- `ChangeReservationStatusService.java:130-138` devuelve el resultado tipado.
- `ReservationStateMachine.java:26-32` devuelve `false` para `from == null`; no existe una transición de creación en la matriz.
- `Reservation.java:128-150` crea el agregado ya con `status = INITIATED`; `Reservation.java:160-164` cambia el estado validando con la máquina.

### Contratos y documentación

- `CU-02/spec.md:11` declara `Actualizar estado reserva` como `<<include>>` para crear la reserva en `Iniciada`.
- `CU-02/spec.md:67-68` exige invocar CU-08 y encender el TTL de 15 minutos.
- `CU-02/plan.md:13` describe el paso 3: invocar CU-08 para persistir por primera vez en `INITIATED`.
- `CU-02/plan.md:35` exige `Idempotency-Key`; `CU-02/plan.md:172` exige TTL durable.
- `CU-08/spec.md:22` dice que cualquier creación o cambio de estado debe pasar por CU-08.
- `CU-08/spec.md:29` incluye “o crearla en `Iniciada`” en el flujo principal.
- `CU-08/spec.md:53` fija la creación en `Iniciada` como único punto de entrada a la máquina.
- `CU-08/spec.md:176` (FR-001) declara CU-08 el único lugar donde se actualiza el estado.
- `CU-08/spec.md:182` (FR-003) incluye `Creación → Iniciada`.
- `CU-08/spec.md:195` (FR-011) exige que el `eventId` se genere en la misma transacción local que la transición.
- `CU-08/plan.md:54` ya contempla un comando de creación aparte, solo desde CU-02.
- `CU-08/plan.md:67` incluye la fila `(creación) → INITIATED` disparada por CU-02 con actor `RENTER`.
- `CU-08/plan.md:191` exige “un solo escritor de estado”.
- `CU-08/plan.md:194` exige transacción única para lectura, validación, cambio, auditoría y outbox.
- `CU-08/plan.md:198` incluye la creación como primera fila inmutable de `reservation_status_audit`.

### Brecha verificada

- `auditoria-h1-h6.md:26` y `:77` (F-01) confirman que T-24 no está cumplido porque `CreateReservationService.java:131` persiste directamente.
- `auditoria-h1-h6.md:184` (P-02) lo marca como precondición NO cumplida para H7.
- `auditoria-h1-h6.md:236` recomienda cerrar T-24 antes de seguir.

### Pruebas que hoy dependen del flujo

- `CreateReservationServiceTest.java:72-80` construye el servicio con 7 dependencias, incluida `StatusAuditPort`.
- `CreateReservationServiceTest.java:118` simula `reservationRepositoryPort.create(...)`.
- `CreateReservationServiceTest.java:125` verifica el encolado de `ReservationInfoProvidedEvent`.
- `CreateReservationServiceTest.java:139` verifica `idempotencyPort.complete(...)`.
- `CreateReservationServiceTest.java:166` verifica que una repetición idempotente no llama a `create`.
- `CreateReservationOutboxIntegrationTest.java:132-146` verifica la fila outbox y el `event_id` determinístico.
- `CreateReservationOutboxIntegrationTest.java:210-255` verifica el rollback conjunto de reserva y outbox cuando falla `idempotencyPort.complete`.
- `ChangeReservationStatusServiceTest.java:406-416` verifica la auditoría de creación `null → INITIATED`.
- `ChangeReservationStatusServiceTest.java:461-471` verifica que la creación en `INITIATED` no envía eventos a M1.

---

## 1.2 Restricciones que la solución no puede romper

1. **Idempotencia:** `tryClaim`/`findReservationId`/`complete` siguen gobernando la creación; una repetición no crea otra reserva ni otro evento (`CreateReservationService.java:79-87,159`).
2. **TTL:** `ttlExpiresAt` se calcula una sola vez con el `now` de la petición y no se reinicia; el TTL debe seguir siendo exactamente 15 minutos (`CU-02/plan.md:172`; `CreateReservationTest.java:159-161`).
3. **Auditoría de creación:** debe existir una sola fila `null → INITIATED`, con actor `RENTER`, `actor_id = renterId`, motivo de creación y `event_ids = [reservation.info.provided]` (`CU-08/plan.md:198`; `ChangeReservationStatusServiceTest.java:406-416`).
4. **Evento `reservation.info.provided`:** debe conservar el `event_id` determinístico y los 7 campos exactos del contrato (`CreateReservationOutboxIntegrationTest.java:145-166`).
5. **Atomicidad:** reserva, auditoría, outbox e idempotencia se confirman juntas o no se confirma nada (`CU-08/spec.md:195`; `CU-08/plan.md:194`).
6. **Único escritor:** ninguna clase fuera del motor CU-08 debe persistir el estado inicial (`CU-08/plan.md:191`).

---

## 1.3 Alternativa A (recomendada): segundo punto de entrada en el motor CU-08

### Idea

Agregar al motor CU-08 un punto de entrada de **creación** que valide la transición lógica `null → INITIATED`, persista la reserva, escriba la auditoría de creación y encole `reservation.info.provided`, todo en la misma transacción. `CreateReservationService` deja de persistir y de escribir auditoría/outbox; conserva validaciones, idempotencia y TTL.

### Diseño propuesto

1. **Puerto nuevo:** `InitiateReservationUseCase` en `application/port/in`, implementado por `ChangeReservationStatusService` (la misma clase que implementa `ChangeReservationStatusUseCase`). Se prefiere un puerto separado antes que agregar un método al puerto genérico para no contaminar el contrato de cambio de estado.
2. **Comando nuevo:** `InitiateReservationCommand` con:
   - `NewReservation` (ya contiene `renterId`, `vesselId`, `ownerId`, fechas, pasajeros, titular, `estimatedTotal`, `departureTime`, `harborTimezone`, `createdAt`, `ttlExpiresAt`);
   - `maxCapacity` (dato de M1 necesario para `reservation.info.provided`, que no se persiste en `Reservation`).
3. **Resultado:** devolver `Reservation` o `ReservationView`; se recomienda `Reservation` para que `CreateReservationService` siga construyendo la vista.
4. **Regla de dominio:** agregar a `ReservationStateMachine` un método explícito `validateCreation(ReservationStatus target)` que solo acepte `INITIATED`. **No** agregar una entrada `null` al `EnumMap` ni cambiar `canTransition(null, ...)`, que debe seguir devolviendo `false` para cualquier llamada genérica.
5. **Implementación de `initiate`:**
   1. validar `machine.validateCreation(INITIATED)`;
   2. `Reservation created = repository.create(Reservation.initiate(command.newReservation()))`;
   3. generar `eventId = UUID.nameUUIDFromBytes(("reservation.info.provided:" + created.getId()))`;
   4. `outbox.enqueue(new ReservationInfoProvidedEvent(... maxCapacity ...))`;
   5. `statusAudit.record(new StatusAuditEntry(created.getId(), null, INITIATED, null, RENTER, "Reserva creada por CU-02", command.createdAt(), command.renterId(), List.of(eventId)))`;
   6. devolver `created`.
6. **`CreateReservationService`:** conserva `now`, validaciones de fechas/zona/capacidad/disponibilidad/solapamiento, `tryClaim`, cálculo de `ttlExpiresAt` y `complete`; sustituye la persistencia + auditoría + outbox por una llamada a `initiateUseCase.initiate(...)`. Deja de necesitar `OutboxPort` y `StatusAuditPort`; conserva `ReservationRepositoryPort` para `findById` y `existsOverlap`.

### Por qué cumple

- La creación pasa por el motor que ya concentra la matriz, la auditoría y el outbox.
- La transición lógica `null → INITIATED` queda validada por una regla explícita y no queda expuesta a otros flujos.
- El evento `reservation.info.provided` se conserva con el mismo contrato y el mismo `event_id` determinístico.
- No hay migración de base de datos.
- No se altera el comportamiento de idempotencia ni el TTL.

### Pros

- Único escritor real: desaparece la escritura directa de `CreateReservationService.java:131`.
- Auditoría y outbox quedan junto al estado, en la misma transacción.
- Reutiliza `ReservationMapper`, `StatusAuditAdapter` y `OutboxAdapter` existentes.
- Alinea el código con `CU-08/spec.md:176,182` y `CU-08/plan.md:54,191,194`.
- Bajo riesgo de regresión funcional: el contrato HTTP de CU-02 y los campos de respuesta no cambian.

### Contras

- `ChangeReservationStatusService` asume una responsabilidad nueva (creación), aunque sea un punto de entrada separado.
- Cambia el constructor de `CreateReservationService` y obliga a reescribir `CreateReservationServiceTest`.
- Hay que pasar `maxCapacity` desde CU-02 a CU-08; es un dato adicional en el comando.
- Introduce una dependencia de aplicación CU-02 → puerto de CU-08; es la dependencia que el propio `<<include>>` documenta.

### Archivos afectados

| Archivo | Cambio |
|---|---|
| `src/main/java/.../application/service/CreateReservationService.java` | Delegar creación; quitar persistencia/auditoría/outbox directas; ajustar constructor. |
| `src/main/java/.../application/service/ChangeReservationStatusService.java` | Implementar `initiate`; extraer helper de auditoría/outbox si conviene. |
| `src/main/java/.../application/port/in/InitiateReservationUseCase.java` | Nuevo puerto. |
| `src/main/java/.../application/dto/InitiateReservationCommand.java` | Nuevo comando. |
| `src/main/java/.../domain/state/ReservationStateMachine.java` | `validateCreation(INITIATED)`. |
| `src/main/java/.../domain/model/Reservation.java` | Opcional: método de fábrica/validación de creación; evitar `changeStatus` desde `null`. |
| `src/test/java/.../application/service/CreateReservationServiceTest.java` | Reescribir expectativas; verificar delegación. |
| `src/test/java/.../integration/ChangeReservationStatusServiceTest.java` | Añadir pruebas de `initiate`. |
| `src/test/java/.../integration/CreateReservationOutboxIntegrationTest.java` | Verificar que sigue pasando; añadir comparación auditoría/outbox. |
| `src/test/java/.../domain/state/ReservationStateMachineTest.java` | Probar `validateCreation` y que `canTransition(null, ...)` sigue en `false`. |

### Riesgo de regresión

**Medio-bajo.** Los puntos concretos:

- `CreateReservationServiceTest` deja de poder verificar `create`/`enqueue`/`record` directamente; hay que mover esas aserciones al test de CU-08 o a integración. Es el cambio más visible.
- `CreateReservationOutboxIntegrationTest` debería seguir verde si CU-08 escribe el evento antes de `idempotencyPort.complete`; el rollback de `:210-255` sigue funcionando.
- `ChangeReservationStatusServiceTest.creationWritesAuditRow` (`:406-416`) debería seguir verde porque la creación sigue escribiendo la misma fila, ahora desde CU-08.
- `ChangeReservationStatusServiceTest.initiatedAndPendingPaymentDoNotEnqueueModuleOne` (`:461-471`) no debería romperse: la creación encola `ReservationInfoProvidedEvent`, no `OperationalStatusEvent` ni `StatusChangedEvent`.
- Si se añade una regla ArchUnit de “solo CU-08 escribe estado”, esta alternativa la cumple; la alternativa B no.

---

## 1.4 Alternativa B: pre-persistir una reserva y luego aplicarle una transición inicial en CU-08

### Idea

`CreateReservationService` persiste primero una reserva “shell” con `status = INITIATED` (o un nuevo estado `DRAFT`) y después invoca el `execute` genérico de CU-08 con destino `INITIATED`. Para que la llamada sea válida habría que permitir `INITIATED → INITIATED` (auto-transición) o `DRAFT → INITIATED`.

### Problemas

1. **Sigue existiendo escritura fuera de CU-08:** la primera persistencia la hace `CreateReservationService`; T-24 exige que ningún código de creación quede fuera del motor.
2. **Rompe la auditoría de creación:** habría dos escrituras (shell + transición) o una fila `DRAFT → INITIATED`; se pierde la fila única `null → INITIATED` que exige `CU-08/plan.md:198` y que verifica `ChangeReservationStatusServiceTest.java:406-416`.
3. **Abre transiciones artificiales:** una auto-transición `INITIATED → INITIATED` o un estado `DRAFT` contamina la máquina y la matriz. `DRAFT` obliga a tocar:
   - `ReservationStatus` y todos los `switch` exhaustivos (`FinanceStatusMapper.java:12-20`, `ModuleNotificationMatrix.java:39-60`);
   - el `CHECK` de `reservation.status` (`V1__init.sql:45-49`);
   - consultas, filtros, GiST y contratos de CU-20/CU-21.
4. **Idempotencia y atomicidad más frágiles:** si la primera persistencia y la transición no comparten exactamente la misma transacción, queda una reserva huérfana; si comparten, el “primer escritor” sigue siendo CU-02.
5. **Evento `reservation.info.provided`:** si se encola en CU-02, el `event_id` y la auditoría no quedan en el mismo escritor; si se encola en CU-08, hay que pasarle igualmente los datos de M1 y ya no se gana nada frente a la alternativa A.
6. **TTL:** la fila shell necesitaría `ttl_expires_at` desde el inicio; el segundo paso no debe reiniciarlo. Es complejidad añadida sin beneficio.

### Pros

- No exige un método nuevo en el puerto de CU-08; reutiliza `execute`.
- El cambio inicial en `ChangeReservationStatusService` sería menor.

### Contras

- No cumple el objetivo real de T-24 (“ningún código de creación fuera de CU-08”).
- Introduce estados/transiciones artificiales y deuda en la máquina.
- Alto riesgo de regresión en DB, mapeos, consultas y pruebas.

### Veredicto

**Rechazada.** Es una solución aparente: hace que la creación “llame” a CU-08, pero deja la primera escritura y la primera auditoría fuera del motor.

---

## 1.5 Cómo se garantiza que `event_ids` y la auditoría sigan en la misma transacción

La garantía no debe depender de la suerte del orden de llamadas; debe ser estructural:

1. **Una sola transacción externa:** `CreateReservationService.execute` sigue anotado `@Transactional` (`CreateReservationService.java:36`).
2. **Propagación `REQUIRED`:** `InitiateReservationUseCase.initiate` se invoca a través del proxy Spring; la clase `ChangeReservationStatusService` mantiene `@Transactional` a nivel de clase (`ChangeReservationStatusService.java:32-34`). No debe usar `REQUIRES_NEW`, `@Async` ni un `TransactionTemplate` independiente.
3. **Mismo `EntityManager`:** `ReservationRepositoryAdapter.create`, `StatusAuditAdapter.record` y `OutboxAdapter.enqueue` usan repositorios JPA que participan en la transacción actual. La reserva se persiste con `saveAndFlush` (`ReservationRepositoryAdapter.java:75-80`) y la auditoría/outbox se insertan antes del commit.
4. **Un solo `event_id`:** dentro de `initiate` se calcula una vez:
   ```java
   UUID eventId = UUID.nameUUIDFromBytes(
       ("reservation.info.provided:" + created.getId()).getBytes(StandardCharsets.UTF_8));
   ```
   Ese mismo valor se pasa a `OutboxPort.enqueue` y a `StatusAuditEntry.eventIds()`. No se genera un UUID aleatorio distinto para cada escritura.
5. **Auditoría y outbox antes del `complete`:** el orden recomendado es `create` → `enqueue` → `record` → `complete`. Si cualquiera falla, la transacción revierte la reserva, la fila de auditoría, la fila de outbox y el `complete` de idempotencia. El test `forcedFailureAfterEnqueueRollsBackReservationAndOutbox` (`CreateReservationOutboxIntegrationTest.java:210-255`) sigue siendo el modelo.
6. **Sin publicación dentro de la transacción:** el relay del outbox publica después del commit; la transacción solo inserta la fila. Esto respeta `CU-08/plan.md:194` y evita que un fallo del broker revierta la creación.
7. **Prueba de atomicidad específica:** leer `reservation_status_audit.event_ids` y `outbox_message.event_id` de la misma reserva y comparar que contienen exactamente el mismo UUID; forzar un fallo en `StatusAuditPort.record` y verificar que no queda reserva ni outbox.

---

## 1.6 Recomendación D1

**Alternativa A.** Es la única que cumple simultáneamente:

- creación dentro del único escritor;
- auditoría `null → INITIATED` real;
- `event_ids` y outbox atómicos;
- conservación de idempotencia, TTL y contrato de `reservation.info.provided`;
- mínimo cambio de esquema (ninguno).

La alternativa B se descarta por violar el objetivo de T-24 y abrir la máquina a estados/transiciones artificiales.

---

## 1.7 Pruebas necesarias D1

### Unitarias

1. `creationDelegatesToInitiateUseCase`: `CreateReservationService` llama una vez a `InitiateReservationUseCase.initiate` y no a `ReservationRepositoryPort.create`.
2. `idempotentRepeatDoesNotCallInitiate`: con `tryClaim = false` no se llama a `initiate` y se devuelve la reserva existente.
3. `initiateRejectsNonInitiatedCreationTarget`: `validateCreation` solo acepta `INITIATED`.
4. `genericCanTransitionStillRejectsNullOrigin`: `canTransition(null, INITIATED)` sigue en `false`.
5. `creationTtlIsFifteenMinutesFromSingleClockRead`: `createdAt` y `ttlExpiresAt` salen del mismo `now`.

### Integración

6. `creationGoesThroughStatusUseCase`: la reserva nace por el puerto de CU-08; `CreateReservationService` no invoca `repository.create` directamente.
7. `creationWritesAuditAndOutboxWithSameEventId`: `audit.event_ids[0] == outbox.event_id`.
8. `creationRollsBackAuditAndOutboxOnFailure`: fallo en auditoría u outbox no deja reserva, auditoría ni outbox.
9. `creationKeepsInfoProvidedPayload`: siguen los 7 campos exactos y el `event_id` determinístico (`CreateReservationOutboxIntegrationTest.java:148-166`).
10. `creationDoesNotEmitStatusChangedOrOperationalStatus`: solo se encola `ReservationInfoProvidedEvent`.
11. `idempotentRepeatCreatesNoSecondOutboxMessage`: se mantiene `:170-208`.
12. `locationHeaderRemains`: `CreateReservationTest.java:148` sigue verde; `Location` ya se emite en `ReservationController.java:65`.

### Regresión obligatoria

13. `creationWritesAuditRow` (`ChangeReservationStatusServiceTest.java:406-416`).
14. `initiatedAndPendingPaymentDoNotEnqueueModuleOne` (`:461-471`).
15. `forcedFailureAfterEnqueueRollsBackReservationAndOutbox` (`CreateReservationOutboxIntegrationTest.java:210-255`).
16. `sameIdempotencyKeyReturnsSameReservation` (`CreateReservationTest.java:165-205`).

---

## 1.8 Preguntas abiertas D1

1. **Propiedad del evento `reservation.info.provided`:** ¿se acepta que lo escriba CU-08 en su punto de entrada de creación, o se quiere mantener la propiedad conceptual en CU-02? La recomendación es CU-08 para garantizar atomicidad.
2. **Puerto:** ¿se prefiere un `InitiateReservationUseCase` separado o agregar `initiate` a `ChangeReservationStatusUseCase`? La recomendación es separado.
3. **`maxCapacity`:** ¿se pasa desde `FleetInfo` en el comando de creación? Es necesario para el payload y no está en `Reservation`.
4. **`estimated_total`:** sigue abierto el origen M3/cliente de T-24 (`plan-trabajo-backend.md:117`, Anexo B); no lo resuelve esta decisión.
5. **Regla ArchUnit:** ¿se añade una regla que prohíba `reservationRepositoryPort.create` fuera de CU-08? La alternativa A la permite; conviene añadirla en T-22.
6. **`Location`:** ya está implementado en `ReservationController.java:65`; confirmar que T-24 no espera otro cambio de contrato.

---

# Decisión 2 — Salir de `EXPIRED` (CU-13, cobro tardío)

## 2.1 Contexto verificado

### La máquina y el servicio actuales

- `ReservationStateMachine.java:22-23` fija `EXPIRED` y `PAYMENT_FAILED` sin salidas.
- `ReservationStateMachine.java:16-19` solo permite `INITIATED → {PENDING_PAYMENT, EXPIRED}`, `PENDING_PAYMENT → {RESERVED, EXPIRED, PAYMENT_FAILED}`, `RESERVED → {IN_NAVIGATION, CANCELLED}`, `IN_NAVIGATION → COMPLETED`.
- `ChangeReservationStatusService.java:146-149` rechaza cualquier transición fuera de la matriz.
- `ChangeReservationStatusService.java:159-165` rechaza toda cancelación que no venga de `RESERVED`.
- `ChangeReservationStatusService.java:180-197` rechaza `SYSTEM` con cualquier subtipo de cancelación.
- `ChangeReservationStatusService.java:199-208` exige subtipo cuando el destino es `CANCELLED`.
- `ChangeReservationStatusService.java:216-226` restringe `payment_reference` a `RESERVED`.
- `ChangeReservationStatusService.java:247-256` solo valida TTL cuando el destino es `RESERVED`.

### Lo que dicen los documentos

- `CU-08/spec.md:168`: “Los estados finales no se pueden tocar nunca más: `Completada`, `Cancelada`, `Expirada` y `Pago Fallido` son definitivos”.
- `CU-08/spec.md:180`: lista los cuatro estados terminales.
- `CU-08/spec.md:182-187`: matriz oficial sin salida de `EXPIRED`.
- `CU-13/spec.md:78-81`: si el pago aprobado llega tras el TTL, M2 **no** puede confirmar la reserva; debe rechazar y pedir reversión.
- `CU-13/spec.md:103-106` (FR-006): no transicionar a `RESERVED`; responder rechazo y ordenar reversión.
- `CU-13/spec.md:107` (FR-007): idempotencia estricta por `(external_transaction_id, result)`.
- `CU-13/plan.md:25`: el Canal B publica `CANCELLED_FLEXIBLE` y **“la reserva en M2 permanece Expirada”**.
- `CU-13/plan.md:260`: la instrucción de reversión se envía como estado `CANCELLED_FLEXIBLE` del evento CU-14.
- `CU-13/plan.external.md:123`: con la reserva ya `Expirada` o en `Pago Fallido`, M2 publica `CANCELLED_FLEXIBLE` a M3 y genera alerta crítica.
- `CU-14/plan.md:62`: “Expirada con cobro aprobado tardío → `CANCELLED_FLEXIBLE` (mapeo por tratamiento financiero: M3 reembolsa 100 %)”.
- `CU-14/plan.md:63`: “Iniciada, Expirada sin cobro, Pago Fallido → No se publican”.
- `CU-14/spec.md:31` y `:66`: única excepción a la no publicación de `Expirada`: el cobro aprobado tardío, que se publica como `CANCELLED_FLEXIBLE`.
- `CU-08/plan.md:214`: única excepción de cobro tardío, publicada como `CANCELLED_FLEXIBLE`.
- `CU-22/spec.md:114-115`: la cancelación solo se notifica a M1 si la reserva ya llegó a `Reservada`; `Expirada` nunca se notifica.
- `FinanceStatusMapper.java:17` y `:23-32`: `CANCELLED` + `FLEXIBLE` → `CANCELLED_FLEXIBLE`; `CANCELLED` + `LATE` → `CANCELLED_STRICT`.
- `FinanceStatusMapper.java:18-19`: `EXPIRED` no se puede publicar a M3.

### La brecha

- `auditoria-h1-h6.md:27` y `:78` (F-02): hoy `EXPIRED → CANCELLED_FLEXIBLE` es imposible.
- `auditoria-h1-h6.md:185` (P-03): es precondición NO cumplida de H7.
- `auditoria-h1-h6.md:237` recomienda habilitar la transición tardía sin romper la inmutabilidad de los demás terminales.
- `plan-trabajo-backend.md:120` (T-16c) exige `approvedAfterTtlPublishesCancelledFlexible`.
- `plan-trabajo-backend.md:285` ya registra la inconsistencia L30 entre `CANCELLED_FLEXIBLE` por cobro tardío y el código, que no tiene ruta para generarlo.

### Estado de persistencia

- `V1__init.sql:45-49` permite `CANCELLED` y `FLEXIBLE`; no hay `CHECK` que impida `EXPIRED → CANCELLED`.
- `V1__init.sql:53-56` excluye `CANCELLED` y `EXPIRED` de la restricción GiST de solapamiento.
- `V1__init.sql:67-89` permite insertar nuevas filas de auditoría; el trigger solo bloquea `UPDATE`/`DELETE`.
- `ReservationStatus.java:10,13-15` marca `EXPIRED` como final.
- `ReservationStateMachineTest.java:32-41` verifica que todo estado `isFinal()` no tenga salidas.

---

## 2.2 ¿Qué transición exacta se necesita?

Para que el contrato financiero de M3 reciba `CANCELLED_FLEXIBLE` con el código actual, la transición tendría que ser:

| Elemento | Valor |
|---|---|
| Origen | `EXPIRED` |
| Destino | `CANCELLED` |
| Subtipo | `FLEXIBLE` (obligatorio) |
| Actor | `SYSTEM` |
| `actor_id` | `null` |
| Causa | `LATE_PAYMENT` (explícita, no un texto libre) |
| Evento M3 | `reservation.status.changed` con `status = CANCELLED_FLEXIBLE` |
| Evento M1 | Ninguno |
| Auditoría | `EXPIRED → CANCELLED`, subtipo `FLEXIBLE`, actor `SYSTEM`, motivo `LATE_PAYMENT` |

**Por qué `FLEXIBLE` y no `LATE`:** `FinanceStatusMapper` mapea `FLEXIBLE` a `CANCELLED_FLEXIBLE` y `LATE` a `CANCELLED_STRICT` (`FinanceStatusMapper.java:27-31`). El cobro tardío debe reembolsarse al 100 % (`CU-14/plan.md:62`), así que usar `LATE` produciría el tratamiento financiero equivocado.

**Precondiciones:** la reserva debe estar en `EXPIRED`; `m1_blocked` debe ser `false`; no debe existir una cancelación previa; la confirmación de pago debe traer una transacción aprobada y una referencia externa; la operación debe ser idempotente por `(external_transaction_id, result)`.

---

## 2.3 Qué dice la spec sobre inmutabilidad de terminales

La spec es explícita: `Expirada` es terminal y “no se puede tocar nunca más” (`CU-08/spec.md:168`), y la matriz oficial no contempla salidas (`CU-08/spec.md:182-187`). Además, `CU-13/plan.md:25` dice que la reserva **permanece Expirada**. Por tanto, una transición real `EXPIRED → CANCELLED` **contradice la letra de la spec** y no puede implementarse como una fila más de la matriz.

La lectura consistente es:

- el **contrato financiero** necesita `CANCELLED_FLEXIBLE` (`CU-14/plan.md:62`);
- la **spec de estados** dice que `EXPIRED` no sale;
- el plan de CU-13 resuelve la tensión diciendo que M2 no cambia de estado y solo publica el evento financiero.

Antes de implementar hay que decidir si se mantiene la letra de la spec (alternativa B) o se aprueba una excepción formal (alternativa A). No se debe “resolver” agregando `EXPIRED → CANCELLED` a la matriz sin cambiar la spec, porque eso abriría la puerta a que cualquier llamador cancele una expirada.

---

## 2.4 Alternativa A (recomendada, condicionada): transición excepcional y dedicada

### Idea

No tocar la matriz genérica. Añadir una **política de dominio** y un **puerto dedicado** para el cobro tardío, de modo que `EXPIRED → CANCELLED/FLEXIBLE` solo sea posible por esa vía, con actor `SYSTEM`, causa `LATE_PAYMENT` y `m1_blocked = false`.

### Diseño propuesto

1. **Puerto nuevo:** `LatePaymentReversalUseCase` (o `ConfirmLatePaymentUseCase`) con un comando dedicado:
   ```java
   record LatePaymentReversalCommand(
       UUID reservationId,
       String externalTransactionId,
       String paymentReference,
       Long expectedVersion
   ) {}
   ```
2. **Implementación:** `ChangeReservationStatusService` implementa el puerto y usa un método privado `execute(command, TransitionCause.LATE_PAYMENT)`. El `execute` genérico sigue usando `TransitionCause.NORMAL`.
3. **Regla de dominio:** en `ReservationStateMachine`, agregar:
   ```java
   public void validateLatePaymentReversal(
       ReservationStatus from, ReservationStatus to, CancellationSubtype subtype) {
       if (from != EXPIRED || to != CANCELLED || subtype != FLEXIBLE) {
           throw new InvalidTransitionException(from, to,
               "Only EXPIRED -> CANCELLED/FLEXIBLE is allowed for a late payment reversal");
       }
   }
   ```
   `canTransition` y `validate` **no cambian**.
4. **Agregado:** agregar un método explícito `Reservation.cancelExpiredForLatePayment(...)` que llame a `validateLatePaymentReversal` y asigne `CANCELLED`/`FLEXIBLE`. No reutilizar `changeStatus` genérico, para que este siga rechazando la salida de terminales.
5. **Validaciones del servicio (todas obligatorias):**
   - `from == EXPIRED`;
   - `to == CANCELLED`;
   - `subtype == FLEXIBLE`;
   - `actor == SYSTEM`;
   - `actorId == null`;
   - causa `LATE_PAYMENT` proveniente del puerto dedicado;
   - `!reservation.isM1Blocked()`;
   - `expectedVersion` coincide si se envía;
   - no existe ya una transición de cobro tardío para la misma referencia externa (idempotencia de CU-13).
6. **Saltos explícitos en `validateCommand`:**
   - no ejecutar `validateCancellationOrigin` para esta causa;
   - no ejecutar `machine.canTransition` para esta causa;
   - permitir `SYSTEM` + `FLEXIBLE` solo en esta causa (`validateActorForSubtype`);
   - permitir `payment_reference` solo en esta causa (`validateFieldPerTarget`);
   - persistir `paymentReference` y, si se decide, `paymentConfirmedAt` en `applyCommandFields`.
7. **Auditoría y outbox:** el flujo normal de `ChangeReservationStatusService` sigue escribiendo la fila y encolando el evento; la única diferencia es que `previousStatus = EXPIRED`, `saved.getStatus() = CANCELLED` y `eventIds` contiene el `m3EventId`.

### Pros

- Reutiliza `StatusAuditAdapter`, `OutboxAdapter`, `FinanceStatusMapper` y la máquina de notificaciones.
- M2 y M3 quedan consistentes: M3 recibe `CANCELLED_FLEXIBLE` y M2 muestra `CANCELLED`.
- La auditoría es una transición real y trazable, con `event_ids`.
- No agrega estados ni migraciones de esquema.
- No abre la matriz: `canTransition(EXPIRED, CANCELLED)` sigue en `false`; solo la política dedicada lo permite.
- `finalStatesHaveNoExit` sigue verde porque la matriz no cambia.

### Contras

- Contradice `CU-08/spec.md:168` y la matriz de `CU-08/spec.md:182-187`; requiere enmienda documental.
- Cambia el estado visible de la reserva a `CANCELLED`; hay que confirmar con producto/M3.
- Hay que decidir qué hacer con `payment_reference` y `paymentConfirmedAt`.
- El `reason` por sí solo no debe autorizar la transición: si se usa un string, cualquier llamador interno podría replicarlo. Por eso se recomienda un puerto/comando dedicado, no un `reason == "LATE_PAYMENT"` en el `execute` genérico.

### Archivos afectados

| Archivo | Cambio |
|---|---|
| `ReservationStateMachine.java` | `validateLatePaymentReversal`; sin cambios en `canTransition`. |
| `Reservation.java` | Método explícito de cancelación por cobro tardío. |
| `ChangeReservationStatusService.java` | Puerto dedicado, causa `LATE_PAYMENT`, guards y persistencia de referencia. |
| `application/port/in/LatePaymentReversalUseCase.java` | Nuevo puerto. |
| `application/dto/LatePaymentReversalCommand.java` | Nuevo comando. |
| `ChangeReservationStatusCommand` | Sin cambios o campo de causa interno; no exponer un booleano abierto. |
| `ModuleNotificationMatrix` | Sin cambios; probar `EXPIRED → CANCELLED`. |
| `FinanceStatusMapper` | Sin cambios; probar `CANCELLED/FLEXIBLE`. |
| `V6__*.sql` (si se implementa T-16c) | Índice único de idempotencia de pago; no es de esta transición. |
| Tests de estado, servicio, outbox, auditoría y TTL | Nuevos casos. |

### Riesgo de regresión

**Medio.** La matriz no cambia, pero el servicio gana una rama excepcional. Los riesgos concretos:

- `ChangeReservationStatusService.java:159-165` rechaza hoy toda cancelación fuera de `RESERVED`; el salto debe ser explícito y probado para que no se relaje por accidente.
- `validateActorForSubtype` rechaza hoy `SYSTEM` con subtipo; el permiso debe limitarse a `FLEXIBLE` + causa `LATE_PAYMENT`.
- `validateFieldPerTarget` rechaza hoy `payment_reference` fuera de `RESERVED`; si se persiste la referencia, hay que abrir solo esa causa.
- `ChangeReservationStatusServiceTest.expiredTransitionDoesNotEnqueueEvents` (`:362-371`) debe seguir verde para `EXPIRED` normal (sin cobro).
- `ModuleNotificationMatrixTest.expiredAndPaymentFailedNotifyNobody` (`:65-76`) debe seguir verde para destino `EXPIRED`/`PAYMENT_FAILED`; el nuevo caso es destino `CANCELLED`.
- `finalStatesHaveNoExit` debe seguir verde.
- `ExpireReservationsService` no cambia; el job seguirá viendo solo `INITIATED`/`PENDING_PAYMENT`.

---

## 2.5 Alternativa B: evento financiero sin cambio de estado

### Idea

Mantener la reserva en `EXPIRED` y publicar un evento `CANCELLED_FLEXIBLE` solo para M3, sin transición de estado. Sería un “evento de compensación financiera” generado por CU-13/CU-08.

### Cómo se podría implementar

- Nuevo `LatePaymentReversalEvent` en el outbox, con `reservation_id`, `status = CANCELLED_FLEXIBLE`, `status_changed_at` y `event_id`.
- Nuevo método en `OutboxPort` o un publicador dedicado que no pase por `FinanceStatusMapper.toFinanceStatus(EXPIRED, ...)`.
- Una fila de auditoría de compensación (`EXPIRED → EXPIRED` con motivo `LATE_PAYMENT` y `event_ids`) o una tabla nueva de eventos financieros.
- CU-13 registra el intento en `payment_attempt` y publica el evento en la misma transacción.

### Pros

- Respeta literalmente `CU-08/spec.md:168` y `CU-13/plan.md:25`.
- No cambia el estado visible de la reserva ni los contratos de CU-20/CU-21.
- No toca la máquina de estados ni la matriz M1/M3.

### Contras

- `FinanceStatusMapper` no puede mapear `EXPIRED`; hay que crear un camino alterno y evitar que alguien use `EXPIRED` en el mapper genérico.
- La auditoría normal de transiciones no representa el evento; haría falta una fila especial o una tabla nueva, con migración.
- M2 y M3 divergen: M2 dice `EXPIRED`, M3 recibe `CANCELLED_FLEXIBLE`. Es intencional, pero complica soporte y consultas.
- El `event_id` no queda vinculado a una transición de estado; la trazabilidad `event_ids` de `reservation_status_audit` pierde sentido para este caso.
- Duplica lógica de outbox/idempotencia y aumenta el riesgo de doble publicación.

### Veredicto

Es el **fallback** si M3/producto confirman que M2 debe seguir mostrando `Expirada`. No es la opción de menor complejidad si se acepta cambiar el estado.

---

## 2.6 Impacto en `m1_blocked`, matriz M1/M3, `FinanceStatusMapper`, auditoría y TTL

### `m1_blocked`

- La reserva expirada nunca alcanzó `RESERVED`; por tanto `m1_blocked` debe ser `false` (`Reservation.java:214-220`; `ReservationMapper.java:67`).
- La transición `EXPIRED → CANCELLED` no debe activar `m1_blocked`; el servicio solo lo pone en `true` al entrar en `RESERVED` (`ChangeReservationStatusService.java:78-80`).
- Añadir un guard explícito: si `m1_blocked == true` en una reserva `EXPIRED`, es una violación de invariante; rechazar la transición y alertar. No se debe enviar una liberación a M1.
- `ReservationMapper.applyChanges` sigue persistiendo el valor (`ReservationMapper.java:95`); no hay cambio.

### Matriz M1/M3

- `ModuleNotificationMatrix.notifiesModuleThree` devuelve `true` para `CANCELLED` (`ModuleNotificationMatrix.java:39-44`), así que el evento M3 sale sin cambios.
- `moduleOneStatus` para `CANCELLED` devuelve `null` si `m1_blocked == false` (`ModuleNotificationMatrix.java:51-58`), que es el caso de una expirada.
- Resultado esperado: `eventsEnqueued = ["AMQP"]`; ningún `M1_HTTP`.
- Se recomienda añadir un test unitario `latePaymentCancellationNotifiesModuleThreeOnly` con `decide(EXPIRED, CANCELLED, false, false)`.
- Opcional de endurecimiento: hacer que `moduleOneStatus` reciba también `from` y devuelva `null` cuando `from == EXPIRED`, aunque `m1_blocked` sea `true`. No es imprescindible si el servicio valida la invariante.

### `FinanceStatusMapper`

- No requiere cambios: `CANCELLED` + `FLEXIBLE` → `CANCELLED_FLEXIBLE` (`FinanceStatusMapper.java:17,27-32`).
- El guard debe impedir `LATE` en esta ruta; si se usara `LATE`, M3 recibiría `CANCELLED_STRICT` y cobraría una penalización indebida.
- `EXPIRED` sigue siendo un estado no publicable (`FinanceStatusMapper.java:18-19`); por eso no se puede publicar el estado actual sin cambiar a `CANCELLED` (alternativa A) o sin un mapper alterno (alternativa B).

### Auditoría

- La fila sería `from_status = EXPIRED`, `to_status = CANCELLED`, `cancellation_subtype = FLEXIBLE`, `actor = SYSTEM`, `actor_id = null`, `reason = LATE_PAYMENT`, `event_ids = [m3EventId]`.
- El trigger de inmutabilidad solo bloquea `UPDATE`/`DELETE` (`V1__init.sql:80-89`); el `INSERT` es válido.
- La fila debe escribirse en la misma transacción que el cambio de estado y el outbox (`StatusAuditAdapter.java:20-33`).
- Si se persiste la referencia de pago, definir si va en `payment_reference` y/o `payment_confirmed_at`; ambas columnas existen (`Reservation.java:36-37`).

### Job de TTL

- `ReservationRepositoryAdapter.findExpiredBefore` solo devuelve `INITIATED`/`PENDING_PAYMENT` (`ReservationRepositoryAdapter.java:130-134`); una reserva `CANCELLED` no vuelve a procesarse.
- `ExpireReservationsService` ya captura `VersionConflictException` e `InvalidTransitionException` (`ExpireReservationsService.java:59-65`), así que una carrera con el cobro tardío no rompe el job.
- No se necesita un job nuevo ni cambiar el corte exacto de 15 minutos.
- Si el job y el cobro tardío compiten, `@Version` decide: uno gana y el otro recibe conflicto/transición inválida. El cobro tardío debe reintentar la lectura y, si ya está `EXPIRED`, aplicar la transición; si ya está `CANCELLED`, responder idempotentemente.
- La consulta final de CU-13 antes del corte sigue siendo responsabilidad del adaptador de CU-13, no del job (`CU-13/plan.md:226`).

### Idempotencia de pago

- `CU-13/spec.md:107` exige clave única `(external_transaction_id, result)` por reserva.
- Hoy `payment_attempt` no tiene restricción única (`V2__remaining_tables.sql:14-23`; `V3__reservation_fields_and_outbox.sql:19-21`). T-16c debe añadir el índice/constraint en una migración nueva (V6) para que la idempotencia no dependa solo del código.
- Si llega una confirmación duplicada después de la transición, debe responder 200 sin nueva transición ni nuevo evento.

### Visibilidad y consultas

- Si se elige A, la reserva pasa a `CANCELLED`/`FLEXIBLE`; CU-20/CU-21 y el frontend deben mostrar “Cancelada” (política flexible) después del reembolso.
- Si se elige B, sigue mostrando “Expirada”. Es una decisión de producto, no solo técnica.

---

## 2.7 Recomendación D2

**Alternativa A, condicionada a la confirmación de M3/producto.** Es la opción que reutiliza el mapper, el outbox y la auditoría existentes, y deja M2 y M3 consistentes con el reembolso del 100 %. Debe implementarse como **política dedicada**:

- no agregar `EXPIRED → CANCELLED` a `ReservationStateMachine.canTransition`;
- no permitir la salida desde el `execute` genérico;
- exigir `SYSTEM` + `FLEXIBLE` + causa `LATE_PAYMENT` + `m1_blocked == false`;
- escribir auditoría y outbox en la misma transacción.

Si M3/producto confirman que la reserva debe permanecer `EXPIRED`, cambiar a la alternativa B antes de implementar; no mezclar ambas.

---

## 2.8 Pruebas necesarias D2

### Dominio

1. `latePaymentReversalOnlyAllowsExpiredToCancelledFlexible`.
2. `latePaymentReversalRejectsOtherOrigins` (`PENDING_PAYMENT`, `RESERVED`, `COMPLETED`, `PAYMENT_FAILED`).
3. `latePaymentReversalRejectsOtherSubtypes` (`LATE`, `MODERATE`, `BY_OWNER`, `NO_SHOW`).
4. `genericCanTransitionStillRejectsExpiredExit`: la matriz sigue sin salida.
5. `finalStatesHaveNoExit` sigue verde (`ReservationStateMachineTest.java:32-41`).

### Servicio

6. `latePaymentCancellationRequiresSystemActor`: `RENTER`/`OWNER` rechazados.
7. `latePaymentCancellationRequiresExplicitCause`: un `execute` genérico con `reason = LATE_PAYMENT` no debe abrir la ruta.
8. `latePaymentCancellationRejectsWhenM1Blocked`: invariante `m1_blocked == false`.
9. `latePaymentCancellationWritesAuditWithFromExpiredAndEventIds`.
10. `latePaymentCancellationPersistsPaymentReference` (si se decide).
11. `latePaymentCancellationIsIdempotent`: duplicado por `(external_transaction_id, result)` responde 200 sin segunda transición/evento.
12. `latePaymentCancellationRejectsExpectedVersionMismatch`.

### Integración / outbox / mapper / TTL

13. `latePaymentCancellationPublishesCancelledFlexible`: payload `status = CANCELLED_FLEXIBLE`.
14. `latePaymentCancellationDoesNotNotifyModuleOne`: ningún `OperationalStatusEvent`; `m1_blocked` sigue `false`.
15. `latePaymentCancellationEnqueuesOnlyAmqp`: `eventsEnqueued = ["AMQP"]`.
16. `ttlJobDoesNotReprocessCancelledReservation`.
17. `ttlJobRaceWithLatePaymentYieldsVersionConflictOrInvalidTransition`.
18. `expiredWithoutLatePaymentStillPublishesNothing`: `expiredTransitionDoesNotEnqueueEvents` (`ChangeReservationStatusServiceTest.java:362-371`).
19. `financeStatusMapperStillMapsFlexibleToCancelledFlexible` (`FinanceStatusMapperTest.java:32-36`).
20. `moduleNotificationMatrixLatePaymentNotifiesOnlyM3` (nuevo caso en `ModuleNotificationMatrixTest`).

---

## 2.9 `[NEEDS CLARIFICATION]` que hay que confirmar con M3 antes de implementar

| ID | Pregunta | Por qué bloquea |
|---|---|---|
| C1 | ¿M3 exige que M2 cambie realmente a `CANCELLED`, o acepta que M2 siga en `EXPIRED` y solo reciba el evento `CANCELLED_FLEXIBLE`? | Decide entre alternativa A y B. `CU-13/plan.md:25` dice “permanece Expirada”; `CU-14/plan.md:62` habla de `CANCELLED_FLEXIBLE`. |
| C2 | ¿El subtipo correcto es `FLEXIBLE` (reembolso 100 %) y no `LATE` (`CANCELLED_STRICT`)? | Evita un tratamiento financiero incorrecto. |
| C3 | ¿La regla de cobro tardío aplica también a `PAYMENT_FAILED`, como sugiere `CU-13/plan.external.md:123`? | Si aplica, habría que abrir otra excepción `PAYMENT_FAILED → CANCELLED`; hoy no está en el alcance de la decisión. |
| C4 | ¿M2 debe persistir la referencia de la transacción tardía y la hora de confirmación? ¿En `payment_reference`/`payment_confirmed_at` o en un campo nuevo? | Define si hay migración y qué se audita. |
| C5 | ¿M3 espera `CANCELLED_FLEXIBLE` incluso si la reserva expiró desde `INITIATED` y nunca recibió `PENDING`? | Hay casos en los que M3 no conoce la reserva; el evento podría ser un huérfano. |
| C6 | ¿M1 debe recibir algo? | La regla de `CU-22/spec.md:114-115` dice que no, porque nunca llegó a `RESERVED`; hay que confirmarlo con la invariante `m1_blocked = false`. |
| C7 | ¿Qué clave de idempotencia exacta usa M3: `(external_transaction_id, result)` por reserva? ¿Qué pasa si llega la misma transacción con distinto `result`? | `CU-13/spec.md:107` lo pide, pero `payment_attempt` aún no tiene unique constraint. |
| C8 | ¿La auditoría debe mostrar `EXPIRED → CANCELLED` como transición real, o M3/producto prefieren una fila de compensación sin cambio de estado? | Afecta la semántica de `reservation_status_audit` y la trazabilidad. |
| C9 | ¿Qué debe ver el usuario en CU-20/CU-21: “Expirada” o “Cancelada (flexible)”? | Cambia la UX y los filtros si se elige A. |
| C10 | ¿Se acepta que el cobro tardío sea una excepción documentada a `CU-08/spec.md:168`, o hay que modificar la spec antes de codificar? | Sin enmienda, la implementación contradice la fuente de verdad. |

---

## 3. Orden de implementación sugerido para H7

1. Cerrar D1 con la alternativa A: puerto `initiate`, delegación de `CreateReservationService`, pruebas de atomicidad.
2. Cerrar C1–C10 con M3/producto; si se elige A, enmendar `CU-08/spec.md` y `CU-13/plan.md` antes de codificar.
3. Implementar D2 con la política dedicada, sin tocar `canTransition`.
4. Añadir el unique constraint de idempotencia de pago en la migración de T-16c.
5. Ejecutar la suite completa y la suite en orden inverso con Docker; registrar conteo y resultado.

---

## 4. Resumen de 10 líneas

1. D1: la creación debe entrar a CU-08 por un `initiate` dedicado; CU-02 conserva validaciones, idempotencia y TTL.
2. CU-08 valida `null → INITIATED`, persiste, escribe la auditoría y encola `reservation.info.provided` en una sola transacción.
3. El mismo `event_id` determinístico se usa en `reservation_status_audit.event_ids` y en `outbox_message.event_id`; si algo falla, todo revierte.
4. Se rechaza la creación “shell” + auto-transición porque deja la primera escritura fuera de CU-08 y rompe la auditoría de creación.
5. D2: el contrato financiero necesita `CANCELLED_FLEXIBLE`; la transición exacta sería `EXPIRED → CANCELLED` con subtipo `FLEXIBLE`, actor `SYSTEM` y causa `LATE_PAYMENT`.
6. La spec declara `EXPIRED` terminal e inmutable (`CU-08/spec.md:168`); el plan de CU-13 dice que M2 permanece `Expirada`; hay contradicción documental que M3/producto debe resolver.
7. Se recomienda una política dedicada que no agregue la salida a la matriz ni a `canTransition`, y que exija `m1_blocked == false`.
8. Impacto: M3 recibe `CANCELLED_FLEXIBLE`; M1 no recibe nada; `m1_blocked` sigue `false`; la auditoría registra `EXPIRED → CANCELLED`; el job de TTL no cambia.
9. `FinanceStatusMapper` no cambia (`FLEXIBLE → CANCELLED_FLEXIBLE`); usar `LATE` sería un error porque publicaría `CANCELLED_STRICT`.
10. Antes de implementar: confirmar C1–C10 con M3, añadir el unique constraint de idempotencia de pago y enmendar la spec si se elige la transición real.
