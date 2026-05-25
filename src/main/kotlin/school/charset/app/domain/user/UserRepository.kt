package school.charset.app.domain.user

interface UserRepository {
    fun findById(id: Long): User?

    fun findByEmail(email: String): User?

    fun create(email: String, name: String, passwordHash: PasswordHash, locale: String): User

    fun update(id: Long, name: String? = null, email: String? = null, locale: String? = null): User

    fun updatePasswordHash(id: Long, passwordHash: PasswordHash): User

    fun deleteById(id: Long)
}
