package no.nav.rekrutteringsbistand.api

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.util.CollectionUtils
import javax.servlet.http.HttpServletResponse

val Any.LOG: Logger
    get() = LoggerFactory.getLogger(this::class.java)

fun Map<String, String>.toMultiValueMap() =
        CollectionUtils.toMultiValueMap(this.mapValues { listOf(it.value) })

fun HttpServletResponse.withAddedHeaders(pairs: Map<String, String>): HttpServletResponse =
        this.apply { pairs.forEach { this.setHeader(it.key, it.value) } }

