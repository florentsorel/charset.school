package school.charset.app.domain.sandbox

import school.charset.app.domain.encoding.Encoding

class SandboxEndianParser {
    fun parse(raw: String): Encoding.Endian = when (raw.trim().lowercase()) {
        "big" -> Encoding.Endian.BigEndian
        "little" -> Encoding.Endian.LittleEndian
        else -> throw SandboxEndianParseException(REASON_INVALID)
    }

    companion object {
        const val REASON_INVALID = "invalid"
    }
}

class SandboxEndianParseException(val reason: String) : RuntimeException("Failed to parse sandbox endian: $reason")
