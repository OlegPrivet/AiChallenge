package org.oleg.ai.challenge.data.network

/**
 * A sealed class representing the result of an API call.
 * This provides type-safe error handling and makes it explicit when operations can fail.
 */
sealed class ApiResult<out T> {
    /**
     * Represents a successful API call with data.
     * @property data The successfully retrieved data
     */
    data class Success<T>(val data: T) : ApiResult<T>()

    /**
     * Represents a failed API call with an error.
     * @property error The error that occurred
     */
    data class Error(val error: ApiError) : ApiResult<Nothing>()

    /**
     * Returns true if this result is a Success, false otherwise.
     */
    fun isSuccess(): Boolean = this is Success

    /**
     * Returns true if this result is an Error, false otherwise.
     */
    fun isError(): Boolean = this is Error

    /**
     * Returns the data if this is a Success, null otherwise.
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }

    /**
     * Returns the error if this is an Error, null otherwise.
     */
    fun errorOrNull(): ApiError? = when (this) {
        is Success -> null
        is Error -> error
    }

    /**
     * Transforms the data if this is a Success using the provided transform function.
     */
    inline fun <R> map(transform: (T) -> R): ApiResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
    }

    /**
     * Executes the given action if this is a Success.
     */
    inline fun onSuccess(action: (T) -> Unit): ApiResult<T> {
        if (this is Success) action(data)
        return this
    }

    /**
     * Executes the given action if this is an Error.
     */
    inline fun onError(action: (ApiError) -> Unit): ApiResult<T> {
        if (this is Error) action(error)
        return this
    }
}

/**
 * Sealed class representing different types of API errors.
 */
sealed class ApiError {
    /**
     * Network-related errors (no internet, timeout, etc.)
     * @property message Human-readable error message
     * @property cause The underlying exception, if any
     */
    data class NetworkError(
        val message: String,
        val cause: Throwable? = null
    ) : ApiError()

    /**
     * HTTP errors with status codes (4xx, 5xx)
     * @property statusCode The HTTP status code
     * @property message Human-readable error message
     * @property body The response body, if available
     */
    data class HttpError(
        val statusCode: Int,
        val message: String,
        val body: String? = null
    ) : ApiError()

    /**
     * JSON parsing/serialization errors
     * @property message Human-readable error message
     * @property cause The underlying exception
     */
    data class SerializationError(
        val message: String,
        val cause: Throwable? = null
    ) : ApiError()

    /**
     * Unknown or unexpected errors
     * @property message Human-readable error message
     * @property cause The underlying exception
     */
    data class UnknownError(
        val message: String,
        val cause: Throwable? = null
    ) : ApiError()

    /**
     * Returns a human-readable description of the error.
     */
    fun getDescription(): String = when (this) {
        is NetworkError -> message
        is HttpError -> "$statusCode: $message"
        is SerializationError -> "Parsing error: $message"
        is UnknownError -> "Unknown error: $message"
    }
}