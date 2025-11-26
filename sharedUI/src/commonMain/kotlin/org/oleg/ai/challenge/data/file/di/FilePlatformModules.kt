package org.oleg.ai.challenge.data.file.di

import org.koin.core.module.Module

/**
 * Platform-specific file picker modules
 */
expect fun filePlatformModules(): List<Module>
