# Implementation Plan: Módulo 2 — Operación de Reservas, Tiempos y Cancelaciones (SEA-SHARE)

**Date**: 2026-09-27
**Spec**: [`context/sea-share.md`](context/sea-share.md) · [`context/consistencia-m2-m3.md`](context/consistencia-m2-m3.md) · [`features/CU-01..CU-21/spec.md`](features/)
**Alcance**: Plan **general** del módulo backend. Los planes técnicos detallados de cada caso de uso se redactarán en una tarea posterior, uno por CU.

## Summary

Este plan define la estrategia de construcción del **Módulo 2** de SEA-SHARE: un servicio backend en **Java 21 + Spring Boot 3.5**, empaquetado con **Maven**, que usa **RabbitMQ** como broker de mensajería para la integración asíncrona y la entrega garantizada de eventos hacia el Módulo 3 ("el sistema").

El módulo es dueño del **ciclo de vida de la reserva** (21 casos de uso: CU-01 a CU-18 operativos y CU-19 a CU-21 de vista de consulta) y se integra con dos sistemas externos mediante contratos de API: **Módulo 1** (Gestión de Flota y Activos P2P) como fuente de verdad de la flota y **Módulo 3** (Liquidación, Seguros y Dispersión de Fondos) como autoridad única en todo cálculo monetario.

Dos restricciones del contexto gobiernan toda la arquitectura:

1. **Regla de negocio estricta "Sin dinero"** (FR-010 de CU-08, FR-002 de CU-11 y CU-12, FR-012 de CU-04): Módulo 2 **jamás** calcula, suma, redondea, retiene ni convierte importes monetarios. Todo monto proviene de Módulo 3. Esta regla es la razón de ser de la separación entre los adaptadores de lectura financiera y el dominio de reservas.
2. **Módulo 1 es la única fuente de verdad de la flota**: Módulo 2 no accede a su base de datos, no cachea sus datos y no duplica atributos de la embarcación (`embarcacion_id`, `propietario_id`). La enunciación de "cero (0) accesos directos a la base de datos de Módulo 1" en CU-09 y CU-10 es un requisito arquitectónico, no una sugerencia.

La consecuencia de diseño es una **arquitectura hexagonal con un único escritor de estado**: CU-08 (`Actualizar estado de reserva`) es la única vía por la que cambia el estado de una reserva, lo que permite un control de concurrencia y una auditoría centralizados, y convierte a la reserva en el *aggregate root* del módulo.

## Technical Context

**Language/Version**: Java 21 (LTS)

**Primary Dependencies**: Spring Boot 3.5.x · Spring Web · Spring Validation · Spring Data JPA · Spring Security · Spring AMQP (`spring-boot-starter-amqp`, cliente RabbitMQ) · Spring Actuator · Flyway o Liquibase (ver *Storage*) · Resilience4j (timeouts, reintentos y circuit breaker de clientes externos) · springdoc-openapi (contratos de API)

**Storage**: **NEEDS CLARIFICATION** — el contexto (`sea-share.md`, `consistencia-m2-m3.md`) no menciona base de datos en ningún punto, pese a que CU-08 FR-004 exige persistencia **atómica** y FR-006 exige control de concurrencia. Requisito que condiciona la elección: soporte de `UUID`, columnas de timestamp con zona horaria, *optimistic locking* y **migraciones versionadas** (el esquema de `reserva` y `disputa_garantia` es estructura de datos propia, no un esquema heredado). La decisión debe cerrarse antes de arrancar la Fase 2.

**Testing**: JUnit 5 · Mockito · Spring Boot Test · Testcontainers (broker RabbitMQ y base de datos, para tests de integración reales) · WireMock (dobles de contrato de Módulo 1 y Módulo 3)

**Target Platform**: Linux server, JVM 21, despliegue en contenedor

**Project Type**: Backend REST + worker de mensajería. Servicio único que expone API HTTP y publica y consume mensajes RabbitMQ.

**Performance Goals** (consolidados de los `SC` de los specs; nótese la inconsistencia entre ellos):

| Objetivo | Fuente |
| :--- | :--- |
| Consultas a Módulo 1 (`Consultar información de embarcación`, estado operativo) < 300 ms | SC-001 de CU-09 y CU-10 *(marcado en ambos specs como `PENDIENTE DE DEFINICIÓN`, valor a confirmar por negocio)* |
| Consulta de información de reserva para Módulo 3 < 200 ms | SC-001 de CU-15 |
| Notificaciones a APIs externas tras consolidar el cambio < 500 ms | SC-002 de CU-08 y CU-14 |
| Notificaciones de Módulo 1 / Módulo 3 en < 1 s (cancelación, inasistencia, check-in, check-out, confirmación de pago) | SC-005 de CU-04, SC-004 de CU-05, SC-002 de CU-06 y CU-07, SC-001 de CU-13 |
| Lote e individual de cotización, y liquidación final | **NEEDS CLARIFICATION** — CU-11 y CU-12 dejan los SLA como placeholders ("p. ej. 800 ms", "p. ej. 1500 ms") |

**Constraints**:

- **Cero aritmética monetaria en el código de Módulo 2** (SC-004 de CU-02, SC-003 de CU-03, SC-007 de CU-04, SC-001 de CU-12, FR-010 de CU-08). Los importes se transportan como `BigDecimal` opacos devueltos por Módulo 3.
- **Cero sobreventa ante pagos concurrentes** (SC-002 de CU-03) y cero eventos perdidos silenciosamente (SC-006 de CU-04). Esto obliga a un patrón transaccional de tipo *outbox*: la transición de estado y el evento saliente deben confirmarse juntos.
- **Fail-safe ante fallo de dependencia externa**: el 100% de los fallos de conexión con Módulo 1 producen rechazo preventivo, no una decisión optimista (FR-008 de CU-09, FR-005 de CU-10). Módulo 1 y Módulo 3 nunca se consultan por vía alternativa, ni caché ni base de datos.
- **Cero montos ni cálculos de dinero en los mensajes** hacia Módulo 3: ni en `Recibir estado de reserva` (SC-003 de CU-14) ni en `Recibir información de disputa de garantía` (FR-003 y SC-002 de CU-18).
- **Idempotencia obligatoria en la entrada**: CU-13 FR-007 (clave idempotente por par `(id_transaccion, estado)`), CU-16 y CU-18 (identificador único por transición de disputa para deduplicación del lado de Módulo 3).
- **Evaluación de ventanas temporales siempre en la zona horaria del puerto de atraque** obtenida de Módulo 1, nunca en hora de servidor ni de dispositivo (FR-004 de CU-04 y CU-05; CU-09 US2). Si Módulo 1 no resuelve la zona horaria, la operación se detiene y se devuelve indisponibilidad de servicio: no existe zona horaria por defecto.
- **La integración con Módulo 1 es exclusivamente por API** (FR-007 de CU-10). Módulo 2 no implementa el inventario físico.
- **Atribución de términos** según `consistencia-m2-m3.md` §1: se usa **Arrendatario** y **Propietario** (nunca "turista" ni "anfitrión"), **"el sistema"** para Módulo 3 (nunca "Módulo 3" dentro del vocabulario de SPEC/Finanzas) y **"Sistema de Reservas y Operaciones"** para Módulo 2 en las interacciones con Finanzas.

**Scale/Scope**: 1 servicio · 21 casos de uso · 2 contextos acotados (Reservas, Disputa de Garantía) · 2 sistemas externos · 7 interacciones formalizadas (tabla §5 de `consistencia-m2-m3.md`) · Volumen de usuarios y concurrencia esperado **NEEDS CLARIFICATION** (el contexto no define cifras de escala ni requisitos de capacidad).

### Decisiones arquitectónicas transversales

**1. Hexagonal con un único escritor de estado.** El dominio de reservas no depende de Spring Web, JPA ni AMQP. Los casos de uso de UI (CU-01 a CU-07), el webhook de Módulo 3 (CU-13) y los temporizadores (TTL de 15 min, ventana de 24 h) convergen todos en CU-08, que es el *único* mutador del estado de la reserva (FR-001). Esto hace la máquina de estados verificable mediante tests unitarios exhaustivos, sin necesidad de contenedor.

**2. Outbox transaccional + RabbitMQ para toda notificación saliente.** El contexto exige "0% de eventos perdidos" (edge case de CU-08, SC-001 de CU-14, FR-005 de CU-18: "reintenta hasta confirmarla en el broker"). Un *fire-and-forget* no lo garantiza. Se usa un patrón *outbox*: la transición de estado y el registro del evento se escriben en la misma transacción local; un *relay* scheduler publica en RabbitMQ con *publisher confirms* y marca el evento como confirmado solo tras el *ack* del broker. Esto resuelve además un problema de consistencia que los specs no abordan: Módulo 1 no participa de la transacción local, por lo que una transición confirmada puede quedar sin su notificación a Módulo 1 y sin posibilidad de *rollback*. El outbox convierte esa divergencia en un evento reintentable en lugar de una pérdida silenciosa.

**3. Topología RabbitMQ.** Exchange de tipo `topic` para la integración con Módulo 3 (enrutamiento flexible por tipo de evento: `reserva.estado.cambio`, `disputa.estado.cambio`); colas con *dead letter*; *manual ack* en todos los consumidores; reintentos con *backoff* progresivo antes de derivar a la DLQ. Los parámetros concretos (número de reintentos, intervalos, retención, DLQ, *partitioning*) están explícitamente diferidos por CU-18 al contrato de integración, por lo que quedan como **NEEDS CLARIFICATION** (ver *Riesgos*).

**4. Reloj inyectable y zona horaria por puerto.** Todas las ventanas (15 min de TTL, 72 h y 24 h de cancelación, 30 min de inasistencia, 24 h de disputa) se evalúan contra un `Clock` inyectado y contra el `ZoneId` del puerto devuelto por Módulo 1. Esto hace deterministas los tests de *border* (exactamente 72 h, exactamente 24 h, exactamente 30 min), que los specs exigen explícitamente.

**5. Temporizadores durables, no en memoria.** El TTL de 15 min y el cierre automático de la disputa a las 24 h deben sobrevivir a un reinicio. Por eso se modelan como columnas de vencimiento consultadas por un job `@Scheduled` de barrido, y no como `ScheduledExecutorService`. La elección concreta depende de la base de datos, aún sin definir.

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
├── docker-compose.yml              # RabbitMQ + base de datos (placeholder)
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
- [ ] T002 Declarar en `pom.xml` las dependencias base: `spring-boot-starter-web`, `-validation`, `-data-jpa`, `-security`, `-amqp`, `-actuator`, `springdoc-openapi`, `resilience4j-spring-boot3` y el driver de base de datos (**pendiente de la decisión de _Storage_**)
- [ ] T003 Configurar el build: `maven-compiler-plugin` (release 21), `maven-surefire-plugin` (tests unitarios), `maven-failsafe-plugin` (tests de integración) y `spring-boot-maven-plugin`
- [ ] T004 Crear la clase principal `M2Application` con *component scanning* de `com.seashare.m2`
- [ ] T005 Crear el árbol de paquetes de _Project Structure_ (`shared/`, `reservas/`, `disputa/`) y los `package-info.java` que documentan la regla "Sin dinero"
- [ ] T006 Externalizar la configuración en `application.yml` y perfiles `dev` / `test` / `prod` (host, puerto y credenciales de RabbitMQ; *datasource*; timeouts), sin secretos en el repositorio
- [ ] T007 Definir `docker-compose.yml` con RabbitMQ (management habilitado) y el servicio de base de datos como **placeholder** a confirmar
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
- [ ] T016 **RabbitMQ — entrega garantizada**: *publisher confirms*, *manual ack* en consumidores, reintentos con *backoff* progresivo y derivación a DLQ. **NEEDS CLARIFICATION**: el número de reintentos, los intervalos y la retención no están definidos en los specs (ver _Riesgos_)
- [ ] T017 **Outbox transaccional**: tabla de salida, escritura conjunta con la transición de estado, *relay* scheduler con confirmación del broker y marcado del evento como entregado
- [ ] T018 **Reloj y zona horaria**: bean `Clock` inyectable y resolución de `ZoneId` a partir del puerto de atraque devuelto por Módulo 1, con *fail-safe* explícito (sin zona horaria por defecto)
- [ ] T019 **Clientes HTTP salientes**: cliente base para Módulo 1 y Módulo 3 con timeouts, reintentos solo ante fallos transitorios, *circuit breaker*, política *fail-safe* uniforme y propagación de *correlation id*
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

**Justificación**: son los tres casos base de las relaciones `<<extend>>` del conjunto — CU-02 (`Iniciar reserva`) se ancla a CU-19, CU-01 extiende hacia CU-19, CU-04 (`Solicitar cancelación`) se ancla a CU-21 y CU-03 declara su extensión sobre CU-20 (con la nota del spec que sugiere CU-21, pendiente de revisión formal — ver riesgo A). Los tres son de **solo lectura, sin máquina de estados ni efectos secundarios**: CU-19 consulta Módulo 1 (vía CU-09) y la cotización individual (vía CU-11); CU-20 y CU-21 leen reservas ya persistidas y exponen el detalle sin escrituras, en línea con las restricciones monetarias de CU-15 (exponen el total oficial, cero montos derivados). Se colocan como fase propia **antes del embudo** para que los casos base existan antes que sus extenders y para desbloquear la verificación de las extensiones que comparten los recorridos de las Fases 7 y 8. Dependen de la Fase 2 (persistencia de CU-20/CU-21) y de la Fase 3 (CU-19 requiere la cotización individual y los datos de Módulo 1).

> Las tareas técnicas detalladas de cada CU se definirán en su propio plan específico más adelante.

---

## Phase 7: Embudo de conversión — de la búsqueda al pago confirmado (P1)

**Qué CUs agrupa**: **CU-01** (Buscar embarcaciones disponibles), **CU-02** (Iniciar reserva), **CU-03** (Iniciar pago), **CU-13** (Confirmar pago)

**Justificación**: es la **vertical de mayor valor de negocio** del marketplace (sin ella el producto no genera ingresos) y forma un único recorrido de principio a fin: búsqueda con cotización por lote → creación de la reserva en `Iniciada` con el TTL en curso → adquisición del bloqueo de inventario y paso a `Pendiente de Pago` → confirmación del pago reportada por Módulo 3 y paso a `Reservada`. Se agrupan porque comparten la misma presión de concurrencia y el mismo indicador de negocio: dos Arrendatarios compiten por el mismo barco y las mismas fechas, y el spec exige cero sobreventa y respuesta atómica. Contiene la condición de no-bloqueo en `Iniciada` (varias reservas concurrentes para el mismo barco) y la carrera de sobreventa en `Pendiente de Pago`. Requiere las Fases 3 (contratos de cotización y liquidación), 4 (máquina de estados), 5 (notificación a Módulo 3) y 6 (vistas base de las extensiones CU-02 y CU-03) completas.

> Las tareas técnicas detalladas de cada CU se definirán en su propio plan específico más adelante.

---

## Phase 8: Operaciones en muelle — check-in, check-out, inasistencia y cancelación (P2)

**Qué CUs agrupa**: **CU-06** (Marcar inicio de navegación), **CU-07** (Marcar fin de navegación), **CU-05** (Marcar inasistencia), **CU-04** (Solicitar cancelación)

**Justificación**: los cuatro comparten **actor (Propietario), contexto físico (operación en el muelle) y punto de entrada** sobre una reserva que ya está `Reservada` o `En Navegación`. Dependen de la Fase 4, de la zona horaria del puerto entregada por CU-09 en la Fase 3 y de la Fase 6 (CU-21 es el caso base de `Solicitar cancelación`). Se agrupan y no se distribuyen porque comparten la misma familia de reglas de **ventana temporal y clasificación** (margen de salida, 30 minutos de tolerancia, 72 h y 24 h de cancelación) y porque las transiciones que producen convergen en las mismas ramifications de la máquina de estados. Dentro de la fase el orden sugerido es **check-in → check-out → cancelación → inasistencia**: los dos primeros son el camino feliz y los mejor especificados, mientras que los dos últimos concentran la mayoría de los `[NEEDS CLARIFICATION]` abiertos (margen de ventana no definido en CU-06, política de fallo de Módulo 1 en CU-05, clasificación del motivo del propietario en CU-04) y conviene abordarlos cuando la máquina de estados ya esté probada. El valor de negocio es completar el ciclo operativo del alquiler.

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

### A. Contradicciones que afectan el agrupamiento y el orden de las fases

1. **Modelo de creación de la reserva inconsistente (parcialmente resuelto).** CU-08, CU-02 y CU-03 ya coinciden en que la reserva nace en `Iniciada` y transiciona a `Pendiente de Pago`; pero quedan enunciados residuales en CU-11 (Key Entities: "creada en estado `Pendiente de Pago` por el caso de uso `Iniciar reserva`" y SC-006 "reservas creadas en estado `Pendiente de Pago`"), en CU-14 (FR-001 "`Pendiente de Pago` (estado inicial de toda reserva)" y SC-001 "a partir de la creación en `Pendiente de Pago`") y en CU-12 (SC-003 "reservas creadas en 'Pendiente de Pago'"). Afecta a las Fases 3, 4 y 7.

2. **CU-03 se ancla como extensión a `Ver mis reservas` (CU-20), pero el caso base no lo declara y el spec sugiere ahora `Ver detalle de reserva` (CU-21).** CU-03 mantiene en su cabecera y en FR-001 la extensión anclada a CU-20, que nunca declara esa relación (su FR-006 solo habilita extender hacia CU-21); además la actualización del 2026-10-06 añadió en CU-03 una nota: *"A nivel de flujo de plataforma se sugiere que este caso de uso extienda de CU-21, pendiente de revisión formal con el equipo"*. **Decisión abierta** — el equipo aún no resuelve cuál es el caso base (ver *Notes*). Afecta a la Fase 7.

3. ***(nuevo)* La carrera sobre `Iniciada` es contradictoria entre CU-02 y CU-08/CU-03.** CU-02 (Scenario 2, FR-014 y SC-002) exige resolver por First-Come First-Served **al crear** la reserva: solo la primera transacción entra a `Iniciada` y el segundo usuario recibe HTTP 409 Conflict. CU-08 (edge case "Sin bloqueo antes del pago") y CU-03 (US2, cuyo `Given` parte de dos reservas en `Iniciada` compitiendo) asumen que dos reservas en `Iniciada` coexisten para el mismo barco/fechas y que la carrera se resuelve al transicionar a `Pendiente de Pago`. Además, como Módulo 1 no bloquea el inventario hasta `Pendiente de Pago`, la validación atómica de CU-02 FR-010 contra Módulo 1 no podría producir ese 409. Afecta a las Fases 4 y 7.

4. ***(nuevo)* `Pago Fallido` no está propagado a los specs que enumeran estados.** El estado ya existe en CU-08 (FR-002, FR-003, FR-007) y CU-13 (FR-005), pero no aparece en CU-04 SC-001 (estados no cancelables), CU-05 FR-006 y CU-06 FR-004 (estados incompatibles que se rechazan), CU-14 US2 (estados terminales notificados) ni en `consistencia-m2-m3.md`. Afecta a las Fases 4, 5, 7 y 8.

5. ***(nuevo)* `consistencia-m2-m3.md` §2 contradice a CU-08 en el conjunto y denominación de estados.** El documento de consistencia afirma que al vencer el TTL "la reserva vuelve a **Disponible**" (CU-08 usa `Expirada`), nombra `Disponible`/`Reservado` en lugar de `Expirada`/`Reservada`, no reconoce `Expirada` ni `Pago Fallido`, y declara que "ambos documentos reconocen exactamente los mismos 9 estados" (CU-08 define 8 estados principales + 5 sub-estados de cancelación). Este plan depende de ese documento para la atribución de términos (Constraints). Afecta a las Fases 4, 5 y 7.

6. ***(nuevo)* Los estados desde los que se puede cancelar no están definidos de forma única.** `consistencia-m2-m3.md` §2 deja explícitamente "pendiente de definir" desde cuáles estados es válido cancelar (Iniciada, Pendiente, Reservado o En Navegación); CU-04 FR-002 permite cancelar **solo** desde `Reservada`; CU-08 no contempla `Iniciada → Cancelada`; y CU-04 SC-001 lista los no cancelables (`Pendiente de Pago`, `En Navegación`, `Completada`, `Expirada`, `Cancelada`) sin incluir `Iniciada` ni `Pago Fallido`. Afecta a las Fases 4, 7 y 8.

### B. Vacíos que bloquean la redacción de un plan de CU

1. CU-06 FR-003: el **margen previo** quedó fijado en 0, coherente con CU-21 FR-016/017/018 (el botón "Marcar inicio de la navegación" se habilita "a partir de la hora exacta de zarpe pactada (sin antelación)"). Persisten dos vacíos: el **límite posterior** al zarpe para ejecutar el check-in (CU-06 añadió un `[NEED CLARIFICATION]` nuevo al respecto) y los textos de CU-06 US2/US3, que siguen hablando de rechazo "dentro de la ventana de preparación previa", en tensión con el "sin antelación" de su propio FR-003.
2. CU-01 FR-002: la fuente de verdad quedó resuelta (el listado se obtiene obligatoriamente vía `<<include>>` a `Proveer información de embarcación` en modo lote; CU-01 no accede a Módulo 1 por otro canal). **CU-10 sigue sin ser referenciado por ningún spec**, aunque existe como CU.
3. CU-07: "Sin esta acción, el proceso se detiene y la reserva termina cancelándose automáticamente" — ninguna spec define esa cancelación automática, su disparador ni su sub-estado.
4. CU-14: US1 describe la notificación como "síncrona" y US2/FR-005 añaden cola de reintentos; no queda claro cuál aplica al camino feliz y cuál al de fallo. CU-18 acentúa la ambigüedad al declarar explícitamente que su publicación **no** es síncrona.
5. **Parámetros de reintento nunca cuantificados** en ningún spec (CU-08, CU-14, CU-18): número máximo de intentos, intervalos, *backoff* y política de DLQ. CU-18 difiere explícitamente la infraestructura de cola al contrato de integración.
6. **Seguridad subespecificada en todo el conjunto de specs**: solo hay chequeos de rol; no existe mecanismo de autenticación, modelo de tokens, TLS, manejo de PII, _rate limiting_ ni protección del log de auditoría. Bloqueante para la Fase 2.
7. **Idempotencia inconsistente**: estricta en la entrada (CU-13), delegada a Módulo 3 en la salida (CU-14 y CU-18) y **ausente** en la cola de reintentos de CU-08, que es justamente la fuente de eventos duplicables.
8. **SLAs contradictorios o sin cuantificar**: 200 ms (CU-15), 300 ms (CU-09 y CU-10 — ambos specs marcan su SC-001 como `PENDIENTE DE DEFINICIÓN`, valor a confirmar por negocio), 500 ms (CU-08 y CU-14), "menos de 1 segundo" (CU-04, CU-05, CU-06, CU-07 y CU-13), "límites de UX aceptables" sin cifra (CU-01 SC-002, ahora con `[NEEDS CLARIFICATION]` explícito) y los placeholders abiertos de CU-11 y CU-12 intactos.
9. **Volumen y capacidad nunca definidos**: número de usuarios, número de reservas y volumen de reservas concurrentes.
10. **Divisa**: CU-11 y CU-12 transportan `currency` (COP, USD) y prohíben la conversión local, pero ninguna spec fija en qué moneda se almacena la reserva ni cómo se reconcilian cotizaciones en monedas distintas. La actualización eliminó el ejemplo en USD de CU-20 FR-010 (ya no muestra montos, solo el estado del depósito) y CU-21 FR-010 exige el total "en COP"; CU-12 (edge case "Moneda del cobro") sigue admitiendo COP o USD según lo devuelto por Módulo 3.
11. CU-05: dos `[NEEDS CLARIFICATION]` sobre la política ante fallo de Módulo 1 al resolver la zona horaria (¿reintentar o rechazar temporalmente la inasistencia?).
12. CU-13: sigue abierta la política ante pago rechazado/fallido (¿reintento dentro del TTL remanente o fallo inmediato?) — `[NEEDS CLARIFICATION]` en FR-005 y en US2 Scenario 1; la antigua duplicación de la misma pregunta quedó eliminada.
13. CU-11: siguen abiertos el límite de lote de Módulo 3 (50 o 100, repetido en varios FR) y su SLA (placeholder). CU-12: siguen abiertos el nombre formal del endpoint (dos candidatos) y su SLA. Nota: CU-01 FR-008 y CU-11 FR-003 fijaron el tamaño de página de la vista en 20 embarcaciones, que no coincide con el límite por solicitud que impone Módulo 3.
14. CU-16, CU-17 y CU-18: cerrada la duda de la ventana de 24 h (es **fija**; se añadió como SLA en CU-16 FR-005, CU-17 FR-013 y CU-18 FR-007). Siguen abiertos: reclamo múltiple o editable dentro de la ventana (CU-16 D-01), política de reintento del job diferido (CU-16 D-03, ahora con `[NEEDS CLARIFICATION]` en el spec), si el Admin puede resolver una disputa `PENDIENTE` sin reclamo registrado (CU-17 D-01), longitud máxima del motivo (CU-17), y garantías de orden y versionado de `eventId` + detalles de la cola (CU-18 D-01/D-02, diferidos al contrato de integración).
15. ***(nuevo)* Estrategia de concurrencia a confirmar**: CU-08 FR-006 quedó marcado como `[PENDIENTE DE DEFINICIÓN]` ("Estrategia de concurrencia y uso de `@Version`/Optimistic Locking a confirmar por negocio"), pese a que FR-004 exige atomicidad y FR-006 control de concurrencia. Condiciona las decisiones T013/T014.
16. ***(nuevo)* Política de reintentos hacia Módulo 1 pendiente**: CU-09 FR-008 y CU-10 FR-006 añadieron `[PENDIENTE DE DEFINICIÓN]` sobre reintentos ante fallos/timeout, en tensión con el fail-safe inmediato (rechazo preventivo) que esos mismos specs y este plan exigen (Constraints).
17. ***(nuevo)* Estrategia de idempotencia del webhook de pago pendiente**: CU-13 FR-007 añadió `[PENDIENTE DE DEFINICIÓN]` ("Estrategia de idempotencia de pago en la validación del webhook"), complemento del ítem B.7.

### D. Decisiones de infraestructura no cubiertas por los specs

1. **Base de datos nunca mencionada** en el contexto, pese a ser requisito de atomicidad (CU-08 FR-004), control de concurrencia (CU-08 FR-006) y "cero sobreventa" (CU-02 SC-002, CU-03 SC-002). La contradicción sobre su existencia se cerró: CU-02, CU-08, CU-12 y CU-14 asumen de forma consistente que la reserva se **persiste en base de datos** al crearse/transicionar, y CU-09 solo niega que Módulo 2 copie los detalles técnicos de los barcos (no que Módulo 2 carezca de base de datos). `Technical Context` mantiene Storage como `NEEDS CLARIFICATION`. Decisión bloqueante para la Fase 2.
2. ***(nuevo)* Tensión reintentos vs fail-safe**: el plan asume en T019 y en Constraints reintentos solo ante fallos transitorios para los clientes HTTP, mientras CU-09 FR-008 y CU-10 FR-006 dejan esa política como `[PENDIENTE DE DEFINICIÓN]`. Debe definirse antes de la Fase 2.

### E. Higiene documental (no bloquea, conviene corregir en los planes por CU)

1. CU-11 invoca un "Contrato UC01 de Módulo 3" que nunca se cita ni se adjunta, y su fórmula contractual `(tarifa base × duración) + (tarifa de seguro × pasajeros)` no aparece en ningún documento de contexto. *(Reescrito: la parte que señalaba los escenarios de aceptación "intactos" de CU-08 US3/US4 se retira porque esos specs ya listan sus escenarios, y la parte que señalaba una sección de dudas inexistente en CU-01 se retira porque la referencia colgante estaba en CU-02 y se eliminó.)*
2. **Duplicados y estructura corregidos, persiste falta de salto de línea final**: CU-01 y CU-19 ya no contienen el bloque `### Functional Requirements` duplicado (la actualización lo consolidó y CU-19 perdió el encabezado vacío); pero 15 de los 21 specs siguen cerrando sin salto de línea final (CU-02 ya corregido; pendientes CU-01, CU-03, CU-04, CU-05, CU-06, CU-07, CU-08, CU-09, CU-10, CU-11, CU-14, CU-15, CU-19, CU-20 y CU-21).
3. **Erratas y numeración**: "**ohne**" (CU-04, US2 Independent Test) y "propieatario" (CU-06 FR-013) corregidas, y el doble guion de CU-03 eliminado; la numeración de requisitos quedó contigua en todos los specs (CU-03 ya no tiene los huecos FR-004 a FR-006) — pero CU-04 introdujo un **`FR-007-bis`** que rompe la secuencia de su propio FR-007.

---

## Notes

- La etiqueta `[CU-nn]` en los planes por CU mantendrá la trazabilidad hasta el spec de origen.
- Los planes por CU se redactarán **después** de la aprobación de este plan general, en archivos `features/CU-nn-*/plan.md`, sin modificar los `spec.md` existentes.
- `context/`, `features/`, `diagrams/` y `templates/` se tratan como **solo lectura**.
- Este plan describe **qué fases existen y en qué orden**; no prescribe tareas de implementación de ningún CU.
- Cada CU debe ser verificable de forma independiente; un plan específico que no pueda demostrarlo debe revisarse antes de implementarse.
- La decisión del caso base de CU-03 (CU-20 vs CU-21, ítem A.2) queda **abierta** a revisión formal del equipo; el plan no la presupone: la Fase 7 programa CU-03 después de las tres vistas de consulta.
- Detener la implementación ante cualquier `[NEEDS CLARIFICATION]` abierto del spec correspondiente: la guía SDD lo establece como regla de oro.
- Commit por tarea o por grupo lógico; detenerse en cada checkpoint de fase para validar.
