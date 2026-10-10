Contrato de Integración Externa: Solicitar Confirmación de Pago (M3)
Módulo Proveedor: Módulo 3 – Liquidación, Seguros y Dispersión de Fondos ("el sistema")
Responsable de implementarlo: Equipo de Módulo 3 (UC06 Solicitar confirmación de pago)
Módulo Consumidor: Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones ("Sistema de Reservas y Operaciones")
CUs de Módulo 2 que lo consumen:

Invocador principal único: CU-13 Confirmar pago (features/CU-13-confirmar-pago/spec.md), ejecutado como proceso interno programado de M2.
Casos de uso de negocio relacionados: CU-03 Iniciar pago (publica el evento PENDIENTE con el token de pago que dispara el cobro en M3) y CU-08 Actualizar estado de reserva (aplica la transición resultante). Fecha: 2026-10-09

1. Resumen y Propósito de la Integración
Módulo 3 es el único operador financiero de SEA-SHARE: ejecuta el cobro con la pasarela (Mercado Pago) mediante un worker asíncrono a partir del evento reservation.status.changed con status PENDIENTE. Módulo 3 no invoca a Módulo 2; por lo tanto, Módulo 2 conoce el resultado del cobro consultando este endpoint de solo lectura.

Invariantes de Diseño:

Solo lectura: la consulta no crea ni modifica registros en M3 ni contacta a la pasarela.
Regla Estricta "Sin Dinero" en Módulo 2: M2 no interpreta, calcula ni altera los montos que devuelve este endpoint. Los transporta únicamente para auditoría.
Solo un estado APROBADO verificable dentro del TTL permite avanzar la reserva a Reservada. Un estado RECHAZADO, CANCELADO o EXPIRADO nunca se trata como aprobado.
APROBADO no implica que la captura o la liquidación posterior ya se haya ejecutado.
2. Definición del Endpoint
Método HTTP y URL
GET /api/v1/reservations/{reservation_id}/payment-confirmation

Elementos de la Petición (Request)
Headers:

Nombre	Obligatorio	Descripción
Authorization	Sí	Bearer <token> — JWT firmado de identidad de servicio expedido para Módulo 2 [NEEDS CLARIFICATION OQ-01]
Accept	No	application/json
X-Correlation-Id	No	Cadena libre para trazabilidad distribuida
Path Parameters:

Nombre	Tipo	Obligatorio	Descripción
reservation_id	string (UUID v4)	Sí	Identificador de la reserva en Módulo 2 cuyo cobro se consulta
Query Parameters: No tiene.

Body: No tiene (operación GET).

Elementos de la Respuesta Esperada (Response)
Código de estado HTTP (éxito): 200 OK

Contrato de respuesta tipado (plano, snake_case):

{
  "reservation_id": "string (UUID)",
  "status": "string (EN_PROCESO | APROBADO | RECHAZADO | CANCELADO | EXPIRADO | DESCONOCIDO)",
  "detail": "string | null",
  "authorized_amount": "string decimal | null",
  "captured_amount": "string decimal | null",
  "released_amount": "string decimal | null",
  "charged_amount": "string decimal | null",
  "external_reference": "string | null"
}
Matriz de valores de status y reacción de Módulo 2:

status	Significado en M3	Reacción de M2 (CU-13 / CU-08)
EN_PROCESO	La pasarela aún no reportó un resultado definitivo	Sin cambios; vuelve a consultar en el siguiente ciclo
APROBADO	La pasarela aprobó la autorización o el cobro	Con TTL vigente: transición a Reservada. Con la reserva ya Expirada o en Pago Fallido: cobro huérfano, M2 publica el estado EXPIRADA a M3 y genera alerta crítica
RECHAZADO	La pasarela rechazó la operación	Registra el motivo; con TTL vigente la reserva sigue en Pendiente de Pago permitiendo reintento con otro token (CU-03)
CANCELADO	La operación fue cancelada	Transición a Pago Fallido y liberación de la embarcación en M1
EXPIRADO	La autorización venció sin captura	Transición a Pago Fallido y liberación de la embarcación en M1
DESCONOCIDO	Estado externo no determinado (incluye falla de comunicación con la pasarela)	Tratado como EN_PROCESO; se registra para auditoría
Ejemplos de Petición y Respuestas Exitosas
Ejemplo 1 — Cobro aprobado (autorizado, aún sin captura)
Petición curl:

curl -X GET "https://finanzas.seashare.internal/api/v1/reservations/e4f81c92-7a20-4215-9c5e-8812c3f1a001/payment-confirmation" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.m2ServiceToken" \
  -H "Accept: application/json" \
  -H "X-Correlation-Id: 5d1f9c34-8a77-4c1e-9b6a-2f3e4d5c6b7a"
Respuesta (200 OK):

{
  "reservation_id": "e4f81c92-7a20-4215-9c5e-8812c3f1a001",
  "status": "APROBADO",
  "detail": null,
  "authorized_amount": "10040000.00",
  "captured_amount": null,
  "released_amount": null,
  "charged_amount": null,
  "external_reference": "pg-8841"
}
Ejemplo 2 — Cobro en proceso
Respuesta (200 OK):

{
  "reservation_id": "e4f81c92-7a20-4215-9c5e-8812c3f1a001",
  "status": "EN_PROCESO",
  "detail": null,
  "authorized_amount": null,
  "captured_amount": null,
  "released_amount": null,
  "charged_amount": null,
  "external_reference": null
}
Ejemplo 3 — Cobro rechazado
Respuesta (200 OK):

{
  "reservation_id": "e4f81c92-7a20-4215-9c5e-8812c3f1a001",
  "status": "RECHAZADO",
  "detail": "cc_rejected_insufficient_amount",
  "authorized_amount": null,
  "captured_amount": null,
  "released_amount": null,
  "charged_amount": null,
  "external_reference": "pg-8842"
}
3. Matriz de Errores y Reacción de Módulo 2
Los errores de M3 se devuelven como application/problem+json con los campos type, title, status, detail, code y retryable.

Código HTTP M3	code	Causa en Módulo 3	Reacción Arquitectónica de Módulo 2
400 Bad Request	VALIDATION_ERROR	reservation_id con formato inválido	Alerta de integración en observabilidad; no reintentable
401 Unauthorized / 403 Forbidden	UNAUTHENTICATED / FORBIDDEN	Credencial de servicio de M2 ausente, inválida o sin permisos	Alerta crítica de infraestructura; no cambia el estado de la reserva
404 Not Found	CHARGE_INTENT_NOT_FOUND	No existe IntenciónDeCobro: M3 la crea de forma asíncrona al procesar el evento PENDIENTE y puede no haberlo procesado aún	Reintenta en el siguiente ciclo de consulta (reintentable)
500 Internal Server Error / 503 Service Unavailable / Timeout	INTERNAL_ERROR	Falla interna o sobrecarga de M3	Sin cambios de estado; reintenta en el siguiente ciclo
4. Parámetros de Resiliencia, Timeouts y Reintentos
Connect Timeout: 100 ms.
Read Timeout: 500 ms [NEEDS CLARIFICATION: PROPUESTA pendiente de ratificación formal por Módulo 3].
Política de Reintentos: cero reintentos dentro de un mismo ciclo; el reintento natural es el siguiente ciclo del job (cada 5 s, configurable) mientras la reserva siga en Pendiente de Pago con TTL vigente.
Idempotencia: operación de solo lectura; repetir la consulta devuelve el estado vigente en ese instante sin efectos secundarios.
Principio Fail-Safe: ante cualquier error o timeout, M2 nunca asume que el pago fue aprobado ni rechazado; la reserva permanece en su estado actual hasta obtener un status definitivo o hasta que venza el TTL.
5. Puntos Abiertos y Aclaraciones Necesarias
[NEEDS CLARIFICATION: mecanismo de autenticación servicio-a-servicio]: validar el emisor y claim del JWT de servicio de M2 en la cabecera Authorization.
[NEEDS CLARIFICATION: SLA y Timeout de la consulta]: ratificar con Módulo 3 la propuesta de SLA < 300 ms y Read Timeout 500 ms.
[NEEDS CLARIFICATION: Estado EXPIRADA en el evento de estado]: para reembolsar un cobro aprobado después de que M2 expiró la reserva, M3 debe aceptar el status EXPIRADA en reservation.status.changed (ver CU-14).
[NEEDS CLARIFICATION: Idempotency key de cobro con número de intento]: la clave actual cobro-<reservation_id> impide reintentar con otro token tras un RECHAZADO; debe incluir el intento.
6. Trazabilidad FR/SC → Elemento del Contrato
Requisito / Criterio	Descripción en Spec CU-13	Elemento de este Contrato
FR-001	Mecanismo para conocer el resultado del cobro realizado por M3	Endpoint GET /api/v1/reservations/{reservation_id}/payment-confirmation
FR-003	Verificación de existencia de la reserva y su estado	Error 404 CHARGE_INTENT_NOT_FOUND y consulta condicionada a reservas en Pendiente de Pago
FR-004	Aprobación dentro del TTL: transición a Reservada	status APROBADO con TTL vigente
FR-005	Rechazos y fallos con reintento dentro del TTL	status RECHAZADO, CANCELADO y EXPIRADO
FR-006	Aprobación con TTL expirado: reversión de fondos	status APROBADO sobre reserva Expirada y estado EXPIRADA hacia M3 (Sección 5)
FR-008	Prohibición de cálculo monetario o llamadas a pasarelas	Respuesta transportada sin aritmética; M2 no contacta a Mercado Pago
SC-001	Transición a Reservada en tiempo acotado tras la confirmación	Periodo del job de consulta (5 s) y SLA de la consulta
SC-002	Cero reservas en Reservada con TTL expirado	Regla de decisión por TTL en la matriz de la Sección 2
SC-003	Cero cobros huérfanos sin instrucción de reversión	APROBADO sobre reserva Expirada genera el estado EXPIRADA hacia M3
SC-005	Cero cálculos monetarios en Módulo 2	Montos transportados sin operaciones
