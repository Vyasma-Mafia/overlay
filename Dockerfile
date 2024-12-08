# STAGE 1: Build the Kotlin app with Gradle
FROM gradle:jdk21 AS build

# Set the working directory in the container
WORKDIR /app

# Copy only build configuration files (dependencies are resolved first to benefit from Docker layer caching)
COPY build.gradle settings.gradle.kts gradlew /app/
COPY gradle /app/gradle

# Grant execute permission to gradlew
RUN chmod +x gradlew

# Download dependencies (this step is cached unless the dependencies change)
RUN ./gradlew --no-daemon build || return 0

# Copy the rest of the source code
COPY src /app/src

# Final build of the project
RUN ./gradlew --no-daemon build

# STAGE 2: Create a minimal image to run the Kotlin app
FROM openjdk:21-jdk-slim

# Set the working directory for the new container
WORKDIR /app

# Copy the JAR file from the build stage into the final container
COPY --from=build /app/build/libs/*.jar /app/app.jar

# Expose the port that your app will run on (optional, change 8080 if needed)
EXPOSE 8080

# Command to run the application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
