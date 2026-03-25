FROM maven:3.9.4-eclipse-temurin-17 AS build
COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY --from=build /target/*.jar app.jar

EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar", "--server.port=${PORT:8083}"]