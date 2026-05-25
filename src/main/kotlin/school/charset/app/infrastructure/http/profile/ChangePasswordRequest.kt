package school.charset.app.infrastructure.http.profile

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ChangePasswordRequest(
    @field:NotBlank(message = "auth.validation.password_required")
    val currentPassword: String,
    @field:NotBlank(message = "auth.validation.password_required")
    @field:Size(min = 8, max = 64, message = "auth.validation.password_size")
    val newPassword: String,
    @field:NotBlank(message = "auth.validation.password_required")
    @field:Size(min = 8, max = 64, message = "auth.validation.password_size")
    val confirmPassword: String,
)
