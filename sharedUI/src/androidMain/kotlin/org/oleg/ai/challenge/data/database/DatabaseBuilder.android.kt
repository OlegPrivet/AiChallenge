package org.oleg.ai.challenge.data.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import org.oleg.ai.challenge.data.database.migrations.MIGRATION_1_2

private lateinit var applicationContext: Context

/**
 * Initialize the database builder with Android application context.
 * This must be called from the Application class or MainActivity.
 */
fun initializeDatabaseContext(context: Context) {
    applicationContext = context.applicationContext
}

/**
 * Android implementation of database builder.
 */
actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val dbFile = applicationContext.getDatabasePath(AppDatabase.DATABASE_NAME)
    return Room.databaseBuilder<AppDatabase>(
        context = applicationContext,
        name = dbFile.absolutePath
    ).addMigrations(MIGRATION_1_2)
}
