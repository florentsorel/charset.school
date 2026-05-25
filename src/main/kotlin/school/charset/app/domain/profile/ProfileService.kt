package school.charset.app.domain.profile

import school.charset.app.domain.auth.OrphanedSessionException
import school.charset.app.domain.auth.PasswordHasher
import school.charset.app.domain.user.RawPassword
import school.charset.app.domain.user.User
import school.charset.app.domain.user.UserRepository

class ProfileService(
    private val userRepository: UserRepository,
    private val passwordHasher: PasswordHasher,
) {
    fun changePassword(userId: Long, currentPassword: RawPassword, newPassword: RawPassword): User {
        val user = userRepository.findById(userId) ?: throw OrphanedSessionException(userId)
        if (!passwordHasher.matches(currentPassword, user.passwordHash)) {
            throw CurrentPasswordMismatchException()
        }
        return userRepository.updatePasswordHash(userId, passwordHasher.hash(newPassword))
    }
}
