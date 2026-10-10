# Contrato de Evento Asíncrono: Notificación de Estado de Reserva (CU-14)

**Módulo Productor**: Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones ("Sistema de Reservas y Operaciones")  
**Módulo Consumidor**: Módulo 3 – Liquidación, Seguros y Dispersión de Fondos ("el sistema")  
**Spec de referencia**: [`Documentación/features/CU-14-recibir-estado-reserva/spec.md`](../../features/CU-14-recibir-estado-reserva/spec.md) y [`CU-08-actualizar-estado-reserva/spec.md`](../../features/CU-08-actualizar-estado-reserva/spec.md)  
**Fecha**: 2026-10-09

Este contrato especifica el evento asíncrono unidireccional enviado por RabbitMQ desde Módulo 2 hacia Módulo 3 cuando una reserva cambia de estado a partir del inicio de la fase de cobro. Se alinea con el contrato UC07 «Brindar el estado de la reserva» de Módulo 3: un único exchange, una única routing key y un enum plano de estados.

---

## 1. Topología AMQP (RabbitMQ)

| Elemento | Nombre / Configuración |
|---|---|
| Exchange | `seashare.reservations` (topic, durable) |
| Routing key | `reservation.status.changed` |
| Cola consumidora (M3) | `finance.reservation-status.v1` |
| Binding | `reservation.status.changed` |
| DLX | `seashare.reservations.dlx` |
| DLQ | `finance.reservation-status.dlq` — **[PEDIR A M3: confirmar nombre]** |

---

## 2. Metadatos y Encabezados del Mensaje (AMQP Properties)

| Propiedad | Obligatorio | Descripción |
|---|---|---|
| `Message-Id` | Sí | UUID del evento generado en el outbox (clave de deduplicación; reemplaza al antiguo `event_id` del payload) |
| `Content-Type` | Sí | `application/json` |
| `delivery_mode` | Sí | `2` (persistente) |
| `correlation_id` | No | UUID de trazabilidad |

---

## 3. Contrato del Payload (JSON Tipado)

```json
{
  "reservation_id": "string (UUID)",
  "status": "string (PENDIENTE | RESERVADO | EN_NAVEGACION | COMPLETADA | CANCELADO_FLEXIBLEMENTE | CANCELADO_MODERADAMENTE | CANCELADO_TARDIAMENTE | CANCELADO_POR_ANFITRION | EXPIRADA*)",
  "status_changed_at": "string (ISO 8601 con zona horaria)",
  "payment_token_ref": "string (obligatorio solo si status = PENDIENTE)",
  "payment_method_type": "string (opcional, solo en PENDIENTE)",
  "payment_metadata": {
    "payer_email": "...",
    "payment_method_id": "...",
    "installments": 1,
    "last_four": "..."
  }
}
```

`*` `EXPIRADA` solo cuando Módulo 3 lo habilite (ver sección 6).

Se eliminan del payload: `event_id` (pasa a `Message-Id`), `reserva_id`, `embarcacion_id`, `estado_anterior`, `nuevo_estado`, `sub_estado`, `actor_disparador`, `anticipacion_horas` y `novedades_cierre`. Esos hechos operativos los consulta Módulo 3 si los necesita; no forman parte del contrato de Módulo 3.

### Regla estricta «Sin dinero» (FR-006, SC-003)

El único dato de pago es la referencia segura (`payment_token_ref`) que Módulo 2 transporta sin interpretar. Se prohíbe incluir cualquier monto, tarifa o número de tarjeta.

---

## 4. Mapeo de estado M2 → `status` M3

| Estado / sub-estado de Módulo 2 | `status` publicado a M3 |
|---|---|
| Pendiente de Pago | `PENDIENTE` (+ campos `payment_*`) |
| Reservada | `RESERVADO` |
| En Navegación | `EN_NAVEGACION` |
| Completada | `COMPLETADA` |
| Cancelada · Flexible | `CANCELADO_FLEXIBLEMENTE` |
| Cancelada · Moderado | `CANCELADO_MODERADAMENTE` |
| Cancelada · Tardío | `CANCELADO_TARDIAMENTE` |
| Cancelada · Por Propietario | `CANCELADO_POR_ANFITRION` |
| Cancelada · Por Inasistencia | `CANCELADO_TARDIAMENTE` (provisional: mismo tratamiento financiero; ver sección 6) |
| Iniciada | No se publica (ver excepción en sección 5) |
| Expirada (sin cobro) | No se publica |
| Expirada con cobro aprobado tardío | `EXPIRADA` (ver sección 6) |
| Pago Fallido | No se publica |

Módulo 3 solo reconoce los 10 estados definidos en su contrato; cualquier otro se registra como inconsistencia sin acción. Por eso Módulo 2 no inventa valores fuera de este mapeo.

### 4.1 Reglas de publicación

- **Disparador:** tras consolidar cualquier transición de CU-08 listada en el mapeo, mediante outbox transaccional.
- **Inicio:** la primera publicación es `PENDIENTE`. Al nacer `Iniciada` no se publica `reservation.status.changed`.
- **Excepción a «nada hacia M3 en Iniciada»:** en `Iniciada` sí se publica el mensaje `reservation.info.provided` (contrato `m3-informacion-reserva.md`), que no es un cambio de estado sino la información de la reserva que Módulo 3 necesita para calcular.
- **Reintento de pago:** cada nuevo intento (CU-03) publica `PENDIENTE` de nuevo con `Message-Id` y `payment_token_ref` nuevos. No reinicia el TTL.
- **Anticipación y sub-estado:** Módulo 3 los deduce del `status`; Módulo 2 ya no los envía.

### 4.2 Ejemplos de eventos publicados

#### Variante 1 — Pendiente de pago

**`Message-Id`:** `1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d`

```json
{
  "reservation_id": "e4f81c92-7a20-4215-9c5e-8812c3f1a001",
  "status": "PENDIENTE",
  "status_changed_at": "2026-10-09T12:05:00-05:00",
  "payment_token_ref": "tok_12345abcdef",
  "payment_method_type": "CREDIT_CARD",
  "payment_metadata": {
    "payer_email": "carlos.mendoza@example.com",
    "payment_method_id": "visa",
    "installments": 1,
    "last_four": "4242"
  }
}
```

#### Variante 2 — Reservada

```json
{
  "reservation_id": "e4f81c92-7a20-4215-9c5e-8812c3f1a001",
  "status": "RESERVADO",
  "status_changed_at": "2026-10-09T12:08:45-05:00"
}
```

#### Variante 3 — Cancelada moderadamente

```json
{
  "reservation_id": "e4f81c92-7a20-4215-9c5e-8812c3f1a001",
  "status": "CANCELADO_MODERADAMENTE",
  "status_changed_at": "2026-11-13T10:00:00-05:00"
}
```

#### Variante 4 — Completada

```json
{
  "reservation_id": "e4f81c92-7a20-4215-9c5e-8812c3f1a001",
  "status": "COMPLETADA",
  "status_changed_at": "2026-11-18T18:30:00-05:00"
}
```

#### Variante 5 — Cancelada por inasistencia (provisional)

```json
{
  "reservation_id": "e4f81c92-7a20-4215-9c5e-8812c3f1a001",
  "status": "CANCELADO_TARDIAMENTE",
  "status_changed_at": "2026-11-15T09:35:00-05:00"
}
```

#### Variante 6 — Cobro huérfano tras expiración (cuando M3 habilite `EXPIRADA`)

```json
{
  "reservation_id": "7b1a2c3d-4e5f-6a7b-8c9d-0e1f2a3b4c5d",
  "status": "EXPIRADA",
  "status_changed_at": "2026-10-09T12:21:05-05:00"
}
```

---

## 5. Política de Entrega Garantizada, Reintentos y DLQ

- **Patrón Outbox Transaccional**: la inserción del evento y la transición de estado se asientan atómicamente en la misma transacción de PostgreSQL. Esto garantiza **0% de eventos perdidos** ante caídas del broker o de la red (SC-001).
- **Confirmación del Broker (*Publisher Confirms*)**: el relay de mensajería marca el mensaje como enviado solo tras recibir el `ack` de RabbitMQ.
- **Consumo con Confirmación Manual (*Manual Ack*)**: Módulo 3 debe operar con `basicAck` explícito únicamente tras procesar e insertar el evento en su almacén local.
- **Política de Reintentos**:
  - Máximo **5 intentos** por evento (primer intento inmediato tras la transacción).
  - *Backoff* exponencial con jitter: **1 s, 5 s, 25 s y 125 s** entre intentos sucesivos.
  - Se reintenta **exclusivamente ante errores de conexión, timeouts o códigos 5xx**.
  - Si un consumidor rechaza permanentemente con error 4xx de validación de esquema, no se reintenta.
  - Tras el quinto intento fallido, el mensaje se transfiere automáticamente a `seashare.reservations.dlx` → `finance.reservation-status.dlq` y se dispara una alarma crítica de observabilidad.
- **Deduplicación en Módulo 3**: la entrega es *at-least-once*. Módulo 3 debe usar la propiedad AMQP `Message-Id` como clave de deduplicación para descartar eventos duplicados sin reprocesar.

---

## 6. Puntos Abiertos y Aclaraciones Necesarias

- **[PEDIR A M3: habilitar `EXPIRADA` en el enum]**: si Módulo 3 ya cobró y Módulo 2 expiró la reserva por una carrera de TTL, Módulo 3 debe reembolsar el 100 %. Sin este estado no hay forma de solicitar el reembolso del cobro huérfano.
- **[PEDIR A M3: opcionalmente habilitar `CANCELADO_POR_INASISTENCIA`]**: tendría el mismo tratamiento financiero que `CANCELADO_TARDIAMENTE`. Hasta entonces Módulo 2 publica `CANCELADO_TARDIAMENTE`.
- **[PEDIR A M3: confirmar nombre de la DLQ]**: `finance.reservation-status.dlq`.
- Confirmar que Módulo 3 ignora los campos extra no definidos en su contrato.

---

## 7. Trazabilidad FR/SC → Elemento del Contrato

| Requisito / Criterio | Descripción en Spec | Elemento de este Contrato |
|---|---|---|
| **FR-001** | Notificación a M3 a partir de `Pendiente de Pago` | Evento de estado `PENDIENTE` y regla de inicio de integración; `reservation.info.provided` se publica por separado en `Iniciada` |
| **FR-002** | Campos requeridos: `reservation_id`, `status`, `status_changed_at` (+ `payment_*` en `PENDIENTE`) | Propiedades del payload en la sección 3 |
| **FR-005** | Entrega garantizada: 5 intentos con backoff (1/5/25/125 s) y DLQ | Especificación formal en la sección 5 |
| **FR-006** | Regla «Sin dinero»: cero cálculos financieros | Cero montos, tarifas o números de tarjeta en el payload |
| **FR-011 (CU-08)** | Identificador único del evento generado en outbox | Propiedad AMQP `Message-Id` (UUID) |
| **SC-001** | 100% de cambios notificados (0% eventos perdidos) | Outbox transaccional y topología durable RabbitMQ |
| **SC-002** | Emisión del primer intento en menos de 500 ms | Outbox relay y SLA de publicación asíncrona |
| **SC-003** | Cero valores monetarios calculados en el mensaje | Estructura estrictamente cualitativa del evento |

---
