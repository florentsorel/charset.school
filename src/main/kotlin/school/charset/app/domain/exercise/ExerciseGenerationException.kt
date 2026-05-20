package school.charset.app.domain.exercise

import school.charset.app.domain.encoding.Encoding

class ExerciseGenerationException(
    val encoding: Encoding,
    val level: Int,
    val reason: String,
) : RuntimeException("Cannot generate exercise for ${encoding.id} level $level: $reason")
