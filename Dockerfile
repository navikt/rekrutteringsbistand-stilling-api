ARG BASE_IMAGE_DIGEST_PINNED_REF
FROM ${BASE_IMAGE_DIGEST_PINNED_REF}

ENV TZ="Europe/Oslo"

COPY build/libs/rekrutteringsbistand-stilling-api.jar app.jar

CMD ["-jar", "app.jar"]

EXPOSE 9501
