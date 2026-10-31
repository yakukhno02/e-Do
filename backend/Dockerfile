# Stage 1: Build JAR using Gradle & OpenJDK 21
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy gradle wrapper and build files
COPY gradlew gradlew.bat ./
COPY gradle gradle
COPY build.gradle settings.gradle ./

# Grant execution rights on gradlew
RUN chmod +x ./gradlew

# Download dependencies (cache layer)
RUN ./gradlew dependencies --no-daemon || true

# Copy source code and build executable jar
COPY src src
RUN ./gradlew bootJar -x test --no-daemon

# Stage 2: Runtime image
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create non-root user for security
RUN addgroup -S edo && adduser -S edo -G edo
USER edo

# Copy built JAR from builder
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
