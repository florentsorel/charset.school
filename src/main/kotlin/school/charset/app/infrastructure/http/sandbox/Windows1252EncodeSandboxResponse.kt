package school.charset.app.infrastructure.http.sandbox

import school.charset.app.domain.exercise.Step

data class Windows1252EncodeSandboxResponse(
    val codepoint: Int,
    val codepointLabel: String,
    val glyph: String?,
    val label: String?,
    val steps: List<Step>,
)
