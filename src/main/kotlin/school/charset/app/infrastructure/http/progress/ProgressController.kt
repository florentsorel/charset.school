package school.charset.app.infrastructure.http.progress

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import school.charset.app.domain.progress.ModuleProgress
import school.charset.app.domain.progress.ProgressService
import school.charset.app.infrastructure.security.requireUserDetailsAdapter

@RestController
@RequestMapping(
    "/api/progress",
    produces = [MediaType.APPLICATION_JSON_VALUE],
)
class ProgressController(
    private val progressService: ProgressService,
) {

    @GetMapping
    fun getAll(authentication: Authentication): ResponseEntity<ProgressResponse> {
        val userId = authentication.requireUserDetailsAdapter().userId
        return ResponseEntity.ok(ProgressResponse(progress = progressService.findAll(userId)))
    }
}

data class ProgressResponse(val progress: List<ModuleProgress>)
