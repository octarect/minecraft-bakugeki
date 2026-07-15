FROM eclipse-temurin:21-jdk AS build
ENV VERSION=dev
WORKDIR /app
COPY . .
RUN ./gradlew shadowJar --no-daemon
RUN ./gradlew shadowJar --no-daemon

FROM alpine:latest
COPY --from=build /app/build/libs/*.jar /
