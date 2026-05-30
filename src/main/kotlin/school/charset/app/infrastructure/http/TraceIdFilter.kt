package school.charset.app.infrastructure.http

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.web.filter.OncePerRequestFilter
import kotlin.uuid.Uuid

// Puts a short per-request trace id into the SLF4J MDC so every log line of a
// request can be correlated (rendered via the `%X{traceId}` pattern, see
// application.yaml). Reuses an inbound `X-Request-Id` ONLY if it's a short,
// safe token - otherwise generates one. Validating the inbound value prevents
// log forging / control characters / oversized values from a client. Always
// cleared in `finally` so the id never leaks to a pooled thread's next request.
class TraceIdFilter : OncePerRequestFilter() {
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val traceId = request.getHeader(HEADER)?.takeIf { SAFE_ID.matches(it) }
            ?: Uuid.random().toHexString().take(8)
        MDC.put(KEY, traceId)
        response.setHeader(HEADER, traceId)
        try {
            filterChain.doFilter(request, response)
        } finally {
            MDC.remove(KEY)
        }
    }

    // Also run on the container's ERROR dispatch so logs emitted while handling
    // an error still carry the trace id (default skips error dispatches).
    override fun shouldNotFilterErrorDispatch(): Boolean = false

    private companion object {
        const val HEADER = "X-Request-Id"
        const val KEY = "traceId"

        // Accept only a short alphanumeric/dash token; anything else is replaced
        // by a generated id (log-forging / control-char defense).
        val SAFE_ID = Regex("^[A-Za-z0-9-]{1,64}$")
    }
}
