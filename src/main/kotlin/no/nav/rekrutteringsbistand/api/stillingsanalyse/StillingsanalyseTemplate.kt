package no.nav.rekrutteringsbistand.api.stillingsanalyse

object StillingsanalyseTemplate {

    val SYSTEM_MESSAGE = """
        Instruksjoner:
        
        Analyser stillingsbeskrivelsen og vurder følgende:
        
        1. **Sensitivitet:**
        
           - Det kan i systemene være kandidater tilknyttet stillinger. Sett `"sensitiv": true` hvis teksten inneholder sensitive ord som avslører informasjon om personer tilknyttet stillingen angående deres deltakelse i programmer og tiltak.
            For eksempel vil stillinger som sier at de søker skal være ips brukere, markeres som sensitive.
           - Sett `"sensitiv": false` hvis sensitive ord brukes i forbindelse med arbeidsoppgaver, ansvar eller mål som er en del av stillingen. Du kan godt jobbe med IPS uten at dette blir markert som sensitivt, Men om du skal være på IPS, så er det sensitivt.
           - Hvis det er for lite informasjon til å finne ut om konteksten til et sensitivt ord, sett alltid stillingen som sensitiv.
        
        2. **Samsvar med tittel:**
        
           - Sett `"samsvarMedTittel": true` hvis stillingstittelen samsvarer med innholdet i stillingsbeskrivelsen.
           - Sett `"samsvarMedTittel": false` hvis det er en uoverensstemmelse mellom tittelen og innholdet.
           - **tittelBegrunnelse**: Gi en kort forklaring på valget ditt.
        
        3. **Samsvar med type:**
        
           - Sett `"samsvarMedType": true` hvis stillingstypen er korrekt i forhold til beskrivelsen.
           - Sett `"samsvarMedType": false` hvis stillingstypen ikke stemmer med innholdet i beskrivelsen.
           - **typeBegrunnelse**: Forklar om stillingstypen passer til beskrivelsen.
        
        **Sensitive ord:**
        
        Følgende ord anses som sensitive hvis de sier noe om egenskaper eller tilknytning for kandidter som kan være tilknyttet stillingen:
        IPS, KVP, Kvalifiseringsprogram, Kvalifiseringslønn, Kvalifiseringsstønad, Aktivitetsplikt, Angst, Arbeid med støtte, Arbeidsevne, Arbeids- og utdanningsreiser, Arbeidsforberedende trening, Arbeidsmarkedskurs, Arbeidspraksis, Arbeidsrettet rehabilitering, Arbeidstrening, AU-reiser, Avklaringstiltak, Barn, Behandling, Behov, Behovsliste, Booppfølging, Deltaker, Depresjon, Diagnoser, Fastlege, Flyktning, Fravær, Gjeld, Helseavklaring, Husleie, Individuell jobbstøtte, Introduksjonsprogram, Introduksjonsstønad, Jobbklar, Jobbspesialist, Kognitive utfordringer, Kognitive problemer, Kognitivt, Kommunale lavterskeltilbud, Kommunale tiltak, Kommunale tjenester, Koordinert bistand, Lån, Langvarig, Livsopphold, Lønnstilskudd, Mentor, Mentortilskudd, Midlertidig bolig, Midlertidig botilbud, Nedsatt arbeidsevne, Nedsatt funksjon, Norskferdigheter, Oppfølging, Oppfølgingstiltak, Oppfølgning i bolig, Opplæring, Opplæringstiltak, Pengestøtte, Problemer, Psykiatri, Psykolog, Rehabilitering, Restanse, Rus, Sommerjobb, Sommerjobbtiltak, Sosial oppfølging, Sosialfaglig oppfølging, Sosialhjelp, Sosialhjelpsmottaker, Sosialstønad, Sosiale problemer, Sosiale utfordringer, Supplerende stønad, Supported Employment, Syk, Sykdom, Sykemeldt, Sykt barn, Tilskudd, Tiltak, Tiltaksdeltaker, Ukrain, Ukraina, Ungdom, Utfordringer, Utvidet oppfølging, Varig tilrettelagt arbeid, Venteliste, Økonomi, Økonomisk kartlegging, Økonomisk rådgivning, Økonomisk sosialhjelp, Økonomisk stønad 
       
        ---
        
        Eksempel på vurdering av sensitivitet:
        1. Stillingsteksten inneholder kun stillingsteksten "IPS eller andre med lignende behov".
            Vurdering: Sett `"sensitiv": true` fordi stillingen er relatert til IPS, og det står ikke noe spesifikt om at dette gjelder arbeidsoppgaver, så vi må anta at det her søkes etter personer med IPS tilknytning.
        2. Stillingstekten innholder teksten: "Ansvarsområder: Veilede personer med IPS."
            Vurdering: Sett `"sensitiv: false` fordi det er klart at det er snakk om arbeidsoppgaver, og det er ikke sensitivt at noen skal jobbe med IPS brukere.
        3. Stillingsteksten inneholder "oppfølging av brukere med IPS".
            Vurdering: Sett `"sensitiv: false` fordi det dette gjelder ansvarsoppgaver til de som får stillingen. Ingen sensitiv informasjon er avslørt, fordi det ikke sier noe sensitivt om personen som er tilknyttet denne stillingen.
            
        **Svarformat:**
        
        Vennligst svar kun med et JSON-objekt på norsk i følgende format:
        
        {
          "sensitiv": true/false,
          "sensitivBegrunnelse": "Din begrunnelse her.",
          "samsvarMedTittel": true/false,
          "tittelBegrunnelse": "Din begrunnelse her.",
          "samsvarMedType": true/false,
          "typeBegrunnelse": "Din begrunnelse her."
        }
        
        ---
        
        **Definisjoner:**
        
        - **samsvarMedTittel**: Evaluer om stillingstittelen korrekt reflekterer oppgavene og ansvaret beskrevet i stillingsbeskrivelsen.
        - **samsvarMedType**: Evaluer om stillingstypen (f.eks. "stilling", "jobbmesse", "formidling") er riktig valgt i forhold til innholdet.
        
    """.trimIndent()

    fun lagUserPrompt(stillingsanalyseDto: StillingsanalyseController.StillingsanalyseDto): String =
        """
            **Stillingstittel:**
            ${stillingsanalyseDto.stillingstittel}
    
            **Stillingstype:**
            ${stillingsanalyseDto.stillingstype}
    
            **Stillingsbeskrivelse:**
            ${stillingsanalyseDto.stillingstekst}
        """.trimIndent()
}
