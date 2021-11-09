#!/bin/sh

java -Xmx${HEAPSPACE_MAX:-"500m"} -jar /app.jar