FROM eclipse-temurin:21 AS base
RUN apt update && apt install -y gdal-bin
