package school.charset.app.infrastructure.http.profile

import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import school.charset.app.domain.auth.OrphanedSessionException
import school.charset.app.domain.user.User
import school.charset.app.domain.user.UserRepository
import school.charset.app.infrastructure.security.UserDetailsAdapter

@RestController
@RequestMapping(
    "/api/profile",
    produces = [MediaType.APPLICATION_JSON_VALUE],
)
class ProfileController(
    private val userRepository: UserRepository,
) {
    @PatchMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun update(
        @Valid @RequestBody req: UpdateProfileRequest,
        authentication: Authentication,
    ): ResponseEntity<User> {
        val userId = (authentication.principal as? UserDetailsAdapter)?.userId
            ?: error("Expected UserDetailsAdapter principal but got ${authentication.principal?.let { it::class.qualifiedName } ?: "null"}")

        userRepository.findById(userId) ?: throw OrphanedSessionException(userId)

        val updated = userRepository.update(
            id = userId,
            name = req.name,
            email = req.email,
            locale = req.locale,
        )
        return ResponseEntity.ok(updated)
    }
}
