FROM maven:3.9.14-eclipse-temurin-25 AS builder

WORKDIR /workspace

COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn
RUN chmod +x mvnw

# cache de dependências (melhora MUITO build no CI)
RUN ./mvnw -B -q -e -DskipTests dependency:go-offline

COPY src src

RUN ./mvnw -B -DskipTests clean package

# ----------------------------

FROM eclipse-temurin:25-jre

WORKDIR /app

COPY --from=builder /workspace/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]