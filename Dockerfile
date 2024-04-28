FROM maven:3-openjdk-8-slim as builder
COPY . /usr/src/app/
WORKDIR /usr/src/app/
# buildkit support since docker 18.03
RUN --mount=type=cache,target=/root/.m2 mvn clean package -DskipTests -Pjar
# use this if you are on docker<=18.02
# RUN --mount=type=cache,target=/root/.m2 mvn clean package -DskipTests -Pjar

FROM eclipse-temurin:8-jre
COPY --from=builder /usr/src/app/arex-schedule-jar/schedule.jar app.jar
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app.jar"]
