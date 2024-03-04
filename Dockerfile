FROM ubuntu:22.04
RUN apt-get update && apt-get -y install maven openjdk-17-jdk-headless
COPY ./ src/
WORKDIR /src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-alpine
RUN apk add fontconfig && apk add ttf-dejavu
EXPOSE 5000
COPY --from=0 /src/target/taxonomy-service.jar /app.jar
COPY ./run-app.sh /run-app.sh
ENTRYPOINT [ "/run-app.sh" ]