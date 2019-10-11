FROM navikt/java:11
COPY import-vault-token.sh /init-scripts
COPY /target/rekrutteringsbistand-api-0.0.1-SNAPSHOT.jar app.jar
