# rekrutteringsbistand-stilling-api

Administrerer Nav-veilders rekrutteringsdata" for NAV
Fungerer også som en proxy for operasjoner mot stillingsystemet til Nav.

# Komme i gang
## Bygge

Bygg og kjør testene:

```bash
./gradlew clean build --warning-mode fail --configuration-cache -Dorg.gradle.configuration-cache.parallel=true
```

## Kjøre
### Mocking
I application.yml, velg mellom mocket stilling eller mot en localhost stillingserver med profilene 'stillingMock' og 'eksternStilling'.
stillingMock er default.

### Starte applikasjonen i utviklingsmiljø 
Start main i klassen RekrutteringsbistandApplication.

## Docker
1. Bygg image: `docker build --build-arg BASE_IMAGE_DIGEST_PINNED_REF=europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-25 -t rekrutteringsbistand-stilling-api .`
2. Kjør container: `docker run -d -p 9501:9501 rekrutteringsbistand-stilling-api`


## Kode generert av GitHub Copilot

Dette repoet bruker GitHub Copilot til å generere kode.


# Henvendelser

## For Nav-ansatte
* Dette Git-repositoriet eies av [team Toi](https://teamkatalog.nav.no/team/76f378c5-eb35-42db-9f4d-0e8197be0131).
* Slack: [#arbeidsgiver-toi-dev](https://nav-it.slack.com/archives/C02HTU8DBSR)

## For folk utenfor Nav
* Teknologiavdelingen i [Arbeids- og velferdsdirektoratet](https://www.nav.no/no/NAV+og+samfunn/Kontakt+NAV/Relatert+informasjon/arbeids-og-velferdsdirektoratet-kontorinformasjon)
