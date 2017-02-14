#!/bin/bash

docker stop taxonomy-postgres
docker rm taxonomy-postgres
docker run -p 192.168.99.100:5432:5432 --name taxonomy-postgres -e POSTGRES_PASSWORD=as -d kiasaki/alpine-postgres

SPRING_DATASOURCE_URL="jdbc:postgresql://192.168.99.100:5432/" SPRING_DATASOURCE_USERNAME="postgres" SPRING_DATASOURCE_PASSWORD="as" SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT="org.hibernate.dialect.PostgreSQL94Dialect" mvn clean install
