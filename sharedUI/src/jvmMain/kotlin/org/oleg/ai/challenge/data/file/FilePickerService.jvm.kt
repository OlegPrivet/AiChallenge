package org.oleg.ai.challenge.data.file

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

actual class FilePickerService {
    actual suspend fun pickFiles(
        allowedExtensions: List<String>,
        allowMultiple: Boolean
    ): List<PickedFile> = withContext(Dispatchers.IO) {
        val dialog = FileDialog(null as Frame?, "Select Files", FileDialog.LOAD).apply {
            // Set file filter if extensions are specified
            if (allowedExtensions.isNotEmpty()) {
                val filter = allowedExtensions.joinToString(", ") { "*.$it" }
                setFilenameFilter { _, name ->
                    allowedExtensions.any { ext ->
                        name.endsWith(".$ext", ignoreCase = true)
                    }
                }
                // Note: FileDialog title can show the filter hint
                title = "Select Files ($filter)"
            }

            isMultipleMode = allowMultiple
            isVisible = true
        }

        val files = dialog.files
        if (files == null || files.isEmpty()) {
            return@withContext emptyList()
        }

        files.mapNotNull { file ->
            try {
                PickedFile(
                    name = file.name,
                    content = file.readBytes(),
                    mimeType = guessMimeType(file)
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private fun guessMimeType(file: File): String? {
        return when (file.extension.lowercase()) {
            "txt" -> "text/plain"
            "pdf" -> "application/pdf"
            "md" -> "text/markdown"
            "html" -> "text/html"
            "json" -> "application/json"
            "xml" -> "application/xml"
            else -> null
        }
    }
}
