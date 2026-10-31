
# ===== BUILD STAGE =====
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src
COPY business-tests ./business-tests


# A imagem só é gerada se as regras de negócio passarem.
RUN mvn -B -f business-tests/pom.xml clean test

# A suíte unitária já foi executada acima; o teste de contexto exige infraestrutura.
RUN mvn -B clean package -Dmaven.test.skip=true


# ===== RUNTIME STAGE =====
FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# IMPORTANTE:
# A aplicação depende das variáveis de ambiente:
# - DB_URL
# - DB_USER
# - DB_PASSWORD
# - JWT_SECRET
#
# Elas devem ser passadas no docker run.

ENTRYPOINT ["java", "-jar", "app.jar"]