package org.oleg.ai.challenge.data.database

import androidx.room.Room
import androidx.room.RoomDatabase
import org.oleg.ai.challenge.data.database.migrations.MIGRATION_1_2
import java.io.File

/**
 * JVM/Desktop implementation of database builder.
 */
actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    // Store database in user's home directory under .aichallenge folder
    val dbFile = File(System.getProperty("user.home"), ".aichallenge/${AppDatabase.DATABASE_NAME}")

    // Ensure parent directory exists
    dbFile.parentFile?.mkdirs()

    return Room.databaseBuilder<AppDatabase>(
        name = dbFile.absolutePath
    ).addMigrations(MIGRATION_1_2)
}
