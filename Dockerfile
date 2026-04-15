FROM maven:3.9.14-eclipse-temurin-25 AS builder
WORKDIR /workspace
COPY pom.xml ./
COPY mvnw ./
COPY .mvn ./.mvn
COPY src ./src
RUN chmod +x mvnw && ./mvnw -DskipTests clean package

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=builder /workspace/target/agenda-telefonica-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
