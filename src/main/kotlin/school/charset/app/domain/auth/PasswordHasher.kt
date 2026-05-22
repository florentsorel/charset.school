package school.charset.app.domain.auth

import school.charset.app.domain.user.PasswordHash
import school.charset.app.domain.user.RawPassword

interface PasswordHasher {
    fun hash(rawPassword: RawPassword): PasswordHash

    fun matches(rawPassword: RawPassword, hash: PasswordHash): Boolean
}
