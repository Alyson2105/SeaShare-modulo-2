# Contrato de Evento: Actualización de Disputa de Garantía (CU-18)

**Módulo Origen**: Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones  
**Módulo Destino**: Módulo 3 – Finanzas y Pasarela de Pagos  
**Fecha**: 2026-10-09  

Este contrato define el evento asíncrono que Módulo 2 emite cuando una disputa sobre el depósito de garantía de una reserva alcanza un estado terminal (resuelta por un administrador en CU-17 o cerrada automáticamente por vencimiento de plazo en CU-16).

---

## 1. Propósito

Notificar a Módulo 3 el resultado final de una disputa para que proceda con la dispersión o devolución de los fondos retenidos en el depósito de garantía, desvinculando la lógica operativa (motivos, descripciones) de la ejecución financiera.

## 2. Disparador

El evento se publica mediante el patrón Outbox transaccional de Módulo 2 en dos escenarios:

1. **Resolución Manual (CU-17)**: Un administrador de SEA-SHARE resuelve la disputa a favor del propietario (`ACEPTADA`) o a favor del arrendatario (`RECHAZADA`).
2. **Cierre Automático (CU-16)**: El sistema cierra la disputa por inactividad del propietario (`RECHAZADA`).

---

## 3. Topología de Red (RabbitMQ)

| Propiedad | Valor |
|---|---|
| **Exchange** | `seashare.reservations` (topic, durable) |
| **Routing key** | `reservation.dispute.updated` |
| **Cola consumidora (M3)** | `finance.guarantee-dispute.v1` |
| **DLX / DLQ** | `seashare.reservations.dlx` / `finance.guarantee-dispute.dlq` *[PEDIR A M3: confirmar nombre]* |

**Headers obligatorios del mensaje**:

- `Message-Id`: UUID (debe tener el mismo valor que el campo `event_key` del payload).
- `Content-Type`: `application/json`
- `delivery_mode`: `2` (persistent)

---

## 4. Estructura del Payload (JSON)

Módulo 2 publica un payload simplificado, optimizado estrictamente para la máquina de estados de Módulo 3. Se eliminan campos internos de M2 (`motivo`, `origen_resolucion`, IDs de usuarios, etc.). La deduplicación en M3 se debe realizar mediante la tupla `(reservation_id, dispute_id, event_key)`.

```json
{
  "reservation_id": "string (UUID)",
  "dispute_id": "string (UUID)",
  "status": "string (RECHAZADO | COMPLETADO)",
  "status_changed_at": "string (ISO 8601)",
  "event_key": "string (clave idempotente, ej. UUID o 'disputa-<id>-<estado>')"
}
```

**Mapeo de valores de estado**:

| **M2 (estado interno de la disputa)** | **status publicado a M3** | **Acción esperada en M3** |
|---|---|---|
| **RECHAZADA** | `RECHAZADO` | M3 reembolsa el 100% del depósito al arrendatario. |
| **ACEPTADA** | `COMPLETADO` | M3 liquida el 100% del depósito al propietario. |
| **PENDIENTE** | *(No se publica)* | M3 acepta el estado PENDIENTE en su diseño, pero M2 no lo necesita emitir; solo publica estados terminales. |

## 5. Ejemplos de Mensajes

### Cierre automático por vencimiento (M2: RECHAZADA → M3: RECHAZADO)

*Ocurre cuando el propietario no aporta pruebas en el tiempo límite (CU-16).*

```json
{
  "reservation_id": "c1f7a8b2-5e4d-4c3b-8a1e-9f0a2b3c4d5e",
  "dispute_id": "d1a2b3c4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
  "status": "RECHAZADO",
  "status_changed_at": "2026-10-10T10:00:00-05:00",
  "event_key": "disputa-d1a2b3c4-RECHAZADO"
}
```

### Resolución del Admin aceptando el reclamo (M2: ACEPTADA → M3: COMPLETADO)

*Ocurre cuando el administrador falla a favor del propietario por daños comprobados (CU-17).*

```json
{
  "reservation_id": "c1f7a8b2-5e4d-4c3b-8a1e-9f0a2b3c4d5e",
  "dispute_id": "d1a2b3c4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
  "status": "COMPLETADO",
  "status_changed_at": "2026-10-09T16:45:00-05:00",
  "event_key": "disputa-d1a2b3c4-COMPLETADO"
}
```

## 6. Comandos de Prueba (RabbitMQ CLI)

Para simular la publicación del evento de una disputa resuelta a favor del propietario directamente en el broker de pruebas:

```bash
rabbitmqadmin publish exchange="seashare.reservations" routing_key="reservation.dispute.updated" \
  payload='{"reservation_id":"c1f7a8b2-5e4d-4c3b-8a1e-9f0a2b3c4d5e","dispute_id":"d1a2b3c4-e5f6-7a8b-9c0d-1e2f3a4b5c6d","status":"COMPLETADO","status_changed_at":"2026-10-09T16:45:00-05:00","event_key":"disputa-d1a2b3c4-COMPLETADO"}' \
  properties='{"delivery_mode": 2, "content_type": "application/json", "headers": {"Message-Id": "disputa-d1a2b3c4-COMPLETADO"}}'
```

## 7. Consideraciones Transversales

### 7.1. Patrón Outbox

Módulo 2 debe garantizar la entrega al menos una vez (*at-least-once delivery*). La actualización en la tabla de disputas y la inserción en la tabla `outbox_events` deben ocurrir dentro de la misma transacción de base de datos relacional.

### 7.2. Ajustes de Coherencia con CU-16 y CU-17

- **Desacople de Motivos**: El motivo de la resolución emitido por el Administrador se sigue guardando en la base de datos de Módulo 2 (para fines informativos y de historial), pero ya no viaja a Módulo 3 a través de este evento.
- **Manejo de Nomenclaturas en CU-17**: El campo de respuesta en la API REST de Módulo 2 (`evento_publicado_m3`) se mantiene igual. Sin embargo, la API REST interna de M2 mantendrá la nomenclatura operativa (`ACEPTADA` / `RECHAZADA`), mientras que el traductor del evento inyectará los estados requeridos por Finanzas (`COMPLETADO` / `RECHAZADO`) exclusivamente para el payload del mensaje.
