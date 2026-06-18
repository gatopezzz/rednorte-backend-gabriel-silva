FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
# Copia el JAR generado en la carpeta target de este módulo
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]