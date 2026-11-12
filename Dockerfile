# Stage 1: Runtime minimal JRE container
FROM eclipse-temurin:25-jre-noble AS runner

# Create dedicated non-root user and group
RUN groupadd -r -g 10001 appgroup && useradd -r -u 10001 -g appgroup appuser

WORKDIR /app

# Support pre-built JAR from CI runner or builder fallback
ARG JAR_FILE=application/target/billing-application-*.jar
COPY ${JAR_FILE} app.jar

# Enforce secure file ownership
RUN chown -R appuser:appgroup /app

USER appuser:appgroup

EXPOSE 8081

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseZGC -XX:+EnableDynamicAgentLoading"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
