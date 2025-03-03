package no.nav.rekrutteringsbistand.api.support.config

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.InetAddress
import java.net.URI
import java.time.LocalDateTime

@Service
class LeaderElection(@Value("\${ELECTOR_PATH:NOLEADERELECTION}") val electorPath: String) {
    private val hostname = InetAddress.getLocalHost().hostName
    private var leader =  ""
    private var lastCalled = LocalDateTime.MIN
    private val electorUri = "http://"+electorPath

    companion object {
        private val jacksonMapper = jacksonObjectMapper()
            .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .registerModule(JavaTimeModule())
    }

    fun isLeader(): Boolean {
        return hostname == getLeader()
    }

    fun getLeader(): String {
        if (electorPath == "NOLEADERELECTION") return hostname
        if (leader.isBlank() || lastCalled.isBefore(LocalDateTime.now().minusMinutes(2))) {
            leader = jacksonMapper.readValue(URI(electorUri).toURL().readText(), Elector::class.java).name
            lastCalled = LocalDateTime.now()
        }
        return leader
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class Elector(val name: String)
