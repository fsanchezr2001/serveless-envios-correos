# Imagen única (un solo contenedor) que sirve el frontend Angular desde
# static/ de Spring Boot y expone la API del backend. Véase agente.md §10.

# 1) Build del frontend Angular: salida directamente en static/ del backend
FROM node:20 AS front
WORKDIR /f
COPY frontend/ .
COPY backend/ /backend
RUN npm ci && npm run build

# 2) Build del backend con el frontend estático ya incluido
FROM maven:3.9-eclipse-temurin-17 AS back
WORKDIR /b
COPY --from=front /backend .
RUN mvn -q package -DskipTests

# 3) Imagen final: solo JRE
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=back /b/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
