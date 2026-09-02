package com.aiwatch.core.net

/**
 * A non-2xx response from OpenRouter, with the server's own message extracted
 * from its `{"error": {...}}` envelope so the UI can show something useful.
 */
class OpenRouterException(
    val httpStatus: Int,
    message: String,
    val errorCode: Int? = null,
) : Exception(message) {

    val isAuthFailure: Boolean get() = httpStatus == 401 || httpStatus == 403
    val isRateLimited: Boolean get() = httpStatus == 429
    val isNetworkProblem: Boolean get() = httpStatus == 0

    /** Short, watch-sized hint derived from the status code. */
    val shortHint: String
        get() = when {
            isAuthFailure -> "Key rejected"
            isRateLimited -> "Rate limited"
            httpStatus == 402 -> "Out of credits"
            httpStatus == 404 -> "Model not found"
            httpStatus in 500..599 -> "OpenRouter error"
            isNetworkProblem -> "No connection"
            else -> "Request failed"
        }
}
