# Build stage - uses a Gradle-provided image since this project doesn't
# check in the gradle wrapper; swap for your own base image if you have
# an internal one with Gradle + JDK 21 preinstalled.
FROM gradle:8.10-jdk21 AS build
WORKDIR /workspace
COPY . .
RUN gradle --no-daemon -Pvaadin.productionMode=true clean bootJar

# Run stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
