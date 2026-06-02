package school.charset.app.infrastructure.repository.exercise

import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.slf4j.LoggerFactory
import school.charset.app.domain.encoding.CodePoint
import school.charset.app.domain.encoding.Encoding
import school.charset.app.domain.exercise.Answer
import school.charset.app.domain.exercise.AttemptStep
import school.charset.app.domain.exercise.ExerciseAttempt
import school.charset.app.domain.exercise.ExerciseAttemptRepository
import school.charset.app.domain.exercise.ExerciseModule
import school.charset.app.domain.exercise.Step
import kotlin.time.Clock

class ExposedExerciseAttemptRepository(
    private val clock: Clock,
) : ExerciseAttemptRepository {
    private val logger = LoggerFactory.getLogger(ExposedExerciseAttemptRepository::class.java)

    override fun create(
        token: String,
        module: ExerciseModule,
        level: Int,
        codePoint: CodePoint,
        encoding: Encoding,
        steps: List<Step>,
    ): ExerciseAttempt = transaction {
        val now = clock.now()
        val attemptId = ExerciseAttemptsTable.insert {
            it[ExerciseAttemptsTable.token] = token
            it[moduleId] = module
            it[ExerciseAttemptsTable.level] = level.toShort()
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
            token = token,
            module = module,
            level = level,
            codePoint = codePoint,
            encoding = encoding,
            correct = false,
            finalized = false,
            durationMs = null,
            steps = attemptSteps,
            createdAt = now,
        )
    }

    override fun findLatestUnfinalizedByTokenAndModule(
        token: String,
        module: ExerciseModule,
    ): ExerciseAttempt? = transaction {
        ExerciseAttemptsTable
            .selectAll()
            .where {
                (ExerciseAttemptsTable.token eq token)
                    .and(ExerciseAttemptsTable.moduleId eq module)
                    .and(ExerciseAttemptsTable.finalized eq false)
            }
            .orderBy(ExerciseAttemptsTable.createdAt, SortOrder.DESC)
            .limit(1)
            .firstOrNull()
            ?.toExerciseAttempt()
    }

    override fun findById(attemptId: Long): ExerciseAttempt? = transaction {
        ExerciseAttemptsTable
            .selectAll()
            .where { ExerciseAttemptsTable.id eq attemptId }
            .singleOrNull()
            ?.toExerciseAttempt()
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

        current.toAttemptStep(overrides = StepRowOverrides(correct, errorType, newAttempts.toInt()))
    }

    override fun markStepRevealed(stepId: Long): AttemptStep = transaction {
        AttemptStepsTable.update({ AttemptStepsTable.id eq stepId }) {
            it[revealed] = true
        }
        AttemptStepsTable
            .selectAll()
            .where { AttemptStepsTable.id eq stepId }
            .single()
            .toAttemptStep()
    }

    override fun finalize(attemptId: Long, correct: Boolean, durationMs: Int?): ExerciseAttempt = transaction {
        ExerciseAttemptsTable.update({ ExerciseAttemptsTable.id eq attemptId }) {
            it[ExerciseAttemptsTable.correct] = correct
            it[finalized] = true
            it[ExerciseAttemptsTable.durationMs] = durationMs
        }
        ExerciseAttemptsTable
            .selectAll()
            .where { ExerciseAttemptsTable.id eq attemptId }
            .singleOrNull()
            ?.toExerciseAttempt()
            ?: run {
                logger.error("Attempt disappeared right after finalize (attemptId={})", attemptId)
                error("Attempt $attemptId disappeared after finalize")
            }
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

            is Step.Offset -> AttemptStepOffsetTable.insert {
                it[AttemptStepOffsetTable.stepId] = stepId
                it[expected] = step.expected
            }
        }
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

            is Answer.OffsetValue -> AttemptStepOffsetTable.update({ AttemptStepOffsetTable.stepId eq stepId }) {
                it[userAnswer] = answer.value
            }
        }
    }
}
