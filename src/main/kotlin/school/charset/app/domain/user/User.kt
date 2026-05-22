package school.charset.app.domain.user

import kotlin.time.Instant

data class User(
    val id: Long,
    val email: String,
    val name: String,
    val passwordHash: PasswordHash,
    val locale: String,
    val createdAt: Instant,
    val updatedAt: Instant?,
)
