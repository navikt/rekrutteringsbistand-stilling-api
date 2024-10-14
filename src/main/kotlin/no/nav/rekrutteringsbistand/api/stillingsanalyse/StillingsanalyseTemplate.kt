package no.nav.rekrutteringsbistand.api.stillingsanalyse


object StillingsanalyseTemplate {

    fun lagPrompt(stillingsanalyseDto: StillingsanalyseController.StillingsanalyseDto): String =
        """
       Vennligst analyser følgende stillingsbeskrivelse og avgjør om den inneholder sensitiv informasjon basert på listen over sensitive ord. Hvis teksten inneholder noen av ordene fra listen, skal den anses som sensitiv med mindre ordene refererer til arbeidsoppgaver, ansvar eller mål for stillingen som skal besettes (for eksempel 'oppfølging av KVP-deltakere' for en 'støttekontakt').
       Sensitive ord relatert til arbeidsoppgaver, ansvar eller mål direkte knyttet til stillingen skal ikke anses som sensitiv informasjon, og skal i de tilfellene oppføres som sensitiv=false.


        **Stillingstittel:**
        ${stillingsanalyseDto.stillingstittel}
        
        **Stillingstype:**
        ${stillingsanalyseDto.stillingstype}

        **Stillingsbeskrivelse:**
        ${stillingsanalyseDto.stillingstekst}

        **Sensitive ord:**
        IPS, KVP, Kvalifiseringsprogram, Kvalifiseringslønn, Kvalifiseringsstønad, Aktivitetsplikt, Angst, Arbeid med støtte, Arbeidsevne, Arbeids- og utdanningsreiser, Arbeidsforberedende trening, Arbeidsmarkedskurs, Arbeidspraksis, Arbeidsrettet rehabilitering, Arbeidstrening, AU-reiser, Avklaringstiltak, Barn, Behandling, Behov, Behovsliste, Booppfølging, Deltaker, Depresjon, Diagnoser, Fastlege, Flyktning, Fravær, Gjeld, Helseavklaring, Husleie, Individuell jobbstøtte, Introduksjonsprogram, Introduksjonsstønad, Jobbklar, Jobbspesialist, Kognitive utfordringer, Kognitive problemer, Kognitivt, Kommunale lavterskeltilbud, Kommunale tiltak, Kommunale tjenester, Koordinert bistand, Lån, Langvarig, Livsopphold, Lønnstilskudd, Mentor, Mentortilskudd, Midlertidig bolig, Midlertidig botilbud, Nedsatt arbeidsevne, Nedsatt funksjon, Norskferdigheter, Oppfølging, Oppfølgingstiltak, Oppfølgning i bolig, Opplæring, Opplæringstiltak, Pengestøtte, Problemer, Psykiatri, Psykolog, Rehabilitering, Restanse, Rus, Sommerjobb, Sommerjobbtiltak, Sosial oppfølging, Sosialfaglig oppfølging, Sosialhjelp, Sosialhjelpsmottaker, Sosialstønad, Sosiale problemer, Sosiale utfordringer, Supplerende stønad, Supported Employment, Syk, Sykdom, Sykemeldt, Sykt barn, Tilskudd, Tiltak, Tiltaksdeltaker, Ukrain, Ukraina, Ungdom, Utfordringer, Utvidet oppfølging, Varig tilrettelagt arbeid, Venteliste, Økonomi, Økonomisk kartlegging, Økonomisk rådgivning, Økonomisk sosialhjelp, Økonomisk stønad

        **Unntak:** Hvis ordet brukes i sammenheng med stillingen som skal besettes (for eksempel "IPS veileder"), skal det ikke anses som sensitivt. Ingenting er sensitivt om det refererer til ønske om en spesialist på et av de sensitive temaene.

        **Gi svaret på norsk i følgende JSON-format:**
        {
          "sensitiv": true/false,
          "sensitivBegrunnelse": "Din begrunnelse her."
          "samsvarMedTittel": true/false,
          "tittelBegrunnelse": "Din begrunnelse her."
          "samsvarMedType": true/false,
          "typeBegrunnelse": "Din begrunnelse her."
        }
        
        samsvarMedTittel referer til om det er samsvar med tittel og stillingstekst
        samsvarMedType referer til om det er samsvar med stillingstype og stillingstekst
        jobbmesser og formidlinger har lite krav til innhold i teksten. Men stillingstype stilling, skal være en stillingsannonse, som refererer til en vanlig stilling som kan utlyses.
        
        Vennligst svar kun med JSON-objektet uten ytterligere forklaring, og uten kodeblokker eller markdown.
        
        """.trimIndent()
}
