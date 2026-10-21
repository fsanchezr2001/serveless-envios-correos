# Backend-only image: Spring Boot API only. No Angular bundle is embedded.
# The Angular SPA is built and served separately by Cloudflare Pages (see that
# project's build settings), so this image exposes nothing but the REST API.
#
# Build locally:
#   docker build -t envio-correo-api .

# 1) Build the Spring Boot JAR
FROM maven:3.9-eclipse-temurin-17 AS back
WORKDIR /b
COPY backend/pom.xml .
COPY backend/src ./src
RUN mvn -q package -DskipTests

# 2) Runtime image: lightweight JRE
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=back /b/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
