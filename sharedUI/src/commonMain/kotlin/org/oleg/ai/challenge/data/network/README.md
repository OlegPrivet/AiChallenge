# Network Layer Documentation

This directory contains the complete network layer implementation for AI chat communication using Ktor in this Kotlin Multiplatform project.

## Overview

The network layer provides:
- Type-safe API communication with OpenRouter's chat completion API
- Platform-specific HTTP engines (OkHttp for Android/JVM, Darwin for iOS)
- Comprehensive error handling with domain-specific error types
- JSON serialization using kotlinx.serialization
- Logging and debugging capabilities
- Dependency injection support via Koin

## Project Structure

```
data/network/
├── model/
│   ├── MessageRole.kt         # Enum for message roles (USER, ASSISTANT, SYSTEM)
│   ├── ChatRequest.kt         # Request data models
│   └── ChatResponse.kt        # Response data models
├── service/
│   └── ChatApiService.kt      # API service interface and implementation
├── di/
│   └── NetworkModule.kt       # Koin dependency injection module
├── ApiResult.kt               # Result wrapper for type-safe error handling
├── ChatApiExtensions.kt       # Extension functions for easy API usage
└── HttpClientFactory.kt       # Ktor HttpClient configuration
```

Platform-specific implementations:
```
androidMain/kotlin/.../network/
└── HttpClientFactory.android.kt  # OkHttp engine for Android

jvmMain/kotlin/.../network/
└── HttpClientFactory.jvm.kt      # OkHttp engine for Desktop

iosMain/kotlin/.../network/
└── HttpClientFactory.ios.kt      # Darwin engine for iOS
```

## Configuration

### API Key Setup

The API key is retrieved from BuildConfig, which reads from:
1. `local.properties` file (recommended for development)
2. Environment variable
3. Default placeholder (if neither is set)

**Option 1: Using local.properties (Recommended)**
Create or edit `local.properties` in the project root:
```properties
OPENROUTER_API_KEY=sk-or-v1-your-actual-api-key-here
```

**Option 2: Using Environment Variable**
```bash
export OPENROUTER_API_KEY=sk-or-v1-your-actual-api-key-here
```

### Koin Module Registration

Add the network module to your Koin configuration:

```kotlin
import org.koin.core.context.startKoin
import org.oleg.ai.challenge.data.network.di.networkModule

startKoin {
    modules(
        networkModule,
        // ... other modules
    )
}
```

## Usage Examples

### Basic Usage with Dependency Injection

```kotlin
import org.koin.compose.koinInject
import org.oleg.ai.challenge.data.network.service.ChatApiService
import org.oleg.ai.challenge.data.network.createSimpleUserRequest

@Composable
fun ChatScreen() {
    val chatApiService: ChatApiService = koinInject()

    LaunchedEffect(Unit) {
        val request = createSimpleUserRequest("What is the meaning of life?")

        when (val result = chatApiService.sendChatCompletion(request)) {
            is ApiResult.Success -> {
                val content = result.data.getContent()
                println("AI Response: $content")
            }
            is ApiResult.Error -> {
                val errorMessage = result.error.getDescription()
                println("Error: $errorMessage")
            }
        }
    }
}
```

### Using Extension Functions

**Simple user message:**
```kotlin
val request = createSimpleUserRequest(
    userMessage = "Explain quantum computing in simple terms",
    model = "qwen/qwen3-coder:free" // Optional, defaults to this
)
```

**With system prompt:**
```kotlin
val request = createRequestWithSystemPrompt(
    systemPrompt = "You are a helpful coding assistant.",
    userMessage = "How do I sort a list in Kotlin?"
)
```

**Conversation with history:**
```kotlin
val messages = listOf(
    systemMessage("You are a helpful assistant."),
    userMessage("What is Kotlin?"),
    assistantMessage("Kotlin is a modern programming language..."),
    userMessage("Tell me more about coroutines")
)

val request = createConversationRequest(messages)
```

**Building messages incrementally:**
```kotlin
val messages = emptyList<ChatMessage>()
    .addSystemMessage("You are a helpful assistant.")
    .addUserMessage("Hello!")
    .addAssistantMessage("Hi! How can I help you?")
    .addUserMessage("What's the weather?")

val request = createConversationRequest(messages)
```

### Manual HttpClient Usage (Without DI)

```kotlin
import org.oleg.ai.challenge.BuildConfig
import org.oleg.ai.challenge.data.network.HttpClientFactory
import org.oleg.ai.challenge.data.network.service.ChatApiServiceImpl

val httpClient = HttpClientFactory.create(
    apiKey = BuildConfig.OPENROUTER_API_KEY,
    enableLogging = true
)

val chatApiService = ChatApiServiceImpl(httpClient)

// Use the service
val result = chatApiService.sendChatCompletion(request)
```

### Handling Different Response Types

```kotlin
when (val result = chatApiService.sendChatCompletion(request)) {
    is ApiResult.Success -> {
        val response = result.data

        // Extract main content
        val content = response.getContent()

        // Access token usage
        println("Tokens used: ${response.usage.totalTokens}")
        println("Prompt tokens: ${response.usage.promptTokens}")
        println("Completion tokens: ${response.usage.completionTokens}")

        // Access metadata
        println("Model used: ${response.model}")
        println("Provider: ${response.provider}")
    }

    is ApiResult.Error -> {
        when (val error = result.error) {
            is ApiError.NetworkError -> {
                println("Network issue: ${error.message}")
                // Show retry option
            }
            is ApiError.HttpError -> {
                println("HTTP ${error.statusCode}: ${error.message}")
                if (error.statusCode == 401) {
                    // Invalid API key
                } else if (error.statusCode == 429) {
                    // Rate limit exceeded
                }
            }
            is ApiError.SerializationError -> {
                println("Parsing error: ${error.message}")
                // Log the issue
            }
            is ApiError.UnknownError -> {
                println("Unexpected error: ${error.message}")
            }
        }
    }
}
```

### Using Result Extension Functions

```kotlin
chatApiService.sendChatCompletion(request)
    .onSuccess { response ->
        println("Got response: ${response.getContent()}")
    }
    .onError { error ->
        println("Error occurred: ${error.getDescription()}")
    }

// Transform result
val contentResult = chatApiService.sendChatCompletion(request)
    .map { response -> response.getContent() ?: "No content" }

// Get nullable value
val content: String? = chatApiService.sendChatCompletion(request)
    .getOrNull()
    ?.getContent()
```

### ViewModel Example with Flow

```kotlin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.oleg.ai.challenge.data.network.service.ChatApiService

class ChatViewModel(
    private val chatApiService: ChatApiService
) {
    private val _chatState = MutableStateFlow<ChatUiState>(ChatUiState.Idle)
    val chatState: StateFlow<ChatUiState> = _chatState

    fun sendMessage(userMessage: String) {
        viewModelScope.launch {
            _chatState.value = ChatUiState.Loading

            val request = createSimpleUserRequest(userMessage)

            when (val result = chatApiService.sendChatCompletion(request)) {
                is ApiResult.Success -> {
                    val content = result.data.getContent() ?: "No response"
                    _chatState.value = ChatUiState.Success(content)
                }
                is ApiResult.Error -> {
                    _chatState.value = ChatUiState.Error(
                        result.error.getDescription()
                    )
                }
            }
        }
    }
}

sealed class ChatUiState {
    object Idle : ChatUiState()
    object Loading : ChatUiState()
    data class Success(val message: String) : ChatUiState()
    data class Error(val message: String) : ChatUiState()
}
```

## Error Handling

The network layer provides four types of errors:

1. **NetworkError**: Connection issues, timeouts
2. **HttpError**: HTTP status codes (4xx, 5xx)
3. **SerializationError**: JSON parsing failures
4. **UnknownError**: Unexpected errors

All errors implement `getDescription()` for user-friendly messages.

## Platform-Specific Details

### Android/JVM (OkHttp)
- Connection pooling: 5 max idle connections, 5-minute keep-alive
- Automatic retry on connection failure
- HTTP/2 support
- Efficient SSL/TLS handling

### iOS (Darwin)
- Uses NSURLSession natively
- Integrates with iOS networking stack
- Handles App Transport Security requirements
- Cache policy: Reload ignoring local cache

## Testing

Mock the `ChatApiService` interface for testing:

```kotlin
class MockChatApiService : ChatApiService {
    override suspend fun sendChatCompletion(request: ChatRequest): ApiResult<ChatResponse> {
        return ApiResult.Success(
            ChatResponse(
                id = "test-id",
                provider = "test",
                model = request.model,
                objectType = "chat.completion",
                created = System.currentTimeMillis(),
                choices = listOf(
                    ChatChoice(
                        finishReason = "stop",
                        nativeFinishReason = "stop",
                        index = 0,
                        message = AssistantMessage(
                            role = "assistant",
                            content = "Test response"
                        )
                    )
                ),
                usage = Usage(
                    promptTokens = 10,
                    completionTokens = 20,
                    totalTokens = 30
                )
            )
        )
    }
}
```

## Performance Considerations

- HttpClient is created as a singleton (via Koin) for connection reuse
- Connection pooling reduces overhead
- JSON parsing is lazy and efficient
- Platform-specific engines are optimized for each target
- Timeouts prevent hanging requests

## Security Notes

- API key is read from BuildConfig (not hardcoded)
- Authorization header is sanitized in logs
- HTTPS is enforced (HTTP upgraded automatically)
- SSL/TLS handled by platform engines

## Troubleshooting

**Build error: BuildConfig not found**
- Run `./gradlew :sharedUI:generateBuildConfig` to generate BuildConfig
- Ensure `OPENROUTER_API_KEY` is set in local.properties or environment

**iOS compilation issues**
- Ensure Darwin engine is properly imported
- Check App Transport Security settings

**Timeout errors**
- Adjust timeout values in HttpClientFactory
- Check network connectivity
- Verify API endpoint is reachable

## Available Models

The default model is `qwen/qwen3-coder:free`, but you can use any OpenRouter-supported model:

```kotlin
val request = createSimpleUserRequest(
    userMessage = "Hello",
    model = "deepseek/deepseek-r1:free"  // or any other model
)
```

## API Reference

For more details on the OpenRouter API, visit: https://openrouter.ai/docs