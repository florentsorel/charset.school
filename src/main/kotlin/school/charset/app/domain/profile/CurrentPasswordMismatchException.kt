package school.charset.app.domain.profile

class CurrentPasswordMismatchException : RuntimeException("Current password does not match the stored hash")
