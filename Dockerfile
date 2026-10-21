# syntax=docker/dockerfile:1

# Build stage: compiles the Spring Boot fat jar with the Gradle wrapper.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
COPY src ./src

RUN --mount=type=cache,target=/root/.gradle \
    chmod +x gradlew && ./gradlew --no-daemon bootJar

# Runtime stage: JRE only, runs as a non-root user.
FROM eclipse-temurin:21-jre AS runtime
RUN groupadd --system app && useradd --system --gid app app
WORKDIR /app

COPY --from=build /workspace/build/libs/*-SNAPSHOT.jar app.jar

USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
