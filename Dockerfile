# Etapa 1: Compilar la aplicación usando Maven
FROM maven:3.8.8-eclipse-temurin-17 AS build
WORKDIR /app

# Copiar el archivo de configuración de dependencias y descargarlas en caché
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar el código fuente y compilar el archivo .jar omitiendo los tests
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Entorno de ejecución liviano
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copiamos el archivo .jar generado en la etapa de build
COPY --from=build /app/target/*.jar app.jar

# Exponemos el puerto de Spring Boot
EXPOSE 8082

# Comando para ejecutar la API
ENTRYPOINT ["java", "-jar", "app.jar"]