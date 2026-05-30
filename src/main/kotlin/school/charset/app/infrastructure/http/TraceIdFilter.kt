package school.charset.app.infrastructure.http

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.web.filter.OncePerRequestFilter
import kotlin.uuid.Uuid

// Puts a short per-request trace id into the SLF4J MDC so every log line of a
// request can be correlated (rendered via the `%X{traceId}` pattern, see
// application.yaml). Reuses an inbound `X-Request-Id` if a proxy already set
// one, otherwise generates a short id. Always cleared in `finally` so the id
// never leaks to a pooled thread's next request.
class TraceIdFilter : OncePerRequestFilter() {
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val traceId = request.getHeader(HEADER)?.takeIf { it.isNotBlank() }
            ?: Uuid.random().toHexString().take(8)
        MDC.put(KEY, traceId)
        response.setHeader(HEADER, traceId)
        try {
            filterChain.doFilter(request, response)
        } finally {
            MDC.remove(KEY)
        }
    }

    private companion object {
        const val HEADER = "X-Request-Id"
        const val KEY = "traceId"
    }
}
