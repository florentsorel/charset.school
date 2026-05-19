package school.charset.app.domain.encoding

fun bytes(vararg values: Int): ByteArray = ByteArray(values.size) { values[it].toByte() }
