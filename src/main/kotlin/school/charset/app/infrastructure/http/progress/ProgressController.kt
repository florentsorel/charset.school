package school.charset.app.infrastructure.http.progress

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import school.charset.app.domain.progress.ModuleProgress
import school.charset.app.domain.progress.ProgressService
import school.charset.app.infrastructure.http.TokenId

@RestController
@RequestMapping(
    "/api/progress",
    produces = [MediaType.APPLICATION_JSON_VALUE],
)
class ProgressController(
    private val progressService: ProgressService,
) {

    @GetMapping
    fun getAll(@TokenId token: String): ResponseEntity<ProgressResponse> = ResponseEntity.ok(ProgressResponse(progress = progressService.findAll(token)))
}

data class ProgressResponse(val progress: List<ModuleProgress>)
