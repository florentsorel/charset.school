package school.charset.app.domain.encoding

class EncodingException(
    codePoint: CodePoint,
    encoding: Encoding,
    reason: String,
) : RuntimeException("Cannot encode $codePoint in ${encoding.id}: $reason")
