package no.nav.rekrutteringsbistand.api.support.featuretoggle

import no.finn.unleash.strategy.Strategy
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class ByClusterStrategy: Strategy {

    @Value("\${nais.cluster-name}")
    lateinit var clusterName: String

    override fun isEnabled(parameters: MutableMap<String, String>): Boolean {
        if (parameters.isEmpty()) return false
        return parameters["cluster"]?.contains(clusterName) ?: false
    }

    override fun getName(): String = "byCluster"
}
