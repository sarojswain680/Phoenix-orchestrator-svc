# ---- Build stage ----
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
# Install required system dependencies for protoc and grpc plugins
RUN apk add --no-cache \
    bash \
    curl \
    unzip \
    libstdc++ \
    libgcc \
    protobuf \
    protobuf-dev

# Download and install protoc-gen-grpc-java plugin
RUN GRPC_VERSION="1.66.0" && \
    curl -L https://repo1.maven.org/maven2/io/grpc/protoc-gen-grpc-java/${GRPC_VERSION}/protoc-gen-grpc-java-${GRPC_VERSION}-linux-x86_64.exe \
    -o /usr/local/bin/protoc-gen-grpc-java && \
    chmod +x /usr/local/bin/protoc-gen-grpc-java


COPY . .
# Ensure Gradle cache is properly initialized and grant execute permissions
RUN chmod +x ./gradlew && \
    ./gradlew clean bootJar -x test --no-daemon

# ---- Runtime stage (minimal attack surface) ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Run as non-root user (security best practice)
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

# Health check to ensure container is running properly
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1
    
ENTRYPOINT ["java", "-jar", "app.jar"]