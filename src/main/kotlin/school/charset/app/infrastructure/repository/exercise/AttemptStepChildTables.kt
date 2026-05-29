package school.charset.app.infrastructure.repository.exercise

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.selectAll
import school.charset.app.domain.encoding.Encoding
import school.charset.app.domain.exercise.Answer
import school.charset.app.domain.exercise.Step
import school.charset.app.domain.exercise.StepType

object AttemptStepFormatTable : Table("attempt_step_format") {
    val stepId = long("step_id").references(AttemptStepsTable.id)
    val choices = array<String>("choices")
    val expected = varchar("expected", 64)
    val userAnswer = varchar("user_answer", 64).nullable()

    override val primaryKey = PrimaryKey(stepId)
}

object AttemptStepBinaryTable : Table("attempt_step_binary") {
    val stepId = long("step_id").references(AttemptStepsTable.id)
    val expected = varchar("expected", 64)
    val bitLength = short("bit_length")
    val userAnswer = varchar("user_answer", 64).nullable()

    override val primaryKey = PrimaryKey(stepId)
}

object AttemptStepBitGroupsTable : Table("attempt_step_bit_groups") {
    val stepId = long("step_id").references(AttemptStepsTable.id)
    val expected = array<String>("expected")
    val userAnswer = array<String>("user_answer").nullable()

    override val primaryKey = PrimaryKey(stepId)
}

object AttemptStepHexBytesTable : Table("attempt_step_hex_bytes") {
    val stepId = long("step_id").references(AttemptStepsTable.id)
    val expected = array<Short>("expected")
    val userAnswer = array<Short>("user_answer").nullable()

    override val primaryKey = PrimaryKey(stepId)
}

object AttemptStepCodePointTable : Table("attempt_step_code_point") {
    val stepId = long("step_id").references(AttemptStepsTable.id)
    val expected = integer("expected")
    val userAnswer = integer("user_answer").nullable()

    override val primaryKey = PrimaryKey(stepId)
}

object AttemptStepUsefulBitCountTable : Table("attempt_step_useful_bit_count") {
    val stepId = long("step_id").references(AttemptStepsTable.id)
    val expected = short("expected")
    val userAnswer = short("user_answer").nullable()

    override val primaryKey = PrimaryKey(stepId)
}

object AttemptStepEndiannessTable : Table("attempt_step_endianness") {
    val stepId = long("step_id").references(AttemptStepsTable.id)
    val expected = varchar("expected", 16).transform(
        wrap = { Encoding.Endian.valueOf(it) },
        unwrap = { it.name },
    )
    val userAnswer = varchar("user_answer", 16).transform(
        wrap = { Encoding.Endian.valueOf(it) },
        unwrap = { it.name },
    ).nullable()

    override val primaryKey = PrimaryKey(stepId)
}

// For each distinct step_type in the parent rows, runs ONE select on the
// matching child table filtered by `step_id IN (...)`. Returns a flat
// map step_id → (Step, Answer?) for cheap lookup during assembly.
fun loadStepDataBatched(stepRows: List<ResultRow>): Map<Long, Pair<Step, Answer?>> {
    if (stepRows.isEmpty()) return emptyMap()

    val idsByType = stepRows.groupBy(
        { it[AttemptStepsTable.stepType] },
        { it[AttemptStepsTable.id] },
    )

    return idsByType.flatMap { (type, ids) -> selectChildRows(type, ids) }.toMap()
}

fun selectChildRows(type: StepType, stepIds: List<Long>): List<Pair<Long, Pair<Step, Answer?>>> = when (type) {
    StepType.Format ->
        AttemptStepFormatTable
            .selectAll()
            .where { AttemptStepFormatTable.stepId inList stepIds }
            .map { row ->
                row[AttemptStepFormatTable.stepId] to (
                    Step.Format(
                        choices = row[AttemptStepFormatTable.choices],
                        expected = row[AttemptStepFormatTable.expected],
                    ) to row[AttemptStepFormatTable.userAnswer]?.let(Answer::FormatChoice)
                    )
            }

    StepType.Binary ->
        AttemptStepBinaryTable
            .selectAll()
            .where { AttemptStepBinaryTable.stepId inList stepIds }
            .map { row ->
                row[AttemptStepBinaryTable.stepId] to (
                    Step.Binary(
                        expected = row[AttemptStepBinaryTable.expected],
                        length = row[AttemptStepBinaryTable.bitLength].toInt(),
                    ) to row[AttemptStepBinaryTable.userAnswer]?.let(Answer::BinaryValue)
                    )
            }

    StepType.BitGroups ->
        AttemptStepBitGroupsTable
            .selectAll()
            .where { AttemptStepBitGroupsTable.stepId inList stepIds }
            .map { row ->
                row[AttemptStepBitGroupsTable.stepId] to (
                    Step.BitGroups(expected = row[AttemptStepBitGroupsTable.expected]) to
                        row[AttemptStepBitGroupsTable.userAnswer]?.let(Answer::BitGroupsValue)
                    )
            }

    StepType.HexBytes ->
        AttemptStepHexBytesTable
            .selectAll()
            .where { AttemptStepHexBytesTable.stepId inList stepIds }
            .map { row ->
                row[AttemptStepHexBytesTable.stepId] to (
                    Step.HexBytes(expected = row[AttemptStepHexBytesTable.expected].map { it.toInt() }) to
                        row[AttemptStepHexBytesTable.userAnswer]?.let { bytes ->
                            Answer.HexBytesValue(bytes.map { it.toInt() })
                        }
                    )
            }

    StepType.CodePointEntry ->
        AttemptStepCodePointTable
            .selectAll()
            .where { AttemptStepCodePointTable.stepId inList stepIds }
            .map { row ->
                row[AttemptStepCodePointTable.stepId] to (
                    Step.CodePointEntry(expected = row[AttemptStepCodePointTable.expected]) to
                        row[AttemptStepCodePointTable.userAnswer]?.let(Answer::CodePointValue)
                    )
            }

    StepType.UsefulBitCount ->
        AttemptStepUsefulBitCountTable
            .selectAll()
            .where { AttemptStepUsefulBitCountTable.stepId inList stepIds }
            .map { row ->
                row[AttemptStepUsefulBitCountTable.stepId] to (
                    Step.UsefulBitCount(expected = row[AttemptStepUsefulBitCountTable.expected].toInt()) to
                        row[AttemptStepUsefulBitCountTable.userAnswer]?.let { Answer.UsefulBitCountValue(it.toInt()) }
                    )
            }

    StepType.Endianness ->
        AttemptStepEndiannessTable
            .selectAll()
            .where { AttemptStepEndiannessTable.stepId inList stepIds }
            .map { row ->
                row[AttemptStepEndiannessTable.stepId] to (
                    Step.Endianness(expected = row[AttemptStepEndiannessTable.expected]) to
                        row[AttemptStepEndiannessTable.userAnswer]?.let(Answer::EndiannessChoice)
                    )
            }
}
