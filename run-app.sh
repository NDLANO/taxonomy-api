#!/bin/bash

java -Xmx${HEAPSPACE_MAX:-"500m"} -jar /app.jar