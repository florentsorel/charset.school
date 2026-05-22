package school.charset.app.config

import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.TimeZone
import kotlin.time.Clock
import kotlin.time.Instant

@Configuration
class ApplicationConfigTest {
    @PostConstruct
    fun setTz() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @Bean
    fun clock(): Clock = object : Clock {
        override fun now(): Instant = FIXED_NOW
    }

    companion object {
        val FIXED_NOW: Instant = Instant.parse("2017-09-24T00:00:00Z")
    }
}
