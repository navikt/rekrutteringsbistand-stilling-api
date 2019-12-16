package no.nav.rekrutteringsbistand.api.support

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.util.CollectionUtils
import org.springframework.util.MultiValueMap
import javax.servlet.http.HttpServletResponse

val Any.LOG: Logger
    get() = LoggerFactory.getLogger(this::class.java)

fun Map<String, String>.toMultiValueMap(): MultiValueMap<String, String> =
        CollectionUtils.toMultiValueMap(this.mapValues { listOf(it.value) })

fun HttpServletResponse.withAddedHeaders(pairs: Map<String, String>): HttpServletResponse =
        this.apply { pairs.forEach { this.setHeader(it.key, it.value) } }

