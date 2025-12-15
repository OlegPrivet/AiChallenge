package org.oleg.ai.challenge.data.settings

import kotlinx.serialization.Serializable

/**
 * User profile data containing name and preferences.
 * This information is injected as a system prompt when creating new chats (if complete).
 */
@Serializable
data class UserProfile(
    val name: String = "",
    val preferences: String = ""
) {
    /**
     * Checks if the profile is complete (both name and preferences are non-empty).
     * Only complete profiles are injected as system prompts.
     */
    fun isComplete(): Boolean = name.isNotBlank() && preferences.isNotBlank()

    /**
     * Formats the profile into a system prompt message for AI.
     */
    fun toSystemPrompt(): String {
        return "ДЛЯ ОБРАЩЕНИЙ КО МНЕ ИСПОЛЬЗУЙ ИМЯ $name. ПЕРСОНАЛИЗИРОВАННАЯ ИНФОРМАЦИЯ ОБО МНЕ: $preferences"
    }

    companion object {
        const val MAX_NAME_LENGTH = 100
        const val MAX_PREFERENCES_LENGTH = 1000
    }
}
