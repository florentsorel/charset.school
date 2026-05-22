package school.charset.app.infrastructure.http

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class ErrorResponse(
    val errorType: String,
    val params: Map<String, String> = emptyMap(),
    val fieldErrors: Map<String, List<String>> = emptyMap(),
)
