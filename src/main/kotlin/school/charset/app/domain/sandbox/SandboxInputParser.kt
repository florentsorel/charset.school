package school.charset.app.domain.sandbox

import school.charset.app.domain.encoding.CodePoint

class SandboxInputParser {
    private val uPlusRegex = Regex("^[Uu]\\+([0-9a-fA-F]{1,6})$")
    private val hexRegex = Regex("^0[xX]([0-9a-fA-F]{1,6})$")
    private val decRegex = Regex("^\\d+$")

    fun parse(raw: String): CodePoint {
        val s = raw.trim()
        if (s.isEmpty()) throw SandboxParseException(REASON_EMPTY)

        uPlusRegex.matchEntire(s)?.groupValues?.get(1)?.let {
            return validateRange(it.toInt(16))
        }
        hexRegex.matchEntire(s)?.groupValues?.get(1)?.let {
            return validateRange(it.toInt(16))
        }
        if (decRegex.matches(s)) {
            return validateRange(s.toInt())
        }

        val codePoints = s.codePoints().toArray()
        if (codePoints.size == 1) {
            return validateRange(codePoints[0])
        }

        throw SandboxParseException(REASON_UNPARSEABLE)
    }

    private fun validateRange(value: Int): CodePoint {
        if (value < CodePoint.MIN || value > CodePoint.MAX) {
            throw SandboxParseException(REASON_OUT_OF_RANGE)
        }
        if (value in CodePoint.SURROGATE_MIN..CodePoint.SURROGATE_MAX) {
            throw SandboxParseException(REASON_SURROGATE)
        }
        return CodePoint(value)
    }

    companion object {
        const val REASON_EMPTY = "empty"
        const val REASON_UNPARSEABLE = "unparseable"
        const val REASON_OUT_OF_RANGE = "out_of_range"
        const val REASON_SURROGATE = "surrogate"
    }
}

class SandboxParseException(val reason: String) : RuntimeException("Failed to parse sandbox input: $reason")
