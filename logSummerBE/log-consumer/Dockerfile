FROM eclipse-temurin:21-jdk-alpine as build
WORKDIR /workspace/app

COPY gradle gradle
COPY gradlew .
COPY settings.gradle .
COPY log-consumer log-consumer

RUN ./gradlew :log-consumer:bootJar -x test

FROM eclipse-temurin:21-jre-alpine
VOLUME /tmp
COPY --from=build /workspace/app/log-consumer/build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]