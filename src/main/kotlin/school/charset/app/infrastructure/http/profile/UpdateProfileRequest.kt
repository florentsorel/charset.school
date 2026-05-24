package school.charset.app.infrastructure.http.profile

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

// PATCH semantics — all fields optional. Constraints fire when the field is
// present in the payload. Validation messages are i18n keys (see GlobalExceptionHandler).
//
// `@Size(min=1)` accepts null (skip) but rejects empty — we can't use @NotBlank
// here because null is a legitimate "don't touch" value for PATCH.
data class UpdateProfileRequest(
    @field:Size(min = 1, max = 255, message = "auth.validation.name_size")
    val name: String? = null,
    @field:Email(message = "auth.validation.email_invalid")
    @field:Size(min = 1, max = 255, message = "auth.validation.email_size")
    val email: String? = null,
    @field:Pattern(regexp = "^(fr|en)$", message = "auth.validation.locale_invalid")
    val locale: String? = null,
)
