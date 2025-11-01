package org.oleg.ai.challenge.component.chat

import co.touchlab.kermit.Logger
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.oleg.ai.challenge.data.network.ApiResult
import org.oleg.ai.challenge.data.network.createSimpleUserRequest
import org.oleg.ai.challenge.data.network.service.ChatApiService
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class DefaultChatComponent(
    componentContext: ComponentContext,
    private val chatApiService: ChatApiService,
    private val onNavigateBack: () -> Unit
) : ChatComponent, ComponentContext by componentContext {

    private val logger = Logger.withTag("DefaultChatComponent")
    private val scope = coroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    private val _messages = MutableValue<List<ChatMessage>>(emptyList())
    override val messages: Value<List<ChatMessage>> = _messages

    private val _inputText = MutableValue("")
    override val inputText: Value<String> = _inputText

    private val _isLoading = MutableValue(false)
    override val isLoading: Value<Boolean> = _isLoading

    override fun onTextChanged(text: String) {
        _inputText.value = text
    }

    override fun onSendMessage() {
        val text = _inputText.value.trim()
        if (text.isEmpty() || _isLoading.value) return

        // Add user message to chat
        val userMessage = ChatMessage(
            id = generateId(),
            text = text,
            isFromUser = true
        )

        _messages.value = _messages.value + userMessage
        _inputText.value = ""
        _isLoading.value = true

        logger.d { "Sending user message: $text" }

        // Send API request
        scope.launch {
            try {
                val request = createSimpleUserRequest(text)
                val result = chatApiService.sendChatCompletion(request)

                when (result) {
                    is ApiResult.Success -> {
                        // Extract AI response from choices[0].message.content
                        if (result.data.choices.isEmpty()) {
                            logger.w { "AI response content is null" }
                            addErrorMessage("No response received from AI")
                        } else {
                            result.data.choices.forEach { choice ->
                                logger.d { "Received AI response: ${choice.message.content}" }
                                val aiMessage = ChatMessage(
                                    id = generateId(),
                                    text = choice.message.content,
                                    isFromUser = false
                                )
                                _messages.value = _messages.value + aiMessage
                            }
                        }
                    }

                    is ApiResult.Error -> {
                        logger.e { "API error: ${result.error.getDescription()}" }
                        addErrorMessage("Error: ${result.error.getDescription()}")
                    }
                }
            } catch (e: Exception) {
                logger.e(e) { "Unexpected error during API call" }
                addErrorMessage("Unexpected error: ${e.message ?: "Unknown error"}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    override fun onNavigateBack() {
        onNavigateBack.invoke()
    }

    /**
     * Adds an error message to the chat as a system message.
     */
    private fun addErrorMessage(errorText: String) {
        val errorMessage = ChatMessage(
            id = generateId(),
            text = errorText,
            isFromUser = false
        )
        _messages.value = _messages.value + errorMessage
    }

    @OptIn(ExperimentalTime::class)
    private fun generateId(): String =
        "${Clock.System.now()}_${(0..1000).random()}"
}
