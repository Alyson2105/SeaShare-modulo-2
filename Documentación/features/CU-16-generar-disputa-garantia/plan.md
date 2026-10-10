# Contrato de Interfaz REST: CU-16 Registrar Reclamo de Disputa

## 1. Identificación y Metadatos

- **Identificador del Contrato**: `REST-M2-CU16-REGISTRAR-RECLAMO-DISPUTA`
- **Módulo Responsable**: Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones
- **Tipo de Interfaz**: REST sincrónico expuesto por Módulo 2
- **Actor / Consumidor Autorizado**: `OWNER` (únicamente el dueño registrado de la embarcación asociada a la reserva)
- **Caso de Uso Base / Relaciones**: 
  - Caso de uso: `CU-16 Generar disputa de garantía`
  - Invocado por: `Marcar fin de navegación` (`CU-07`, `<<include>>`), que crea automáticamente la disputa en estado `PENDING` al transicionar la reserva a `COMPLETED`.
  - Incluye: `Actualizar estado de disputa de garantía` (`CU-17`, `<<include>>`).
- **Versión del Contrato**: 1.0.0
- **Fecha de Creación**: 2026-10-09

---

## 2. Propósito y Alcance del Endpoint

Este endpoint permite al **Propietario registrado** radicar formalmente su reclamo por daños, retrasos o averías sobre una disputa de garantía preexistente dentro de la ventana contractual de 24 horas.

### Responsabilidades del Endpoint:
1. **Verificación de Legitimidad y Estado Operativo (`FR-006`, `SC-005`)**:
   - Valida mediante el token JWT que el solicitante sea el propietario registrado de la embarcación vinculada a la reserva.
   - Valida que la disputa exista y se encuentre en estado `PENDING`.
   - Valida que la ventana contractual de 24 horas contada desde la creación de la disputa aún permanezca abierta (`ahora < fecha_fin_ventana`). Si la ventana expiró, el reclamo se deniega con `409 Conflict` (`WINDOW_EXPIRED`), pues el sistema ya habrá cerrado la disputa automáticamente en `REJECTED`.
2. **Registro Fáctico del Reclamo (`FR-006`, `FR-015`)**:
   - Almacena la descripción detallada del daño o problema reportado por el Propietario.
   - Registra la categoría opcional del daño (`structure`, `engine`, `equipment`, `cleaning`, `delay`) y URLs opcionales de evidencia fotográfica.
   - Fija la marca de tiempo oficial de recepción del reclamo con offset local.
3. **Mantenimiento del Estado PENDING y Cancelación del Cierre Automático**:
   - El registro del reclamo **NO cambia el estado de la disputa**: la disputa continúa en estado `PENDING` a la espera de la revisión del Administrador (`FR-006`).
   - El registro **desactiva el cierre automático por vencimiento de 24h**: la disputa ya no se cerrará en `REJECTED` de forma desatendida, sino que quedará en la bandeja administrativa para decisión humana.
4. **Ámbito Estrictamente Interno (Sin Notificación Externa en PENDING, `FR-011`)**:
   - La radicación del reclamo no emite mensajes a Módulo 3. Módulo 3 solo recibe eventos cuando la disputa alcanza un estado final (`ACCEPTED` o `REJECTED`).
5. **Regla Estricta "Sin Dinero" (`FR-010`, `SC-004`)**:
   - Módulo 2 **no tasa económicamente los daños, no solicita valoraciones monetarias, no calcula presupuestos de reparación ni debita garantías**. Toda la consecuencia financiera pertenece a Módulo 3 tras el veredicto del Admin.
6. **Diferenciación con el Check-out Náutico (`FR-007`)**:
   - El texto informativo de novedades registrado en el muelle al marcar fin de navegación (`CU-07`) es solo referencial y no constituye un reclamo formal. Este endpoint es el único medio contractual para abrir el expediente formal de disputa de garantía.

---

## 3. Definición del Endpoint

- **Método HTTP**: `POST`
- **Ruta**: `/api/v1/disputes/{dispute_id}/claims`
- **Ruta Alternativa (Alias de contexto)**: `/api/v1/reservations/{reservation_id}/dispute/claims`
- **Formato de Petición / Respuesta**: `application/json`
- **Codificación**: `UTF-8`

### 3.1 Encabezados HTTP (Headers)

| Header | Tipo | Obligatorio | Descripción |
| :--- | :--- | :--- | :--- |
| `Authorization` | String | Sí | Token Bearer JWT del Propietario autenticado (`Bearer eyJhbG...`). |
| `Content-Type` | String | Sí | Debe ser `application/json; charset=utf-8`. |
| `X-Idempotency-Key` | String (UUID) | Opcional | Clave para prevenir doble envío accidental del reclamo. |

### 3.2 Parámetros de Ruta (Path Parameters)

| Parámetro | Tipo | Restricción | Descripción |
| :--- | :--- | :--- | :--- |
| `dispute_id` | String (UUIDv4) | Obligatorio | Identificador universal único de la disputa de garantía en curso. |

---

## 4. Estructura de Datos (Schemas JSON)

### 4.1 Cuerpo de la Petición (Request Body)

```json
{
  "description": "string (obligatorio, entre 10 y 2000 caracteres)",
  "damage_category": "structure | engine | equipment | cleaning | delay | other",
  "evidence_urls": [
    "https://cdn.seashare.com/disputas/evidencia-1.jpg"
  ]
}
```

#### Descripción de Campos de Entrada

| Campo | Tipo | Obligatoriedad | Descripción / Reglas |
| :--- | :--- | :--- | :--- |
| `description` | String | **Obligatorio** | Relato fáctico detallado del daño, incidente o retraso observado (mínimo 10, máximo 2000 caracteres). |
| `damage_category` | String (Enum) | Opcional | Clasificación preliminar del incidente: `"structure"`, `"engine"`, `"equipment"`, `"cleaning"`, `"delay"`, `"other"`. |
| `evidence_urls` | Array de Strings | Opcional | Lista de enlaces seguros (máx 5) a fotografías o videos probatorios subidos al almacenamiento de SEA-SHARE. |

### 4.2 Cuerpo de Respuesta Exitosa (`201 Created`)

```json
{
  "dispute_id": "d1a2b3c4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
  "reservation_id": "c1f7a8b2-5e4d-4c3b-8a1e-9f0a2b3c4d5e",
  "status": "PENDING",
  "claim": {
    "claim_id": "r9a8b7c6-d5e4-3f2a-1b0c-9d8e7f6a5b4c",
    "owner_id": "p9a8b7c6-d5e4-3f2a-1b0c-9d8e7f6a5b4c",
    "description": "El timón llegó con holgura crítica por impacto y hubo 3 horas de retraso no justificado en la entrega de la embarcación.",
    "damage_category": "structure",
    "evidence_urls": [
      "https://cdn.seashare.com/disputas/evidencia-1.jpg"
    ],
    "registered_at": "2026-10-09T14:30:00-05:00"
  },
  "auto_close_disabled": true,
  "window_24h": {
    "start_at": "2026-10-09T10:00:00-05:00",
    "end_at": "2026-10-10T10:00:00-05:00",
    "window_status": "CLOSED_BY_CLAIM_FILED"
  },
  "message": "Tu reclamo ha quedado formalmente registrado y ha sido derivado a la bandeja de revisión administrativa. La disputa continuará en estado PENDIENTE hasta su resolución por un Administrador. No se ha aplicado ningún débito ni cálculo monetario."
}
```

#### Descripción de Campos de Salida

| Campo | Tipo | Nulabilidad | Descripción |
| :--- | :--- | :--- | :--- |
| `dispute_id` | String (UUID) | No nulo | Identificador de la disputa de garantía. |
| `reservation_id` | String (UUID) | No nulo | Identificador de la reserva vinculada. |
| `status` | String (Enum) | No nulo | Estado actual de la disputa. Siempre `"PENDING"`. |
| `claim` | Objeto | No nulo | Objeto de dominio con el reporte formal radicado por el Propietario. |
| `claim.claim_id` | String (UUID) | No nulo | Identificador universal único del reclamo registrado. |
| `claim.owner_id`| String (UUID) | No nulo | Identificador del propietario que emitió el reclamo. |
| `claim.description` | String | No nulo | Texto íntegro del reporte de avería o incidente. |
| `claim.damage_category`| String | Nulo condicional | Clasificación temática reportada. |
| `claim.evidence_urls`| Array | No nulo | Arreglo de enlaces a pruebas gráficas. |
| `claim.registered_at`| String (ISO 8601) | No nulo | Marca temporal oficial de radicación. |
| `auto_close_disabled` | Boolean | No nulo | `true`: indica que el timer de rechazo automático por inacción fue cancelado. |
| `window_24h` | Objeto | No nulo | Metadatos temporales de la ventana de garantía. |
| `message` | String | No nulo | Notificación explicativa y confirmatoria para el usuario. |

---

## 5. Ejemplos de Petición y Respuesta

### Ejemplo 1: Radicación exitosa de reclamo por daños mecánicos y estructurales

#### Petición HTTP (`curl`)
```bash
curl -X POST "https://api.seashare.com/api/v1/disputes/d1a2b3c4-e5f6-7a8b-9c0d-1e2f3a4b5c6d/claims" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -H "X-Idempotency-Key: f1111111-2222-3333-4444-555555555555" \
  -d '{
    "description": "El timón llegó con holgura crítica por impacto contra el lecho marino y hubo 3 horas de retraso no justificado en la entrega del catamarán.",
    "damage_category": "structure",
    "evidence_urls": [
      "https://cdn.seashare.com/disputas/timon-golpeado.jpg",
      "https://cdn.seashare.com/disputas/helice-muesca.jpg"
    ]
  }'
```

#### Respuesta Exitosa (`201 Created`)
```json
{
  "dispute_id": "d1a2b3c4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
  "reservation_id": "c1f7a8b2-5e4d-4c3b-8a1e-9f0a2b3c4d5e",
  "status": "PENDING",
  "claim": {
    "claim_id": "r9a8b7c6-d5e4-3f2a-1b0c-9d8e7f6a5b4c",
    "owner_id": "p9a8b7c6-d5e4-3f2a-1b0c-9d8e7f6a5b4c",
    "description": "El timón llegó con holgura crítica por impacto contra el lecho marino y hubo 3 horas de retraso no justificado en la entrega del catamarán.",
    "damage_category": "structure",
    "evidence_urls": [
      "https://cdn.seashare.com/disputas/timon-golpeado.jpg",
      "https://cdn.seashare.com/disputas/helice-muesca.jpg"
    ],
    "registered_at": "2026-10-09T14:30:00-05:00"
  },
  "auto_close_disabled": true,
  "window_24h": {
    "start_at": "2026-10-09T10:00:00-05:00",
    "end_at": "2026-10-10T10:00:00-05:00",
    "window_status": "CLOSED_BY_CLAIM_FILED"
  },
  "message": "Tu reclamo ha quedado formalmente registrado y ha sido derivado a la bandeja de revisión administrativa. La disputa continuará en estado PENDIENTE hasta su resolución por un Administrador. No se ha aplicado ningún débito ni cálculo monetario."
}
```

---

## 6. Respuestas de Error y Códigos HTTP

Todos los errores retornan el sobre uniforme con `code` y `message`:

```json
{
  "code": "ERROR_CODE",
  "message": "Descripción técnica clara del rechazo."
}
```

| Código HTTP | Código Interno (`code`) | Causa / Condición de Disparo | Cuerpo de Respuesta de Ejemplo |
| :--- | :--- | :--- | :--- |
| **`400 Bad Request`** | `VALIDATION_ERROR` | Descripción vacía o menor a 10 caracteres; categoría no reconocida. | `{"code": "VALIDATION_ERROR", "message": "La descripción del daño es obligatoria y debe contener al menos 10 caracteres."}` |
| **`400 Bad Request`** | `INVALID_UUID` | El parámetro `dispute_id` no es un UUIDv4 válido. | `{"code": "INVALID_UUID", "message": "El identificador de disputa proporcionado no es válido."}` |
| **`401 Unauthorized`** | `AUTH_TOKEN_MISSING_OR_INVALID` | El header `Authorization` no fue provisto o expiró. | `{"code": "AUTH_TOKEN_MISSING_OR_INVALID", "message": "Token de autenticación ausente o inválido."}` |
| **`403 Forbidden`** | `FORBIDDEN_NOT_OWNER` | El usuario autenticado no es el Propietario registrado del barco asociado a la disputa. | `{"code": "FORBIDDEN_NOT_OWNER", "message": "Acceso denegado: solo el propietario registrado de la embarcación puede radicar un reclamo de garantía."}` |
| **`404 Not Found`** | `DISPUTE_NOT_FOUND` | La disputa no existe en la base de datos de Módulo 2. | `{"code": "DISPUTE_NOT_FOUND", "message": "No se encontró ninguna disputa de garantía asociada al identificador provisto."}` |
| **`409 Conflict`** | `WINDOW_EXPIRED` | La ventana de 24 horas ya venció y la disputa fue cerrada automáticamente en `REJECTED`. | `{"code": "WINDOW_EXPIRED", "message": "La ventana contractual de 24 horas ha expirado. La disputa fue cerrada automáticamente por el sistema como improcedente (RECHAZADA)."}` |
| **`409 Conflict`** | `CLAIM_ALREADY_EXISTS` | Ya se radicó previamente un reclamo para esta disputa. Solo se admite un único registro formal. | `{"code": "CLAIM_ALREADY_EXISTS", "message": "Ya existe un reclamo registrado en curso para esta disputa de garantía. No se admiten múltiples registros simultáneos."}` |
| **`409 Conflict`** | `DISPUTE_ALREADY_RESOLVED` | La disputa ya no está en `PENDING` (se encuentra en `ACCEPTED` o `REJECTED`). | `{"code": "DISPUTE_ALREADY_RESOLVED", "message": "No es posible radicar reclamos sobre una disputa que ya ha alcanzado un estado final."}` |
| **`500 Internal Server Error`** | `INTERNAL_SERVER_ERROR` | Fallo de base de datos al persistir el reclamo. | `{"code": "INTERNAL_SERVER_ERROR", "message": "Error interno del servidor al procesar el reclamo de garantía."}` |

---

## 7. Notas Transversales y Reglas de Negocio Arquitecturales

### 7.1 Regla de Oro "Sin Dinero" (Zero Financial Logic)
En estricto apego a `FR-010` y `SC-004`:
- Este endpoint no solicita presupuestos de reparación, facturas con valores ni cotizaciones de repuestos.
- El objeto `claim` captura únicamente descripciones fácticas, evidencias y marcas de tiempo.
- Ninguna operación de débito, retención o cobro sobre el depósito se ejecuta en Módulo 2.

### 7.2 Cancelación Atómica del Cierre Automático
El temporizador o job programado de 24 horas (implementado en PostgreSQL como columna `expires_at timestamptz`) se desactiva atómicamente al insertar el reclamo (`UPDATE dispute_guarantee SET claim_id = :id, has_claim = true WHERE id = :id AND status = 'PENDING'`). De este modo, la disputa queda protegida del barrido automático y reservada para la intervención del Administrador.

---

## 8. Trazabilidad de Requisitos

| Requisito Funcional / Criterio | Descripción en Spec | Cobertura en este Contrato |
| :--- | :--- | :--- |
| **FR-001** | La disputa se crea automáticamente al completarse la navegación (`CU-07`). | Contextualizado en sección 1 y 2. |
| **FR-002** | Cierres que terminan en `CANCELLED` no generan disputa. | Módulo 2 solo genera disputa desde reservas `COMPLETED`. |
| **FR-003** | Creador es el Sistema; el Propietario no genera disputas (solo reclama). | Definición estricta: este endpoint es para "Registrar reclamo", no para crear disputas. |
| **FR-004** | Creación inicial en estado `PENDING`. | Confirmado en campo `status: "PENDING"`. |
| **FR-005** | Ventana de 24 horas para registrar reclamos (SLA de 24h). | Validación temporal contra `window_24h.end_at` y error `409 WINDOW_EXPIRED`. |
| **FR-006** | Propietario registra reclamo; no cambia el estado (sigue PENDING). | Implementado en payload; estado devuelto se mantiene en `PENDING`. |
| **FR-007** | Novedades del check-out de CU-07 no cuentan como reclamo formal. | Clarificado en notas; el reclamo exige invocación explícita a este endpoint. |
| **FR-008** | Cierre automático en REJECTED tras 24h sin reclamo. | Lógica de respaldo; si no se invoca este endpoint, el job ejecuta CU-17 en `REJECTED`. |
| **FR-009** | Reclamo fuera de ventana debe rechazarse sin alterar el estado. | Código `409 Conflict` (`WINDOW_EXPIRED`). |
| **FR-010** | Regla "Sin dinero": cero operaciones financieras o de depósito en M2. | Verificado en schema. Cero montos o transacciones. |
| **FR-011** | Estados finales se publican a M3 (`CU-18`); `PENDING` no se publica. | Documentado: no hay emisión de evento AMQP en este endpoint. |
| **FR-012** | Banner de cuenta regresiva y botón "Reportar problema". | Soportado por la fecha límite de la ventana. |
| **FR-015** | Tarjeta de auditoría "Tu reclamo" con descripción y fecha. | Campos `claim.description` y `claim.registered_at` expuestos para la UI. |
| **FR-016** | Ventana emergente "Reportar problema con la garantía" (descripción y envío). | Schema del request body modelado exactamente para capturar este formulario modal. |
| **SC-004** | Cero operaciones financieras o montos gestionados por M2. | Cumplimiento total. |
| **SC-005** | 100% de reclamos fuera de ventana son rechazados. | Garantizado por validación temporal en backend. |
