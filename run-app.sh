#!/bin/sh

# Export docker profile to enable json logging when running in docker
if [[ "$RUNNING_IN_DOCKER" = "true" ]]; then
  PROVIDED_PROFILES=${SPRING_PROFILES_ACTIVE}
  if [[ -z "$PROVIDED_PROFILES" ]]; then
    export SPRING_PROFILES_ACTIVE="docker"
  else
    export SPRING_PROFILES_ACTIVE="docker,$PROVIDED_PROFILES"
  fi
fi

java -Xmx${HEAPSPACE_MAX:-"1000m"} --add-exports java.desktop/sun.font=ALL-UNNAMED -jar /app.jar
