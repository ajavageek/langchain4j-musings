FROM eclipse-temurin:21-jdk-jammy AS build

COPY .mvn .mvn
COPY mvnw .
COPY pom.xml .
COPY src src

RUN --mount=type=cache,target=/root/.m2,rw ./mvnw package -DskipTests

FROM eclipse-temurin:21-jre-jammy

COPY --from=build target/langchain4j-musings-0.1-SNAPSHOT.jar langchain4j-musings.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "langchain4j-musings.jar"]
