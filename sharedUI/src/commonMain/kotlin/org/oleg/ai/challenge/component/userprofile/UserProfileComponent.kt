package org.oleg.ai.challenge.component.userprofile

import com.arkivanov.decompose.value.Value
import org.oleg.ai.challenge.data.settings.UserProfile

/**
 * State representation for the UI layer.
 */
data class UserProfileState(
    val name: String = "",
    val preferences: String = "",
    val isComplete: Boolean = false
) {
    companion object {
        /**
         * Factory method to convert from domain model to UI state.
         */
        fun from(profile: UserProfile): UserProfileState {
            return UserProfileState(
                name = profile.name,
                preferences = profile.preferences,
                isComplete = profile.isComplete()
            )
        }
    }
}

/**
 * Component interface for user profile management.
 * Allows users to input their name and preferences for AI personalization.
 */
interface UserProfileComponent {
    /**
     * Observable state for the UI.
     */
    val state: Value<UserProfileState>

    /**
     * Updates the user's name.
     */
    fun updateName(name: String)

    /**
     * Updates the user's preferences/description.
     */
    fun updatePreferences(preferences: String)

    /**
     * Clears the entire profile.
     */
    fun clearProfile()

    /**
     * Navigate back from this screen.
     */
    fun onBack()
}
