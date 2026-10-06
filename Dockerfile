# ============================================================
# RutaU API - imagen Docker (construcción en 2 etapas)
# ============================================================

# Etapa 1: compilar el proyecto con Maven y Java 21
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Primero solo el pom.xml: así Docker reutiliza las dependencias descargadas si el pom no cambia
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

# Etapa 2: imagen liviana que solo ejecuta el .jar
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
