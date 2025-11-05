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
import org.oleg.ai.challenge.data.network.createConversationRequest
import org.oleg.ai.challenge.data.network.json
import org.oleg.ai.challenge.data.network.service.ChatApiService
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class DefaultChatComponent(
    componentContext: ComponentContext,
    private val chatApiService: ChatApiService,
    private val onNavigateBack: () -> Unit,
    initialSystemPrompt: String = "",
    initialAssistantPrompt: String = "",
) : ChatComponent, ComponentContext by componentContext {

    private val logger = Logger.withTag("DefaultChatComponent")
    private val scope = coroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    private val _messages = MutableValue<List<ChatMessage>>(emptyList())
    override val messages: Value<List<ChatMessage>> = _messages

    private val _inputText = MutableValue("")
    override val inputText: Value<String> = _inputText

    private val _isLoading = MutableValue(false)
    override val isLoading: Value<Boolean> = _isLoading

    private val _isPromptDialogVisible = MutableValue(false)
    override val isPromptDialogVisible: Value<Boolean> = _isPromptDialogVisible

    private val _systemPrompt = MutableValue(initialSystemPrompt)
    override val systemPrompt: Value<String> = _systemPrompt

    private val _assistantPrompt = MutableValue(initialAssistantPrompt)
    override val assistantPrompt: Value<String> = _assistantPrompt

    init {
        // Add initial system and assistant prompts as hidden messages
        val initialMessages = mutableListOf<ChatMessage>()

        if (initialSystemPrompt.isNotEmpty()) {
            initialMessages.add(
                ChatMessage(
                    id = generateId(),
                    text = InputText.System(initialSystemPrompt),
                    isFromUser = false,
                    role = MessageRole.SYSTEM,
                    isVisibleInUI = false
                )
            )
        }

        if (initialAssistantPrompt.isNotEmpty()) {
            initialMessages.add(
                ChatMessage(
                    id = generateId(),
                    text = InputText.Assistant(prompt = initialAssistantPrompt),
                    isFromUser = false,
                    role = MessageRole.ASSISTANT,
                    isVisibleInUI = false
                )
            )
        }

        _messages.value = initialMessages
    }

    override fun onTextChanged(text: String) {
        _inputText.value = text
    }

    override fun onSendMessage() {
        val text = _inputText.value.trim()
        if (text.isEmpty() || _isLoading.value) return

        // Add user message to chat
        val userMessage = ChatMessage(
            id = generateId(),
            text = InputText.User(text),
            isFromUser = true,
            role = MessageRole.USER,
            isVisibleInUI = true
        )

        _messages.value = _messages.value + userMessage
        _inputText.value = ""
        _isLoading.value = true

        logger.d { "Sending user message: $text" }

        // Send API request
        scope.launch {
            try {
                // Convert UI messages to API messages
                val apiMessages = _messages.value.map { uiMessage ->
                    val apiRole = when (uiMessage.role) {
                        MessageRole.SYSTEM -> org.oleg.ai.challenge.data.network.model.MessageRole.SYSTEM
                        MessageRole.ASSISTANT -> org.oleg.ai.challenge.data.network.model.MessageRole.ASSISTANT
                        MessageRole.USER -> org.oleg.ai.challenge.data.network.model.MessageRole.USER
                        null -> if (uiMessage.isFromUser) {
                            org.oleg.ai.challenge.data.network.model.MessageRole.USER
                        } else {
                            org.oleg.ai.challenge.data.network.model.MessageRole.ASSISTANT
                        }
                    }
                    org.oleg.ai.challenge.data.network.model.ChatMessage(
                        role = apiRole,
                        content = when (val text = uiMessage.text) {
                            is InputText.Assistant -> text.prompt ?: text.content!!
                            is InputText.System -> text.text
                            is InputText.User -> text.text
                        }
                    )
                }

                // Create request with full conversation history
                val request = createConversationRequest(apiMessages)
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
                                // Format JSON if the response is JSON, otherwise keep as-is
                                val formattedText = json.decodeFromString<InputText.Assistant>(choice.message.content)
                                val aiMessage = ChatMessage(
                                    id = generateId(),
                                    text = formattedText,
                                    isFromUser = false,
                                    role = MessageRole.ASSISTANT,
                                    isVisibleInUI = true
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

    override fun onShowPromptDialog() {
        _isPromptDialogVisible.value = true
    }

    override fun onDismissPromptDialog() {
        _isPromptDialogVisible.value = false
    }

    override fun onSavePrompts(systemPrompt: String, assistantPrompt: String) {
        // If system prompt has changed, insert a new SYSTEM message into the conversation
        if (systemPrompt != _systemPrompt.value && systemPrompt.isNotEmpty()) {
            val newSystemMessage = ChatMessage(
                id = generateId(),
                text = InputText.System(systemPrompt),
                isFromUser = false,
                role = MessageRole.SYSTEM,
                isVisibleInUI = false
            )
            _messages.value = _messages.value + newSystemMessage
        }

        _systemPrompt.value = systemPrompt
        _assistantPrompt.value = assistantPrompt
        _isPromptDialogVisible.value = false
    }

    override fun onClearPrompts() {
        _systemPrompt.value = ""
        _assistantPrompt.value = ""
    }

    /**
     * Adds an error message to the chat as a system message.
     */
    private fun addErrorMessage(errorText: String) {
        val errorMessage = ChatMessage(
            id = generateId(),
            text = InputText.User(text = errorText),
            isFromUser = false
        )
        _messages.value = _messages.value + errorMessage
    }

    @OptIn(ExperimentalTime::class)
    private fun generateId(): String =
        "${Clock.System.now()}_${(0..1000).random()}"
}
