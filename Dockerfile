# Use an official OpenJDK runtime as the base image
FROM openjdk:17-jdk-slim

# Set the working directory inside the container
WORKDIR /app

# Copy the packaged JAR file into the container
COPY target/iot_ws_simple-1.0.0-CLOSEBETA.jar /app/app.jar

# Expose the application port
EXPOSE 8060

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
