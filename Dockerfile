FROM maven:3.9.9-eclipse-temurin-21 as builder
COPY . /usr/src/app/
WORKDIR /usr/src/app
RUN mvn clean package -DskipTests -Pjar

FROM eclipse-temurin:21-jre
COPY --from=builder /usr/src/app/arex-storage-jar/storage.jar app.jar
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app.jar"]
