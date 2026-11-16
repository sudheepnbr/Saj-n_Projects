FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
COPY src src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 idp
COPY --from=build /src/target/oih-idp-0.3.0-SNAPSHOT.jar /app/idp.jar
USER 10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/idp.jar"]
