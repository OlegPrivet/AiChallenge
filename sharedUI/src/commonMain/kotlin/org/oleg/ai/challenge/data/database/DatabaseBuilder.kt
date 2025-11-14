package org.oleg.ai.challenge.data.database

import androidx.room.RoomDatabase

/**
 * Expected function to get a platform-specific database builder.
 * Each platform (Android, JVM, iOS) provides its own implementation.
 */
expect fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase>
