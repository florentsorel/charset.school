package school.charset.app.infrastructure.repository.user

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp
import school.charset.app.domain.user.PasswordHash
import school.charset.app.domain.user.User

object UsersTable : Table("users") {
    val id = long("id").autoIncrement()
    val email = varchar("email", 255).uniqueIndex()
    val name = varchar("name", 255)
    val passwordHash = varchar("password_hash", 60)
    val locale = varchar("locale", 5)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at").nullable()

    override val primaryKey = PrimaryKey(id)
}

fun ResultRow.toUser(): User = User(
    id = this[UsersTable.id],
    email = this[UsersTable.email],
    name = this[UsersTable.name],
    passwordHash = PasswordHash(this[UsersTable.passwordHash]),
    locale = this[UsersTable.locale],
    createdAt = this[UsersTable.createdAt],
    updatedAt = this[UsersTable.updatedAt],
)
