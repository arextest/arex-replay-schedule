FROM maven:3.9.9-eclipse-temurin-21 as builder
COPY . /usr/src/app/
WORKDIR /usr/src/app/
# buildkit support since docker 18.03
RUN --mount=type=cache,target=/root/.m2 mvn clean package -DskipTests -Pjar
# use this if you are on docker<=18.02
# RUN mvn clean package -DskipTests -Pjar

FROM eclipse-temurin:21-jre
COPY --from=builder /usr/src/app/arex-schedule-jar/schedule.jar app.jar
ENTRYPOINT ["sh", "-c", "java --add-opens=java.base/jdk.internal.loader=ALL-UNNAMED --add-opens=java.base/java.net=ALL-UNNAMED ${JAVA_OPTS} -jar /app.jar"]
