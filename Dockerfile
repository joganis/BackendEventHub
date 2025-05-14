# Stage 1: Build with Maven
FROM maven:3.8.7-eclipse-temurin-17 AS build

# Directorio de trabajo
WORKDIR /app

# Copia sólo los ficheros de dependencias
COPY pom.xml .
# (Opcional: si tienes un settings.xml, cópialo aquí también)
RUN mvn dependency:go-offline

# Copia el código fuente
COPY src ./src

# Empaqueta el JAR (sin tests para acelerar)
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copia el JAR compilado desde el stage anterior
COPY --from=build /app/target/backend-eventHub-0.0.1-SNAPSHOT.jar app.jar

# Exponer (metadata, no es obligatorio para Docker)
EXPOSE 8070

# Arrancar usando la variable PORT que Render inyecta
ENTRYPOINT ["sh","-c","java -Dserver.port=${PORT} -jar app.jar"] ["sh","-c","java -Dserver.port=${PORT} -jar app.jar"]