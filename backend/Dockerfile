# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -ntp clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 mylog
COPY --from=build /workspace/target/mylog-backend-*.jar app.jar
USER mylog
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
