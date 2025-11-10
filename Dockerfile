FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21
ENV TZ="Europe/Oslo"
COPY ./target/rekrutteringsbistand-stilling-api-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 9501
CMD ["-jar", "app.jar"]
