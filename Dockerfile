FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline -B
COPY src src
# The build runs the suite, and DevSeedWritesOnlyValuesTheDomainAcceptsTest reads
# the dev seed to check it against the domain. Build stage only — the runtime
# stage below copies just the jar, so the fixture never reaches the image.
COPY docker/dev-seed docker/dev-seed
RUN ./mvnw package -Dtest="!VointikaApplicationTests" -B

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app --no-create-home app
COPY --from=build /app/target/*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
