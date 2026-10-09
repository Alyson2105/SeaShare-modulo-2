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
POST /api/v1/reservas/{reservaId}/pago
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
| `reservaId` | string (UUID) | Sí | Identificador de la reserva en estado `Iniciada` (FR-001) |

**Query Parameters**: No tiene.

**Body (JSON)**:

```json
{
  "acepta_politica_cancelacion": true
}
```

*Validaciones de entrada (FR-013, FR-014)*:
- `acepta_politica_cancelacion`: booleano obligatorio. Debe ser estrictamente `true`. Si es `false` o no se envía, el backend bloquea el avance retornando `400 Bad Request`.

---

### Elementos de la Respuesta (Response)

**Código de estado HTTP (éxito)**: `200 OK`

**Contrato de respuesta tipado**:

```json
{
  "reserva_id": "string (UUID)",
  "estado": "string (valor literal: 'Pendiente de Pago')",
  "embarcacion_id": "string (UUID)",
  "calculo_total": {
    "calculo_id": "string (UUID emitido por Módulo 3)",
    "monto_total": "number (BigDecimal literal entregado por Módulo 3)",
    "moneda": "string (ej. COP)",
    "desglose": {
      "alquiler_base": "number (BigDecimal literal)",
      "seguro_nautico": "number (BigDecimal literal)",
      "deposito_garantia": "number (BigDecimal literal)"
    },
    "mensaje_garantia": "string (texto aclaratorio de M3)"
  },
  "ttl_segundos_restantes": "number (entero con el tiempo remanente del TTL)",
  "expira_en": "string (ISO 8601 timestamp con vencimiento original)",
  "pasarela": {
    "url_redireccion": "string (URL de cobro oficial de Módulo 3)",
    "token_cobro": "string (token o referencia segura de la transacción)"
  }
}
```

*(Módulo 2 transporta íntegramente los montos y el desglose recibido de Módulo 3 sin ejecutar sumas, divisiones ni deducciones de comisiones locales, FR-004, FR-012, SC-003).*

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
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMTIyMzMzNC00NDU1LTY2NzctODg5OS1hYWJiY2NkZGVlZmYiLCJyb2wiOiJBcnJlbmRhdGFyaW8iLCJpYXQiOjE3OTE1NDAwMDB9.sampleToken" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "acepta_politica_cancelacion": true
  }'
```

**Respuesta (`200 OK`)**:

```json
{
  "reserva_id": "e4f81c92-7a20-4215-9c5e-8812c3f1a001",
  "estado": "Pendiente de Pago",
  "embarcacion_id": "d3b07384-d113-49cd-a5d6-812e9bcfc101",
  "calculo_total": {
    "calculo_id": "f8a912bc-81d3-41bb-92cc-77aa12dd34ee",
    "monto_total": 10560000.00,
    "moneda": "COP",
    "desglose": {
      "alquiler_base": 9600000.00,
      "seguro_nautico": 160000.00,
      "deposito_garantia": 800000.00
    },
    "mensaje_garantia": "El depósito se reembolsa completo si el barco se devuelve sin daños"
  },
  "ttl_segundos_restantes": 645,
  "expira_en": "2026-10-09T12:20:00-05:00",
  "pasarela": {
    "url_redireccion": "https://pagos.seashare.internal/checkout/session/f8a912bc-81d3-41bb-92cc-77aa12dd34ee",
    "token_cobro": "chk_live_9a8b7c6d5e4f3a2b1c"
  }
}
```

---

### Manejo de Errores

| Código | Caso | Cuerpo de respuesta (ejemplo) |
|---|---|---|
| `400 Bad Request` | No se aceptó la política de cancelación (`acepta_politica_cancelacion: false`) o `reservaId` no es UUID (FR-014) | `{ "codigo": "POLITICA_NO_ACEPTADA", "mensaje": "Se requiere tu consentimiento explícito para las políticas de cancelación para iniciar el pago" }` |
| `401 Unauthorized` | Token ausente, inválido o expirado | `{ "codigo": "NO_AUTENTICADO", "mensaje": "Token de autenticación ausente o inválido" }` |
| `403 Forbidden` | El usuario autenticado no es el Arrendatario titular de la reserva | `{ "codigo": "RESERVA_NO_PERTENECE", "mensaje": "No tienes autorización para iniciar el pago de esta reserva" }` |
| `404 Not Found` | La reserva no existe en el sistema | `{ "codigo": "RESERVA_NO_ENCONTRADA", "mensaje": "La reserva especificada no existe" }` |
| `409 Conflict` (Estado Inválido) | La reserva no se encuentra en estado `Iniciada` (ej. ya está `Pendiente de Pago`, `Reservada`, `Cancelada`) (FR-002) | `{ "codigo": "ESTADO_INVALIDO", "mensaje": "Solo se puede iniciar el pago de reservas en estado Iniciada (estado actual: Pendiente de Pago)" }` |
| `409 Conflict` (TTL Expirado) | El temporizador TTL de 15 minutos venció antes de iniciar el pago (Edge Case TTL) | `{ "codigo": "RESERVA_EXPIRADA", "mensaje": "El tiempo límite de 15 minutos para iniciar el pago de esta reserva ha expirado" }` |
| `409 Conflict` (Carrera Concurrente FCFS) | Las fechas acaban de ser bloqueadas por otro usuario que pagó primero (FR-008, SC-002) | `{ "codigo": "CONFLICTO_CONCURRENCIA", "mensaje": "La embarcación ya no se encuentra disponible para las fechas seleccionadas debido a un pago concurrente" }` |
| `503 Service Unavailable` | Módulo 3 no responde o falla al entregar el cálculo definitivo (*fail-safe*, FR-003, FR-008) | `{ "codigo": "CALCULO_NO_DISPONIBLE", "mensaje": "No se pudo obtener el cálculo total de la reserva desde el servicio de liquidación" }` |
| `503 Service Unavailable` | Módulo 1 no responde al chequeo de disponibilidad bajo lock | `{ "codigo": "SERVICIO_FLOTA_NO_DISPONIBLE", "mensaje": "No se pudo verificar el estado de la flota en este momento" }` |
| `500 Internal Server Error` | Falla interna no controlada | `{ "codigo": "ERROR_INTERNO", "mensaje": "Ocurrió un error inesperado al iniciar el pago" }` |

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
- **Inmutabilidad del Temporizador TTL**: el TTL de 15 minutos nació en `CU-02 Iniciar reserva`. Al transicionar a `Pendiente de Pago`, el backend **no reinicia** el contador (FR-007, SC-001); calcula los segundos remanentes (`ttl_segundos_restantes`) respecto al timestamp original `expira_en`.
- **Integración con Pasarela de Pagos**: Módulo 2 transfiere el control a Módulo 3 entregando `pasarela.url_redireccion` y `token_cobro`. El cobro se ejecuta de forma asíncrona en la pasarela, y la confirmación final ingresará posteriormente a Módulo 2 mediante el webhook/endpoint de `CU-13 Confirmar pago`.

---

### Trazabilidad FR/SC → Elemento del Contrato

| Requisito / Criterio | Descripción en Spec | Elemento de este Contrato |
|---|---|---|
| **FR-001** | Activación como extensión desde `Ver detalle de reserva` | Path parameter `reservaId` y contexto de invocación |
| **FR-002** | Inicio de pago exclusivo desde estado `Iniciada` | Manejo de error `409 ESTADO_INVALIDO` |
| **FR-003** | Invocación subordinada a `Brindar cálculo total de la reserva` | Integración respaldada por [`m3-calculo-total.md`](../external/m3-calculo-total.md) |
| **FR-004** | Prohibición estricta de cálculo o alteración monetaria en M2 | Objeto `calculo_total` poblado literalmente |
| **FR-005** | Validación atómica bajo lock en M1 (CU-10) | Consulta respaldada por [`m1-consultar-estado-operativo.md`](../external/m1-consultar-estado-operativo.md) |
| **FR-006** | Transición de `Iniciada` a `Pendiente de Pago` vía CU-08 | Respuesta `200 OK` con `estado: "Pendiente de Pago"` |
| **FR-007** | Prohibición de reiniciar el TTL de 15 minutos | Campo `ttl_segundos_restantes` calculado desde `expira_en` |
| **FR-008** | Rechazo por conflicto concurrente sin mutar reserva | Error `409 CONFLICTO_CONCURRENCIA` |
| **FR-009** | Transferencia hacia interfaz de cobro de M3 | Objeto `pasarela` con `url_redireccion` y `token_cobro` |
| **FR-012** | Desglose oficial de M3 (alquiler, seguro, depósito, mensaje) | Objeto `calculo_total.desglose` y `mensaje_garantia` |
| **FR-013 / FR-014** | Obligatoriedad de aceptar política de cancelación | Campo de request `acepta_politica_cancelacion: true` y error `400` |
| **SC-001** | 100% de transiciones exitosas sin reiniciar el TTL | Verificado en contrato de respuesta |
| **SC-002** | 0% sobreventas ante competencia concurrente | Garantizado por lock transaccional y FCFS |
| **SC-003** | Cero valores financieros calculados en Módulo 2 | Inmutabilidad de los datos financieros |
| **SC-004** | Confirmación explícita previa de política de cancelación | Validación estricta en el body del request |
