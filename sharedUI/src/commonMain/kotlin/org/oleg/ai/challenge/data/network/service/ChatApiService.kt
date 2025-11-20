package org.oleg.ai.challenge.data.network.service

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerializationException
import org.oleg.ai.challenge.data.network.ApiError
import org.oleg.ai.challenge.data.network.ApiResult
import org.oleg.ai.challenge.data.network.model.ChatRequest
import org.oleg.ai.challenge.data.network.model.ChatResponse

/**
 * Interface for chat API operations.
 * Defines the contract for communicating with the OpenRouter chat completion API.
 */
interface ChatApiService {
    /**
     * Sends a chat completion request to the API.
     *
     * @param request The chat request containing model and messages
     * @return ApiResult containing either the successful ChatResponse or an ApiError
     */
    suspend fun sendChatCompletion(request: ChatRequest): ApiResult<ChatResponse>
}

/**
 * Implementation of ChatApiService using Ktor HttpClient.
 *
 * @property httpClient The configured HttpClient instance
 * @property logger Logger instance for debugging
 */
class ChatApiServiceImpl(
    private val httpClient: HttpClient,
    private val mcpClientService: McpClientService,
    private val logger: Logger = Logger.withTag("ChatApiService")
) : ChatApiService {

    companion object {
        private const val CHAT_COMPLETIONS_ENDPOINT = "chat/completions"
    }

    override suspend fun sendChatCompletion(request: ChatRequest): ApiResult<ChatResponse> {
        return try {
            logger.d { "Sending chat completion request for model: ${request.model}" }
            logger.v { "Request payload: $request" }

            val response: HttpResponse = httpClient.post(CHAT_COMPLETIONS_ENDPOINT) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            logger.d { "Received response with status: ${response.status}" }

            val chatResponse = response.body<ChatResponse>()
            logger.v { "Parsed response: $chatResponse" }
            logger.d { "Completion tokens used: ${chatResponse.usage.totalTokens}" }

            ApiResult.Success(chatResponse)

        } catch (e: io.ktor.client.plugins.ClientRequestException) {
            // 4xx errors
            logger.e(e) { "Client error during chat completion: ${e.response.status}" }
            handleHttpError(e.response, e)

        } catch (e: io.ktor.client.plugins.ServerResponseException) {
            // 5xx errors
            logger.e(e) { "Server error during chat completion: ${e.response.status}" }
            handleHttpError(e.response, e)

        } catch (e: SerializationException) {
            logger.e(e) { "Failed to parse API response" }
            ApiResult.Error(
                ApiError.SerializationError(
                    message = "Failed to parse server response: ${e.message ?: "Unknown error"}",
                    cause = e
                )
            )

        } catch (e: io.ktor.client.plugins.HttpRequestTimeoutException) {
            logger.e(e) { "Request timeout" }
            ApiResult.Error(
                ApiError.NetworkError(
                    message = "Request timeout. Please check your internet connection and try again.",
                    cause = e
                )
            )

        } catch (e: io.ktor.client.network.sockets.ConnectTimeoutException) {
            logger.e(e) { "Connection timeout" }
            ApiResult.Error(
                ApiError.NetworkError(
                    message = "Connection timeout. Please check your internet connection.",
                    cause = e
                )
            )

        } catch (e: io.ktor.client.network.sockets.SocketTimeoutException) {
            logger.e(e) { "Socket timeout" }
            ApiResult.Error(
                ApiError.NetworkError(
                    message = "Network timeout. The server took too long to respond.",
                    cause = e
                )
            )

        } catch (e: Exception) {
            logger.e(e) { "Unexpected error during chat completion" }
            ApiResult.Error(
                ApiError.UnknownError(
                    message = e.message ?: "An unexpected error occurred",
                    cause = e
                )
            )
        }
    }

    /**
     * Handles HTTP error responses by extracting status code and body.
     */
    private suspend fun handleHttpError(
        response: HttpResponse,
        exception: Exception
    ): ApiResult<Nothing> {
        val statusCode = response.status.value
        val errorBody = try {
            response.bodyAsText()
        } catch (e: Exception) {
            logger.w(e) { "Failed to read error response body" }
            null
        }

        return ApiResult.Error(
            ApiError.HttpError(
                statusCode = statusCode,
                message = getHttpErrorMessage(statusCode),
                body = errorBody
            )
        )
    }

    /**
     * Provides human-readable error messages based on HTTP status codes.
     */
    private fun getHttpErrorMessage(statusCode: Int): String = when (statusCode) {
        400 -> "Bad Request: The request was invalid"
        401 -> "Unauthorized: Invalid API key"
        403 -> "Forbidden: Access denied"
        404 -> "Not Found: The requested resource was not found"
        429 -> "Too Many Requests: Rate limit exceeded"
        500 -> "Internal Server Error: The server encountered an error"
        502 -> "Bad Gateway: Invalid response from the server"
        503 -> "Service Unavailable: The service is temporarily unavailable"
        else -> "HTTP Error $statusCode"
    }
}
