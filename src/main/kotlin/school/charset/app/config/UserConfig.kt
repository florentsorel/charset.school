package school.charset.app.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import school.charset.app.domain.user.UserRepository
import school.charset.app.infrastructure.http.auth.serde.UserSerializer
import school.charset.app.infrastructure.repository.user.ExposedUserRepository
import tools.jackson.databind.JacksonModule
import tools.jackson.databind.module.SimpleModule
import kotlin.time.Clock

@Configuration
class UserConfig {
    @Bean
    fun userRepository(clock: Clock): UserRepository = ExposedUserRepository(clock)

    @Bean
    fun userJacksonModule(): JacksonModule = SimpleModule().apply {
        addSerializer(UserSerializer())
    }
}
