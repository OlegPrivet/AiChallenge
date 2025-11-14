package org.oleg.ai.challenge.di

import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.oleg.ai.challenge.data.database.di.databaseModule
import org.oleg.ai.challenge.data.network.di.networkModule

fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration()
    modules(componentModule)
    modules(networkModule)
    modules(databaseModule)
}
