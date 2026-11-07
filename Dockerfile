# SEA-SHARE Modulo 2 - imagen multi-stage
# Etapa 1 (build): Maven + JDK 21. Etapa 2 (runtime): solo JRE 21, usuario no root.

# --- Etapa de build ---
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Capa de dependencias primero para cachear descargas
COPY pom.xml .
RUN mvn -B -q -DskipTests dependency:go-offline || true

COPY src ./src
RUN mvn -B -q -DskipTests package

# --- Etapa de runtime ---
FROM eclipse-temurin:21-jre
WORKDIR /app

# Usuario sin privilegios (no root)
RUN groupadd --system seashare \
    && useradd --system --gid seashare --home-dir /app --shell /usr/sbin/nologin seashare

# Sin secretos en la imagen: la configuracion llega por variables de entorno en runtime
COPY --from=build /workspace/target/*.jar app.jar
RUN chown seashare:seashare app.jar

USER seashare

# Puerto configurable en runtime via SERVER_PORT (Spring lo lee por relaxed binding)
ENV SERVER_PORT=8080
EXPOSE ${SERVER_PORT}

ENTRYPOINT ["java", "-jar", "app.jar"]
