# Contrato de Integración Externa: Asignar Estado Operativo de Embarcación (M1)

**Módulo Proveedor**: Módulo 1 – Gestión de Flota y Activos P2P  
**Responsable de implementarlo**: Equipo de Módulo 1  
**Módulo Consumidor**: Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones  
**CUs de Módulo 2 que lo consumen**: 
- Invocador principal único: `CU-08 Actualizar estado de reserva` (`features/CU-08-actualizar-estado-reserva/spec.md`).
- Casos de uso de negocio que detonan esta actualización a través de CU-08:
  - `CU-03 Iniciar pago`: transición a `Pendiente de Pago` $\rightarrow$ asigna `Reservado`.
  - `CU-06 Marcar inicio de navegación`: transición a `En Navegación` $\rightarrow$ asigna `En Navegación`.
  - `CU-07 Marcar fin de navegación`: transición a `Completada` $\rightarrow$ asigna `Disponible`.
  - `CU-04 Solicitar cancelación`: ordinaria $\rightarrow$ asigna `Disponible`; por avería del propietario $\rightarrow$ asigna `En Mantenimiento/Limpieza`.
  - Expiración del TTL desde `Pendiente de Pago`: $\rightarrow$ asigna `Disponible`.
  - `CU-13 Confirmar pago`: pago rechazado definitivo $\rightarrow$ asigna `Disponible`.
**Fecha**: 2026-10-09  

---

## 1. Resumen y Propósito de la Integración

Módulo 1 es el **administrador del inventario físico y del estado de amarre** en puerto. Para garantizar la consistencia entre los compromisos operativos de las reservas y la disponibilidad comercial del catálogo, Módulo 2 sincroniza el estado del activo naval en Módulo 1 ante cada momento clave del ciclo de vida de la reserva (FR-007 de CU-08).

**Reglas de Sincronización Mandatorias (FR-007 de CU-08)**:
1. **Creación en `Iniciada`**: **NO** se notifica a Módulo 1 (la embarcación sigue `Disponible`).
2. **Paso a `Pendiente de Pago`**: se actualiza a **`Reservado`** (bloqueo operativo formal).
3. **Paso a `En Navegación` (Check-in)**: se actualiza a **`En Navegación`**.
4. **Paso a `Completada` (Check-out)**: se actualiza a **`Disponible`**.
5. **Paso a `Expirada` desde `Pendiente de Pago` o `Pago Fallido`**: se actualiza a **`Disponible`**.
6. **Paso a `Expirada` desde `Iniciada`**: **NO** se notifica a Módulo 1 (nunca hubo bloqueo previo; notificarlo liberaría indebidamente el bloqueo de otra reserva vigente, provocando sobreventa).
7. **Paso a `Cancelada` con avería reportada por el Propietario**: se actualiza a **`En Mantenimiento/Limpieza`**.
8. **Paso a `Cancelada` ordinaria**: se actualiza a **`Disponible`**.

---

## 2. Definición del Endpoint

### Método HTTP y URL

```http
PUT /api/v1/embarcaciones/{embarcacionId}/estado-operativo
```

*(Ruta y verbo sujetos a ratificación con M1: `PUT` o `PATCH` [NEEDS CLARIFICATION]).*

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
| `embarcacionId` | string (UUID) | Sí | Identificador único del activo naval en Módulo 1 |

**Query Parameters**: No tiene.

**Body (JSON)**:

```json
{
  "estado_operativo": "string (Disponible | Reservado | En Navegación | En Mantenimiento/Limpieza)",
  "motivo": "string (descripción operativa opcional)",
  "reserva_id": "string (UUID, identificador de la reserva para auditoría cruzada)"
}
```

*Validaciones de entrada*:
- `estado_operativo`: debe ser estrictamente uno de los cuatro estados oficiales de flota.
- `reserva_id`: UUID válido de la reserva que origina la mutación de estado.

---

### Elementos de la Respuesta Esperada (Response)

**Código de estado HTTP (éxito)**: `200 OK`

**Contrato de respuesta tipado**:

```json
{
  "embarcacion_id": "string (UUID)",
  "estado_operativo": "string (enum actualizado)",
  "actualizado_en": "string (ISO 8601 timestamp con zona horaria)"
}
```

---

### Ejemplos de Petición y Respuestas Exitosas

#### Ejemplo 1 — Bloqueo por Inicio de Pago (`Reservado`)

**Petición `curl`**:

```bash
curl -X PUT "https://flota.seashare.internal/api/v1/embarcaciones/d3b07384-d113-49cd-a5d6-812e9bcfc101/estado-operativo" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.m2ServiceToken" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "estado_operativo": "Reservado",
    "motivo": "Bloqueo temporal por proceso de pago iniciado",
    "reserva_id": "e4f81c92-7a20-4215-9c5e-8812c3f1a001"
  }'
```

**Respuesta (`200 OK`)**:

```json
{
  "embarcacion_id": "d3b07384-d113-49cd-a5d6-812e9bcfc101",
  "estado_operativo": "Reservado",
  "actualizado_en": "2026-10-09T12:05:00-05:00"
}
```

---

#### Ejemplo 2 — Inhabilitación por Avería Mecánica (`En Mantenimiento/Limpieza`)

**Petición `curl`**:

```bash
curl -X PUT "https://flota.seashare.internal/api/v1/embarcaciones/d3b07384-d113-49cd-a5d6-812e9bcfc101/estado-operativo" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.m2ServiceToken" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "estado_operativo": "En Mantenimiento/Limpieza",
    "motivo": "Cancelación por avería en motor reportada por propietario",
    "reserva_id": "e4f81c92-7a20-4215-9c5e-8812c3f1a001"
  }'
```

**Respuesta (`200 OK`)**:

```json
{
  "embarcacion_id": "d3b07384-d113-49cd-a5d6-812e9bcfc101",
  "estado_operativo": "En Mantenimiento/Limpieza",
  "actualizado_en": "2026-10-09T12:30:15-05:00"
}
```

---

## 3. Matriz de Errores y Reacción de Módulo 2

| Código HTTP M1 | Causa en Módulo 1 | Reacción Arquitectónica de Módulo 2 | Impacto en la Transición |
|---|---|---|---|
| `400 Bad Request` | Estado operativo no reconocido o sintaxis inválida | Alerta crítica de integración en observabilidad; no reintentable | Error de sistema; no se reintenta |
| `401 / 403` | Token de servicio de M2 expirado o sin permisos | Alerta crítica de infraestructura | No se reintenta; revisión de credenciales |
| `404 Not Found` | Embarcación no existe en flota | Alerta por inconsistencia de datos | Se aborta el mensaje de salida |
| `500 / 503 / Timeout` | Indisponibilidad transitoria en Módulo 1 | Se activa la política de reintentos mediante el **patrón outbox transaccional** | La transición local de M2 queda guardada; el relay reintenta la entrega |

---

## 4. Parámetros de Resiliencia, Timeouts y Reintentos

- **Connect Timeout**: `100 ms`.
- **Read Timeout**: `300 ms` (en línea con el presupuesto de latencia de M1).
- **Patrón Outbox Transaccional y Reintentos Garantizados**:
  - Para evitar inconsistencias si M1 no responde durante la transición local de M2, la actualización del estado y el registro del aviso saliente se asientan en la **misma transacción local de PostgreSQL** (Decisión 2 de plan.md).
  - Un scheduler/relay emite la llamada hacia M1.
  - **Política de Reintentos**: máximo **5 intentos**, con *backoff* exponencial con jitter (1 s, 5 s, 25 s, 125 s); **solo ante 5xx o timeout**, nunca ante 4xx; tras el quinto fallo, se deriva a la Dead Letter Queue (DLQ) y se genera una alerta inmediata (Edge Case "Falla pasajera", T016 de plan.md).
- **Prohibición de Bloqueos Fantasma (SC-003 de CU-08)**: 0% de embarcaciones bloqueadas físicamente en M1 sin que exista una reserva en `Pendiente de Pago`, `Reservada` o `En Navegación` que la respalde.

---

## 5. Puntos Abiertos y Aclaraciones Necesarias

- `[NEEDS CLARIFICATION: Método PUT vs PATCH en Módulo 1]`: acordar con el equipo de Módulo 1 si el verbo formal para la mutación del estado operativo es `PUT` o `PATCH`.
- `[NEEDS CLARIFICATION: mecanismo de autenticación servicio-a-servicio]`: validar el emisor y claim del JWT de servicio en la cabecera `Authorization`.

---

## 6. Trazabilidad FR/SC → Elemento del Contrato

| Requisito / Criterio | Descripción en Spec CU-08 | Elemento de este Contrato |
|---|---|---|
| **FR-007 (Regla 1)** | `Iniciada`: cero aviso de bloqueo a M1 | Exclusión explícita documentada en la Sección 1 |
| **FR-007 (Regla 2)** | `Pendiente de Pago`: actualizar a `Reservado` | Request body con `estado_operativo: "Reservado"` (Ejemplo 1) |
| **FR-007 (Regla 3)** | `En Navegación`: actualizar a `En Navegación` | Request body con `estado_operativo: "En Navegación"` |
| **FR-007 (Regla 4)** | Cierre normal, cancelación ordinaria o expiración liberan | Request body con `estado_operativo: "Disponible"` |
| **FR-007 (Regla 5)** | Expiración desde `Iniciada`: NO notificar a M1 | Invariante técnica garantizada en la Sección 1 |
| **FR-007 (Regla 6)** | Cancelación por avería del propietario: `En Mantenimiento` | Request body con `estado_operativo: "En Mantenimiento/Limpieza"` (Ejemplo 2) |
| **FR-010** | Regla "Sin cálculo financiero" | Payload sin referencias económicas |
| **SC-002** | Emisión del primer aviso en < 500 ms | Read Timeout de 300 ms y outbox relay optimizado |
| **SC-003** | 0% embarcaciones bloqueadas sin reserva de respaldo | Reglas de correspondencia exacta de estados en Sección 1 |
