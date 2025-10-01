# Multi-stage build for Research Agent Server
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom.xml and download dependencies (layer caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -Dmaven.test.skip=true

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy JAR from build stage
COPY --from=build /app/target/research-agent-server-*.jar app.jar

# Expose Spring Boot port
EXPOSE 8080

# Set environment variables with defaults
ENV OLLAMA_BASE_URL=http://ollama:11434
ENV MCP_WEB_URL=http://mcp-web:9101

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]