
# Stage 1: Build Stage (Maven + JDK 21 on Alpine Linux)

FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

#first copy pom so layer cache proper use
COPY pom.xml .
RUN mvn dependency:go-offline -B

#copy source code and package the executable Spring Boot fat JAR.
COPY src ./src
RUN mvn clean package -DskipTests -B


# Stage 2: Production Runtime Stage (Minimal JRE 21 on Alpine Linux)

FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

#non-root user, least privilege
RUN addgroup -S unispot && adduser -S unispot -G unispot

#get only the compiled JAR from the builder stage and set ownership
COPY --from=builder --chown=unispot:unispot /build/target/*.jar app.jar

# Switch from root to non-root user
USER unispot

# will use port in docker cmd, this just show
EXPOSE 8080  

# Exec form ensures 'java' runs as PID 1 and receives SIGTERM directly for graceful shutdown.
# -XX:MaxRAMPercentage=75.0 leaves 25% container RAM headroom for JVM Metaspace,
# Direct ByteBuffers, and Virtual Thread carrier/native stacks.
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]