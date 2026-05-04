package no.nav.rekrutteringsbistand.api.support.rest

import com.fasterxml.jackson.annotation.JsonInclude
import org.slf4j.MDC
import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail

@JsonInclude(JsonInclude.Include.NON_NULL)
class TraceableProblemDetail : ProblemDetail {
    var traceId: String? = null

    private constructor(status: HttpStatusCode) : super(status.value()) {
        traceId = MDC.get("trace_id")?.takeIf { it.isNotBlank() }
    }

    private constructor(other: ProblemDetail) : super(other) {
        traceId = MDC.get("trace_id")?.takeIf { it.isNotBlank() }
    }

    companion object {
        fun forStatus(status: HttpStatusCode): TraceableProblemDetail =
            TraceableProblemDetail(status)

        fun forStatusAndDetail(status: HttpStatusCode, detail: String): TraceableProblemDetail =
            TraceableProblemDetail(status).apply { this.detail = detail }

        fun from(other: ProblemDetail): TraceableProblemDetail =
            TraceableProblemDetail(other)
    }
}