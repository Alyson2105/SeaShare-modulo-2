# Auditoría H8 — CU-04 a CU-07

**Fecha:** 2026-11-13
**Alcance:** CU-04 (solicitar cancelación), CU-05 (marcar inasistencia), CU-06 (marcar inicio de navegación) y CU-07 (marcar fin de navegación).
**Método:** lectura estática de código, pruebas, migraciones y planes. No se ejecutaron pruebas, Docker, Git ni comandos que modifiquen archivos. No se editó código de producción ni de pruebas.

**Clasificación**

| Etiqueta | Significado |
|---|---|
| **VERIFICADO** | Observado directamente en el archivo o en el conteo estático. |
| **DECLARADO** | Afirmado por documentación, sin ejecución. |
| **NO VERIFICABLE** | Requiere ejecutar pruebas, Docker o servicios M1/M3. |

---

## 1. Resumen ejecutivo

1. **H8 es consistente con H1–H7 en lo esencial:** usa el mismo resolver de identidad por headers, el mismo formato de error `{code,message}`, la misma matriz de notificaciones y el mismo motor CU-08.
2. **Hallazgo alto:** la notificación a M1 por `M1_HTTP` no tiene cliente HTTP real. El único adaptador es `FakeFleetStatusNotificationAdapter`, que registra el mensaje y responde `DELIVERED`; el relay marca el evento como publicado sin aplicar el estado en M1.
3. **Hallazgo alto:** CU-07 no creaba la disputa de CU-16: `dispute_id` y `dispute_window_expires_at` siempre salían `null`, aunque el plan de CU-07 los exige. Quedó resuelto en el commit 5222f47.
4. **Hallazgo medio:** CU-04 no implementa la idempotencia de 60 segundos descrita en su plan.
5. **Hallazgo medio:** los cuatro servicios leen la zona horaria desde `reservation.harborTimezone`; no consultan M1/CU-09 en el momento de la operación, como indican los planes.
6. **Hallazgo medio:** hay desviaciones en el contrato de respuesta (campos que el plan exige no nulos y el código devuelve nulos).
7. **Reglas transversales:** CU-08 es el único escritor de estado; no hay aritmética monetaria en H8; el tiempo se obtiene por `ClockPort`; los eventos se escriben después de CU-08.
8. **Cobertura:** se contaron 140 métodos `@Test` en H8. No se ejecutaron; los conteos son estáticos.

---

## 2. Estado verificado

| Comprobación | Resultado | Evidencia |
|---|---|---|
| Identidad | Headers `X-User-Id` / `X-User-Role`; no JWT | `CallerIdentityArgumentResolver.java`; controladores CU-04..07 |
| Formato de error | `{code, message}` | `GlobalExceptionHandler`; pruebas de controladores |
| Zona horaria | Guardada en `reservation.harborTimezone` | `RequestCancellationService.java:83`; `StartNavigationService.java:75`; `ReportNoShowService.java:73`; `EndNavigationService.java:70` |
| CU-08 | Único escritor de estado | Los cuatro servicios invocan `ChangeReservationStatusUseCase` |
| Orden de eventos | Después de CU-08 | CU-04 L132; CU-05 L121; CU-06 L123; CU-07 L99 |
| Persistencia de tiempo real | Antes de CU-08 en inicio/fin | `StartNavigationService.java:100`; `EndNavigationService.java:76` |
| Dinero | Sin aritmética monetaria en H8 | Sin `BigDecimal`, `RoundingMode`, sumas ni redondeos en servicios H8 |
| Reloj | `ClockPort` | Sin `Instant.now()` en servicios H8 |
| Migraciones reales | V1 a V6 | `src/main/resources/db/migration/` |
| Códigos de error | 29 constantes | `ErrorCode.java`; `ErrorCodeTest` espera 29 |
| M1 consultas | HTTP real disponible en `mode=http` | `HttpFleetInfoAdapter`; `HttpFleetOperationalStatusAdapter`; `FleetHttpConfig` |
| M1 notificación | Outbox destino `M1_HTTP`; adaptador fake | `PublishPendingOutboxService.java:141`; `FakeFleetStatusNotificationAdapter.java:26` |
| M3 | Outbox/AMQP | `ChangeReservationStatusService`; `OutboxAdapter` |

---

## 3. Hallazgos

| ID | Severidad | Hallazgo | Evidencia |
|---|---|---|---|
| H8-F01 | Alta | La notificación de estado operativo a M1 no llega por HTTP. El adaptador fake responde `DELIVERED` y el mensaje se marca como publicado sin ejecutar el cambio en M1. | `FakeFleetStatusNotificationAdapter.java:23-26`; `PublishPendingOutboxService.java:141-149` |
| H8-F02 | Alta | CU-07 no abre la ventana de disputa de CU-16. La respuesta devuelve `null` en `dispute_id` y `dispute_window_expires_at`; el plan exige UUID y fecha a 24 horas. Resuelto en el commit 5222f47. | `EndNavigationService.java:111-112`; `CU-07/plan.md:14,76-77,203` |
| H8-F03 | Media | CU-04 no implementa la idempotencia de 60 s ni lee `X-Idempotency-Key`; un doble clic con la misma clave recibe `409`, no `200` con la respuesta anterior. | `CU-04/plan.md:74,378-379`; ausencia de idempotencia en `CancellationController` y `RequestCancellationService` |
| H8-F04 | Media | La zona horaria se toma de la reserva, no de una consulta actual a M1/CU-09. Si la zona guardada existe, no se detecta una caída o cambio en M1; el fail-safe solo cubre zona nula o inválida. | `RequestCancellationService.java:83`; `StartNavigationService.java:75`; `ReportNoShowService.java:73`; `EndNavigationService.java:70`; planes CU-04 L29 y CU-05 L12 |
| H8-F05 | Media | Contrato de respuesta desviado: `m1_sync_status` y `m3_notification_status` pueden ser `null`; `vessel_operational_status` puede ser `null`; `insurance_activated` puede ser `false`. Los planes los presentan como no nulos o con valor literal. En CU-06, vessel_operational_status nunca es null: la matriz devuelve IN_NAVIGATION para ese destino.| `RequestCancellationService.java:142-145`; `StartNavigationService.java:129-130,138`; `ReportNoShowService.java:101-105`; planes CU-04 L118-141, CU-05 L75, CU-06 L77-78 |
| H8-F06 | Media | Para el arrendatario, `justification` se envía como `reason` a CU-08. El plan dice que `reason` es nulo para RENTER y que la justificación va aparte. | `RequestCancellationService.java:114,122`; `CU-04/plan.md:54-57` |
| H8-F07 | Media | **Resuelto en commit d06039d.** `CancellationEventAdapter` usaba `save` en vez de `saveAndFlush`; hoy usa `saveAndFlush` (L26), igual que los otros tres adaptadores de evento. | `CancellationEventAdapter.java:26`; `CheckinEventAdapter.java:25`; `NoShowEventAdapter.java:26`; `CheckoutEventAdapter.java:25` |
| H8-F08 | Media | No hay prueba de integración que demuestre el rollback del tiempo real guardado antes de CU-08. Las pruebas unitarias verifican que CU-08 propaga el error y que no se inserta el evento, pero no el rollback de `actualDepartureAt` / `actualArrivalAt`. | `StartNavigationServiceTest.java:405-412`; `EndNavigationServiceTest.java:368-375`; `StartNavigationService.java:100`; `EndNavigationService.java:76` |
| H8-F09 | Media | Lógica duplicada: `resolveZone` en cuatro servicios; `parseUuid` y `resolveActor` en cuatro controladores; `containsDestination` en dos servicios; tolerancia de 30 min en inicio e inasistencia. | `RequestCancellationService.java:201`; `StartNavigationService.java:172`; `ReportNoShowService.java:164`; `EndNavigationService.java:145`; controladores CU-04..07 |
| H8-F10 | Baja | **Resuelto parcialmente en commit d06039d.** Estilo inconsistente: `StartNavigationService` y `NavigationStartIntegrationTest` ya usan nombre corto; quedan usos de nombre completo fuera de H8 (`VesselSearchResult.java:14`, `ChangeReservationStatusServiceTest.java:431`, `PaymentIntegrationTest.java:109`, `PostgresTestContainers.java:43`) y uno en `CancellationControllerTest.java:204` (pendiente). | `VesselSearchResult.java:14`; `ChangeReservationStatusServiceTest.java:431`; `PaymentIntegrationTest.java:109`; `PostgresTestContainers.java:43`; `CancellationControllerTest.java:204` |
| H8-F11 | Baja | Documentación desactualizada: `README.md` dice migraciones V1 a V4; el repositorio tiene V1 a V6. Corregido en el commit de documentacion de H8: README.md pasa a V1 a V6 y plan-trabajo-backend.md a 29 ErrorCode; queda pendiente la L241 ("son 21"). | `README.md:37`; `plan-trabajo-backend.md:236` |
| H8-F12 | Baja | CU-04 no tiene prueba de integración; solo pruebas de clasificador, servicio y controlador. | `src/test/java/.../CancellationClassifierTest`; `RequestCancellationServiceTest`; `CancellationControllerTest` |
| H8-F13 | NO VERIFICABLE | No se ejecutaron las 140 pruebas H8 ni la suite completa. | Restricción de la auditoría |
| H8-F14 | Baja | `ChangeReservationStatusCommand` tiene constructor canónico de 12 argumentos y dos constructores de compatibilidad (uno de 11 argumentos sin `paymentTokenRef` y uno de 5). Verificado por lectura del archivo; con eso se cierra la duda de 11 vs 12. | `ChangeReservationStatusCommand.java:9-48` |

---

## 4. Matriz plan vs código

| CU | Aspecto | Estado | Observación |
|---|---|---|---|
| CU-04 | Clasificación 72/24 h | **Cumple** | Umbrales inclusivos; RENTER en zarpe o después recibe `409`. |
| CU-04 | Identidad y pertenencia | **Cumple** | Header de identidad y `RESERVATION_NOT_OWNED`. |
| CU-04 | CU-08 como escritor | **Cumple** | Cancela vía comando; evento después. |
| CU-04 | M1 notificación | **Parcial** | Outbox `M1_HTTP`, pero adaptador fake. |
| CU-04 | M3 AMQP | **Cumple (código)** | Evento encolado por CU-08; sin ejecución. |
| CU-04 | Idempotencia 60 s | **No cumple** | Ausente. |
| CU-04 | Respuesta no nula | **Parcial** | Estados M1/M3 pueden ser `null`. |
| CU-05 | Tolerancia 30 min | **Cumple** | Exactamente 30 min ya permite reportar; minutos truncados. |
| CU-05 | Espera y observaciones | **Cumple** | `wait_minutes_recorded` y máximo 500. |
| CU-05 | Estado operativo | **Parcial** | Puede ser `null`; el plan espera `AVAILABLE`. |
| CU-05 | Error de tolerancia | **Parcial** | Código correcto, pero sin `minutes_remaining` ni `seconds_remaining`. |
| CU-05 | Fail-safe de zona | **Parcial** | Código `HARBOR_TIMEZONE_UNAVAILABLE`; el plan dice `FLEET_SERVICE_UNAVAILABLE`. |
| CU-06 | Inicio y salida | **Cumple** | Salida antes de lo pactado -> `EARLY_DEPARTURE`; doble inicio -> `TRIP_ALREADY_STARTED`. |
| CU-06 | Embarcación ocupada | **Cumple** | `VESSEL_CURRENTLY_IN_NAVIGATION`. |
| CU-06 | Seguro | **Parcial** | `insurance_activated` solo es `true` si M3 fue encolado. |
| CU-06 | M1 notificación | **Parcial** | Fake; estado de respuesta puede ser `null`. |
| CU-07 | Cierre | **Cumple** | `IN_NAVIGATION -> COMPLETED`; doble cierre -> `TRIP_ALREADY_COMPLETED`. |
| CU-07 | Notas de cierre | **Cumple** | Máximo 2000; blanco/null se guarda como `null`. |
| CU-07 | Disputa CU-16 | **No cumple** | No se crea; campos `null`. Resuelto en 5222f47: ahora se abre la disputa. |
| CU-07 | M1 notificación | **Parcial** | Fake; estado de respuesta puede ser `null`. |

---

## 5. Reglas transversales

| Regla | Resultado | Evidencia |
|---|---|---|
| CU-08 es el único escritor de estado | **Cumple** | Los cuatro servicios cambian estado solo por `ChangeReservationStatusUseCase`. |
| Sin dinero | **Cumple** | Sin operaciones monetarias en H8. |
| Reloj inyectable | **Cumple** | `ClockPort` en los cuatro servicios. |
| Eventos inmutables | **Cumple** | Tablas y entidades inmutables; los cuatro adaptadores usan saveAndFlush (resuelto en commit d06039d). |
| Eventos después de CU-08 | **Cumple** | CU-04 L132; CU-05 L121; CU-06 L123; CU-07 L99. |
| Sin llamadas externas dentro de transacción | **Cumple en H8** | H8 no llama M1 en línea; usa la zona persistida. |
| Identidad por headers | **Cumple** | Mismo resolver que H6/H7. |
| Respuesta uniforme de errores | **Cumple** | `{code,message}`; sin tildes en códigos. |

---

## 6. Pruebas H8

| Archivo | Métodos `@Test` |
|---|---:|
| `CancellationClassifierTest` | 8 |
| `RequestCancellationServiceTest` | 20 |
| `CancellationControllerTest` | 8 |
| `EndNavigationServiceTest` | 19 |
| `NavigationEndControllerTest` | 7 |
| `NavigationEndIntegrationTest` | 7 |
| `StartNavigationServiceTest` | 21 |
| `NavigationStartControllerTest` | 7 |
| `NavigationStartIntegrationTest` | 10 |
| `ReportNoShowServiceTest` | 18 |
| `NoShowControllerTest` | 6 |
| `NoShowIntegrationTest` | 9 |
| **Total H8** | **140** |

No se ejecutaron. Los conteos se obtuvieron por búsqueda estática de `@Test`.

---

## 7. [NEEDS CLARIFICATION]

1. **B4 en `plan-trabajo-backend.md` (resuelto):** decisión tomada el 13 de noviembre de 2026: L226 corregida (lista real de migraciones V1 a V6), L236 corregida (29 constantes, rango L8-L36), L256 y L260 sin tocar, y una línea "Nota H8" agregada debajo de la tabla del Anexo B. Queda abierto solo L241 ("son 21"), que no se tocó por la restricción de no editar nada más del archivo: [NEEDS CLARIFICATION].
2. **Notificación M1:** confirmar si el fake de `M1_HTTP` es una entrega aceptada para H8 o si H8 debía incluir el cliente HTTP real.
3. **CU-07 y CU-16 (resuelto):** la disputa no se difirió; se implementó en el commit 5222f47 y el plan de CU-07 se cumple.
4. **Zona horaria:** confirmar si la zona persistida en la reserva es la fuente oficial aceptada, o si cada operación debe consultar M1/CU-09 como dice el plan.
5. **Contrato M1/M3:** confirmar si los estados pueden ser `null` cuando no hay destino encolado, o si deben devolver siempre un valor literal (`ENQUEUED`, `PUBLISHED`, `AVAILABLE`, `IN_NAVIGATION`, `MAINTENANCE_CLEANING`).
6. **Idempotencia CU-04:** confirmar si la ventana de 60 s sigue vigente para H8 o si se difiere.

---

## Diferencias documentadas en los planes

| Marca | CU | plan.md editado | Resumen en una linea | Contradice el resumen (si/no) |
|---|---|---|---|---|
| [DIFERENCIA H8-CU04-01] | CU-04 | `Documentación/features/CU-04-solicitar-cancelacion/plan.md` | Zona desde `reservation.harborTimezone` con 503 `HARBOR_TIMEZONE_UNAVAILABLE`; el plan pide consultar CU-09 | no |
| [DIFERENCIA H8-CU04-02] | CU-04 | `Documentación/features/CU-04-solicitar-cancelacion/plan.md` | Identidad por headers `X-User-Id`/`X-User-Role`; sin Bearer JWT (401 solo si faltan headers) | no |
| [DIFERENCIA H8-CU04-03] | CU-04 | `Documentación/features/CU-04-solicitar-cancelacion/plan.md` | Notificacion M1 por outbox `M1_HTTP` con unico adaptador fake (`DELIVERED` sin HTTP) | no |
| [DIFERENCIA H8-CU04-04] | CU-04 | `Documentación/features/CU-04-solicitar-cancelacion/plan.md` | `m1_sync_status`, `m3_notification_status` y `vessel_new_operational_status` pueden ser `null`; el plan los da no nulos | no |
| [DIFERENCIA H8-CU04-05] | CU-04 | `Documentación/features/CU-04-solicitar-cancelacion/plan.md` | Sin `X-Idempotency-Key` ni ventana de 60 s; el doble clic no recibe la respuesta anterior | no |
| [DIFERENCIA H8-CU04-06] | CU-04 | `Documentación/features/CU-04-solicitar-cancelacion/plan.md` | Para RENTER la justificacion viaja como `reason` a CU-08; el plan dice `reason` nulo | no |
| [DIFERENCIA H8-CU05-01] | CU-05 | `Documentación/features/CU-05-marcar-inasistencia/plan.md` | Codigo 503 real es `HARBOR_TIMEZONE_UNAVAILABLE` desde la zona guardada; el plan dice `FLEET_SERVICE_UNAVAILABLE` desde CU-09 | no |
| [DIFERENCIA H8-CU05-02] | CU-05 | `Documentación/features/CU-05-marcar-inasistencia/plan.md` | Identidad por headers `X-User-Id`/`X-User-Role`; sin Bearer JWT | no |
| [DIFERENCIA H8-CU05-03] | CU-05 | `Documentación/features/CU-05-marcar-inasistencia/plan.md` | Notificacion M1 por outbox `M1_HTTP` con unico adaptador fake | no |
| [DIFERENCIA H8-CU05-04] | CU-05 | `Documentación/features/CU-05-marcar-inasistencia/plan.md` | `vessel_operational_status` es `AVAILABLE` solo si m1_blocked y si no `null`, con mensaje distinto; el plan lo da literal | no |
| [DIFERENCIA H8-CU05-05] | CU-05 | `Documentación/features/CU-05-marcar-inasistencia/plan.md` | Sin campos `minutes_remaining`/`seconds_remaining`; el tiempo restante va dentro del message en ingles del 409 | no |
| [DIFERENCIA H8-CU06-01] | CU-06 | `Documentación/features/CU-06-marcar-inicio-navegacion/plan.md` | El plan no define zona horaria ni error 503; el codigo usa la zona guardada con 503 `HARBOR_TIMEZONE_UNAVAILABLE` | si |
| [DIFERENCIA H8-CU06-02] | CU-06 | `Documentación/features/CU-06-marcar-inicio-navegacion/plan.md` | Identidad por headers `X-User-Id`/`X-User-Role`; sin Bearer JWT | no |
| [DIFERENCIA H8-CU06-03] | CU-06 | `Documentación/features/CU-06-marcar-inicio-navegacion/plan.md` | Notificacion M1 por outbox `M1_HTTP` con unico adaptador fake | no |
| [DIFERENCIA H8-CU06-04] | CU-06 | `Documentación/features/CU-06-marcar-inicio-navegacion/plan.md` | `insurance_activated` es `true` solo si el destino AMQP fue encolado; el plan lo da literal `true` | no |
| [DIFERENCIA H8-CU06-05] | CU-06 | `Documentación/features/CU-06-marcar-inicio-navegacion/plan.md` | Orden real: 404, 403, 400, 409, 503, 409 `EARLY_DEPARTURE`, 400, 409, 500; el plan no tiene fila 503 | si |
| [DIFERENCIA H8-CU07-01] | CU-07 | `Documentación/features/CU-07-marcar-fin-navegacion/plan.md` | Sin diferencia desde el commit 5222f47: el codigo abre la disputa de CU-16 y devuelve `dispute_id` y `dispute_window_expires_at`; antes salian `null` | no |
| [DIFERENCIA H8-CU07-02] | CU-07 | `Documentación/features/CU-07-marcar-fin-navegacion/plan.md` | `vessel_operational_status` es `MAINTENANCE_CLEANING` solo si m1_blocked y si no `null`, con mensaje sin clausula de M1 | no |
| [DIFERENCIA H8-CU07-03] | CU-07 | `Documentación/features/CU-07-marcar-fin-navegacion/plan.md` | Identidad por headers, cuerpo opcional y sin Content-Type obligatorio; el plan exige Bearer JWT y Content-Type | no |
| [DIFERENCIA H8-CU07-04] | CU-07 | `Documentación/features/CU-07-marcar-fin-navegacion/plan.md` | Notificacion M1 por outbox `M1_HTTP` con unico adaptador fake | no |
| [DIFERENCIA H8-CU07-05] | CU-07 | `Documentación/features/CU-07-marcar-fin-navegacion/plan.md` | El plan no define zona horaria ni error 503; el codigo usa la zona guardada con 503 `HARBOR_TIMEZONE_UNAVAILABLE` | si |
