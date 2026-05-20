package school.charset.app.domain.exercise

import school.charset.app.domain.encoding.Encoding

interface EncodingExerciseGenerator {
    val encoding: Encoding

    fun generate(level: Int, granularity: Granularity): Exercise
}
