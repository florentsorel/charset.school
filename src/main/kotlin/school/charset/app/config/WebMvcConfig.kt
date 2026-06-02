package school.charset.app.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import school.charset.app.infrastructure.http.TokenIdArgumentResolver

// Registers the @TokenId argument resolver so controllers can receive the
// anonymous visitor token straight from the cookie.
@Configuration
class WebMvcConfig : WebMvcConfigurer {
    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(TokenIdArgumentResolver())
    }
}
