package school.charset.app.domain.exercise.generator

import school.charset.app.domain.encoding.Encoding
import school.charset.app.domain.exercise.Exercise
import school.charset.app.domain.exercise.Granularity

interface EncodingExerciseGenerator {
    val encoding: Encoding

    fun generate(level: Int, granularity: Granularity): Exercise
}
