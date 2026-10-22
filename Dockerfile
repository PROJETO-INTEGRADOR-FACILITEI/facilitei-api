# Build multi-stage. O contexto de build e a raiz do repositorio facilitei-api
# (e o que o Railway usa ao clonar o repo; ver railway.json).
FROM maven:3.9-amazoncorretto-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q clean package -DskipTests

FROM amazoncorretto:21-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# A plataforma injeta a porta via variavel PORT; a aplicacao le server.port=${PORT:8080}.
EXPOSE 8080
# Respeita o limite de memoria do container em vez de dimensionar pelo host.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
