# 1. Imagen base que incluye Java 17 JRE
FROM eclipse-temurin:17-jre

# 2. Directorio de trabajo dentro del contenedor
WORKDIR /app

# 3. Copia el JAR empacado al contenedor
COPY target/backend-eventHub-0.0.1-SNAPSHOT.jar app.jar

# 4. Expón el puerto 8070
EXPOSE 8070

# 5. Comando que se ejecuta al arrancar el contenedor
ENTRYPOINT ["java","-jar","app.jar"]
