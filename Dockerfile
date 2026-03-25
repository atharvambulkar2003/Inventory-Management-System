# Build stage
FROM maven:3.9.4-eclipse-temurin-17 AS build
# Add this line!
WORKDIR /build
COPY . .
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
# Updated to point to the new build directory
COPY --from=build /build/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar", "--server.port=${PORT:8083}"]