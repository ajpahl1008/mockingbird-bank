# Build stage - gradlew pins the exact Gradle version (see
# gradle/wrapper/gradle-wrapper.properties), so any JDK 21 image works
# here; no need for a Gradle-specific base image, and no risk of the
# build using a different Gradle version than local/CI does.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY . .
RUN ./gradlew --no-daemon -Pvaadin.productionMode=true clean bootJar

# Run stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
