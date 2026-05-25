package school.charset.app.infrastructure.http.profile

import jakarta.validation.constraints.NotBlank

data class DeleteAccountRequest(
    @field:NotBlank(message = "auth.validation.password_required")
    val password: String,
)
