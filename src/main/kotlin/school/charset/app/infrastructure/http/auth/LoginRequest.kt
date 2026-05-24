package school.charset.app.infrastructure.http.auth

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class LoginRequest(
    @field:NotBlank(message = "auth.validation.email_required")
    @field:Email(message = "auth.validation.email_invalid")
    val email: String,
    @field:NotBlank(message = "auth.validation.password_required")
    val password: String,
    val rememberMe: Boolean = false,
)
