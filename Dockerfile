FROM cgr.dev/chainguard/jre:openjdk-21
ENV TZ="Europe/Oslo"
COPY ./target/rekrutteringsbistand-stilling-api-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 9501
CMD ["-jar", "app.jar"]
