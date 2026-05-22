package school.charset.app.infrastructure.repository.user

import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.postgresql.util.PSQLState
import school.charset.app.domain.user.EmailAlreadyTakenException
import school.charset.app.domain.user.PasswordHash
import school.charset.app.domain.user.User
import school.charset.app.domain.user.UserRepository
import kotlin.time.Clock

class ExposedUserRepository(
    private val clock: Clock,
) : UserRepository {
    override fun findById(id: Long): User? = transaction {
        UsersTable
            .selectAll()
            .where { UsersTable.id eq id }
            .singleOrNull()
            ?.toUser()
    }

    override fun findByEmail(email: String): User? = transaction {
        UsersTable
            .selectAll()
            .where { UsersTable.email eq email }
            .singleOrNull()
            ?.toUser()
    }

    override fun create(email: String, name: String, passwordHash: PasswordHash, locale: String): User = transaction {
        try {
            val now = clock.now()
            val insertedId = UsersTable.insert {
                it[UsersTable.email] = email
                it[UsersTable.name] = name
                it[UsersTable.passwordHash] = passwordHash.value
                it[UsersTable.locale] = locale
                it[UsersTable.createdAt] = now
            } get UsersTable.id
            User(
                id = insertedId,
                email = email,
                name = name,
                passwordHash = passwordHash,
                locale = locale,
                createdAt = now,
                updatedAt = null,
            )
        } catch (e: ExposedSQLException) {
            if (e.sqlState == PSQLState.UNIQUE_VIOLATION.state) {
                throw EmailAlreadyTakenException(email)
            }
            throw e
        }
    }
}
