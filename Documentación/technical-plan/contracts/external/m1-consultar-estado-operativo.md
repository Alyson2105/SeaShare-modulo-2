# Contrato de Integración Externa: Consultar Estado Operativo de Embarcación (M1)

**Módulo Proveedor**: Módulo 1 – Gestión de Flota y Activos P2P  
**Responsable de implementarlo**: Equipo de Módulo 1  
**Módulo Consumidor**: Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones  
**CUs de Módulo 2 que lo consumen**: 
- Invocador principal: `CU-10 Brindar información de estado operativo` (`features/CU-10-brindar-informacion-estado-operativo/spec.md`).
- Consumidores directos en flujos transaccionales:
  - `CU-02 Iniciar reserva` (chequeo instantáneo de estado antes de crear en `INITIATED`, FR-010).
  - `CU-03 Iniciar pago` (validación atómica de disponibilidad bajo bloqueo de fila antes de transicionar a `PENDING_PAYMENT`, FR-005).
**Fecha**: 2026-10-09  

---

## 1. Resumen y Propósito de la Integración

Módulo 1 es el **administrador del inventario físico** en la plataforma SEA-SHARE. Para prevenir la sobreventa y garantizar que ningún usuario proceda a reservar o pagar un activo náutico que se encuentre ocupado, averiado o en dique seco, Módulo 2 consume este endpoint para consultar el estado operativo instantáneo en tiempo real (FR-001, FR-002 de CU-10).

**Invariantes de Diseño**:
- **Cero acceso a Base de Datos**: Módulo 2 no accede a las tablas de Módulo 1; toda consulta es estrictamente a través de esta API síncrona (FR-007, SC-004).
- **Cero Caché**: las consultas se resuelven en tiempo real en el momento de la petición; no se permite caché local para evitar lecturas sucias durante colisiones concurrentes (Edge Case "Información siempre en tiempo real").
- **Cero Dinero**: este endpoint solo verifica la condición física y comercial del activo; carece totalmente de tarifas, montos o depósitos (FR-008).

---

## 2. Definición del Endpoint

### Método HTTP y URL

```http
GET /api/v1/vessels/{vessel_id}/operational-status
```

### Elementos de la Petición (Request)

**Headers**:

| Nombre | Obligatorio | Descripción |
|---|---|---|
| `Authorization` | Sí | `Bearer <token>` — JWT firmado de identidad de servicio expedido para Módulo 2 [NEEDS CLARIFICATION] |
| `Accept` | No | `application/json` |

**Path Parameters**:

| Nombre | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `vessel_id` | string (UUID) | Sí | Identificador único de la embarcación en Módulo 1 (FR-001) |

**Query Parameters**: No tiene.

**Body**: No tiene (operación `GET`).

---

### Elementos de la Respuesta Esperada (Response)

**Código de estado HTTP (éxito)**: `200 OK`

**Contrato de respuesta tipado**:

```json
{
  "vessel_id": "string (UUID)",
  "operational_status": "string (enum oficial de Módulo 1)",
  "queried_at": "string (ISO 8601 timestamp con zona horaria)"
}
```

#### Matriz de Estados Operativos Oficiales (FR-003, FR-004, FR-005):

| Valor de `operational_status` | Significado en Módulo 1 | Dictamen en Módulo 2 | Efecto en CU-02 / CU-03 |
|---|---|---|---|
| `AVAILABLE` | Activo náutico libre para alquiler comercial | **Apto para Reserva** | Permite continuar con la creación de reserva o inicio de pago |
| `RESERVED` | Activo bloqueado temporalmente por pago en curso o reserva confirmada | **No Apto para Reserva** | Rechaza con `409 Conflict` (motivo: bloqueado por otro proceso) |
| `IN_NAVIGATION` | Embarcación en el mar cumpliendo un contrato de viaje | **No Apto para Reserva** | Rechaza con `409 Conflict` (motivo: embarcación en uso náutico) |
| `MAINTENANCE_CLEANING` | Inhabilitada por reparaciones mecánicas, aseo o inspección | **No Apto para Reserva** | Rechaza con `409 Conflict` (motivo: activo inhabilitado) |

---

### Ejemplos de Petición y Respuestas Exitosas

#### Ejemplo 1 — Embarcación Disponible (Apta para Reserva)

**Petición `curl`**:

```bash
curl -X GET "https://flota.seashare.internal/api/v1/vessels/d3b07384-d113-49cd-a5d6-812e9bcfc101/operational-status" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.serviceTokenM2" \
  -H "Accept: application/json"
```

**Respuesta (`200 OK`)**:

```json
{
  "vessel_id": "d3b07384-d113-49cd-a5d6-812e9bcfc101",
  "operational_status": "AVAILABLE",
  "queried_at": "2026-10-09T12:05:00-05:00"
}
```

---

#### Ejemplo 2 — Embarcación Ocupada o Bloqueada (No Apta)

**Petición `curl`**:

```bash
curl -X GET "https://flota.seashare.internal/api/v1/vessels/8f4b2319-58b9-4c8d-b0a3-9e41f7d12a99/operational-status" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.serviceTokenM2" \
  -H "Accept: application/json"
```

**Respuesta (`200 OK`)**:

```json
{
  "vessel_id": "8f4b2319-58b9-4c8d-b0a3-9e41f7d12a99",
  "operational_status": "RESERVED",
  "queried_at": "2026-10-09T12:05:02-05:00"
}
```

---

## 3. Matriz de Errores y Reacción de Módulo 2

| Código HTTP M1 | Causa en Módulo 1 | Reacción Arquitectónica de Módulo 2 | Mapeo al Endpoint de M2 |
|---|---|---|---|
| `400 Bad Request` | `vessel_id` con formato UUID corrupto | Módulo 2 aborta sin reintento | `400 Bad Request` (`INVALID_ID`) |
| `401 Unauthorized` / `403 Forbidden` | Credencial de servicio de M2 rechazada | Alerta crítica de seguridad en observabilidad | `500 Internal Server Error` (`INTERNAL_ERROR`) |
| `404 Not Found` | Embarcación no existe en los registros de flota | Declara activo inexistente sin reintento (SC-005) | `404 Not Found` (`VESSEL_UNAVAILABLE`) |
| `500 Internal Server Error` / `502 Bad Gateway` | Falla interna en los servidores de Módulo 1 | Aplica máximo 1 reintento rápido; si persiste, activa *fail-safe* preventivo | `503 Service Unavailable` (`FLEET_SERVICE_UNAVAILABLE`) |
| `Timeout` (> 300 ms) | Latencia excedida en la red o base de datos de M1 | Ejecuta 1 reintento inmediato; si se agota, activa *fail-safe* preventivo | `503 Service Unavailable` (`FLEET_SERVICE_UNAVAILABLE`) |

---

## 4. Parámetros de Resiliencia, Timeouts y Reintentos

- **SLA de Respuesta**: `< 300 ms` en condiciones normales (SC-001 de CU-10).
- **Read Timeout**: `300 ms` (estricto).
- **Connect Timeout**: `100 ms`.
- **Política de Reintentos**:
  - Máximo **un (1) reintento rápido** ante fallos transitorios de red (`SocketTimeoutException`, desconexión súbita o 503).
  - **Cero reintentos** ante errores `4xx` (FR-006).
- **Principio Fail-Safe Obligatorio**: si la llamada a Módulo 1 falla tras el reintento o agota el tiempo de espera, Módulo 2 asume de forma estricta y segura que el barco **NO está disponible** (*No Apto para Reserva*), bloqueando preventivamente la operación para evitar sobreventas o alquileres sobre activos averiados (FR-006, SC-003).

---

## 5. Puntos Abiertos y Aclaraciones Necesarias

- `[NEEDS CLARIFICATION: mecanismo de autenticación servicio-a-servicio]`: acordar con el equipo de Módulo 1 la validación de identidad mediante JWT de servicio en cabecera `Authorization: Bearer <token>`.
- `[NEEDS CLARIFICATION: endpoint formal en Módulo 1]`: ratificar la ruta `GET /api/v1/vessels/{vessel_id}/operational-status` en la especificación OpenAPI de Módulo 1.

---

## 6. Trazabilidad FR/SC → Elemento del Contrato

| Requisito / Criterio | Descripción en Spec CU-10 | Elemento de este Contrato |
|---|---|---|
| **FR-001** | Interfaz interna receptora de identificador de barco | Path parameter `vessel_id` |
| **FR-002** | Consulta directa a API externa de Módulo 1 | Endpoint `GET /api/v1/vessels/{vessel_id}/operational-status` |
| **FR-003** | Reconocimiento de los cuatro estados oficiales | Matriz de estados operativos en la Sección 2 |
| **FR-004** | Dictamen de Apto para Reserva solo ante `AVAILABLE` | Regla operativa vinculada a `200 OK` en CU-02 y CU-03 |
| **FR-005** | Dictamen de No Apto ante los otros 3 estados | Mapeo de rechazo hacia error `409 Conflict` |
| **FR-006** | Fail-safe preventivo y reintentos (máx 1 rápido) | Sección 4: Timeout 300 ms, 1 reintento, corte seguro |
| **FR-007** | Prohibición absoluta de acceso a base de datos de M1 | Integración exclusivamente vía HTTP REST |
| **FR-008** | Regla "Sin dinero" (cero precios o cobros) | Payload estricto sin campos financieros |
| **FR-009** | Registro auditable de cada consulta realizada | Trazabilidad con `queried_at` y correlation id |
| **SC-001** | 100% de consultas resueltas en < 300 ms | Read Timeout configurado en 300 ms |
| **SC-002** | 0% reservas en barcos no disponibles | Verificado por la matriz de estados en backend |
| **SC-003** | 100% de errores de conexión bloquean preventivamente | Comportamiento fail-safe documentado en la Sección 4 |
| **SC-004** | Cero accesos directos a BD o cálculos de precios | Consumo de API REST sin aritmética |
| **SC-005** | Consultas de barcos inexistentes no rompen el sistema | Manejo de respuesta `404 Not Found` |
