package org.oleg.ai.challenge.data.audio.di

import org.koin.core.module.Module

/**
 * Platform-specific audio modules
 */
expect fun audioPlatformModules(): List<Module>
