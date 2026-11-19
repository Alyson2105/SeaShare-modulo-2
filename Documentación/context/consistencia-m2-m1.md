# Contrato de Evento: Estado Operativo de Embarcación (CU-22 → Módulo 1)

| Campo | Valor |
|---|---|
| Módulo productor | Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones |
| Módulo consumidor | Módulo 1 – Gestión de Flota y Activos P2P (`Asignar estado operativo`) |
| Caso de uso de M2 que lo emite | CU-22 Recibir información estado operativo (invocado por CU-08 Actualizar estado reserva mediante `<<include>>`) |
| Spec de referencia | CU-22-recibir-informacion-estado-operativo/spec.md y CU-08-actualizar-estado-reserva/spec.md |
| Tipo | Salida asíncrona por outbox (HTTP POST, destino M1_HTTP). Módulo 1 responde de forma síncrona con `{event_id, outcome}` |
| Fecha | 2026-10-22 |

## 1. Propósito

Cada vez que una reserva cambia de estado y la tabla de FR-002 de CU-22 indica que se notifica, Módulo 2 avisa a Módulo 1 el **estado operativo de la embarcación ya mapeado** (nunca el estado crudo de la reserva). Módulo 1 es la fuente de verdad del inventario náutico y no participa de la transacción local de Módulo 2.

La notificación es **puramente operativa**: no lleva montos, tarifas, penalidades, valores de reembolso ni datos de liquidación o pago (FR-007). Además, Módulo 2 **no espera una respuesta de negocio** de Módulo 1 ni revierte la transición local según el resultado de la entrega (FR-008): la consistencia es eventual y se alcanza por reintento.

Módulo 1 gestiona por su cuenta la transición final `En Mantenimiento/Limpieza → Disponible`; Módulo 2 no espera esa confirmación ni la modela.

## 2. Cuándo se publica

- Cada vez que CU-08 consolida una transición de estado cuya fila en la tabla de FR-002 de CU-22 indique **Sí** (o **Condicional** con su condición cumplida).
- Las transiciones llegan a CU-22 a través de los casos de uso de negocio que usan CU-08:

| Caso de uso de M2 | Transición de la reserva | Notificación a Módulo 1 |
|---|---|---|
| CU-13 Confirmar pago | → Reservada | Sí: `RESERVED` |
| CU-06 Marcar inicio de navegación | → En navegación | Sí: `IN_NAVIGATION` |
| CU-07 Marcar fin de navegación | → Completada | Sí: `MAINTENANCE_CLEANING` (señal de Mantenimiento/Limpieza) |
| CU-04 Solicitar cancelación | → Cancelada (ordinaria) | Sí, sólo si ya había llegado a Reservada: `AVAILABLE` |
| CU-04 Solicitar cancelación | → Cancelada (avería mecánica) | Sí, sólo si ya había llegado a Reservada: `MAINTENANCE_CLEANING` |
| CU-05 Marcar inasistencia | → Cancelada (No-Show) | Sí, sólo si ya había llegado a Reservada: `AVAILABLE` |
| CU-02 Iniciar reserva | → Iniciada | No |
| CU-03 Iniciar pago | → Pendiente de pago | No |
| CU-08 (TTL) | → Expirada | No |
| CU-08 | → Pago Fallido | No |

- La primera notificación del ciclo es `RESERVED` (al confirmarse el pago). La reserva que nace en Iniciada no genera ninguna notificación de estado operativo.
- **No se publica** nada para Iniciada, Pendiente de pago, Expirada ni Pago Fallido, bajo ninguna circunstancia.

## 3. Dónde se publica

| Elemento | Valor |
|---|---|
| Método y ruta | `POST /api/v1/internal/status-events` (VERIFICADO en M1: `StatusEventController.java:20,31`) |
| Autenticación | Sin `X-Internal-Service-Token` en esta ruta (VERIFICADO en M1: `SecurityConfig.java:92-93` la declara `permitAll`) |
| Transporte | HTTP REST síncrono, invocado desde el relay del outbox |

> El transporte es HTTP POST y no AMQP: no se define exchange, routing key ni cola para Módulo 1. Cualquier referencia anterior a una cola de M1 queda sin efecto.

## 4. Mensaje

Headers HTTP:

| Header | Obligatorio | Valor |
|---|---|---|
| `Content-Type` | Sí | `application/json` |
| `Accept` | No | `application/json` |

> El identificador de deduplicación viaja en el cuerpo (`event_id`); no se usa el header AMQP `Message-Id`.

Payload:

| Campo | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `vessel_id` | UUID | Sí | Identificador de la embarcación en Módulo 1 |
| `operational_status` | string | Sí | Estado operativo resultante, ya mapeado, según la tabla de abajo |
| `occurred_at` | datetime (ISO 8601) | Sí | Marca temporal exacta de la transición que originó el aviso |
| `event_id` | UUID | Sí | Identificador único por transición (idempotencia) |

Valores posibles de `operational_status` (estados operativos oficiales de Módulo 1):

| Valor | Significado |
|---|---|
| `AVAILABLE` | Disponible |
| `RESERVED` | Reservado |
| `IN_NAVIGATION` | En Navegación |
| `MAINTENANCE_CLEANING` | En Mantenimiento/Limpieza |

Módulo 1 acepta además los alias `NAVIGATION` y `MAINTENANCE` (VERIFICADO en M1: `AssignOperationalStatusService.java:129-130`). Módulo 2 no los emite: solo envía los cuatro valores canónicos.

Respuesta `200 OK` (VERIFICADO en M1: `StatusEventController.java:53` devuelve `{event_id, outcome}`):

| Campo | Tipo | Descripción |
|---|---|---|
| `event_id` | string | Eco del `event_id` recibido |
| `outcome` | string | Resultado de aplicar el aviso |

Catálogo de `outcome` y tratamiento en Módulo 2:

| `outcome` | Significado en Módulo 1 (VERIFICADO: `AssignOperationalStatusUseCase.java:29-36`) | Tratamiento en Módulo 2 (VERIFICADO) |
|---|---|---|
| `APPLIED` | El estado cambió | Entregado (`HttpFleetStatusNotificationAdapter.java:81`) |
| `DUPLICATE` | Ese `event_id` ya se procesó | Entregado (`:81`) |
| `NO_CHANGE` | La embarcación ya tenía ese estado | Entregado (`:81`) |
| `STALE` | El suceso es anterior al último cambio | Entregado (`:81`) |
| `UNKNOWN_VESSEL` | La embarcación no existe o es borrador | Permanente → `dead` (`:82`) |
| `INVALID_TRANSITION` | Transición fuera del contrato | Permanente → `dead` (`:82`) |

Un `outcome` ausente o fuera de ese catálogo se trata como fallo reintentable (`:76-79, :83-86`).

Respuesta `400` (VERIFICADO en M1: `GlobalExceptionHandler.java:65-71`): `ProblemDetail` con `status: 400`, `detail` y la propiedad `errors[{field, message}]` para `event_id`, `vessel_id`, `operational_status` y `occurred_at`. No usa el sobre `{code, message}` de Módulo 2.

Mapeo estado de la reserva → `operational_status` (**copia de referencia**; la fuente de verdad única es la tabla de FR-002 de `CU-22-recibir-informacion-estado-operativo/spec.md`. Ante cualquier divergencia, prevalece CU-22 FR-002):

| Estado de la reserva | ¿Se notifica a Módulo 1? | `operational_status` | Detalle |
|---|---|---|---|
| Iniciada | No | — | Sin bloqueo operativo. |
| Pendiente de pago | No | — | Estado transitorio gestionado íntegramente dentro de Módulo 2 (TTL de 15 minutos en curso y espera del resultado del cobro). El bloqueo operativo en Módulo 1 ocurre únicamente al llegar a Reservada. |
| Reservada | Sí | `RESERVED` | Bloqueo operativo de la embarcación. Es el único momento en que Módulo 1 recibe `RESERVED`, y ocurre una vez confirmado el pago. |
| En navegación | Sí | `IN_NAVIGATION` | Embarcación en uso. |
| Completada | Sí | `MAINTENANCE_CLEANING` | Señal de que la embarcación debe pasar a Mantenimiento/Limpieza. Módulo 1 gestiona internamente esa transición y, después, su retorno a Disponible. |
| Cancelada | Condicional | `AVAILABLE` por defecto; `MAINTENANCE_CLEANING` si la cancelación está asociada a una avería mecánica reportada por el Propietario | Sólo se notifica si la reserva ya había llegado previamente al estado Reservada. La condición aplica igual para la liberación normal y para la liberación con avería mecánica. |
| Expirada | No | — | La reserva nunca alcanzó Reservada; Módulo 1 nunca la tuvo bloqueada. |
| Pago Fallido | No | — | La reserva nunca alcanzó Reservada; Módulo 1 nunca la tuvo bloqueada. |

Módulo 2 sólo emite los cuatro estados operativos canónicos y nunca el estado crudo de la reserva. Un valor fuera de catálogo lo rechaza Módulo 1 con `400` (VERIFICADO en M1: `AssignOperationalStatusService.java:101-104,131`).

### 4.1 Transiciones que aplica Módulo 1 (VERIFICADO: `AssignOperationalStatusService.java:33-37`)

| Desde | Hacia |
|---|---|
| `AVAILABLE` | `RESERVED` |
| `RESERVED` | `IN_NAVIGATION`, `MAINTENANCE_CLEANING`, `AVAILABLE` |
| `IN_NAVIGATION` | `MAINTENANCE_CLEANING` |
| `MAINTENANCE_CLEANING` | Ninguna por aviso: la termina el propietario |

> Consecuencia para Módulo 2: un aviso que no encaje en esta tabla devuelve `INVALID_TRANSITION` y el evento queda `dead` (`HttpFleetStatusNotificationAdapter.java:82`).
>
> **Hallazgo (2026-11-19):** `AVAILABLE → NAVIGATION` **no** está permitida hoy. Si Módulo 2 deja de enviar `RESERVED` (diseño aprobado, ver `context/consistencia-m1-m2-mantenimiento.md`), el check-in devolvería `INVALID_TRANSITION` hasta que M1 añada esa transición.

## 5. Ejemplos

> Módulo 2 emite `occurred_at` en UTC (`Z`), porque serializa un `Instant` (`OutboxAdapter.java:124`). El contrato acepta `Z` o un offset explícito (VERIFICADO en M1: `AssignOperationalStatusService.java:108` usa `OffsetDateTime.parse`); los ejemplos de abajo se mantienen con `-05:00` como muestra del formato aceptado.

Reservada (bloqueo por pago confirmado):

```json
{
  "vessel_id": "d3b07384-d113-49cd-a5d6-812e9bcfc101",
  "operational_status": "RESERVED",
  "occurred_at": "2026-10-22T12:05:00-05:00",
  "event_id": "7f3c1a9e-2b64-4d0f-9c81-5a2d6e4b1101"
}
```

En navegación (check-in):

```json
{
  "vessel_id": "d3b07384-d113-49cd-a5d6-812e9bcfc101",
  "operational_status": "IN_NAVIGATION",
  "occurred_at": "2026-10-22T14:10:00-05:00",
  "event_id": "8a4d2b0f-3c75-4e1a-8d92-6b3e7f5c2202"
}
```

Completada (señal hacia Mantenimiento/Limpieza; el retorno a Disponible es interno de Módulo 1):

```json
{
  "vessel_id": "d3b07384-d113-49cd-a5d6-812e9bcfc101",
  "operational_status": "MAINTENANCE_CLEANING",
  "occurred_at": "2026-10-25T18:30:00-05:00",
  "event_id": "9b5e3c1a-4d86-4f2b-9e03-7c4f8a6d3303"
}
```

Cancelada ordinaria (liberación; sólo si ya había llegado a Reservada):

```json
{
  "vessel_id": "d3b07384-d113-49cd-a5d6-812e9bcfc101",
  "operational_status": "AVAILABLE",
  "occurred_at": "2026-10-24T10:00:00-05:00",
  "event_id": "0c6f4d2b-5e97-4a3c-8f14-8d5a9b7e4404"
}
```

Cancelada por avería mecánica (señal hacia Mantenimiento/Limpieza; sólo si ya había llegado a Reservada):

```json
{
  "vessel_id": "d3b07384-d113-49cd-a5d6-812e9bcfc101",
  "operational_status": "MAINTENANCE_CLEANING",
  "occurred_at": "2026-10-24T10:00:00-05:00",
  "event_id": "1d7a5e3c-6f08-4b4d-9a25-9e6b0c8f5505"
}
```

Respuesta de Módulo 1 para cualquiera de los avisos anteriores (VERIFICADO: `StatusEventController.java:53`):

```json
{
  "event_id": "1d7a5e3c-6f08-4b4d-9a25-9e6b0c8f5505",
  "outcome": "APPLIED"
}
```

## 6. Reglas de procesamiento

1. **Cuándo se publica**: cada vez que CU-08 consolida una transición notificable según FR-002. El evento se guarda en la misma transacción local que el cambio de estado (outbox) y un proceso aparte lo publica (FR-005).
2. **Entrega garantizada**: el aviso se encola y se entrega con reintentos; no existe envío síncrono directo (FR-006).
3. **Sin dinero**: el mensaje nunca lleva montos, tarifas, penalidades, valores de reembolso ni datos de liquidación o pago (FR-007).
4. **Respuesta técnica, sin negocio**: Módulo 1 responde `200` con `{event_id, outcome}`. No hay respuesta de negocio ni reversión local. Módulo 2 marca el evento como publicado cuando el `outcome` es `APPLIED`, `DUPLICATE`, `NO_CHANGE` o `STALE` (FR-008).
5. **Idempotencia**: cada transición genera un `event_id` único. Si el mensaje se entrega más de una vez, Módulo 1 lo descarta por `event_id` / `Message-Id`, que es idéntico en todos los reintentos de esa misma transición.
6. **Reintentos**: máximo 5 intentos por evento (el primero inmediato), con backoff exponencial y jitter de 1 s, 5 s, 25 s y 125 s. Sólo se reintenta ante 5xx o timeout; nunca ante un 4xx permanente.
7. **Evento muerto**: tras el quinto fallo, el evento se marca `dead=true` en `outbox_message` y se emite una alerta crítica en el log. No hay Dead Letter Queue para M1: el evento no se descarta y la transición local ya confirmada no se altera.
8. **Error permanente (4xx)**: todo 4xx se considera permanente: no se reintenta y el evento pasa a `dead=true` con alerta (`HttpFleetStatusNotificationAdapter.java:62-63`). M1 sólo produce `400` (validación), `404` (no existe) y `500` (genérico) (`GlobalExceptionHandler.java:60-71,103-117`); `408` y `429` no se emiten hoy. Queda como deuda de Módulo 2 reintentar `408/429` si M1 llegara a usarlos.
9. **Módulo 1 caído o conexión rechazada**: el evento queda en el outbox y se reintenta según la regla 6; la transición local ya quedó guardada.
10. **Orden**: este contrato no garantiza orden entre eventos de la misma embarcación. Módulo 1 descarta como `STALE` un evento anterior al último cambio (VERIFICADO en M1: `AssignOperationalStatusService.java:71-74`, comparación estricta `isBefore`); Módulo 2 no ordena los envíos.
11. **Transición final interna de Módulo 1**: cuando recibe `MAINTENANCE_CLEANING`, Módulo 1 gestiona por su cuenta el paso posterior a `Disponible`. Módulo 2 no lo notifica ni espera esa confirmación.

Pendientes con Módulo 1:
1. ~~Método, ruta y autenticación~~: **cerrado el 2026-11-19** (M1 `StatusEventController.java:20,31`; `SecurityConfig.java:92-93`).
2. ~~Catálogo de `outcome`, forma del `400` y `event_id` en la respuesta~~: **cerrado el 2026-11-19** (M1 `AssignOperationalStatusUseCase.java:29-36`; `GlobalExceptionHandler.java:65-71`; `StatusEventController.java:53`).
3. **Deuda de Módulo 2**: `408` y `429` no los emite M1; si los emitiera, hoy se tratarían como permanentes.
4. **Cerrado el 2026-11-19**: M1 guarda un único estado (`Vessel.java:35`) y no tiene fechas, así que `RESERVED` bloquea la embarcación completa. El diseño aprobado para resolverlo está en `context/consistencia-m1-m2-mantenimiento.md`.

Cerrado el 2026-11-18: el transporte es HTTP POST (destino `M1_HTTP`), no AMQP; no existe exchange, routing key ni cola de Módulo 1.

## 7. Trazabilidad

| Requisito | Descripción | Dónde se cumple |
|---|---|---|
| FR-001 de CU-22 | Encolar la notificación cuando CU-08 consolide una transición notificable | Secciones 2 y 6 (regla 1) |
| FR-002 de CU-22 | Enviar el estado operativo ya mapeado | Sección 4 (tabla de mapeo) |
| FR-003 de CU-22 | Payload: embarcación, estado mapeado y marca temporal ISO 8601 | Sección 4 |
| FR-004 de CU-22 | Identificador único por transición (`event_id`) | Sección 4 y regla 5 de la sección 6 |
| FR-005 de CU-22 | Identificador generado en la misma transacción (outbox) | Sección 6 (regla 1) |
| FR-006 de CU-22 | Entrega garantizada: reintentos, backoff, DLQ | Sección 6 (reglas 2, 6 y 7) |
| FR-007 de CU-22 / SC-003 | Sin información financiera | Sección 1 y regla 3 de la sección 6 |
| FR-008 de CU-22 | Sin espera de respuesta ni reversión local | Sección 1 y regla 4 de la sección 6 |
| FR-009 de CU-22 | No resolver doble reserva en esta notificación | Sección 1 (fuera de alcance) |
| FR-007 de CU-08 | Notificar el estado operativo a Módulo 1 | Todo el contrato |
| SC-001 de CU-22 | 0% de eventos perdidos | Outbox + reintentos + DLQ (sección 6) |
| SC-002 de CU-22 | Primer intento en menos de 500 ms | Publisher confirm (sección 6, regla 4) |
| SC-004 de CU-22 | 100% de eventos con identificador único | Sección 4 y regla 5 de la sección 6 |
| SC-005 de CU-22 | Cero estados crudos de reserva en el payload | Sección 4 (tabla de mapeo) |
