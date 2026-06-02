package school.charset.app.infrastructure.http

import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.MethodParameter
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

const val TOKEN_ID_COOKIE = "token_id"

// Resolves a `@TokenId token: String` parameter from the `token_id` cookie.
// The cookie is minted at the Nuxt edge (server middleware), so a request that
// reaches the API without it is a contract violation → MissingTokenIdException
// (we never mint server-side: a single minting authority avoids divergent ids).
class TokenIdArgumentResolver : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean = parameter.hasParameterAnnotation(TokenId::class.java) && parameter.parameterType == String::class.java

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): String {
        val request = webRequest.getNativeRequest(HttpServletRequest::class.java)
            ?: throw MissingTokenIdException()
        val token = request.cookies?.firstOrNull { it.name == TOKEN_ID_COOKIE }?.value
        if (token.isNullOrBlank()) throw MissingTokenIdException()
        return token
    }
}

class MissingTokenIdException : RuntimeException("Missing or blank $TOKEN_ID_COOKIE cookie")
