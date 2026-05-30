package school.charset.app.config

import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import school.charset.app.infrastructure.http.TraceIdFilter

@Configuration
class LoggingConfig {
    // Registered at highest precedence so the trace id is in the MDC before
    // any other filter runs (security included), giving every log line of the
    // request - even auth failures - the same correlation id.
    @Bean
    fun traceIdFilter(): FilterRegistrationBean<TraceIdFilter> {
        val registration = FilterRegistrationBean(TraceIdFilter())
        registration.order = Ordered.HIGHEST_PRECEDENCE
        return registration
    }
}
