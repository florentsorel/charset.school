package school.charset.app.domain.profile

// `field` lets the caller declare which form input should surface the error.
// Defaults to "currentPassword" (used by the change-password form). The delete
// flow throws with field = "password" because the danger-zone modal exposes a
// single generic password input.
class CurrentPasswordMismatchException(
    val field: String = "currentPassword",
) : RuntimeException("Current password does not match the stored hash")
