package school.charset.app.infrastructure.http.auth

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.RememberMeServices
import org.springframework.security.web.context.SecurityContextRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import school.charset.app.domain.auth.AuthService
import school.charset.app.domain.auth.OrphanedSessionException
import school.charset.app.domain.user.RawPassword
import school.charset.app.domain.user.User
import school.charset.app.domain.user.UserRepository
import school.charset.app.infrastructure.security.UserDetailsAdapter
import school.charset.app.infrastructure.security.requireUserDetailsAdapter

@RestController
@RequestMapping(
    "/api/auth",
    produces = [MediaType.APPLICATION_JSON_VALUE],
)
class AuthController(
    private val authService: AuthService,
    private val userRepository: UserRepository,
    private val authenticationManager: AuthenticationManager,
    private val securityContextRepository: SecurityContextRepository,
    private val rememberMeServices: RememberMeServices,
) {
    @PostMapping("/register", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun register(
        @Valid @RequestBody req: RegisterRequest,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): ResponseEntity<User> {
        val user = authService.register(
            email = req.email,
            name = req.name,
            rawPassword = RawPassword(req.password),
            locale = req.locale,
        )

        val principal = UserDetailsAdapter(
            userId = user.id,
            email = user.email,
            passwordHashValue = user.passwordHash.value,
        )
        val auth = UsernamePasswordAuthenticationToken.authenticated(
            principal,
            null,
            principal.authorities,
        )
        val context = SecurityContextHolder.createEmptyContext()
        context.authentication = auth
        SecurityContextHolder.setContext(context)
        securityContextRepository.saveContext(context, request, response)

        return ResponseEntity.status(HttpStatus.CREATED).body(user)
    }

    @PostMapping("/login", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun login(
        @Valid @RequestBody req: LoginRequest,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): ResponseEntity<User> {
        val unauth = UsernamePasswordAuthenticationToken.unauthenticated(req.email, req.password)
        val auth = authenticationManager.authenticate(unauth)

        val context = SecurityContextHolder.createEmptyContext()
        context.authentication = auth
        SecurityContextHolder.setContext(context)
        securityContextRepository.saveContext(context, request, response)

        if (req.rememberMe) {
            val wrappedRequest = object : HttpServletRequestWrapper(request) {
                override fun getParameter(name: String): String? = if (name == REMEMBER_ME_PARAM) "true" else super.getParameter(name)
            }
            rememberMeServices.loginSuccess(wrappedRequest, response, auth)
        }

        val userId = auth.requireUserDetailsAdapter().userId
        val user = userRepository.findById(userId)
            ?: throw OrphanedSessionException(userId)
        return ResponseEntity.ok(user)
    }

    @GetMapping("/me")
    fun me(authentication: Authentication): ResponseEntity<User> {
        val userId = authentication.requireUserDetailsAdapter().userId
        val user = userRepository.findById(userId)
            ?: throw OrphanedSessionException(userId)
        return ResponseEntity.ok(user)
    }

    private companion object {
        const val REMEMBER_ME_PARAM = "rememberMe"
    }
}
