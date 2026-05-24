package school.charset.app.infrastructure.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.web.filter.OncePerRequestFilter

// Spring Security 6's CsrfFilter only writes the XSRF-TOKEN cookie when the
// token is actually accessed (lazy save). The CsrfTokenRequestAttributeHandler
// merely registers a Supplier; the cookie isn't persisted until `.getToken()`
// is called — which in a SPA pattern only happens on the first mutating
// request's validation, by which time the client never had the cookie to echo
// back, causing a 403 on that first mutation.
//
// This filter eagerly accesses the token on every request so the XSRF-TOKEN
// cookie is always set on the response.
class CsrfCookieFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val csrfToken = request.getAttribute(CsrfToken::class.java.name) as? CsrfToken
        csrfToken?.token
        filterChain.doFilter(request, response)
    }
}
