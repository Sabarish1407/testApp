# ==============================================================================
# Dockerfile for testApp (Customer / Order Pricing Service)
# Stack: Oracle OpenJDK 21, Spring Boot
# ==============================================================================

# Oracle JDK 21 base image as specified in neuro-Deploy specification
FROM container-registry.oracle.com/java/openjdk:21

WORKDIR /app

# Application JAR location - neuro-Deploy updates JAR file name or path here
COPY target/app.jar app.jar

# Default service port
EXPOSE 8080

# Run application executable
ENTRYPOINT ["java", "-jar", "app.jar"]
