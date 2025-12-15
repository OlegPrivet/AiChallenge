package org.oleg.ai.challenge.data.settings

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Service for managing user profile persistence using multiplatform-settings.
 * Stores user name and preferences that can be injected into AI conversations.
 */
class UserProfileService(private val settings: Settings) {

    private val _userProfile = MutableStateFlow(loadProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    /**
     * Loads the user profile from persistent storage.
     */
    fun loadProfile(): UserProfile {
        return UserProfile(
            name = settings[KEY_USER_NAME, ""],
            preferences = settings[KEY_USER_PREFERENCES, ""]
        )
    }

    /**
     * Saves the user profile to persistent storage and updates the StateFlow.
     */
    fun saveProfile(profile: UserProfile) {
        settings[KEY_USER_NAME] = profile.name
        settings[KEY_USER_PREFERENCES] = profile.preferences
        _userProfile.value = profile
    }

    /**
     * Updates only the user's name.
     */
    fun updateName(name: String) {
        val current = _userProfile.value
        saveProfile(current.copy(name = name))
    }

    /**
     * Updates only the user's preferences.
     */
    fun updatePreferences(preferences: String) {
        val current = _userProfile.value
        saveProfile(current.copy(preferences = preferences))
    }

    /**
     * Clears the user profile (resets to empty).
     */
    fun clearProfile() {
        saveProfile(UserProfile())
    }

    companion object {
        private const val KEY_USER_NAME = "user_profile_name"
        private const val KEY_USER_PREFERENCES = "user_profile_preferences"
    }
}
