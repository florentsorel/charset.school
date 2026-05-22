package school.charset.app.config

import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.TimeZone
import kotlin.time.Clock

@Configuration
class ApplicationConfiguration {
    @PostConstruct
    fun setTz() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @Bean
    fun clock(): Clock = Clock.System
}
