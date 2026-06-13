FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace
COPY gradle gradle
COPY gradlew build.gradle settings.gradle ./
COPY src src
RUN ./gradlew clean bootJar --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 1001 bank
COPY --from=build /workspace/build/libs/*.jar app.jar
USER bank
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
