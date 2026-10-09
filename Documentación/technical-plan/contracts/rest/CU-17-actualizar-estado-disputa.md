# Contrato de Interfaz REST: CU-17 Actualizar Estado de Disputa

## 1. Identificación y Metadatos

- **Identificador del Contrato**: `REST-M2-CU17-ACTUALIZAR-ESTADO-DISPUTA`
- **Módulo Responsable**: Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones
- **Tipo de Interfaz**: REST sincrónico expuesto por Módulo 2
- **Actor / Consumidor Autorizado**: `ADMIN` (Administrador de la plataforma SEA-SHARE)
- **Caso de Uso Base / Relaciones**: 
  - Caso de uso: `CU-17 Actualizar estado de disputa de garantía`
  - Incluido por: `Generar disputa de garantía` (`CU-16`, `<<include>>`), que delega aquí tanto el registro inicial como el cierre automático por vencimiento de 24 horas.
  - Gatilla: `Recibir información de disputa de garantía` (`CU-18`, evento AMQP saliente hacia Módulo 3) ante estados finales `ACEPTADA` o `RECHAZADA`.
- **Versión del Contrato**: 1.0.0
- **Fecha de Creación**: 2026-10-09

---

## 2. Propósito y Alcance del Endpoint

Este endpoint procesa la resolución administrativa emitida por el Administrador sobre una disputa de garantía abierta, actualizando formalmente su estado a `ACEPTADA` o `RECHAZADA` (o manteniendo `PENDIENTE` si la investigación sigue en curso), y desencadenando la notificación asíncrona hacia Módulo 3 para la ejecución financiera del depósito.

### Responsabilidades del Endpoint:
1. **Control de Autorización Estricto (`FR-001`, `SC-001`)**:
   - Exige obligatoriamente el rol `ADMIN` en el token JWT. Propietarios, arrendatarios o usuarios no autenticados son rechazados de inmediato con `403 Forbidden`.
2. **Validación de la Máquina de Estados de la Disputa (`FR-001`, `FR-002`, `FR-003`, `SC-002`)**:
   - Solo se permite actualizar disputas que se encuentren en estado `PENDIENTE`.
   - Si la disputa ya se encuentra en `ACEPTADA` o `RECHAZADA` (sea por resolución previa o por cierre automático de 24h), el intento de modificación se rechaza con `409 Conflict`. `ACEPTADA` y `RECHAZADA` son **estados terminales inmutables sin posibilidad de reapertura**.
   - Los únicos estados destino permitidos son:
     - `ACEPTADA`: El reclamo del Propietario procede; el depósito se liquidará al Propietario.
     - `RECHAZADA`: El reclamo no procede; el depósito se reembolsará íntegro al Arrendatario.
     - `PENDIENTE`: La revisión administrativa continúa en curso (registra actividad sin emitir eventos).
3. **Registro Informativo del Motivo de Resolución (`FR-004`, `SC-004`)**:
   - El Administrador puede adjuntar un campo `motivo` de texto libre (opcional).
   - Este motivo es de naturaleza estrictamente informativa y de trazabilidad de auditoría; **no condiciona ni altera importes financieros**.
4. **Regla Estricta "Sin Dinero" (`FR-005`, `SC-003`)**:
   - El Administrador y Módulo 2 **no introducen montos, no ejecutan transferencias de dinero ni interactúan con pasarelas de pago**. Toda la liquidación monetaria pertenece en forma exclusiva a Módulo 3.
5. **Publicación Desacoplada hacia Módulo 3 (`FR-007`)**:
   - Cuando la transición alcanza un estado final (`ACEPTADA` o `RECHAZADA`), el endpoint confirma la transacción local y emite el evento de dominio a RabbitMQ (`CU-18-disputa-garantia`) con garantías de entrega mediante patrón outbox.
   - Si el estado destino es `PENDIENTE`, **no se emite ningún mensaje** hacia Módulo 3.
6. **Cumplimiento de SLA Operativo (`FR-013`)**:
   - El SLA fijado para la resolución de disputas administrativas es de **24 horas**.

---

## 3. Definición del Endpoint

- **Método HTTP**: `PUT`
- **Ruta**: `/api/v1/disputas/{disputaId}/estado`
- **Formato de Petición / Respuesta**: `application/json`
- **Codificación**: `UTF-8`

### 3.1 Encabezados HTTP (Headers)

| Header | Tipo | Obligatorio | Descripción |
| :--- | :--- | :--- | :--- |
| `Authorization` | String | Sí | Token Bearer JWT con rol `ROLE_ADMIN` (`Bearer eyJhbG...`). |
| `Content-Type` | String | Sí | Debe ser `application/json; charset=utf-8`. |

### 3.2 Parámetros de Ruta (Path Parameters)

| Parámetro | Tipo | Restricción | Descripción |
| :--- | :--- | :--- | :--- |
| `disputaId` | String (UUIDv4) | Obligatorio | Identificador universal único de la disputa de garantía a resolver. |

---

## 4. Estructura de Datos (Schemas JSON)

### 4.1 Cuerpo de la Petición (Request Body)

```json
{
  "nuevo_estado": "ACEPTADA | RECHAZADA | PENDIENTE",
  "motivo": "string (opcional, máximo 1000 caracteres)",
  "notas_internas": "string (opcional, máximo 1000 caracteres, solo para auditoría interna)"
}
```

#### Descripción de Campos de Entrada

| Campo | Tipo | Obligatoriedad | Descripción / Reglas |
| :--- | :--- | :--- | :--- |
| `nuevo_estado` | String (Enum) | **Obligatorio** | Estado destino: `"ACEPTADA"`, `"RECHAZADA"` o `"PENDIENTE"`. |
| `motivo` | String | Opcional | Justificación textual de la decisión administrativa. Solo informativo y visible para las partes. |
| `notas_internas` | String | Opcional | Notas privadas de auditoría visibles únicamente por el equipo administrativo. |

### 4.2 Cuerpo de Respuesta Exitosa (`200 OK`)

```json
{
  "disputa_id": "d1a2b3c4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
  "reserva_id": "c1f7a8b2-5e4d-4c3b-8a1e-9f0a2b3c4d5e",
  "estado_anterior": "PENDIENTE",
  "nuevo_estado": "ACEPTADA | RECHAZADA | PENDIENTE",
  "resuelto_por": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
  "fecha_resolucion": "2026-10-09T16:45:00-05:00",
  "motivo": "string | null",
  "evento_publicado_m3": true,
  "mensaje": "Disputa resuelta exitosamente. Módulo 3 ha sido notificado para la ejecución financiera del depósito de garantía."
}
```

#### Descripción de Campos de Salida

| Campo | Tipo | Nulabilidad | Descripción |
| :--- | :--- | :--- | :--- |
| `disputa_id` | String (UUID) | No nulo | Identificador de la disputa de garantía. |
| `reserva_id` | String (UUID) | No nulo | Identificador de la reserva náutica asociada. |
| `estado_anterior` | String (Enum) | No nulo | Estado previo al cambio (`"PENDIENTE"`). |
| `nuevo_estado` | String (Enum) | No nulo | Estado formal asignado (`"ACEPTADA"`, `"RECHAZADA"` o `"PENDIENTE"`). |
| `resuelto_por` | String (UUID) | No nulo | Identificador del Administrador que firmó la resolución. |
| `fecha_resolucion` | String (ISO 8601) | No nulo | Marca temporal oficial de la resolución. |
| `motivo` | String | Nulo condicional | Justificación informativa registrada. |
| `evento_publicado_m3` | Boolean | No nulo | `true` si se emitió el evento a RabbitMQ (`ACEPTADA`/`RECHAZADA`); `false` si quedó en `PENDIENTE`. |
| `mensaje` | String | No nulo | Resumen textual confirmatorio de la operación. |

---

## 5. Ejemplos de Petición y Respuesta

### Ejemplo 1: Resolución favorable al Propietario (`ACEPTADA`)

#### Petición HTTP (`curl`)
```bash
curl -X PUT "https://api.seashare.com/api/v1/disputas/d1a2b3c4-e5f6-7a8b-9c0d-1e2f3a4b5c6d/estado" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "nuevo_estado": "ACEPTADA",
    "motivo": "Se verificó evidencia fotográfica de impacto en el timón incompatible con el desgaste normal de uso. Reclamo procedente.",
    "notas_internas": "Inspección técnica validada contra el acta de entrega previa."
  }'
```

#### Respuesta Exitosa (`200 OK`)
```json
{
  "disputa_id": "d1a2b3c4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
  "reserva_id": "c1f7a8b2-5e4d-4c3b-8a1e-9f0a2b3c4d5e",
  "estado_anterior": "PENDIENTE",
  "nuevo_estado": "ACEPTADA",
  "resuelto_por": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
  "fecha_resolucion": "2026-10-09T16:45:00-05:00",
  "motivo": "Se verificó evidencia fotográfica de impacto en el timón incompatible con el desgaste normal de uso. Reclamo procedente.",
  "evento_publicado_m3": true,
  "mensaje": "Disputa resuelta como ACEPTADA. Módulo 3 ha sido notificado para liquidar el 100% del depósito de garantía al Propietario."
}
```

---

### Ejemplo 2: Resolución desfavorable al Propietario (`RECHAZADA` con motivo)

#### Petición HTTP (`curl`)
```bash
curl -X PUT "https://api.seashare.com/api/v1/disputas/d1a2b3c4-e5f6-7a8b-9c0d-1e2f3a4b5c6d/estado" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "nuevo_estado": "RECHAZADA",
    "motivo": "El desgaste reportado en los tapizados corresponde a fatiga normal de material y no a negligencia del arrendatario.",
    "notas_internas": "No se aprecian quemaduras ni cortes intencionales en las fotografías."
  }'
```

#### Respuesta Exitosa (`200 OK`)
```json
{
  "disputa_id": "d1a2b3c4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
  "reserva_id": "c1f7a8b2-5e4d-4c3b-8a1e-9f0a2b3c4d5e",
  "estado_anterior": "PENDIENTE",
  "nuevo_estado": "RECHAZADA",
  "resuelto_por": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
  "fecha_resolucion": "2026-10-09T17:10:00-05:00",
  "motivo": "El desgaste reportado en los tapizados corresponde a fatiga normal de material y no a negligencia del arrendatario.",
  "evento_publicado_m3": true,
  "mensaje": "Disputa resuelta como RECHAZADA. Módulo 3 ha sido notificado para reembolsar el 100% del depósito de garantía al Arrendatario."
}
```

---

### Ejemplo 3: Mantener revisión en curso (`PENDIENTE`)

#### Petición HTTP (`curl`)
```bash
curl -X PUT "https://api.seashare.com/api/v1/disputas/d1a2b3c4-e5f6-7a8b-9c0d-1e2f3a4b5c6d/estado" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "nuevo_estado": "PENDIENTE",
    "notas_internas": "Se solicitó peritaje fotográfico complementario a la administración del puerto."
  }'
```

#### Respuesta Exitosa (`200 OK`)
```json
{
  "disputa_id": "d1a2b3c4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
  "reserva_id": "c1f7a8b2-5e4d-4c3b-8a1e-9f0a2b3c4d5e",
  "estado_anterior": "PENDIENTE",
  "nuevo_estado": "PENDIENTE",
  "resuelto_por": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
  "fecha_resolucion": "2026-10-09T17:30:00-05:00",
  "motivo": null,
  "evento_publicado_m3": false,
  "mensaje": "Revisión en curso registrada. La disputa permanece en estado PENDIENTE. No se emitió ninguna notificación a Módulo 3."
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
| **`400 Bad Request`** | `INVALID_UUID` | El parámetro `disputaId` no es un UUIDv4 válido. | `{"codigo": "INVALID_UUID", "mensaje": "El identificador de disputa proporcionado no es válido."}` |
| **`400 Bad Request`** | `INVALID_STATE_TRANSITION` | El valor de `nuevo_estado` no es uno de `ACEPTADA`, `RECHAZADA` o `PENDIENTE`. | `{"codigo": "INVALID_STATE_TRANSITION", "mensaje": "Estado destino no permitido. Los únicos estados válidos son ACEPTADA, RECHAZADA o PENDIENTE."}` |
| **`401 Unauthorized`** | `AUTH_TOKEN_MISSING_OR_INVALID` | El header `Authorization` no fue provisto o el token expiró. | `{"codigo": "AUTH_TOKEN_MISSING_OR_INVALID", "mensaje": "Token de autenticación ausente o inválido."}` |
| **`403 Forbidden`** | `FORBIDDEN_NOT_ADMIN` | El usuario autenticado no posee el rol `ADMIN` en su token JWT. | `{"codigo": "FORBIDDEN_NOT_ADMIN", "mensaje": "Acceso denegado: solo el perfil Administrador tiene atribuciones para resolver disputas de garantía."}` |
| **`404 Not Found`** | `DISPUTE_NOT_FOUND` | La disputa no existe en la base de datos de Módulo 2. | `{"codigo": "DISPUTE_NOT_FOUND", "mensaje": "No se encontró ninguna disputa de garantía asociada al identificador provisto."}` |
| **`409 Conflict`** | `DISPUTE_ALREADY_FINALIZED` | La disputa ya se encuentra en `ACEPTADA` o `RECHAZADA` (`SC-002`, `FR-003`). No admite reapertura. | `{"codigo": "DISPUTE_ALREADY_FINALIZED", "mensaje": "Conflicto: la disputa ya se encuentra en un estado final inmutable y no puede ser modificada ni reabierta."}` |
| **`500 Internal Server Error`** | `INTERNAL_SERVER_ERROR` | Fallo de base de datos o broker de eventos al confirmar la resolución. | `{"codigo": "INTERNAL_SERVER_ERROR", "mensaje": "Error interno del servidor al procesar la resolución de la disputa."}` |

---

## 7. Notas Transversales y Reglas de Negocio Arquitecturales

### 7.1 Regla de Oro "Sin Dinero" (Zero Financial Calculations)
En estricto cumplimiento de `FR-005` y `SC-003`:
- El Admin no asigna deducciones parciales (ej. retener el 30% del depósito): **no existe retención parcial en SEA-SHARE** (según §3 de `consistencia-m2-m3.md`, el depósito se entrega completo al Arrendatario o completo al Propietario).
- Toda la matemática monetaria y dispersión bancaria es ejecutada autónomamente por Módulo 3.

### 7.2 Inmutabilidad de Estados Finales
Una vez consolidada la transición hacia `ACEPTADA` o `RECHAZADA`:
- La fila queda protegida por triggers o validación de dominio (`disputa.isFinal() == true`).
- Ni el Admin ni ningún proceso de sistema puede revertir o reabrir el caso, garantizando consistencia legal y financiera.

### 7.3 Concurrencia y Carrera con Cierre Automático
Si el Admin envía la resolución exactamente en el instante en que el job programado de 24 horas intenta ejecutar el cierre automático por vencimiento:
- El control de concurrencia optimista (`@Version`) asegura que solo una transacción consolide.
- Si gana el Admin, la disputa pasa a su veredicto. Si gana el job, el Admin recibe `409 Conflict` (`DISPUTE_ALREADY_FINALIZED`), garantizando cero estados inconsistentes.

---

## 8. Trazabilidad de Requisitos

| Requisito Funcional / Criterio | Descripción en Spec | Cobertura en este Contrato |
| :--- | :--- | :--- |
| **FR-001** | Permitir al Admin actualizar el estado si y solo si la disputa está en `PENDIENTE`. | Validación de estado previo y códigos `403` / `409`. |
| **FR-002** | Estados destino permitidos: `PENDIENTE`, `RECHAZADA` o `ACEPTADA`. | Restricción en schema de entrada y código `400 INVALID_STATE_TRANSITION`. |
| **FR-003** | Rechazar cualquier cambio si ya está en `RECHAZADA` o `ACEPTADA` (inmutables). | Código `409 DISPUTE_ALREADY_FINALIZED`. |
| **FR-004** | Motivo opcional, informativo, sin efecto financiero. | Campo `motivo` documentado como informativo en sección 4.1 y 7.1. |
| **FR-005** | Regla estricta "Sin dinero": cero montos ni operaciones de pasarela. | Verificado en schema. Cero montos en entrada o salida. |
| **FR-006** | Cierre automático de CU-16 se ejecuta mediante esta misma operación interna. | Modelo unificado de máquina de estados de disputa. |
| **FR-007** | Publicar a M3 (`CU-18`) cada cambio a `RECHAZADA` o `ACEPTADA`. PENDIENTE no se publica. | Orquestación descrita en sección 2 y campo `evento_publicado_m3`. |
| **FR-008** | Soporte para vista administrativa de resolución de disputas. | Información contextual para la bandeja y menús. |
| **FR-009** | Bloque de cabecera con IDs, nombres de partes y etiqueta de estado. | Información provista en el response. |
| **FR-010** | Panel de reclamo del propietario vs novedades del cierre. | Contexto de soporte documentado. |
| **FR-011** | Campo de texto libre para motivo de resolución informativa. | Campo `motivo` en el payload de entrada. |
| **FR-012** | Botones de acción "Rechazar reclamo" y "Aceptar reclamo". | Mapeados a `nuevo_estado: "RECHAZADA"` y `"ACEPTADA"`. |
| **FR-013** | SLA límite de respuesta a disputas fijado en 24 horas. | Documentado en metadatos y objetivos. |
| **SC-001** | 100% de actualizaciones aplicadas sobre disputas en PENDIENTE. | Garantizado por máquina de estados en backend. |
| **SC-002** | Cero cambios aplicados sobre disputas ya en RECHAZADA o ACEPTADA. | Garantizado por código `409 Conflict`. |
| **SC-003** | Cero montos u operaciones financieras introducidas en este flujo. | Verificado en especificación de datos. |
| **SC-004** | 100% de rechazos con motivo guardado solo como campo informativo. | Confirmado en persistencia. |
