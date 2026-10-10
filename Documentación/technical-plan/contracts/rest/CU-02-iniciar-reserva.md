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
  "embarcacion_id": "string (UUID)",
  "fecha_inicio": "string (ISO 8601, YYYY-MM-DD)",
  "fecha_fin": "string (ISO 8601, YYYY-MM-DD)",
  "pasajeros": "number (entero positivo)",
  "titular": {
    "nombre_completo": "string (obligatorio)",
    "celular": "string (obligatorio, formato telefónico)",
    "email": "string (opcional, formato email válido)"
  }
}
```

**Validaciones de entrada (FR-006, FR-007, FR-008):**

- `embarcacion_id`: UUID versión 4 obligatorio.
- `fecha_inicio` y `fecha_fin`: fechas válidas; `fecha_fin` debe ser mayor o igual a `fecha_inicio`.
- `pasajeros`: número entero positivo.
- `titular.nombre_completo`: no vacío (FR-006).
- `titular.celular`: no vacío, formato válido (FR-006).

---

## Elementos de la Respuesta (Response)

**Código de estado HTTP (éxito):** `201 Created`

**Contrato de respuesta tipado:**

```json
{
  "reserva_id": "string (UUID)",
  "estado": "string (valor literal: 'Iniciada')",
  "embarcacion_id": "string (UUID)",
  "fecha_inicio": "string (ISO 8601, YYYY-MM-DD)",
  "fecha_fin": "string (ISO 8601, YYYY-MM-DD)",
  "pasajeros": "number (entero)",
  "titular": {
    "nombre_completo": "string",
    "celular": "string",
    "email": "string"
  },
  "tarifa_estimada": {
    "monto": "number (valor literal provisto por M3, sin cálculo local)",
    "moneda": "string (ej. COP)"
  },
  "ttl_minutos": "number (fijo: 15)",
  "expira_en": "string (ISO 8601 timestamp con zona horaria del puerto)",
  "creado_en": "string (ISO 8601 timestamp)",
  "mensaje_aclaratorio": "string (texto literal: 'Monto previo. El valor final y vinculante se confirma en el paso de pago.')"
}
```

**Headers de respuesta:**

| Nombre | Valor |
|---|---|
| `Content-Type` | `application/json` |
| `Location` | `/api/v1/reservas/{reserva_id}` |

---

## Ejemplo de Petición y Respuesta Exitosa

**Petición `curl`:**

```bash
curl -X POST "https://api.seashare.com/api/v1/reservas" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.sampleToken" \
  -H "Content-Type: application/json" \
  -d '{
    "embarcacion_id": "d3b07384-d113-49cd-a5d6-812e9bcfc101",
    "fecha_inicio": "2026-11-15",
    "fecha_fin": "2026-11-18",
    "pasajeros": 8,
    "titular": {
      "nombre_completo": "Carlos Mendoza Gómez",
      "celular": "+573105558899",
      "email": "carlos.mendoza@example.com"
    }
  }'
```

**Respuesta (`201 Created`):**

```json
{
  "reserva_id": "e4f81c92-7a20-4215-9c5e-8812c3f1a001",
  "estado": "Iniciada",
  "embarcacion_id": "d3b07384-d113-49cd-a5d6-812e9bcfc101",
  "fecha_inicio": "2026-11-15",
  "fecha_fin": "2026-11-18",
  "pasajeros": 8,
  "titular": {
    "nombre_completo": "Carlos Mendoza Gómez",
    "celular": "+573105558899",
    "email": "carlos.mendoza@example.com"
  },
  "tarifa_estimada": {
    "monto": 9600000.00,
    "moneda": "COP"
  },
  "ttl_minutos": 15,
  "expira_en": "2026-10-09T12:20:00-05:00",
  "creado_en": "2026-10-09T12:05:00-05:00",
  "mensaje_aclaratorio": "Monto previo. El valor final y vinculante se confirma en el paso de pago."
}
```

---

## Manejo de Errores

| Código | Caso | Cuerpo de respuesta (ejemplo) |
|---|---|---|
| `400 Bad Request` | Falta nombre completo, celular inválido o fechas erróneas (FR-006, FR-008) | `{ "codigo": "PARAMETROS_INVALIDOS", "mensaje": "Completa el nombre del titular y el celular de contacto para continuar" }` |
| `401 Unauthorized` | Token ausente, corrupto o expirado | `{ "codigo": "NO_AUTENTICADO", "mensaje": "Token de autenticación ausente o inválido" }` |
| `403 Forbidden` | Perfil de usuario distinto de Arrendatario | `{ "codigo": "PERFIL_NO_AUTORIZADO", "mensaje": "Este recurso requiere perfil Arrendatario" }` |
| `404 Not Found` | La embarcación indicada no existe | `{ "codigo": "EMBARCACION_NO_ENCONTRADA", "mensaje": "La embarcación seleccionada no se encuentra registrada" }` |
| `409 Conflict` | La embarcación no está apta según el chequeo instantáneo en M1 (FR-010) | `{ "codigo": "EMBARCACION_NO_APTA", "mensaje": "La embarcación no está disponible para reserva (estado operativo actual: En Mantenimiento/Limpieza)" }` |
| `503 Service Unavailable` | Falla de conexión al consultar el estado operativo en Módulo 1 (CU-10) | `{ "codigo": "SERVICIO_FLOTA_NO_DISPONIBLE", "mensaje": "No se pudo verificar la disponibilidad de la embarcación en este momento" }` |

---

## Notas Transversales

- **Sin Bloqueo de Inventario en Módulo 1:** la creación en `Iniciada` **no** notifica a Módulo 1 ni bloquea el activo (FR-013).
- **Coexistencia y FCFS:** varias reservas en estado `Iniciada` pueden coexistir de forma simultánea (FR-014). La carrera transaccional se resuelve exclusivamente al momento de ejecutar el pago, adquiriendo el bloqueo atómico para el primer competidor y retornando `409 Conflict` al segundo.
- **Temporizador TTL Durable:** la expiración de 15 minutos (`expira_en`) se calcula y persiste en base de datos. Si el usuario abandona el flujo y el tiempo expira, la reserva transiciona pasivamente a `Expirada`.

---

## Trazabilidad FR/SC → Elemento del Contrato

| Requisito / Criterio | Descripción en Spec | Elemento de este Contrato |
|---|---|---|
| **FR-001** | Recepción de parámetros desde CU-19 o CU-01 | Request body: `embarcacion_id`, fechas, pasajeros |
| **FR-004** | Presentación literal de montos sin aritmética | Campo `tarifa_estimada.monto` y `mensaje_aclaratorio` |
| **FR-006** | Nombre completo y celular obligatorios | Objeto `titular.nombre_completo` y `titular.celular` en request |
| **FR-007** | Correo electrónico opcional | Objeto `titular.email` en request |
| **FR-008** | Validación de campos y rechazo ante incompletitud | Error `400 Bad Request` (`PARAMETROS_INVALIDOS`) |
| **FR-010** | Chequeo instantáneo de estado operativo en M1 (CU-10) | Error `409 Conflict` y lógica descrita en sección inicial |
| **FR-011** | Persistencia formal en estado `Iniciada` | Respuesta `201 Created` con `estado: "Iniciada"` |
| **FR-012** | Inicio del temporizador TTL de 15 minutos | Campos `ttl_minutos: 15` y `expira_en` en respuesta |
| **FR-013** | Prohibición de notificar bloqueo a M1 en `Iniciada` | Invariante técnica garantizada en Notas Transversales |
| **FR-014** | Coexistencia concurrente y FCFS en pago | Reglas de concurrencia documentadas en Notas Transversales |
| **SC-001** | 100% de reservas válidas en `Iniciada` sin bloqueo M1 | Garantizado por contrato de respuesta |
| **SC-002** | Cero sobreventas ante competencia concurrente | Garantizado en la orquestación (FCFS en el pago) |
| **SC-003** | Cero operaciones aritméticas en M2 | Transporte literal de cifras en `tarifa_estimada` |
