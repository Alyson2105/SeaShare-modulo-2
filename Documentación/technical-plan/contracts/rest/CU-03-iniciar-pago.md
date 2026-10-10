# Contrato REST: Iniciar Pago (CU-03)

**Módulo**: Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones  
**Spec de referencia**: [`Documentación/features/CU-03-iniciar-pago/spec.md`](../../features/CU-03-iniciar-pago/spec.md)  
**Fecha**: 2026-10-09  

Este caso de uso expone la operación REST que formaliza la transición transaccional hacia el cobro de una reserva existente (FR-001 a FR-015). Actúa como la extensión (`<<extend>>`) anclada a `Ver detalle de reserva` (CU-21), ejecutándose cuando el Arrendatario titular decide proceder al pago de una reserva en estado `Iniciada` tras aceptar expresamente la política de cancelación.

Al ejecutarse exitosamente:
1. Valida que la reserva exista, pertenezca al usuario autenticado y se encuentre estrictamente en estado **`Iniciada`** con su TTL vigente (FR-002).
2. Consulta el cálculo final oficial vinculante invocando a `Brindar cálculo total de la reserva` (`<<include>>`, FR-003) respaldado por el contrato externo [`m3-calculo-total.md`](../external/m3-calculo-total.md).
3. Adquiere el bloqueo pesimista de fila en base de datos (`SELECT ... FOR UPDATE`) y ejecuta la validación atómica de disponibilidad en tiempo real consultando a Módulo 1 mediante [`m1-consultar-estado-operativo.md`](../external/m1-consultar-estado-operativo.md) (`<<include>>`, FR-005).
4. Resuelve la contienda concurrente bajo la regla **First-Come First-Served (FCFS)**: si otra reserva ganó el activo para las mismas fechas, rechaza con `409 Conflict` (FR-008, SC-002).
5. Si resulta victorioso, invoca a `Actualizar estado reserva` (CU-08) transicionando a **`Pendiente de Pago`** y notificando el bloqueo de inventario a Módulo 1 (`Reservado`).
6. **No reinicia el TTL**: el temporizador iniciado en `Iniciada` continúa consumiéndose con su marca de vencimiento original (FR-007, SC-001).
7. Retorna los datos requeridos para redirigir al usuario hacia la pasarela o motor de pagos de Módulo 3 (FR-009).

---

## Endpoint — Transición a Pendiente de Pago e Inicio de Pasarela

### Método HTTP y URL

```http
POST /api/v1/reservas/{reservation_id}/pago
```

### Elementos de la Petición (Request)

**Headers**:

| Nombre | Obligatorio | Descripción |
|---|---|---|
| `Authorization` | Sí | `Bearer <token>` — JWT válido del Arrendatario titular de la reserva |
| `Content-Type` | Sí | `application/json` |
| `Accept` | No | `application/json` |

**Path Parameters**:

| Nombre | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `reservation_id` | string (UUID) | Sí | Identificador de la reserva en estado `Iniciada` (FR-001) |

**Query Parameters**: No tiene.

**Body (JSON)**:

```json
{
  "accepts_cancellation_policy": true,
  "payment": {
    "payment_token_ref": "string (token de un solo uso generado en el frontend por el SDK de Mercado Pago)",
    "payment_method_type": "string (ej. CREDIT_CARD)",
    "payment_metadata": {
      "payer_email": "string (email del pagador, obligatorio)",
      "payment_method_id": "string (ej. visa, master)",
      "installments": 1,
      "last_four": "string (opcional, solo informativo)"
    }
  }
}
```

*Validaciones de entrada (FR-013, FR-014)*:
- `accepts_cancellation_policy`: booleano obligatorio. Debe ser estrictamente `true`. Si es `false` o no se envía, el backend bloquea el avance retornando `400 Bad Request`.
- `payment.payment_token_ref`: obligatorio, no vacío. Módulo 2 nunca recibe ni almacena número de tarjeta ni CVV.
- `payment.payment_metadata.payer_email`: obligatorio y con formato email válido (M3 lo exige para cobrar). El frontend lo toma de `holder.email` o lo solicita en el formulario de pago.
- `payment.payment_metadata.payment_method_id e installments`: obligatorios.
---

### Elementos de la Respuesta (Response)

**Código de estado HTTP (éxito)**: `200 OK`

**Contrato de respuesta tipado**:

```json
{
  "reservation_id": "string (UUID)",
  "status": "string (valor literal: 'Pendiente de Pago')",
  "vessel_id": "string (UUID)",
  "total_calculation": {
    "currency": "string (constante de plataforma: COP)",
    "total_amount": "string decimal (literal de M3)",
    "breakdown": {
      "rental_amount": "string decimal (literal de M3: rental_amount)",
      "insurance_amount": "string decimal (literal de M3: insurance_amount)",
      "guarantee_deposit_amount": "string decimal (literal de M3: guarantee_deposit_amount)"
    },
    "guarantee_message": "string (texto constante de M2)"
  },
  "payment": {
    "charge_status": "string (EN_PROCESO)",
    "attempt": "number (entero, 1 en el primer pago)",
    "confirmation_mode": "CONSULTA"
  },
  "ttl_remaining_seconds": "number (entero)",
  "expires_at": "string (ISO 8601 con vencimiento original)"
}
```

*(Módulo 2 transporta íntegramente los montos y el breakdown recibido de Módulo 3 sin ejecutar sumas, divisiones ni deducciones de comisiones locales, FR-004, FR-012, SC-003).*

**Headers de respuesta**:

| Nombre | Valor |
|---|---|
| `Content-Type` | `application/json` |
| `Cache-Control` | `no-store` |

---

### Ejemplo de Petición y Respuesta Exitosa

**Petición `curl`**:

```bash
curl -X POST "https://api.seashare.com/api/v1/reservas/e4f81c92-7a20-4215-9c5e-8812c3f1a001/pago" \
  -H "Authorization: Bearer <jwt_arrendatario>" \
  -H "Content-Type: application/json" \
  -d '{
    "accepts_cancellation_policy": true,
    "payment": {
      "payment_token_ref": "tok_12345abcdef",
      "payment_method_type": "CREDIT_CARD",
      "payment_metadata": {
        "payer_email": "carlos.mendoza@example.com",
        "payment_method_id": "visa",
        "installments": 1,
        "last_four": "4242"
      }
    }
  }'
```

**Respuesta (`200 OK`)**:

```json
{
  "reservation_id": "e4f81c92-7a20-4215-9c5e-8812c3f1a001",
  "status": "Pendiente de Pago",
  "vessel_id": "d3b07384-d113-49cd-a5d6-812e9bcfc101",
  "total_calculation": {
    "currency": "COP",
    "total_amount": "10040000.00",
    "breakdown": {
      "rental_amount": "9600000.00",
      "insurance_amount": "120000.00",
      "guarantee_deposit_amount": "320000.00"
    },
    "guarantee_message": "El depósito se reembolsa completo si el barco se devuelve sin daños"
  },
  "payment": { "charge_status": "EN_PROCESO", "attempt": 1, "confirmation_mode": "CONSULTA" },
  "ttl_remaining_seconds": 645,
  "expires_at": "2026-10-09T12:20:00-05:00"
}
```

---

### Manejo de Errores

| Código | Caso | Cuerpo de respuesta (ejemplo) |
|---|---|---|
| `400 Bad Request` | No se aceptó la política de cancelación (`accepts_cancellation_policy: false`) o `reservation_id` no es UUID (FR-014) | `{ "code": "POLITICA_NO_ACEPTADA", "message": "Se requiere tu consentimiento explícito para las políticas de cancelación para iniciar el pago" }` |
| `400	Bad Request` |Falta payment.payment_token_ref, payer_email, payment_method_id o installments |  `{ "code": "DATOS_PAGO_INCOMPLETOS", "message": "Faltan los datos del medio de pago para iniciar el cobro" }` |
| `401 Unauthorized` | Token ausente, inválido o expirado | `{ "code": "NO_AUTENTICADO", "message": "Token de autenticación ausente o inválido" }` |
| `403 Forbidden` | El usuario autenticado no es el Arrendatario titular de la reserva | `{ "code": "RESERVA_NO_PERTENECE", "message": "No tienes autorización para iniciar el pago de esta reserva" }` |
| `404 Not Found` | La reserva no existe en el sistema | `{ "code": "RESERVA_NO_ENCONTRADA", "message": "La reserva especificada no existe" }` |
| `409 Conflict` (Estado Inválido) | La reserva no se encuentra en estado `Iniciada` (ej. ya está `Pendiente de Pago`, `Reservada`, `Cancelada`) (FR-002) | `{ "code": "ESTADO_INVALIDO", "message": "Solo se puede iniciar el pago de reservas en estado Iniciada (estado actual: Pendiente de Pago)" }` |
| `409 Conflict` (TTL Expirado) | El temporizador TTL de 15 minutos venció antes de iniciar el pago (Edge Case TTL) | `{ "code": "RESERVA_EXPIRADA", "message": "El tiempo límite de 15 minutos para iniciar el pago de esta reserva ha expirado" }` |
| `409 Conflict` (Carrera Concurrente FCFS) | Las fechas acaban de ser bloqueadas por otro usuario que pagó primero (FR-008, SC-002) | `{ "code": "CONFLICTO_CONCURRENCIA", "message": "La embarcación ya no se encuentra disponible para las fechas seleccionadas debido a un pago concurrente" }` |
| `409 Conflict` | Reintento no permitido: la reserva está en Pendiente de Pago y el último cobro de M3 no figura como RECHAZADO | `{ "code": "REINTENTO_NO_PERMITIDO", "message": "Ya hay un cobro en proceso para esta reserva" }` |
| `503 Service Unavailable` | Módulo 3 no responde o falla al entregar el cálculo definitivo (*fail-safe*, FR-003, FR-008) | `{ "code": "CALCULO_NO_DISPONIBLE", "message": "No se pudo obtener el cálculo total de la reserva desde el servicio de liquidación" }` |
| `503 Service Unavailable` | Módulo 1 no responde al chequeo de disponibilidad bajo lock | `{ "code": "SERVICIO_FLOTA_NO_DISPONIBLE", "message": "No se pudo verificar el estado de la flota en este momento" }` |
| `500 Internal Server Error` | Falla interna no controlada | `{ "code": "ERROR_INTERNO", "message": "Ocurrió un error inesperado al iniciar el pago" }` |

---

### Paginación

No aplica. Operación transaccional sobre un agregado de reserva individual.

---

### Seguridad y Perfiles

- Perfil autorizado: **Arrendatario**.
- **Regla estricta de pertenencia**: el backend valida que el `sub` del JWT coincida con el identificador del usuario que creó la reserva. Ningún usuario puede iniciar el pago de la reserva de otro Arrendatario.
- Regla "Sin dinero": el endpoint consume literalmente la respuesta de [`m3-calculo-total.md`](../external/m3-calculo-total.md) y asocia las cifras numéricas sin aplicar ningún porcentaje, recargo o redondeo en Módulo 2 (FR-004, SC-003).

---

### Notas Transversales

- **Concurrencia Atómica y Cero Sobreventa**: para satisfacer SC-002 y la regla First-Come First-Served (FCFS), la consulta de cálculo y la verificación en M1 se protegen mediante bloqueo transaccional (`SELECT ... FOR UPDATE` en PostgreSQL). Si la verificación detecta que la embarcación pasó a `Reservado`, la transacción aborta, la reserva permanece en `Iniciada` y se retorna `409 CONFLICTO_CONCURRENCIA`.
- **Inmutabilidad del Temporizador TTL**: el TTL de 15 minutos nació en `CU-02 Iniciar reserva`. Al transicionar a `Pendiente de Pago`, el backend **no reinicia** el contador (FR-007, SC-001); calcula los segundos remanentes (`ttl_remaining_seconds`) respecto al timestamp original `expires_at`.
- **Cobro por token (Mercado Pago)**: El frontend genera el `payment_token_ref` con el SDK de Mercado Pago y lo envía a este endpoint. Módulo 2 lo transporta sin interpretarlo en el evento `reservation.status.changed` con `status: PENDIENTE` (ver CU-14). Un worker de Módulo 3 ejecuta el cobro de forma asíncrona. Módulo 2 conoce el resultado consultando periódicamente a Módulo 3 (CU-13). No existe webhook de M3 hacia M2.
- **Reintento de pago con otra tarjeta**: Si el cobro fue `RECHAZADO` y el TTL sigue vigente, este mismo endpoint se puede invocar de nuevo sobre la reserva en Pendiente de Pago con un `payment_token_ref` nuevo. No se reinicia el TTL ni se vuelve a consultar el cálculo total (se reutiliza el ya asociado). M2 incrementa `attempt` y publica de nuevo el evento `PENDIENTE` con un `Message-Id` nuevo. `[PEDIR A M3: idempotency key de cobro con número de attempt]`.

---

### Trazabilidad FR/SC → Elemento del Contrato

| Requisito / Criterio | Descripción en Spec | Elemento de este Contrato |
|---|---|---|
| **FR-001** | Activación como extensión desde `Ver detalle de reserva` | Path parameter `reservation_id` y contexto de invocación |
| **FR-002** | Inicio de pago exclusivo desde estado `Iniciada` | Manejo de error `409 ESTADO_INVALIDO` |
| **FR-003** | Invocación subordinada a `Brindar cálculo total de la reserva` | Integración respaldada por [`m3-calculo-total.md`](../external/m3-calculo-total.md) |
| **FR-004** | Prohibición estricta de cálculo o alteración monetaria en M2 | Objeto `total_calculation` poblado literalmente |
| **FR-005** | Validación atómica bajo lock en M1 (CU-10) | Consulta respaldada por [`m1-consultar-estado-operativo.md`](../external/m1-consultar-estado-operativo.md) |
| **FR-006** | Transición de `Iniciada` a `Pendiente de Pago` vía CU-08 | Respuesta `200 OK` con `status: "Pendiente de Pago"` |
| **FR-007** | Prohibición de reiniciar el TTL de 15 minutos | Campo `ttl_remaining_seconds` calculado desde `expires_at` |
| **FR-008** | Rechazo por conflicto concurrente sin mutar reserva | Error `409 CONFLICTO_CONCURRENCIA` |
| **FR-009** |  Transferencia hacia el cobro de M3 | Objeto `pasarela` con `url_redireccion` y `token_cobro` |
| **FR-012** | Desglose oficial de M3 (alquiler, seguro, depósito, mensaje) | Objeto pago y evento PENDIENTE con payment_token_ref (CU-14) |
| **FR-013 / FR-014** | Obligatoriedad de aceptar política de cancelación | Campo de request `accepts_cancellation_policy: true` y error `400` |
| **SC-001** | 100% de transiciones exitosas sin reiniciar el TTL | Verificado en contrato de respuesta |
| **SC-002** | 0% sobreventas ante competencia concurrente | Garantizado por lock transaccional y FCFS |
| **SC-003** | Cero valores financieros calculados en Módulo 2 | Inmutabilidad de los datos financieros |
| **SC-004** | Confirmación explícita previa de política de cancelación | Validación estricta en el body del request |
