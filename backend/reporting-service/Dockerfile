FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy the whole backend for parent POM resolution
COPY ../pom.xml ./pom.xml
#COPY ../monitoring-service ./monitoring-service
COPY ../reporting-service ./reporting-service

WORKDIR /app/reporting-service
RUN mvn clean package -DskipTests \
    && echo "Selecting repackaged Spring Boot jar" \
    && J=$(ls target/*.jar | grep -v '\\.original$' | head -n 1) \
    && cp "$J" target/app.jar \
    && echo "Copied $J to target/app.jar"

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/reporting-service/target/app.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
