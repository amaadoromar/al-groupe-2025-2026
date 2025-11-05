FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY ../pom.xml ./pom.xml
COPY ../monitoring-service ./monitoring-service

WORKDIR /app/monitoring-service
RUN mvn clean package spring-boot:repackage -DskipTests \
    && J=$(ls target/*.jar | grep -v '\\.original$' | head -n 1) \
    && cp "$J" target/app.jar

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/monitoring-service/target/app.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
