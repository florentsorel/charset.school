package school.charset.app.domain.exercise

import school.charset.app.domain.encoding.Encoding

class ExerciseGenerator(
    generators: Set<EncodingExerciseGenerator>,
) {
    private val byEncoding: Map<Encoding, EncodingExerciseGenerator> =
        generators.associateBy { it.encoding }

    init {
        require(generators.size == byEncoding.size) {
            "Duplicate generators registered for the same encoding"
        }
    }

    fun generate(
        encoding: Encoding,
        level: Int,
        granularity: Granularity,
    ): Exercise {
        val generator = byEncoding[encoding]
            ?: throw ExerciseGenerationException(
                encoding = encoding,
                level = level,
                reason = "no generator registered for this encoding",
            )
        return generator.generate(level, granularity)
    }
}
