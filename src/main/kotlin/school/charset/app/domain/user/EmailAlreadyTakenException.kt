package school.charset.app.domain.user

class EmailAlreadyTakenException(val email: String) : RuntimeException("Email already taken: $email")
