#!/bin/bash
set -e

COMMAND="mvn clean install"
if [ "$1" == "-i" ]
then
    COMMAND="mvn spring-boot:run"
fi

IP=127.0.0.1

if which docker-machine > /dev/null
then
    if ! docker-machine inspect default > /dev/null
    then
        echo creating docker-machine default
        docker-machine create --driver virtualbox default
    fi
    if [ "$(docker-machine status default)" != "Running" ]
    then
        echo starting docker-machine default
        docker-machine start default
    fi
    eval "$(docker-machine env default)"
    IP=$(docker-machine ip default)
fi

if docker inspect taxonomy-postgres > /dev/null 2>&1
then
    echo removing old instance
    docker stop taxonomy-postgres > /dev/null 2>&1 || true
    docker rm taxonomy-postgres
fi

echo running postgres
docker run -p ${IP}:5432:5432 --name taxonomy-postgres -d postgres:alpine

set +e
EMBEDDED="false" \
SPRING_DATASOURCE_URL="jdbc:postgresql://${IP}:5432/postgres" \
    SPRING_DATASOURCE_USERNAME="postgres" SPRING_DATASOURCE_PASSWORD="" \
    SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT="org.hibernate.dialect.PostgreSQL94Dialect" \
    ${COMMAND}
EXIT_CODE=$?
set -e

echo cleaning up

if docker inspect taxonomy-postgres > /dev/null
then
    echo removing old instance
    docker stop taxonomy-postgres
    docker rm taxonomy-postgres
fi

exit ${EXIT_CODE}
