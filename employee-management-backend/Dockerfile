FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system manageflow \
    && useradd --system --gid manageflow --home-dir /app --no-create-home manageflow

WORKDIR /app

COPY --from=build /workspace/target/*.jar app.jar

# Attachment storage must be writable by the unprivileged runtime user
RUN mkdir -p /app/uploads/task-attachments \
    && chown -R manageflow:manageflow /app

USER manageflow

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=5s --start-period=60s --retries=5 \
    CMD curl -fsS http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
