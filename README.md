# TAG - rekrutteringsbistand api
Administrerer veileder rekrutteringsdata for NAV
Fungerer også som en proxy for operasjoner mot stillinger.

## Starte applikasjonen i utviklingsmiljø
Start main i klassen RekrutteringsbistandApplication. 

### Mocking
I application.yml, velg mellom mocket stilling eller mot en localhost stillingserver med profilene 'mock' og 'ekstern'.
Mock er default.


## Starte applikasjonen i docker

### Bygg image
`docker build -t rekrutteringsbistand-api .`

###Kjør container
`docker run -d -p 9501:9501 rekrutteringsbistand-api`

## Åpne i browser
[http://localhost:9501/rekrutteringsbistand-api/swagger-ui.html](http://localhost:9501/rekrutteringsbistand-api/swagger-ui.html)


