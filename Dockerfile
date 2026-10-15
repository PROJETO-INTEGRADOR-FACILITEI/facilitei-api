# Build multi-stage. O contexto de build e a raiz do repositorio facilitei-api
# (e o que o Render usa ao clonar o repo).
FROM maven:3.9-amazoncorretto-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q clean package -DskipTests

FROM amazoncorretto:21-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# O Render injeta a porta via variavel PORT; a aplicacao le server.port=${PORT:8080}.
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
