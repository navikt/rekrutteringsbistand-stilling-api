package no.nav.rekrutteringsbistand.api.geografi

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit.WireMockRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class GeografiKomponentTest {

    @get:Rule
    val wiremock = WireMockRule(WireMockConfiguration.options().port(9914))

    @LocalServerPort
    private var port = 0

    val localBaseUrl by lazy { "http://localhost:$port" }

    private val restTemplate = TestRestTemplate(TestRestTemplate.HttpClientOption.ENABLE_COOKIES)

    @Before
    fun authenticateClient() {
        restTemplate.getForObject("$localBaseUrl/veileder-token-cookie", Unit::class.java)
    }

    @Test
    fun `GET mot counties skal returnere HTTP 200 med fylker`() {
        mockString("/api/v1/geography/counties", fylkeResponsBody)
        restTemplate.getForEntity("$localBaseUrl/rekrutteringsbistand/api/v1/geography/counties", String::class.java).also {
            assertThat(it.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(it.body).isEqualTo(fylkeResponsBody)
        }
    }

    @Test
    fun `GET mot countries skal returnere HTTP 200 med land`() {
        mockString("/api/v1/geography/countries", landResponsBody)
        restTemplate.getForEntity("$localBaseUrl/rekrutteringsbistand/api/v1/geography/countries", String::class.java).also {
            assertThat(it.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(it.body).isEqualTo(landResponsBody)
        }
    }

    @Test
    fun `GET mot municipals skal returnere HTTP 200 med kommuner`() {
        mockString("/api/v1/geography/municipals", kommunerJson)
        restTemplate.getForEntity("$localBaseUrl/rekrutteringsbistand/api/v1/geography/municipals", String::class.java).also {
            assertThat(it.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(it.body).isEqualTo(kommunerJson)
        }
    }

    @Test
    fun `GET mot categories-with-altnames skal returnere HTTP 200 med STYRK-kategorier`() {
        mockString("/api/v1/categories-with-altnames", styrkkoderJson)
        restTemplate.getForEntity("$localBaseUrl/rekrutteringsbistand/api/v1/categories-with-altnames", String::class.java).also {
            assertThat(it.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(it.body).isEqualTo(styrkkoderJson)
        }
    }

    @Test
    fun `GET mot postdata skal returnere HTTP 200 med informasjon om postnumre`() {
        mockString("/api/v1/postdata", postnumreJson);
        restTemplate.getForEntity("$localBaseUrl/rekrutteringsbistand/api/v1/postdata", String::class.java).also {
            assertThat(it.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(it.body).isEqualTo(postnumreJson)
        }
    }


    private fun mockString(urlPath: String, responseBody: String) {
        wiremock.stubFor(
                WireMock.get(WireMock.urlPathMatching(urlPath))
                        .withHeader(HttpHeaders.CONTENT_TYPE, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
                        .withHeader(HttpHeaders.ACCEPT, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
                        .withHeader(HttpHeaders.AUTHORIZATION, WireMock.matching("Bearer .*"))
                        .willReturn(WireMock.aResponse().withStatus(200)
                                .withHeader(HttpHeaders.CONNECTION, "close") // https://stackoverflow.com/questions/55624675/how-to-fix-nohttpresponseexception-when-running-wiremock-on-jenkins
                                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .withBody(responseBody))
        )
    }

    private val landResponsBody = """
        [{"code":"AD","name":"ANDORRA"},{"code":"AE","name":"DE FORENTE ARABISKE EMIRATER"},{"code":"AF","name":"AFGHANISTAN"},{"code":"AG","name":"ANTIGUA OG BARBUDA"},{"code":"AI","name":"ANGUILLA"},{"code":"AL","name":"ALBANIA"},{"code":"AM","name":"ARMENIA"},{"code":"AO","name":"ANGOLA"},{"code":"AR","name":"ARGENTINA"},{"code":"AS","name":"AMERIKANSK SAMOA"},{"code":"AT","name":"ØSTERRIKE"},{"code":"AU","name":"AUSTRALIA"},{"code":"AW","name":"ARUBA"},{"code":"AX","name":"ÅLAND"},{"code":"AZ","name":"ASERBAJDSJAN"},{"code":"BA","name":"BOSNIA-HERCEGOVINA"},{"code":"BB","name":"BARBADOS"},{"code":"BD","name":"BANGLADESH"},{"code":"BE","name":"BELGIA"},{"code":"BF","name":"BURKINA FASO"},{"code":"BG","name":"BULGARIA"},{"code":"BH","name":"BAHRAIN"},{"code":"BI","name":"BURUNDI"},{"code":"BJ","name":"BENIN"},{"code":"BL","name":"SAINT BARTHELEMY"},{"code":"BM","name":"BERMUDA"},{"code":"BN","name":"BRUNEI DARUSSALAM"},{"code":"BO","name":"BOLIVIA"},{"code":"BQ","name":"BONAIRE, SAINT EUSTATIUS OG SABA"},{"code":"BR","name":"BRASIL"},{"code":"BS","name":"BAHAMAS"},{"code":"BT","name":"BHUTAN"},{"code":"BW","name":"BOTSWANA"},{"code":"BY","name":"HVITERUSSLAND"},{"code":"BZ","name":"BELIZE"},{"code":"CA","name":"CANADA"},{"code":"CC","name":"KOKOSØYENE (KEELINGØYENE)"},{"code":"CD","name":"KONGO"},{"code":"CF","name":"SENTRALAFRIKANSKE REPUBLIKK"},{"code":"CG","name":"KONGO, BRAZZAVILLE"},{"code":"CH","name":"SVEITS"},{"code":"CI","name":"ELFENBENSKYSTEN"},{"code":"CK","name":"COOKØYENE"},{"code":"CL","name":"CHILE"},{"code":"CM","name":"KAMERUN"},{"code":"CN","name":"KINA"},{"code":"CO","name":"COLOMBIA"},{"code":"CR","name":"COSTA RICA"},{"code":"CU","name":"CUBA"},{"code":"CV","name":"KAPP VERDE"},{"code":"CW","name":"CURACAO"},{"code":"CX","name":"CHRISTMASØYA"},{"code":"CY","name":"KYPROS"},{"code":"CZ","name":"TSJEKKIA"},{"code":"DE","name":"TYSKLAND"},{"code":"DJ","name":"DJIBOUTI"},{"code":"DK","name":"DANMARK"},{"code":"DM","name":"DOMINICA"},{"code":"DO","name":"DEN DOMINIKANSKE REPUBLIKK"},{"code":"DZ","name":"ALGERIE"},{"code":"EC","name":"ECUADOR"},{"code":"EE","name":"ESTLAND"},{"code":"EG","name":"EGYPT"},{"code":"EH","name":"VEST-SAHARA"},{"code":"ER","name":"ERITREA"},{"code":"ES","name":"SPANIA"},{"code":"ET","name":"ETIOPIA"},{"code":"FI","name":"FINLAND"},{"code":"FJ","name":"FIJI"},{"code":"FK","name":"FALKLANDSØYENE (MALVINAS)"},{"code":"FM","name":"MIKRONESIAFØDERASJONEN"},{"code":"FO","name":"FÆRØYENE"},{"code":"FR","name":"FRANKRIKE"},{"code":"GA","name":"GABON"},{"code":"GB","name":"STORBRITANNIA"},{"code":"GD","name":"GRENADA"},{"code":"GE","name":"GEORGIA"},{"code":"GF","name":"FRANSK GUYANA"},{"code":"GG","name":"GUERNSEY"},{"code":"GH","name":"GHANA"},{"code":"GI","name":"GIBRALTAR"},{"code":"GL","name":"GRØNLAND"},{"code":"GM","name":"GAMBIA"},{"code":"GN","name":"GUINEA"},{"code":"GP","name":"GUADELOUPE"},{"code":"GQ","name":"EKVATORIAL-GUINEA"},{"code":"GR","name":"HELLAS"},{"code":"GS","name":"SØR-GEORGIA/SØNDRE SANDWICHØYENE"},{"code":"GT","name":"GUATEMALA"},{"code":"GU","name":"GUAM"},{"code":"GW","name":"GUINEA-BISSAU"},{"code":"GY","name":"GUYANA"},{"code":"HK","name":"HONG KONG"},{"code":"HM","name":"HEARD- OG MCDONALDØYENE"},{"code":"HN","name":"HONDURAS"},{"code":"HR","name":"KROATIA"},{"code":"HT","name":"HAITI"},{"code":"HU","name":"UNGARN"},{"code":"ID","name":"INDONESIA"},{"code":"IE","name":"IRLAND"},{"code":"IL","name":"ISRAEL"},{"code":"IM","name":"ISLE OF MAN"},{"code":"IN","name":"INDIA"},{"code":"IO","name":"DET BRITISKE TERR. I INDIAHAVET"},{"code":"IQ","name":"IRAK"},{"code":"IR","name":"IRAN"},{"code":"IS","name":"ISLAND"},{"code":"IT","name":"ITALIA"},{"code":"JE","name":"JERSEY"},{"code":"JM","name":"JAMAICA"},{"code":"JO","name":"JORDAN"},{"code":"JP","name":"JAPAN"},{"code":"KE","name":"KENYA"},{"code":"KG","name":"KIRGISISTAN"},{"code":"KH","name":"KAMBODSJA"},{"code":"KI","name":"KIRIBATI"},{"code":"KM","name":"KOMORENE"},{"code":"KN","name":"SAINT KITTS OG NEVIS"},{"code":"KP","name":"NORD-KOREA"},{"code":"KR","name":"SØR-KOREA"},{"code":"KW","name":"KUWAIT"},{"code":"KY","name":"CAYMANØYENE"},{"code":"KZ","name":"KASAKHSTAN"},{"code":"LA","name":"LAOS"},{"code":"LB","name":"LIBANON"},{"code":"LC","name":"SAINT LUCIA"},{"code":"LI","name":"LIECHTENSTEIN"},{"code":"LK","name":"SRI LANKA"},{"code":"LR","name":"LIBERIA"},{"code":"LS","name":"LESOTHO"},{"code":"LT","name":"LITAUEN"},{"code":"LU","name":"LUXEMBURG"},{"code":"LV","name":"LATVIA"},{"code":"LY","name":"LIBYA"},{"code":"MA","name":"MAROKKO"},{"code":"MC","name":"MONACO"},{"code":"MD","name":"MOLDOVA"},{"code":"ME","name":"MONTENEGRO"},{"code":"MF","name":"SAINT MARTIN, FR"},{"code":"MG","name":"MADAGASKAR"},{"code":"MH","name":"MARSHALLØYENE"},{"code":"MK","name":"MAKEDONIA"},{"code":"ML","name":"MALI"},{"code":"MM","name":"MYANMAR/BURMA"},{"code":"MN","name":"MONGOLIA"},{"code":"MO","name":"MACAO"},{"code":"MP","name":"NORDRE MARIANENE"},{"code":"MQ","name":"MARTINIQUE"},{"code":"MR","name":"MAURITANIA"},{"code":"MS","name":"MONTSERRAT"},{"code":"MT","name":"MALTA"},{"code":"MU","name":"MAURITIUS"},{"code":"MV","name":"MALDIVENE"},{"code":"MW","name":"MALAWI"},{"code":"MX","name":"MEXICO"},{"code":"MY","name":"MALAYSIA"},{"code":"MZ","name":"MOSAMBIK"},{"code":"NA","name":"NAMIBIA"},{"code":"NC","name":"NY CALEDONIA"},{"code":"NE","name":"NIGER"},{"code":"NF","name":"NORFOLKØYA"},{"code":"NG","name":"NIGERIA"},{"code":"NI","name":"NICARAGUA"},{"code":"NL","name":"NEDERLAND"},{"code":"NO","name":"NORGE"},{"code":"NP","name":"NEPAL"},{"code":"NR","name":"NAURU"},{"code":"NU","name":"NIUE"},{"code":"NZ","name":"NEW ZEALAND"},{"code":"OM","name":"OMAN"},{"code":"PA","name":"PANAMA"},{"code":"PE","name":"PERU"},{"code":"PF","name":"FRANSK POLYNESIA"},{"code":"PG","name":"PAPUA NY-GUINEA"},{"code":"PH","name":"FILIPPINENE"},{"code":"PK","name":"PAKISTAN"},{"code":"PL","name":"POLEN"},{"code":"PM","name":"SAINT PIERRE OG MIQUELON"},{"code":"PN","name":"PITCAIRN"},{"code":"PR","name":"PUERTO RICO"},{"code":"PS","name":"PALESTINA"},{"code":"PT","name":"PORTUGAL"},{"code":"PW","name":"PALAU"},{"code":"PY","name":"PARAGUAY"},{"code":"QA","name":"QATAR"},{"code":"RE","name":"REUNION"},{"code":"RO","name":"ROMANIA"},{"code":"RS","name":"SERBIA"},{"code":"RU","name":"RUSSLAND"},{"code":"RW","name":"RWANDA"},{"code":"SA","name":"SAUDI-ARABIA"},{"code":"SB","name":"SALOMONØYENE"},{"code":"SC","name":"SEYCHELLENE"},{"code":"SD","name":"SUDAN"},{"code":"SE","name":"SVERIGE"},{"code":"SG","name":"SINGAPORE"},{"code":"SH","name":"SANKT HELENA"},{"code":"SI","name":"SLOVENIA"},{"code":"SK","name":"SLOVAKIA"},{"code":"SL","name":"SIERRA LEONE"},{"code":"SM","name":"SAN MARINO"},{"code":"SN","name":"SENEGAL"},{"code":"SO","name":"SOMALIA"},{"code":"SR","name":"SURINAM"},{"code":"SS","name":"SØR-SUDAN"},{"code":"ST","name":"SAO TOME OG PRINCIPE"},{"code":"SV","name":"EL SALVADOR"},{"code":"SX","name":"SINT MARTEEN (NEDERLANDSK DEL)"},{"code":"SY","name":"SYRIA"},{"code":"SZ","name":"SWAZILAND"},{"code":"TC","name":"TURKS OG CAICOSØYENE"},{"code":"TD","name":"TSJAD"},{"code":"TF","name":"FRANSKE SØRLIGE TERRITORIER"},{"code":"TG","name":"TOGO"},{"code":"TH","name":"THAILAND"},{"code":"TJ","name":"TADSJIKISTAN"},{"code":"TK","name":"TOKELAU"},{"code":"TL","name":"ØST-TIMOR"},{"code":"TM","name":"TURKMENISTAN"},{"code":"TN","name":"TUNISIA"},{"code":"TO","name":"TONGA"},{"code":"TR","name":"TYRKIA"},{"code":"TT","name":"TRINIDAD OG TOBAGO"},{"code":"TV","name":"TUVALU"},{"code":"TW","name":"TAIWAN"},{"code":"TZ","name":"TANZANIA"},{"code":"UA","name":"UKRAINA"},{"code":"UG","name":"UGANDA"},{"code":"UM","name":"USA MINDRE UTENFORLIGGENDE ØYER"},{"code":"US","name":"USA"},{"code":"UY","name":"URUGUAY"},{"code":"UZ","name":"USBEKISTAN"},{"code":"VA","name":"VATIKANSTATEN"},{"code":"VC","name":"SAINT VINCENT OG GRENADINE"},{"code":"VE","name":"VENEZUELA"},{"code":"VG","name":"JOMFRUØYENE, BRITISK"},{"code":"VI","name":"JOMFRUØYENE, US"},{"code":"VN","name":"VIETNAM"},{"code":"VU","name":"VANUATU"},{"code":"WF","name":"WALLIS OG FUTUNA"},{"code":"WS","name":"SAMOA"},{"code":"XB","name":"KANARIØYENE"},{"code":"XC","name":"CEUTA OG MELILLA"},{"code":"XK","name":"KOSOVO"},{"code":"YE","name":"JEMEN"},{"code":"YT","name":"MAYOTTE"},{"code":"ZA","name":"SØR-AFRIKA"},{"code":"ZM","name":"ZAMBIA"},{"code":"ZW","name":"ZIMBABWE"}]
        """.trimIndent()

    private val fylkeResponsBody = """
       [{"code":"02","name":"AKERSHUS"},{"code":"03","name":"OSLO"},{"code":"09","name":"AUST-AGDER"},{"code":"06","name":"BUSKERUD"},{"code":"08","name":"TELEMARK"},{"code":"23","name":"KONTINENTALSOKKELEN"},{"code":"05","name":"OPPLAND"},{"code":"15","name":"MØRE OG ROMSDAL"},{"code":"11","name":"ROGALAND"},{"code":"12","name":"HORDALAND"},{"code":"18","name":"NORDLAND"},{"code":"20","name":"FINNMARK"},{"code":"14","name":"SOGN OG FJORDANE"},{"code":"21","name":"SVALBARD"},{"code":"04","name":"HEDMARK"},{"code":"50","name":"TRØNDELAG"},{"code":"22","name":"JAN MAYEN"},{"code":"19","name":"TROMS"},{"code":"10","name":"VEST-AGDER"},{"code":"07","name":"VESTFOLD"},{"code":"01","name":"ØSTFOLD"}]
       """.trimIndent()

    private val kommunerJson = """
            [
                {
                    "code": "1818",
                    "name": "HERØY (NORDLAND)",
                    "countyCode": "18"
                },
                {
                    "code": "1903",
                    "name": "HARSTAD",
                    "countyCode": "19"
                }
            ]
        """.trimIndent()

    private val styrkkoderJson = """
            [
                {
                    "id": 393,
                    "code": "1311.21",
                    "categoryType": "STYRK08NAV",
                    "name": "Fylkesgartner",
                    "description": null,
                    "parentId": 372,
                    "alternativeNames": []
                }
            ]
        """.trimIndent()

    private val postnumreJson = """
            [
                {
                    "postalCode": "4971",
                    "city": "SUNDEBRU",
                    "municipality": {
                        "code": "0911",
                        "name": "GJERSTAD",
                        "countyCode": "09"
                    },
                    "county": {
                        "code": "09",
                        "name": "AUST-AGDER"
                    }
                }
            ]
        """.trimIndent()
}
