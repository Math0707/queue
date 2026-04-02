# Build
FROM eclipse-temurin:21-jdk-noble AS builder
WORKDIR /app
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
COPY src ./src
RUN ./mvnw -B package -DskipTests

# Run
FROM eclipse-temurin:21-jre-noble
WORKDIR /app
RUN useradd --no-log-init --system --uid 1001 appuser
COPY --from=builder /app/target/*.jar application.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "application.jar"]
