package school.charset.app.config

import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.RememberMeServices
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.security.web.context.SecurityContextRepository
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession
import school.charset.app.domain.user.UserRepository
import school.charset.app.infrastructure.security.CsrfCookieFilter
import school.charset.app.infrastructure.security.CustomUserDetailsService
import javax.sql.DataSource

@ConfigurationProperties(prefix = "app.remember-me")
data class RememberMeProperties(val key: String)

@Configuration
@EnableJdbcHttpSession
@EnableConfigurationProperties(RememberMeProperties::class)
class SecurityConfig {
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder(BCRYPT_STRENGTH)

    @Bean
    fun userDetailsService(userRepository: UserRepository): UserDetailsService = CustomUserDetailsService(userRepository)

    @Bean
    fun authenticationManager(
        userDetailsService: UserDetailsService,
        passwordEncoder: PasswordEncoder,
    ): AuthenticationManager {
        val provider = DaoAuthenticationProvider(userDetailsService)
        provider.setPasswordEncoder(passwordEncoder)
        return ProviderManager(provider)
    }

    @Bean
    fun persistentTokenRepository(dataSource: DataSource): PersistentTokenRepository = JdbcTokenRepositoryImpl().apply { setDataSource(dataSource) }

    @Bean
    fun rememberMeServices(
        userDetailsService: UserDetailsService,
        tokenRepository: PersistentTokenRepository,
        rememberMeProperties: RememberMeProperties,
    ): RememberMeServices {
        val services = PersistentTokenBasedRememberMeServices(
            rememberMeProperties.key,
            userDetailsService,
            tokenRepository,
        )
        services.parameter = REMEMBER_ME_PARAM
        services.setCookieName(REMEMBER_ME_COOKIE)
        services.setTokenValiditySeconds(REMEMBER_ME_VALIDITY_SECONDS)
        return services
    }

    @Bean
    fun securityContextRepository(): SecurityContextRepository = HttpSessionSecurityContextRepository()

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        rememberMeServices: RememberMeServices,
        securityContextRepository: SecurityContextRepository,
        rememberMeProperties: RememberMeProperties,
    ): SecurityFilterChain {
        http
            .authorizeHttpRequests { auth ->
                auth.requestMatchers(
                    "/api/auth/register",
                    "/api/auth/login",
                    "/api/auth/forgot-password",
                    "/api/auth/reset-password",
                    "/api/sandbox/**",
                ).permitAll()
                auth.anyRequest().authenticated()
            }
            .csrf { csrf ->
                csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                val xorDelegate = XorCsrfTokenRequestAttributeHandler()
                csrf.csrfTokenRequestHandler(xorDelegate::handle)
                csrf.ignoringRequestMatchers(
                    "/api/auth/register",
                    "/api/auth/login",
                    "/api/auth/forgot-password",
                    "/api/auth/reset-password",
                    "/api/sandbox/**",
                )
            }
            // Forces the lazy XSRF-TOKEN cookie to be written on every response.
            // Without this filter, the cookie is only saved on the first mutating
            // request's validation — by which time the client never had it to
            // echo back in X-XSRF-TOKEN, so that request always 403s.
            .addFilterAfter(CsrfCookieFilter(), BasicAuthenticationFilter::class.java)
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .logout { logout ->
                logout.logoutUrl("/api/auth/logout")
                logout.logoutSuccessHandler { _, response, _ ->
                    response.status = HttpServletResponse.SC_NO_CONTENT
                }
                logout.deleteCookies("SESSION", REMEMBER_ME_COOKIE)
            }
            .rememberMe { rm ->
                rm.rememberMeServices(rememberMeServices)
                rm.key(rememberMeProperties.key)
            }
            .securityContext { ctx -> ctx.securityContextRepository(securityContextRepository) }
            .exceptionHandling { eh ->
                // No redirect to a login page (JSON API) - return 401 to the client.
                eh.authenticationEntryPoint { _, response, _ ->
                    response.status = HttpServletResponse.SC_UNAUTHORIZED
                }
            }
        return http.build()
    }

    private companion object {
        const val BCRYPT_STRENGTH = 12

        // Name of the JSON body field / request parameter that triggers remember-me.
        // Aligned with the camelCase field `rememberMe` in `LoginRequest`.
        const val REMEMBER_ME_PARAM = "rememberMe"

        // Cookie name on the wire - kept kebab-case by wire convention.
        const val REMEMBER_ME_COOKIE = "remember-me"
        const val REMEMBER_ME_VALIDITY_SECONDS = 60 * 60 * 24 * 14 // 2 weeks
    }
}
