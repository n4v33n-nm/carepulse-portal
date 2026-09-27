# =============================================================================
# CarePulse Portal Backend - Production Multi-Stage Dockerfile
# =============================================================================

# Build stage
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
WORKDIR /app

# Cache dependency layer
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Build application bundle
COPY src ./src
RUN mvn clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Non-root security user
RUN addgroup -S carepulse && adduser -S carepulse -G carepulse

COPY --from=builder /app/target/carepulse-backend-*.jar app.jar
RUN chown -R carepulse:carepulse /app

USER carepulse

EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
