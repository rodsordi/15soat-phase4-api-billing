# Stage 1: Build stage
FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /workspace

# Copy POMs and source
COPY pom.xml .
COPY domain/pom.xml domain/
COPY domain/src domain/src
COPY application/pom.xml application/
COPY application/src application/src

# Package application jar
RUN mvn clean package -DskipTests

# Stage 2: Runtime distroless/minimal JRE container
FROM eclipse-temurin:25-jre-noble AS runner

# Create dedicated non-root user and group
RUN groupadd -r appgroup && useradd -r -g appgroup -u 1000 appuser

WORKDIR /app

# Copy executable jar from builder
COPY --from=builder /workspace/application/target/billing-application-*.jar app.jar

# Enforce secure file ownership
RUN chown -R appuser:appgroup /app

USER appuser:appgroup

EXPOSE 8081

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseZGC -XX:+EnableDynamicAgentLoading"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
