package school.charset.app.domain.encoding

class DecoderException(
    bytes: ByteArray,
    encoding: Encoding,
    val reason: String,
) : RuntimeException("Cannot decode [${bytes.toHex()}] in ${encoding.id}: $reason")
