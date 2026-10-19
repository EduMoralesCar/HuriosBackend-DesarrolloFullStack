FROM maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /app

COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src
COPY .mvn ./.mvn
COPY mvnw .
COPY mvnw.cmd .

RUN mvn -DskipTests package

FROM eclipse-temurin:17-jre

WORKDIR /app

ENV PORT=8080
ENV SPRING_PROFILES_ACTIVE=prod

COPY --from=builder /app/target/*.jar /app/app.jar

RUN mkdir -p /app/uploads/products /app/uploads/backups

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
