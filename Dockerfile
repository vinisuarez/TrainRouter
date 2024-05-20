FROM gradle:8.7-jdk11 as build
WORKDIR /app
COPY . .
RUN gradle build

FROM openjdk:11-jre-slim
WORKDIR /app
COPY --from=build /app/build/libs/*.jar /app/trainRoute.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "trainRoute.jar"]