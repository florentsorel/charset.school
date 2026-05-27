package school.charset.app.domain.encoding

class EncoderException(
    codePoint: CodePoint,
    encoding: Encoding,
    val reason: String,
) : RuntimeException("Cannot encode $codePoint in ${encoding.id}: $reason")
