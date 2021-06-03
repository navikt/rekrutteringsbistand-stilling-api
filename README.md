# rekrutteringsbistand-stilling-api

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
[http://localhost:9501/rekrutteringsbistand-stilling-api/swagger-ui.html](http://localhost:9501/rekrutteringsbistand-stilling-api/swagger-ui.html)

## Docker
1. Bygg image: `docker build -t rekrutteringsbistand-stilling-api .`
2. Kjør container: `docker run -d -p 9501:9501 rekrutteringsbistand-stilling-api`

# Overvåkning og observability
* Prodfeil detekteres av [Nais-alerts](https://doc.nais.io/observability/alerts), som genererer meldinger i Slack-kanalen [#tag-inkludering-alerts-prod](https://nav-it.slack.com/archives/CR00PGB1P).
  * Alerts er definert i filen prod-alerts.yaml
  * For å se gjeldende (deployede) alerts, kjør kommandoen `kubectl describe alerts rekrutteringsbistand-stilling-api`
* [Grafana-dashboard](https://grafana.adeo.no/d/odDKuXbWk/rekrutteringsbistand-stilling-api-prod-fss) viser tekniske metrikker
* Kibana brukes for å lese applikasjonsloggen. [Eksempel på spørring](https://logs.adeo.no/s/read-only/app/kibana#/discover?_g=(refreshInterval:(pause:!t,value:0),time:(from:now-4h,mode:quick,to:now))&_a=(columns:!(message,level,application,cluster,exception,namespace),index:'96e648c0-980a-11e9-830a-e17bbd64b4db',interval:auto,query:(language:lucene,query:'cluster:%20prod-fss%20AND%20application:%20rekrutteringsbistand-stilling-api%20AND%20(level:%20Warning%20OR%20level:%20Error)'),sort:!('@timestamp',desc)))

__NB:__ Per desember 2019 vil errors og warnings som logges ved oppstart av applikasjonen føre til melding i Slack først etter en time. Det skyldes at alerten utløses av en _økning_ i errors/warnings _etter_ at applikasjonen er ferdig startet.

# Henvendelser

## For Nav-ansatte
* Dette Git-repositoriet eies av [Team inkludering i Produktområde arbeidsgiver](https://navno.sharepoint.com/sites/intranett-prosjekter-og-utvikling/SitePages/Produktomr%C3%A5de-arbeidsgiver.aspx).
* Slack-kanaler:
  * [#inkludering-utvikling](https://nav-it.slack.com/archives/CQZU35J6A)
  * [#arbeidsgiver-utvikling](https://nav-it.slack.com/archives/CD4MES6BB)
  * [#arbeidsgiver-general](https://nav-it.slack.com/archives/CCM649PDH)

## For folk utenfor Nav
* Opprett gjerne en issue i Github for alle typer spørsmål
* IT-utviklerne i Github-teamet https://github.com/orgs/navikt/teams/arbeidsgiver
* IT-avdelingen i [Arbeids- og velferdsdirektoratet](https://www.nav.no/no/NAV+og+samfunn/Kontakt+NAV/Relatert+informasjon/arbeids-og-velferdsdirektoratet-kontorinformasjon)