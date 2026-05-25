package school.charset.app.infrastructure.http.profile

import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import school.charset.app.domain.auth.OrphanedSessionException
import school.charset.app.domain.profile.PasswordConfirmationMismatchException
import school.charset.app.domain.profile.ProfileService
import school.charset.app.domain.user.RawPassword
import school.charset.app.domain.user.User
import school.charset.app.domain.user.UserRepository
import school.charset.app.infrastructure.security.requireUserDetailsAdapter

@RestController
@RequestMapping(
    "/api/profile",
    produces = [MediaType.APPLICATION_JSON_VALUE],
)
class ProfileController(
    private val userRepository: UserRepository,
    private val profileService: ProfileService,
) {
    @PatchMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun update(
        @Valid @RequestBody req: UpdateProfileRequest,
        authentication: Authentication,
    ): ResponseEntity<User> {
        val userId = authentication.requireUserDetailsAdapter().userId

        userRepository.findById(userId) ?: throw OrphanedSessionException(userId)

        val updated = userRepository.update(
            id = userId,
            name = req.name,
            email = req.email,
            locale = req.locale,
        )
        return ResponseEntity.ok(updated)
    }

    @PatchMapping("/password", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun changePassword(
        @Valid @RequestBody req: ChangePasswordRequest,
        authentication: Authentication,
    ): ResponseEntity<Void> {
        if (req.newPassword != req.confirmPassword) throw PasswordConfirmationMismatchException()

        val userId = authentication.requireUserDetailsAdapter().userId
        profileService.changePassword(
            userId = userId,
            currentPassword = RawPassword(req.currentPassword),
            newPassword = RawPassword(req.newPassword),
        )
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }

    @DeleteMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun deleteAccount(
        @Valid @RequestBody req: DeleteAccountRequest,
        authentication: Authentication,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): ResponseEntity<Void> {
        val userId = authentication.requireUserDetailsAdapter().userId
        profileService.deleteAccount(userId, RawPassword(req.password))

        // Mirror what `/api/auth/logout` does: invalidate server-side session,
        // clear the thread-local SecurityContext, and tell the browser to drop
        // SESSION + remember-me cookies. Orphaned persistent_logins rows are
        // harmless (loadUserByUsername returns null on auto-login → rejected).
        request.getSession(false)?.invalidate()
        SecurityContextHolder.clearContext()
        listOf("SESSION", "remember-me").forEach { name ->
            response.addCookie(
                Cookie(name, null).apply {
                    path = "/"
                    maxAge = 0
                },
            )
        }
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }
}
