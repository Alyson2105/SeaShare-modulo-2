# Implementation Plan: Módulo 2 — Operación de Reservas, Tiempos y Cancelaciones (SEA-SHARE)

**Date**: 2026-09-27
**Spec**: [`context/sea-share.md`](context/sea-share.md) · [`context/consistencia-m2-m3.md`](context/consistencia-m2-m3.md) · [`features/CU-01..CU-21/spec.md`](features/)
**Alcance**: Plan **general** del módulo backend. Los planes técnicos detallados de cada caso de uso se redactarán en una tarea posterior, uno por CU.

## Summary

Este plan define la estrategia de construcción del **Módulo 2** de SEA-SHARE: un servicio backend en **Java 21 + Spring Boot 3.5**, empaquetado con **Maven**, que usa **RabbitMQ** como broker de mensajería para la integración asíncrona y la entrega garantizada de eventos hacia el Módulo 3 ("el sistema").

El módulo es dueño del **ciclo de vida de la reserva** (21 casos de uso: CU-01 a CU-18 operativos y CU-19 a CU-21 de vista de consulta) y se integra con dos sistemas externos mediante contratos de API: **Módulo 1** (Gestión de Flota y Activos P2P) como fuente de verdad de la flota y **Módulo 3** (Liquidación, Seguros y Dispersión de Fondos) como autoridad única en todo cálculo monetario.

Dos restricciones del contexto gobiernan toda la arquitectura:

1. **Regla de negocio estricta "Sin dinero"** (FR-010 de CU-08, FR-002 de CU-11 y CU-12, FR-013 de CU-04): Módulo 2 **jamás** calcula, suma, redondea, retiene ni convierte importes monetarios. Todo monto proviene de Módulo 3. Esta regla es la razón de ser de la separación entre los adaptadores de lectura financiera y el dominio de reservas.
2. **Módulo 1 es la única fuente de verdad de la flota**: Módulo 2 no accede a su base de datos, no cachea sus datos técnicos (nombre, tipo, puerto, servicios); solo persiste los identificadores (`embarcacion_id`, `propietario_id`) como referencia, cf. CU-09 Edge Case y CU-20 FR-002. La enunciación de "cero (0) accesos directos a la base de datos de Módulo 1" en CU-09 y CU-10 es un requisito arquitectónico, no una sugerencia.

La consecuencia de diseño es una **arquitectura hexagonal con un único escritor de estado**: CU-08 (`Actualizar estado de reserva`) es la única vía por la que cambia el estado de una reserva, lo que permite un control de concurrencia y una auditoría centralizados, y convierte a la reserva en el *aggregate root* del módulo.

## Technical Context

**Language/Version**: Java 21 (LTS)

**Primary Dependencies**: Spring Boot 3.5.x · Spring Web · Spring Validation · Spring Data JPA · Spring Security · Spring AMQP (`spring-boot-starter-amqp`, cliente RabbitMQ) · Spring Actuator · Flyway (migraciones versionadas) · Resilience4j (timeouts, reintentos y circuit breaker de clientes externos) · springdoc-openapi (contratos de API)

**Storage**: **PostgreSQL 16** con claves primarias `UUID` (vía `gen_random_uuid()`), columnas de timestamp con zona horaria (`timestamptz`), *optimistic locking* con columna de versión (CU-08 FR-006), bloqueo pesimista de filas `SELECT … FOR UPDATE` donde lo exija la atomicidad (CU-03), y **migraciones versionadas con Flyway** (el esquema de `reserva` y `disputa_garantia` es estructura de datos propia, no un esquema heredado). Se eligió por: soporte nativo de `UUID`, `timestamptz`, `@Version`, locks de fila, rango de exclusividad `EXCLUDE`/`btree_gist` para duplicados de `(embarcación, fechas)` si se requiere, y soporte maduro en Flyway y Testcontainers.

**Testing**: JUnit 5 · Mockito · Spring Boot Test · Testcontainers (broker RabbitMQ y base de datos, para tests de integración reales) · WireMock (dobles de contrato de Módulo 1 y Módulo 3)

**Target Platform**: Linux server, JVM 21, despliegue en contenedor

**Project Type**: Backend REST + worker de mensajería. Servicio único que expone API HTTP y publica y consume mensajes RabbitMQ.

**Performance Goals** (consolidados de los `SC` de los specs; nótese la inconsistencia entre ellos):

| Objetivo | Fuente |
| :--- | :--- |
| Consultas a Módulo 1 (`Consultar información de embarcación`, estado operativo) < 300 ms | SC-001 de CU-09 y CU-10 *(confirmado por negocio el 2026-10-07)* |
| Búsqueda en catálogo con cotización en lote < 2000 ms | SC-002 de CU-01 *(confirmado por negocio el 2026-10-07)* |
| Consulta de información de reserva para Módulo 3 < 200 ms | SC-001 de CU-15 |
| Notificaciones a APIs externas tras consolidar el cambio < 500 ms | SC-002 de CU-08 y CU-14 |
| Notificaciones de Módulo 1 / Módulo 3 en < 1 s (cancelación, inasistencia, check-in, check-out, confirmación de pago) | SC-005 de CU-04, SC-004 de CU-05, SC-002 de CU-06 y CU-07, SC-001 de CU-13 |
| Lote e individual de cotización, y liquidación final | **NEEDS CLARIFICATION** — CU-11 y CU-12 dejan los SLA como placeholders ("p. ej. 800 ms", "p. ej. 1500 ms") |

**Constraints**:

- **Cero aritmética monetaria en el código de Módulo 2** (SC-003 de CU-02, SC-003 de CU-03, SC-007 de CU-04, SC-001 de CU-12, FR-010 de CU-08). Los importes se transportan como `BigDecimal` opacos devueltos por Módulo 3.
- **Cero sobreventa ante pagos concurrentes** (SC-002 de CU-03) y cero eventos perdidos silenciosamente (SC-006 de CU-04). Esto obliga a un patrón transaccional de tipo *outbox*: la transición de estado y el evento saliente deben confirmarse juntos.
- **Fail-safe ante fallo de dependencia externa**: hacia Módulo 1 se admite como máximo un (1) reintento rápido ante timeout o desconexión transitoria; si el reintento también falla, el 100% de los fallos de conexión producen rechazo preventivo, no una decisión optimista (FR-008 de CU-09, FR-006 de CU-10). Módulo 1 y Módulo 3 nunca se consultan por vía alternativa, ni caché ni base de datos.
- **Cero montos ni cálculos de dinero en los mensajes** hacia Módulo 3: ni en `Recibir estado de reserva` (SC-003 de CU-14) ni en `Recibir información de disputa de garantía` (FR-003 y SC-002 de CU-18).
- **Idempotencia obligatoria**: en la **entrada**, CU-13 FR-007 (clave idempotente por par `(id_transaccion_externo, resultado)`), y CU-16 y CU-18 (identificador único por transición de disputa para deduplicación del lado de Módulo 3); en la **salida**, CU-08 FR-011 y CU-14 portan un `eventId` único por transición (patrón outbox) para que Módulo 3 deduplique los eventos.
- **Evaluación de ventanas temporales siempre en la zona horaria del puerto de atraque** obtenida de Módulo 1, nunca en hora de servidor ni de dispositivo (FR-004 de CU-04 y CU-05; CU-09 US2). Si Módulo 1 no resuelve la zona horaria, la operación se detiene y se devuelve indisponibilidad de servicio: no existe zona horaria por defecto.
- **La integración con Módulo 1 es exclusivamente por API** (FR-007 de CU-10). Módulo 2 no implementa el inventario físico.
- **Atribución de términos** según `consistencia-m2-m3.md` §1: se usa **Arrendatario** y **Propietario** (nunca "turista" ni "anfitrión"), **"el sistema"** para Módulo 3 (nunca "Módulo 3" dentro del vocabulario de SPEC/Finanzas) y **"Sistema de Reservas y Operaciones"** para Módulo 2 en las interacciones con Finanzas.

**Scale/Scope**: 1 servicio · 21 casos de uso · 2 contextos acotados (Reservas, Disputa de Garantía) · 2 sistemas externos · 7 interacciones formalizadas (tabla §5 de `consistencia-m2-m3.md`) · Volumen de diseño asumido por el equipo *(a validar con negocio)*: **5.000 usuarios activos/mes · 200 reservas/día · 50 reservas concurrentes en pico · 20 RPS en el pico**.

### Decisiones arquitectónicas transversales

**1. Hexagonal con un único escritor de estado.** El dominio de reservas no depende de Spring Web, JPA ni AMQP. Los casos de uso de UI (CU-01 a CU-07), el webhook de Módulo 3 (CU-13) y los temporizadores (TTL de 15 min, ventana de 24 h) convergen todos en CU-08, que es el *único* mutador del estado de la reserva (FR-001). Esto hace la máquina de estados verificable mediante tests unitarios exhaustivos, sin necesidad de contenedor.

**2. Outbox transaccional + RabbitMQ para toda notificación saliente.** El contexto exige "0% de eventos perdidos" (edge case de CU-08, SC-001 de CU-14, FR-005 de CU-18: "reintenta hasta confirmarla en el broker"). Un *fire-and-forget* no lo garantiza. Se usa un patrón *outbox*: la transición de estado y el registro del evento se escriben en la misma transacción local; un *relay* scheduler publica en RabbitMQ con *publisher confirms* y marca el evento como confirmado solo tras el *ack* del broker. Esto resuelve además un problema de consistencia que los specs no abordan: Módulo 1 no participa de la transacción local, por lo que una transición confirmada puede quedar sin su notificación a Módulo 1 y sin posibilidad de *rollback*. El outbox convierte esa divergencia en un evento reintentable en lugar de una pérdida silenciosa.

**3. Topología RabbitMQ.** Exchange de tipo `topic` para la integración con Módulo 3 (enrutamiento flexible por tipo de evento: `reserva.estado.cambio`, `disputa.estado.cambio`); colas con *dead letter*; *manual ack* en todos los consumidores; reintentos con *backoff* progresivo antes de derivar a la DLQ. Parámetros de reintento fijados para CU-08 y CU-14: máximo 5 intentos (primer intento inmediato), backoff exponencial con jitter de 1 s, 5 s, 25 s y 125 s, solo ante 5xx/timeout y derivación a DLQ con alerta tras el quinto fallo. La retención, los tópicos, el *partitioning* y las garantías de orden de la cola de disputas (CU-18) siguen diferidos al contrato de integración (ver *Riesgos*).

**4. Reloj inyectable y zona horaria por puerto.** Todas las ventanas (15 min de TTL, 72 h y 24 h de cancelación, 30 min de inasistencia, 24 h de disputa) se evalúan contra un `Clock` inyectado y contra el `ZoneId` del puerto devuelto por Módulo 1. Esto hace deterministas los tests de *border* (exactamente 72 h, exactamente 24 h, exactamente 30 min), que los specs exigen explícitamente.

**5. Temporizadores durables, no en memoria.** El TTL de 15 min y el cierre automático de la disputa a las 24 h deben sobrevivir a un reinicio. Por eso se modelan como columnas de vencimiento (`timestamptz`) consultadas por un job `@Scheduled` de barrido en **PostgreSQL 16**, y no como `ScheduledExecutorService`.

**6. Seguridad por roles y por identidad de servicio.** Tres roles de negocio: **Arrendatario**, **Propietario** (debe ser el propietario registrado de la embarcación de esa reserva) y **Admin** (exclusivo de CU-17, que "no introduce montos ni ejecuta operaciones de pasarela"). Módulo 3 entra como identidad de servicio para CU-13 y CU-15. **NEEDS CLARIFICATION**: los specs no especifican mecanismo de autenticación, modelo de tokens, TLS, manejo de PII, *rate limiting* ni protección del log de auditoría. Bloqueante para la Fase 2.

**7. Trazabilidad.** CU-09 FR-010 y CU-10 FR-009 exigen registrar cada consulta externa; CU-15 expone datos históricos. Se usa logging estructurado con *correlation id* propagado en headers HTTP y en headers AMQP.

**8. Fuera de alcance: la UI.** Varios specs contienen requisitos de presentación (por ejemplo FR-009 a FR-013 de CU-03: imagen de portada, *countdown*, banner de advertencia, checkbox de política de cancelación). Este plan cubre **solo el backend**; esos requisitos se entregarán como contrato de API y su implementación visual corresponde a un cliente externo.

## Project Structure

### Documentation (this feature)

```text
Documentación/
├── plan.md                    # Este archivo — Plan general del Módulo 2
├── context/                   # Solo lectura — no modificar
│   ├── sea-share.md
│   └── consistencia-m2-m3.md
├── features/                  # Solo lectura en esta tarea — planes por CU luego
│   └── CU-01..CU-21/
│       ├── spec.md            # (existente)
│       └── plan.md            # (se creará en la tarea posterior, un archivo por CU)
├── diagrams/                  # Solo lectura
└── templates/                 # Solo lectura
```

### Source Code (repository root)

Estructura **por contexto acotado**, y dentro de cada uno por capa hexagonal (`domain` → `application` → `infrastructure`), con un paquete `shared` para lo transversal. Se elige esta opción porque los 21 CUs se reparten de forma natural en dos contextos con máquinas de estado propias (Reservas y Disputa de Garantía), y porque los specs describen fronteras estrictas ("API externa", "sin acceso directo a BD", "sin cálculos de dinero") que la estructura debe hacer explícitas.

```text
SeaShare-modulo-2/
├── pom.xml
├── Dockerfile
├── docker-compose.yml              # RabbitMQ + PostgreSQL 16
├── .gitignore
├── README.md
│
├── src/main/java/com/seashare/m2/
│   ├── M2Application.java
│   │
│   ├── shared/                                     # Transversal (Fase 2)
│   │   ├── error/               # GlobalExceptionHandler, jerarquía de errores de dominio
│   │   ├── security/            # Roles, autorización, identidad de servicio de Módulo 3
│   │   ├── time/                # ClockPort, ZoneId por puerto
│   │   ├── messaging/           # Topología RabbitMQ, outbox, DLQ, política de reintentos
│   │   ├── persistence/         # BaseEntity, auditing, migraciones, locking
│   │   ├── web/                 # Manejo global de errores, versionado de API, OpenAPI
│   │   └── observability/       # Correlation id, MDC, métricas
│   │
│   ├── reservas/                                  # Contexto: Reservas — CU-01 a CU-15, CU-19 a CU-21
│   │   ├── domain/
│   │   │   ├── model/           # Reserva, EstadoReserva, SubEstadoCancelacion,
│   │   │   │                     # AuditoriaEstado, CheckIn, CheckOut, Cancelacion, NoShow
│   │   │   ├── state/           # TransicionValida, MaquinaDeEstados  (CU-08)
│   │   │   ├── policy/          # VentanaCancelacion, VentanaNoShow, VentanaTTL  (sin dinero)
│   │   │   └── event/           # Eventos de dominio de la reserva
│   │   ├── application/
│   │   │   ├── port/in/         # Puertos de entrada (uno por CU)
│   │   │   ├── port/out/        # Puertos de salida: repositorios, Módulo 1, Módulo 3, reloj
│   │   │   └── usecase/         # Orquestación de cada caso de uso
│   │   └── infrastructure/
│   │       ├── web/             # Controllers REST          (CU-01..07, 13, 15, 19, 20, 21)
│   │       ├── client/          # Clientes HTTP Módulo 1 / Módulo 3
│   │       │                    #   (CU-09, 10, 11, 12, 14)
│   │       ├── messaging/       # Publicadores, outbox relay, consumidores
│   │       ├── persistence/     # Entidades JPA, repositorios, mappers
│   │       └── scheduler/       # Job de expiración del TTL de 15 min
│   │
│   └── disputa/                                   # Contexto: Disputa de Garantía — CU-16 a CU-18
│       ├── domain/
│       │   ├── model/           # DisputaGarantia, EstadoDisputa, VentanaReclamo, Reclamo
│       │   ├── state/           # Maquina de estados de la disputa  (CU-17)
│       │   └── event/           # EventoDisputaGarantia
│       ├── application/
│       │   ├── port/in/         # (CU-16, CU-17)
│       │   ├── port/out/        # (CU-18 → publicación de eventos)
│       │   └── usecase/
│       └── infrastructure/
│           ├── web/             # Controller de reclamo y de decisión Admin
│           ├── messaging/       # Publicador RabbitMQ de la disputa  (CU-18)
│           ├── persistence/     # Repositorio de disputas
│           └── scheduler/       # Job de cierre automático de la ventana de 24 h
│
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   ├── application-test.yml
│   └── db/migration/            # Migraciones versionadas (Flyway / Liquibase)
│
└── src/test/java/com/seashare/m2/
    ├── shared/                  # Fixtures, bases de pruebas, Testcontainers compartidas
    ├── reservas/
    │   ├── domain/              # Tests unitarios de la matriz de estados y de las políticas
    │   ├── contract/            # Tests de contrato contra los dobles de Módulo 1 / Módulo 3
    │   ├── integration/         # Tests de integración (persistencia + outbox + RabbitMQ)
    │   └── web/                 # Tests de endpoint (MockMvc / WebTestClient)
    └── disputa/                 # Mismo desglose que reservas/
```

**Structure Decision**: proyecto único (opción por defecto, sin frontend), organizado en **dos contextos acotados** — `reservas` y `disputa` — porque cada uno posee su propia máquina de estados, sus entidades y sus invariantes, y comparten solo la infraestructura transversal de `shared/`. Dentro de cada contexto se aplica *ports & adapters*: `domain/` sin dependencias de framework, `application/` con los puertos de entrada y salida, e `infrastructure/` con los adaptadores concretos (REST, HTTP saliente, RabbitMQ, JPA, schedulers). La frontera entre `domain` y el resto es lo que hace verificables por tests unitarios la regla "Sin dinero" y la matriz de transiciones de CU-08. El nombre del paquete raíz sigue el proyecto IntelliJ existente, `SeaShare-modulo-2`.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Inicialización del repositorio Spring Boot y de la estructura base del proyecto.

- [ ] T001 Inicializar el repositorio Maven con `spring-boot-starter-parent` 3.5.x y `java.version` 21
- [ ] T002 Declarar en `pom.xml` las dependencias base: `spring-boot-starter-web`, `-validation`, `-data-jpa`, `-security`, `-amqp`, `-actuator`, `springdoc-openapi`, `resilience4j-spring-boot3` y el driver `postgresql`
- [ ] T003 Configurar el build: `maven-compiler-plugin` (release 21), `maven-surefire-plugin` (tests unitarios), `maven-failsafe-plugin` (tests de integración) y `spring-boot-maven-plugin`
- [ ] T004 Crear la clase principal `M2Application` con *component scanning* de `com.seashare.m2`
- [ ] T005 Crear el árbol de paquetes de _Project Structure_ (`shared/`, `reservas/`, `disputa/`) y los `package-info.java` que documentan la regla "Sin dinero"
- [ ] T006 Externalizar la configuración en `application.yml` y perfiles `dev` / `test` / `prod` (host, puerto y credenciales de RabbitMQ; *datasource*; timeouts), sin secretos en el repositorio
- [ ] T007 Definir `docker-compose.yml` con RabbitMQ (management habilitado) y `postgres:16`
- [ ] T008 Crear el `Dockerfile` multi-stage para empaquetar el servicio
- [ ] T009 Configurar verificación de estilo y formato (Checkstyle o Spotless) integrada al build
- [ ] T010 Completar `.gitignore` y el `README.md` con la guía de arranque local

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Infraestructura crítica que debe existir **antes** de implementar cualquier caso de uso. Los 21 CUs dependen de estos elementos: todos los que cambian estado pasan por la persistencia transaccional y el outbox, todos los que validan ventanas dependen del reloj y de la zona horaria por puerto, y todos los que notifican a Módulo 3 dependen de la topología RabbitMQ.

- [ ] T011 **Manejo global de errores**: jerarquía de excepciones de dominio mapeada a código HTTP y cuerpo de error uniforme; validar que ninguna respuesta de error expone datos sensibles ni *stack traces*
- [ ] T012 **Seguridad**: autenticación y autorización por rol (**Arrendatario**, **Propietario**, **Admin**) más identidad de servicio para Módulo 3 (CU-13 y CU-15); endpoints protegidos por defecto; CORS y TLS. **NEEDS CLARIFICATION**: el mecanismo de autenticación no está definido en los specs
- [ ] T013 **Persistencia**: framework de migraciones versionadas y esquema base; entidad base con *auditing* (`creadoEn`, `actualizadoEn`), clave `UUID` y columna de *optimistic locking* para satisfacer FR-004 y FR-006 de CU-08
- [ ] T014 **Persistencia**: repositorios de `Reserva` y `DisputaGarantia` con soporte transaccional y de bloqueo pesimista donde la atomicidad lo requiera (adquisición del bloqueo de inventario en CU-03)
- [ ] T015 **RabbitMQ — topología**: exchange de tipo `topic`, colas de integración con Módulo 3, *bindings* por tipo de evento, colas de *dead letter* y política de retención
- [ ] T016 **RabbitMQ — entrega garantizada**: *publisher confirms*, *manual ack* en consumidores, reintentos con *backoff* exponencial con jitter (5 intentos: 1 s, 5 s, 25 s, 125 s; solo ante 5xx/timeout) y derivación a DLQ con alerta tras el quinto fallo
- [ ] T017 **Outbox transaccional**: tabla de salida, escritura conjunta con la transición de estado, *relay* scheduler con confirmación del broker y marcado del evento como entregado
- [ ] T018 **Reloj y zona horaria**: bean `Clock` inyectable y resolución de `ZoneId` a partir del puerto de atraque devuelto por Módulo 1, con *fail-safe* explícito (sin zona horaria por defecto)
- [ ] T019 **Clientes HTTP salientes**: cliente base para Módulo 1 y Módulo 3 con timeouts, un (1) reintento rápido máximo hacia Módulo 1 antes del *fail-safe* y reintentos de cola (según T016) hacia Módulo 3, *circuit breaker*, política *fail-safe* uniforme y propagación de *correlation id*
- [ ] T020 **Contratos de API**: versionado, DTOs de entrada y salida separados del dominio, convención de nombres JSON, `BigDecimal` para importes **opacos sin aritmética**, y especificación OpenAPI
- [ ] T021 **Observabilidad**: *correlation id* en HTTP y AMQP, logging estructurado con MDC, métricas Actuator y *health checks* de RabbitMQ y de la base de datos
- [ ] T022 **Infraestructura de testing**: contenedores Testcontainers para RabbitMQ y base de datos; servidor WireMock con los dobles de Módulo 1 y Módulo 3; base de pruebas reutilizable
- [ ] T023 **Idempotencia**: mecanismo transversal de claves idempotentes de entrada y de deduplicación de eventos salientes (satisface FR-007 de CU-13 y FR-004 de CU-18)

**Checkpoint**: la Fundación está lista — la implementación de los casos de uso puede comenzar en paralelo.

---

## Phase 3: Adaptadores de integración de solo lectura (P1)

**Qué CUs agrupa**: **CU-09** (Proveer información de embarcación), **CU-10** (Brindar información de estado operativo), **CU-11** (Proveer información cotización de reserva), **CU-12** (Brindar cálculo total de la reserva)

**Justificación**: los cuatro son adaptadores **sin máquina de estados, sin persistencia de dominio y sin efectos secundarios**: solo consultan Módulo 1 o Módulo 3 y devuelven el resultado sin transformarlo. Esto los convierte en el punto de entrada natural del proyecto: son verificables de forma aislada contra dobles de prueba, sin depender de ningún otro CU ni de la base de datos, y filtran y condicionan la frontera más delicada del sistema — el límite "sin dinero" y el comportamiento *fail-safe* ante fallos de Módulo 1. Además son **desbloqueantes**: CU-02 y CU-04 dependen de la zona horaria del puerto que entrega CU-09, CU-01 depende del modo lote de CU-11, y CU-03 depende de la liquidación final de CU-12. Empezar por ellos permite avanzar en paralelo con el núcleo de dominio y fija los contratos de integración con Módulo 1 y Módulo 3 antes de construir lógica de negocio encima.

> Las tareas técnicas detalladas de cada CU se definirán en su propio plan específico más adelante.

---

## Phase 4: Núcleo de dominio — máquina de estados de la reserva (P1)

**Qué CUs agrupa**: **CU-08** (Actualizar estado de reserva)

**Justificación**: es la pieza de mayor riesgo técnico y el **cuello de botella de todo el módulo**. FR-001 lo convierte en el único escritor de estado, y ocho componentes dependen de él (`Iniciar reserva`, `Iniciar pago`, `Confirmar pago`, `Marcar inicio de navegación`, `Marcar fin de navegación`, `Solicitar cancelación`, `Marcar inasistencia` y el temporizador del TTL). Concentra los requisitos más exigentes del módulo: matriz de transiciones validada estrictamente, atomicidad de guardado, control de concurrencia, inmutabilidad de los estados terminales, tabla de sincronización con Módulo 1 y con Módulo 3, y el barrido de expiración del TTL de 15 minutos. Debe resolverse antes que cualquier caso de uso que mute estado, porque de lo contrario cada CU construiría su propia lógica de transición y se perdería la garantía de consistencia que el propio spec declara como su propósito. Al ser lógica pura de dominio, se implementa y se verifica sin RabbitMQ ni base de datos.

> Las tareas técnicas detalladas de cada CU se definirán en su propio plan específico más adelante.

---

## Phase 5: Superficie de integración expuesta a Módulo 3 (P1)

**Qué CUs agrupa**: **CU-14** (Recibir estado de reserva), **CU-15** (Solicitar información de la reserva)

**Justificación**: son los dos puntos de contacto que la tabla §5 de `consistencia-m2-m3.md` define desde el lado de Módulo 2, y ambos dependen de la Fase 4 (CU-14 se dispara desde la máquina de estados; CU-15 consulta reservas ya persistidas). Se agrupan porque **comparten la verificación de una misma garantía**: que el módulo expone información operativa exacta, sin montos, sin cálculos y con idempotencia. CU-14 es además la **primera prueba real de la infraestructura de RabbitMQ construida en la Fase 2** (outbox, reintentos, DLQ), de modo que implementarlo aquí valida la Fase 2 antes de que el volumen de mensajes crezca con el resto de casos de uso. Cerrar esta fase completa el contrato de integración con Módulo 3 para el ciclo de la reserva.

> Las tareas técnicas detalladas de cada CU se definirán en su propio plan específico más adelante.

---

## Phase 6: Vistas de consulta — CU-19, CU-20, CU-21 (P1)

**Qué CUs agrupa**: **CU-19** (Ver detalle de embarcación), **CU-20** (Ver mis reservas), **CU-21** (Ver detalle de reserva)

**Justificación**: son los tres casos base de las relaciones `<<extend>>` del conjunto — CU-02 (`Iniciar reserva`) se ancla a CU-19, CU-01 extiende hacia CU-19, CU-04 (`Solicitar cancelación`) y CU-03 (`Iniciar pago`) se anclan a CU-21 (caso base de CU-03 resuelto el 2026-10-07). Los tres son de **solo lectura, sin máquina de estados ni efectos secundarios**: CU-19 consulta Módulo 1 (vía CU-09) y la cotización individual (vía CU-11); CU-20 y CU-21 leen reservas ya persistidas y exponen el detalle sin escrituras, en línea con las restricciones monetarias de CU-15 (exponen el total oficial, cero montos derivados). Se colocan como fase propia **antes del embudo** para que los casos base existan antes que sus extenders y para desbloquear la verificación de las extensiones que comparten los recorridos de las Fases 7 y 8. Dependen de la Fase 2 (persistencia de CU-20/CU-21) y de la Fase 3 (CU-19 requiere la cotización individual y los datos de Módulo 1).

> Las tareas técnicas detalladas de cada CU se definirán en su propio plan específico más adelante.

---

## Phase 7: Embudo de conversión — de la búsqueda al pago confirmado (P1)

**Qué CUs agrupa**: **CU-01** (Buscar embarcaciones disponibles), **CU-02** (Iniciar reserva), **CU-03** (Iniciar pago), **CU-13** (Confirmar pago)

**Justificación**: es la **vertical de mayor valor de negocio** del marketplace (sin ella el producto no genera ingresos) y forma un único recorrido de principio a fin: búsqueda con cotización por lote → creación de la reserva en `Iniciada` con el TTL en curso → adquisición del bloqueo de inventario y paso a `Pendiente de Pago` → confirmación del pago reportada por Módulo 3 y paso a `Reservada`. Se agrupan porque comparten la misma presión de concurrencia y el mismo indicador de negocio: dos Arrendatarios compiten por el mismo barco y las mismas fechas, y el spec exige cero sobreventa y respuesta atómica. Contiene la condición de no-bloqueo en `Iniciada` (varias reservas concurrentes para el mismo barco) y la carrera de sobreventa en `Pendiente de Pago`. Requiere las Fases 3 (contratos de cotización y liquidación), 4 (máquina de estados), 5 (notificación a Módulo 3) y 6 (vistas base de las extensiones CU-02 y CU-03) completas.

> Las tareas técnicas detalladas de cada CU se definirán en su propio plan específico más adelante.

---

## Phase 8: Operaciones en muelle — check-in, check-out, inasistencia y cancelación (P2)

**Qué CUs agrupa**: **CU-06** (Marcar inicio de navegación), **CU-07** (Marcar fin de navegación), **CU-05** (Marcar inasistencia), **CU-04** (Solicitar cancelación)

**Justificación**: los cuatro comparten **actor (Propietario), contexto físico (operación en el muelle) y punto de entrada** sobre una reserva que ya está `Reservada` o `En Navegación`. Dependen de la Fase 4, de la zona horaria del puerto entregada por CU-09 en la Fase 3 y de la Fase 6 (CU-21 es el caso base de `Solicitar cancelación`). Se agrupan y no se distribuyen porque comparten la misma familia de reglas de **ventana temporal y clasificación** (margen de salida, 30 minutos de tolerancia, 72 h y 24 h de cancelación) y porque las transiciones que producen convergen en las mismas ramifications de la máquina de estados. Dentro de la fase el orden sugerido es **check-in → check-out → cancelación → inasistencia**: los dos primeros son el camino feliz y los mejor especificados, mientras que los dos últimos concentran los `[NEEDS CLARIFICATION]` que siguen abiertos (clasificación del motivo del propietario en CU-04) y conviene abordarlos cuando la máquina de estados ya esté probada. El valor de negocio es completar el ciclo operativo del alquiler.

> Las tareas técnicas detalladas de cada CU se definirán en su propio plan específico más adelante.

---

## Phase 9: Disputa de garantía (P2)

**Qué CUs agrupa**: **CU-16** (Generar disputa de garantía), **CU-17** (Actualizar estado de disputa de garantía), **CU-18** (Recibir información de disputa de garantía)

**Justificación**: constituye un **contexto acotado independiente**, con su propia máquina de estados (`PENDIENTE` / `RECHAZADA` / `ACEPTADA`), sus propias entidades y sus propias invariantes, sin ninguna transición sobre la reserva. Solo es alcanzable a través de CU-07 (`Completada`) y puede desarrollarse **en paralelo con la Fase 8** una vez que exista la infraestructura de mensajería de la Fase 2. Se mantiene como fase aparte precisamente para permitir ese paralelismo y para no mezclar dos máquinas de estado en los mismos archivos. Concentra el requisito asíncrono más estricto del módulo: CU-18 publica **solo** en estados finales, **cero mensajes en `PENDIENTE`**, con identificador único para deduplicación, sin ningún monto ni instrucción de pago, y con reintento hasta confirmación del broker. Cierra el ciclo financiero del depósito de garantía.

> Las tareas técnicas detalladas de cada CU se definirán en su propio plan específico más adelante.

---

## Phase 10: Polish & Cross-Cutting Concerns

**Purpose**: Mejoras que afectan a múltiples casos de uso.

- [ ] TXXX Cierre de los `[NEEDS CLARIFICATION]` heredados de los specs y actualización de los planes por CU
- [ ] TXXX Endurecimiento de seguridad: _rate limiting_, protección del log de auditoría, validación de PII
- [ ] TXXX Pruebas de carga y verificación de los objetivos de latencia de la tabla de _Performance Goals_
- [ ] TXXX Observabilidad operativa: alertas por acumulación de mensajes en DLQ, por etapas de reintentos agotados y por fallos de notificación a Módulo 3
- [ ] TXXX Pruebas de resiliencia: caída de Módulo 1, caída de Módulo 3, reinicio del servicio con TTL y ventanas de disputa en curso, y reinicio del broker
- [ ] TXXX Documentación de la API y guía de arranque local
- [ ] TXXX Limpieza de código y refactorización final

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Fase 1)**: sin dependencias — puede comenzar de inmediato
- **Foundational (Fase 2)**: depende de la Fase 1 — **bloquea a todos los CUs**
- **Fase 3 — Adaptadores de lectura**: depende de la Fase 2 (clientes HTTP, manejo de errores). No depende de ninguna otra fase de CU
- **Fase 4 — Máquina de estados**: depende de la Fase 2 (persistencia, outbox, reloj). No depende de la Fase 3
- **Fase 5 — Superficie Módulo 3**: depende de las Fases 2 y 4
- **Fase 6 — Vistas de consulta**: depende de las Fases 2 (persistencia de CU-20/CU-21) y 3 (CU-19: cotización individual y datos de Módulo 1); CU-20 y CU-21 leen reservas gestionadas desde la Fase 4
- **Fase 7 — Embudo de conversión**: depende de las Fases 3, 4, 5 y 6
- **Fase 8 — Operaciones en muelle**: depende de las Fases 3, 4, 5 y 6
- **Fase 9 — Disputa de garantía**: depende de la Fase 2 y del disparo de CU-07 (Fase 8), aunque su núcleo de dominio puede desarrollarse en paralelo con la Fase 8
- **Polish (Fase 10)**: depende de las fases de CU deseadas

### Orden y paralelismo

```text
Fase 1 ──▶ Fase 2 ──┬──▶ Fase 3 (lectura) ──┬──▶ Fase 7 (conversión)
                    │                       │
                    ├──▶ Fase 4 (núcleo) ───┼──▶ Fase 8 (muelle) ──┬──▶ Fase 9 (disputa)
                    │         │             │                      │          ▲
                    │         └──▶ Fase 5 ───┘                      └──────────┘
                    │            (Módulo 3)
                    ├──▶ Fase 6 (vistas) ──▶ Fase 7 / Fase 8
                    └──▶ Fase 9 (dominio, en paralelo)
```

- **Fases 3 y 4** pueden ejecutarse simultáneamente tras completar la Fase 2: no comparten código de dominio.
- **Fase 6 (vistas)** puede ejecutarse en paralelo con las Fases 4 y 5 una vez cerradas las Fases 2 y 3.
- **Fases 7 y 8** pueden ejecutarse en paralelo una vez cerradas las Fases 3, 4, 5 y 6.
- **Fase 9** puede avanzar en paralelo con la Fase 8 si se acuerda el contrato de integración del disparo desde `Completada`.

### Dentro de cada CU

- Modelo de dominio antes que casos de uso; casos de uso antes que adaptadores
- Contrato de Módulo 1 / Módulo 3 definido antes de su adaptador
- Núcleo antes que integración
- Pruebas de la máquina de estados antes de exponer el endpoint
- Las tareas marcadas con `[P]` pueden ser paralelas entre sí dentro de una fase

---

## Riesgos y contradicciones detectadas en los specs

Esta sección **no resuelve** las inconsistencias: las señala para que se cierren antes de escribir el plan técnico del CU afectado.

> **Reauditoría 2026-10-06** contra el estado de los `spec.md` posterior al commit `c999890` ("Docs: Corregir inconsistencias de specs"). Los ítems cuya inconsistencia ya fue corregida en los specs se **eliminaron** de esta sección. Eliminados: A — expiración del TTL desde `Iniciada`, bloqueo en Módulo 1 al entrar en `Iniciada`, rubros derivados (Comisión/Neto/USD) en las vistas, CU-12 sobre reserva inexistente, `Pago Fallido` inexistente, dirección `<<include>>` CU-01/CU-11, publicación de la disputa `RECHAZADA`, tabla de sincronización con Módulo 1 y cobertura 18/21 (este plan ahora cubre CU-01 a CU-21, ver Fase 6); C — expiración del TTL en pantalla (CU-03 ya lo resolvió: inhabilitar botones, modal, redirigir a CU-01 y liberar el inventario); E — FR-005-enunciado-como-pregunta en CU-13 y variantes de nomenclatura (todas unificadas; sin rastros de "anfitrión").

> **Reauditoría 2026-10-07** (resolución de la sección **B. Vacíos que bloquean la redacción de un plan de CU**). Se resolvieron y **eliminaron** de B todos los ítems salvo B.1, B.2 y B.3 (renumerados; B.2/B.3 dependen del contrato de Módulo 3). Resueltos, con la decisión registrada en el spec indicado (y donde se diga, en este plan): **B.1** margen posterior y textos de CU-06 (`CU-06` US2/US3, FR-003) · **B.2** CU-10 referenciado por CU-02/CU-03 (`CU-02`, `CU-03`, `CU-10`) · **B.3** sin cancelación automática (`CU-07`) · **B.4** notificación asíncrona vía cola garantizada (`CU-14` FR-005; decisión 3) · **B.5** reintentos: máx. 5 con backoff 1/5/25/125 s solo ante 5xx/timeout + DLQ con alerta (`CU-08`, `CU-14` FR-005; decisión 3, T016) · **B.6** *(se mantiene abierta: ver B.1 — seguridad, decisión diferida)* · **B.7** idempotencia: `eventId`/outbox en salida + clave por transición en disputas (Constraints; `CU-08` FR-011) · **B.8** SLAs: 300 ms y 2000 ms confirmados (`CU-09`, `CU-10`, `CU-01`; tabla Performance Goals) · **B.9** volumen de diseño asumido (`Scale/Scope`) · **B.10** moneda según Módulo 3, persistida literal, sin mezcla (`CU-12`, `CU-21`) · **B.11** rechazo temporal 503 en CU-05 alineado con CU-04 (`CU-05`) · **B.12** reintento de pago dentro del TTL, `Expirada`/`Pago Fallido` (`CU-13` FR-005, US2) · **B.13/B.14** *(se mantienen abiertas: dependen del contrato de Módulo 3, ver B.2/B.3)* · **B.15** `@Version`/409 + bloqueo pesimista (`CU-08` FR-006, `CU-03`) · **B.16** 1 reintento rápido + fail-safe (`CU-09` FR-008, `CU-10` FR-006; Constraints, T019) · **B.17** idempotencia del webhook por `(id_transaccion_externo, resultado)` (`CU-13` FR-007). La sección **D** pasó a **C** y su ítem 2 (duplicado del resuelto B.16) se eliminó.

> **Reauditoría 2026-10-07** (resolución de las secciones **A. Contradicciones que afectan el agrupamiento y el orden de las fases** y **C. Decisiones de infraestructura no cubiertas por los specs**). Se resolvieron y **eliminaron** ambas secciones por completo. Resuelto — **A.1** modelo de creación unificado: la reserva nace en `Iniciada` y transiciona a `Pendiente de Pago` (`CU-11` FR-013/Key Entities/SC-006, `CU-12` SC-003, `CU-14` US1/FR-001/SC-001) · **A.2** caso base de `Iniciar pago` fijado en `CU-21 Ver detalle de reserva` (`CU-03` cabecera/US1/FR-001; `CU-21` declara la extensión) · **A.3** carrera sobre `Iniciada` resuelta en `Iniciar pago` (FCFS + bloqueo pesimista; `CU-02` Scenario 2/FR-010/FR-014/SC-002, `CU-11` edge case de concurrencia) · **A.4** `Iniciada` y `Pago Fallido` propagados a las listas de estados (`CU-04` FR-004/SC-001, `CU-05` FR-006, `CU-06` FR-004, `CU-14` US2, `consistencia-m2-m3.md` §2) · **A.5** `consistencia-m2-m3.md` §2 alineado a CU-08 (8 estados + 5 sub-estados de cancelación; TTL → `Expirada`; `Disponible`/`En Mantenimiento` son estados de la embarcación en Módulo 1) · **A.6** cancelación solo desde `Reservada` (rama de cancelación de `consistencia-m2-m3.md` §2) · **C.1** base de datos definida: **PostgreSQL 16** (Technical Context `Storage`, decisión transversal 5, T002, T007, docker-compose).

### B. Vacíos que bloquean la redacción de un plan de CU

1. **Seguridad subespecificada en todo el conjunto de specs**: solo hay chequeos de rol; no existe mecanismo de autenticación, modelo de tokens, TLS, manejo de PII, _rate limiting_ ni protección del log de auditoría. Bloqueante para la Fase 2. *(Decisión diferida por negocio el 2026-10-07; el mecanismo se definirá en la decisión transversal 6 y en T012 antes de la Fase 2.)*
2. CU-11: siguen abiertos el límite de lote de Módulo 3 (50 o 100, repetido en varios FR) y su SLA (placeholder). CU-12: siguen abiertos el nombre formal del endpoint (dos candidatos) y su SLA. Nota: CU-01 FR-008 y CU-11 FR-003 fijaron el tamaño de página de la vista en 20 embarcaciones, que no coincide con el límite por solicitud que impone Módulo 3.
3. CU-16, CU-17 y CU-18: cerrada la duda de la ventana de 24 h (es **fija**; se añadió como SLA en CU-16 FR-005, CU-17 FR-013 y CU-18 FR-007). Siguen abiertos: reclamo múltiple o editable dentro de la ventana (CU-16 D-01), política de reintento del job diferido (CU-16 D-03, ahora con `[NEEDS CLARIFICATION]` en el spec), si el Admin puede resolver una disputa `PENDIENTE` sin reclamo registrado (CU-17 D-01), longitud máxima del motivo (CU-17), y garantías de orden y versionado de `eventId` + detalles de la cola (CU-18 D-01/D-02, diferidos al contrato de integración). *(Dependen del contrato de integración con Módulo 3; sin resolver hasta entonces.)*

### C. Reauditoría 2026-10-08

> **Reauditoría 2026-10-08** contra el conjunto completo de especificaciones (`CU-01` a `CU-21`) y documentos de contexto (`sea-share.md`, `consistencia-m2-m3.md`). Se consolidaron 23 hallazgos: las correcciones mecánicas y decididas fueron aplicadas directamente en los `spec.md` y documentos afectados, mientras que los temas de arquitectura/negocio pendientes de alineación externa se registran como abiertos.

| # | Hallazgo | Alcance / Descripción | Estado |
| :--- | :--- | :--- | :--- |
| **H1** | Chequeo instantáneo vs rango de fechas en catálogo y reserva | Catálogo no valida rango de fechas (fuera de alcance; exclusividad FCFS en pago). CU-02 y CU-03 validan estado operativo instantáneo `Disponible` vía CU-10 bajo lock atómico. | `✅ aplicado` (`CU-01`, `CU-02`, `CU-03`) |
| **H2** | Notificación de desbloqueo a Módulo 1 según origen de `Expirada` | Expiración desde `Pendiente de Pago` notifica `Disponible` a Módulo 1. Expiración desde `Iniciada` NO notifica a Módulo 1 (nunca hubo bloqueo; notificar liberaría erróneamente otra reserva). | `✅ aplicado` (`CU-08`) |
| **H3** | Alcance del respaldo de bloqueo operativo en Módulo 1 | Respaldo del bloqueo de inventario ampliado a `Pendiente de Pago`, `Reservada` o `En Navegación`. | `✅ aplicado` (`CU-08`) |
| **H4** | Retención de garantía tras finalización de viaje | Al transicionar a `Completada`, Módulo 3 libera el pago al Propietario pero la garantía permanece retenida hasta la resolución de la disputa (ventana de 24 h). | `✅ aplicado` (`CU-07`, `CU-14`) |
| **H5** | Consistencia de estados de disputa y motivo opcional | Estados de disputa alineados a `RECHAZADA` / `ACEPTADA` (se descarta `PENDIENTE` en mensajes salientes). Motivo opcional añadido a evento en CU-18. | `✅ aplicado` (`consistencia-m2-m3.md`, `CU-18`) |
| **H6** | Campos y dirección de `CU-15` vs §5 de consistencia | Posible discrepancia de campos informativos y sentido de llamada en la interacción de consulta de reserva. | `⏳ abierto` *(decisión pendiente: acordar contrato formal de consulta con Módulo 3)* |
| **H7** | Residuos de la resolución A.1 en CU-11 | La reserva nace en `Iniciada` (creada por `Iniciar reserva` con TTL) y transiciona a `Pendiente de Pago` en `Iniciar pago`; fallas de cotización abortan sin persistir ninguna reserva. | `✅ aplicado` (`CU-11`) |
| **H8** | Relación de invocación formal de CU-11 en modo individual | CU-19 es el caso invocador formal (`<<include>>`); CU-02 recibe los montos como caso extendido. | `✅ aplicado` (`CU-11`) |
| **H9** | Interfaz CU-09/CU-01 (modo lote y parámetros) | Modo lote entre catálogo y Módulo 1, campos requeridos vs opcionales y consistencia de filtros. | `⏳ abierto` *(decisión pendiente: definir contrato de búsqueda en lote con Módulo 1)* |
| **H10** | Política de reintentos en CU-14 US2 | Reintentos alineados con FR-005: máximo 5 reintentos con backoff exponencial progresivo y derivación a DLQ ante agotamiento. | `✅ aplicado` (`CU-14`) |
| **H11** | Acotación de SLA de notificación al primer intento | Umbrales de SLA (< 500 ms, < 1 s) en criterios de éxito acotados a la emisión del primer intento de notificación. | `✅ aplicado` (`CU-04`, `CU-05`, `CU-06`, `CU-07`, `CU-08`) |
| **H12** | Confirmación de pago sobre reserva no pendiente | Generalización ante confirmaciones recibidas sobre reservas que ya no están en `Pendiente de Pago` (ej. `Expirada`): rechazo y solicitud de reversión en M3. | `✅ aplicado` (`CU-13`) |
| **H13** | Semántica del snapshot de "Total" con/sin depósito | Definir si el total persistido y expuesto en vistas incluye o excluye el depósito de garantía reembolsable. | `⏳ abierto` *(decisión pendiente: alinear definición contable y visual con Módulo 3)* |
| **H14** | Presentación literal de desglose financiero en CU-02 | Los valores de tarifa y noches se muestran literalmente desde Módulo 3; si la cotización en lote carece de subtotal, se muestra solo la tarifa base sin aritmética local. | `✅ aplicado` (`CU-02`) |
| **H15** | Reglas temporales de cancelación (frontera 72 h) y 3 SLAs de 24 h | Frontera exacta de franjas de cancelación y unificación semántica de las tres ventanas de 24 h del ciclo de vida. | `⏳ abierto` *(decisión pendiente: ratificar definiciones de política y SLAs con producto)* |
| **H16** | Precedencia y circularidad entre CU-16 y CU-17 | Orden de transiciones y causalidad entre la generación de disputa (`CU-16`) y su resolución/actualización (`CU-17`). | `⏳ abierto` *(decisión pendiente: formalizar el flujo de estados de disputa en el contrato con M3)* |
| **H17** | Estandarización de lectura hacia Módulo 1 en CU-04 y CU-05 | Reemplazo de la ruta de lectura externa por invocación a `Proveer información de embarcación` (`CU-09 <<include>>`), y CU-09 actualizado. | `✅ aplicado` (`CU-04`, `CU-05`, `CU-09`) |
| **H18** | Cobertura de estados inválidos en CU-05 | Inclusión explícita de `Iniciada` y `Pago Fallido` en las validaciones de incompatibilidad para marcar inasistencia. | `✅ aplicado` (`CU-05`) |
| **H19** | Corrección de referencia de no-aritmética en plan.md | Corrección de referencia bibliográfica interna en el plan general: SC-004 pasa a SC-003 de CU-02. | `✅ aplicado` (`plan.md`) |
| **H20** | Temporizador regresivo en UI vs vencimiento de TTL | Manejo de sincronización del reloj en frontend versus verificación estricta de expiración en backend. | `⏳ abierto` *(decisión pendiente: definir estrategia de temporizador y sincronización cliente/servidor)* |
| **H21** | Ventana máxima para check-in y no-show tras zarpe pactado | Definición del momento límite en que expira la potestad del propietario para marcar check-in o inasistencia. | `⏳ abierto` *(decisión pendiente: definir regla de corte post-zarpe con negocio)* |
| **H22** | Clarificación de caché e identificadores de Módulo 1 en plan.md | Módulo 2 no cachea datos técnicos de la flota; persiste únicamente identificadores como referencia foránea. | `✅ aplicado` (`plan.md`) |
| **H23** | Supuestos de cotización implícitos en catálogo (1 día / 1 pasajero) | Explicitar en CU-01 los parámetros por defecto asumidos por Módulo 3 en la cotización previa. | `⏳ abierto` *(decisión pendiente: coordinar con diseño y producto la visibilidad de los supuestos tarifarios)* |

---

## Notes

- La etiqueta `[CU-nn]` en los planes por CU mantendrá la trazabilidad hasta el spec de origen.
- Los planes por CU se redactarán **después** de la aprobación de este plan general, en archivos `features/CU-nn-*/plan.md`, sin modificar los `spec.md` existentes.
- `context/`, `features/`, `diagrams/` y `templates/` se tratan como **solo lectura**. **Excepción aplicada (2026-10-06)**: higiene documental sobre `features/` (salto de línea final, renumeración de `FR-007-bis` en CU-04 y marcado `[NEEDS CLARIFICATION]` en CU-11), autorizada explícitamente y sin cambios de contenido semántico. **Excepción adicional (2026-10-07)**: resolución de los vacíos de la sección B sobre `features/` (reescritura de marcadores `[NEEDS CLARIFICATION]`/`[PENDIENTE DE DEFINICIÓN]` y de los textos de resolución en CU-01, CU-02, CU-03, CU-05, CU-06, CU-07, CU-08, CU-09, CU-10, CU-12, CU-13, CU-14 y CU-21), autorizada explícitamente por negocio. **Excepción adicional (2026-10-07, secciones A y C)**: resolución de contradicciones sobre `features/` (CU-02, CU-03, CU-04, CU-05, CU-06, CU-11, CU-12, CU-14, CU-21) y sobre `context/consistencia-m2-m3.md` (§2), autorizada explícitamente por negocio. **Excepción adicional (2026-10-08)**: aplicación de la reauditoría 2026-10-08 sobre `features/` (CU-01, CU-02, CU-03, CU-04, CU-05, CU-06, CU-07, CU-08, CU-09, CU-11, CU-13, CU-14, CU-18) y sobre `context/consistencia-m2-m3.md` (§3, §4, §5) (correcciones mecánicas H5, H7, H8, H10-H12, H14, H17-H19, H22 y correcciones decididas H1, H2/H3, H4), autorizada explícitamente por negocio.
- Este plan describe **qué fases existen y en qué orden**; no prescribe tareas de implementación de ningún CU.
- Cada CU debe ser verificable de forma independiente; un plan específico que no pueda demostrarlo debe revisarse antes de implementarse.
- El caso base de CU-03 se resolvió el 2026-10-07: **`Ver detalle de reserva` (CU-21)**. La Fase 7 programa CU-03 después de las tres vistas de consulta.
- Detener la implementación ante cualquier `[NEEDS CLARIFICATION]` abierto del spec correspondiente: la guía SDD lo establece como regla de oro.
- Commit por tarea o por grupo lógico; detenerse en cada checkpoint de fase para validar.
