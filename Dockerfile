FROM maven:3-openjdk-8-slim as builder
COPY . /usr/src/app/
WORKDIR /usr/src/app
RUN mvn clean package -DskipTests

FROM tomcat:9.0-jdk8-openjdk
COPY --from=builder /usr/src/app/arex-storage-web-api/target/arex-storage-web-api.war /usr/local/tomcat/webapps/arex-storage-web-api.war
WORKDIR /usr/local/tomcat/conf
RUN sed -i 'N;152a\\t<Context path="" docBase="arex-storage-web-api.war" reloadable="true" />' server.xml

WORKDIR /usr/local/tomcat
EXPOSE 8080
CMD ["catalina.sh","run"]
