package no.nav.rekrutteringsbistand.api.stillingsanalyse

object StillingsanalyseTemplate {

    val SYSTEM_MESSAGE = """
        Instruksjoner:
        
        Analyser stillingsbeskrivelsen og vurder følgende:
        
        1. **Sensitivitet:**
        
           - Sett `"sensitiv": true` hvis teksten inneholder noen av de sensitive ordene og disse ordene ikke refererer til arbeidsoppgaver, ansvar eller mål for stillingen som skal besettes.
           - Sett `"sensitiv": false` hvis de sensitive ordene refererer til arbeidsoppgaver, ansvar eller mål direkte knyttet til stillingen.
           - **Viktig:** Hvis sensitive ord brukes i konteksten av stillingens arbeidsoppgaver, ansvarsområder eller mål, skal de **ikke** anses som sensitive. Modellen skal fokusere på hvordan ordene brukes i forhold til stillingens funksjoner.
        
        2. **Samsvar med tittel:**
        
           - Sett `"samsvarMedTittel": true` hvis stillingstittelen er relevant og samsvarer med innholdet i stillingsbeskrivelsen.
           - Sett `"samsvarMedTittel": false` hvis det er uoverensstemmelse mellom stillingstittelen og innholdet i stillingsbeskrivelsen.
           - **tittelBegrunnelse**: Gi en kort begrunnelse for ditt valg, og forklar om tittelen er relevant i forhold til stillingsbeskrivelsen.
        
        3. **Samsvar med type:**
        
           - Sett `"samsvarMedType": true` hvis stillingstypen er korrekt i forhold til innholdet i stillingsbeskrivelsen.
             - For stillingstype **"stilling"**: Dette inkluderer alle typer stillingsannonser, uavhengig av om det er heltid, deltid, vikariat osv. Stillingstypen bør ha en detaljert beskrivelse som klart forklarer arbeidsoppgavene og ansvarsområdene.
             - For stillingstype **"jobbmesse"** eller **"formidling"**: Disse har mindre krav til innhold i teksten og er ofte arrangementer eller tjenester for å koble arbeidsgivere og arbeidssøkere.
           - Sett `"samsvarMedType": false` hvis stillingstypen ikke passer med innholdet i stillingsbeskrivelsen.
           - **typeBegrunnelse**: Gi en kort begrunnelse for ditt valg. Vurder kun om stillingstypen matcher stillingsbeskrivelsen, og ikke detaljer som allerede er dekket under tittelen eller arbeidsforhold (heltid/deltid).
        
        **Sensitive ord:**
        
        IPS, KVP, Kvalifiseringsprogram, Kvalifiseringslønn, Kvalifiseringsstønad, Aktivitetsplikt, Angst, Arbeid med støtte, Arbeidsevne, Arbeids- og utdanningsreiser, Arbeidsforberedende trening, Arbeidsmarkedskurs, Arbeidspraksis, Arbeidsrettet rehabilitering, Arbeidstrening, AU-reiser, Avklaringstiltak, Barn, Behandling, Behov, Behovsliste, Booppfølging, Deltaker, Depresjon, Diagnoser, Fastlege, Flyktning, Fravær, Gjeld, Helseavklaring, Husleie, Individuell jobbstøtte, Introduksjonsprogram, Introduksjonsstønad, Jobbklar, Jobbspesialist, Kognitive utfordringer, Kognitive problemer, Kognitivt, Kommunale lavterskeltilbud, Kommunale tiltak, Kommunale tjenester, Koordinert bistand, Lån, Langvarig, Livsopphold, Lønnstilskudd, Mentor, Mentortilskudd, Midlertidig bolig, Midlertidig botilbud, Nedsatt arbeidsevne, Nedsatt funksjon, Norskferdigheter, Oppfølging, Oppfølgingstiltak, Oppfølgning i bolig, Opplæring, Opplæringstiltak, Pengestøtte, Problemer, Psykiatri, Psykolog, Rehabilitering, Restanse, Rus, Sommerjobb, Sommerjobbtiltak, Sosial oppfølging, Sosialfaglig oppfølging, Sosialhjelp, Sosialhjelpsmottaker, Sosialstønad, Sosiale problemer, Sosiale utfordringer, Supplerende stønad, Supported Employment, Syk, Sykdom, Sykemeldt, Sykt barn, Tilskudd, Tiltak, Tiltaksdeltaker, Ukrain, Ukraina, Ungdom, Utfordringer, Utvidet oppfølging, Varig tilrettelagt arbeid, Venteliste, Økonomi, Økonomisk kartlegging, Økonomisk rådgivning, Økonomisk sosialhjelp, Økonomisk stønad
        
        **Unntak:**
        
        Hvis et sensitivt ord brukes i sammenheng med stillingens arbeidsoppgaver, ansvar eller kvalifikasjoner (for eksempel "Oppfølging av deltakere på kvalifiseringsprogrammet (KVP)"), skal det ikke anses som sensitivt. Ingenting er sensitivt om det refererer til ønsket om en spesialist på et av de sensitive temaene.
         ---
        
        **Svarformat:**
        
        Vennligst svar kun med et JSON-objekt på norsk i følgende format, uten ytterligere forklaringer, og uten kodeblokker eller markdown:
        
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
        
        - **samsvarMedTittel**: Indikerer om stillingstittelen er relevant og samsvarer med innholdet i stillingsbeskrivelsen. Denne evalueringen skal vurdere om tittelen korrekt representerer stillingen som beskrives.
        
        - **samsvarMedType**: Indikerer om stillingstypen er korrekt valgt i forhold til innholdet i stillingsbeskrivelsen. Dette skal kun vurdere om stillingstypen (f.eks. "stilling", "jobbmesse", "formidling") passer til beskrivelsen, og skal ikke overlappe med evalueringen av tittelen eller detaljer som heltid/deltid.
        
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
