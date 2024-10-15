package no.nav.rekrutteringsbistand.api.stillingsanalyse

object StillingsanalyseTemplate {

    val SYSTEM_MESSAGE = """
        Instruksjoner:
        
        Analyser stillingsbeskrivelsen og vurder følgende:
        
        1. **Sensitivitet:**
        
           - Sett `"sensitiv": true` hvis teksten inneholder sensitive ord og disse ordene ikke refererer til stillingens arbeidsoppgaver, ansvar eller mål.
           - Sett `"sensitiv": false` hvis de sensitive ordene brukes i sammenheng med arbeidsoppgaver, ansvar eller mål direkte knyttet til stillingen.
           - **Viktig:** Sensitive ord som er relatert til arbeidsoppgaver eller ansvar i stillingen skal **ikke** anses som sensitive. Det er heller ikke sensitivt når en stilling har som oppgave å følge opp personer i sårbare grupper, som for eksempel deltakere i KVP, når dette er en naturlig del av arbeidsoppgavene som er eksplisitt nevnt i stillingsbeskrivelsen. 
           - Sensitivitet oppstår når teksten gir innsikt i spesifikke personer som er koblet til stillingen, og som dermed kan avsløre deltakelse i programmer eller tiltak (som KVP). Dette gjelder særlig når teksten kan gi en indirekte identifisering av brukere, for eksempel ved å koble kandidaten direkte til sensitive tjenester eller tiltak. Hvis det er tvil om sensitivitet, skal det settes til `true` for å beskytte mot eksponering av sårbare grupper.
        
        2. **Samsvar med tittel:**
        
           - Sett `"samsvarMedTittel": true` hvis stillingstittelen samsvarer med innholdet i stillingsbeskrivelsen.
           - Sett `"samsvarMedTittel": false` hvis det er en uoverensstemmelse mellom tittelen og innholdet.
           - **tittelBegrunnelse**: Gi en kort begrunnelse for ditt valg. Forklar om tittelen representerer arbeidsoppgaver og ansvarsområder i stillingen på en korrekt måte.
        
        3. **Samsvar med type:**
        
           - Sett `"samsvarMedType": true` hvis stillingstypen er korrekt i forhold til beskrivelsen.
           - Sett `"samsvarMedType": false` hvis stillingstypen ikke stemmer med innholdet i beskrivelsen.
           - **typeBegrunnelse**: Forklar om stillingstypen samsvarer med beskrivelsen. Fokuser på om typene "stilling", "jobbmesse", eller "formidling" er korrekte, uten å overlappe med tittelvurderingen.
        
        **Sensitive ord:**
        
        Følgende ord anses som sensitive hvis de brukes i en kontekst som kan eksponere individer eller gi innsikt i deres tilknytning til programmer, sårbarheter eller spesifikke tiltak: 
        IPS, KVP, Kvalifiseringsprogram, Kvalifiseringslønn, Kvalifiseringsstønad, Aktivitetsplikt, Angst, Arbeid med støtte, Arbeidsevne, Arbeids- og utdanningsreiser, Arbeidsforberedende trening, Arbeidsmarkedskurs, Arbeidspraksis, Arbeidsrettet rehabilitering, Arbeidstrening, AU-reiser, Avklaringstiltak, Barn, Behandling, Behov, Behovsliste, Booppfølging, Deltaker, Depresjon, Diagnoser, Fastlege, Flyktning, Fravær, Gjeld, Helseavklaring, Husleie, Individuell jobbstøtte, Introduksjonsprogram, Introduksjonsstønad, Jobbklar, Jobbspesialist, Kognitive utfordringer, Kognitive problemer, Kognitivt, Kommunale lavterskeltilbud, Kommunale tiltak, Kommunale tjenester, Koordinert bistand, Lån, Langvarig, Livsopphold, Lønnstilskudd, Mentor, Mentortilskudd, Midlertidig bolig, Midlertidig botilbud, Nedsatt arbeidsevne, Nedsatt funksjon, Norskferdigheter, Oppfølging, Oppfølgingstiltak, Oppfølgning i bolig, Opplæring, Opplæringstiltak, Pengestøtte, Problemer, Psykiatri, Psykolog, Rehabilitering, Restanse, Rus, Sommerjobb, Sommerjobbtiltak, Sosial oppfølging, Sosialfaglig oppfølging, Sosialhjelp, Sosialhjelpsmottaker, Sosialstønad, Sosiale problemer, Sosiale utfordringer, Supplerende stønad, Supported Employment, Syk, Sykdom, Sykemeldt, Sykt barn, Tilskudd, Tiltak, Tiltaksdeltaker, Ukrain, Ukraina, Ungdom, Utfordringer, Utvidet oppfølging, Varig tilrettelagt arbeid, Venteliste, Økonomi, Økonomisk kartlegging, Økonomisk rådgivning, Økonomisk sosialhjelp, Økonomisk stønad 
        
        **Unntak:**
        
        Hvis et sensitivt ord brukes i sammenheng med arbeidsoppgaver, ansvar eller kvalifikasjoner (f.eks. "Oppfølging av deltakere på kvalifiseringsprogrammet (KVP)"), skal det ikke anses som sensitivt. Sensitivitet gjelder kun hvis bruken kan identifisere sårbare individer eller koble spesifikke brukere til stillingen, noe som kan avsløre deres deltakelse i beskyttede eller sårbare programmer.
        
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
