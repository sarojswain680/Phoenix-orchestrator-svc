# ---- Build stage ----
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Externalized, updatable version (was hardcoded)
ARG GRPC_VERSION=1.66.0

RUN echo "${GRPC_VERSION}" | grep -qE '^[0-9]+\.[0-9]+\.[0-9]+$' \
    || (echo "ERROR: Invalid GRPC_VERSION '${GRPC_VERSION}'" && exit 1)

# Build-only tooling (dev headers live here, NOT in runtime image)
RUN apk add --no-cache \
    bash \
    curl \
    unzip \
    libstdc++ \
    libgcc \
    protobuf \
    protobuf-dev

# Download protoc-gen-grpc-java with error handling + retries.
# (Maven Central serves over HTTPS. If you have a published SHA-256,
#  add a `sha256sum -c` verification step here for supply-chain safety.)
RUN set -eux; \
    url="https://repo1.maven.org/maven2/io/grpc/protoc-gen-grpc-java/${GRPC_VERSION}/protoc-gen-grpc-java-${GRPC_VERSION}-linux-x86_64.exe"; \
    curl -fsSL --retry 3 --retry-delay 2 "$url" -o /usr/local/bin/protoc-gen-grpc-java; \
    test -s /usr/local/bin/protoc-gen-grpc-java; \
     # echo "${GRPC_PLUGIN_SHA256}  /usr/local/bin/protoc-gen-grpc-java" | sha256sum -c -; \
    chmod +x /usr/local/bin/protoc-gen-grpc-java

COPY . .
# Ensure Gradle cache is properly initialized and grant execute permissions
RUN chmod +x ./gradlew && \
    ./gradlew clean bootJar -x test --no-daemon

# ---- Runtime stage (minimal attack surface — no protobuf-dev here) ----
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

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+UseG1GC", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-jar", "app.jar"]
