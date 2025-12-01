package org.oleg.ai.challenge.di

import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.oleg.ai.challenge.data.database.di.databaseModule
import org.oleg.ai.challenge.data.file.di.filePlatformModules
import org.oleg.ai.challenge.data.network.di.networkModule
import org.oleg.ai.challenge.data.rag.di.ragModule
import org.oleg.ai.challenge.data.rag.di.ragPlatformModules
import org.oleg.ai.challenge.data.settings.di.settingsModule

fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration()
    modules(
        listOf(
            componentModule,
            networkModule,
            databaseModule,
            ragModule,
            settingsModule,
            commandModule
        ) + ragPlatformModules() + filePlatformModules()
    )
}
