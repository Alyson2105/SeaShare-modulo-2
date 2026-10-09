# Contrato de Evento Asíncrono: Notificación de Estado de Reserva (CU-14)

**Módulo Productor**: Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones ("Sistema de Reservas y Operaciones")  
**Módulo Consumidor**: Módulo 3 – Liquidación, Seguros y Dispersión de Fondos ("el sistema")  
**Spec de referencia**: [`Documentación/features/CU-14-recibir-estado-reserva/spec.md`](../../features/CU-14-recibir-estado-reserva/spec.md) y [`CU-08-actualizar-estado-reserva/spec.md`](../../features/CU-08-actualizar-estado-reserva/spec.md)  
**Fecha**: 2026-10-09  

Este contrato especifica el evento asíncrono unidireccional transmitido a través del broker de mensajería **RabbitMQ** desde Módulo 2 hacia Módulo 3 cada vez que una reserva cambia de estado operativo a partir del inicio de la fase de cobro (FR-001 a FR-006).

**Disparador y Condiciones de Publicación**:
- **Disparador**: se emite automáticamente tras la consolidación exitosa de cualquier transición de estado en la base de datos local gestionada por `CU-08 Actualizar estado reserva`.
- **Inicio de la integración**: la publicación inicia **estrictamente a partir de la transición a `Pendiente de Pago`** (FR-001).
- **Cuándo NO se publica**: al nacer la reserva en estado `Iniciada` (vía `Iniciar reserva`), **no** se emite ningún evento hacia Módulo 3, dado que en ese momento solo existe una reserva preliminar con TTL en curso y la plataforma financiera aún no tiene interés transaccional sobre la misma (FR-001).

---

## 1. Topología AMQP (RabbitMQ)

| Elemento | Nombre / Configuración | Descripción |
|---|---|---|
| **Exchange** | `seashare.reservas` | Exchange principal de dominio |
| **Tipo de Exchange** | `topic` | Durable, persistente |
| **Routing Key** | `reserva.estado.<nuevo_estado_slug>` | Enrutamiento segmentado por estado (ej. `reserva.estado.pendiente_pago`, `reserva.estado.reservada`, `reserva.estado.cancelada`) |
| **Cola Consumidora (M3)** | `finanzas.reservas.estado-cambio.queue` | Cola durable perteneciente al Módulo 3 |
| **Binding Pattern** | `reserva.estado.*` | Enrutamiento de todos los cambios de ciclo de vida |
| **Dead Letter Exchange (DLX)** | `seashare.reservas.dlx` | Exchange de desvío ante fallos agotados |
| **Dead Letter Queue (DLQ)** | `finanzas.reservas.estado-cambio.dlq` | Cola de mensajes muertos con alerta operativa inmediata |

---

## 2. Metadatos y Encabezados del Mensaje (AMQP Properties)

| Propiedad | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `message_id` | string (UUID) | Sí | Identificador único del evento (`event_id`) generado en el *outbox* para deduplicación idempotente en Módulo 3 (FR-011 de CU-08) |
| `correlation_id` | string (UUID) | Sí | Identificador de trazabilidad propagado en toda la cadena transaccional |
| `content_type` | string | Sí | `application/json` |
| `delivery_mode` | number | Sí | `2` (Mensaje persistente en disco) |
| `timestamp` | number | Sí | Timestamp de publicación (Unix epoch milliseconds) |

---

## 3. Contrato del Payload (JSON Tipado)

```json
{
  "event_id": "string (UUID)",
  "reserva_id": "string (UUID)",
  "embarcacion_id": "string (UUID)",
  "estado_anterior": "string (Iniciada | Pendiente de Pago | Reservada | En Navegación)",
  "nuevo_estado": "string (Pendiente de Pago | Reservada | En Navegación | Completada | Cancelada | Expirada | Pago Fallido)",
  "sub_estado": "string (Flexible | Moderado | Tardío | Por Propietario | Por Inasistencia | null)",
  "actor_disparador": "string (Arrendatario | Propietario | Sistema)",
  "timestamp": "string (ISO 8601 timestamp con zona horaria)",
  "anticipacion_horas": "number (decimal/entero, presente solo en Cancelada; null en los demás)",
  "novedades_cierre": "string (texto opcional de novedades del Propietario en Completada; null en los demás)"
}
```

### Regla Estricta "Sin Dinero" (FR-006, SC-003)
El cuerpo del evento es **estrictamente operativo**. Módulo 2 tiene terminantemente prohibido incluir montos de reembolso, valores de penalidad calculados, tarifas o cómputos de dispersión de fondos. Módulo 3 es el único responsable de interpretar el `nuevo_estado` y `sub_estado` para activar seguros, ejecutar cobros, dispensar fondos o retener depósitos.

---

## 4. Ejemplos de Eventos Publicados por Variante

### Variante 1 — Transición a `Pendiente de Pago` (Inicio de Interés Financiero)
**Routing Key**: `reserva.estado.pendiente_pago`

```json
{
  "event_id": "1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d",
  "reserva_id": "e4f81c92-7a20-4215-9c5e-8812c3f1a001",
  "embarcacion_id": "d3b07384-d113-49cd-a5d6-812e9bcfc101",
  "estado_anterior": "Iniciada",
  "nuevo_estado": "Pendiente de Pago",
  "sub_estado": null,
  "actor_disparador": "Arrendatario",
  "timestamp": "2026-10-09T12:05:00-05:00",
  "anticipacion_horas": null,
  "novedades_cierre": null
}
```

---

### Variante 2 — Transición a `Reservada` (Cobro Exitoso Confirmado por M3)
**Routing Key**: `reserva.estado.reservada`

```json
{
  "event_id": "2b3c4d5e-6f7a-8b9c-0d1e-2f3a4b5c6d7e",
  "reserva_id": "e4f81c92-7a20-4215-9c5e-8812c3f1a001",
  "embarcacion_id": "d3b07384-d113-49cd-a5d6-812e9bcfc101",
  "estado_anterior": "Pendiente de Pago",
  "nuevo_estado": "Reservada",
  "sub_estado": null,
  "actor_disparador": "Sistema",
  "timestamp": "2026-10-09T12:08:45-05:00",
  "anticipacion_horas": null,
  "novedades_cierre": null
}
```

---

### Variante 3 — Transición a `Cancelada` con Sub-Estado `Moderado` (72h a 24h)
**Routing Key**: `reserva.estado.cancelada`

```json
{
  "event_id": "3c4d5e6f-7a8b-9c0d-1e2f-3a4b5c6d7e8f",
  "reserva_id": "e4f81c92-7a20-4215-9c5e-8812c3f1a001",
  "embarcacion_id": "d3b07384-d113-49cd-a5d6-812e9bcfc101",
  "estado_anterior": "Reservada",
  "nuevo_estado": "Cancelada",
  "sub_estado": "Moderado",
  "actor_disparador": "Arrendatario",
  "timestamp": "2026-11-13T10:00:00-05:00",
  "anticipacion_horas": 48.0,
  "novedades_cierre": null
}
```

*(Módulo 3 interpreta el sub-estado `Moderado` para dispersar el 50% al Propietario y reembolsar el 50% restante al Arrendatario, conforme a sus reglas internas).*

---

### Variante 4 — Transición a `Completada` con Novedades Reportadas por Propietario
**Routing Key**: `reserva.estado.completada`

```json
{
  "event_id": "4d5e6f7a-8b9c-0d1e-2f3a-4b5c6d7e8f9a",
  "reserva_id": "e4f81c92-7a20-4215-9c5e-8812c3f1a001",
  "embarcacion_id": "d3b07384-d113-49cd-a5d6-812e9bcfc101",
  "estado_anterior": "En Navegación",
  "nuevo_estado": "Completada",
  "sub_estado": null,
  "actor_disparador": "Propietario",
  "timestamp": "2026-11-18T18:30:00-05:00",
  "anticipacion_horas": null,
  "novedades_cierre": "Regreso sin demoras; se detectó rasgadura menor en el cojín de babor al atracar."
}
```

*(El campo `novedades_cierre` es puramente informativo, FR-004. Módulo 3 liquida el alquiler base de inmediato y retiene la garantía hasta la resolución de la disputa).*

---

### Variante 5 — Transición a `Expirada` por Vencimiento del TTL
**Routing Key**: `reserva.estado.expirada`

```json
{
  "event_id": "5e6f7a8b-9c0d-1e2f-3a4b-5c6d7e8f9a0b",
  "reserva_id": "7b1a2c3d-4e5f-6a7b-8c9d-0e1f2a3b4c5d",
  "embarcacion_id": "8f4b2319-58b9-4c8d-b0a3-9e41f7d12a99",
  "estado_anterior": "Pendiente de Pago",
  "nuevo_estado": "Expirada",
  "sub_estado": null,
  "actor_disparador": "Sistema",
  "timestamp": "2026-10-09T12:20:01-05:00",
  "anticipacion_horas": null,
  "novedades_cierre": null
}
```

---

## 5. Política de Entrega Garantizada, Reintentos y DLQ

- **Patrón Outbox Transaccional**: la inserción del evento y la transición de estado se asientan atómicamente en la misma transacción de PostgreSQL (Decisión 2 de plan.md). Esto garantiza **0% de eventos perdidos** ante caídas del broker o de la red (SC-001).
- **Confirmación del Broker (*Publisher Confirms*)**: el relay de mensajería marca el mensaje como enviado solo tras recibir el `ack` de RabbitMQ.
- **Consumo con Confirmación Manual (*Manual Ack*)**: Módulo 3 debe operar con `basicAck` explícito únicamente tras procesar e insertar el evento en su almacén local.
- **Política de Reintentos (FR-005 de CU-14, T016 de plan.md)**:
  - Máximo **5 intentos** por evento (primer intento inmediato tras la transacción).
  - *Backoff* exponencial con jitter: **1 s, 5 s, 25 s y 125 s** entre intentos sucesivos.
  - Se reintenta **exclusivamente ante errores de conexión, timeouts o códigos 5xx**.
  - Si un consumidor rechaza permanentemente con error 4xx de validación de esquema, no se reintenta.
  - Tras el quinto intento fallido, el mensaje se transfiere automáticamente a `seashare.reservas.dlx` $\rightarrow$ `finanzas.reservas.estado-cambio.dlq` y se dispara una alarma crítica de observabilidad.
- **Deduplicación en Módulo 3**: la entrega es *at-least-once*. Módulo 3 debe usar el campo `event_id` como clave de deduplicación para descartar eventos duplicados sin reprocesar (Edge Case "Idempotencia").

---

## 6. Puntos Abiertos y Aclaraciones Necesarias

- `[NEEDS CLARIFICATION: Nombres formales de colas en RabbitMQ]`: confirmar con el equipo de Módulo 3 si el nombre de su cola consumidora es `finanzas.reservas.estado-cambio.queue` o si utilizan un prefijo de entorno (ej. `m3.reservas.estado`).
- `[NEEDS CLARIFICATION: TTL del mensaje en cola]`: ratificar la política de retención en cola (`x-message-ttl`: 7 días sugeridos) para mensajes no consumidos antes de DLQ.

---

## 7. Trazabilidad FR/SC → Elemento del Contrato

| Requisito / Criterio | Descripción en Spec | Elemento de este Contrato |
|---|---|---|
| **FR-001** | Notificación a M3 a partir de `Pendiente de Pago` | Evento Variante 1 y regla de inicio de integración |
| **FR-002** | Campos requeridos del evento (ID, estados, marca temporal, actor) | Propiedades del objeto JSON en Sección 3 |
| **FR-003** | Inclusión de sub-estado y anticipación en cancelación | Campos `sub_estado` y `anticipacion_horas` (Variante 3) |
| **FR-004** | Texto opcional de novedades del propietario en cierre | Campo `novedades_cierre` informativo (Variante 4) |
| **FR-005** | Entrega garantizada: 5 intentos con backoff (1/5/25/125s) y DLQ | Especificación formal en la Sección 5 |
| **FR-006** | Regla "Sin dinero": cero cálculos financieros | Cero montos o valores monetarios en el payload |
| **FR-011 (CU-08)** | Identificador único `event_id` generado en outbox | Header `message_id` y propiedad `event_id` (UUID) |
| **SC-001** | 100% de cambios notificados (0% eventos perdidos) | Outbox transaccional y topología durable RabbitMQ |
| **SC-002** | Emisión del primer intento en < 500 ms | Outbox relay y SLA de publicación asíncrona |
| **SC-003** | Cero valores monetarios calculados en el mensaje | Estructura estrictamente cualitativa del evento |
