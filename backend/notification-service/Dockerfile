FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy parent POM for multi-module resolution and the notification module
COPY ../pom.xml ./pom.xml
COPY ../notification-service ./notification-service

WORKDIR /app/notification-service
RUN mvn -q -e -DskipTests clean package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/notification-service/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

