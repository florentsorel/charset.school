package school.charset.app.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.password.PasswordEncoder
import school.charset.app.domain.auth.AuthService
import school.charset.app.domain.auth.PasswordHasher
import school.charset.app.domain.user.UserRepository
import school.charset.app.infrastructure.security.BCryptPasswordHasher

@Configuration
class AuthConfig {
    @Bean
    fun passwordHasher(passwordEncoder: PasswordEncoder): PasswordHasher = BCryptPasswordHasher(passwordEncoder)

    @Bean
    fun authService(userRepository: UserRepository, passwordHasher: PasswordHasher): AuthService = AuthService(userRepository, passwordHasher)
}
