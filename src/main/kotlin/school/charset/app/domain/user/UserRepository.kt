package school.charset.app.domain.user

interface UserRepository {
    fun findById(id: Long): User?

    fun findByEmail(email: String): User?

    fun create(email: String, name: String, passwordHash: PasswordHash, locale: String): User
}
