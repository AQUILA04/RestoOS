# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN apk add --no-cache maven && mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN adduser -D -u 10001 restoos
COPY --from=build /workspace/target/resto-os-backend-*.jar /app/app.jar
USER restoos
EXPOSE 8080
HEALTHCHECK --interval=20s --timeout=5s --retries=5 --start-period=60s \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
