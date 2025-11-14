package org.oleg.ai.challenge.data.database.converters

import androidx.room.TypeConverter
import org.oleg.ai.challenge.data.model.MessageRole

/**
 * Room TypeConverters for converting complex types to/from database-compatible types.
 */
class Converters {

    /**
     * Converts MessageRole enum to String for database storage.
     */
    @TypeConverter
    fun fromMessageRole(role: MessageRole?): String? {
        return role?.name
    }

    /**
     * Converts String from database to MessageRole enum.
     */
    @TypeConverter
    fun toMessageRole(value: String?): MessageRole? {
        return value?.let { MessageRole.valueOf(it) }
    }
}
