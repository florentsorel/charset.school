package school.charset.app.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import school.charset.app.domain.auth.PasswordHasher
import school.charset.app.domain.profile.ProfileService
import school.charset.app.domain.user.UserRepository

@Configuration
class ProfileConfig {
    @Bean
    fun profileService(
        userRepository: UserRepository,
        passwordHasher: PasswordHasher,
    ): ProfileService = ProfileService(userRepository, passwordHasher)
}
