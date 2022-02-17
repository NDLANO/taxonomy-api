#!/bin/sh

java -Xmx${HEAPSPACE_MAX:-"1000m"} --add-exports java.desktop/sun.font=ALL-UNNAMED -jar /app.jar