# Contrato REST: Iniciar Reserva (CU-02)

**Módulo:** Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones  
**Spec de referencia:** `Documentación/features/CU-02-iniciar-reserva/spec.md`  
**Fecha:** 2026-10-09

Este caso de uso expone la operación REST que formaliza la creación y persistencia de una nueva reserva en el sistema, actuando como extensión (`<<extend>>`) de los casos de uso de búsqueda o detalle de embarcación. Se activa cuando el Arrendatario confirma sus datos de contacto en el modal de checkout.

Al ejecutarse exitosamente:

1. Verifica de forma previa que la embarcación figure como `Disponible` mediante el chequeo instantáneo a Módulo 1 (`<<include>>` Brindar información de estado operativo) (FR-010).
2. Invoca internamente a `Actualizar estado reserva` (`<<include>>`) para persistir la reserva por primera vez en la base de datos con el estado inicial **`Iniciada`** (FR-011).
3. Enciende el temporizador **TTL de 15 minutos** asociado a la transacción (FR-012).
4. **No bloquea inventario en Módulo 1**: en estado `Iniciada` el activo permanece disponible en el catálogo de flota. La retención exclusiva se adquiere únicamente al transicionar a `Pendiente de Pago` en `Iniciar pago` (FR-013, FR-014).

---

## Endpoint — Creación e Inicio de Reserva

### Método HTTP y URL

```http
POST /api/v1/reservas
```

### Elementos de la Petición (Request)

**Headers:**

| Nombre | Obligatorio | Descripción |
|---|---|---|
| `Authorization` | Sí | `Bearer <token>` — JWT válido del usuario con perfil Arrendatario autenticado |
| `Content-Type` | Sí | `application/json` |
| `Accept` | No | `application/json` |

**Path Parameters:** No tiene.

**Body (JSON):**

```json
{
  "vessel_id": "string (UUID)",
  "start_at": "string (ISO 8601, YYYY-MM-DD)",
  "end_at": "string (ISO 8601, YYYY-MM-DD)",
  "passengers": "number (entero positivo)",
  "holder": {
    "full_name": "string (obligatorio)",
    "phone": "string (obligatorio, formato telefónico)",
    "email": "string (opcional, formato email válido)"
  }
}
```

**Validaciones de entrada (FR-006, FR-007, FR-008):**

- `vessel_id`: UUID versión 4 obligatorio.
- `start_at` y `end_at`: fechas válidas; `end_at` debe ser mayor o igual a `start_at`.
- `passengers`: número entero positivo.
- `holder.full_name`: no vacío (FR-006).
- `holder.phone`: no vacío, formato válido (FR-006).

---

## Elementos de la Respuesta (Response)

**Código de estado HTTP (éxito):** `201 Created`

**Contrato de respuesta tipado:**

```json
{
  "reservation_id": "string (UUID)",
  "status": "string (valor literal: 'Iniciada')",
  "vessel_id": "string (UUID)",
  "start_at": "string (ISO 8601, YYYY-MM-DD)",
  "end_at": "string (ISO 8601, YYYY-MM-DD)",
  "passengers": "number (entero)",
  "holder": {
    "full_name": "string",
    "phone": "string",
    "email": "string"
  },
  "estimated_fare": {
    "amount": "number (valor literal provisto por M3, sin cálculo local)",
    "currency": "string (ej. COP)"
  },
  "ttl_minutes": "number (fijo: 15)",
  "expires_at": "string (ISO 8601 timestamp con zona horaria del puerto)",
  "created_at": "string (ISO 8601 timestamp)",
  "clarifying_message": "string (texto literal: 'Monto previo. El valor final y vinculante se confirma en el paso de pago.')"
}
```

**Headers de respuesta:**

| Nombre | Valor |
|---|---|
| `Content-Type` | `application/json` |
| `Location` | `/api/v1/reservas/{reservation_id}` |

---

## Ejemplo de Petición y Respuesta Exitosa

**Petición `curl`:**

```bash
curl -X POST "https://api.seashare.com/api/v1/reservas" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.sampleToken" \
  -H "Content-Type: application/json" \
  -d '{
    "vessel_id": "d3b07384-d113-49cd-a5d6-812e9bcfc101",
    "start_at": "2026-11-15",
    "end_at": "2026-11-18",
    "passengers": 8,
    "holder": {
      "full_name": "Carlos Mendoza Gómez",
      "phone": "+573105558899",
      "email": "carlos.mendoza@example.com"
    }
  }'
```

**Respuesta (`201 Created`):**

```json
{
  "reservation_id": "e4f81c92-7a20-4215-9c5e-8812c3f1a001",
  "status": "Iniciada",
  "vessel_id": "d3b07384-d113-49cd-a5d6-812e9bcfc101",
  "start_at": "2026-11-15",
  "end_at": "2026-11-18",
  "passengers": 8,
  "holder": {
    "full_name": "Carlos Mendoza Gómez",
    "phone": "+573105558899",
    "email": "carlos.mendoza@example.com"
  },
  "estimated_fare": {
    "amount": 9600000.00,
    "currency": "COP"
  },
  "ttl_minutes": 15,
  "expires_at": "2026-10-09T12:20:00-05:00",
  "created_at": "2026-10-09T12:05:00-05:00",
  "clarifying_message": "Monto previo. El valor final y vinculante se confirma en el paso de pago."
}
```

---

## Manejo de Errores

| Código | Caso | Cuerpo de respuesta (ejemplo) |
|---|---|---|
| `400 Bad Request` | Falta nombre completo, celular inválido o fechas erróneas (FR-006, FR-008) | `{ "code": "PARAMETROS_INVALIDOS", "message": "Completa el nombre del titular y el celular de contacto para continuar" }` |
| `401 Unauthorized` | Token ausente, corrupto o expirado | `{ "code": "NO_AUTENTICADO", "message": "Token de autenticación ausente o inválido" }` |
| `403 Forbidden` | Perfil de usuario distinto de Arrendatario | `{ "code": "PERFIL_NO_AUTORIZADO", "message": "Este recurso requiere perfil Arrendatario" }` |
| `404 Not Found` | La embarcación indicada no existe | `{ "code": "EMBARCACION_NO_ENCONTRADA", "message": "La embarcación seleccionada no se encuentra registrada" }` |
| `409 Conflict` | La embarcación no está apta según el chequeo instantáneo en M1 (FR-010) | `{ "code": "EMBARCACION_NO_APTA", "message": "La embarcación no está disponible para reserva (estado operativo actual: En Mantenimiento/Limpieza)" }` |
| `503 Service Unavailable` | Falla de conexión al consultar el estado operativo en Módulo 1 (CU-10) | `{ "code": "SERVICIO_FLOTA_NO_DISPONIBLE", "message": "No se pudo verificar la disponibilidad de la embarcación en este momento" }` |

---

## Notas Transversales

- **Sin Bloqueo de Inventario en Módulo 1:** la creación en `Iniciada` **no** notifica a Módulo 1 ni bloquea el activo (FR-013).
- **Coexistencia y FCFS:** varias reservas en estado `Iniciada` pueden coexistir de forma simultánea (FR-014). La carrera transaccional se resuelve exclusivamente al momento de ejecutar el pago, adquiriendo el bloqueo atómico para el primer competidor y retornando `409 Conflict` al segundo.
- **Temporizador TTL Durable:** la expiración de 15 minutos (`expires_at`) se calcula y persiste en base de datos. Si el usuario abandona el flujo y el tiempo expira, la reserva transiciona pasivamente a `Expirada`.

---

## Trazabilidad FR/SC → Elemento del Contrato

| Requisito / Criterio | Descripción en Spec | Elemento de este Contrato |
|---|---|---|
| **FR-001** | Recepción de parámetros desde CU-19 o CU-01 | Request body: `vessel_id`, fechas, pasajeros |
| **FR-004** | Presentación literal de montos sin aritmética | Campo `estimated_fare.amount` y `clarifying_message` |
| **FR-006** | Nombre completo y celular obligatorios | Objeto `holder.full_name` y `holder.phone` en request |
| **FR-007** | Correo electrónico opcional | Objeto `holder.email` en request |
| **FR-008** | Validación de campos y rechazo ante incompletitud | Error `400 Bad Request` (`PARAMETROS_INVALIDOS`) |
| **FR-010** | Chequeo instantáneo de estado operativo en M1 (CU-10) | Error `409 Conflict` y lógica descrita en sección inicial |
| **FR-011** | Persistencia formal en estado `Iniciada` | Respuesta `201 Created` con `status: "Iniciada"` |
| **FR-012** | Inicio del temporizador TTL de 15 minutos | Campos `ttl_minutes: 15` y `expires_at` en respuesta |
| **FR-013** | Prohibición de notificar bloqueo a M1 en `Iniciada` | Invariante técnica garantizada en Notas Transversales |
| **FR-014** | Coexistencia concurrente y FCFS en pago | Reglas de concurrencia documentadas en Notas Transversales |
| **SC-001** | 100% de reservas válidas en `Iniciada` sin bloqueo M1 | Garantizado por contrato de respuesta |
| **SC-002** | Cero sobreventas ante competencia concurrente | Garantizado en la orquestación (FCFS en el pago) |
| **SC-003** | Cero operaciones aritméticas en M2 | Transporte literal de cifras en `estimated_fare` |
