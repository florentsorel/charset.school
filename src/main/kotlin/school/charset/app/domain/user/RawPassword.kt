package school.charset.app.domain.user

@JvmInline
value class RawPassword(val value: String) {
    override fun toString(): String = "RawPassword(***)"
}
