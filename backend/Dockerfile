# Dependencies first, so a source-only change does not refetch the world.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline
COPY src ./src
# Tests are not run here: CI runs them (including Testcontainers) before a merge,
# and the image build has no Docker daemon of its own.
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S aether && adduser -S -G aether aether
WORKDIR /app
COPY --from=build /build/target/backend-*.jar app.jar
USER aether
EXPOSE 8080
# Let the JVM size itself from the container's memory limit rather than the host's.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
