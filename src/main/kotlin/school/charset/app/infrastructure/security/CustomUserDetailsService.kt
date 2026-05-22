package school.charset.app.infrastructure.security

import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import school.charset.app.domain.user.UserRepository

class CustomUserDetailsService(
    private val userRepository: UserRepository,
) : UserDetailsService {
    override fun loadUserByUsername(username: String): UserDetails = userRepository.findByEmail(username)?.let {
        UserDetailsAdapter(
            userId = it.id,
            email = it.email,
            passwordHashValue = it.passwordHash.value,
        )
    } ?: throw UsernameNotFoundException("User not found: $username")
}
