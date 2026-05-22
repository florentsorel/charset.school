package school.charset.app.domain.auth

import school.charset.app.domain.user.RawPassword
import school.charset.app.domain.user.User
import school.charset.app.domain.user.UserRepository

class AuthService(
    private val userRepository: UserRepository,
    private val passwordHasher: PasswordHasher,
) {
    fun register(email: String, name: String, rawPassword: RawPassword, locale: String): User = userRepository.create(
        email = email,
        name = name,
        passwordHash = passwordHasher.hash(rawPassword),
        locale = locale,
    )
}
