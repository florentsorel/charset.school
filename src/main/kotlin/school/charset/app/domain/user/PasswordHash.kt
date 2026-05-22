package school.charset.app.domain.user

@JvmInline
value class PasswordHash(val value: String) {
    override fun toString(): String = "PasswordHash(***)"
}
