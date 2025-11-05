FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Build context must be the backend folder. Copy parent POM and the service module.
COPY ./pom.xml ./pom.xml
COPY ./identity-service ./identity-service

WORKDIR /app/identity-service
RUN mvn -B clean package -DskipTests \
 && echo "Selecting repackaged Spring Boot jar" \
 && J=$(ls target/*.jar | grep -v '\.original$' | head -n 1) \
 && cp "$J" target/app.jar \
 && echo "Copied $J to target/app.jar"

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
## Default DB connection for local runs via 'docker run'
## Override in docker-compose for in-cluster Postgres
ENV SPRING_DATASOURCE_URL="jdbc:postgresql://host.docker.internal:5432/eSantePg?sslmode=disable"
ENV SPRING_DATASOURCE_USERNAME="admin"
ENV SPRING_DATASOURCE_PASSWORD="admin"
## Copy only the repackaged Spring Boot jar (exclude .jar.original)
COPY --from=build /app/identity-service/target/app.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
