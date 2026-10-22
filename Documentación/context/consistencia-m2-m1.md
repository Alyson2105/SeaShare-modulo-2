# Contrato de Evento: Estado Operativo de Embarcación (CU-22 → Módulo 1)

| Campo | Valor |
|---|---|
| Módulo productor | Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones |
| Módulo consumidor | Módulo 1 – Gestión de Flota y Activos P2P (`Asignar estado operativo`) |
| Caso de uso de M2 que lo emite | CU-22 Recibir información estado operativo (invocado por CU-08 Actualizar estado reserva mediante `<<include>>`) |
| Spec de referencia | CU-22-recibir-informacion-estado-operativo/spec.md y CU-08-actualizar-estado-reserva/spec.md |
| Tipo | Asíncrono, unidireccional (AMQP / RabbitMQ). Módulo 1 no responde |
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
| Exchange | `<exchange-pendiente-de-definir>` (topic; convención de referencia del contrato con M3: `seashare.<dominio>`) |
| Routing key | `<routing-key-pendiente-de-definir>` (convención de referencia del contrato con M3: `<entidad>.<evento>`) |
| Cola de Módulo 1 | `<nombre-cola-pendiente-de-definir>` (convención de referencia del contrato con M3: `<consumidor>.<evento>.v1`) |

> Los tres nombres están **pendientes de definir** y se acuerdan con Módulo 1. Se sigue la convención del contrato de M3 (`CU-14-recibir-estado-reserva/plan.md`), pero este contrato no fija nombres definitivos.

## 4. Mensaje

Headers AMQP:

| Header | Obligatorio | Valor |
|---|---|---|
| `Message-Id` | Sí | UUID único del evento (mismo valor que `event_id`; Módulo 1 lo usa para descartar duplicados) |
| `Content-Type` | Sí | `application/json` |

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

Módulo 1 sólo reconoce estos cuatro estados operativos. Cualquier otro valor lo registra como inconsistencia y no hace nada; por eso Módulo 2 no publica nada fuera de esta tabla.

## 5. Ejemplos

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

## 6. Reglas de procesamiento

1. **Cuándo se publica**: cada vez que CU-08 consolida una transición notificable según FR-002. El evento se guarda en la misma transacción local que el cambio de estado (outbox) y un proceso aparte lo publica (FR-005).
2. **Entrega garantizada**: el aviso se encola y se entrega con reintentos; no existe envío síncrono directo (FR-006).
3. **Sin dinero**: el mensaje nunca lleva montos, tarifas, penalidades, valores de reembolso ni datos de liquidación o pago (FR-007).
4. **Sin respuesta**: no aplica respuesta de negocio. Para Módulo 2 el mensaje se considera entregado cuando el broker confirma la recepción (publisher confirm); recién entonces se marca como publicado en el outbox (FR-008).
5. **Idempotencia**: cada transición genera un `event_id` único. Si el mensaje se entrega más de una vez, Módulo 1 lo descarta por `event_id` / `Message-Id`, que es idéntico en todos los reintentos de esa misma transición.
6. **Reintentos**: máximo 5 intentos por evento (el primero inmediato), con backoff exponencial y jitter de 1 s, 5 s, 25 s y 125 s. Sólo se reintenta ante 5xx o timeout; nunca ante un 4xx permanente.
7. **DLQ**: tras el quinto fallo, el evento se deriva a la Dead Letter Queue y se genera una alerta crítica. El evento no se descarta y la transición local ya confirmada no se altera.
8. **Error permanente (4xx)**: si Módulo 1 rechaza el aviso con un 4xx (por ejemplo, embarcación inexistente o estado operativo no reconocido), no se reintenta y se escala directamente a DLQ con alerta.
9. **Broker caído o conexión rechazada**: el evento queda en el outbox y se reintenta según la regla 6; la transición local ya quedó guardada.
10. **Orden**: este contrato no garantiza orden entre eventos de la misma embarcación. Módulo 1 debe tratar cada evento como una transición idempotente identificada por su `event_id` (pendiente de confirmar con Módulo 1; ver sección 7).
11. **Transición final interna de Módulo 1**: cuando recibe `MAINTENANCE_CLEANING`, Módulo 1 gestiona por su cuenta el paso posterior a `Disponible`. Módulo 2 no lo notifica ni espera esa confirmación.

Pendientes con Módulo 1:
1. Definir el nombre del exchange, la routing key y la cola de Módulo 1 (sección 3).
2. Confirmar que Módulo 1 consume la cola directamente (último salto por AMQP) y no mediante un relay HTTP.
3. Confirmar las garantías de orden entre eventos de la misma embarcación (el edge case "Entrega fuera de orden" de CU-22 sigue como `[NEEDS CLARIFICATION]`).
4. Acordar el tratamiento de los 4xx del consumidor (qué códigos se consideran permanentes).

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
