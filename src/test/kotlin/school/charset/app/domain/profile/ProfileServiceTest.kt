package school.charset.app.domain.profile

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import school.charset.app.domain.auth.OrphanedSessionException
import school.charset.app.domain.auth.PasswordHasher
import school.charset.app.domain.user.PasswordHash
import school.charset.app.domain.user.RawPassword
import school.charset.app.domain.user.User
import school.charset.app.domain.user.UserRepository
import kotlin.time.Instant

class ProfileServiceTest :
    FunSpec({
        val userRepository = mockk<UserRepository>()
        val passwordHasher = mockk<PasswordHasher>()
        val service = ProfileService(userRepository, passwordHasher)

        val existingUser = User(
            id = 42,
            email = "user@example.com",
            name = "User",
            passwordHash = PasswordHash("stored-hash"),
            locale = "fr",
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
            updatedAt = null,
        )

        // FunSpec tests share state — reset mock call history between each test
        // so `verify(exactly = 0)` only sees calls from the current test.
        beforeTest { clearMocks(userRepository, passwordHasher) }

        test("changePassword hashes the new password and persists it on success") {
            val current = RawPassword("current")
            val new = RawPassword("new-password")
            every { userRepository.findById(42) } returns existingUser
            every { passwordHasher.matches(current, existingUser.passwordHash) } returns true
            every { passwordHasher.hash(new) } returns PasswordHash("new-hash")
            every { userRepository.updatePasswordHash(42, PasswordHash("new-hash")) } returns
                existingUser.copy(passwordHash = PasswordHash("new-hash"))

            val result = service.changePassword(42, current, new)

            result.passwordHash shouldBe PasswordHash("new-hash")
            verify { userRepository.updatePasswordHash(42, PasswordHash("new-hash")) }
        }

        test("changePassword throws CurrentPasswordMismatchException when current password is wrong") {
            val current = RawPassword("wrong")
            val new = RawPassword("new-password")
            every { userRepository.findById(42) } returns existingUser
            every { passwordHasher.matches(current, existingUser.passwordHash) } returns false

            shouldThrow<CurrentPasswordMismatchException> {
                service.changePassword(42, current, new)
            }
            verify(exactly = 0) { userRepository.updatePasswordHash(any(), any()) }
            verify(exactly = 0) { passwordHasher.hash(any()) }
        }

        test("changePassword throws OrphanedSessionException when user no longer exists") {
            every { userRepository.findById(42) } returns null

            shouldThrow<OrphanedSessionException> {
                service.changePassword(42, RawPassword("x"), RawPassword("new-password"))
            }
            verify(exactly = 0) { passwordHasher.matches(any(), any()) }
        }
    })
