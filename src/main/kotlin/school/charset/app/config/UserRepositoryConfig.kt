package school.charset.app.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import school.charset.app.domain.user.UserRepository
import school.charset.app.infrastructure.repository.user.ExposedUserRepository
import kotlin.time.Clock

@Configuration
class UserRepositoryConfig {
    @Bean
    fun userRepository(clock: Clock): UserRepository = ExposedUserRepository(clock)
}
