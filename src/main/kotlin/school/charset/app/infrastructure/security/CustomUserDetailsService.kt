package school.charset.app.infrastructure.security

import org.slf4j.LoggerFactory
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import school.charset.app.domain.user.UserRepository

class CustomUserDetailsService(
    private val userRepository: UserRepository,
) : UserDetailsService {
    private val logger = LoggerFactory.getLogger(CustomUserDetailsService::class.java)

    override fun loadUserByUsername(username: String): UserDetails {
        // DEBUG (not WARN): a missing user during login already surfaces as a
        // BadCredentials WARN in GlobalExceptionHandler - avoids double-logging.
        return userRepository.findByEmail(username)?.let {
            UserDetailsAdapter(
                userId = it.id,
                email = it.email,
                passwordHashValue = it.passwordHash.value,
            )
        } ?: run {
            logger.debug("User lookup failed (email={})", username)
            throw UsernameNotFoundException("User not found: $username")
        }
    }
}
