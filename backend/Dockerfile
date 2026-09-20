FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -DskipTests dependency:go-offline

COPY src/ src/
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --system --uid 10001 --create-home mylog
COPY --from=build /workspace/target/*.jar /app/app.jar

USER mylog
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
