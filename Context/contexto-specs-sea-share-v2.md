# Contexto del Proyecto: Sea-Share — Specs por Caso de Uso (Módulo 2) — v2

## 1. Qué es el proyecto

Sea-Share es un marketplace P2P de alquiler náutico (turistas alquilan embarcaciones a propietarios/anfitriones). El sistema está dividido en 3 módulos, tratados como **APIs independientes**:

- **Módulo 1 – Gestión de Embarcación**: dueño de la entidad Embarcación (inventario, atributos, estado operativo: Disponible / Reservado / En Navegación / En Mantenimiento).
- **Módulo 2 – Gestión de Reserva** *(el que estamos especificando)*: gestiona el ciclo de vida de la reserva — creación, tiempo, cancelaciones, estado, navegación.
- **Módulo 3 – Gestión de Liquidación**: finanzas — pagos, cotizaciones, comisiones, montos y porcentajes de reembolso/penalidad, dispersión de fondos al propietario. **Módulo 3 maneja internamente todos los montos económicos; Módulo 2 nunca calcula ni recibe de vuelta cifras de dinero, solo tipos/clasificaciones.**

Módulo 2 **consume** APIs de Módulo 1 (consulta info y estado de embarcación) y **coordina** con Módulo 3 (pagos, cotizaciones, cancelaciones), sin conocer el detalle financiero interno de ninguno de los dos.

## 2. Metodología de specs

Un archivo `.md` por caso de uso del diagrama UML de casos de uso de Módulo 2 (spec-driven development, estilo GitHub Spec Kit). Cada spec contiene: User Stories priorizadas (P1, P2...) como rebanadas independientes y testeables de la misma feature, Acceptance Scenarios Given/When/Then, Edge Cases, Functional Requirements (FR-XXX, "el sistema DEBE..."), Key Entities, Success Criteria (SC-XXX). Lo no definido se marca `[NEEDS CLARIFICATION: ...]`, nunca se inventa.

## 3. Reglas de modelado acordadas

1. **`<<include>>` = delegación obligatoria, sin duplicar lógica.** El spec base solo dice "el sistema DEBE invocar X" y referencia su archivo. El detalle vive UNA sola vez, en el spec incluido. **Ya detectamos y corregimos dos violaciones de esta regla**: (a) "Iniciar reserva" tenía la lógica de validación de fechas que le correspondía a "Establecer tiempo reserva"; (b) "Solicitar cancelación" tenía la lógica de clasificación 72h/24h que le corresponde a "Calcular tipo de cancelación". **Al escribir cada nuevo spec, revisar explícitamente si algo de su contenido debería vivir en un `<<include>>` en vez de ahí mismo.**

2. **Cuándo separar un paso en su propio caso de uso/spec**: si (a) lo reutiliza más de un caso de uso, o (b) tiene lógica de negocio propia no trivial, aunque solo lo use un caso de uso.

3. **Criterio para decidir si un caso de uso del diagrama lleva spec propio: la FRONTERA DEL RECTÁNGULO, no si depende de una API externa.**
   - **Fuera del rectángulo de Módulo 2** (pertenecen a Módulo 1 o Módulo 3: "Proveer información de embarcación", "Brindar información estado operativo", "Recibir estado de reserva", "Recibir solicitud de pago", "Confirmar pago", "Solicitar información de la reserva", "Solicitar tipo de cancelación") → **NO llevan spec de nuestro equipo**. Se documentan solo como dependencia dentro del FR del caso de uso nuestro que las invoca, y su contrato detallado (endpoint, payload, manejo de errores) va en un documento de "contrato de integración" aparte — no con la plantilla de caso de uso.
   - **Dentro del rectángulo de Módulo 2** (los 11 casos de uso propios) → **SÍ llevan spec completo**, aunque consuman datos de otro módulo (ej. "Proveer información cotización de reserva" y "Calcular tipo de cancelación" consumen datos de Módulo 3, pero sí son nuestros).

4. **`<<extend>>` cuestionado y corregido**: "Solicitar cancelación" NO depende de haberse ejecutado recién "Iniciar reserva" — solo depende de que exista una reserva en estado cancelable, sin restricción temporal. El spec de "Iniciar reserva" no contiene ninguna historia de cancelación.

5. **Regla "origen vs. ejecución"**: el spec donde se dispara la acción documenta el "qué y cuándo" (trigger); el spec que la ejecuta documenta el "cómo". Ejemplo aplicado: el TTL de 15 min se origina y referencia en "Iniciar reserva" (FR-007 a FR-009), pero la transición de estado la ejecuta "Actualizar estado reserva" (aún no especificado).

6. **Regla de negocio del TTL** (viene del .md de descripción, no estaba en el diagrama): al crear una reserva exitosamente (después de que "Establecer tiempo reserva" valide el periodo), se bloquea la embarcación por 15 minutos. Si no se confirma pago vía Módulo 3 en ese lapso, la reserva expira automáticamente y dispara "Actualizar estado reserva". Ya documentado en `spec-iniciar-reserva.md`.

7. **Solicitar cancelación tiene dos historias, no una** — Arrendatario y Propietario, con reglas distintas: Arrendatario aplica las franjas 72h/24h/proyecto; Propietario se clasifica siempre como "Cancelación por Anfitrión" sin franja horaria, con `[NEEDS CLARIFICATION]` sobre la política exacta de reembolso al Arrendatario en ese caso.

8. **"Calcular tipo de cancelación" es quien de verdad clasifica** (Flexible/Moderada/Tardía/Cancelación por Anfitrión); "Solicitar cancelación" solo delega hacia él y actúa sobre el resultado (notificar a Módulo 3, transicionar estado, notificar a Módulo 1). Módulo 2 nunca calcula ni recibe montos — solo comunica el tipo/clasificación; los montos los resuelve Módulo 3 internamente.

## 4. Reglas de estilo — el spec NO debe contener decisiones de diseño técnico

Detectamos varias veces que el agente generador (opencode) se adelantaba a fase de implementación dentro del spec. Vigilar y corregir siempre que aparezca:

- **Nada de notación matemática/LaTeX** ($T_{cancel}$, $\Delta T$, etc.) en Acceptance Scenarios ni FR — todo en lenguaje llano ("con más de 72 horas de anticipación", no fórmulas.
- **Nada de nombres de clase/tabla en Key Entities** (ej. `BookingLock`, `CancellationEvent` con backticks) — son entidades de dominio en prosa, no diseño de esquema.
- **Nada de mecanismos de concurrencia/persistencia explícitos** (lock optimista vs. pesimista, transacciones T1/T2) en Acceptance Scenarios — el comportamiento se describe observable ("el sistema garantiza que solo una se registre"), el mecanismo queda en `[NEEDS CLARIFICATION]`.
- **Nada de políticas de reintento/entrega de mensajes** (retry policies, colas) en FR — se expresa la necesidad de negocio ("Módulo 1 y Módulo 3 deben enterarse, sin pérdida de eventos"), el mecanismo es diseño posterior.
- **No enumerar payloads/campos exactos de integración** (id, timestamp, actor...) dentro de Acceptance Scenarios si el FR correspondiente todavía tiene `[NEEDS CLARIFICATION]` sobre el formato — es una contradicción; el detalle de contrato va aparte.

## 5. Estado actual de los specs (Módulo 2)

| Spec | Estado |
|---|---|
| `spec-iniciar-reserva.md` | **Completo** (v2, con TTL incorporado, sin historia de cancelación) |
| `spec-establecer-tiempo-reserva.md` | Completo, con correcciones pendientes de aplicar: sacar `BookingLock`/margen de Key Entities, reescribir escenario de concurrencia sin lenguaje técnico, y resolver ambigüedad de quién dispara "Proveer información cotización de reserva" (¿este spec o "Iniciar reserva"? — pendiente de decidir) |
| `spec-solicitar-cancelacion.md` | Completo tras corrección de estilo (sin LaTeX, sin contradicción de payload); pendiente aplicar el fix de mover la clasificación 72h/24h a "Calcular tipo de cancelación" (quitar FR-005/006, colapsar los 3 escenarios de US1 en uno genérico) |
| `spec-calcular-tipo-cancelacion.md` | **Por crear** — aquí va la lógica real de clasificación (Flexible/Moderada/Tardía/Cancelación por Anfitrión), recibida como `<<include>>` de "Solicitar cancelación", e incluye a su vez la API externa "Solicitar tipo de cancelación" (Módulo 3) |
| `spec-actualizar-estado-reserva.md` | No iniciado — "hub" de transiciones de estado (inasistencia, inicio/fin de navegación, confirmación de pago, expiración por TTL) |
| `spec-proveer-informacion-cotizacion-reserva.md` | No iniciado — consume datos/tarifas de Módulo 3 |
| Resto (Marcar inasistencia, Iniciar pago, Marcar inicio/fin de navegación) | No iniciados |

## 6. `[NEEDS CLARIFICATION]` abiertos globalmente

- Timeout/falla de la API de Módulo 1 al consultar embarcación/estado operativo.
- Duración mínima/máxima de una reserva.
- Estrategia de concurrencia para evitar traslapes simultáneos (mecanismo concreto, no si debe existir — eso ya es un requisito).
- Margen de limpieza/traslado entre reservas consecutivas.
- Máquina de estados completa de la entidad Reserva (nombres exactos de cada estado y transiciones válidas — usada por casi todos los specs).
- Race condition entre expiración de TTL y confirmación de pago casi simultáneas.
- Si Módulo 2 gestiona identidad de usuarios o eso viene de otro servicio.
- SLA de respuesta esperado de Módulo 1 y Módulo 3.
- Política de reembolso al Arrendatario cuando el Propietario cancela.
- Formato/payload exacto de los contratos de integración con Módulo 1 y Módulo 3 (pendiente documento de "contrato de integración" aparte, aún no iniciado).

## 7. Pendiente transversal

Falta crear los **documentos de contrato de integración** (formato distinto a la plantilla de caso de uso) para: "Proveer información de embarcación" + "Brindar información estado operativo" (Módulo 1), y "Solicitar tipo de cancelación" + "Recibir solicitud de pago" + "Confirmar pago" (Módulo 3). Estos documentan el lado de Módulo 2 de cada integración: qué se envía, qué se espera recibir, y qué hacer ante fallas/timeouts.
