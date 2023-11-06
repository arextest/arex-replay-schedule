FROM eclipse-temurin:8-jdk-alpine as builder
COPY . /usr/src/app/
WORKDIR /usr/src/app/arex-schedule-web-api
RUN mvn clean package -DskipTests

FROM tomcat:9.0-jdk8-openjdk
COPY --from=builder /usr/src/app/arex-schedule-web-api/target/arex-schedule-web-api.war /usr/local/tomcat/webapps/arex-schedule-web-api.war
WORKDIR /usr/local/tomcat/conf
RUN sed -i 'N;152a\\t<Context path="" docBase="arex-schedule-web-api.war" reloadable="true" />' server.xml

WORKDIR /usr/local/tomcat
EXPOSE 8080
CMD ["catalina.sh","run"]
