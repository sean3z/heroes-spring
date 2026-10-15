# Spring Boot 3.5.16 Heroes on Java 25 (virtual threads on).
FROM maven:3-eclipse-temurin-25 AS build
WORKDIR /src
COPY pom.xml .
COPY src src
RUN mvn -q -DskipTests package && cp target/*.jar /heroes.jar

FROM eclipse-temurin:25-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
COPY --from=build /heroes.jar /app/heroes.jar
EXPOSE 8000
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/heroes.jar"]
