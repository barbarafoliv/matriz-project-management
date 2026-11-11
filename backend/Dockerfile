# Matriz backend image (ADR 0007). Settings come from environment variables at run time
# (DB_URL, DB_USERNAME, DB_PASSWORD, see .env.example); no credentials are in the image.

# Build: compile and package the jar with the Maven wrapper. Tests run in CI, not here.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline
COPY src/ src/
RUN ./mvnw -B -q -DskipTests package && cp target/matriz-backend-*.jar app.jar

# Run: Java 21 JRE only, as a non-root user.
FROM eclipse-temurin:21-jre
RUN groupadd --system matriz && useradd --system --gid matriz --no-create-home matriz
WORKDIR /app
COPY --from=build /workspace/app.jar app.jar
USER matriz
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
