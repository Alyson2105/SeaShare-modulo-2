# Contrato de Interfaz REST: CU-21 Ver Detalle de Reserva

## 1. Identificación y Metadatos

- **Identificador del Contrato**: `REST-M2-CU21-VER-DETALLE-RESERVA`
- **Módulo Responsable**: Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones
- **Tipo de Interfaz**: REST sincrónico expuesto por Módulo 2
- **Actor / Consumidor Autorizado**: 
  - `RENTER` (únicamente el titular de la reserva)
  - `OWNER` (únicamente el dueño registrado de la embarcación asociada)
- **Caso de Uso Base / Relaciones**: 
  - Extiende a: `Ver mis reservas` (`<<extend>>` - CU-20)
  - Es extendido por: `Solicitar cancelación` (`<<extend>>` - CU-04)
  - Es extendido por: `Iniciar pago` (`<<extend>>` - CU-03)
- **Versión del Contrato**: 1.0.0
- **Fecha de Creación**: 2026-10-09

---

## 2. Propósito y Alcance del Endpoint

Este endpoint de solo lectura expone la ficha técnica y contractual completa de una reserva específica, sirviendo como punto de anclaje visual y operativo para el seguimiento del viaje, la gestión de acciones de muelle y la activación de cancelaciones o pagos.

### Responsabilidades del Endpoint:
1. **Control Estricto de Acceso y Privacidad (`SC-002`, `FR-002`)**:
   - Valida que el usuario autenticado sea el `renter_id` de la reserva o el `owner_id` de la embarcación.
   - Cualquier tercero no involucrado es denegado de forma inmediata con `403 Forbidden`.
   - Protege la privacidad de datos cruzados: no expone instrumentos bancarios ni números de tarjeta de crédito de las partes.
2. **Exposición Integral de la Entidad de Dominio (`FR-003`, `FR-007`)**:
   - Entrega los metadatos de la reserva, el itinerario pactado (fechas, horas, puerto de zarpe y zona horaria) y los hitos de ejecución real (`actual_departure_at` y `actual_arrival_at` conforme el viaje transiciona de estados).
   - Provee los servicios incluidos y comodidades de la embarcación en formato de lista para el arrendatario (`FR-008`).
   - Adapta la información del interlocutor: entrega al arrendatario los datos del propietario y entrega al propietario los datos del cliente titular con su récord de viajes previos (`FR-009`).
3. **Regla Estricta "Sin Dinero" (Inmutabilidad Financiera, `FR-004`, `FR-010`)**:
   - Todos los importes monetarios exhibidos provienen literalmente de la liquidación oficial realizada por Módulo 3.
   - Módulo 2 **jamás ejecuta matemáticas locales, descuentos ni estimaciones de penalidades**.
   - En reservas `CANCELLED`, expone el total original congelado junto con notas aclaratorias delegando la liquidación contable en Módulo 3 (`FR-006`, `FR-014`).
4. **Resumen de Pago Adaptativo según Estado Operativo**:
   - En `INITIATED`: presenta la cotización estimada previa al pago formal.
   - En `RESERVED` y `IN_NAVIGATION`: exhibe el breakdown oficial de tres conceptos vinculantes: "Tarifa base de alquiler", "Seguro obligatorio" y "Depósito de garantía", junto con el "Total pagado" en moneda oficial (`FR-011`, `FR-012`).
   - En `COMPLETED` (ventana de disputa): oculta tarifa y seguro para focalizar la visualización exclusivamente en el "Depósito de garantía" sujeto a resolución operativa (`FR-013`).
   - En `CANCELLED (NO_SHOW)`: titula el bloque como "Compensación", mostrando el total original y la leyenda aclaratoria de liquidación (`FR-014`).
5. **Habilitación Dinámica de Acciones y Puntos de Extensión (`FR-005`, `FR-015` a `FR-021`)**:
   - Provee indicadores booleanos (`can_cancel`, `can_initiate_payment`, `can_mark_trip_start`, `can_mark_no_show`, `can_mark_trip_end`) que determinan los botones e interacciones activas en la interfaz del cliente.
   - Computa contadores de tiempo en vivo: segundos restantes del TTL (15 min), margen de cortesía de espera en muelle (30 min) y ventana para radicar disputas de daños (24 h).

---

## 3. Definición del Endpoint

- **Método HTTP**: `GET`
- **Ruta**: `/api/v1/reservations/{reservation_id}`
- **Formato de Petición / Respuesta**: `application/json`
- **Codificación**: `UTF-8`

### 3.1 Encabezados HTTP (Headers)

| Header | Tipo | Obligatorio | Descripción |
| :--- | :--- | :--- | :--- |
| `Authorization` | String | Sí | Token Bearer JWT del usuario autenticado (`Bearer eyJhbG...`). |
| `Accept` | String | Sí | Debe ser `application/json`. |

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
  "status": "RESERVED",
  "sub_status": null,
  "status_badge": {
    "text": "Confirmada",
    "semantic_color": "verde",
    "description": "Pago confirmado y lista para zarpe"
  },
  "vessel": {
    "vessel_id": "b3e2a1d0-4f5c-6b7a-8e9f-0a1b2c3d4e5f",
    "name": "Catamarán Sea Breeze",
    "type": "Catamarán",
    "image_url": "https://cdn.seashare.com/images/boats/sea-breeze-main.jpg",
    "included_services": [
      "GPS Náutico",
      "Chalecos salvavidas certificados",
      "Nevera con hielo",
      "Capitán certificado",
      "Equipo de sonido Bluetooth"
    ]
  },
  "itinerary": {
    "scheduled_departure": "2026-11-20T09:00:00-05:00",
    "scheduled_arrival": "2026-11-22T18:00:00-05:00",
    "actual_departure_at": null,
    "actual_arrival_at": null,
    "departure_port": {
      "name": "Marina Santa Marta",
      "city": "Santa Marta",
      "timezone": "America/Bogota"
    },
    "duration_days": 3,
    "duration_nights": 2,
    "passengers": 4
  },
  "renter": {
    "renter_id": "u1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
    "name": "Laura Gómez",
    "previous_trips": 5
  },
  "owner": {
    "owner_id": "p9a8b7c6-d5e4-3f2a-1b0c-9d8e7f6a5b4c",
    "name": "Carlos Mendoza"
  },
  "payment_summary": {
    "section_title": "Resumen de pago",
    "currency": "COP",
    "rental_amount": 2800000.00,
    "insurance_amount": 350000.00,
    "guarantee_deposit_amount": 300000.00,
    "total_paid": 3450000.00,
    "clarifying_note": null
  },
  "available_actions": {
    "can_cancel": true,
    "can_initiate_payment": false,
    "can_mark_trip_start": false,
    "can_mark_no_show": false,
    "can_mark_trip_end": false
  },
  "time_indicators": {
    "ttl_remaining_seconds": null,
    "minutes_to_departure": 15850,
    "courtesy_elapsed_minutes": null,
    "dispute_window_remaining_seconds": null
  },
  "contextual_banner": {
    "type": "informative",
    "title": "Reserva confirmada",
    "message": "La embarcación estará alistada en el muelle de Marina Santa Marta a las 09:00 AM."
  },
  "created_at": "2026-10-09T10:00:00-05:00"
}
```

#### Descripción de Campos de Salida

| Campo | Tipo | Nulabilidad | Descripción |
| :--- | :--- | :--- | :--- |
| `reservation_id` | String (UUID) | No nulo | Identificador universal único de la reserva. |
| `reservation_code` | String | No nulo | Código de referencia náutica (ej. `"#RS-4492"`). |
| `status` | String (Enum) | No nulo | Estado operativo principal (`"INITIATED"`, `"PENDING_PAYMENT"`, `"RESERVED"`, `"IN_NAVIGATION"`, `"COMPLETED"`, `"CANCELLED"`, `"EXPIRED"`, `"PAYMENT_FAILED"`). |
| `sub_status` | String (Enum) | Nulo condicional | Sub-clasificación contractual en reservas canceladas (`"FLEXIBLE"`, `"MODERATE"`, `"LATE"`, `"BY_OWNER"`, `"NO_SHOW"`). Nulo en otros estados. |
| `status_badge` | Objeto | No nulo | Datos semánticos de visualización (`text`, `semantic_color`, `description`). |
| `vessel` | Objeto | No nulo | Ficha técnica resumida del activo y sus comodidades incluidas (`included_services[]`). |
| `itinerary` | Objeto | No nulo | Detalle cronológico pactado y tiempos reales de navegación (`actual_departure_at`, `actual_arrival_at`, `departure_port`). |
| `renter` | Objeto | No nulo | Datos del cliente titular y viajes históricos en la plataforma (`previous_trips`). |
| `owner` | Objeto | No nulo | Datos del anfitrión propietario de la embarcación. |
| `payment_summary` | Objeto | No nulo | Desglose financiero oficial de solo lectura emitido por M3. Se adapta según el estado de la reserva. |
| `payment_summary.rental_amount` | Number / null | Nulo condicional | Costo del alquiler (oculto en `COMPLETED` durante disputa de garantía). |
| `payment_summary.insurance_amount` | Number / null | Nulo condicional | Costo de la póliza de seguro marítimo obligatorio. |
| `payment_summary.guarantee_deposit_amount` | Number / null | Nulo condicional | Depósito de custodia (10% tarifa base). Siempre visible en `RESERVED`, `IN_NAVIGATION` y `COMPLETED`. |
| `payment_summary.total_paid` | Number | No nulo | Total pagado oficial y congelado en moneda local. |
| `payment_summary.clarifying_note` | String / null | Nulo condicional | Advertencia legal (ej. `"El sistema de pagos gestionará la compensación al propietario."`). |
| `available_actions` | Objeto | No nulo | Flags booleanos que orquestan los botones interactivos de la interfaz según rol y estado. |
| `time_indicators`| Objeto | No nulo | Métricas en segundos/minutos para renderizar contadores regresivos en la UI. |
| `contextual_banner` | Objeto | Nulo condicional | Información destacada de estado para banners superiores en la pantalla. |
| `created_at` | String (ISO 8601) | No nulo | Marca de tiempo de registro inicial de la reserva. |

---

## 5. Ejemplos de Petición y Respuesta

### Ejemplo 1: Reserva en estado `RESERVED` (Previo al inicio, acción de cancelación disponible)

#### Petición HTTP (`curl`)
```bash
curl -X GET "https://api.seashare.com/api/v1/reservations/c1f7a8b2-5e4d-4c3b-8a1e-9f0a2b3c4d5e" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Accept: application/json"
```

#### Respuesta Exitosa (`200 OK`)
```json
{
  "reservation_id": "c1f7a8b2-5e4d-4c3b-8a1e-9f0a2b3c4d5e",
  "reservation_code": "#RS-4492",
  "status": "RESERVED",
  "sub_status": null,
  "status_badge": {
    "text": "Confirmada",
    "semantic_color": "verde",
    "description": "Reserva lista para zarpe"
  },
  "vessel": {
    "vessel_id": "b3e2a1d0-4f5c-6b7a-8e9f-0a1b2c3d4e5f",
    "name": "Catamarán Sea Breeze",
    "type": "Catamarán",
    "image_url": "https://cdn.seashare.com/images/boats/sea-breeze-main.jpg",
    "included_services": [
      "GPS Náutico",
      "Chalecos salvavidas certificados",
      "Nevera con hielo",
      "Capitán certificado"
    ]
  },
  "itinerary": {
    "scheduled_departure": "2026-11-20T09:00:00-05:00",
    "scheduled_arrival": "2026-11-22T18:00:00-05:00",
    "actual_departure_at": null,
    "actual_arrival_at": null,
    "departure_port": {
      "name": "Marina Santa Marta",
      "city": "Santa Marta",
      "timezone": "America/Bogota"
    },
    "duration_days": 3,
    "duration_nights": 2,
    "passengers": 4
  },
  "renter": {
    "renter_id": "u1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
    "name": "Laura Gómez",
    "previous_trips": 5
  },
  "owner": {
    "owner_id": "p9a8b7c6-d5e4-3f2a-1b0c-9d8e7f6a5b4c",
    "name": "Carlos Mendoza"
  },
  "payment_summary": {
    "section_title": "Resumen de pago",
    "currency": "COP",
    "rental_amount": 2800000.00,
    "insurance_amount": 350000.00,
    "guarantee_deposit_amount": 300000.00,
    "total_paid": 3450000.00,
    "clarifying_note": null
  },
  "available_actions": {
    "can_cancel": true,
    "can_initiate_payment": false,
    "can_mark_trip_start": false,
    "can_mark_no_show": false,
    "can_mark_trip_end": false
  },
  "time_indicators": {
    "ttl_remaining_seconds": null,
    "minutes_to_departure": 15850,
    "courtesy_elapsed_minutes": null,
    "dispute_window_remaining_seconds": null
  },
  "contextual_banner": {
    "type": "informative",
    "title": "Reserva confirmada",
    "message": "La embarcación estará alistada en el muelle a la hora pactada."
  },
  "created_at": "2026-10-09T10:00:00-05:00"
}
```

---

### Ejemplo 2: Reserva en estado `IN_NAVIGATION` (Viaje en curso, salida real asentada)

#### Petición HTTP (`curl`)
```bash
curl -X GET "https://api.seashare.com/api/v1/reservations/a9b8c7d6-e5f4-3a2b-1c0d-9e8f7a6b5c4d" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Accept: application/json"
```

#### Respuesta Exitosa (`200 OK`)
```json
{
  "reservation_id": "a9b8c7d6-e5f4-3a2b-1c0d-9e8f7a6b5c4d",
  "reservation_code": "#RS-4480",
  "status": "IN_NAVIGATION",
  "sub_status": null,
  "status_badge": {
    "text": "En navegación",
    "semantic_color": "amarillo",
    "description": "Embarcación en el mar"
  },
  "vessel": {
    "vessel_id": "b3e2a1d0-4f5c-6b7a-8e9f-0a1b2c3d4e5f",
    "name": "Catamarán Sea Breeze",
    "type": "Catamarán",
    "image_url": "https://cdn.seashare.com/images/boats/sea-breeze-main.jpg",
    "included_services": [
      "GPS Náutico",
      "Chalecos salvavidas",
      "Nevera"
    ]
  },
  "itinerary": {
    "scheduled_departure": "2026-10-09T08:00:00-05:00",
    "scheduled_arrival": "2026-10-09T17:00:00-05:00",
    "actual_departure_at": "2026-10-09T08:05:22-05:00",
    "actual_arrival_at": null,
    "departure_port": {
      "name": "Marina Santa Marta",
      "city": "Santa Marta",
      "timezone": "America/Bogota"
    },
    "duration_days": 1,
    "duration_nights": 0,
    "passengers": 6
  },
  "renter": {
    "renter_id": "u9z8y7x6-w5v4-3u2t-1s0r-9q8p7o6n5m4l",
    "name": "Mateo Rossi",
    "previous_trips": 2
  },
  "owner": {
    "owner_id": "p9a8b7c6-d5e4-3f2a-1b0c-9d8e7f6a5b4c",
    "name": "Carlos Mendoza"
  },
  "payment_summary": {
    "section_title": "Resumen de pago",
    "currency": "COP",
    "rental_amount": 1400000.00,
    "insurance_amount": 250000.00,
    "guarantee_deposit_amount": 150000.00,
    "total_paid": 1800000.00,
    "clarifying_note": null
  },
  "available_actions": {
    "can_cancel": false,
    "can_initiate_payment": false,
    "can_mark_trip_start": false,
    "can_mark_no_show": false,
    "can_mark_trip_end": true
  },
  "time_indicators": {
    "ttl_remaining_seconds": null,
    "minutes_to_departure": 0,
    "courtesy_elapsed_minutes": null,
    "dispute_window_remaining_seconds": null
  },
  "contextual_banner": {
    "type": "active",
    "title": "El viaje está en curso",
    "message": "Navegación iniciada a las 08:05 AM. Se espera el atraque antes de las 17:00."
  },
  "created_at": "2026-10-08T16:00:00-05:00"
}
```

---

### Ejemplo 3: Reserva en estado `COMPLETED` (Ventana de 24h para disputa de garantía)

#### Petición HTTP (`curl`)
```bash
curl -X GET "https://api.seashare.com/api/v1/reservations/f5a6b7c8-9d0e-1f2a-3b4c-5d6e7f8a9b0c" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Accept: application/json"
```

#### Respuesta Exitosa (`200 OK`)
```json
{
  "reservation_id": "f5a6b7c8-9d0e-1f2a-3b4c-5d6e7f8a9b0c",
  "reservation_code": "#RS-2950",
  "status": "COMPLETED",
  "sub_status": null,
  "status_badge": {
    "text": "Completada",
    "semantic_color": "verde",
    "description": "Viaje finalizado exitosamente"
  },
  "vessel": {
    "vessel_id": "b3e2a1d0-4f5c-6b7a-8e9f-0a1b2c3d4e5f",
    "name": "Catamarán Sea Breeze",
    "type": "Catamarán",
    "image_url": "https://cdn.seashare.com/images/boats/sea-breeze-main.jpg",
    "included_services": [
      "GPS Náutico",
      "Chalecos salvavidas certificados"
    ]
  },
  "itinerary": {
    "scheduled_departure": "2026-09-10T10:00:00-05:00",
    "scheduled_arrival": "2026-09-11T16:00:00-05:00",
    "actual_departure_at": "2026-09-10T10:12:00-05:00",
    "actual_arrival_at": "2026-09-11T15:50:30-05:00",
    "departure_port": {
      "name": "Marina Santa Marta",
      "city": "Santa Marta",
      "timezone": "America/Bogota"
    },
    "duration_days": 2,
    "duration_nights": 1,
    "passengers": 4
  },
  "renter": {
    "renter_id": "u1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
    "name": "Laura Gómez",
    "previous_trips": 4
  },
  "owner": {
    "owner_id": "p9a8b7c6-d5e4-3f2a-1b0c-9d8e7f6a5b4c",
    "name": "Carlos Mendoza"
  },
  "payment_summary": {
    "section_title": "Garantía en custodia",
    "currency": "COP",
    "rental_amount": null,
    "insurance_amount": null,
    "guarantee_deposit_amount": 200000.00,
    "total_paid": 2300000.00,
    "clarifying_note": "Depósito en ventana de evaluación (24h). De no presentarse reclamos, será liberado al arrendatario por Módulo 3."
  },
  "available_actions": {
    "can_cancel": false,
    "can_initiate_payment": false,
    "can_mark_trip_start": false,
    "can_mark_no_show": false,
    "can_mark_trip_end": false
  },
  "time_indicators": {
    "ttl_remaining_seconds": null,
    "minutes_to_departure": 0,
    "courtesy_elapsed_minutes": null,
    "dispute_window_remaining_seconds": 43200
  },
  "contextual_banner": {
    "type": "informative",
    "title": "Viaje completado",
    "message": "La embarcación ha sido devuelta y atracada. Quedan 12 horas para el cierre automático de la ventana de inspección."
  },
  "created_at": "2026-09-02T11:00:00-05:00"
}
```

---

### Ejemplo 4: Reserva `CANCELLED` con sub-estado `NO_SHOW` (No-Show)

#### Petición HTTP (`curl`)
```bash
curl -X GET "https://api.seashare.com/api/v1/reservations/8b9c0d1e-2f3a-4b5c-6d7e-8f9a0b1c2d3e" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Accept: application/json"
```

#### Respuesta Exitosa (`200 OK`)
```json
{
  "reservation_id": "8b9c0d1e-2f3a-4b5c-6d7e-8f9a0b1c2d3e",
  "reservation_code": "#RS-1904",
  "status": "CANCELLED",
  "sub_status": "NO_SHOW",
  "status_badge": {
    "text": "Cancelado por inasistencia",
    "semantic_color": "rojo",
    "description": "El arrendatario no se presentó tras cumplirse la cortesía de 30 minutos"
  },
  "vessel": {
    "vessel_id": "b3e2a1d0-4f5c-6b7a-8e9f-0a1b2c3d4e5f",
    "name": "Catamarán Sea Breeze",
    "type": "Catamarán",
    "image_url": "https://cdn.seashare.com/images/boats/sea-breeze-main.jpg",
    "included_services": [
      "GPS Náutico",
      "Chalecos salvavidas certificados"
    ]
  },
  "itinerary": {
    "scheduled_departure": "2026-08-15T09:00:00-05:00",
    "scheduled_arrival": "2026-08-15T18:00:00-05:00",
    "actual_departure_at": null,
    "actual_arrival_at": null,
    "departure_port": {
      "name": "Marina Santa Marta",
      "city": "Santa Marta",
      "timezone": "America/Bogota"
    },
    "duration_days": 1,
    "duration_nights": 0,
    "passengers": 4
  },
  "renter": {
    "renter_id": "u4b5c6d7-e8f9-0a1b-2c3d-4e5f6a7b8c9d",
    "name": "Andrés Gómez",
    "previous_trips": 1
  },
  "owner": {
    "owner_id": "p9a8b7c6-d5e4-3f2a-1b0c-9d8e7f6a5b4c",
    "name": "Carlos Mendoza"
  },
  "payment_summary": {
    "section_title": "Compensación",
    "currency": "COP",
    "rental_amount": null,
    "insurance_amount": null,
    "guarantee_deposit_amount": null,
    "total_paid": 1600000.00,
    "clarifying_note": "El sistema de pagos (Módulo 3) gestionará la compensación al propietario según las políticas de No-Show."
  },
  "available_actions": {
    "can_cancel": false,
    "can_initiate_payment": false,
    "can_mark_trip_start": false,
    "can_mark_no_show": false,
    "can_mark_trip_end": false
  },
  "time_indicators": {
    "ttl_remaining_seconds": null,
    "minutes_to_departure": 0,
    "courtesy_elapsed_minutes": 30,
    "dispute_window_remaining_seconds": null
  },
  "contextual_banner": {
    "type": "alert",
    "title": "Reserva cancelada por inasistencia",
    "message": "Se cumplió la ventana de cortesía de 30 minutos sin presentación del cliente. Inasistencia reportada por el propietario Carlos Mendoza."
  },
  "created_at": "2026-08-10T09:00:00-05:00"
}
```

---

## 6. Respuestas de Error y Códigos HTTP

Todos los errores retornan un sobre uniforme con `code` y `message`:

```json
{
  "code": "ERROR_CODE",
  "message": "Descripción detallada del motivo de rechazo."
}
```

| Código HTTP | Código Interno (`code`) | Causa / Condición de Disparo | Cuerpo de Respuesta de Ejemplo |
| :--- | :--- | :--- | :--- |
| **`400 Bad Request`** | `INVALID_UUID` | El parámetro de ruta `reservation_id` no cumple el estándar UUIDv4. | `{"code": "INVALID_UUID", "message": "El identificador de reserva proporcionado no tiene un formato UUID válido."}` |
| **`401 Unauthorized`** | `AUTH_TOKEN_MISSING_OR_INVALID` | El header `Authorization` no fue provisto o el token JWT expiró. | `{"code": "AUTH_TOKEN_MISSING_OR_INVALID", "message": "Token de autenticación ausente o inválido."}` |
| **`403 Forbidden`** | `FORBIDDEN_NOT_AUTHORIZED` | El usuario autenticado no es el Arrendatario titular ni el Propietario del activo náutico asociado (`SC-002`). | `{"code": "FORBIDDEN_NOT_AUTHORIZED", "message": "Acceso denegado: no está autorizado para consultar los detalles de una reserva en la que no participa activamente."}` |
| **`404 Not Found`** | `RESERVATION_NOT_FOUND` | No existe ninguna reserva en Módulo 2 con el `reservation_id` indicado. | `{"code": "RESERVATION_NOT_FOUND", "message": "No se encontró ninguna reserva asociada al identificador proporcionado."}` |
| **`500 Internal Server Error`** | `INTERNAL_SERVER_ERROR` | Fallo inesperado en base de datos al recuperar la información. | `{"code": "INTERNAL_SERVER_ERROR", "message": "Error interno del servidor al procesar la consulta del detalle de la reserva."}` |

---

## 7. Notas Transversales y Reglas de Negocio Arquitecturales

### 7.1 Regla de Oro "Sin Dinero" (Inmutabilidad Financiera)
El objeto `payment_summary` refleja de manera fidedigna los importes liquidados por Módulo 3 y almacenados de manera inmutable en la entidad `Reservation`.
- Módulo 2 no efectúa sumas, redondeos de impuestos, conversiones de divisas ni tasas de descuento.
- En estado `COMPLETED`, la vista se centra en el `guarantee_deposit_amount` en custodia para la resolución de daños.
- En estado `CANCELLED`, no se calculan montos de penalidad en la respuesta: el sistema muestra el valor total original pagado y delega la cifra de dispersión a Módulo 3.

### 7.2 Orquestación Visual de Acciones en Muelle
El objeto `available_actions` abstrae la complejidad temporal y de roles:
- El botón de **Cancelar** solo se habilita (`can_cancel: true`) si el estado es exactamente `RESERVED` (previo al inicio de navegación).
- Los botones de **Inicio de Navegación** e **Inasistencia** se habilitan únicamente para el Propietario, calculando contra el huso horario oficial del puerto amarrado en la reserva.

---

## 8. Trazabilidad de Requisitos

| Requisito Funcional / Criterio | Descripción en Spec | Cobertura en este Contrato |
| :--- | :--- | :--- |
| **FR-001** | Recibir el identificador de la reserva específica como parámetro (`<<extend>>` de Ver mis reservas). | Path parameter `reservation_id` en `/api/v1/reservations/{reservation_id}`. |
| **FR-002** | Validar autorización del usuario en sesión (Arrendatario o Propietario). | Verificación JWT y código `403 FORBIDDEN_NOT_AUTHORIZED`. |
| **FR-003** | Exponer ID, fechas, horas, estado, sub-estado y breakdown monetario guardado. | Estructura JSON completa en sección 4.1. |
| **FR-004** | Todos los valores financieros son de solo lectura y reflejan lo devuelto por Módulo 3. | Regla documentada en sección 2 y 7.1. Cero matemáticas locales. |
| **FR-005** | En `RESERVED`, proveer punto de acceso para detonar `Solicitar cancelación`. | Flag `available_actions.can_cancel: true` en estado `RESERVED`. |
| **FR-006** | En `CANCELLED`, mostrar sub-estado exacto junto con el monto total original. | Campos `sub_status` y `payment_summary.total_paid` persistido. |
| **FR-007** | Bloque de "Itinerario" con fechas pactadas, puerto y salidas/llegadas reales. | Objeto `itinerary` con `actual_departure_at` y `actual_arrival_at`. |
| **FR-008** | Servicios que incluye su reserva en etiquetas para Arrendatario. | Arreglo `vessel.included_services[]`. |
| **FR-009** | Datos del interlocutor (Propietario / Cliente con viajes previos). | Objetos `renter.previous_trips` y `owner.name`. |
| **FR-010** | Monto consolidado en moneda oficial registrada por M3 (COP). | Atributo `payment_summary.currency: "COP"`. |
| **FR-011** | En `RESERVED`, breakdown de tarifa base, seguro obligatorio, depósito y total pagado. | Esquema JSON adaptativo en sección 4.1 y Ejemplo 1. |
| **FR-012** | En `IN_NAVIGATION`, mantener visible breakdown de 3 rubros y total pagado. | Validado en Ejemplo 2. |
| **FR-013** | En `COMPLETED`, ocultar tarifa y seguro, mostrando depósito de garantía exclusivo. | Validado en Ejemplo 3 con `rental_amount: null` y `insurance_amount: null`. |
| **FR-014** | En `CANCELLED (NO_SHOW)`, bloque titulado "Compensación" con nota de liquidación de M3. | Validado en Ejemplo 4 con `section_title: "Compensación"`. |
| **FR-015** | Renderizado semántico de etiquetas y botones según etapa del viaje. | Objeto `status_badge` y `available_actions`. |
| **FR-016** | En `RESERVED` antes del zarpe, banner de tiempo restante y acciones deshabilitadas. | Campos `minutes_to_departure` y `contextual_banner`. |
| **FR-017** | Tras hora pactada, alerta de cortesía en curso con botón de inicio habilitado. | Campo `courtesy_elapsed_minutes` en `time_indicators`. |
| **FR-018** | Tras 30 minutos de cortesía, habilitar botón de marcar inasistencia. | Flag `can_mark_no_show: true` al superar la tolerancia. |
| **FR-019** | En `IN_NAVIGATION`, banner de viaje en curso con botón de marcar fin de navegación. | Flag `can_mark_trip_end: true` y banner de viaje activo. |
| **FR-020** | En No-Show, banner terminal indicando inasistencia y minutos de espera. | Detallado en Ejemplo 4. |
| **FR-021** | En `INITIATED`, proveer acceso para detonar `Iniciar pago`. | Flag `available_actions.can_initiate_payment: true`. |
| **SC-001** | 100% de reservas en `RESERVED` proveen acceso directo al flujo de cancelación. | Validado mediante `can_cancel: true`. |
| **SC-002** | Cero (0%) filtraciones de datos a usuarios no autorizados. | Garantizado por validación estricta de pertenencia y código `403`. |
