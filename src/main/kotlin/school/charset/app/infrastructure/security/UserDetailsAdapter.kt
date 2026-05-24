package school.charset.app.infrastructure.security

import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class UserDetailsAdapter(
    val userId: Long,
    private val email: String,
    private val passwordHashValue: String,
) : UserDetails {
    override fun getUsername(): String = email

    override fun getPassword(): String = passwordHashValue

    override fun getAuthorities(): Collection<GrantedAuthority> = emptyList()

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = true

    override fun isCredentialsNonExpired(): Boolean = true

    override fun isEnabled(): Boolean = true
}

// All our authenticated endpoints store a UserDetailsAdapter as the principal
// (set by CustomUserDetailsService on login and by AuthController on register
// auto-login). Anything else means a misconfigured filter chain — fail loud.
fun Authentication.requireUserDetailsAdapter(): UserDetailsAdapter = principal as? UserDetailsAdapter
    ?: error("Expected UserDetailsAdapter principal but got ${principal?.let { it::class.qualifiedName } ?: "null"}")
