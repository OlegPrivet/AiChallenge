package org.oleg.ai.challenge.component.userprofile

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.oleg.ai.challenge.data.settings.UserProfileService

/**
 * Default implementation of UserProfileComponent.
 * Manages user profile state and delegates persistence to UserProfileService.
 */
class DefaultUserProfileComponent(
    componentContext: ComponentContext,
    private val userProfileService: UserProfileService,
    private val onBack: () -> Unit
) : UserProfileComponent, ComponentContext by componentContext {

    private val scope = coroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    private val _state = MutableValue(UserProfileState.from(userProfileService.loadProfile()))
    override val state: Value<UserProfileState> = _state

    init {
        // Subscribe to profile changes from the service
        userProfileService.userProfile
            .onEach { profile ->
                _state.value = UserProfileState.from(profile)
            }
            .launchIn(scope)
    }

    override fun updateName(name: String) {
        userProfileService.updateName(name)
    }

    override fun updatePreferences(preferences: String) {
        userProfileService.updatePreferences(preferences)
    }

    override fun clearProfile() {
        userProfileService.clearProfile()
    }

    override fun onBack() = onBack.invoke()
}
