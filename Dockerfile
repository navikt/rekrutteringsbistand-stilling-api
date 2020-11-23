FROM navikt/java:13
COPY import-vault-token.sh /init-scripts
COPY /target/rekrutteringsbistand-api-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 9501
