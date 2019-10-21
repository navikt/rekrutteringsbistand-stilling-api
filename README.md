# TAG - rekrutteringsbistand api


### Oppsett

Bygg image
`docker build -t rekrutteringsbistand-api .`

Kjør container
`docker run -d -p 9501:9501 rekrutteringsbistand-api`

Åpnes i browser: [http://localhost:9501/rekrutteringsbistand-api/internal/healthcheck](http://localhost:9501/rekrutteringsbistand-api/internal/healthcheck)

### Notater
Applikasjonen er ikke enablet til vault enda (vi tar det når det blir behov for noen secrets)
