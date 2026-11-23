FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Download dependencies first so they are cached between builds
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src

RUN mvn -B package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

# Run as a non-root user
RUN useradd --system --create-home appuser
USER appuser

COPY --from=build /app/target/*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 3000

CMD ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
