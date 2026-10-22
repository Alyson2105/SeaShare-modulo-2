# Notificación Asíncrona de Estado Operativo de Embarcación hacia Módulo 1 (vía CU-22)

**Módulo Proveedor**: Módulo 1 – Gestión de Flota y Activos P2P  
**Responsable de implementarlo**: Equipo de Módulo 1  
**Módulo Consumidor**: Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones  
**Canal de entrega**: asíncrono — cola de notificación a Módulo 1 definida en CU-22 (`Recibir información estado operativo`). Módulo 2 no espera respuesta de Módulo 1 ni depende de ella; la entrega se garantiza con reintentos y backoff, DLQ e idempotencia por identificador único de evento (CU-22 FR-006).
**CUs de Módulo 2 que lo consumen**: 
- Invocador principal único: `CU-08 Actualizar estado de reserva` (`features/CU-08-actualizar-estado-reserva/spec.md`).
- Casos de uso de negocio que detonan esta actualización a través de CU-08:
  - `CU-13 Confirmar pago`: transición a `RESERVED` $\rightarrow$ asigna `RESERVED`.
  - `CU-06 Marcar inicio de navegación`: transición a `IN_NAVIGATION` $\rightarrow$ asigna `IN_NAVIGATION`.
  - `CU-07 Marcar fin de navegación`: transición a `COMPLETED` $\rightarrow$ asigna `MAINTENANCE_CLEANING` (señal de que la embarcación debe pasar a Mantenimiento/Limpieza; la transición final a `AVAILABLE` la gestiona Módulo 1 internamente).
  - `CU-04 Solicitar cancelación`: ordinaria $\rightarrow$ asigna `AVAILABLE`; por avería del propietario $\rightarrow$ asigna `MAINTENANCE_CLEANING`.
  - `CU-05 Marcar inasistencia`: transición a `CANCELLED` (No-Show) $\rightarrow$ asigna `AVAILABLE`.
  - **No** se notifica Módulo 1 en las transiciones a `INITIATED`, `PENDING_PAYMENT`, `EXPIRED` ni `PAYMENT_FAILED`.
**Fecha**: 2026-10-09  

---

## 1. Resumen y Propósito de la Integración

Módulo 1 es el **administrador del inventario físico y del estado de amarre** en puerto. Para garantizar la consistencia entre los compromisos operativos de las reservas y la disponibilidad comercial del catálogo, Módulo 2 **notifica de forma asíncrona** el estado del activo naval a Módulo 1 (publicación en la cola de notificación definida en CU-22) ante cada momento clave del ciclo de vida de la reserva (FR-007 de CU-08). El flujo del caso de uso no espera respuesta de Módulo 1 ni depende de ella: la entrega la garantizan los reintentos con backoff, la DLQ y la idempotencia por identificador único de evento definidos en CU-22 FR-006; un fallo de entrega no revierte la transición local de Módulo 2.

**Reglas de Notificación Asíncrona Obligatorias (fuente de verdad: tabla de FR-002 de `CU-22` — `Recibir información estado operativo`)**:

> Este contrato **no** replica el mapeo con granularidad propia: refleja la tabla de FR-002 de `CU-22`, que es su **única fuente de verdad**. Cualquier divergencia se resuelve a favor de `CU-22` FR-002.

1. **Creación en `INITIATED`**: **NO** se notifica a Módulo 1 (la embarcación sigue `AVAILABLE`).
2. **Paso a `PENDING_PAYMENT`**: **NO** se notifica a Módulo 1. El TTL de 15 minutos y el estado de pago pendiente se gestionan íntegramente dentro de Módulo 2; el bloqueo operativo en Módulo 1 no se produce en este estado.
3. **Paso a `RESERVED`** (pago confirmado): se actualiza a **`RESERVED`** (bloqueo operativo formal). Es el único momento en que la embarcación queda reservada en el inventario de Módulo 1.
4. **Paso a `IN_NAVIGATION` (Check-in)**: se actualiza a **`IN_NAVIGATION`**.
5. **Paso a `COMPLETED` (Check-out)**: se actualiza a **`MAINTENANCE_CLEANING`** (señal de que la embarcación debe pasar a Mantenimiento/Limpieza; la transición final a `AVAILABLE` la gestiona Módulo 1 internamente).
6. **Paso a `CANCELLED`**: se actualiza a **`AVAILABLE`** en la cancelación ordinaria y a **`MAINTENANCE_CLEANING`** cuando el Propietario reporta avería mecánica. Ambas variantes se notifican **solo si la reserva ya había alcanzado `RESERVED`** (condición que en la práctica siempre se cumple, porque la cancelación activa solo se admite desde `RESERVED`).
7. **Paso a `EXPIRED` o a `PAYMENT_FAILED`**: **NO** se notifica a Módulo 1. Como la reserva nunca alcanzó `RESERVED`, Módulo 1 nunca la tuvo bloqueada y no hay nada que liberar.

---

## 2. Contrato del Receptor (Módulo 1) para la Entrega Asíncrona

> Este contrato describe lo que Módulo 1 expone al worker de entrega. **No es una llamada síncrona del flujo de Módulo 2**: la señal se origina en la cola de notificación definida en CU-22 y llega al receptor mediante la entrega asíncrona gestionada por CU-22 (reintentos con backoff, DLQ e idempotencia por identificador único de evento). Las peticiones de ejemplo son las que emite el worker de entrega.

### Método HTTP y URL

```http
PATCH /api/v1/vessels/{vessel_id}/operational-status
```
### Elementos de la Petición (Request)

**Headers**:

| Nombre | Obligatorio | Descripción |
|---|---|---|
| `Authorization` | Sí | `Bearer <token>` — JWT firmado de identidad de servicio emitido para Módulo 2 [NEEDS CLARIFICATION] |
| `Content-Type` | Sí | `application/json` |
| `Accept` | No | `application/json` |

**Path Parameters**:

| Nombre | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `vessel_id` | string (UUID) | Sí | Identificador único del activo naval en Módulo 1 |

**Query Parameters**: No tiene.

**Body (JSON)**:

```json
{
  "operational_status": "string (AVAILABLE | RESERVED | IN_NAVIGATION | MAINTENANCE_CLEANING)",
  "reason": "string (descripción operativa opcional)",
  "reservation_id": "string (UUID, identificador de la reserva para auditoría cruzada)"
}
```

*Validaciones de entrada*:
- `operational_status`: debe ser estrictamente uno de los cuatro estados oficiales de flota.
- `reservation_id`: UUID válido de la reserva que origina la mutación de estado.

---

### Elementos de la Respuesta Esperada (Response)

**Código de estado HTTP (éxito)**: `200 OK`

**Contrato de respuesta tipado**:

```json
{
  "vessel_id": "string (UUID)",
  "operational_status": "string (enum actualizado)",
  "updated_at": "string (ISO 8601 timestamp con zona horaria)"
}
```

---

### Ejemplos de Petición y Respuestas Exitosas

#### Ejemplo 1 — Bloqueo por Confirmación de Pago (`RESERVED`)

**Petición `curl`** (invocación del worker de entrega, no del flujo síncrono del caso de uso):

```bash
curl -X PUT "https://flota.seashare.internal/api/v1/vessels/d3b07384-d113-49cd-a5d6-812e9bcfc101/operational-status" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.m2ServiceToken" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "operational_status": "RESERVED",
    "reason": "Bloqueo por reserva confirmada (pago verificado)",
    "reservation_id": "e4f81c92-7a20-4215-9c5e-8812c3f1a001"
  }'
```

**Respuesta (`200 OK`)**:

```json
{
  "vessel_id": "d3b07384-d113-49cd-a5d6-812e9bcfc101",
  "operational_status": "RESERVED",
  "updated_at": "2026-10-09T12:05:00-05:00"
}
```

---

#### Ejemplo 2 — Inhabilitación por Avería Mecánica (`MAINTENANCE_CLEANING`)

**Petición `curl`** (invocación del worker de entrega, no del flujo síncrono del caso de uso):

```bash
curl -X PUT "https://flota.seashare.internal/api/v1/vessels/d3b07384-d113-49cd-a5d6-812e9bcfc101/operational-status" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.m2ServiceToken" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "operational_status": "MAINTENANCE_CLEANING",
    "reason": "Cancelación por avería en motor reportada por propietario",
    "reservation_id": "e4f81c92-7a20-4215-9c5e-8812c3f1a001"
  }'
```

**Respuesta (`200 OK`)**:

```json
{
  "vessel_id": "d3b07384-d113-49cd-a5d6-812e9bcfc101",
  "operational_status": "MAINTENANCE_CLEANING",
  "updated_at": "2026-10-09T12:30:15-05:00"
}
```

---

## 3. Matriz de Errores del Mecanismo de Entrega Asíncrona

> Estos errores **no se propagan al flujo del caso de uso**: la transición local de Módulo 2 ya está confirmada. El manejo de los fallos de entrega pertenece a CU-22 FR-006 (reintentos con backoff, DLQ e idempotencia por identificador único de evento).

| Resultado del intento de entrega (HTTP hacia Módulo 1) | Causa en Módulo 1 | Reacción del mecanismo de entrega (CU-22 FR-006) | Impacto en la Transición |
|---|---|---|---|
| `400 Bad Request` | Estado operativo no reconocido o sintaxis inválida | Alerta crítica de integración en observabilidad; no reintentable | Error de sistema; no se reintenta |
| `401 / 403` | Token de servicio de M2 expirado o sin permisos | Alerta crítica de infraestructura | No se reintenta; revisión de credenciales |
| `404 Not Found` | Embarcación no existe en flota | Alerta por inconsistencia de datos | Se aborta el mensaje de salida |
| `500 / 503 / Timeout` | Indisponibilidad transitoria en Módulo 1 | Se activa la política de reintentos mediante el **patrón outbox transaccional** | La transición local de M2 queda guardada; el relay reintenta la entrega |

---

## 4. Parámetros de Resiliencia, Timeouts y Reintentos

- **Connect Timeout**: `100 ms`.
- **Read Timeout**: `300 ms` (en línea con el presupuesto de latencia de M1). Aplica al worker de entrega; el flujo del caso de uso nunca espera esta respuesta.
- **Patrón Outbox Transaccional y Reintentos Garantizados**:
  - Para evitar inconsistencias si la entrega no se materializa durante la transición local de M2, la actualización del estado y el registro del aviso saliente se asientan en la **misma transacción local de PostgreSQL** (Decisión 2 de plan.md).
  - El scheduler/relay publica la señal en la cola de notificación definida en CU-22; la entrega se consume con confirmación del broker. El flujo del caso de uso no espera respuesta de Módulo 1.
  - **Política de Reintentos**: máximo **5 intentos**, con *backoff* exponencial con jitter (1 s, 5 s, 25 s, 125 s); **solo ante 5xx o timeout**, nunca ante 4xx; tras el quinto fallo, se deriva a la Dead Letter Queue (DLQ) y se genera una alerta inmediata (Edge Case "Falla pasajera", T016 de plan.md).
- **Prohibición de Bloqueos Fantasma (SC-003 de CU-08)**: 0% de embarcaciones bloqueadas físicamente en M1 sin que exista una reserva en `RESERVED` o `IN_NAVIGATION` que la respalde. Como en `PENDING_PAYMENT` no se notifica a M1, el estado `PENDING_PAYMENT` no respalda ningún bloqueo físico en M1.

---

## 5. Puntos Abiertos y Aclaraciones Necesarias

- `[NEEDS CLARIFICATION: Método PUT vs PATCH en Módulo 1]`: acordar con el equipo de Módulo 1 si el verbo formal para la mutación del estado operativo es `PUT` o `PATCH`.
- `[NEEDS CLARIFICATION: mecanismo de autenticación servicio-a-servicio]`: validar el emisor y claim del JWT de servicio en la cabecera `Authorization`.

---

## 6. Trazabilidad FR/SC → Elemento del Contrato

| Requisito / Criterio | Descripción en Spec CU-08 | Elemento de este Contrato |
|---|---|---|
| **FR-007 → CU-22 FR-002 (`INITIATED`)** | `INITIATED`: cero aviso de bloqueo a M1 | Exclusión explícita documentada en la Sección 1 (Regla 1) |
| **FR-007 → CU-22 FR-002 (`PENDING_PAYMENT`)** | `PENDING_PAYMENT`: cero aviso a M1 (TTL y pago pendiente se gestionan en Módulo 2) | Exclusión explícita documentada en la Sección 1 (Regla 2) |
| **FR-007 → CU-22 FR-002 (`RESERVED`)** | `RESERVED`: actualizar a `RESERVED` | Mensaje encolado con `operational_status: "RESERVED"` en la cola de notificación de CU-22 (Ejemplo 1) |
| **FR-007 → CU-22 FR-002 (`IN_NAVIGATION`)** | `IN_NAVIGATION`: actualizar a `IN_NAVIGATION` | Mensaje encolado con `operational_status: "IN_NAVIGATION"` en la cola de notificación de CU-22 |
| **FR-007 → CU-22 FR-002 (`COMPLETED`)** | `COMPLETED`: señal hacia `MAINTENANCE_CLEANING` (el retorno a `AVAILABLE` es interno de M1) | Mensaje encolado con `operational_status: "MAINTENANCE_CLEANING"` en la cola de notificación de CU-22 (Regla 5 de la Sección 1) |
| **FR-007 → CU-22 FR-002 (`CANCELLED`)** | Cancelación ordinaria: `AVAILABLE`; con avería del propietario: `MAINTENANCE_CLEANING`; solo si ya había alcanzado `RESERVED` | Mensaje encolado con `operational_status: "MAINTENANCE_CLEANING"` en la cola de notificación de CU-22 (Ejemplo 2) y Regla 6 de la Sección 1 |
| **FR-007 → CU-22 FR-002 (`EXPIRED` / `PAYMENT_FAILED`)** | `EXPIRED` y `PAYMENT_FAILED`: cero avisos a M1 | Exclusión explícita documentada en la Sección 1 (Regla 7) |
| **FR-010** | Regla "Sin cálculo financiero" | Payload sin referencias económicas |
| **SC-002** | Emisión del primer aviso en < 500 ms | Confirmación del broker y outbox relay optimizado (sin espera de respuesta de Módulo 1) |
| **SC-003** | 0% embarcaciones bloqueadas sin reserva de respaldo | Reglas de correspondencia exacta de estados en Sección 1 |
