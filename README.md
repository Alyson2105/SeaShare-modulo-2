# SEA-SHARE · Módulo 2 — Operación de Reservas, Tiempos y Cancelaciones

Backend en **Java 21 + Spring Boot 4.1.2 + Maven + PostgreSQL**, arquitectura hexagonal,
paquete `com.seashare.m2`. Este módulo es dueño del ciclo de vida de la reserva.

---

## 1. Requisitos

| Requisito | Versión |
|---|---|
| JDK | 21 |
| Maven | 3.10.0 (usa el wrapper: `mvnw.cmd` / `./mvnw`) |
| Docker + Docker Compose | cualquiera actual (probado con Docker Desktop 29.x) |
| PostgreSQL | 16 (se levanta por contenedor) |
| RabbitMQ | 3 (se levanta por contenedor) |

## 2. Infraestructura local (PostgreSQL y RabbitMQ)

El repositorio **ya incluye** `docker-compose.yml` con ambos servicios:

```bash
docker compose up -d      # levanta PostgreSQL y RabbitMQ
docker compose ps         # verifica
docker compose down       # apaga conservando datos
docker compose down -v    # apaga BORRANDO datos (pide confirmación antes de usarlo)
```

Servicios:

| Servicio | Puerto en tu equipo | Credenciales |
|---|---|---|
| PostgreSQL (`seashare_m2`) | `localhost:5433` | `seashare` / `seashare` |
| RabbitMQ AMQP | `localhost:5672` | `seashare` / `seashare` |
| RabbitMQ panel web | http://localhost:15672 | `seashare` / `seashare` |

Flyway gestiona el esquema (`V1`, `V2`, `V3`) y `spring.jpa.hibernate.ddl-auto: validate`
verifica que las entidades coincidan con la BD. **Nunca se editan migraciones ya aplicadas.**

## 3. Correr la aplicación

```bash
# Con el perfil dev (local):
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.profiles.active=dev"

# Con el perfil de test (verificación manual):
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.profiles.active=test"

# Unix:
./mvnw spring-boot:run "-Dspring-boot.run.arguments=--spring.profiles.active=dev"
```

La app queda en `http://localhost:8080` (cambiable con la variable de entorno `SERVER_PORT`).
Actuator expone `/actuator/health` e `/actuator/info`.

Endpoints disponibles hoy (reservas):

| Método | Ruta |
|---|---|
| `POST` | `/api/v1/reservations` |
| `GET` | `/api/v1/reservations` |
| `GET` | `/api/v1/reservations/{id}` |

### Identidad temporal (sin Spring Security)

La identidad **es temporal** y llega por headers HTTP:

- `X-User-Id`: UUID del usuario.
- `X-User-Role`: `RENTER` u `OWNER` (`POST` solo acepta `RENTER`).

```bash
curl -X POST http://localhost:8080/api/v1/reservations \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 3f1c8f5e-0000-4000-8000-000000000001" \
  -H "X-User-Role: RENTER" \
  -H "Idempotency-Key: 3f1c8f5e-0000-4000-8000-000000000002" \
  -d '{ ... }'
```

La autenticación real queda para una fase posterior (decisión D-13).

### Módulos 1 y 3 simulados

- **Módulo 1 (flota)**: simulado con *fakes* por defecto (`seashare.fleet.mode=fake`,
  `matchIfMissing=true` → `FakeFleetInfoAdapter` y `FakeFleetOperationalStatusAdapter`).
  El cliente HTTP real no existe todavía.
- **Módulo 3 (finanzas)**: **no hay integración real ni fake**. Las cotizaciones y
  confirmaciones de pago de M3 aún no existen; los eventos hacia M3 solo se encolan en la
  tabla `outbox_message` (no hay relay ni publicación a RabbitMQ).

## 4. Tests

```bash
docker                 # solo hace falta Docker en ejecución (NO docker-compose)
.\mvnw.cmd test        # suite completa (en Windows)
```

- **Los tests no necesitan `docker-compose`**: usan Testcontainers, que levanta un
  PostgreSQL 16 en contenedor (uno solo, compartido por toda la corrida) y lo retira al
  terminar. Requisito único: Docker corriendo.
- `docker-compose.yml` sigue siendo necesario para **correr la app en desarrollo**
  (su PostgreSQL en `localhost:5433` y RabbitMQ), no para los tests.
- No hay H2: todo `@SpringBootTest` necesita PostgreSQL real; lo aporta Testcontainers
  vía `@Import(PostgresTestContainers.class)` (`src/test/java/com/seashare/m2/shared/`).
- Tests unitarios puros no abren contexto de Spring ni levantan contenedor.
- Para una clase o método: `.\mvnw.cmd test "-Dtest=CreateReservationTest"`.

## 5. Perfiles de configuración

Extensión `.yaml` (decisión D-11). Archivos en `src/main/resources/`:

| Perfile | Archivo | Uso | Qué cambia |
|---|---|---|---|
| (base) | `application.yaml` | siempre se carga | datasource local, `ddl-auto: validate`, Flyway, AMQP local, `m2.scheduler.*` |
| `dev` | `application-dev.yaml` | desarrollo local | logging `DEBUG` en `com.seashare` |
| `test` | `application-test.yaml` | arranque manual de verificación | logging `WARN`, job TTL desactivado |
| `prod` | `application-prod.yaml` | despliegue | **solo variables de entorno**, logging `INFO` |

Notas:

- `ddl-auto: validate` se mantiene en **todos** los perfiles (heredado de `application.yaml`;
  ningún perfil lo sobreescribe).
- En `prod` no hay secretos en el repositorio. Variables requeridas:
  `SEASHARE_DB_URL`, `SEASHARE_DB_USERNAME`, `SEASHARE_DB_PASSWORD`,
  `SEASHARE_RABBIT_HOST`, `SEASHARE_RABBIT_USERNAME`, `SEASHARE_RABBIT_PASSWORD`
  (opcional `SEASHARE_RABBIT_PORT`, por defecto `5672`).
- La suite de tests **no** activa ningún perfil (ningún test usa `@ActiveProfiles`), así que
  `application-test.yaml` no altera el comportamiento de `.\mvnw.cmd test`.

## 6. Docker (imagen de la app)

```bash
docker build -t seashare-m2 .
docker run --rm -p 8080:8080 \
  -e SERVER_PORT=8080 \
  -e SEASHARE_DB_URL=jdbc:postgresql://host.docker.internal:5433/seashare_m2 \
  -e SEASHARE_DB_USERNAME=seashare \
  -e SEASHARE_DB_PASSWORD=seashare \
  seashare-m2
```

Imagen multi-stage: build con Maven + JDK 21, runtime con JRE 21 y usuario no root.
Sin secretos incrustados; el puerto es configurable con `SERVER_PORT`.

## 7. Convenciones

- Identificadores de código y API en inglés; comentarios y documentación en español.
- JSON en `snake_case`; errores con cuerpo `{ "code", "message" }` desde `ErrorCode`.
- Regla de dinero: nunca calcular ni redondear importes; Módulo 3 es la autoridad única.
- Regla de git: commits en español (`feat:`, `fix:`, `chore:`, `docs:`) con rutas explícitas;
  nunca `git add .` ni `git stash pop`.
