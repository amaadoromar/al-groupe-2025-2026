FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy parent POM for multi-module resolution and the notification module
COPY ../pom.xml ./pom.xml
COPY ../notification-service ./notification-service

WORKDIR /app/notification-service
RUN mvn -Dmaven.test.skip=true clean package \
    && echo "Selecting repackaged Spring Boot jar" \
    && J=$(ls target/*.jar | grep -v '\\.original$' | head -n 1) \
    && cp "$J" target/app.jar \
    && echo "Copied $J to target/app.jar"

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/notification-service/target/app.jar app.jar
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar"]

