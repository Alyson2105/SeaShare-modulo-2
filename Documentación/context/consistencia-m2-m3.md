# Consistencias entre Módulo 2 y Módulo 3

> Contrato de consistencia entre el Módulo 2 (Reservas y Operaciones) y el Módulo 3 (Finanzas / "el sistema"). No es documentación general de SEA-SHARE: solo cubre lo que ambos módulos deben nombrar y entender igual. Fuentes: `sea-share.md` y `contexto-modulo3.md`.

## 1. Nombres y actores

| Concepto | Módulo 2 (`sea-share.md`) | Módulo 3 (`contexto-modulo3.md`) | Nombre que debe utilizarse |
| --- | --- | --- | --- |
| Arrendatario | "turistas" en la introducción general; "Arrendatario" en el detalle operativo (2.1, 2.2) | "Arrendatario" (único término, sin excepciones) | **Arrendatario** |
| Propietario | "Propietario" en la entidad Embarcación (Módulo 1); pero "**anfitrión**" en la regla de cancelación tardía (2.2: "compensación al anfitrión") | "Propietario" (único término, sin excepciones) | **Propietario** — ver inconsistencia §6 |
| Módulo 2 (como sistema) | "Módulo 2: Operación de Reservas, Tiempos y Cancelaciones" | "Sistema de Reservas y Operaciones" | Ambos son equivalentes; en interacciones con Finanzas usar **"Sistema de Reservas y Operaciones"** |
| Módulo 3 (como sistema) | "Módulo 3: Liquidación, Seguros y Dispersión de Fondos" | Se autodenomina **"el sistema"** | Equivalentes; en todo contenido de SPEC/Finanzas usar **"el sistema"**, nunca "Módulo 3" |

**Actores no compartidos** (existen solo del lado de Finanzas y no requieren nombre común con Módulo 2): Administrador Financiero, Pasarela de Pago. Módulo 2 no los menciona porque no interactúa directamente con ellos.

---

## 2. Reserva

Una reserva es el acuerdo entre Arrendatario y Propietario para el alquiler temporal de una embarcación. Módulo 2 es dueño del ciclo de vida y los tiempos de la reserva (TTL, ventanas de cancelación, umbral de No-Show); Módulo 3 es dueño de los cálculos y movimientos financieros asociados a cada transición de estado que se lo solicite.

- **Cuándo comienza**: la reserva como entidad nace cuando el Arrendatario oprime "Reservar" → estado **Iniciada**. Antes de eso (pantalla de exploración) solo existe una *intención de reserva* sin ID, cubierta por "Solicitar estimación para reserva" en modo lote.
- **Información que necesita Módulo 3 de Módulo 2**: identificador de reserva, embarcación, días, pasajeros, propietario y capacidad máxima ("Brindar información de reserva"); el estado vigente de la reserva ("Brindar el estado de la reserva"); el estado de la disputa de garantía ("Brindar información de disputa de garantía").
- **Información que necesita Módulo 2 de Módulo 3**: estimación preliminar ("Solicitar estimación para reserva"), desglose y valor total definitivo ("Solicitar el valor calculado de la reserva"), y el resultado del cobro ("Solicitar confirmación de pago").
- **Qué ocurre al reservar**: → Iniciada; arranca el TTL de 15 min; la embarcación **continúa figurando disponible** en Módulo 1 hasta que se formaliza el pago (varias reservas en `Iniciada` pueden coexistir para el mismo barco y fechas).
- **Qué ocurre al pagar**: el Arrendatario oprime "Confirmar pago" → `Pendiente de Pago`; esto habilita a Finanzas a ejecutar "Procesar cobro" y es el momento en que la embarcación pasa a `Reservado` en Módulo 1; el TTL sigue corriendo desde "Iniciada" (no se reinicia).
- **Qué ocurre cuando el pago se confirma**: Módulo 2 consulta "Solicitar confirmación de pago"; solo un resultado aprobado y verificable avanza la reserva a `Reservada`.
- **Qué ocurre si expira**: si el TTL vence (iniciado en "Iniciada") sin confirmación exitosa, la reserva pasa a `Expirada` y la embarcación vuelve a `Disponible` en Módulo 1; cualquier autorización pendiente en la pasarela debe cancelarse o quedar en conciliación (no se asume que el pago falló).
- **Qué ocurre si el pago se rechaza sin posibilidad de retiro**: la reserva pasa a `Pago Fallido` y la embarcación vuelve a `Disponible` en Módulo 1.
- **Qué ocurre si se cancela**: solo es válido desde `Reservada` (ver rama de cancelación).
- **Qué ocurre al finalizar**: el Propietario marca la reserva como Completada tras la devolución → Finanzas liquida alquiler + seguro de inmediato; el depósito de garantía queda pendiente hasta el resultado de la disputa de garantía.

No se detectó contradicción entre ambos documentos sobre el origen del TTL (ambos coinciden en que nace en "Iniciada" y no se reinicia en "Pendiente de Pago"); `contexto-modulo3.md` simplemente añade el detalle operación-a-operación que `sea-share.md` no desarrolla.

### Flujo de la reserva

```text
Disponible (embarcación, Módulo 1)
   ↓ (Arrendatario oprime "Reservar")
Iniciada  ───────────────────────┐  (arranca TTL 15 min; M1 aún no bloquea)
   ↓ (Arrendatario confirma pago)                │
Pendiente de Pago ── (mismo TTL, no se reinicia) │
   ↓ (Finanzas confirma pago)      │  TTL vence sin pago confirmado   │ rechazo definitivo
Reservada                           ↓                                   ↓
   ↓ (check-in)                Expirada                             Pago Fallido
En Navegación                 (embarcación → Disponible en M1)
   ↓ (Propietario marca devuelta)
Completada
   ↓ (resultado de disputa de garantía — ver §4)
[depósito → Arrendatario]  o  [depósito → Propietario]
```

Rama de cancelación (**solo se admite desde `Reservada`** — pago confirmado y previo al check-in; `Iniciada` y `Pendiente de Pago` no admiten cancelación activa y se resuelven por expiración del TTL; `Pago Fallido`, `En Navegación`, `Completada` y `Expirada` tampoco son cancelables):

```text
Reservada → Cancelada (Flexible)   (>72h)          → Reembolso 100%
Reservada → Cancelada (Moderado)  (72h–24h)        → Reembolso 50% + dispersión 50% al Propietario
Reservada → Cancelada (Tardío / Por Inasistencia) (<24h) → Dispersión 100% al Propietario, sin reembolso
Reservada → Cancelada (Por Propietario)            → Reembolso 100% al Arrendatario (cancelación del anfitrión)
```

### Estados de la reserva

| Estado de la reserva | Qué significa | Qué ocurre para llegar a este estado |
| --- | --- | --- |
| Iniciada | Arranca el TTL de 15 min; la embarcación aún no se bloquea en Módulo 1. | El Arrendatario oprime "Reservar". |
| Pendiente de Pago | Continúa el mismo TTL (no se reinicia); habilita "Procesar cobro"; la embarcación pasa a `Reservado` en Módulo 1. | El Arrendatario oprime "Confirmar pago". |
| Reservada | Pago confirmado; reserva exitosa; aún sin uso. | Finanzas confirma el pago dentro del TTL. |
| En Navegación | Contrato activo; embarcación en uso. | Se realiza el check-in. |
| Completada | Alquiler y seguro se liquidan de inmediato; depósito queda pendiente. | El Propietario marca la reserva como completada tras la devolución. |
| Cancelada | Cancelación activa desde `Reservada`, o por No-Show (inasistencia); el sub-estado define la compensación. | El Arrendatario o el Propietario cancelan desde `Reservada`, o transcurren los 30 min de espera sin presentación. |
| Expirada | El TTL venció sin confirmación de pago; la embarcación vuelve a `Disponible` en Módulo 1. | Vence el TTL (iniciado en `Iniciada`) sin pago confirmado. |
| Pago Fallido | Rechazo definitivo del cobro que no admite reintento; la embarcación vuelve a `Disponible` en Módulo 1. | El cobro se rechaza sin posibilidad de reintento dentro del TTL. |

**Sub-estados de cancelación** (sobre `Cancelada`): `Flexible` (>72h), `Moderado` (72h–24h), `Tardío` (<24h), `Por Propietario` y `Por Inasistencia`.

**Vocabulario de Finanzas**: "Pendiente" se corresponde con `Pendiente de Pago`; "Reservado" con `Reservada`; "Cancelado Flexible/Moderado/Tardío" con `Cancelada` + su sub-estado. `Expirada` y `Pago Fallido` son estados de la reserva en Módulo 2; Módulo 3 los recibe como notificaciones de estado y libera la retención del inventario.

**Coherencia de estados**: Módulo 2 y Módulo 3 reconocen el **mismo conjunto de estados**. Módulo 2 maneja 8 estados principales más 5 sub-estados de cancelación (definidos por CU-08 FR-002/FR-003); Módulo 3 distingue las compensaciones por el sub-estado recibido. `Disponible`/`En Mantenimiento` en el flujo son **estados de la embarcación (Módulo 1)**, no estados de la reserva.

---

## 3. Garantía (Depósito)

- **Qué es**: 10% de la tarifa base diaria de la embarcación.
- **Momento en la reserva**: se calcula y se congela en "Solicitar el valor calculado de la reserva" (antes de confirmar pago); se cobra junto con alquiler y seguro como **un único monto** en "Procesar cobro" (sin operación separada en la pasarela, aunque Módulo 3 conserva el desglose internamente); se retiene tras "Completada" hasta que se resuelve la disputa de garantía.
- **Qué módulo interviene**: Módulo 2 crea y gestiona la disputa (otorga la ventana para reportar daños y decide el resultado operativo); Módulo 3 solo ejecuta la consecuencia financiera (reembolso o liquidación total) a partir del estado recibido, usando montos que ya tiene registrados internamente — nunca recibe montos de Módulo 2.
- **Información que Módulo 2 debe enviar a Módulo 3**: únicamente identificador de reserva, identificador de disputa, estado (`RECHAZADA`/`ACEPTADA`), clave idempotente y, si es `RECHAZADA`, un motivo opcional. Nunca montos ni instrucciones de pago.
- **Qué ocurre después de resolver**: el depósito se entrega **completo** al Arrendatario o **completo** al Propietario; no existe retención parcial en ningún caso.

---

## 4. Disputa de garantía

- **Qué se considera**: el proceso mediante el cual se decide si el depósito se devuelve al Arrendatario o se liquida al Propietario, según si se detectaron daños menores al regreso de la embarcación.
- **Cuándo se genera**: tras "Completada", dentro de la ventana que Módulo 2 concede al Propietario para reportar daños (`contexto-modulo3.md` fija esta ventana en 24 horas; `sea-share.md` no la menciona — ver §6).
- **Quién interviene**: el Propietario (reporta o no reporta daños) y el Módulo 2 (crea y gestiona la disputa, incluida la evaluación de procedencia del reclamo).
- **Qué módulo la gestiona**: Módulo 2 gestiona la disputa por completo; Módulo 3 únicamente consume su resultado final y ejecuta la operación financiera correspondiente.
- **Estados** (definidos solo en `contexto-modulo3.md`; `sea-share.md` no usa el término "disputa" ni estos nombres — ver §6):
  - `PENDIENTE`: existe o continúa en revisión; sin acción financiera.
  - `RECHAZADA`: el reclamo no procede (incluye ausencia de reclamo al vencer la ventana); depósito 100% al Arrendatario.
  - `ACEPTADA`: el reclamo procede; depósito 100% al Propietario.
- **Qué ocurre al resolverse**: Módulo 2 notifica el estado final a Módulo 3, que ejecuta el reembolso o la liquidación total con sus propios registros.
- **Efecto sobre reserva/garantía**: cierra el ciclo financiero del depósito de esa reserva. Mientras no exista `RECHAZADA` o `ACEPTADA`, el depósito se considera "pendiente de resolución" y puede seguir contabilizándose en reportes financieros sucesivos.

---

## 5. Interacciones entre módulos

| Acción / evento | Módulo que inicia | Módulo que recibe | Información relevante |
| --- | --- | --- | --- |
| Solicitar estimación para reserva | Módulo 2 | Módulo 3 | Lista de IDs de embarcación (fechas/pasajeros opcionales). |
| Brindar información de reserva | Módulo 2 | Módulo 3 | ID reserva, ID embarcación, días, pasajeros, propietario, capacidad máxima. Unidireccional: Módulo 3 no responde. |
| Solicitar el valor calculado de la reserva | Módulo 2 | Módulo 3 | ID reserva → desglose (alquiler, seguro, depósito, total). |
| Procesar cobro | Módulo 2 (reserva en `Pendiente de Pago`) | Módulo 3 | ID reserva, token/referencia segura de pago. |
| Solicitar confirmación de pago | Módulo 2 | Módulo 3 | ID reserva → estado del cobro. |
| Brindar el estado de la reserva | Módulo 2 | Módulo 3 | ID reserva, estado principal y sub-estado de cancelación si aplica. Unidireccional: Módulo 3 no responde ni notifica fallos a Módulo 2. |
| Brindar información de disputa de garantía | Módulo 2 | Módulo 3 | ID reserva, ID disputa, estado (`RECHAZADA`/`ACEPTADA`), clave idempotente, motivo opcional. Unidireccional, sin montos. |

---

