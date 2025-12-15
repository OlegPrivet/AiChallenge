package org.oleg.ai.challenge.data.settings.di

import com.russhwolf.settings.Settings
import org.koin.dsl.module
import org.oleg.ai.challenge.data.settings.RagSettingsService
import org.oleg.ai.challenge.data.settings.UserProfileService

val settingsModule = module {
    single { Settings() }
    single { RagSettingsService(get()) }
    single { UserProfileService(get()) }
}
