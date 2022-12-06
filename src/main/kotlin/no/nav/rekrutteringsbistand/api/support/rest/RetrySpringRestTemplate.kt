package no.nav.rekrutteringsbistand.api.support.rest

import io.github.resilience4j.core.IntervalFunction
import io.github.resilience4j.kotlin.retry.executeFunction
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import org.springframework.http.ResponseEntity
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import java.io.IOException
import java.time.Duration

object RetrySpringRestTemplate {
    private fun isIOException(t: Throwable?): Boolean =
        when (t) {
            null -> false
            is IOException -> true
            else -> isIOException(t.cause)
        }

    private val exponentialBackoff = IntervalFunction.ofExponentialBackoff(Duration.ofMillis(500), 3.0)

    private val retryConfig = RetryConfig.custom<ResponseEntity<*>>()
        .maxAttempts(3)
        .intervalFunction(exponentialBackoff)
        .retryOnResult { it.statusCode.is5xxServerError }
        .retryOnException(this::isIOException)
        .retryExceptions(HttpServerErrorException::class.java, ResourceAccessException::class.java)
        .build()

    private val retry: Retry =
        Retry.of("RetrySpringRestTemplate", retryConfig)

    fun <T> retry(block: () -> T): T = retry.executeFunction(block)
}
