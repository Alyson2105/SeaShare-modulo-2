# Contrato de Interfaz REST: CU-21 Ver Detalle de Reserva

## 1. Identificación y Metadatos

- **Identificador del Contrato**: `REST-M2-CU21-VER-DETALLE-RESERVA`
- **Módulo Responsable**: Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones
- **Tipo de Interfaz**: REST sincrónico expuesto por Módulo 2
- **Actor / Consumidor Autorizado**: 
  - `ARRENDATARIO` (únicamente el titular de la reserva)
  - `PROPIETARIO` (únicamente el dueño registrado de la embarcación asociada)
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
   - Valida que el usuario autenticado sea el `arrendatario_id` de la reserva o el `propietario_id` de la embarcación.
   - Cualquier tercero no involucrado es denegado de forma inmediata con `403 Forbidden`.
   - Protege la privacidad de datos cruzados: no expone instrumentos bancarios ni números de tarjeta de crédito de las partes.
2. **Exposición Integral de la Entidad de Dominio (`FR-003`, `FR-007`)**:
   - Entrega los metadatos de la reserva, el itinerario pactado (fechas, horas, puerto de zarpe y zona horaria) y los hitos de ejecución real (`salida_real` y `llegada_real` conforme el viaje transiciona de estados).
   - Provee los servicios incluidos y comodidades de la embarcación en formato de lista para el arrendatario (`FR-008`).
   - Adapta la información del interlocutor: entrega al arrendatario los datos del propietario y entrega al propietario los datos del cliente titular con su récord de viajes previos (`FR-009`).
3. **Regla Estricta "Sin Dinero" (Inmutabilidad Financiera, `FR-004`, `FR-010`)**:
   - Todos los importes monetarios exhibidos provienen literalmente de la liquidación oficial realizada por Módulo 3.
   - Módulo 2 **jamás ejecuta matemáticas locales, descuentos ni estimaciones de penalidades**.
   - En reservas `Canceladas`, expone el total original congelado junto con notas aclaratorias delegando la liquidación contable en Módulo 3 (`FR-006`, `FR-014`).
4. **Resumen de Pago Adaptativo según Estado Operativo**:
   - En `Iniciada`: presenta la cotización estimada previa al pago formal.
   - En `Reservada` y `En Navegación`: exhibe el desglose oficial de tres conceptos vinculantes: "Tarifa base de alquiler", "Seguro obligatorio" y "Depósito de garantía", junto con el "Total pagado" en moneda oficial (`FR-011`, `FR-012`).
   - En `Completada` (ventana de disputa): oculta tarifa y seguro para focalizar la visualización exclusivamente en el "Depósito de garantía" sujeto a resolución operativa (`FR-013`).
   - En `Cancelada por inasistencia`: titula el bloque como "Compensación", mostrando el total original y la leyenda aclaratoria de liquidación (`FR-014`).
5. **Habilitación Dinámica de Acciones y Puntos de Extensión (`FR-005`, `FR-015` a `FR-021`)**:
   - Provee indicadores booleanos (`puede_cancelar`, `puede_iniciar_pago`, `puede_marcar_inicio_navegacion`, `puede_marcar_inasistencia`, `puede_marcar_fin_navegacion`) que determinan los botones e interacciones activas en la interfaz del cliente.
   - Computa contadores de tiempo en vivo: segundos restantes del TTL (15 min), margen de cortesía de espera en muelle (30 min) y ventana para radicar disputas de daños (24 h).

---

## 3. Definición del Endpoint

- **Método HTTP**: `GET`
- **Ruta**: `/api/v1/reservas/{reservaId}`
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
| `reservaId` | String (UUIDv4) | Obligatorio | Identificador universal único de la reserva a consultar. |

---

## 4. Estructura de Datos (Schema JSON)

### 4.1 Cuerpo de Respuesta Exitosa (`200 OK`)

```json
{
  "reserva_id": "c1f7a8b2-5e4d-4c3b-8a1e-9f0a2b3c4d5e",
  "codigo_reserva": "#RS-4492",
  "estado": "Reservada",
  "sub_estado": null,
  "insignia_estado": {
    "texto": "Confirmada",
    "color_semantico": "verde",
    "descripcion": "Pago confirmado y lista para zarpe"
  },
  "embarcacion": {
    "embarcacion_id": "b3e2a1d0-4f5c-6b7a-8e9f-0a1b2c3d4e5f",
    "nombre": "Catamarán Sea Breeze",
    "tipo": "Catamarán",
    "imagen_url": "https://cdn.seashare.com/images/boats/sea-breeze-main.jpg",
    "servicios_incluidos": [
      "GPS Náutico",
      "Chalecos salvavidas certificados",
      "Nevera con hielo",
      "Capitán certificado",
      "Equipo de sonido Bluetooth"
    ]
  },
  "itinerario": {
    "fecha_embarque_pactada": "2026-11-20T09:00:00-05:00",
    "fecha_desembarque_pactada": "2026-11-22T18:00:00-05:00",
    "salida_real": null,
    "llegada_real": null,
    "puerto_zarpe": {
      "nombre": "Marina Santa Marta",
      "ciudad": "Santa Marta",
      "zona_horaria": "America/Bogota"
    },
    "duracion_dias": 3,
    "duracion_noches": 2,
    "pasajeros": 4
  },
  "arrendatario": {
    "arrendatario_id": "u1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
    "nombre": "Laura Gómez",
    "viajes_previos": 5
  },
  "propietario": {
    "propietario_id": "p9a8b7c6-d5e4-3f2a-1b0c-9d8e7f6a5b4c",
    "nombre": "Carlos Mendoza"
  },
  "resumen_pago": {
    "titulo_seccion": "Resumen de pago",
    "moneda": "COP",
    "tarifa_base": 2800000.00,
    "seguro_obligatorio": 350000.00,
    "deposito_garantia": 300000.00,
    "total_pagado": 3450000.00,
    "nota_aclaratoria": null
  },
  "acciones_disponibles": {
    "puede_cancelar": true,
    "puede_iniciar_pago": false,
    "puede_marcar_inicio_navegacion": false,
    "puede_marcar_inasistencia": false,
    "puede_marcar_fin_navegacion": false
  },
  "indicadores_temporales": {
    "ttl_restante_segundos": null,
    "minutos_restantes_zarpe": 15850,
    "cortesia_transcurrida_minutos": null,
    "ventana_disputa_restante_segundos": null
  },
  "banner_contextual": {
    "tipo": "informativo",
    "titulo": "Reserva confirmada",
    "mensaje": "La embarcación estará alistada en el muelle de Marina Santa Marta a las 09:00 AM."
  },
  "created_at": "2026-10-09T10:00:00-05:00"
}
```

#### Descripción de Campos de Salida

| Campo | Tipo | Nulabilidad | Descripción |
| :--- | :--- | :--- | :--- |
| `reserva_id` | String (UUID) | No nulo | Identificador universal único de la reserva. |
| `codigo_reserva` | String | No nulo | Código de referencia náutica (ej. `"#RS-4492"`). |
| `estado` | String (Enum) | No nulo | Estado operativo principal (`"Iniciada"`, `"Pendiente de Pago"`, `"Reservada"`, `"En Navegación"`, `"Completada"`, `"Cancelada"`, `"Expirada"`, `"Pago Fallido"`). |
| `sub_estado` | String (Enum) | Nulo condicional | Sub-clasificación contractual en reservas canceladas (`"Flexible"`, `"Moderado"`, `"Tardío"`, `"Por Propietario"`, `"Por Inasistencia"`). Nulo en otros estados. |
| `insignia_estado` | Objeto | No nulo | Datos semánticos de visualización (`texto`, `color_semantico`, `descripcion`). |
| `embarcacion` | Objeto | No nulo | Ficha técnica resumida del activo y sus comodidades incluidas (`servicios_incluidos[]`). |
| `itinerario` | Objeto | No nulo | Detalle cronológico pactado y tiempos reales de navegación (`salida_real`, `llegada_real`, `puerto_zarpe`). |
| `arrendatario` | Objeto | No nulo | Datos del cliente titular y viajes históricos en la plataforma (`viajes_previos`). |
| `propietario` | Objeto | No nulo | Datos del anfitrión propietario de la embarcación. |
| `resumen_pago` | Objeto | No nulo | Desglose financiero oficial de solo lectura emitido por M3. Se adapta según el estado de la reserva. |
| `resumen_pago.tarifa_base` | Number / null | Nulo condicional | Costo del alquiler (oculto en `Completada` durante disputa de garantía). |
| `resumen_pago.seguro_obligatorio` | Number / null | Nulo condicional | Costo de la póliza de seguro marítimo obligatorio. |
| `resumen_pago.deposito_garantia` | Number / null | Nulo condicional | Depósito de custodia (10% tarifa base). Siempre visible en `Reservada`, `En Navegación` y `Completada`. |
| `resumen_pago.total_pagado` | Number | No nulo | Total pagado oficial y congelado en moneda local. |
| `resumen_pago.nota_aclaratoria` | String / null | Nulo condicional | Advertencia legal (ej. `"El sistema de pagos gestionará la compensación al propietario."`). |
| `acciones_disponibles` | Objeto | No nulo | Flags booleanos que orquestan los botones interactivos de la interfaz según rol y estado. |
| `indicadores_temporales`| Objeto | No nulo | Métricas en segundos/minutos para renderizar contadores regresivos en la UI. |
| `banner_contextual` | Objeto | Nulo condicional | Información destacada de estado para banners superiores en la pantalla. |
| `created_at` | String (ISO 8601) | No nulo | Marca de tiempo de registro inicial de la reserva. |

---

## 5. Ejemplos de Petición y Respuesta

### Ejemplo 1: Reserva en estado `Reservada` (Previo al inicio, acción de cancelación disponible)

#### Petición HTTP (`curl`)
```bash
curl -X GET "https://api.seashare.com/api/v1/reservas/c1f7a8b2-5e4d-4c3b-8a1e-9f0a2b3c4d5e" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Accept: application/json"
```

#### Respuesta Exitosa (`200 OK`)
```json
{
  "reserva_id": "c1f7a8b2-5e4d-4c3b-8a1e-9f0a2b3c4d5e",
  "codigo_reserva": "#RS-4492",
  "estado": "Reservada",
  "sub_estado": null,
  "insignia_estado": {
    "texto": "Confirmada",
    "color_semantico": "verde",
    "descripcion": "Reserva lista para zarpe"
  },
  "embarcacion": {
    "embarcacion_id": "b3e2a1d0-4f5c-6b7a-8e9f-0a1b2c3d4e5f",
    "nombre": "Catamarán Sea Breeze",
    "tipo": "Catamarán",
    "imagen_url": "https://cdn.seashare.com/images/boats/sea-breeze-main.jpg",
    "servicios_incluidos": [
      "GPS Náutico",
      "Chalecos salvavidas certificados",
      "Nevera con hielo",
      "Capitán certificado"
    ]
  },
  "itinerario": {
    "fecha_embarque_pactada": "2026-11-20T09:00:00-05:00",
    "fecha_desembarque_pactada": "2026-11-22T18:00:00-05:00",
    "salida_real": null,
    "llegada_real": null,
    "puerto_zarpe": {
      "nombre": "Marina Santa Marta",
      "ciudad": "Santa Marta",
      "zona_horaria": "America/Bogota"
    },
    "duracion_dias": 3,
    "duracion_noches": 2,
    "pasajeros": 4
  },
  "arrendatario": {
    "arrendatario_id": "u1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
    "nombre": "Laura Gómez",
    "viajes_previos": 5
  },
  "propietario": {
    "propietario_id": "p9a8b7c6-d5e4-3f2a-1b0c-9d8e7f6a5b4c",
    "nombre": "Carlos Mendoza"
  },
  "resumen_pago": {
    "titulo_seccion": "Resumen de pago",
    "moneda": "COP",
    "tarifa_base": 2800000.00,
    "seguro_obligatorio": 350000.00,
    "deposito_garantia": 300000.00,
    "total_pagado": 3450000.00,
    "nota_aclaratoria": null
  },
  "acciones_disponibles": {
    "puede_cancelar": true,
    "puede_iniciar_pago": false,
    "puede_marcar_inicio_navegacion": false,
    "puede_marcar_inasistencia": false,
    "puede_marcar_fin_navegacion": false
  },
  "indicadores_temporales": {
    "ttl_restante_segundos": null,
    "minutos_restantes_zarpe": 15850,
    "cortesia_transcurrida_minutos": null,
    "ventana_disputa_restante_segundos": null
  },
  "banner_contextual": {
    "tipo": "informativo",
    "titulo": "Reserva confirmada",
    "mensaje": "La embarcación estará alistada en el muelle a la hora pactada."
  },
  "created_at": "2026-10-09T10:00:00-05:00"
}
```

---

### Ejemplo 2: Reserva en estado `En Navegación` (Viaje en curso, salida real asentada)

#### Petición HTTP (`curl`)
```bash
curl -X GET "https://api.seashare.com/api/v1/reservas/a9b8c7d6-e5f4-3a2b-1c0d-9e8f7a6b5c4d" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Accept: application/json"
```

#### Respuesta Exitosa (`200 OK`)
```json
{
  "reserva_id": "a9b8c7d6-e5f4-3a2b-1c0d-9e8f7a6b5c4d",
  "codigo_reserva": "#RS-4480",
  "estado": "En Navegación",
  "sub_estado": null,
  "insignia_estado": {
    "texto": "En navegación",
    "color_semantico": "amarillo",
    "descripcion": "Embarcación en el mar"
  },
  "embarcacion": {
    "embarcacion_id": "b3e2a1d0-4f5c-6b7a-8e9f-0a1b2c3d4e5f",
    "nombre": "Catamarán Sea Breeze",
    "tipo": "Catamarán",
    "imagen_url": "https://cdn.seashare.com/images/boats/sea-breeze-main.jpg",
    "servicios_incluidos": [
      "GPS Náutico",
      "Chalecos salvavidas",
      "Nevera"
    ]
  },
  "itinerario": {
    "fecha_embarque_pactada": "2026-10-09T08:00:00-05:00",
    "fecha_desembarque_pactada": "2026-10-09T17:00:00-05:00",
    "salida_real": "2026-10-09T08:05:22-05:00",
    "llegada_real": null,
    "puerto_zarpe": {
      "nombre": "Marina Santa Marta",
      "ciudad": "Santa Marta",
      "zona_horaria": "America/Bogota"
    },
    "duracion_dias": 1,
    "duracion_noches": 0,
    "pasajeros": 6
  },
  "arrendatario": {
    "arrendatario_id": "u9z8y7x6-w5v4-3u2t-1s0r-9q8p7o6n5m4l",
    "nombre": "Mateo Rossi",
    "viajes_previos": 2
  },
  "propietario": {
    "propietario_id": "p9a8b7c6-d5e4-3f2a-1b0c-9d8e7f6a5b4c",
    "nombre": "Carlos Mendoza"
  },
  "resumen_pago": {
    "titulo_seccion": "Resumen de pago",
    "moneda": "COP",
    "tarifa_base": 1400000.00,
    "seguro_obligatorio": 250000.00,
    "deposito_garantia": 150000.00,
    "total_pagado": 1800000.00,
    "nota_aclaratoria": null
  },
  "acciones_disponibles": {
    "puede_cancelar": false,
    "puede_iniciar_pago": false,
    "puede_marcar_inicio_navegacion": false,
    "puede_marcar_inasistencia": false,
    "puede_marcar_fin_navegacion": true
  },
  "indicadores_temporales": {
    "ttl_restante_segundos": null,
    "minutos_restantes_zarpe": 0,
    "cortesia_transcurrida_minutos": null,
    "ventana_disputa_restante_segundos": null
  },
  "banner_contextual": {
    "tipo": "activo",
    "titulo": "El viaje está en curso",
    "mensaje": "Navegación iniciada a las 08:05 AM. Se espera el atraque antes de las 17:00."
  },
  "created_at": "2026-10-08T16:00:00-05:00"
}
```

---

### Ejemplo 3: Reserva en estado `Completada` (Ventana de 24h para disputa de garantía)

#### Petición HTTP (`curl`)
```bash
curl -X GET "https://api.seashare.com/api/v1/reservas/f5a6b7c8-9d0e-1f2a-3b4c-5d6e7f8a9b0c" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Accept: application/json"
```

#### Respuesta Exitosa (`200 OK`)
```json
{
  "reserva_id": "f5a6b7c8-9d0e-1f2a-3b4c-5d6e7f8a9b0c",
  "codigo_reserva": "#RS-2950",
  "estado": "Completada",
  "sub_estado": null,
  "insignia_estado": {
    "texto": "Completada",
    "color_semantico": "verde",
    "descripcion": "Viaje finalizado exitosamente"
  },
  "embarcacion": {
    "embarcacion_id": "b3e2a1d0-4f5c-6b7a-8e9f-0a1b2c3d4e5f",
    "nombre": "Catamarán Sea Breeze",
    "tipo": "Catamarán",
    "imagen_url": "https://cdn.seashare.com/images/boats/sea-breeze-main.jpg",
    "servicios_incluidos": [
      "GPS Náutico",
      "Chalecos salvavidas certificados"
    ]
  },
  "itinerario": {
    "fecha_embarque_pactada": "2026-09-10T10:00:00-05:00",
    "fecha_desembarque_pactada": "2026-09-11T16:00:00-05:00",
    "salida_real": "2026-09-10T10:12:00-05:00",
    "llegada_real": "2026-09-11T15:50:30-05:00",
    "puerto_zarpe": {
      "nombre": "Marina Santa Marta",
      "ciudad": "Santa Marta",
      "zona_horaria": "America/Bogota"
    },
    "duracion_dias": 2,
    "duracion_noches": 1,
    "pasajeros": 4
  },
  "arrendatario": {
    "arrendatario_id": "u1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
    "nombre": "Laura Gómez",
    "viajes_previos": 4
  },
  "propietario": {
    "propietario_id": "p9a8b7c6-d5e4-3f2a-1b0c-9d8e7f6a5b4c",
    "nombre": "Carlos Mendoza"
  },
  "resumen_pago": {
    "titulo_seccion": "Garantía en custodia",
    "moneda": "COP",
    "tarifa_base": null,
    "seguro_obligatorio": null,
    "deposito_garantia": 200000.00,
    "total_pagado": 2300000.00,
    "nota_aclaratoria": "Depósito en ventana de evaluación (24h). De no presentarse reclamos, será liberado al arrendatario por Módulo 3."
  },
  "acciones_disponibles": {
    "puede_cancelar": false,
    "puede_iniciar_pago": false,
    "puede_marcar_inicio_navegacion": false,
    "puede_marcar_inasistencia": false,
    "puede_marcar_fin_navegacion": false
  },
  "indicadores_temporales": {
    "ttl_restante_segundos": null,
    "minutos_restantes_zarpe": 0,
    "cortesia_transcurrida_minutos": null,
    "ventana_disputa_restante_segundos": 43200
  },
  "banner_contextual": {
    "tipo": "informativo",
    "titulo": "Viaje completado",
    "mensaje": "La embarcación ha sido devuelta y atracada. Quedan 12 horas para el cierre automático de la ventana de inspección."
  },
  "created_at": "2026-09-02T11:00:00-05:00"
}
```

---

### Ejemplo 4: Reserva `Cancelada` con sub-estado `Por Inasistencia` (No-Show)

#### Petición HTTP (`curl`)
```bash
curl -X GET "https://api.seashare.com/api/v1/reservas/8b9c0d1e-2f3a-4b5c-6d7e-8f9a0b1c2d3e" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Accept: application/json"
```

#### Respuesta Exitosa (`200 OK`)
```json
{
  "reserva_id": "8b9c0d1e-2f3a-4b5c-6d7e-8f9a0b1c2d3e",
  "codigo_reserva": "#RS-1904",
  "estado": "Cancelada",
  "sub_estado": "Por Inasistencia",
  "insignia_estado": {
    "texto": "Cancelado por inasistencia",
    "color_semantico": "rojo",
    "descripcion": "El arrendatario no se presentó tras cumplirse la cortesía de 30 minutos"
  },
  "embarcacion": {
    "embarcacion_id": "b3e2a1d0-4f5c-6b7a-8e9f-0a1b2c3d4e5f",
    "nombre": "Catamarán Sea Breeze",
    "tipo": "Catamarán",
    "imagen_url": "https://cdn.seashare.com/images/boats/sea-breeze-main.jpg",
    "servicios_incluidos": [
      "GPS Náutico",
      "Chalecos salvavidas certificados"
    ]
  },
  "itinerario": {
    "fecha_embarque_pactada": "2026-08-15T09:00:00-05:00",
    "fecha_desembarque_pactada": "2026-08-15T18:00:00-05:00",
    "salida_real": null,
    "llegada_real": null,
    "puerto_zarpe": {
      "nombre": "Marina Santa Marta",
      "ciudad": "Santa Marta",
      "zona_horaria": "America/Bogota"
    },
    "duracion_dias": 1,
    "duracion_noches": 0,
    "pasajeros": 4
  },
  "arrendatario": {
    "arrendatario_id": "u4b5c6d7-e8f9-0a1b-2c3d-4e5f6a7b8c9d",
    "nombre": "Andrés Gómez",
    "viajes_previos": 1
  },
  "propietario": {
    "propietario_id": "p9a8b7c6-d5e4-3f2a-1b0c-9d8e7f6a5b4c",
    "nombre": "Carlos Mendoza"
  },
  "resumen_pago": {
    "titulo_seccion": "Compensación",
    "moneda": "COP",
    "tarifa_base": null,
    "seguro_obligatorio": null,
    "deposito_garantia": null,
    "total_pagado": 1600000.00,
    "nota_aclaratoria": "El sistema de pagos (Módulo 3) gestionará la compensación al propietario según las políticas de No-Show."
  },
  "acciones_disponibles": {
    "puede_cancelar": false,
    "puede_iniciar_pago": false,
    "puede_marcar_inicio_navegacion": false,
    "puede_marcar_inasistencia": false,
    "puede_marcar_fin_navegacion": false
  },
  "indicadores_temporales": {
    "ttl_restante_segundos": null,
    "minutos_restantes_zarpe": 0,
    "cortesia_transcurrida_minutos": 30,
    "ventana_disputa_restante_segundos": null
  },
  "banner_contextual": {
    "tipo": "alerta",
    "titulo": "Reserva cancelada por inasistencia",
    "mensaje": "Se cumplió la ventana de cortesía de 30 minutos sin presentación del cliente. Inasistencia reportada por el propietario Carlos Mendoza."
  },
  "created_at": "2026-08-10T09:00:00-05:00"
}
```

---

## 6. Respuestas de Error y Códigos HTTP

Todos los errores retornan un sobre uniforme con `codigo` y `mensaje`:

```json
{
  "codigo": "CODIGO_ERROR",
  "mensaje": "Descripción detallada del motivo de rechazo."
}
```

| Código HTTP | Código Interno (`codigo`) | Causa / Condición de Disparo | Cuerpo de Respuesta de Ejemplo |
| :--- | :--- | :--- | :--- |
| **`400 Bad Request`** | `INVALID_UUID` | El parámetro de ruta `reservaId` no cumple el estándar UUIDv4. | `{"codigo": "INVALID_UUID", "mensaje": "El identificador de reserva proporcionado no tiene un formato UUID válido."}` |
| **`401 Unauthorized`** | `AUTH_TOKEN_MISSING_OR_INVALID` | El header `Authorization` no fue provisto o el token JWT expiró. | `{"codigo": "AUTH_TOKEN_MISSING_OR_INVALID", "mensaje": "Token de autenticación ausente o inválido."}` |
| **`403 Forbidden`** | `FORBIDDEN_NOT_AUTHORIZED` | El usuario autenticado no es el Arrendatario titular ni el Propietario del activo náutico asociado (`SC-002`). | `{"codigo": "FORBIDDEN_NOT_AUTHORIZED", "mensaje": "Acceso denegado: no está autorizado para consultar los detalles de una reserva en la que no participa activamente."}` |
| **`404 Not Found`** | `RESERVATION_NOT_FOUND` | No existe ninguna reserva en Módulo 2 con el `reservaId` indicado. | `{"codigo": "RESERVATION_NOT_FOUND", "mensaje": "No se encontró ninguna reserva asociada al identificador proporcionado."}` |
| **`500 Internal Server Error`** | `INTERNAL_SERVER_ERROR` | Fallo inesperado en base de datos al recuperar la información. | `{"codigo": "INTERNAL_SERVER_ERROR", "mensaje": "Error interno del servidor al procesar la consulta del detalle de la reserva."}` |

---

## 7. Notas Transversales y Reglas de Negocio Arquitecturales

### 7.1 Regla de Oro "Sin Dinero" (Inmutabilidad Financiera)
El objeto `resumen_pago` refleja de manera fidedigna los importes liquidados por Módulo 3 y almacenados de manera inmutable en la entidad `Reserva`.
- Módulo 2 no efectúa sumas, redondeos de impuestos, conversiones de divisas ni tasas de descuento.
- En estado `Completada`, la vista se centra en el `deposito_garantia` en custodia para la resolución de daños.
- En estado `Cancelada`, no se calculan montos de penalidad en la respuesta: el sistema muestra el valor total original pagado y delega la cifra de dispersión a Módulo 3.

### 7.2 Orquestación Visual de Acciones en Muelle
El objeto `acciones_disponibles` abstrae la complejidad temporal y de roles:
- El botón de **Cancelar** solo se habilita (`puede_cancelar: true`) si el estado es exactamente `Reservada` (previo al inicio de navegación).
- Los botones de **Inicio de Navegación** e **Inasistencia** se habilitan únicamente para el Propietario, calculando contra el huso horario oficial del puerto amarrado en la reserva.

---

## 8. Trazabilidad de Requisitos

| Requisito Funcional / Criterio | Descripción en Spec | Cobertura en este Contrato |
| :--- | :--- | :--- |
| **FR-001** | Recibir el identificador de la reserva específica como parámetro (`<<extend>>` de Ver mis reservas). | Path parameter `reservaId` en `/api/v1/reservas/{reservaId}`. |
| **FR-002** | Validar autorización del usuario en sesión (Arrendatario o Propietario). | Verificación JWT y código `403 FORBIDDEN_NOT_AUTHORIZED`. |
| **FR-003** | Exponer ID, fechas, horas, estado, sub-estado y desglose monetario guardado. | Estructura JSON completa en sección 4.1. |
| **FR-004** | Todos los valores financieros son de solo lectura y reflejan lo devuelto por Módulo 3. | Regla documentada en sección 2 y 7.1. Cero matemáticas locales. |
| **FR-005** | En `Reservada`, proveer punto de acceso para detonar `Solicitar cancelación`. | Flag `acciones_disponibles.puede_cancelar: true` en estado `Reservada`. |
| **FR-006** | En `Cancelada`, mostrar sub-estado exacto junto con el monto total original. | Campos `sub_estado` y `resumen_pago.total_pagado` persistido. |
| **FR-007** | Bloque de "Itinerario" con fechas pactadas, puerto y salidas/llegadas reales. | Objeto `itinerario` con `salida_real` y `llegada_real`. |
| **FR-008** | Servicios que incluye su reserva en etiquetas para Arrendatario. | Arreglo `embarcacion.servicios_incluidos[]`. |
| **FR-009** | Datos del interlocutor (Propietario / Cliente con viajes previos). | Objetos `arrendatario.viajes_previos` y `propietario.nombre`. |
| **FR-010** | Monto consolidado en moneda oficial registrada por M3 (COP). | Atributo `resumen_pago.moneda: "COP"`. |
| **FR-011** | En `Reservada`, desglose de tarifa base, seguro obligatorio, depósito y total pagado. | Esquema JSON adaptativo en sección 4.1 y Ejemplo 1. |
| **FR-012** | En `En Navegación`, mantener visible desglose de 3 rubros y total pagado. | Validado en Ejemplo 2. |
| **FR-013** | En `Completada`, ocultar tarifa y seguro, mostrando depósito de garantía exclusivo. | Validado en Ejemplo 3 con `tarifa_base: null` y `seguro_obligatorio: null`. |
| **FR-014** | En `Cancelada por inasistencia`, bloque titulado "Compensación" con nota de liquidación de M3. | Validado en Ejemplo 4 con `titulo_seccion: "Compensación"`. |
| **FR-015** | Renderizado semántico de etiquetas y botones según etapa del viaje. | Objeto `insignia_estado` y `acciones_disponibles`. |
| **FR-016** | En `Reservada` antes del zarpe, banner de tiempo restante y acciones deshabilitadas. | Campos `minutos_restantes_zarpe` y `banner_contextual`. |
| **FR-017** | Tras hora pactada, alerta de cortesía en curso con botón de inicio habilitado. | Campo `cortesia_transcurrida_minutos` en `indicadores_temporales`. |
| **FR-018** | Tras 30 minutos de cortesía, habilitar botón de marcar inasistencia. | Flag `puede_marcar_inasistencia: true` al superar la tolerancia. |
| **FR-019** | En `En Navegación`, banner de viaje en curso con botón de marcar fin de navegación. | Flag `puede_marcar_fin_navegacion: true` y banner de viaje activo. |
| **FR-020** | En No-Show, banner terminal indicando inasistencia y minutos de espera. | Detallado en Ejemplo 4. |
| **FR-021** | En `Iniciada`, proveer acceso para detonar `Iniciar pago`. | Flag `acciones_disponibles.puede_iniciar_pago: true`. |
| **SC-001** | 100% de reservas en `Reservada` proveen acceso directo al flujo de cancelación. | Validado mediante `puede_cancelar: true`. |
| **SC-002** | Cero (0%) filtraciones de datos a usuarios no autorizados. | Garantizado por validación estricta de pertenencia y código `403`. |
