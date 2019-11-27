# rekrutteringsbistand-api

Administrerer Nav-veilders rekrutteringsdata" for NAV
Fungerer også som en proxy for operasjoner mot stillingsystemet til Nav.


# Komme i gang
## Bygge
Maven: `mvn clean install`

## Kjøre
### Mocking
I application.yml, velg mellom mocket stilling eller mot en localhost stillingserver med profilene 'stillingMock' og 'eksternStilling'.
stillingMock er default.

### Starte applikasjonen i utviklingsmiljø
Start main i klassen RekrutteringsbistandApplication. 

## Åpne i browser
[http://localhost:9501/rekrutteringsbistand-api/swagger-ui.html](http://localhost:9501/rekrutteringsbistand-api/swagger-ui.html)

## Docker
1. Bygg image: `docker build -t rekrutteringsbistand-api .`
2. Kjør container: `docker run -d -p 9501:9501 rekrutteringsbistand-api`


# Henvendelser

* Opprett gjerne en issue i Github for alle typer spørsmål
* IT-utviklerne i Github-teamet https://github.com/orgs/navikt/teams/teamtag
* IT-avdelingen i [Arbeids- og velferdsdirektoratet](https://www.nav.no/no/NAV+og+samfunn/Kontakt+NAV/Relatert+informasjon/arbeids-og-velferdsdirektoratet-kontorinformasjon)

## For NAV-ansatte

* Slack-kanalene [#tag-utvikling](https://nav-it.slack.com/archives/CD4MES6BB) eller [#tag-general](https://nav-it.slack.com/archives/CCM649PDH)
* [Confluence](https://confluence.adeo.no/x/GdBxDw)
* [Microsoft Teams](https://teams.microsoft.com/l/team/19%3af272e8d7060f48b19d2c40af46947228%40thread.skype/conversations?groupId=5296beb6-98bf-48d1-a3a5-089f57670a4d&tenantId=62366534-1ec3-4962-8869-9b5535279d0b)
