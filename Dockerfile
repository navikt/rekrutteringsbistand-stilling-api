FROM ghcr.io/navikt/baseimages/temurin:21
COPY /target/rekrutteringsbistand-stilling-api-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 9501
