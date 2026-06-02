package school.charset.app.infrastructure.http

// Marks a controller method parameter that should receive the anonymous visitor
// token read from the `token_id` HttpOnly cookie. Resolved by
// TokenIdArgumentResolver (registered in config/WebMvcConfig).
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class TokenId
