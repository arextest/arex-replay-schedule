FROM maven:3-openjdk-8-slim as builder
COPY . /usr/src/app/
WORKDIR /usr/src/app/arex-schedule-web-api
RUN mvn clean package -DskipTests -Pjar

FROM eclipse-temurin:8-jdk
COPY --from=builder /usr/src/app/arex-schedule-jar/schedule.jar app.jar
ENTRYPOINT ["java","${JAVA_OPTS}","-jar","/app.jar"]