package school.charset.app.domain.auth

object AuthErrorType {
    const val EMAIL_ALREADY_TAKEN = "auth.email-already-taken"
    const val BAD_CREDENTIALS = "auth.bad-credentials"
    const val SESSION_ORPHANED = "auth.session-orphaned"
}
