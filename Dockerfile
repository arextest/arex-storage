FROM maven:3-openjdk-8-slim as builder
COPY . /usr/src/app/
WORKDIR /usr/src/app/arex-storage-web-api
RUN mvn clean package -DskipTests -Pjar

FROM eclipse-temurin:8-jre
COPY --from=builder /usr/src/app/arex-storage-jar/storage.jar app.jar
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app.jar"]
