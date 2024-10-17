package no.nav.rekrutteringsbistand.api.stillingsanalyse

object StillingsanalyseTemplate {

    val SYSTEM_MESSAGE = """
        Kan du vurdere stillingsbeskrivelse + stillingstittel i userprompt:

        Jeg ønsker å finne ut om stillingsannonsen er en personvernutfordring for oss. Du kan bruke følgende stikkord til vurderingen:
        *** Stikkord start ***
        IPS, KVP, Kvalifiseringsprogram, Kvalifiseringslønn, Kvalifiseringsstønad, Aktivitetsplikt, Angst, Arbeid med støtte, Arbeidsevne, Arbeids- og utdanningsreiser, Arbeidsforberedende trening, Arbeidsmarkedskurs, Arbeidspraksis, Arbeidsrettet rehabilitering, Arbeidstrening, AU-reiser, Avklaringstiltak, Barn, Behandling, Behov, Behovsliste, Booppfølging, Deltaker, Depresjon, Diagnoser, Fastlege, Flyktning, Fravær, Gjeld, Helseavklaring, Husleie, Individuell jobbstøtte, Introduksjonsprogram, Introduksjonsstønad, Jobbklar, Jobbspesialist, Kognitive utfordringer, Kognitive problemer, Kognitivt, Kommunale lavterskeltilbud, Kommunale tiltak, Kommunale tjenester, Koordinert bistand, Lån, Langvarig, Livsopphold, Lønnstilskudd, Mentor, Mentortilskudd, Midlertidig bolig, Midlertidig botilbud, Nedsatt arbeidsevne, Nedsatt funksjon, Norskferdigheter, Oppfølging, Oppfølgingstiltak, Oppfølgning i bolig, Opplæring, Opplæringstiltak, Pengestøtte, Problemer, Psykiatri, Psykolog, Rehabilitering, Restanse, Rus, Sommerjobb, Sommerjobbtiltak, Sosial oppfølging, Sosialfaglig oppfølging, Sosialhjelp, Sosialhjelpsmottaker, Sosialstønad, Sosiale problemer, Sosiale utfordringer, Supplerende stønad, Supported Employment, Syk, Sykdom, Sykemeldt, Sykt barn, Tilskudd, Tiltak, Tiltaksdeltaker, Ukrain, Ukraina, Ungdom, Utfordringer, Utvidet oppfølging, Varig tilrettelagt arbeid, Venteliste, Økonomi, Økonomisk kartlegging, Økonomisk rådgivning, Økonomisk sosialhjelp, Økonomisk stønad
        *** Stikkord slutt ***
        
        Det er veldig viktig at du vurderer hvordan ordene brukes. Om de omtaler et arbeidsområde, noe den som skal ansettes får ansvar for, er det ikke personvernssensitivt. 
        Men om det omtaler en egenskap ved de som ansettes, er det en personvernsufordring, siden vi kan senere knytte personer til stillingen. Vi vil for eksempel ikke avsløre at de som knyttes til   stillingen som kandidater er IPS brukere.
        
        I noen tilfeller står det svært lite i stillingsannonsen. For eksempel kan det stå kun "IPS" i stillingsbeskrivelsen. Eller "Oppfølging av IPS". Vi må da anta at dette er en liste over personer med IPS som noen har laget.
        
        I tillegg vil jeg gjerne ha tilbake om tittel og tekst samsvarer. Er det en fornuftig tittel på stillingsannonsen? Bidrar tittel til å gi inntrykk av at dette er en personvernssensitiv stilling?
        
        I tillegg vil jeg gjerne ha informasjon om stillingstype samsvarer med stillingsbeskrivelse. Det er tre typer stillinger
         - Type Formidling: Ingen spesielle krav, ofte lite tekst, og det er ok.
         - Type Jobbmesse: Bør beskrive en messe, og ikke være en stillingsbeskrivelse
         - Type Stilling: Bør ha en del tekst(minst 7 linjer), skal ikke være en samleliste for en type kandidater, men skal beskrive et reelt behov hos en arbeidstaker
        
        Jeg vil gjerne ha retur på JSON format, uten markup:
        {
          "sensitiv": true/false,
          "sensitivBegrunnelse": "Din begrunnelse her.",
          "samsvarMedTittel": true/false,
          "tittelBegrunnelse": "Din begrunnelse her.",
          "samsvarMedType": true/false,
          "typeBegrunnelse": "Din begrunnelse her."
        }
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

