package school.charset.app.domain.auth

/**
 * Raised when an authenticated session references a user that no longer exists in DB
 * (e.g., user self-deleted while keeping the cookie active, or multi-tab scenario
 * where one tab deleted the account). The HTTP layer maps it to 401 + invalidates
 * the session so the client can re-authenticate cleanly.
 */
class OrphanedSessionException(val userId: Long) : RuntimeException("Authenticated session points to missing user: $userId")
