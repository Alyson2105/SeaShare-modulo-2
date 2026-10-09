# Contrato de Interfaz de Eventos AMQP: CU-18 Disputa de Garantía

## 1. Identificación y Metadatos

- **Identificador del Contrato**: `EVENT-M2-CU18-DISPUTA-GARANTIA`
- **Módulo Publicador**: Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones ("Sistema de Reservas y Operaciones")
- **Módulo Consumidor**: Módulo 3 – Liquidación, Seguros y Dispersión de Fondos ("el sistema")
- **Tipo de Interfaz**: Asíncrona unidireccional vía AMQP 0-9-1 (RabbitMQ)
- **Caso de Uso Base / Relaciones**: 
  - Caso de uso: `CU-18 Recibir información de disputa de garantía`
  - Gatillado por: `CU-16 Generar disputa de garantía` (cierre automático en RECHAZADA) y `CU-17 Actualizar estado de disputa de garantía` (resolución administrativa en ACEPTADA o RECHAZADA).
- **Versión del Contrato**: 1.0.0
- **Fecha de Creación**: 2026-10-09

---

## 2. Propósito y Alcance del Mensaje

Este contrato formaliza la emisión de eventos de dominio desde Módulo 2 hacia Módulo 3 para comunicar **única y exclusivamente los estados finales alcanzados por una disputa de depósito de garantía**.

### Responsabilidades del Evento:
1. **Comunicación Exclusiva de Estados Finales (`FR-001`, `SC-001`)**:
   - Se emite un mensaje si y solo si la disputa transiciona a uno de los dos estados terminales válidos: `RECHAZADA` o `ACEPTADA`.
   - **El estado `PENDIENTE` es de ámbito estrictamente interno de Módulo 2 y jamás genera mensajes hacia la cola** (`FR-001`, `SC-001`).
2. **Desencadenamiento de la Liquidación Financiera en Módulo 3**:
   - Si el estado recibido es `RECHAZADA`: Módulo 3 gestiona internamente el **reembolso íntegro (100%)** del depósito de garantía al Arrendatario.
   - Si el estado recibido es `ACEPTADA`: Módulo 3 gestiona internamente la **liquidación total (100%)** del depósito de garantía al Propietario.
   - En SEA-SHARE **no existe retención parcial del depósito** (§3 y §4 de `consistencia-m2-m3.md`).
3. **Regla Estricta "Sin Dinero" (`FR-003`, `FR-006`, `SC-002`)**:
   - El mensaje **NO contiene montos, importes monetarios, porcentajes, cuentas bancarias ni instrucciones de pasarela**. Módulo 3 posee en sus registros el monto original congelado del depósito desde el momento del pago y calcula la dispersión con total autonomía.
4. **Garantía de Idempotencia y Deduplicación (`FR-004`, `SC-003`)**:
   - Cada evento incluye un identificador único global `event_id` y un número de versión secuencial para que Módulo 3 pueda descartar reenvíos duplicados de red sin necesidad de reconsultar el estado completo de la reserva.
5. **Garantía de Entrega y Tolerancia a Fallos (`FR-005`, `SC-004`)**:
   - Implementado mediante el patrón transaccional **Transactional Outbox** en PostgreSQL y *Publisher Confirms* en RabbitMQ. Si el broker se encuentra temporalmente inaccesible, el relay de Módulo 2 reintenta progresivamente hasta obtener confirmación de recepción (0% de eventos perdidos).

---

## 3. Topología RabbitMQ y Enrutamiento AMQP

| Propiedad | Valor / Definición | Notas de Configuración |
| :--- | :--- | :--- |
| **Exchange** | `seashare.disputas` | Exchange de tipo `topic`, durable, no auto-delete. |
| **Routing Key Patrón** | `disputa.garantia.<estado>` | Permite a Módulo 3 suscribirse selectivamente. |
| **Routing Keys Emitidas** | `disputa.garantia.rechazada`<br>`disputa.garantia.aceptada` | `disputa.garantia.pendiente` **nunca se emite**. |
| **Cola Sugerida (M3)** | `m3.disputas.garantia.queue` | Cola durable configurada por Módulo 3 con binding `disputa.garantia.*`. |
| **Dead Letter Exchange (DLQ)** | `seashare.disputas.dlx` | Exchange para mensajes envenenados o rechazados tras reintentos agotados. |
| **Cola DLQ (M3)** | `m3.disputas.garantia.dlq` | Cola de almacenamiento de mensajes no procesables para auditoría. |
| **Formato de Serialización** | `application/json` | Codificación UTF-8 estándar. |

---

## 4. Estructura del Mensaje (Schema JSON)

### 4.1 Encabezados AMQP (Message Headers / Properties)

| Header AMQP | Tipo | Obligatorio | Descripción |
| :--- | :--- | :--- | :--- |
| `message_id` | String (UUID) | Sí | Mismo valor que `event_id` del payload. |
| `timestamp` | Timestamp Unix | Sí | Época en milisegundos de emisión del evento. |
| `content_type` | String | Sí | `application/json`. |
| `delivery_mode` | Integer | Sí | `2` (Persistente / Durable en disco). |
| `correlation_id` | String (UUID) | Opcional | Identificador de trazabilidad distribuida extremo a extremo. |

### 4.2 Payload JSON del Evento

```json
{
  "event_id": "disp-evt-9f8e7d6c-5b4a-3c2d-1e0f-9a8b7c6d5e4f",
  "disputa_id": "d1a2b3c4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
  "reserva_id": "c1f7a8b2-5e4d-4c3b-8a1e-9f0a2b3c4d5e",
  "embarcacion_id": "b3e2a1d0-4f5c-6b7a-8e9f-0a1b2c3d4e5f",
  "arrendatario_id": "u1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
  "propietario_id": "p9a8b7c6-d5e4-3f2a-1b0c-9d8e7f6a5b4c",
  "estado": "RECHAZADA | ACEPTADA",
  "origen_resolucion": "AUTOMATICA_VENCIMIENTO_VENTANA | ADMINISTRATIVA_DECISION_ADMIN",
  "motivo": "string | null",
  "timestamp": "2026-10-09T16:45:00-05:00",
  "version": 1
}
```

#### Descripción de Atributos

| Atributo | Tipo | Nulabilidad | Descripción |
| :--- | :--- | :--- | :--- |
| `event_id` | String (UUID) | No nulo | Identificador universal único del evento para deduplicación idempotente (`FR-004`). |
| `disputa_id` | String (UUID) | No nulo | Identificador de la disputa de garantía en Módulo 2. |
| `reserva_id` | String (UUID) | No nulo | Identificador de la reserva náutica asociada. |
| `embarcacion_id` | String (UUID) | No nulo | Identificador de la embarcación objeto del servicio. |
| `arrendatario_id` | String (UUID) | No nulo | Identificador del cliente arrendatario titular. |
| `propietario_id` | String (UUID) | No nulo | Identificador del anfitrión propietario de la nave. |
| `estado` | String (Enum) | No nulo | Veredicto final alcanzado: `"RECHAZADA"` o `"ACEPTADA"`. |
| `origen_resolucion` | String (Enum) | No nulo | Causa del cierre: `"AUTOMATICA_VENCIMIENTO_VENTANA"` o `"ADMINISTRATIVA_DECISION_ADMIN"`. |
| `motivo` | String | Nulo condicional | Justificación informativa (ej. `"sin reclamo en ventana"` o dictamen del Admin). |
| `timestamp` | String (ISO 8601) | No nulo | Marca temporal oficial de consolidación de la resolución. |
| `version` | Entero | No nulo | Número secuencial incremental de versión del evento (por defecto `1`). |

---

## 5. Ejemplos de Mensajes por Escenario

### Escenario 1: Cierre automático por vencimiento de 24 horas sin reclamo (`RECHAZADA`)
*Módulo 3 reembolsa el 100% del depósito al Arrendatario.*

- **Routing Key**: `disputa.garantia.rechazada`

#### Payload JSON
```json
{
  "event_id": "disp-evt-01928374-abcd-ef01-2345-6789abcdef01",
  "disputa_id": "d1a2b3c4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
  "reserva_id": "c1f7a8b2-5e4d-4c3b-8a1e-9f0a2b3c4d5e",
  "embarcacion_id": "b3e2a1d0-4f5c-6b7a-8e9f-0a1b2c3d4e5f",
  "arrendatario_id": "u1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
  "propietario_id": "p9a8b7c6-d5e4-3f2a-1b0c-9d8e7f6a5b4c",
  "estado": "RECHAZADA",
  "origen_resolucion": "AUTOMATICA_VENCIMIENTO_VENTANA",
  "motivo": "sin reclamo en ventana",
  "timestamp": "2026-10-10T10:00:00-05:00",
  "version": 1
}
```

---

### Escenario 2: Resolución administrativa del Admin aceptando el reclamo (`ACEPTADA`)
*Módulo 3 liquida el 100% del depósito al Propietario.*

- **Routing Key**: `disputa.garantia.aceptada`

#### Payload JSON
```json
{
  "event_id": "disp-evt-01928374-abcd-ef01-2345-6789abcdef02",
  "disputa_id": "d1a2b3c4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
  "reserva_id": "c1f7a8b2-5e4d-4c3b-8a1e-9f0a2b3c4d5e",
  "embarcacion_id": "b3e2a1d0-4f5c-6b7a-8e9f-0a1b2c3d4e5f",
  "arrendatario_id": "u1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
  "propietario_id": "p9a8b7c6-d5e4-3f2a-1b0c-9d8e7f6a5b4c",
  "estado": "ACEPTADA",
  "origen_resolucion": "ADMINISTRATIVA_DECISION_ADMIN",
  "motivo": "Se verificó evidencia fotográfica de impacto en el timón incompatible con el desgaste normal de uso. Reclamo procedente.",
  "timestamp": "2026-10-09T16:45:00-05:00",
  "version": 1
}
```

---

### Escenario 3: Resolución administrativa del Admin rechazando el reclamo (`RECHAZADA`)
*Módulo 3 reembolsa el 100% del depósito al Arrendatario.*

- **Routing Key**: `disputa.garantia.rechazada`

#### Payload JSON
```json
{
  "event_id": "disp-evt-01928374-abcd-ef01-2345-6789abcdef03",
  "disputa_id": "d1a2b3c4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
  "reserva_id": "c1f7a8b2-5e4d-4c3b-8a1e-9f0a2b3c4d5e",
  "embarcacion_id": "b3e2a1d0-4f5c-6b7a-8e9f-0a1b2c3d4e5f",
  "arrendatario_id": "u1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
  "propietario_id": "p9a8b7c6-d5e4-3f2a-1b0c-9d8e7f6a5b4c",
  "estado": "RECHAZADA",
  "origen_resolucion": "ADMINISTRATIVA_DECISION_ADMIN",
  "motivo": "El desgaste reportado en los tapizados corresponde a fatiga normal de material y no a negligencia del arrendatario.",
  "timestamp": "2026-10-09T17:10:00-05:00",
  "version": 1
}
```

---

## 6. Comandos de Prueba y Publicación AMQP

### 6.1 Publicación mediante RabbitMQ Management CLI (`rabbitmqadmin`)

```bash
rabbitmqadmin publish exchange="seashare.disputas" routing_key="disputa.garantia.aceptada" \
  payload='{
    "event_id": "disp-evt-01928374-abcd-ef01-2345-6789abcdef02",
    "disputa_id": "d1a2b3c4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
    "reserva_id": "c1f7a8b2-5e4d-4c3b-8a1e-9f0a2b3c4d5e",
    "embarcacion_id": "b3e2a1d0-4f5c-6b7a-8e9f-0a1b2c3d4e5f",
    "arrendatario_id": "u1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
    "propietario_id": "p9a8b7c6-d5e4-3f2a-1b0c-9d8e7f6a5b4c",
    "estado": "ACEPTADA",
    "origen_resolucion": "ADMINISTRATIVA_DECISION_ADMIN",
    "motivo": "Daño estructural confirmado en inspección de quilla.",
    "timestamp": "2026-10-09T16:45:00-05:00",
    "version": 1
  }' \
  properties='{"delivery_mode": 2, "content_type": "application/json"}'
```

---

## 7. Políticas de Reintentos, Deduplicación y Manejo de Errores

### 7.1 Reintentos de Publicación en Módulo 2 (Patrón Outbox)
1. **Transacción Atómica Local**: La actualización del estado de la disputa y la inserción del evento en la tabla `outbox_evento` ocurren bajo la misma transacción de PostgreSQL (`@Transactional`).
2. **Outbox Relay**: Un componente programado lee eventos no confirmados de la tabla `outbox_evento` y los envía a RabbitMQ usando *Publisher Confirms*.
3. **Confirmación**: Solo tras recibir el `ACK` del broker, el evento se marca como `publicado = true`.
4. **Política de Reintentos**: Si RabbitMQ está caído o rechaza la conexión, el relay reintenta con backoff exponencial (1s, 5s, 25s, 125s) hasta confirmar el mensaje, asegurando 0% de pérdidas (`SC-004`).

### 7.2 Manejo de Duplicados en Módulo 3
- Módulo 3 debe registrar el par `(disputa_id, estado)` o el `event_id` en una tabla de mensajes procesados.
- Si un mensaje con un `event_id` ya registrado arriba a la cola por reenvío de red, Módulo 3 debe emitir un `ACK` inmediato y descartar el procesamiento redundante sin duplicar operaciones bancarias (`FR-004`).

---

## 8. Trazabilidad de Requisitos

| Requisito Funcional / Criterio | Descripción en Spec | Cobertura en este Contrato |
| :--- | :--- | :--- |
| **FR-001** | Publicar únicamente en estados finales (`RECHAZADA` o `ACEPTADA`). `PENDIENTE` no se publica. | Especificación estricta de routing keys y topología. Cero eventos en `PENDIENTE`. |
| **FR-002** | Payload contiene únicamente estado, IDs necesarios y motivo opcional si aplica. | Esquema JSON limpio en sección 4.2. |
| **FR-003** | Mensaje NO contiene montos, porcentajes ni instrucciones de pago. | Verificado en schema. Total ausencia de dinero en el payload. |
| **FR-004** | Incluir identificador único de evento para deduplicación idempotente en M3. | Campo `event_id` persistente y header `message_id`. |
| **FR-005** | Si la publicación falla, reintentar hasta confirmación en el broker (sin pérdidas). | Patrón Transactional Outbox + Publisher Confirms en sección 7.1. |
| **FR-006** | REGLA ESTRICTA (Sin dinero): Cero valores monetarios o cálculos en M2. | Cumplimiento total. M3 decide la liquidación con sus registros. |
| **FR-007** | SLA límite de respuesta fijado en 24 horas. | Soportado por la ventana de evaluación y jobs de M2. |
| **SC-001** | 100% de transiciones a RECHAZADA o ACEPTADA generan exactamente un mensaje. | Mapeo garantizado por el outbox relay. |
| **SC-002** | Cero mensajes con montos o referencias de pago emitidos por Módulo 2. | Esquema JSON auditado. |
| **SC-003** | 100% de mensajes incluyen identificador único para deduplicación. | Atributo `event_id` obligatorio no nulo. |
| **SC-004** | Cero (0%) eventos perdidos por fallos de publicación. | Outbox durable en PostgreSQL + reintentos progresivos. |
