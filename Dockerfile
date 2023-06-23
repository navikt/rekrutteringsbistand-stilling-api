FROM ghcr.io/navikt/baseimages/temurin:17
COPY /target/rekrutteringsbistand-stilling-api-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 9501
