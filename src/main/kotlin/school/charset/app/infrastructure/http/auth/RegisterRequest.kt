package school.charset.app.infrastructure.http.auth

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class RegisterRequest(
    @field:NotBlank(message = "auth.validation.email_required")
    @field:Email(message = "auth.validation.email_invalid")
    @field:Size(max = 255, message = "auth.validation.email_too_long")
    val email: String,
    @field:NotBlank(message = "auth.validation.name_required")
    @field:Size(max = 255, message = "auth.validation.name_too_long")
    val name: String,
    @field:NotBlank(message = "auth.validation.password_required")
    @field:Size(min = 8, max = 64, message = "auth.validation.password_size")
    val password: String,
    @field:Pattern(regexp = "^(fr|en)$", message = "auth.validation.locale_invalid")
    val locale: String = "fr",
)
