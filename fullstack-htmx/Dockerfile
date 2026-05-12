FROM eclipse-temurin:21-jdk-noble AS build
WORKDIR /workspace
COPY . .
RUN chmod +x gradlew && \
    ./gradlew :fullstack-htmx:app:bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-noble AS extract
WORKDIR /layers
COPY --from=build /workspace/fullstack-htmx/app/build/libs/*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

FROM eclipse-temurin:21-jre-noble
RUN groupadd --system spring && useradd --system --gid spring --no-create-home spring
WORKDIR /app
COPY --from=extract /layers/extracted/dependencies/ ./
COPY --from=extract /layers/extracted/spring-boot-loader/ ./
COPY --from=extract /layers/extracted/snapshot-dependencies/ ./
COPY --from=extract /layers/extracted/application/ ./
RUN chown -R spring:spring /app
USER spring:spring
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
