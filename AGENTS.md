# AGENTS.md

## Quick facts
- **Artifact**: `com.seashare:m2` (Spring Boot 4.1.2, Java 21)
- **Entry**: `com.seashare.m2.M2Application`
- **Architecture**: Hexagonal/ports-and-adapters under `src/main/java/com/seashare/m2/reservas/` (domain, application/ports, infrastructure adapters in/out). 
- **DB**: PostgreSQL via JPA; schema managed by Flyway (`src/main/resources/db/migration/V*.sql`). `spring.jpa.hibernate.ddl-auto: validate`.
- **Messaging**: RabbitMQ (AMQP) configured in `application.yaml`.
- **Local deps**: `docker-compose.yml` starts `postgres` on `localhost:5433` (db `seashare_m2`, user/pass `seashare`) and `rabbitmq` on `5672`/`15672`.

## Build/test/run
- **Wrapper**: use `./mvnw` (or `mvnw.cmd` on Windows). 
- **Build/package**: `./mvnw clean package`
- **Run app**: `./mvnw spring-boot:run` (needs Postgres/RabbitMQ up)
- **Tests**: `./mvnw test` (uses Spring Boot test starters; tests under `src/test/`)
- **Local infra**: `docker compose up -d` from repo root (see `docker-compose.yml` comments)

## Config gotchas
- **DB port**: app connects to `jdbc:postgresql://localhost:5433/seashare_m2` in `src/main/resources/application.yaml` (not 5432). Must match `docker-compose.yml`.
- **Flyway**: enabled, locations `classpath:db/migration`. Migrations present: `V1__init.sql`, `V2__remaining_tables.sql`, `V3__reservation_fields_and_outbox.sql`. Do not rely on Hibernate to create schema.
- **JPA**: `spring.jpa.open-in-view: false`. 
- **Actuator**: only `health,info` exposed.

## Structure notes
- **Main package**: `com.seashare.m2.reservas.*` contains ports/in (`web`, `...`), application (`...`), ports/out, and adapters/out (`persistence`, `clock`, `fleet`, `messaging` etc.). Look there for real flow/entrypoints rather than guessing.
- **Outbox pattern**: persistence includes `OutboxMessage*`, `OutboxAdapter` (events toward M3 per context docs). 
- **Domain context**: see `Documentación/context/sea-share.md` for business rules (TTL 15min, no-show 30min, cancellation/refund rules). Treat as product context; trust code/config for implementation details.
