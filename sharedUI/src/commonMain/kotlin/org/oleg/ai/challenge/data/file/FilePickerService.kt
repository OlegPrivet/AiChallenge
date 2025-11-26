package org.oleg.ai.challenge.data.file

/**
 * Represents a picked file with its content and metadata
 */
data class PickedFile(
    val name: String,
    val content: ByteArray,
    val mimeType: String?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as PickedFile

        if (name != other.name) return false
        if (!content.contentEquals(other.content)) return false
        if (mimeType != other.mimeType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + content.contentHashCode()
        result = 31 * result + (mimeType?.hashCode() ?: 0)
        return result
    }
}

/**
 * Service for picking files from the file system
 * Platform-specific implementations will handle the actual file selection
 */
expect class FilePickerService {
    /**
     * Opens a file picker dialog and returns the selected file(s)
     * @param allowedExtensions List of allowed file extensions (e.g., ["txt", "pdf", "md"])
     * @param allowMultiple Whether to allow selecting multiple files
     * @return List of picked files, empty if cancelled
     */
    suspend fun pickFiles(
        allowedExtensions: List<String> = listOf("txt", "pdf", "md"),
        allowMultiple: Boolean = false
    ): List<PickedFile>
}
