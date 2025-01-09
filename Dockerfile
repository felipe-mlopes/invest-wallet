# Use Alpine as base image
FROM eclipse-temurin:17-jre-alpine

# Install AWS CLI and required dependencies
RUN apk add --no-cache \
    aws-cli \
    python3 \
    jq \
    bash

# Set working directory
WORKDIR /app

# Copy application jar
COPY target/*.jar app.jar

# Copy startup script
COPY start.sh start.sh
RUN chmod +x start.sh

# Expose application port
EXPOSE 8080

# Use the startup script as entrypoint
ENTRYPOINT ["/app/start.sh"]