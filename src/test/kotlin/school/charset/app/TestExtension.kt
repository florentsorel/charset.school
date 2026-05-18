package school.charset.app

fun ByteArray.toHex(): String = joinToString(" ") { "%02X".format(it.toInt() and 0xFF) }
