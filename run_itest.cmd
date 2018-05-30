docker stop taxonomy-postgres
docker rm taxonomy-postgres
docker run -p 127.0.0.1:5432:5432 --name taxonomy-postgres -d postgres:alpine
SET SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5432/postgres
SET SPRING_DATASOURCE_PASSWORD=
SET SPRING_DATASOURCE_USERNAME=postgres
SET EMBEDDED=false
mvn clean install