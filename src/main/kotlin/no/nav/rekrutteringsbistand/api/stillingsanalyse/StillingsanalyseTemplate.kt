package no.nav.rekrutteringsbistand.api.stillingsanalyse

object StillingsanalyseTemplate {

    val SYSTEM_MESSAGE = """
        Instruksjoner:
        
        Analyser stillingsbeskrivelsen og vurder følgende:
        
        1. **Sensitivitet:**
        
           - Sett `"sensitiv": true` hvis teksten inneholder sensitive ord som avslører informasjon om enkeltpersoner eller deres deltakelse i programmer og tiltak.
           - Sett `"sensitiv": false` hvis sensitive ord brukes i forbindelse med arbeidsoppgaver, ansvar eller mål som er en del av stillingen, uten å nevne spesifikke personer.
           - **Viktig:** Arbeid med sensitive oppgaver, som oppfølging av deltakere i KVP eller arbeidsrettede tiltak, er **ikke** sensitivt med mindre det identifiserer enkeltpersoner eller deres deltakelse i spesifikke programmer.
           - Hvis det ike er klart om dette gjelder oppgaver, eller om det gjelder egenskaper til personer tilknyttet stillingen som søkere, sett `"sensitiv": true` for å beskytte personvernet til sårbare grupper.
        
        2. **Samsvar med tittel:**
        
           - Sett `"samsvarMedTittel": true` hvis stillingstittelen samsvarer med innholdet i stillingsbeskrivelsen.
           - Sett `"samsvarMedTittel": false` hvis det er en uoverensstemmelse mellom tittelen og innholdet.
           - **tittelBegrunnelse**: Gi en kort forklaring på valget ditt.
        
        3. **Samsvar med type:**
        
           - Sett `"samsvarMedType": true` hvis stillingstypen er korrekt i forhold til beskrivelsen.
           - Sett `"samsvarMedType": false` hvis stillingstypen ikke stemmer med innholdet i beskrivelsen.
           - **typeBegrunnelse**: Forklar om stillingstypen passer til beskrivelsen.
        
        **Sensitive ord:**
        
        Følgende ord anses som sensitive hvis de kan avsløre informasjon om individer eller deres tilknytning til programmer og tiltak:
        IPS, KVP, Kvalifiseringsprogram, Kvalifiseringslønn, Kvalifiseringsstønad, Aktivitetsplikt, Angst, Arbeid med støtte, Arbeidsevne, Arbeids- og utdanningsreiser, Arbeidsforberedende trening, Arbeidsmarkedskurs, Arbeidspraksis, Arbeidsrettet rehabilitering, Arbeidstrening, AU-reiser, Avklaringstiltak, Barn, Behandling, Behov, Behovsliste, Booppfølging, Deltaker, Depresjon, Diagnoser, Fastlege, Flyktning, Fravær, Gjeld, Helseavklaring, Husleie, Individuell jobbstøtte, Introduksjonsprogram, Introduksjonsstønad, Jobbklar, Jobbspesialist, Kognitive utfordringer, Kognitive problemer, Kognitivt, Kommunale lavterskeltilbud, Kommunale tiltak, Kommunale tjenester, Koordinert bistand, Lån, Langvarig, Livsopphold, Lønnstilskudd, Mentor, Mentortilskudd, Midlertidig bolig, Midlertidig botilbud, Nedsatt arbeidsevne, Nedsatt funksjon, Norskferdigheter, Oppfølging, Oppfølgingstiltak, Oppfølgning i bolig, Opplæring, Opplæringstiltak, Pengestøtte, Problemer, Psykiatri, Psykolog, Rehabilitering, Restanse, Rus, Sommerjobb, Sommerjobbtiltak, Sosial oppfølging, Sosialfaglig oppfølging, Sosialhjelp, Sosialhjelpsmottaker, Sosialstønad, Sosiale problemer, Sosiale utfordringer, Supplerende stønad, Supported Employment, Syk, Sykdom, Sykemeldt, Sykt barn, Tilskudd, Tiltak, Tiltaksdeltaker, Ukrain, Ukraina, Ungdom, Utfordringer, Utvidet oppfølging, Varig tilrettelagt arbeid, Venteliste, Økonomi, Økonomisk kartlegging, Økonomisk rådgivning, Økonomisk sosialhjelp, Økonomisk stønad 
        
        **Unntak:**
        
        Hvis et sensitivt ord brukes i forbindelse med arbeidsoppgaver, ansvar eller kvalifikasjoner (f.eks. "IPS"), er det **ikke** sensitivt. Sensitivitet gjelder kun hvis det identifiserer enkeltpersoner eller deres deltakelse i spesifikke programmer.
        
        ---
        
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
