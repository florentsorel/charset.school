package school.charset.app.infrastructure.repository.exercise

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import school.charset.app.domain.encoding.CodePoint
import school.charset.app.domain.encoding.Encoding
import school.charset.app.domain.exercise.Answer
import school.charset.app.domain.exercise.AttemptStep
import school.charset.app.domain.exercise.ExerciseAttempt
import school.charset.app.domain.exercise.ExerciseAttemptRepository
import school.charset.app.domain.exercise.ExerciseModule
import school.charset.app.domain.exercise.Granularity
import school.charset.app.domain.exercise.Step
import school.charset.app.domain.exercise.StepType
import kotlin.time.Clock

class ExposedExerciseAttemptRepository(
    private val clock: Clock,
) : ExerciseAttemptRepository {

    override fun create(
        userId: Long,
        module: ExerciseModule,
        level: Int,
        granularity: Granularity,
        codePoint: CodePoint,
        encoding: Encoding,
        steps: List<Step>,
    ): ExerciseAttempt = transaction {
        val now = clock.now()
        val attemptId = ExerciseAttemptsTable.insert {
            it[ExerciseAttemptsTable.userId] = userId
            it[moduleId] = module
            it[ExerciseAttemptsTable.level] = level.toShort()
            it[ExerciseAttemptsTable.granularity] = granularity
            it[ExerciseAttemptsTable.codePoint] = codePoint.value
            it[ExerciseAttemptsTable.encoding] = encoding
            it[correct] = false
            it[finalized] = false
            it[createdAt] = now
        } get ExerciseAttemptsTable.id

        val attemptSteps = steps.mapIndexed { index, step ->
            val stepId = AttemptStepsTable.insert {
                it[AttemptStepsTable.attemptId] = attemptId
                it[position] = index.toShort()
                it[stepType] = step.type
                it[correct] = false
                it[errorType] = null
                it[attempts] = 0
                it[revealed] = false
            } get AttemptStepsTable.id

            insertStepExpected(stepId, step)

            AttemptStep(
                id = stepId,
                position = index,
                step = step,
                correct = false,
                errorType = null,
                attempts = 0,
                revealed = false,
                userAnswer = null,
            )
        }

        ExerciseAttempt(
            id = attemptId,
            userId = userId,
            module = module,
            level = level,
            granularity = granularity,
            codePoint = codePoint,
            encoding = encoding,
            correct = false,
            finalized = false,
            durationMs = null,
            steps = attemptSteps,
            createdAt = now,
        )
    }

    override fun findLatestUnfinalizedByUserAndModule(userId: Long, module: ExerciseModule): ExerciseAttempt? = transaction {
        val latestId = ExerciseAttemptsTable
            .selectAll()
            .where {
                (ExerciseAttemptsTable.userId eq userId)
                    .and(ExerciseAttemptsTable.moduleId eq module)
                    .and(ExerciseAttemptsTable.finalized eq false)
            }
            .orderBy(ExerciseAttemptsTable.createdAt, SortOrder.DESC)
            .limit(1)
            .firstOrNull()
            ?.get(ExerciseAttemptsTable.id)
            ?: return@transaction null

        findById(latestId)
    }

    override fun findById(attemptId: Long): ExerciseAttempt? = transaction {
        val attemptRow = ExerciseAttemptsTable
            .selectAll()
            .where { ExerciseAttemptsTable.id eq attemptId }
            .singleOrNull()
            ?: return@transaction null

        val steps = AttemptStepsTable
            .selectAll()
            .where { AttemptStepsTable.attemptId eq attemptId }
            .orderBy(AttemptStepsTable.position, SortOrder.ASC)
            .map { it.toAttemptStep() }

        ExerciseAttempt(
            id = attemptId,
            userId = attemptRow[ExerciseAttemptsTable.userId],
            module = attemptRow[ExerciseAttemptsTable.moduleId],
            level = attemptRow[ExerciseAttemptsTable.level].toInt(),
            granularity = attemptRow[ExerciseAttemptsTable.granularity],
            codePoint = CodePoint(attemptRow[ExerciseAttemptsTable.codePoint]),
            encoding = attemptRow[ExerciseAttemptsTable.encoding],
            correct = attemptRow[ExerciseAttemptsTable.correct],
            finalized = attemptRow[ExerciseAttemptsTable.finalized],
            durationMs = attemptRow[ExerciseAttemptsTable.durationMs],
            steps = steps,
            createdAt = attemptRow[ExerciseAttemptsTable.createdAt],
        )
    }

    override fun recordStepSubmission(
        stepId: Long,
        userAnswer: Answer,
        correct: Boolean,
        errorType: String?,
    ): AttemptStep = transaction {
        val current = AttemptStepsTable
            .selectAll()
            .where { AttemptStepsTable.id eq stepId }
            .single()

        val newAttempts = (current[AttemptStepsTable.attempts] + 1).toShort()

        AttemptStepsTable.update({ AttemptStepsTable.id eq stepId }) {
            it[AttemptStepsTable.correct] = correct
            it[AttemptStepsTable.errorType] = errorType
            it[attempts] = newAttempts
        }
        updateUserAnswer(stepId, userAnswer)

        loadAttemptStep(stepId)
    }

    override fun markStepRevealed(stepId: Long): AttemptStep = transaction {
        AttemptStepsTable.update({ AttemptStepsTable.id eq stepId }) {
            it[revealed] = true
        }
        loadAttemptStep(stepId)
    }

    override fun finalize(attemptId: Long, correct: Boolean, durationMs: Int?): ExerciseAttempt = transaction {
        ExerciseAttemptsTable.update({ ExerciseAttemptsTable.id eq attemptId }) {
            it[ExerciseAttemptsTable.correct] = correct
            it[finalized] = true
            it[ExerciseAttemptsTable.durationMs] = durationMs
        }
        findById(attemptId) ?: error("Attempt $attemptId disappeared after finalize")
    }

    private fun loadAttemptStep(stepId: Long): AttemptStep = AttemptStepsTable
        .selectAll()
        .where { AttemptStepsTable.id eq stepId }
        .single()
        .toAttemptStep()

    private fun ResultRow.toAttemptStep(): AttemptStep {
        val stepId = this[AttemptStepsTable.id]
        val stepType = this[AttemptStepsTable.stepType]
        return AttemptStep(
            id = stepId,
            position = this[AttemptStepsTable.position].toInt(),
            step = loadStep(stepId, stepType),
            correct = this[AttemptStepsTable.correct],
            errorType = this[AttemptStepsTable.errorType],
            attempts = this[AttemptStepsTable.attempts].toInt(),
            revealed = this[AttemptStepsTable.revealed],
            userAnswer = loadUserAnswer(stepId, stepType),
        )
    }

    private fun insertStepExpected(stepId: Long, step: Step) {
        when (step) {
            is Step.Format -> AttemptStepFormatTable.insert {
                it[AttemptStepFormatTable.stepId] = stepId
                it[choices] = step.choices
                it[expected] = step.expected
            }
            is Step.Binary -> AttemptStepBinaryTable.insert {
                it[AttemptStepBinaryTable.stepId] = stepId
                it[expected] = step.expected
                it[bitLength] = step.length.toShort()
            }
            is Step.BitGroups -> AttemptStepBitGroupsTable.insert {
                it[AttemptStepBitGroupsTable.stepId] = stepId
                it[expected] = step.expected
            }
            is Step.HexBytes -> AttemptStepHexBytesTable.insert {
                it[AttemptStepHexBytesTable.stepId] = stepId
                it[expected] = step.expected.map { byte -> byte.toShort() }
            }
            is Step.CodePointEntry -> AttemptStepCodePointTable.insert {
                it[AttemptStepCodePointTable.stepId] = stepId
                it[expected] = step.expected
            }
            is Step.UsefulBitCount -> AttemptStepUsefulBitCountTable.insert {
                it[AttemptStepUsefulBitCountTable.stepId] = stepId
                it[expected] = step.expected.toShort()
            }
            is Step.Endianness -> AttemptStepEndiannessTable.insert {
                it[AttemptStepEndiannessTable.stepId] = stepId
                it[expected] = step.expected
            }
        }
    }

    private fun loadStep(stepId: Long, stepType: StepType): Step = when (stepType) {
        StepType.Format ->
            AttemptStepFormatTable
                .selectAll()
                .where { AttemptStepFormatTable.stepId eq stepId }
                .single()
                .let { Step.Format(choices = it[AttemptStepFormatTable.choices], expected = it[AttemptStepFormatTable.expected]) }

        StepType.Binary ->
            AttemptStepBinaryTable
                .selectAll()
                .where { AttemptStepBinaryTable.stepId eq stepId }
                .single()
                .let { Step.Binary(expected = it[AttemptStepBinaryTable.expected], length = it[AttemptStepBinaryTable.bitLength].toInt()) }

        StepType.BitGroups ->
            AttemptStepBitGroupsTable
                .selectAll()
                .where { AttemptStepBitGroupsTable.stepId eq stepId }
                .single()
                .let { Step.BitGroups(expected = it[AttemptStepBitGroupsTable.expected]) }

        StepType.HexBytes ->
            AttemptStepHexBytesTable
                .selectAll()
                .where { AttemptStepHexBytesTable.stepId eq stepId }
                .single()
                .let { Step.HexBytes(expected = it[AttemptStepHexBytesTable.expected].map { byte -> byte.toInt() }) }

        StepType.CodePointEntry ->
            AttemptStepCodePointTable
                .selectAll()
                .where { AttemptStepCodePointTable.stepId eq stepId }
                .single()
                .let { Step.CodePointEntry(expected = it[AttemptStepCodePointTable.expected]) }

        StepType.UsefulBitCount ->
            AttemptStepUsefulBitCountTable
                .selectAll()
                .where { AttemptStepUsefulBitCountTable.stepId eq stepId }
                .single()
                .let { Step.UsefulBitCount(expected = it[AttemptStepUsefulBitCountTable.expected].toInt()) }

        StepType.Endianness ->
            AttemptStepEndiannessTable
                .selectAll()
                .where { AttemptStepEndiannessTable.stepId eq stepId }
                .single()
                .let { Step.Endianness(expected = it[AttemptStepEndiannessTable.expected]) }
    }

    private fun loadUserAnswer(stepId: Long, stepType: StepType): Answer? = when (stepType) {
        StepType.Format ->
            AttemptStepFormatTable
                .selectAll()
                .where { AttemptStepFormatTable.stepId eq stepId }
                .single()[AttemptStepFormatTable.userAnswer]
                ?.let(Answer::FormatChoice)

        StepType.Binary ->
            AttemptStepBinaryTable
                .selectAll()
                .where { AttemptStepBinaryTable.stepId eq stepId }
                .single()[AttemptStepBinaryTable.userAnswer]
                ?.let(Answer::BinaryValue)

        StepType.BitGroups ->
            AttemptStepBitGroupsTable
                .selectAll()
                .where { AttemptStepBitGroupsTable.stepId eq stepId }
                .single()[AttemptStepBitGroupsTable.userAnswer]
                ?.let(Answer::BitGroupsValue)

        StepType.HexBytes ->
            AttemptStepHexBytesTable
                .selectAll()
                .where { AttemptStepHexBytesTable.stepId eq stepId }
                .single()[AttemptStepHexBytesTable.userAnswer]
                ?.let { bytes -> Answer.HexBytesValue(bytes.map { it.toInt() }) }

        StepType.CodePointEntry ->
            AttemptStepCodePointTable
                .selectAll()
                .where { AttemptStepCodePointTable.stepId eq stepId }
                .single()[AttemptStepCodePointTable.userAnswer]
                ?.let(Answer::CodePointValue)

        StepType.UsefulBitCount ->
            AttemptStepUsefulBitCountTable
                .selectAll()
                .where { AttemptStepUsefulBitCountTable.stepId eq stepId }
                .single()[AttemptStepUsefulBitCountTable.userAnswer]
                ?.let { Answer.UsefulBitCountValue(it.toInt()) }

        StepType.Endianness ->
            AttemptStepEndiannessTable
                .selectAll()
                .where { AttemptStepEndiannessTable.stepId eq stepId }
                .single()[AttemptStepEndiannessTable.userAnswer]
                ?.let(Answer::EndiannessChoice)
    }

    private fun updateUserAnswer(stepId: Long, answer: Answer) {
        when (answer) {
            is Answer.FormatChoice -> AttemptStepFormatTable.update({ AttemptStepFormatTable.stepId eq stepId }) {
                it[userAnswer] = answer.value
            }
            is Answer.BinaryValue -> AttemptStepBinaryTable.update({ AttemptStepBinaryTable.stepId eq stepId }) {
                it[userAnswer] = answer.bits
            }
            is Answer.BitGroupsValue -> AttemptStepBitGroupsTable.update({ AttemptStepBitGroupsTable.stepId eq stepId }) {
                it[userAnswer] = answer.groups
            }
            is Answer.HexBytesValue -> AttemptStepHexBytesTable.update({ AttemptStepHexBytesTable.stepId eq stepId }) {
                it[userAnswer] = answer.bytes.map { byte -> byte.toShort() }
            }
            is Answer.CodePointValue -> AttemptStepCodePointTable.update({ AttemptStepCodePointTable.stepId eq stepId }) {
                it[userAnswer] = answer.value
            }
            is Answer.UsefulBitCountValue -> AttemptStepUsefulBitCountTable.update({ AttemptStepUsefulBitCountTable.stepId eq stepId }) {
                it[userAnswer] = answer.value.toShort()
            }
            is Answer.EndiannessChoice -> AttemptStepEndiannessTable.update({ AttemptStepEndiannessTable.stepId eq stepId }) {
                it[userAnswer] = answer.value
            }
        }
    }
}
