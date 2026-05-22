package school.charset.app.infrastructure.http.auth

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class RegisterRequest(
    @field:NotBlank
    @field:Email
    @field:Size(max = 255)
    val email: String,
    @field:NotBlank
    @field:Size(max = 255)
    val name: String,
    @field:NotBlank
    @field:Size(min = 8, max = 255)
    val password: String,
    @field:Pattern(regexp = "^(fr|en)$")
    val locale: String = "fr",
)
