# syntax=docker/dockerfile:1

# ---- Stage 1: build the jar with the Maven Wrapper (same Maven version as locally) ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Download dependencies first, so this layer is cached until pom.xml changes
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src/ src/
# Tests run with ./mvnw test outside Docker; skipping them here keeps image builds fast
RUN ./mvnw -B -q package -DskipTests && cp target/animal-pictures-*.jar /workspace/app.jar

# ---- Stage 2: small runtime image with only the JRE and the jar ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# Run as a non-root user; /app/data holds the H2 database file (mounted as a volume)
RUN groupadd --system app && useradd --system --gid app app \
    && mkdir -p /app/data && chown -R app:app /app
USER app

COPY --from=build /workspace/app.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
