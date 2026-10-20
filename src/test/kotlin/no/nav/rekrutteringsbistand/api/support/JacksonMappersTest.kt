package no.nav.rekrutteringsbistand.api.support

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class JacksonMappersTest {

    @Autowired
    lateinit var springJsonMapper: JsonMapper

    data class MedCollections(val liste: List<String>, val map: Map<String, String>)

    private val jsonMedNull = """{"liste": ["a", null], "map": {"a": "b", "c": null}}"""

    @Test
    fun `Spring sin JsonMapper skal tåle null i collections slik som Jackson 2`() {
        val resultat = springJsonMapper.readValue<MedCollections>(jsonMedNull)

        assertThat(resultat.liste).containsExactly("a", null)
        assertThat(resultat.map).containsEntry("c", null)
    }

    data class MedÆøå(val ærlig: String, val øre: String, val åpen: String)

    @Test
    fun `Spring sin JsonMapper skal ta med felt som starter med æøå`() {
        val json = springJsonMapper.writeValueAsString(MedÆøå("a", "b", "c"))

        assertThat(json).isEqualTo("""{"ærlig":"a","øre":"b","åpen":"c"}""")
        assertThat(springJsonMapper.readValue<MedÆøå>(json)).isEqualTo(MedÆøå("a", "b", "c"))
    }

    @Test
    fun `JacksonMappers standard skal tåle null i collections slik som Jackson 2`() {
        val resultat = JacksonMappers.standard.readValue<MedCollections>(jsonMedNull)

        assertThat(resultat.liste).containsExactly("a", null)
        assertThat(resultat.map).containsEntry("c", null)
    }
}
