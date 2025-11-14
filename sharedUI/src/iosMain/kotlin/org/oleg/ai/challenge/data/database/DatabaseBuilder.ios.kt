package org.oleg.ai.challenge.data.database

import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/**
 * iOS implementation of database builder.
 */
@OptIn(ExperimentalForeignApi::class)
actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    // Get iOS documents directory
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null
    )

    val dbFilePath = requireNotNull(documentDirectory?.path) { "Failed to get document directory" } +
            "/${AppDatabase.DATABASE_NAME}"

    return Room.databaseBuilder<AppDatabase>(
        name = dbFilePath
    )
}
