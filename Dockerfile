FROM maven:3.8.6-jdk-11 AS build
WORKDIR /app
COPY . /app
RUN mvn -q package -DskipTests
FROM eclipse-temurin:11-jre
WORKDIR /app
COPY --from=build /app/target/*.jar /app/app.jar
EXPOSE 8080
CMD ["java","-cp","/app/app.jar","org.example.App"]
