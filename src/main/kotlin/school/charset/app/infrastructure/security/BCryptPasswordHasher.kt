package school.charset.app.infrastructure.security

import org.springframework.security.crypto.password.PasswordEncoder
import school.charset.app.domain.auth.PasswordHasher
import school.charset.app.domain.user.PasswordHash
import school.charset.app.domain.user.RawPassword

class BCryptPasswordHasher(
    private val encoder: PasswordEncoder,
) : PasswordHasher {
    override fun hash(rawPassword: RawPassword): PasswordHash = PasswordHash(
        encoder.encode(rawPassword.value)
            ?: error("PasswordEncoder.encode returned null for non-null input"),
    )

    override fun matches(rawPassword: RawPassword, hash: PasswordHash): Boolean = encoder.matches(rawPassword.value, hash.value)
}
