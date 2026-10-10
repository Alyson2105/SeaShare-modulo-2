# Contrato de Interfaz REST: CU-15 Solicitar Información de Reserva

## 1. Identificación y Metadatos

- **Identificador del Contrato**: `REST-M2-CU15-SOLICITAR-INFORMACION-RESERVA`
- **Módulo Responsable**: Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones
- **Tipo de Interfaz**: REST sincrónico expuesto por Módulo 2 hacia Módulo 3 (M2M / Service-to-Service)
- **Actor / Consumidor Autorizado**: Módulo 3 ("el sistema" / Motor de Liquidación, Seguros y Dispersión de Fondos)
- **Caso de Uso Base / Relaciones**: 
  - Caso de uso: `CU-15 Solicitar información de la reserva` (Consulta síncrona de solo lectura)
  - Interacciones: Corresponde a la operación *"Brindar información de reserva"* descrita en la tabla §5 de `consistencia-m2-m3.md`.
- **Versión del Contrato**: 1.0.0
- **Fecha de Creación**: 2026-10-09

---

## 2. Propósito y Alcance del Endpoint

Este endpoint de lectura síncrona de alto rendimiento permite a Módulo 3 obtener en tiempo real los hechos operativos oficiales y el estado vigente de una reserva para sustentar sus procesos financieros de cobro, activación de pólizas de seguro, custodia de garantías, dispersión de fondos y cálculo autónomo de reembolsos o penalidades.

### Responsabilidades del Endpoint:
1. **Suministro de Hechos Operativos Fidedignos (`FR-001`, `FR-002`)**:
   - Entrega los identificadores clave de la transacción (`reservation_id`, `renter_id`, `vessel_id`, `owner_id`), fechas/horas pactadas de zarpe y desembarque, cantidad de pasajeros y estado principal actual.
   - Provee la referencia original de la cotización vinculante (`original_quotation_reference`).
2. **Exposición de Metadatos Específicos por Estado**:
   - **En `Pendiente de Pago` (`FR-005`)**: Entrega la marca de tiempo exacta de expiración del temporizador TTL de 15 minutos (`expires_at`) y los segundos restantes, permitiendo a Módulo 3 validar que la autorización bancaria o cobro no se ejecute fuera de la ventana hábil.
   - **En `Completada` (`FR-003`)**: Entrega la fecha y hora real de check-out en muelle, la bandera de detección de daños (`damage_detected: boolean`) y el texto literal de observaciones o novedades reportadas por el Propietario (`reported_remarks`), insumo fundamental para que Módulo 3 determine la custodia o liberación del depósito de garantía.
   - **En `Cancelada` (`FR-004`)**: Entrega el sub-estado clasificado (`Flexible`, `Moderado`, `Tardío`, `Por Propietario`, `Por Inasistencia`), el actor que detonó la cancelación (`cancellation_actor`) y las horas exactas de anticipación calculadas (`cancellation_anticipation_hours`), delegando en Módulo 3 el cálculo aritmético del reembolso.
3. **Regla Estricta "Sin Dinero" (Zero Financial Calculations, `FR-006`, `SC-003`)**:
   - Módulo 2 **no calcula penalidades monetarias, no tasa económicamente los daños, no liquida porcentajes de comisión ni deduce reembolsos**.
   - Módulo 2 expone exclusivamente magnitudes físicas y temporales (horas, fechas, estados, textos descriptivos). Toda la valoración económica y dispersión bancaria pertenece en forma exclusiva a Módulo 3.
4. **Idempotencia Absoluta y Cero Efectos Secundarios (`FR-007`, `SC-002`)**:
   - La consulta no muta la base de datos de Módulo 2, no altera la máquina de estados, no reinicia contadores TTL, no invoca a Módulo 1 y no emite eventos hacia RabbitMQ. Módulo 3 puede consultarlo de forma recurrente sin alterar el ciclo de vida del alquiler.
5. **Alto Rendimiento y Baja Latencia (`SC-001`)**:
   - Responde de forma síncrona en un tiempo inferior a **200 milisegundos** bajo condiciones normales de carga.

---

## 3. Definición del Endpoint

- **Método HTTP**: `GET`
- **Ruta Oficial**: `/api/v1/internal/reservas/{reservation_id}`
- **Formato de Petición / Respuesta**: `application/json`
- **Codificación**: `UTF-8`

### 3.1 Encabezados HTTP (Headers)

| Header | Tipo | Obligatorio | Descripción |
| :--- | :--- | :--- | :--- |
| `Authorization` | String | Sí | Token Bearer JWT firmado con identidad de servicio de Módulo 3 (`service: modulo-3`). |
| `Accept` | String | Sí | Debe ser `application/json`. |
| `X-Correlation-Id` | String (UUID) | Opcional | Identificador de correlación para observabilidad distribuida entre M3 y M2. |

### 3.2 Parámetros de Ruta (Path Parameters)

| Parámetro | Tipo | Restricción | Descripción |
| :--- | :--- | :--- | :--- |
| `reservation_id` | String (UUIDv4) | Obligatorio | Identificador universal único de la reserva a consultar. |

---

## 4. Estructura de Datos (Schema JSON)

### 4.1 Cuerpo de Respuesta Exitosa (`200 OK`)

```json
{
  "reservation_id": "c1f7a8b2-5e4d-4c3b-8a1e-9f0a2b3c4d5e",
  "reservation_code": "#RS-4492",
  "renter_id": "u1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
  "vessel_id": "b3e2a1d0-4f5c-6b7a-8e9f-0a1b2c3d4e5f",
  "owner_id": "p9a8b7c6-d5e4-3f2a-1b0c-9d8e7f6a5b4c",
  "dates": {
    "scheduled_departure": "2026-11-20T09:00:00-05:00",
    "scheduled_arrival": "2026-11-22T18:00:00-05:00",
    "timezone": "America/Bogota",
    "duration_days": 3,
    "duration_nights": 2
  },
  "passengers": 4,
  "primary_status": "Reservada",
  "cancellation_sub_status": null,
  "cancellation_actor": null,
  "cancellation_anticipation_hours": null,
  "checkin_checkout": {
    "checkin_at": null,
    "checkout_at": null,
    "reported_remarks": null,
    "damage_detected": false
  },
  "ttl_timer": {
    "applies": false,
    "expires_at": null,
    "remaining_seconds": 0
  },
  "original_quotation_reference": "f8e7d6c5-b4a3-2c1d-0e9f-8a7b6c5d4e3f",
  "created_at": "2026-10-09T10:00:00-05:00",
  "updated_at": "2026-10-09T10:05:00-05:00"
}
```

#### Descripción de Campos de Salida

| Campo | Tipo | Nulabilidad | Descripción |
| :--- | :--- | :--- | :--- |
| `reservation_id` | String (UUID) | No nulo | Identificador universal único de la reserva. |
| `reservation_code` | String | No nulo | Código de negocio (ej. `"#RS-4492"`). |
| `renter_id` | String (UUID) | No nulo | Identificador del arrendatario titular en la plataforma. |
| `vessel_id` | String (UUID) | No nulo | Identificador de la embarcación en Módulo 1. |
| `owner_id` | String (UUID) | No nulo | Identificador del propietario de la embarcación. |
| `dates` | Objeto | No nulo | Bloque de itinerario temporal pactado y huso horario del puerto. |
| `dates.scheduled_departure` | String (ISO 8601) | No nulo | Fecha y hora pactada para el inicio del servicio con offset local. |
| `dates.scheduled_arrival` | String (ISO 8601) | No nulo | Fecha y hora pactada para el desembarque con offset local. |
| `dates.timezone` | String | No nulo | Zona horaria oficial del puerto de atraque (`America/Bogota`). |
| `dates.duration_days` | Entero | No nulo | Cantidad total de días del alquiler. |
| `dates.duration_nights`| Entero | No nulo | Cantidad de noches contempladas en la reserva. |
| `passengers` | Entero | No nulo | Ocupantes autorizados para el viaje náutico. |
| `primary_status` | String (Enum) | No nulo | Estado vigente en Módulo 2 (`Iniciada`, `Pendiente de Pago`, `Reservada`, `En Navegación`, `Completada`, `Cancelada`, `Expirada`, `Pago Fallido`). |
| `cancellation_sub_status`| String (Enum) | Nulo condicional | Sub-estado si `primary_status == Cancelada`: `"Flexible"`, `"Moderado"`, `"Tardío"`, `"Por Propietario"`, `"Por Inasistencia"`. |
| `cancellation_actor` | String (Enum) | Nulo condicional | Actor que originó la cancelación: `"Arrendatario"`, `"Propietario"`, `"Sistema_TTL"`, `"Sistema_NoShow"`. |
| `cancellation_anticipation_hours` | Number (Float) | Nulo condicional | Horas exactas con decimales de anticipación evaluadas al momento de cancelar. |
| `checkin_checkout` | Objeto | No nulo | Datos de operación física en muelle. |
| `checkin_checkout.checkin_at` | String (ISO 8601) | Nulo condicional | Timestamp del zarpe real confirmado por el Propietario. |
| `checkin_checkout.checkout_at`| String (ISO 8601) | Nulo condicional | Timestamp del desembarque real confirmado por el Propietario. |
| `checkin_checkout.reported_remarks` | String | Nulo condicional | Texto literal de observaciones o daños ingresado por el Propietario al finalizar. |
| `checkin_checkout.damage_detected` | Boolean | No nulo | Indicador booleano reportado en el checkout sobre novedades de averías. |
| `ttl_timer` | Objeto | No nulo | Estado del temporizador de 15 minutos de reserva. |
| `ttl_timer.applies` | Boolean | No nulo | Verdadero si la reserva está sujeta a ventana TTL (`Iniciada` o `Pendiente de Pago`). |
| `ttl_timer.expires_at` | String (ISO 8601) | Nulo condicional | Marca de tiempo exacta en que expira la reserva si no se confirma el pago. |
| `ttl_timer.remaining_seconds` | Entero | No nulo | Segundos restantes de la ventana de pago al momento de procesar este GET. |
| `original_quotation_reference` | String (UUID) | No nulo | Identificador de cotización preliminar emitido previamente por Módulo 3. |
| `created_at` | String (ISO 8601) | No nulo | Timestamp de creación inicial de la reserva. |
| `updated_at` | String (ISO 8601) | No nulo | Timestamp de la última mutación confirmada en la reserva. |

---

## 5. Ejemplos de Petición y Respuesta

### Ejemplo 1: Consulta de reserva en estado `Pendiente de Pago` (Validación de ventana TTL para cobro)

#### Petición HTTP (`curl`)
```bash
curl -X GET "https://api.seashare.com/api/v1/internal/reservas/c1f7a8b2-5e4d-4c3b-8a1e-9f0a2b3c4d5e" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Accept: application/json" \
  -H "X-Correlation-Id: e1111111-2222-3333-4444-555555555555"
```

#### Respuesta Exitosa (`200 OK`)
```json
{
  "reservation_id": "c1f7a8b2-5e4d-4c3b-8a1e-9f0a2b3c4d5e",
  "reservation_code": "#RS-4492",
  "renter_id": "u1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
  "vessel_id": "b3e2a1d0-4f5c-6b7a-8e9f-0a1b2c3d4e5f",
  "owner_id": "p9a8b7c6-d5e4-3f2a-1b0c-9d8e7f6a5b4c",
  "dates": {
    "scheduled_departure": "2026-11-20T09:00:00-05:00",
    "scheduled_arrival": "2026-11-22T18:00:00-05:00",
    "timezone": "America/Bogota",
    "duration_days": 3,
    "duration_nights": 2
  },
  "passengers": 4,
  "primary_status": "Pendiente de Pago",
  "cancellation_sub_status": null,
  "cancellation_actor": null,
  "cancellation_anticipation_hours": null,
  "checkin_checkout": {
    "checkin_at": null,
    "checkout_at": null,
    "reported_remarks": null,
    "damage_detected": false
  },
  "ttl_timer": {
    "applies": true,
    "expires_at": "2026-10-09T10:15:00-05:00",
    "remaining_seconds": 540
  },
  "original_quotation_reference": "f8e7d6c5-b4a3-2c1d-0e9f-8a7b6c5d4e3f",
  "created_at": "2026-10-09T10:00:00-05:00",
  "updated_at": "2026-10-09T10:06:00-05:00"
}
```

---

### Ejemplo 2: Consulta de reserva en estado `Completada` con novedades (Liquidación de garantía)

#### Petición HTTP (`curl`)
```bash
curl -X GET "https://api.seashare.com/api/v1/internal/reservas/f5a6b7c8-9d0e-1f2a-3b4c-5d6e7f8a9b0c" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Accept: application/json"
```

#### Respuesta Exitosa (`200 OK`)
```json
{
  "reservation_id": "f5a6b7c8-9d0e-1f2a-3b4c-5d6e7f8a9b0c",
  "reservation_code": "#RS-2950",
  "renter_id": "u1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
  "vessel_id": "b3e2a1d0-4f5c-6b7a-8e9f-0a1b2c3d4e5f",
  "owner_id": "p9a8b7c6-d5e4-3f2a-1b0c-9d8e7f6a5b4c",
  "dates": {
    "scheduled_departure": "2026-09-10T10:00:00-05:00",
    "scheduled_arrival": "2026-09-11T16:00:00-05:00",
    "timezone": "America/Bogota",
    "duration_days": 2,
    "duration_nights": 1
  },
  "passengers": 4,
  "primary_status": "Completada",
  "cancellation_sub_status": null,
  "cancellation_actor": null,
  "cancellation_anticipation_hours": null,
  "checkin_checkout": {
    "checkin_at": "2026-09-10T10:12:00-05:00",
    "checkout_at": "2026-09-11T15:50:30-05:00",
    "reported_remarks": "Se detectó rotura en la baranda de babor durante el desembarque y pérdida de un chaleco salvavidas.",
    "damage_detected": true
  },
  "ttl_timer": {
    "applies": false,
    "expires_at": null,
    "remaining_seconds": 0
  },
  "original_quotation_reference": "f8e7d6c5-b4a3-2c1d-0e9f-8a7b6c5d4e3f",
  "created_at": "2026-09-02T11:00:00-05:00",
  "updated_at": "2026-09-11T15:50:30-05:00"
}
```

---

### Ejemplo 3: Consulta de reserva en estado `Cancelada` con sub-estado `Moderado` (Cálculo de reembolso)

#### Petición HTTP (`curl`)
```bash
curl -X GET "https://api.seashare.com/api/v1/internal/reservas/d2e3f4a5-6b7c-8d9e-0f1a-2b3c4d5e6f7a" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Accept: application/json"
```

#### Respuesta Exitosa (`200 OK`)
```json
{
  "reservation_id": "d2e3f4a5-6b7c-8d9e-0f1a-2b3c4d5e6f7a",
  "reservation_code": "#RS-3810",
  "renter_id": "u1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
  "vessel_id": "e4f5a6b7-8c9d-0e1f-2a3b-4c5d6e7f8a9b",
  "owner_id": "p8b7a6c5-d4e3-2f1a-0b9c-8d7e6f5a4b3c",
  "dates": {
    "scheduled_departure": "2026-10-15T08:00:00-05:00",
    "scheduled_arrival": "2026-10-15T17:00:00-05:00",
    "timezone": "America/Bogota",
    "duration_days": 1,
    "duration_nights": 0
  },
  "passengers": 6,
  "primary_status": "Cancelada",
  "cancellation_sub_status": "Moderado",
  "cancellation_actor": "Arrendatario",
  "cancellation_anticipation_hours": 36.5,
  "checkin_checkout": {
    "checkin_at": null,
    "checkout_at": null,
    "reported_remarks": null,
    "damage_detected": false
  },
  "ttl_timer": {
    "applies": false,
    "expires_at": null,
    "remaining_seconds": 0
  },
  "original_quotation_reference": "f8e7d6c5-b4a3-2c1d-0e9f-8a7b6c5d4e3f",
  "created_at": "2026-10-01T14:20:00-05:00",
  "updated_at": "2026-10-13T19:30:00-05:00"
}
```

---

## 6. Respuestas de Error y Códigos HTTP

Todos los errores retornan un sobre uniforme con `code` y `message`:

```json
{
  "code": "CODIGO_ERROR",
  "message": "Descripción detallada del motivo de rechazo."
}
```

| Código HTTP | Código Interno (`code`) | Causa / Condición de Disparo | Cuerpo de Respuesta de Ejemplo |
| :--- | :--- | :--- | :--- |
| **`400 Bad Request`** | `INVALID_UUID` | El parámetro `reservation_id` no cumple el estándar UUIDv4. | `{"code": "INVALID_UUID", "message": "El identificador de reserva proporcionado no tiene un formato UUID válido."}` |
| **`401 Unauthorized`** | `AUTH_TOKEN_MISSING_OR_INVALID` | El header `Authorization` no fue provisto o el token de servicio es inválido. | `{"code": "AUTH_TOKEN_MISSING_OR_INVALID", "message": "Token de autenticación de servicio ausente o inválido."}` |
| **`403 Forbidden`** | `FORBIDDEN_NOT_SERVICE_M3` | El token JWT no contiene la identidad de servicio autorizada de Módulo 3. Petición rechazada para usuarios estándar. | `{"code": "FORBIDDEN_NOT_SERVICE_M3", "message": "Acceso denegado: este endpoint interno está reservado exclusivamente para el consumo del motor financiero de Módulo 3."}` |
| **`404 Not Found`** | `RESERVATION_NOT_FOUND` | La reserva no existe en la base de datos de Módulo 2. | `{"code": "RESERVATION_NOT_FOUND", "message": "No se encontró ninguna reserva asociada al identificador proporcionado."}` |
| **`500 Internal Server Error`** | `INTERNAL_SERVER_ERROR` | Fallo inesperado en el servidor al recuperar la entidad. | `{"code": "INTERNAL_SERVER_ERROR", "message": "Error interno del servidor al procesar la consulta operativa de la reserva."}` |

---

## 7. Notas Transversales y Reglas de Negocio Arquitecturales

### 7.1 Regla de Oro "Sin Dinero" (Zero Financial Logic)
En cumplimiento estricto de `FR-006` y `SC-003`, este contrato no calcula ningún monto ni deriva operaciones contables. Módulo 2 actúa como el testigo fáctico del viaje náutico:
- Módulo 2 reporta: *"Cancelada con 36.5 horas de anticipación por el Arrendatario (sub-estado Moderado)"*.
- Módulo 3 decide y calcula de forma autónoma: *"Corresponde reembolso del 50% al arrendatario y dispersión del 50% al anfitrión sobre el monto X que tengo registrado"*.

### 7.2 Inmutabilidad y Cero Efectos Secundarios (Read-Only Safety)
Este endpoint garantiza no poseer efectos colaterales (`SC-002`, `FR-007`). No muta registros, no reinicia temporizadores TTL de 15 minutos ni compite contra el job scheduler de barrido de la base de datos.

### 7.3 Latencia Ultra-Baja (< 200 ms)
La consulta resuelve mediante búsqueda directa por clave primaria en PostgreSQL (`SELECT ... FROM reserva WHERE id = :id`), garantizando tiempos de respuesta inferiores a 20 ms a nivel de base de datos y cumpliendo holgadamente el SLA de 200 ms pactado en `SC-001`.

---

## 8. Trazabilidad de Requisitos

| Requisito Funcional / Criterio | Descripción en Spec | Cobertura en este Contrato |
| :--- | :--- | :--- |
| **FR-001** | Exponer un endpoint de lectura síncrona dedicado a proveer información a Módulo 3. | Definido en `/api/v1/internal/reservas/{reservation_id}`. |
| **FR-002** | Retornar payload estructurado con IDs (reserva, arrendatario, embarcación), fechas/horas, pasajeros, estado principal y cotización original. | Campos `reservation_id`, `renter_id`, `vessel_id`, `dates`, `passengers`, `primary_status`, `original_quotation_reference`. |
| **FR-003** | En `Completada`, incluir fecha/hora real de check-out y texto de observaciones/daños si existe. | Campos `checkin_checkout.checkout_at`, `reported_remarks` y `damage_detected`. |
| **FR-004** | En `Cancelada`, incluir sub-estado, actor y horas exactas de anticipación calculadas. | Campos `cancellation_sub_status`, `cancellation_actor` y `cancellation_anticipation_hours`. |
| **FR-005** | En `Pendiente de Pago`, incluir marca de tiempo de expiración del TTL (15 min) y segundos restantes. | Bloque `ttl_timer` (`expires_at`, `remaining_seconds`). |
| **FR-006** | REGLA ESTRICTA: Cero cálculos de penalidades, reembolsos o tasación económica de daños. | Verificado en schema. No contiene montos derivados ni deducciones. |
| **FR-007** | Garantizar que no genera escrituras en BD ni llamadas hacia Módulo 1. | Endpoint idempotente de solo lectura documentado en sección 2 y 7.2. |
| **SC-001** | 100% de consultas resueltas y entregadas en menos de 200 milisegundos. | Arquitectura optimizada sobre clave primaria en BD documentada en 7.3. |
| **SC-002** | Cero (0%) alteraciones o mutaciones de estado en base de datos. | Naturaleza puramente síncrona de lectura sin transacciones de escritura. |
| **SC-003** | Cero (0) valores financieros o monetarios calculados internamente por Módulo 2. | Total cumplimiento de la regla "Sin dinero". |
