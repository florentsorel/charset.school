package school.charset.app.domain.exercise

import school.charset.app.domain.encoding.CodePoint
import school.charset.app.domain.encoding.Encoding

data class Exercise(
    val codePoint: CodePoint,
    val encoding: Encoding,
    val level: Int,
    val granularity: Granularity,
    val steps: List<Step>,
)
