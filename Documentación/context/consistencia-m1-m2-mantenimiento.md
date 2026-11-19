# Consistencia M1–M2: inconsistencias y factibilidad

**Fecha:** 2026-11-18 · **Actualizado:** 2026-11-19
**Alcance:** M2 (este repo) y M1 (`C:\Users\josue\Documents\IJ\SeaShareM1`), en modo solo lectura. El análisis de Módulo 3 está en `consistencia-m2-m3-pendientes.md`.
**Naturaleza:** dictamen de factibilidad y diseño. No se implementó nada.
**Contrato normativo:** `consistencia-m2-m1.md`.
**Estado de implementación (2026-11-19):** las correcciones de M2 (C4 sin `RESERVED`, CU-10 por rango, C5, idempotencia, `auto-cancel` y job de respaldo) están implementadas en la rama `feature/consistencia-m1-m2`. Las piezas de M1 (§2.2 y catálogo C2) siguen pendientes en `SeaShareM1`.
**Despliegue objetivo:** sábado 2026-11-21.

---

## 0. Método y evidencia

Cada afirmación se clasifica así:

- **VERIFICADO**: leído en el código del módulo indicado, con `archivo:línea`.
- **DECLARADO**: decisión del equipo, sin código que la respalde todavía.
- **NO VERIFICABLE**: requiere ejecución, entorno o repo ausente.

Hechos base usados en todo el documento:

| # | Hecho | Estado | Evidencia |
|---|---|---|---|
| 1 | M1 aplica el aviso sobre un único `status`; no hay fechas | VERIFICADO | M1 `Vessel.java:35`, `V10__operational_status_events.sql:5-6` |
| 2 | M1 solo permite `AVAILABLE→RESERVED`, `RESERVED→{NAVIGATION,MAINTENANCE,AVAILABLE}`, `NAVIGATION→MAINTENANCE` | VERIFICADO | M1 `AssignOperationalStatusService.java:33-37` |
| 3 | M1 devuelve `MAINTENANCE`/`NAVIGATION`, no `MAINTENANCE_CLEANING`/`IN_NAVIGATION` | VERIFICADO | M1 `VesselStatus.java:4-9`, `ReservationsVesselController.java:65` |
| 4 | M1 exige `occurred_at` con offset (`OffsetDateTime.parse`) | VERIFICADO | M1 `AssignOperationalStatusService.java:108` |
| 5 | M1 acepta alias `NAVIGATION`/`MAINTENANCE` y `event_id` ≤ 100 | VERIFICADO | M1 `AssignOperationalStatusService.java:26,91,129-130` |
| 6 | M1 no tiene cliente saliente hacia M2; el único `RestClient` es Cloudinary | VERIFICADO | M1 `StorageConfig.java:17` |
| 7 | M2 envía `RESERVED` al confirmar el pago | VERIFICADO | M2 `ModuleNotificationMatrix.java:48`, `ChangeReservationStatusService.java:164-170` |
| 8 | M2 marca `m1_blocked` solo al encolar `RESERVED` | VERIFICADO | M2 `ChangeReservationStatusService.java:118-120` |
| 9 | M2 exige `actorId` si el actor no es `SYSTEM` y prohíbe `SYSTEM` con subtipo | VERIFICADO | M2 `ChangeReservationStatusService.java:230-235,253-257` |
| 10 | M2 resuelve solapamiento con `daterange` inclusivo y `EXCLUDE` | VERIFICADO | M2 `V1__init.sql:53-56`, `ReservationJpaRepository.java:27` |
| 11 | CU-10 solo compara con `AVAILABLE` y no envía fechas | VERIFICADO | M2 `HttpFleetOperationalStatusAdapter.java:48,64` |
| 12 | M2 declara las colas como clásicas con `seashare.reservations.dlx` | VERIFICADO | M2 `RabbitTopologyConfig.java:39-46` |
| 13 | Las decisiones 1 a 7 son del equipo, no del código | DECLARADO | Encargo de este documento |
| 14 | Comportamiento real de M1 en un entorno desplegado | NO VERIFICABLE | No hay entorno ni ejecución en esta revisión |

---

## 1. Resumen ejecutivo

Las decisiones 1 a 7 se implementan con el **2026-11-21** como fecha límite única: no hay trabajo posterior ni fases siguientes. Las decisiones 8 y 9 (M3) viven en `consistencia-m2-m3-pendientes.md`.

| # | Decisión | Veredicto | Bloqueante |
|---|---|---|---|
| 1 | M2 deja de enviar `RESERVED` | **FACTIBLE CON CAMBIOS** | M1 debe permitir `AVAILABLE→NAVIGATION`; M2 debe redefinir `m1_blocked` o pierde `MAINTENANCE_CLEANING` |
| 2 | M2 autoridad de fechas | **FACTIBLE** | Ya lo es (D2 no requiere cambios) |
| 3 | `maintenance_until` en M1 | **FACTIBLE CON CAMBIOS** | Migración, entidad, DTO y liberación en M1 |
| 4 | Push M1 → M2 | **FACTIBLE CON CAMBIOS** | M1 no tiene cliente saliente ni outbox: hay que crearlos |
| 5 | CU-10 con rango | **FACTIBLE CON CAMBIOS** | Depende de D3 (fechas en M1) |
| 6 | Reutilizar CU-08 | **FACTIBLE CON CAMBIOS** | Excepción acotada a `SYSTEM`+`BY_OWNER`; auditoría y evento |
| 7 | `auto-cancel` | **FACTIBLE** | Sin D4 no tiene disparador |

**Alcance del sábado:** D1 a D7 y el catálogo C2. **Riesgo principal:** D3, D4 y D5 requieren cambios en M1; no hay plan B ni fecha posterior. **Además:** el catálogo de CU-01 no existe en M1 (§2.3), así que con `mode=http` la búsqueda no funciona hasta que C2 esté implementado.

---

## 2. Tabla de cambios

Esfuerzo: **S** ≤ 4 h · **M** 1–2 días · **L** > 2 días. Prioridad sábado: **SÍ** / **NO**.

### 2.1 Cambios en M2 (los únicos que haremos nosotros)

| Pieza | Archivo | Cambio | Esfuerzo | Prioridad sábado |
|---|---|---|---|---|
| Matriz de notificaciones | `application/service/ModuleNotificationMatrix.java:46-60` | Eliminar `RESERVED`; condicionar `CANCELLED` a que no haya mantenimiento activo | S | **SÍ** |
| Guarda de ocupación | `application/service/ChangeReservationStatusService.java:118-120` | Activar la guarda al encolar `IN_NAVIGATION` (y renombrar a `m1_occupied`); sin esto se pierde `MAINTENANCE_CLEANING` | S | **SÍ** |
| Puerto CU-10 | `application/port/out/FleetOperationalStatusPort.java:7` | `isAvailable(UUID, LocalDate start, LocalDate end)` | S | **SÍ** |
| Adaptador CU-10 | `infrastructure/.../fleet/HttpFleetOperationalStatusAdapter.java:46-71` | Interpretar estados y fechas de mantenimiento; fallo seguro igual que hoy | M | **SÍ** |
| Llamadas CU-10 | `CreateReservationService.java:87`, `StartPaymentService.java:101` | Pasar el rango; mensaje sin "fechas seleccionadas" cuando el bloqueo es de mantenimiento | S | **SÍ** |
| Controlador interno | nuevo `infrastructure/adapter/in/web/MaintenanceEventController.java` | `POST /api/v1/internal/maintenance-events` | S | **SÍ** |
| DTO interno | nuevos `MaintenanceEventRequest/Response` | Contrato D.1; `{code,message}` en errores | S | **SÍ** |
| Servicio de aplicación | nuevo `application/service/ApplyMaintenanceEventService.java` | Idempotencia, consulta de afectadas, cancelación por reserva | M | **SÍ** |
| Consulta de afectadas | `ReservationRepositoryPort` + `ReservationJpaRepository` | `findReservedByVesselOverlapping(vesselId, start, end)` | S | **SÍ** |
| Excepción de actor | `ChangeReservationStatusService.java:223-240` | Permitir `SYSTEM`+`BY_OWNER` solo con causa `MAINTENANCE` (patrón `diseno-h7-previo.md`) | M | **SÍ** |
| Causa y auditoría | `ChangeReservationStatusCommand`, auditoría | Registrar causa; `reservation_status_audit` solo tiene `actor_id`/`event_ids` (`V4:16-17`) → columna o reutilizar `cancellation_event` | M | **SÍ** |
| Idempotencia | `V2__remaining_tables.sql:137-143` (reutilizar) | `key = event_id`, `endpoint = /api/v1/internal/maintenance-events`, `response_ref = outcome` | S | **SÍ** |
| Config | `application.yaml` + `application-prod.yaml` | `m2.maintenance.auto-cancel` (default `true`), `m2.maintenance.scan.*` | S | **SÍ** (solo el flag) |
| Job de reconciliación | nuevo `.../scheduler/MaintenanceReconciliationJob.java` | Se incluye apagado por defecto; reutiliza el servicio | M | **SÍ** |
| Códigos de error | `shared/domain/ErrorCode.java` | `MAINTENANCE_EVENT_PARTIAL_FAILURE` (503), `INVALID_MAINTENANCE_RANGE` (400) | S | **SÍ** |

### 2.2 Peticiones a M1 (no las implementamos nosotros)

| Pieza | Archivo | Cambio | Esfuerzo | Prioridad sábado |
|---|---|---|---|---|
| Transiciones | `application/usecase/AssignOperationalStatusService.java:33-37` | Permitir `AVAILABLE→NAVIGATION`; `AVAILABLE→MAINTENANCE` por aviso no se permite | S | **SÍ** |
| Esquema | nueva `V11__maintenance_window.sql` | `maintenance_start DATETIME(6) NULL`, `maintenance_until DATETIME(6) NULL` | S | **SÍ** |
| Dominio | `domain/model/Vessel.java` | Campos nuevos y reglas de mantenimiento | M | **SÍ** |
| Persistencia | `VesselEntity`, `VesselPersistenceMapper`, `VesselJpaRepository`, `VesselPersistenceAdapter` | Leer/escribir ventana; SQL de inicio/fin de mantenimiento | M | **SÍ** |
| Servicios de mantenimiento | `OwnerMaintenanceService`, `AdminMaintenanceService` | Exigir `maintenance_until`; permitir el mantenimiento con reservas futuras (M2 cancela) | M | **SÍ** |
| DTO de estado | `ReservationsVesselQuery.OperationalStatus`, `OperationalStatusResponse` | Añadir `maintenance_start`/`maintenance_until` | S | **SÍ** |
| Liberación | `ReservationsVesselQueryService` o job | Evaluar vencimiento al consultar y/o job | M | **SÍ** |
| Cliente saliente | nuevo `infrastructure/adapter/out/reservations/...` | `RestClient` a M2 con timeouts | M | **SÍ** |
| Aviso pendiente | nueva tabla `maintenance_event_outbox` + relay | Persistir y reintentar; sin esto se pierde el aviso si M2 cae | L | **SÍ** |
| Config | `application.yml` | `seashare.modules.m2.base-url`, timeouts, reintentos | S | **SÍ** |

### 2.3 Inconsistencias adicionales M1 ↔ M2

**Catálogo de embarcaciones (CU-01/CU-09).** Módulo 2 pide `GET /api/v1/vessels?page&size&q` (`HttpFleetCatalogAdapter.java:42-44`) y Módulo 1 no expone la colección: sólo `/{vessel_id}` y `/{vessel_id}/operational-status` (`ReservationsVesselController.java:35,60`). Cualquier respuesta no `2xx` se convierte en `503 FLEET_SERVICE_UNAVAILABLE` (`HttpFleetCatalogAdapter.java:52-54`), así que con `mode=http` la búsqueda de CU-01 no funciona. La ficha individual (C1) y el estado (C3) sí existen. Contrato objetivo en `consistencia-m2-m1.md` §3.

| Pieza | Módulo | Archivo | Cambio | Esfuerzo | Prioridad sábado |
|---|---|---|---|---|---|
| Catálogo | M1 | nuevo controlador de listado | `GET /api/v1/vessels` con `{vessels, pagination}`, `page`, `size`, `q`, público | M | **SÍ** |

---

## 3. Contratos

> **Fuente normativa:** `consistencia-m2-m1.md`. Aquí se resumen solo para explicar el diseño; cualquier divergencia se resuelve a favor del contrato.

### 3.1 `POST {M2_URL}/api/v1/internal/maintenance-events` (M1 → M2)

**Cuándo lo envía M1:** al pasar una embarcación a `MAINTENANCE` (propietario o admin), una sola vez por evento.

**Request**

```json
{
  "event_id": "b0f5f0a2-6a0e-4f0e-9a59-1f1c2f3a4b5c",
  "vessel_id": "37a31793-29e9-4765-9d3d-39b74a744476",
  "maintenance_start": "2026-11-20T08:00:00-05:00",
  "maintenance_until": "2026-11-27T18:00:00-05:00",
  "occurred_at": "2026-11-18T20:15:00Z",
  "reason": "OWNER"
}
```

| Campo | Tipo | Obligatorio | Regla |
|---|---|---|---|
| `event_id` | string | Sí | UUID o texto ≤ 100; clave de idempotencia |
| `vessel_id` | string (UUID) | Sí | Embarcación |
| `maintenance_start` | string (ISO-8601 con offset) | No | `null` = empieza ahora |
| `maintenance_until` | string (ISO-8601 con offset) | Sí | `null` o `until <= start` se rechaza con 400 (`INVALID_MAINTENANCE_RANGE`) |
| `occurred_at` | string (ISO-8601 con offset) | Sí | Momento del cambio |
| `reason` | string | No | `OWNER` o `ADMIN`; informativo |

**Response `200 OK`**

```json
{
  "event_id": "b0f5f0a2-6a0e-4f0e-9a59-1f1c2f3a4b5c",
  "outcome": "APPLIED"
}
```

| `outcome` | Cuándo | Efecto en M1 |
|---|---|---|
| `APPLIED` | Se canceló al menos una reserva o no había ninguna | No reintentar |
| `DUPLICATE` | `event_id` ya procesado | No reintentar |
| `NO_AFFECTED_RESERVATIONS` | No hay reservas `RESERVED` que crucen el rango | No reintentar |
| `UNKNOWN_VESSEL` | `vessel_id` no existe en M2 | No reintentar |
| `INVALID_RANGE` | Rango válido pero íntegramente en el pasado | No reintentar (corregir) |
| `AUTO_CANCEL_DISABLED` | `m2.maintenance.auto-cancel=false` | No reintentar |

**Errores**

```json
{ "code": "MAINTENANCE_EVENT_PARTIAL_FAILURE", "message": "1 de 3 reservas no se pudo cancelar" }
```

| HTTP | `code` | Cuándo | Reintento de M1 |
|---|---|---|---|
| 400 | `INVALID_PARAMETERS` | Falta campo, UUID inválido, fecha mal formada | No |
| 400 | `INVALID_MAINTENANCE_RANGE` | Rango incoherente | No |
| 503 | `MAINTENANCE_EVENT_PARTIAL_FAILURE` | Alguna cancelación falló; las demás se conservan | **Sí**, con el mismo `event_id` |
| 500 | `INTERNAL_ERROR` | Fallo inesperado | Sí (una vez) |

**Idempotencia:** clave `event_id` en `idempotency_key` (`endpoint = /api/v1/internal/maintenance-events`) con `response_ref = outcome`. Un reenvío devuelve `DUPLICATE` sin volver a cancelar. Si hubo `503`, **no** se guarda registro: el reintento reprocesa y salta las reservas ya canceladas por estado.

**Reintentos esperados de M1:** 5 intentos con backoff (1 s, 5 s, 25 s, 125 s) solo ante timeout, `503` o `5xx`. Nunca ante `4xx`.

### 3.2 `GET {M1_URL}/api/v1/vessels/{id}/operational-status`

**Response `200 OK`**

```json
{
  "vessel_id": "37a31793-29e9-4765-9d3d-39b74a744476",
  "operational_status": "MAINTENANCE",
  "queried_at": "2026-11-18T20:15:00-05:00",
  "maintenance_start": "2026-11-20T08:00:00-05:00",
  "maintenance_until": "2026-11-27T18:00:00-05:00"
}
```

Sin mantenimiento:

```json
{
  "vessel_id": "37a31793-29e9-4765-9d3d-39b74a744476",
  "operational_status": "AVAILABLE",
  "queried_at": "2026-11-18T20:15:00-05:00",
  "maintenance_start": null,
  "maintenance_until": null
}
```

- `queried_at` sigue en la zona del puerto (`ReservationsVesselController.java:25-27`).
- Los dos campos nuevos van **siempre**, con `null` cuando no aplica.
- `MAINTENANCE` sin fechas (datos históricos) bloquea todo: es el comportamiento seguro.

### 3.3 `POST {M1_URL}/api/v1/internal/status-events` (sin `RESERVED`)

El payload no cambia: `event_id`, `vessel_id`, `operational_status`, `occurred_at` (M2 `OutboxAdapter.java:122-125`).

| Transición en M2 | Valor enviado | Cuándo | Condición |
|---|---|---|---|
| `RESERVED` | **ninguno** | — | Ya no se envía |
| `IN_NAVIGATION` | `IN_NAVIGATION` | Check-in (CU-06) | Siempre |
| `COMPLETED` | `MAINTENANCE_CLEANING` | Fin de navegación (CU-07) | Solo si M1 conocía la ocupación |
| `CANCELLED` ordinaria o no-show | `AVAILABLE` | Cancelación (CU-04/CU-05) | Solo si M1 conocía la ocupación **y** no hay mantenimiento activo |
| `CANCELLED` por avería | `MAINTENANCE_CLEANING` | Cancelación (CU-04) | Igual condición |
| `INITIATED`, `PENDING_PAYMENT`, `EXPIRED`, `PAYMENT_FAILED` | ninguno | — | Sin cambios |

**Cambio de semántica obligatorio:** la guarda `m1_blocked` hoy solo se activa con `RESERVED` (M2 `ChangeReservationStatusService.java:118-120`). Al dejar de enviarlo, debe activarse con `IN_NAVIGATION`; de lo contrario `COMPLETED` no enviaría `MAINTENANCE_CLEANING` (M2 `ModuleNotificationMatrix.java:50`) y M1 no sabría que el barco requiere limpieza.

### 3.4 Reglas de fechas y zonas

- **Mantenimiento**: instantes ISO-8601 con offset. M2 los convierte a fecha local del puerto (`harbor_timezone` de la reserva, `V1__init.sql:13` / `V3:8`).
- **Reserva**: días completos (`DATE`), rango inclusivo `[]`; el último día cuenta como ocupado (`V1__init.sql:20,55`).
- **Cruce**: hay afectación si `fecha(maintenance_start) <= end_date` y `fecha(maintenance_until) >= start_date`.
- **Ambigüedad declarada**: un mantenimiento es horario y una reserva es diaria. Regla propuesta: **cualquier día tocado por el mantenimiento se considera bloqueado**. Debe confirmarse con M1 (pregunta 4 de §5.3).

---

## 4. Diseño detallado

### 4.1 M1

**A.1 Transiciones sin `RESERVED`.**
`ALLOWED` (`AssignOperationalStatusService.java:33-37`) no contempla `AVAILABLE→NAVIGATION`. Con la decisión 1, el check-in devolvería `INVALID_TRANSITION` (M1 `:79`), M2 lo trataría como permanente y el evento quedaría `dead` (M2 `HttpFleetStatusNotificationAdapter.java:82`). **Cambio mínimo:** añadir `NAVIGATION` al conjunto de `AVAILABLE`. Evaluar también `AVAILABLE→MAINTENANCE`, si el propietario podrá mandar a mantenimiento sin aviso previo.

**A.2 Modelo de mantenimiento.**
Hoy solo existe `maintenance_by` (`V3__create_vessel.sql:19`) y `status_changed_at` (`V10:5-6`). Añadir en una migración nueva:

```sql
ALTER TABLE vessel
    ADD COLUMN maintenance_start DATETIME(6) NULL,
    ADD COLUMN maintenance_until DATETIME(6) NULL;
```

- `maintenance_until` obligatorio al iniciar mantenimiento; `maintenance_start` opcional (nulo = ahora).
- Validación: `until > start`.
- **Permitir mantenimiento con reservas futuras.** Hoy solo se permite desde `AVAILABLE` (`Vessel.java:167-169`; `VesselJpaRepository.java:52-57`). Con la decisión 1, M1 quedará en `AVAILABLE` o `NAVIGATION`. Se decide: el mantenimiento solo se inicia desde `AVAILABLE` (nunca desde `NAVIGATION`) y **sí se permite con reservas futuras**; lo que reemplaza la protección que daba `RESERVED` es la cancelación automática de M2 (D7) más el aviso push (D4). M1 no conoce reservas.

**A.3 Cliente saliente y aviso pendiente.**
No hay infraestructura: el único `RestClient` es de Cloudinary (`StorageConfig.java:17`). Diseño recomendado, patrón outbox:

1. En la **misma transacción** del cambio a mantenimiento, insertar en `maintenance_event_outbox` (`event_id`, `vessel_id`, `start`, `until`, `occurred_at`, `attempts`, `next_attempt_at`, `published_at`, `last_error`).
2. Un relay (`@Scheduled` + ShedLock, o `@Transactional` + reintento) hace `POST` a M2.
3. Reintentos 1/5/25/125 s, máximo 5; `4xx` no se reintenta.
4. Configuración: `seashare.modules.m2.base-url`, `connect-timeout`, `read-timeout`, `max-attempts`.

Enganchar **después del commit** evita bloquear la transacción si M2 no responde. Si se hace dentro, un timeout de M2 retrasa y puede abortar el mantenimiento.

**A.4 DTO de estado.** Añadir los dos campos al record `OperationalStatus` (`ReservationsVesselQuery.java:47`) y al DTO (`OperationalStatusResponse.java:6-9`), con el formato de §3.2.

**A.5 Liberación.**
Hoy el fin de mantenimiento es manual (`OwnerMaintenanceService.java:58-62`, `VesselJpaRepository.java:69-103`). Con `maintenance_until`:

Se decide **evaluación perezosa al consultar**: si `maintenance_until < ahora`, el endpoint devuelve `AVAILABLE` con las fechas en `null`, y el estado persistido se corrige de forma perezosa. No hay job de liberación.
- Si nadie lo libera y no se evalúa, M2 seguiría viendo `MAINTENANCE` con `maintenance_until` vencido. Regla de M2: **si `maintenance_until` es anterior a hoy, se considera disponible** (defensa en profundidad).

**A.6 Seguridad.**
M1 abre `POST /api/v1/internal/status-events` y `GET /api/v1/vessels/**` (`SecurityConfig.java:92-96`). M2 no tiene Spring Security ni autenticación: el endpoint nuevo será público. Es coherente con lo decidido y con lo existente. No hay que abrir nada más en M1.

### 4.2 M2

**B.1 Controlador y servicio.**
`POST /api/v1/internal/maintenance-events` con el contrato §3.1. Flujo:

1. Validar campos y rango → `400`.
2. Idempotencia por `event_id` → si existe, `DUPLICATE`.
3. `UNKNOWN_VESSEL` si la embarcación no tiene reservas ni historial (M2 no conoce embarcaciones; se decide por ausencia de reservas **y** por no existir en datos locales; si se prefiere, M2 responde `NO_AFFECTED_RESERVATIONS` y M1 no distingue).
4. Con `auto-cancel=true`, buscar reservas `RESERVED` del barco que crucen el rango.
5. Cancelar **una transacción por reserva** vía CU-08 con causa `MAINTENANCE`.
6. Guardar idempotencia con el `outcome` final y responder `200`.
7. Si alguna cancelación falla: continuar con las demás, no guardar idempotencia y responder `503 MAINTENANCE_EVENT_PARTIAL_FAILURE`.

**B.2 Idempotencia.**
Reutilizar `idempotency_key` (`V2:137-143`): `key = event_id`, `endpoint = /api/v1/internal/maintenance-events`, `response_ref = outcome`. Es suficiente: no hace falta tabla nueva. El fallo parcial no registra, y el reintento salta lo ya cancelado porque el estado ya no es `RESERVED`.

**B.3 Consulta de afectadas.**
Nuevo método en `ReservationRepositoryPort`:

```java
List<UUID> findReservedByVesselOverlapping(UUID vesselId, LocalDate from, LocalDate to);
```

JPQL: `status = RESERVED and vesselId = :id and startDate <= :to and endDate >= :from`. Bordes inclusivos, igual que `existsOverlapping` (`ReservationJpaRepository.java:27`). Ordenar por `start_date` para logs deterministas. El índice GiST de la `EXCLUDE` (`V1:53-56`) cubre `vessel_id`; revisar el plan y, si no se usa, añadir `(vessel_id, status, start_date, end_date)`.

**B.4 Excepción acotada a `SYSTEM`+`BY_OWNER`.**
Hoy es imposible: `ValidateActorForSubtype` rechaza `SYSTEM` con subtipo (`:230-231`) y `BY_OWNER` exige `OWNER` (`:235`). Siguiendo `diseno-h7-previo.md`:

- Añadir `CancellationCause { RENTER, OWNER, NO_SHOW, LATE_PAYMENT, MAINTENANCE }` al comando (o un puerto dedicado `MaintenanceCancellationPort`).
- `validateActorForSubtype`: permitir `SYSTEM`+`BY_OWNER` **solo** si `cause == MAINTENANCE`. El resto de reglas intactas.
- `validateCancellationData`: con causa `MAINTENANCE` no exigir `mechanical_failure`.
- Auditoría: `reservation_status_audit` solo guarda `actor_id` y `event_ids` (`V4:16-17`); registrar la causa requiere columna nueva o usar `cancellation_event` (verificar sus columnas antes de implementar).
- El evento hacia M3 se mantiene fuera de este documento: ver `consistencia-m2-m3-pendientes.md`.

**B.5 Matriz sin `RESERVED`.**

| Transición | `notifyModuleThree` | `moduleOneStatus` | Condición |
|---|---|---|---|
| `→ IN_NAVIGATION` | sí | `IN_NAVIGATION` | Siempre; activa la guarda de ocupación |
| `→ COMPLETED` | sí | `MAINTENANCE_CLEANING` | Solo si la guarda de ocupación está activa |
| `→ CANCELLED` | sí | `AVAILABLE` / `MAINTENANCE_CLEANING` | Solo si la guarda está activa **y** no hay mantenimiento activo |
| `→ RESERVED` | sí | **ninguno** | — |

Riesgo señalado en la decisión 5: una cancelación **no** debe enviar `AVAILABLE` si la embarcación está en mantenimiento. Implementación decidida: guardar en la reserva un indicador `maintenance_blocked` al procesar el evento y pasarlo a la matriz.

Tests afectados (reescribir): `ModuleNotificationMatrixTest`, `ChangeReservationStatusServiceTest` (`:468` y alrededores), `RequestCancellationServiceTest`, `ReportNoShowServiceTest`, `EndNavigationServiceTest`, `TtlExpiryIntegrationTest`, `PaymentIntegrationTest` (aserciones sobre `M1_HTTP`).

**B.6 Adaptador de CU-10.**
Nueva firma y reglas:

| Respuesta de M1 | Resultado |
|---|---|
| `AVAILABLE`, `RESERVED`, `NAVIGATION`/`IN_NAVIGATION` | disponible para el rango |
| `MAINTENANCE`/`MAINTENANCE_CLEANING` con fechas que **no** cruzan | disponible |
| `MAINTENANCE` con fechas que cruzan | no disponible |
| `MAINTENANCE` sin fechas | no disponible (seguro) |
| valor desconocido, cuerpo vacío o ilegible | `FLEET_SERVICE_UNAVAILABLE` (503, fail-safe) |

Mantener el reintento en `503` y el mapeo de `404`/`400`/`401`. Mensaje de `VESSEL_NOT_AVAILABLE` sin "fechas seleccionadas" cuando el motivo sea mantenimiento.

**B.7 Job de reconciliación.**
**Necesario solo como respaldo.** El mecanismo primario debe ser el outbox de M1 (A.3). Si se implementa:

- ShedLock + `m2.maintenance.scan.fixed-delay-ms`, apagado por defecto.
- Coste: 1 llamada a CU-10 por embarcación con reservas `RESERVED` en una ventana (p. ej. próximos 60 días). Con 100 barcos, 100 llamadas por pasada.
- Reutiliza `ApplyMaintenanceEventService` construyendo un evento sintético; no duplica lógica.

**B.8 Tests a crear.**

| Test | Cobertura |
|---|---|
| `MaintenanceEventControllerTest` | 200/400/503, `{code,message}`, `{event_id,outcome}` |
| `ApplyMaintenanceEventServiceTest` | `APPLIED`, `DUPLICATE`, `NO_AFFECTED_RESERVATIONS`, `UNKNOWN_VESSEL`, `INVALID_RANGE`, `AUTO_CANCEL_DISABLED` |
| `MaintenanceCancellationServiceTest` | bordes inclusivos, mantenimiento sin fecha, varias afectadas, fallo parcial |
| `MaintenanceIdempotencyIntegrationTest` | reenvío, fallo parcial + reintento |
| `HttpFleetOperationalStatusAdapterTest` | mantenimiento con/sin fechas, cruce/no cruce, valor desconocido |
| `ModuleNotificationMatrixTest` | sin `RESERVED`; `COMPLETED` con guarda; cancelación con mantenimiento activo |
| `ChangeReservationStatusServiceTest` | `SYSTEM`+`BY_OWNER` con causa `MAINTENANCE`, versión, auditoría |

**B.9 Documentos a actualizar.**
`CU-10/spec.md` y `plan.md` (firma con rango, mantenimiento por fechas), `CU-22/spec.md` y `plan.md` (FR-002 sin `RESERVED`, nuevo aviso y guarda), `consistencia-m2-m1.md` (contratos C3, C4 y C5), `technical-plan/plan.md` (C3, D-07, resiliencia, jobs), `CU-02`/`CU-03` (mensaje de error), `CU-09` (catálogo C2).

> El análisis de Módulo 3 (reembolso, colas y tarifas base) se movió a `context/consistencia-m2-m3-pendientes.md`.

---

## 5. Riesgos y decisiones

### 5.1 Escenarios límite

| Escenario | Comportamiento esperado | Riesgo |
|---|---|---|
| Mantenimiento cruza una reserva `IN_NAVIGATION` | No se cancela (solo `RESERVED`); M1 no puede pasar a mantenimiento desde `NAVIGATION` | Se declara el mantenimiento al terminar el viaje; M2 no cancela la travesía en curso |
| Mantenimiento sin fecha | Se rechaza con `400 INVALID_MAINTENANCE_RANGE` | M1 no debe emitir eventos sin `maintenance_until` |
| Mantenimiento extendido después | Nuevo evento con nuevo `event_id`; cancela reservas adicionales | Correcto si M1 reenvía |
| Mantenimiento cancelado antes de tiempo | No hay evento de "fin" en el contrato; M1 tendría que liberar y no avisar | Definir si hace falta `maintenance-cancelled` |
| Reenvío del mismo aviso | `DUPLICATE` sin efectos | Correcto |
| M2 caído cuando M1 avisa | M1 reintenta desde su outbox | Requiere A.3 |
| Muchas reservas afectadas | Una transacción por reserva; respuesta 503 si alguna falla | Se procesan todas las afectadas; sin límite de lote en esta versión |
| Reserva creada entre el mantenimiento y el aviso | Se crea en `INITIATED`; no se cancela; expira por TTL o falla en CU-03 por CU-10 | Aceptable |
| Relojes desfasados | Cruce por fechas locales del puerto | Definir tolerancia |
| `maintenance_until` vencido y nadie libera | M2 lo trata como disponible (§4.1 A.5) | Depende de que M1 devuelva la fecha |

### 5.2 Alcance

Todo el alcance descrito (D1 a D7 y el catálogo C2) entra en el despliegue del **2026-11-21**. No hay piezas diferidas a una fecha posterior.

### 5.3 Decisiones aplicadas

Las decisiones están en `consistencia-m2-m1.md` §9 (D-01 a D-12). Resumen de las que afectan a este diagnóstico:

- `maintenance_until` obligatorio; mantenimiento solo desde `AVAILABLE`; liberación perezosa al consultar.
- Cruce por zona del puerto con rango inclusivo: cualquier día tocado queda bloqueado.
- M1 añade `AVAILABLE → NAVIGATION` y deja de recibir `RESERVED`.
- M1 usa outbox para C5 con reintentos 1/5/25/125 s.
- M2 cancela solo reservas `RESERVED` que cruzan el rango; las demás expiran por TTL.
- M1 crea C2 (catálogo) antes del sábado.

---

## 6. Plan de implementación (todo antes del sábado)

### M1

1. Añadir `NAVIGATION` a las salidas de `AVAILABLE` (`AssignOperationalStatusService`). Un archivo y un test.
2. Migración de ventana de mantenimiento (`maintenance_start`, `maintenance_until`), dominio, persistencia y DTO de estado con fechas.
3. Exigir `maintenance_until` y permitir el mantenimiento solo desde `AVAILABLE`.
4. Liberación perezosa al consultar (`maintenance_until` vencido → `AVAILABLE`).
5. Cliente HTTP hacia M2 + tabla `maintenance_event_outbox` + relay con reintentos.
6. Catálogo C2: `GET /api/v1/vessels` con `{vessels, pagination}`, `page`, `size`, `q`, público.

### M2

1. Quitar `RESERVED` de `ModuleNotificationMatrix` y mover la guarda de ocupación a `IN_NAVIGATION`. Tests de matriz y cancelación.
2. Endpoint `maintenance-events` + idempotencia + tests.
3. Excepción acotada de actor (`SYSTEM`+`BY_OWNER` con causa `MAINTENANCE`), auditoría y evento de cancelación.
4. CU-10 con rango + adaptador + llamadas en CU-02 y CU-03.
5. `m2.maintenance.auto-cancel` y job de reconciliación apagado por defecto.

### Verificación conjunta

1. Reservar, pagar y hacer check-in con `mode=http`: M1 pasa a `NAVIGATION` sin haber recibido `RESERVED`.
2. Declarar mantenimiento con una reserva `RESERVED` que cruza: M2 cancela y M3 recibe `CANCELLED_BY_HOST`.
3. Declarar mantenimiento con una reserva fuera del rango: no se cancela.
4. Buscar embarcaciones con `mode=http`: C2 responde con paginación.
5. `maintenance_until` vencido sin liberar: C3 devuelve `AVAILABLE`.
