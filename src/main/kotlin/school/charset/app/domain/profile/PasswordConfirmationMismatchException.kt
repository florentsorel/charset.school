package school.charset.app.domain.profile

class PasswordConfirmationMismatchException : RuntimeException("New password and confirmation do not match")
