package no.nav.rekrutteringsbistand.api.stillingsanalyse


object StillingsanalyseTemplate {

    fun lagPrompt(stillingsanalyseDto: StillingsanalyseController.StillingsanalyseDto): String =
        """
      Instruksjoner:

      Analyser stillingsbeskrivelsen og avgjør om den inneholder sensitiv informasjon basert på listen over sensitive ord.

      Sensitiv: Sett "sensitiv": true hvis teksten inneholder noen av de sensitive ordene og disse ordene ikke refererer til arbeidsoppgaver, ansvar eller mål for stillingen som skal besettes.

      Ikke sensitiv: Sett "sensitiv": false hvis de sensitive ordene refererer til arbeidsoppgaver, ansvar eller mål direkte knyttet til stillingen.

      Det er ikke ansett som sensitivt å lyse ut en stilling der man søker en person som skal arbeide med utsatte grupper eller sensitive temaer.

      Sensitive ord:

      IPS, KVP, Kvalifiseringsprogram, Kvalifiseringslønn, Kvalifiseringsstønad, Aktivitetsplikt, Angst, Arbeid med støtte, Arbeidsevne, Arbeids- og utdanningsreiser, Arbeidsforberedende trening, Arbeidsmarkedskurs, Arbeidspraksis, Arbeidsrettet rehabilitering, Arbeidstrening, AU-reiser, Avklaringstiltak, Barn, Behandling, Behov, Behovsliste, Booppfølging, Deltaker, Depresjon, Diagnoser, Fastlege, Flyktning, Fravær, Gjeld, Helseavklaring, Husleie, Individuell jobbstøtte, Introduksjonsprogram, Introduksjonsstønad, Jobbklar, Jobbspesialist, Kognitive utfordringer, Kognitive problemer, Kognitivt, Kommunale lavterskeltilbud, Kommunale tiltak, Kommunale tjenester, Koordinert bistand, Lån, Langvarig, Livsopphold, Lønnstilskudd, Mentor, Mentortilskudd, Midlertidig bolig, Midlertidig botilbud, Nedsatt arbeidsevne, Nedsatt funksjon, Norskferdigheter, Oppfølging, Oppfølgingstiltak, Oppfølgning i bolig, Opplæring, Opplæringstiltak, Pengestøtte, Problemer, Psykiatri, Psykolog, Rehabilitering, Restanse, Rus, Sommerjobb, Sommerjobbtiltak, Sosial oppfølging, Sosialfaglig oppfølging, Sosialhjelp, Sosialhjelpsmottaker, Sosialstønad, Sosiale problemer, Sosiale utfordringer, Supplerende stønad, Supported Employment, Syk, Sykdom, Sykemeldt, Sykt barn, Tilskudd, Tiltak, Tiltaksdeltaker, Ukrain, Ukraina, Ungdom, Utfordringer, Utvidet oppfølging, Varig tilrettelagt arbeid, Venteliste, Økonomi, Økonomisk kartlegging, Økonomisk rådgivning, Økonomisk sosialhjelp, Økonomisk stønad

      Unntak:

      Hvis et sensitivt ord brukes i sammenheng med stillingens oppgaver, ansvar eller kvalifikasjoner (for eksempel "Oppfølging av deltakere på kvalifiseringsprogrammet (KVP)"), skal det ikke anses som sensitivt.

      Svarformat:

      Vennligst svar kun med et JSON-objekt på norsk i følgende format, uten ytterligere forklaringer:

      {
        "sensitiv": true/false,
        "sensitivBegrunnelse": "Din begrunnelse her.",
        "samsvarMedTittel": true/false,
        "tittelBegrunnelse": "Din begrunnelse her.",
        "samsvarMedType": true/false,
        "typeBegrunnelse": "Din begrunnelse her."
      }
      samsvarMedTittel: Angi om det er samsvar mellom stillingstittel og stillingstekst.
      samsvarMedType: Angi om det er samsvar mellom stillingstype og stillingstekst.
      
      Merk:

      Jobbmesser og formidlinger har lite krav til innhold i teksten.
      Stillingsstype "stilling" skal være en stillingsannonse som refererer til en vanlig stilling som kan utlyses.
      Stillingstittel: ${stillingsanalyseDto.stillingstittel}

      Stillingstype: ${stillingsanalyseDto.stillingstype}

      Stillingsbeskrivelse: ${stillingsanalyseDto.stillingstekst}
        
        """.trimIndent()
}
