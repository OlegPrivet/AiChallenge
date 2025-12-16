package org.oleg.ai.challenge.data.audio.di

import org.koin.core.module.Module
import org.koin.dsl.module
import org.oleg.ai.challenge.data.audio.SpeechRecognizer

val audioAndroidModule = module {
    single { SpeechRecognizer() }
}

actual fun audioPlatformModules(): List<Module> = listOf(audioAndroidModule)
