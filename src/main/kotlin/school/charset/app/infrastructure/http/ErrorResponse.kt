package school.charset.app.infrastructure.http

data class ErrorResponse(
    val errorType: String,
    val params: Map<String, String> = emptyMap(),
)
