package school.charset.app.domain.encoding

class EncoderException(
    codePoint: CodePoint,
    encoding: Encoding,
    reason: String,
) : RuntimeException("Cannot encode $codePoint in ${encoding.id}: $reason")
