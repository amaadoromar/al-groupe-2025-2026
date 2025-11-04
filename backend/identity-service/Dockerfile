FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy parent POM and modules for reactor resolution
COPY ../pom.xml ./pom.xml
COPY ../identity-service ./identity-service
COPY ../monitoring-service ./monitoring-service
COPY ../reporting-service ./reporting-service

WORKDIR /app/identity-service
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/identity-service/target/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]

