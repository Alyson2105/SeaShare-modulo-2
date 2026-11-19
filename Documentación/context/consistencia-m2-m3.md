# Consistencias entre Módulo 2 y Módulo 3

> Contrato de consistencia entre el Módulo 2 (Reservas y Operaciones) y el Módulo 3 (Finanzas / "el sistema"). No es documentación general de SEA-SHARE: solo cubre lo que ambos módulos deben nombrar y entender igual.
>
> **Vigencia:** este documento refleja el estado final acordado para la entrega del sábado. **M3 es la referencia y no se modifica. M1 no se modifica. M2 se adapta.** Las decisiones que antes estaban abiertas se cierran en §6.
>
> **Fuentes:** `sea-share.md`, `contexto-modulo3.md`, `consistencia-m2-m1.md` y el código de M2/M3 a la fecha.

## 1. Nombres y actores

| Concepto | Módulo 2 (`sea-share.md`) | Módulo 3 (`contexto-modulo3.md`) | Nombre que debe utilizarse |
| --- | --- | --- | --- |
| Arrendatario | "turistas" en la introducción general; "Arrendatario" en el detalle operativo (2.1, 2.2) | "Arrendatario" (único término, sin excepciones) | **Arrendatario** |
| Propietario | "Propietario" en la entidad Embarcación (Módulo 1); "anfitrión" aparece solo en textos históricos de cancelación | "Propietario" (único término, sin excepciones) | **Propietario** |
| Módulo 2 (como sistema) | "Módulo 2: Operación de Reservas, Tiempos y Cancelaciones" | "Sistema de Reservas y Operaciones" | **Sistema de Reservas y Operaciones** en interacciones con Finanzas |
| Módulo 3 (como sistema) | "Módulo 3: Liquidación, Seguros y Dispersión de Fondos" | Se autodenomina **"el sistema"** | **"el sistema"** en todo contenido de SPEC/Finanzas; "Módulo 3" solo para referirse al repo o al despliegue |

**Actores no compartidos** (existen solo del lado de Finanzas y no requieren nombre común con Módulo 2): Administrador Financiero, Pasarela de Pago. Módulo 2 no interactúa directamente con ellos.

---

## 2. Reserva

Una reserva es el acuerdo entre Arrendatario y Propietario para el alquiler temporal de una embarcación. Módulo 2 es dueño del ciclo de vida y los tiempos de la reserva (TTL, ventanas de cancelación, umbral de No-Show); Módulo 3 es dueño de los cálculos y movimientos financieros asociados a cada transición de estado que se lo solicite.

- **Cuándo comienza**: la reserva como entidad nace cuando el Arrendatario oprime "Reservar" → estado **Iniciada**. Antes de eso (pantalla de exploración) solo existe una *intención de reserva* sin ID, cubierta por "Solicitar estimación para reserva" en modo lote.
- **Información que necesita Módulo 3 de Módulo 2**: identificador de reserva, embarcación, fechas (`start_date`/`end_date`), pasajeros, propietario y capacidad máxima ("Brindar información de reserva"); el estado vigente de la reserva ("Brindar el estado de la reserva"); el estado de la disputa de garantía ("Brindar información de disputa de garantía").
- **Información que necesita Módulo 2 de Módulo 3**: estimación preliminar ("Solicitar estimación para reserva"), desglose y valor total definitivo ("Solicitar el valor calculado de la reserva"), y el resultado del cobro ("Solicitar confirmación de pago").
- **Qué ocurre al reservar**: → Iniciada; arranca el TTL de 15 min. La embarcación **continúa figurando disponible en Módulo 1**; M2 retiene las fechas en su propia base. Varias reservas en `Iniciada` pueden coexistir para el mismo barco y fechas.
- **Qué ocurre al pagar**: el Arrendatario oprime "Confirmar pago" → `Pendiente de Pago`; esto habilita a Finanzas a ejecutar "Procesar cobro". Este estado es transitorio y lo gestiona íntegramente Módulo 2 (TTL de 15 minutos en curso, sin intervención de Módulo 1); el TTL sigue corriendo desde "Iniciada" (no se reinicia).
- **Qué ocurre cuando el pago se confirma**: Módulo 2 consulta "Solicitar confirmación de pago"; solo un resultado aprobado y verificable avanza la reserva a `Reservada`. **Módulo 2 no notifica a Módulo 1 el estado `Reservado`**: el bloqueo de fechas es local de M2 y M1 sigue viendo la embarcación disponible para el rango. M1 recibirá `IN_NAVIGATION` en el check-in (contrato M1–M2, D-04).
- **Qué ocurre si expira**: si el TTL vence (iniciado en "Iniciada") sin confirmación exitosa, la reserva pasa a `Expirada`; no se notifica nada a Módulo 1, porque Módulo 1 nunca recibió la ocupación. Cualquier autorización pendiente en la pasarela debe cancelarse o quedar en conciliación (no se asume que el pago falló).
- **Qué ocurre si el pago se rechaza sin posibilidad de retiro**: un rechazo simple (`REJECTED`) mantiene la reserva en `Pendiente de Pago` para reintento mientras quede TTL. Si M3 reporta el cobro como cancelado o expirado, la reserva pasa a `Pago Fallido`; tampoco se notifica nada a Módulo 1.
- **Qué ocurre si se cancela**: solo es válido desde `Reservada`. Como M1 no recibe `RESERVED`, **M1 no recibe ninguna señal de liberación al cancelar**: nunca conoció la ocupación. La avería mecánica reportada por el Propietario se registra en M2, pero no genera aviso C4 porque no hay ocupación que liberar.
- **Qué ocurre al finalizar**: el Propietario marca la reserva como Completada tras la devolución → Finanzas liquida alquiler + seguro de inmediato; el depósito de garantía queda pendiente hasta el resultado de la disputa de garantía. M2 solo notifica a M1 `MAINTENANCE_CLEANING` si M1 ya conocía la ocupación (`IN_NAVIGATION`); si no, no envía nada. El retorno final a `Disponible` es una transición interna de Módulo 1.

No se detectó contradicción entre ambos documentos sobre el origen del TTL (ambos coinciden en que nace en "Iniciada" y no se reinicia en "Pendiente de Pago"); `contexto-modulo3.md` simplemente añade el detalle operación-a-operación que `sea-share.md` no desarrolla.

### Flujo de la reserva

```text
Disponible (embarcación, Módulo 1)
   ↓ (Arrendatario oprime "Reservar")
Iniciada  ───────────────────────┐  (arranca TTL 15 min; M1 sigue disponible)
   ↓ (Arrendatario confirma pago)                │
Pendiente de Pago ── (mismo TTL, no se reinicia) │  (transitorio interno de Módulo 2; M1 no participa)
   ↓ (Finanzas confirma pago)      │  TTL vence sin pago confirmado   │ rechazo definitivo
Reservada (M2 retiene las fechas; M1 no recibe RESERVED)   ↓          ↓
   ↓ (check-in)                Expirada                             Pago Fallido
En Navegación (aquí M1 recibe IN_NAVIGATION)   (sin señal a M1)     (sin señal a M1)
   ↓ (Propietario marca devuelta)
Completada (señal a M1 → Mantenimiento/Limpieza si M1 conocía la ocupación; el retorno a Disponible es interno de M1)
   ↓ (resultado de disputa de garantía — ver §4)
[depósito → Arrendatario]  o  [depósito → Propietario]
```

Rama de cancelación (**solo se admite desde `Reservada`** — pago confirmado y previo al check-in; `Iniciada` y `Pendiente de Pago` no admiten cancelación activa y se resuelven por expiración del TTL; `Pago Fallido`, `En Navegación`, `Completada` y `Expirada` tampoco son cancelables):

```text
Reservada → Cancelada (Flexible)   (>=72h)              → Reembolso 100%
Reservada → Cancelada (Moderado)  (>=24h y <72h)        → Reembolso 50% + dispersión 50% al Propietario
Reservada → Cancelada (Tardío / Por Inasistencia) (<24h) → Dispersión 100% al Propietario, sin reembolso del alquiler
Reservada → Cancelada (Por Propietario)            → Reembolso 100% al Arrendatario
```

### Estados de la reserva

| Estado de la reserva | Qué significa | Qué ocurre para llegar a este estado |
| --- | --- | --- |
| Iniciada | Arranca el TTL de 15 min; M2 retiene las fechas localmente; M1 sigue viendo la embarcación disponible. | El Arrendatario oprime "Reservar". |
| Pendiente de Pago | Continúa el mismo TTL (no se reinicia); habilita "Procesar cobro"; estado transitorio gestionado íntegramente por Módulo 2. | El Arrendatario oprime "Confirmar pago". |
| Reservada | Pago confirmado; reserva exitosa; aún sin uso. M2 retiene las fechas en su inventario local; **M1 no recibe `RESERVED`**. | Finanzas confirma el pago dentro del TTL. |
| En Navegación | Contrato activo; embarcación en uso. M2 notifica `IN_NAVIGATION` a M1. | Se realiza el check-in. |
| Completada | Alquiler y seguro se liquidan de inmediato; depósito queda pendiente. | El Propietario marca la reserva como completada tras la devolución. |
| Cancelada | Cancelación activa desde `Reservada`, o por No-Show (inasistencia); el sub-estado define la compensación. | El Arrendatario o el Propietario cancelan desde `Reservada`, o transcurren los 30 min de espera sin presentación. |
| Expirada | El TTL venció sin confirmación de pago; no se notifica a Módulo 1. | Vence el TTL (iniciado en `Iniciada`) sin pago confirmado. |
| Pago Fallido | El cobro quedó cancelado o expirado en M3 y no admite reintento; no se notifica a Módulo 1. | M3 reporta `CANCELLED` o `EXPIRED` para el cobro. |

**Sub-estados de cancelación** (sobre `Cancelada`): `Flexible` (>=72h), `Moderado` (>=24h y <72h), `Tardío` (<24h), `Por Propietario` y `Por Inasistencia`.

**Vocabulario de Finanzas**: "Pendiente" se corresponde con `PENDING`; "Reservado" con `RESERVED`; "Cancelado Flexible/Moderado/Tardío" con `CANCELLED_*` y su sub-estado. `Expirada` y `Pago Fallido` son estados de la reserva en Módulo 2 y **no se publican a Módulo 3** (CU-14 FR-001). La única excepción es el cobro aprobado después del TTL: Módulo 2 mantiene la reserva en `Expirada` y publica `CANCELLED_FLEXIBLE`, que Módulo 3 trata como reversión del 100 %.

**Mapeo de estados publicado a Módulo 3**:

| Estado de M2 | `status` que recibe M3 |
| --- | --- |
| Pendiente de Pago | `PENDING` |
| Reservada | `RESERVED` |
| En Navegación | `IN_NAVIGATION` |
| Completada | `COMPLETED` |
| Cancelada · Flexible | `CANCELLED_FLEXIBLE` |
| Cancelada · Moderado | `CANCELLED_MODERATE` |
| Cancelada · Tardío | `CANCELLED_STRICT` |
| Cancelada · Por Propietario | `CANCELLED_BY_HOST` |
| Cancelada · Por Inasistencia | `CANCELLED_STRICT` (mismo tratamiento financiero) |
| Expirada con cobro aprobado tardío | `CANCELLED_FLEXIBLE` (reversión del 100 %) |
| Iniciada, Expirada sin cobro, Pago Fallido | No se publican |

**Coherencia de estados**: Módulo 2 maneja 8 estados internos y 5 sub-estados de cancelación; Módulo 3 tiene 10 valores en su enum (`AVAILABLE`, `INITIATED`, `RESERVED`, `IN_NAVIGATION`, `PENDING`, `CANCELLED_FLEXIBLE`, `CANCELLED_MODERATE`, `CANCELLED_STRICT`, `CANCELLED_BY_HOST`, `COMPLETED`). El conjunto que M2 publica es el de la tabla anterior. M3 no tiene `EXPIRED` ni `PAYMENT_FAILED`, y **no distingue `Tardío` de `Por Inasistencia`**: ambos llegan como `CANCELLED_STRICT`. `Disponible`/`En Mantenimiento` son estados de la embarcación (Módulo 1), no de la reserva.

---

## 3. Garantía (Depósito)

- **Qué es**: 10% de la tarifa diaria **vigente, con el ajuste dinámico ya aplicado** (fin de semana/temporada alta). No se multiplica por días, pasajeros ni daño.
- **Momento en la reserva**: se calcula y se congela en "Solicitar el valor calculado de la reserva" (antes de confirmar pago); se cobra junto con alquiler y seguro como **un único monto** en "Procesar cobro" (sin operación separada en la pasarela, aunque Módulo 3 conserva el desglose internamente); se retiene tras "Completada" hasta que se resuelve la disputa de garantía.
- **Qué módulo interviene**: Módulo 2 crea y gestiona la disputa (otorga la ventana para reportar daños y decide el resultado operativo); Módulo 3 solo ejecuta la consecuencia financiera (reembolso o liquidación total) a partir del estado recibido, usando montos que ya tiene registrados internamente — nunca recibe montos de Módulo 2.
- **Información que Módulo 2 debe enviar a Módulo 3**: únicamente identificador de reserva, identificador de disputa, estado (`REJECTED`/`COMPLETED`), marca temporal y clave idempotente (`event_key`). **Nunca montos, instrucciones de pago ni motivo**: el motivo queda local en la revisión administrativa de M2.
- **Precondición financiera**: Módulo 3 solo puede reembolsar o liquidar el depósito si el cobro está `CAPTURED`. M2 garantiza esa condición porque solo pasa a `Reservada` cuando M3 devuelve `APPROVED`, y M3 solo devuelve `APPROVED` para un cobro `CAPTURED`.
- **Qué ocurre después de resolver**: el depósito se entrega **completo** al Arrendatario o **completo** al Propietario; no existe retención parcial en ningún caso.

---

## 4. Disputa de garantía

- **Qué se considera**: el proceso mediante el cual se decide si el depósito se devuelve al Arrendatario o se liquida al Propietario, según si se detectaron daños menores al regreso de la embarcación.
- **Cuándo se genera**: tras "Completada", dentro de la ventana de **24 horas** que Módulo 2 concede al Propietario para reportar daños.
- **Quién interviene**: el Propietario (reporta o no reporta daños) y el Módulo 2 (crea y gestiona la disputa, incluida la evaluación de procedencia del reclamo).
- **Qué módulo la gestiona**: Módulo 2 gestiona la disputa por completo; Módulo 3 únicamente consume su resultado final y ejecuta la operación financiera correspondiente.
- **Estados** (los de M3, que es la referencia):
  - `PENDING`: existe o continúa en revisión; sin acción financiera. M2 no publica este estado.
  - `REJECTED`: el reclamo no procede (incluye ausencia de reclamo al vencer la ventana); depósito 100% al Arrendatario.
  - `COMPLETED`: el reclamo procede; depósito 100% al Propietario.
- **Qué ocurre al resolverse**: Módulo 2 notifica el estado final a Módulo 3, que ejecuta el reembolso o la liquidación total con sus propios registros.
- **Efecto sobre reserva/garantía**: cierra el ciclo financiero del depósito de esa reserva. Mientras no exista `REJECTED` o `COMPLETED`, el depósito se considera "pendiente de resolución" y puede seguir contabilizándose en reportes financieros sucesivos.

---

## 5. Interacciones entre módulos

| Acción / evento | Módulo que inicia | Módulo que recibe | Información relevante |
| --- | --- | --- | --- |
| Solicitar estimación para reserva (lote) | Módulo 2 | Módulo 3 | Solo `boat_ids`. M3 usa la fecha de hoy, 1 día y 1 pasajero; es una cotización de referencia, no el precio final. |
| Solicitar estimación individual | Módulo 2 | Módulo 3 | `boat_id`, `start_date`, `end_date`, `passengers` → `estimated_total` y `warning`. |
| Brindar información de reserva | Módulo 2 | Módulo 3 | `reservation_id`, `boat_id`, `start_date`, `end_date`, `passengers`, `owner_id`, `max_capacity`. Unidireccional: Módulo 3 no responde. |
| Solicitar el valor calculado de la reserva | Módulo 2 | Módulo 3 | `POST /api/v1/reservations/{id}/calculated-value` → desglose (alquiler, seguro, depósito, total). |
| Procesar cobro | Módulo 2 (reserva en `Pendiente de Pago`) | Módulo 3 | Evento `PENDING` con `reservation_id`, `status`, `status_changed_at`, `payment_token_ref`, `payment_method_type` y `payment_metadata` (`payer_email`, `last_four`). |
| Solicitar confirmación de pago | Módulo 2 | Módulo 3 | `GET /api/v1/reservations/{id}/payment-confirmation` → estado del cobro. |
| Brindar el estado de la reserva | Módulo 2 | Módulo 3 | `reservation_id`, **un único `status`** (incluye el sub-estado de cancelación cuando aplica) y `status_changed_at`. Unidireccional: Módulo 3 no responde ni notifica fallos a Módulo 2. |
| Brindar información de disputa de garantía | Módulo 2 | Módulo 3 | `reservation_id`, `dispute_id`, `status` (`REJECTED`/`COMPLETED`), `status_changed_at`, `event_key`. Unidireccional, sin montos ni motivo. |

**Sobre CU-15**: M2 expone `GET /api/v1/internal/reservations/{id}`, pero M3 **no lo consume**: obtiene la información por el evento `reservation.info.provided`. El endpoint se conserva; no forma parte de la integración M2–M3.

**Dependencia externa I2**: para calcular la tarifa base, M3 consulta a M1 `POST /api/v1/fleet/base-rates`; M1 expone `POST /api/v1/boats/base-rates`. Es una llamada **M3→M1** en la que M2 no participa. Si M1 no expone el path que M3 espera, UC02/UC03 no funciona y no hay cambio de M2 que lo resuelva.

---

## 6. Decisiones finales

Estas decisiones cierran el contrato para la entrega del sábado. **No se solicita ningún cambio a M3**; M1 tampoco se modifica.

| Tema | Decisión final |
| --- | --- |
| **Topología de colas** | M2 adopta la declaración de M3: las tres colas compartidas (`finance.reservation-status.v1`, `finance.reservation-info.v1`, `finance.guarantee-dispute.v1`) son **quorum** con `x-dead-letter-exchange=""` y `x-dead-letter-routing-key=<cola>.dlq`. Las DLQ siguen durables y clásicas. M2 elimina `seashare.reservations.dlx` y sus bindings. |
| **I2 (tarifas base)** | Dependencia externa M3→M1. M2 no interviene. En el M1 disponible solo existe `/api/v1/boats/base-rates`; si el cierre de M1 no añadió el alias, el flujo UC02/UC03 queda bloqueado y se documenta como riesgo. |
| **CU-15** | El endpoint interno se conserva; M3 no lo usa. |
| **Idempotencia** | `Message-Id = event_id` es trazabilidad. La deduplicación real de M3 es por clave de dominio en `reservation_status_log` (`reservation_id`, `status`, `operation_type`, `idempotency_key`). En `PENDING` la clave incluye `status_changed_at`: un reintento con nueva marca es un cobro nuevo, por diseño. |
| **Disputa** | Estados `PENDING`/`REJECTED`/`COMPLETED`. M2 nunca envía `ACCEPTED` ni `reason`; el motivo queda local. |
| **Pagos y cuotas** | M2 sigue aceptando `installments` y `payment_method_id` en su API, pero **no los reenvía**. M3 fija `installments = 1` y decide la marca de la pasarela a partir de `payment_method_type` (omite tipos genéricos y envía marcas en minúsculas). `payment_metadata` solo lleva `payer_email` y `last_four`. |
| **Frontera de cancelación** | `>= 72 h` → Flexible; `>= 24 h` → Moderado; `< 24 h` → Tardío. Es la regla implementada. |
| **Depósito** | 10 % de la tarifa diaria vigente **con ajuste dinámico aplicado**. |
| **Riesgo aceptado N10/N11** | M3 no deduplica `reservation.info.provided` y su upsert invalida montos calculados. Para esta entrega **no se añade reconciliación en M2**; el camino feliz funciona y el riesgo de un reenvío ambiguo se acepta y se documenta. |
| **Riesgo aceptado N15** | M3 nunca emite `PaymentStatus.FAILED`; M2 lo conserva como valor tolerado. |
| **Riesgo aceptado N16** | No hay trazabilidad `X-Correlation-Id` extremo a extremo en AMQP; M3 solo la lee por HTTP. Se acepta. |
