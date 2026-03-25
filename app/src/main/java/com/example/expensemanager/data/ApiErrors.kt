package com.example.expensemanager.data

import com.google.gson.Gson
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

data class ApiErrorResponse(
    val detail: String? = null
)

class ApiException(
    val code: Int,
    override val message: String
) : Exception(message)

private val gson = Gson()

fun Response<*>.errorMessage(fallback: String): String {
    val rawBody = runCatching { errorBody()?.string() }.getOrNull().orEmpty()
    val detail = rawBody
        .takeIf { it.isNotBlank() }
        ?.let { body -> runCatching { gson.fromJson(body, ApiErrorResponse::class.java) }.getOrNull() }
        ?.detail
        ?.takeIf { it.isNotBlank() }

    return detail ?: rawBody.takeIf { it.isNotBlank() } ?: fallback
}

fun Throwable.isUnauthorized(): Boolean {
    return when (this) {
        is ApiException -> code == 401
        is HttpException -> code() == 401
        else -> false
    }
}

fun Throwable.toUserFacingMessage(defaultMessage: String): String {
    return when (this) {
        is UnknownHostException -> "Cannot reach the server. Check your internet connection or confirm the backend URL."
        is SocketTimeoutException -> "The server took too long to respond. Please try again."
        is ConnectException -> "Could not connect to the server. Please try again in a moment."
        is IOException -> "A network error occurred. Check your connection and try again."
        is ApiException -> message
        is HttpException -> message()
        else -> message ?: defaultMessage
    }
}
