# Contrato de Interfaz REST: CU-04 Solicitar Cancelación

## 1. Identificación y Metadatos

- **Identificador del Contrato**: `REST-M2-CU04-SOLICITAR-CANCELACION`
- **Módulo Responsable**: Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones
- **Tipo de Interfaz**: REST sincrónico expuesto por Módulo 2
- **Actor / Consumidor Autorizado**: 
  - `ARRENDATARIO` (únicamente el titular de la reserva)
  - `PROPIETARIO` (únicamente el dueño registrado de la embarcación asociada a la reserva)
- **Caso de Uso Base / Relaciones**: 
  - Extiende a: `Ver detalle de reserva` (`<<extend>>` - CU-21)
  - Incluye: `Actualizar estado reserva` (`<<include>>` - CU-08)
  - Incluye: `Proveer información de embarcación` (`<<include>>` - CU-09 de Módulo 1)
- **Versión del Contrato**: 1.0.0
- **Fecha de Creación**: 2026-10-09

---

## 2. Propósito y Alcance del Endpoint

Este endpoint procesa la solicitud voluntaria de cancelación de una reserva antes del zarpe o inicio formal de la navegación.

### Responsabilidades del Endpoint:
1. **Validación de Identidad y Legitimidad**: Verifica mediante el token JWT que el solicitante sea unívocamente el arrendatario titular o el propietario registrado de la embarcación. Cualquier otro usuario recibe `403 Forbidden`.
2. **Validación de Estado Operativo**: Permite la cancelación **única y exclusivamente** si la reserva se encuentra en estado principal `Reservada`.
   - Si la reserva está en `Iniciada` o `Pendiente de Pago`, la petición es rechazada con `409 Conflict`, indicando que dichas reservas no admiten cancelación activa y deben expirar pasivamente por vencimiento del temporizador TTL.
   - Si la reserva está en `En Navegación`, `Completada`, `Expirada`, `Pago Fallido` o ya fue `Cancelada`, se rechaza con `409 Conflict`.
3. **Resolución de Zona Horaria Oficial**: Consulta a Módulo 1 (`CU-09`) el puerto de atraque para obtener la zona horaria oficial del activo (`America/Bogota`, etc.). Si la consulta a Módulo 1 falla o no responde, el sistema **no asume una zona horaria por defecto** y rechaza la operación con `503 Service Unavailable` como fail-safe de protección de derechos.
4. **Cálculo de Anticipación y Clasificación Contractual (Arrendatario)**:
   - Computa la diferencia exacta en horas y minutos entre el instante de la solicitud y la fecha/hora pactada de inicio de la reserva, bajo la zona horaria del puerto.
   - Aplica las reglas contractuales temporales:
     - **Flexible**: Anticipación $\ge 72$ horas (`anticipacion >= 72h`). En el límite exacto de 72h:00m:00s es Flexible (a favor del cliente).
     - **Moderado**: Anticipación entre 24 y 72 horas (`24h <= anticipacion < 72h`). En el límite exacto de 24h:00m:00s es Moderado.
     - **Tardío**: Anticipación estrictamente menor a 24 horas (`anticipacion < 24h`). Se permite cancelar hasta el último minuto previo al zarpe bajo esta franja.
   - Determina el nuevo estado operativo de la embarcación en Módulo 1 como `Disponible`.
5. **Clasificación Directa y Causal (Propietario)**:
   - Asigna directamente la clasificación **Por Propietario**, sin evaluar el tiempo de anticipación ni franjas horarias.
   - Exige obligatoriamente seleccionar el motivo:
     - `fuerza_mayor_logistica`: La embarcación se libera en Módulo 1 a estado `Disponible`.
     - `averia_mecanica`: La embarcación pasa en Módulo 1 a estado `En Mantenimiento/Limpieza`, inhabilitando su oferta comercial.
6. **Transición Formal y Publicación de Eventos**: Invoca internamente a `Actualizar estado reserva` (`CU-08`), actualiza síncronamente el estado operativo del activo en Módulo 1 (`PUT /api/v1/embarcaciones/{vessel_id}/estado-operativo`) y emite el evento de dominio AMQP hacia Módulo 3 (`reserva.estado.cancelada`) para la dispersión y reembolso de fondos.
7. **Regla Estricta "Sin Dinero"**: Módulo 2 **no calcula importes de devolución, porcentajes de retención ni montos de penalidad monetaria**. Toda la valoración económica y liquidación corresponde a Módulo 3.

---

## 3. Definición del Endpoint

- **Método HTTP**: `POST`
- **Ruta**: `/api/v1/reservas/{reservation_id}/cancelacion`
- **Formato de Petición / Respuesta**: `application/json`
- **Codificación**: `UTF-8`

### 3.1 Encabezados HTTP (Headers)

| Header | Tipo | Obligatorio | Descripción |
| :--- | :--- | :--- | :--- |
| `Authorization` | String | Sí | Token Bearer JWT del usuario autenticado (`Bearer eyJhbG...`). |
| `Content-Type` | String | Sí | Debe ser `application/json; charset=utf-8`. |
| `X-Idempotency-Key` | String (UUID) | Opcional | Clave para evitar doble procesamiento accidental por doble clic del cliente. |

### 3.2 Parámetros de Ruta (Path Parameters)

| Parámetro | Tipo | Restricción | Descripción |
| :--- | :--- | :--- | :--- |
| `reservation_id` | String (UUIDv4) | Obligatorio | Identificador universal único de la reserva a cancelar. |

---

## 4. Estructura de Datos (Schemas JSON)

### 4.1 Cuerpo de la Petición (Request Body)

```json
{
  "reason": "fuerza_mayor_logistica | averia_mecanica | null",
  "justification": "string (opcional, máximo 500 caracteres)"
}
```

#### Descripción de Campos de Entrada

| Campo | Tipo | Obligatoriedad | Descripción / Reglas |
| :--- | :--- | :--- | :--- |
| `reason` | String (Enum) | Condicional | **Obligatorio si el solicitante es Propietario**. Opcional/ignorado si es Arrendatario. Valores permitidos: `"fuerza_mayor_logistica"`, `"averia_mecanica"`. |
| `justification` | String | Opcional | Texto libre descriptivo de hasta 500 caracteres que detalla el contexto o causa de la cancelación. |

### 4.2 Cuerpo de Respuesta Exitosa (`200 OK`)

```json
{
  "reservation_id": "c1f7a8b2-5e4d-4c3b-8a1e-9f0a2b3c4d5e",
  "status": "Cancelada",
  "sub_status": "Flexible | Moderado | Tardío | Por Propietario",
  "requesting_actor": "Arrendatario | Propietario",
  "anticipation_hours": 74.5,
  "port_timezone": "America/Bogota",
  "scheduled_departure": "2026-11-20T09:00:00-05:00",
  "cancelled_at": "2026-11-17T06:30:00-05:00",
  "reason": "fuerza_mayor_logistica | averia_mecanica | null",
  "justification": "string | null",
  "vessel_id": "b3e2a1d0-4f5c-6b7a-8e9f-0a1b2c3d4e5f",
  "vessel_new_operational_status": "Disponible | En Mantenimiento/Limpieza",
  "m1_sync_status": "CONFIRMADA",
  "m3_notification_status": "PUBLICADA",
  "message": "La reserva ha sido cancelada exitosamente bajo la política clasificada. Módulo 3 gestionará la liquidación financiera correspondiente."
}
```

#### Descripción de Campos de Salida

| Campo | Tipo | Nulabilidad | Descripción |
| :--- | :--- | :--- | :--- |
| `reservation_id` | String (UUID) | No nulo | Identificador de la reserva cancelada. |
| `status` | String | No nulo | Estado terminal asignado. Siempre `"Cancelada"`. |
| `sub_status` | String (Enum) | No nulo | Clasificación de la política: `"Flexible"`, `"Moderado"`, `"Tardío"` o `"Por Propietario"`. |
| `requesting_actor` | String (Enum) | No nulo | Rol del usuario que detonó la acción: `"Arrendatario"` o `"Propietario"`. |
| `anticipation_hours` | Number (Float) | Nulo condicional | Horas exactas con decimales de anticipación respecto al zarpe. Nulo si el actor fue el Propietario. |
| `port_timezone` | String | No nulo | Identificador IANA de huso horario oficial del puerto de atraque (`America/Bogota`). |
| `scheduled_departure` | String (ISO 8601) | No nulo | Fecha y hora programada de zarpe con offset del puerto local. |
| `cancelled_at` | String (ISO 8601) | No nulo | Marca de tiempo oficial de recepción de la solicitud de cancelación con offset local. |
| `reason` | String | Nulo condicional | Causal seleccionada (obligatoria en Propietario, nula si Arrendatario no la aportó). |
| `justification` | String | Nulo condicional | Texto libre descriptivo registrado para auditoría. |
| `vessel_id` | String (UUID) | No nulo | Identificador de la embarcación asociada. |
| `vessel_new_operational_status` | String (Enum) | No nulo | Estado asignado al activo en Módulo 1: `"Disponible"` o `"En Mantenimiento/Limpieza"`. |
| `m1_sync_status` | String | No nulo | Estado de la sincronización síncrona con M1: `"CONFIRMADA"` o `"FALLIDA_CIRCUIT_BREAKER"`. |
| `m3_notification_status` | String | No nulo | Estado de emisión del evento AMQP a Módulo 3: `"PUBLICADA"`. |
| `message` | String | No nulo | Resumen textual informativo confirmatorio. |

---

## 5. Ejemplos de Petición y Respuesta

### Ejemplo 1: Cancelación por Arrendatario — Política Flexible (> 72 horas de anticipación)

#### Petición HTTP (`curl`)
```bash
curl -X POST "https://api.seashare.com/api/v1/reservas/c1f7a8b2-5e4d-4c3b-8a1e-9f0a2b3c4d5e/cancelacion" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -H "X-Idempotency-Key: a1111111-2222-3333-4444-555555555555" \
  -d '{
    "justification": "Cambio de planes familiares de vacaciones."
  }'
```

#### Respuesta Exitosa (`200 OK`)
```json
{
  "reservation_id": "c1f7a8b2-5e4d-4c3b-8a1e-9f0a2b3c4d5e",
  "status": "Cancelada",
  "sub_status": "Flexible",
  "requesting_actor": "Arrendatario",
  "anticipation_hours": 85.5,
  "port_timezone": "America/Bogota",
  "scheduled_departure": "2026-11-20T09:00:00-05:00",
  "cancelled_at": "2026-11-16T19:30:00-05:00",
  "reason": null,
  "justification": "Cambio de planes familiares de vacaciones.",
  "vessel_id": "b3e2a1d0-4f5c-6b7a-8e9f-0a1b2c3d4e5f",
  "vessel_new_operational_status": "Disponible",
  "m1_sync_status": "CONFIRMADA",
  "m3_notification_status": "PUBLICADA",
  "message": "Reserva cancelada con más de 72 horas de anticipación. Clasificación Flexible aplicada. El activo náutico ha quedado Disponible."
}
```

---

### Ejemplo 2: Cancelación por Arrendatario — Política Moderada (entre 24 y 72 horas)

#### Petición HTTP (`curl`)
```bash
curl -X POST "https://api.seashare.com/api/v1/reservas/c1f7a8b2-5e4d-4c3b-8a1e-9f0a2b3c4d5e/cancelacion" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "justification": "Imprevisto laboral no postergable."
  }'
```

#### Respuesta Exitosa (`200 OK`)
```json
{
  "reservation_id": "c1f7a8b2-5e4d-4c3b-8a1e-9f0a2b3c4d5e",
  "status": "Cancelada",
  "sub_status": "Moderado",
  "requesting_actor": "Arrendatario",
  "anticipation_hours": 36.0,
  "port_timezone": "America/Bogota",
  "scheduled_departure": "2026-11-20T09:00:00-05:00",
  "cancelled_at": "2026-11-18T21:00:00-05:00",
  "reason": null,
  "justification": "Imprevisto laboral no postergable.",
  "vessel_id": "b3e2a1d0-4f5c-6b7a-8e9f-0a1b2c3d4e5f",
  "vessel_new_operational_status": "Disponible",
  "m1_sync_status": "CONFIRMADA",
  "m3_notification_status": "PUBLICADA",
  "message": "Reserva cancelada con 36 horas de anticipación. Clasificación Moderado aplicada. El activo náutico ha quedado Disponible."
}
```

---

### Ejemplo 3: Cancelación por Arrendatario — Política Tardía (< 24 horas antes del zarpe)

#### Petición HTTP (`curl`)
```bash
curl -X POST "https://api.seashare.com/api/v1/reservas/c1f7a8b2-5e4d-4c3b-8a1e-9f0a2b3c4d5e/cancelacion" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "justification": "No podremos llegar a la ciudad a tiempo para la salida de mañana."
  }'
```

#### Respuesta Exitosa (`200 OK`)
```json
{
  "reservation_id": "c1f7a8b2-5e4d-4c3b-8a1e-9f0a2b3c4d5e",
  "status": "Cancelada",
  "sub_status": "Tardío",
  "requesting_actor": "Arrendatario",
  "anticipation_hours": 8.5,
  "port_timezone": "America/Bogota",
  "scheduled_departure": "2026-11-20T09:00:00-05:00",
  "cancelled_at": "2026-11-20T00:30:00-05:00",
  "reason": null,
  "justification": "No podremos llegar a la ciudad a tiempo para la salida de mañana.",
  "vessel_id": "b3e2a1d0-4f5c-6b7a-8e9f-0a1b2c3d4e5f",
  "vessel_new_operational_status": "Disponible",
  "m1_sync_status": "CONFIRMADA",
  "m3_notification_status": "PUBLICADA",
  "message": "Reserva cancelada con menos de 24 horas de anticipación. Clasificación Tardío aplicada. Módulo 3 tramitará la liquidación sin reembolso al arrendatario."
}
```

---

### Ejemplo 4: Cancelación por Propietario — Causal Fuerza Mayor Logística

#### Petición HTTP (`curl`)
```bash
curl -X POST "https://api.seashare.com/api/v1/reservas/c1f7a8b2-5e4d-4c3b-8a1e-9f0a2b3c4d5e/cancelacion" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "reason": "fuerza_mayor_logistica",
    "justification": "Capitán asignado con incapacidad médica de urgencia. No se cuenta con relevo certificado para la fecha."
  }'
```

#### Respuesta Exitosa (`200 OK`)
```json
{
  "reservation_id": "c1f7a8b2-5e4d-4c3b-8a1e-9f0a2b3c4d5e",
  "status": "Cancelada",
  "sub_status": "Por Propietario",
  "requesting_actor": "Propietario",
  "anticipation_hours": null,
  "port_timezone": "America/Bogota",
  "scheduled_departure": "2026-11-20T09:00:00-05:00",
  "cancelled_at": "2026-11-19T14:15:00-05:00",
  "reason": "fuerza_mayor_logistica",
  "justification": "Capitán asignado con incapacidad médica de urgencia. No se cuenta con relevo certificado para la fecha.",
  "vessel_id": "b3e2a1d0-4f5c-6b7a-8e9f-0a1b2c3d4e5f",
  "vessel_new_operational_status": "Disponible",
  "m1_sync_status": "CONFIRMADA",
  "m3_notification_status": "PUBLICADA",
  "message": "Cancelación por Propietario procesada. Clasificación Por Propietario asignada. La embarcación se mantiene Disponible al no mediar avería mecánica."
}
```

---

### Ejemplo 5: Cancelación por Propietario — Causal Avería Mecánica (Inhabilitación del Activo)

#### Petición HTTP (`curl`)
```bash
curl -X POST "https://api.seashare.com/api/v1/reservas/c1f7a8b2-5e4d-4c3b-8a1e-9f0a2b3c4d5e/cancelacion" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "reason": "averia_mecanica",
    "justification": "Fallo crítico en el sistema de propulsión de estribor detectado en la inspección técnica previa al zarpe."
  }'
```

#### Respuesta Exitosa (`200 OK`)
```json
{
  "reservation_id": "c1f7a8b2-5e4d-4c3b-8a1e-9f0a2b3c4d5e",
  "status": "Cancelada",
  "sub_status": "Por Propietario",
  "requesting_actor": "Propietario",
  "anticipation_hours": null,
  "port_timezone": "America/Bogota",
  "scheduled_departure": "2026-11-20T09:00:00-05:00",
  "cancelled_at": "2026-11-20T07:10:00-05:00",
  "reason": "averia_mecanica",
  "justification": "Fallo crítico en el sistema de propulsión de estribor detectado en la inspección técnica previa al zarpe.",
  "vessel_id": "b3e2a1d0-4f5c-6b7a-8e9f-0a1b2c3d4e5f",
  "vessel_new_operational_status": "En Mantenimiento/Limpieza",
  "m1_sync_status": "CONFIRMADA",
  "m3_notification_status": "PUBLICADA",
  "message": "Cancelación por Propietario procesada. Clasificación Por Propietario asignada. La embarcación ha sido transferida a 'En Mantenimiento/Limpieza' en Módulo 1 para inhabilitar nuevas reservas."
}
```

---

## 6. Respuestas de Error y Códigos HTTP

Todos los errores retornan un sobre uniforme con `code` y `message`:

```json
{
  "code": "CODIGO_ERROR",
  "message": "Descripción detallada y accionable del motivo de rechazo."
}
```

| Código HTTP | Código Interno (`code`) | Causa / Condición de Disparo | Cuerpo de Respuesta de Ejemplo |
| :--- | :--- | :--- | :--- |
| **`400 Bad Request`** | `INVALID_UUID` | El identificador `reservation_id` no cumple el formato UUIDv4. | `{"code": "INVALID_UUID", "message": "El identificador de reserva proporcionado no es un UUID válido."}` |
| **`400 Bad Request`** | `VALIDATION_ERROR` | El solicitante es Propietario y omitió el campo `reason`, o envió un valor no soportado. | `{"code": "VALIDATION_ERROR", "message": "El campo 'reason' es obligatorio para el Propietario y debe ser 'fuerza_mayor_logistica' o 'averia_mecanica'."}` |
| **`401 Unauthorized`** | `AUTH_TOKEN_MISSING_OR_INVALID` | El header `Authorization` no fue proporcionado o el JWT expiró. | `{"code": "AUTH_TOKEN_MISSING_OR_INVALID", "message": "Token de autenticación ausente o inválido."}` |
| **`403 Forbidden`** | `FORBIDDEN_NOT_AUTHORIZED` | El usuario autenticado no es ni el Arrendatario titular ni el Propietario registrado de la reserva. | `{"code": "FORBIDDEN_NOT_AUTHORIZED", "message": "No está autorizado para cancelar esta reserva. Solo el arrendatario titular o el propietario del activo pueden solicitar la cancelación."}` |
| **`404 Not Found`** | `RESERVATION_NOT_FOUND` | La reserva no existe en la base de datos de Módulo 2. | `{"code": "RESERVATION_NOT_FOUND", "message": "No se encontró ninguna reserva asociada al identificador proporcionado."}` |
| **`409 Conflict`** | `INVALID_RESERVATION_STATE` | La reserva no está en estado `Reservada` (p. ej. en `Iniciada`, `Pendiente de Pago`, `En Navegación`, `Completada` o `Cancelada`). | `{"code": "INVALID_RESERVATION_STATE", "message": "Solo se pueden cancelar reservas en estado 'Reservada'. Las reservas 'Pendiente de Pago' no admiten cancelación activa y deben esperar la expiración de su temporizador TTL."}` |
| **`409 Conflict`** | `CONCURRENT_STATE_CHANGE` | Condición de carrera: la reserva comenzó la navegación o fue cancelada por el otro actor milisegundos antes. | `{"code": "CONCURRENT_STATE_CHANGE", "message": "Conflicto de concurrencia: el estado de la reserva ha cambiado simultáneamente por otra operación confirmada."}` |
| **`503 Service Unavailable`** | `HARBOR_TIMEZONE_UNAVAILABLE` | Fallo de comunicación o timeout con Módulo 1 (`CU-09`) impidiendo conocer la zona horaria del puerto. Fail-safe activo. | `{"code": "HARBOR_TIMEZONE_UNAVAILABLE", "message": "No es posible calcular la anticipación de la cancelación debido a que el servicio de Flota (Módulo 1) no respondió con la zona horaria del puerto. Operación detenida por seguridad contractual. Reintente en unos instantes."}` |
| **`500 Internal Server Error`** | `INTERNAL_SERVER_ERROR` | Fallo no controlado de base de datos o broker de eventos. | `{"code": "INTERNAL_SERVER_ERROR", "message": "Error interno del servidor al procesar la cancelación."}` |

---

## 7. Notas Transversales y Reglas de Negocio Arquitecturales

### 7.1 Regla de Oro "Sin Dinero" (Zero-Financial Footprint)
Módulo 2 actúa como orquestador temporal, regulador de estados contractuales y clasificador de políticas náuticas. Bajo ninguna circunstancia este endpoint:
- Calcula montos monetarios de penalidad o retención.
- Calcula montos monetarios a reembolsar al cliente.
- Realiza transferencias, débitos bancarios o reversiones en pasarelas de pago.

La respuesta síncrona solo reporta la clasificación (`Flexible`, `Moderado`, `Tardío` o `Por Propietario`), y el evento emitido a Módulo 3 (`reserva.estado.cancelada`) delega toda la liquidación contable en el motor financiero de Módulo 3.

### 7.2 Resolución Estricta de Límites Temporales (Límites Inclusivos)
Para evitar disputas legales con el arrendatario, la resolución de los límites exactos de anticipación horaria se implementa con carácter inclusivo a favor del consumidor:
- Si `anticipacion == 72h:00m:00s` $\rightarrow$ Clasifica como **Flexible** (100% elegible para reembolso).
- Si `anticipacion == 24h:00m:00s` $\rightarrow$ Clasifica como **Moderado** (50% elegible para reembolso).
- Si `anticipacion < 24h:00m:00s` $\rightarrow$ Clasifica como **Tardío** (0% elegible para reembolso).

### 7.3 Fail-Safe de Zona Horaria (Dependencia de Módulo 1)
La anticipación debe evaluarse estrictamente en la zona horaria oficial del puerto donde está amarrada la embarcación (obtenida mediante `CU-09 Proveer información de embarcación`), nunca según el reloj del dispositivo del usuario ni la zona horaria local del servidor de backend. Si Módulo 1 no responde, el sistema **no asume ninguna zona horaria por defecto** (`UTC` o de servidor) y aborta la petición con `503 Service Unavailable`, garantizando la certeza legal del cálculo de anticipación.

### 7.4 Cancelación por Propietario y Matriz de Estado Náutico
Cuando el Propietario cancela, no se mide el tiempo respecto al zarpe. Su elección en el campo `reason` rige la sincronización síncrona con Módulo 1 (`PUT /api/v1/embarcaciones/{vessel_id}/estado-operativo`):
1. **Fuerza mayor logística**: Embarcación pasa a `Disponible` (puede volver a recibir reservas).
2. **Avería mecánica**: Embarcación pasa a `En Mantenimiento/Limpieza` (queda bloqueada del catálogo comercial hasta que Módulo 1 certifique su reparación).

### 7.5 Idempotencia y Manejo de Doble Clic
Si el cliente reenvía la petición con el mismo `X-Idempotency-Key` dentro de una ventana de 60 segundos sobre una reserva que ya fue cancelada mediante dicha clave, el endpoint retorna `200 OK` con la misma carga de respuesta previamente generada en lugar de un error `409 Conflict`.

---

## 8. Trazabilidad de Requisitos

| Requisito Funcional / Criterio | Descripción en Spec | Cobertura en este Contrato |
| :--- | :--- | :--- |
| **FR-001** | Activación como extensión (`<<extend>>`) de `Ver detalle de reserva`. | Diseñado para invocarse desde el contexto profundo de la reserva; path `/api/v1/reservas/{reservation_id}/cancelacion`. |
| **FR-002** | Permitir cancelación si y solo si la reserva está en `Reservada`. | Validación estricta de estado. Retorna `409 Conflict` si no está en `Reservada`. |
| **FR-003** | Validar que el solicitante sea Arrendatario titular o Propietario registrado. | Seguridad JWT y autorización estricta en sección 3 y código `403 FORBIDDEN_NOT_AUTHORIZED`. |
| **FR-004** | Denegar explícitamente reservas en `Iniciada` y `Pendiente de Pago` (deben expirar pasivamente). | Regla explícita documentada en sección 2 y error `409 INVALID_RESERVATION_STATE`. |
| **FR-005** | Consultar puerto y zona horaria a M1 (`CU-09`); no asumir huso por defecto si M1 falla. | Inclusión obligatoria de zona horaria del puerto y código `503 HARBOR_TIMEZONE_UNAVAILABLE`. |
| **FR-006** | Calcular anticipación exacta en horas y minutos bajo zona horaria del puerto. | Atributo `anticipation_hours` computado según `port_timezone`. |
| **FR-007** | Clasificación temporal Arrendatario: Flexible ($\ge 72$h), Moderado ($24$h a $72$h), Tardío ($< 24$h). | Matriz de cálculo y ejemplos 1, 2 y 3. |
| **FR-008** | Permitir cancelar hasta el último minuto previo al zarpe bajo franja Tardío. | Confirmado en reglas de negocio y ejemplo 3. |
| **FR-009** | Propietario recibe clasificación directa "Por Propietario" sin evaluar horas. | Ejemplos 4 y 5. Campo `anticipation_hours` se serializa como `null`. |
| **FR-010** | Captura obligatoria de motivo para Propietario (`fuerza_mayor_logistica` o `averia_mecanica`). | Validación de Request Body en sección 4.1 y error `400 VALIDATION_ERROR`. |
| **FR-011** | Invocar obligatoriamente el caso de uso interno `Actualizar estado reserva` (`CU-08`). | Se delega internamente la persistencia y control transaccional a CU-08. |
| **FR-012** | Delegar actualización de estado operativo de M1 (`Disponible` o `En Mantenimiento/Limpieza`). | Sincronización documentada en campos `vessel_new_operational_status` y `m1_sync_status`. |
| **FR-013** | Delegar notificación AMQP a Módulo 3 (`reserva.estado.cancelada`) con sub-estado. | Notificación AMQP documentada en campo `m3_notification_status` y contrato `CU-14-estado-reserva`. |
| **FR-014** | Regla estricta "Sin dinero": cero cálculos de montos o penalidades en M2. | Cumplimiento total. No existe ningún atributo monetario en el request ni response JSON. |
| **FR-015** | Registro auditable del evento `CancellationEvent`. | Atributos registrados en la base de datos de dominio y reflejados en el response. |
| **FR-016** | Soporte de datos para ventana emergente de cancelación del Arrendatario. | Campos `scheduled_departure`, `cancelled_at`, `anticipation_hours` y `sub_status` provistos para la UI. |
| **FR-017** | Soporte de datos para ventana emergente del Propietario (motivo y justificación). | Campos `reason` y `justification` recibidos y procesados. |
| **SC-001** | Cero cancelaciones permitidas sobre reservas en estado no cancelable. | Garantizado por código `409 Conflict`. |
| **SC-002** | 100% de clasificaciones correctas según franjas horarias del Arrendatario. | Algoritmo estricto con límites inclusivos en horas completas y minutos. |
| **SC-003** | 100% de cancelaciones del Propietario reciben "Por Propietario". | Lógica directa por rol del actor. |
| **SC-004** | Cero clasificaciones emitidas con datos incompletos o zona horaria ausente. | Garantizado por fail-safe `503 HARBOR_TIMEZONE_UNAVAILABLE`. |
| **SC-005** | Actualización de M1 (`Asignar estado operativo`) en $< 1$ segundo tras la cancelación. | Sincronización HTTP síncrona inmediata documentada. |
| **SC-006** | 100% de eventos notificados a Módulo 3 para liquidación de fondos. | Publicación garantizada en RabbitMQ (`reserva.estado.cancelada`). |
| **SC-007** | Cero cálculos de reembolsos, comisiones o penalidades monetarias en M2. | Verificado en esquemas de datos. |
| **SC-008** | Cero cancelaciones autorizadas a terceros no legítimos. | Garantizado por validación JWT y código `403 Forbidden`. |
