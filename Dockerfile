FROM eclipse-temurin:25 AS base
RUN apt update && apt install -y gdal-bin
