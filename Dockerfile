# =============================================================================
# CarePulse Portal - Unified Production Multi-Stage Dockerfile
# =============================================================================

# Stage 1: Build React Frontend
FROM node:20-alpine AS frontend-builder
WORKDIR /app/frontend

COPY frontend/package.json frontend/package-lock.json* ./
RUN npm ci || npm install

COPY frontend/ ./
RUN npm run build

# Stage 2: Build Spring Boot Backend with embedded Frontend static assets
FROM maven:3.9.6-eclipse-temurin-17-alpine AS backend-builder
WORKDIR /app/backend

COPY backend/pom.xml .
RUN mvn dependency:go-offline -B

COPY backend/src ./src

# Embed compiled frontend distribution into Spring Boot's static resources
COPY --from=frontend-builder /app/frontend/dist ./src/main/resources/static/

RUN mvn clean package -DskipTests -B

# Stage 3: Lightweight Production JRE Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Non-root security user
RUN addgroup -S carepulse && adduser -S carepulse -G carepulse

COPY --from=backend-builder /app/backend/target/carepulse-backend-*.jar app.jar
RUN chown -R carepulse:carepulse /app

USER carepulse

EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
