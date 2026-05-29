package school.charset.app.infrastructure.repository.exercise

import org.jetbrains.exposed.v1.core.Table
import school.charset.app.domain.encoding.Encoding

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
