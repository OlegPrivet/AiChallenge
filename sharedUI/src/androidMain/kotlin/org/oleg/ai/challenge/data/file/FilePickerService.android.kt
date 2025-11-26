package org.oleg.ai.challenge.data.file

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual class FilePickerService(private val activity: ComponentActivity) {
    private var currentContinuation: ((List<PickedFile>) -> Unit)? = null

    private val filePickerLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val files = if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val uris = mutableListOf<Uri>()

            data?.clipData?.let { clipData ->
                // Multiple files selected
                for (i in 0 until clipData.itemCount) {
                    clipData.getItemAt(i).uri?.let { uris.add(it) }
                }
            } ?: data?.data?.let { uri ->
                // Single file selected
                uris.add(uri)
            }

            uris.mapNotNull { uri -> readFile(uri) }
        } else {
            emptyList()
        }

        currentContinuation?.invoke(files)
        currentContinuation = null
    }

    actual suspend fun pickFiles(
        allowedExtensions: List<String>,
        allowMultiple: Boolean
    ): List<PickedFile> = suspendCancellableCoroutine { continuation ->
        currentContinuation = { files ->
            continuation.resume(files)
        }

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiple)

            // Set MIME types based on allowed extensions
            if (allowedExtensions.isNotEmpty()) {
                val mimeTypes = allowedExtensions.mapNotNull { ext ->
                    when (ext.lowercase()) {
                        "txt" -> "text/plain"
                        "pdf" -> "application/pdf"
                        "md" -> "text/markdown"
                        else -> null
                    }
                }.toTypedArray()

                if (mimeTypes.isNotEmpty()) {
                    putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
                }
            }
        }

        filePickerLauncher.launch(intent)

        continuation.invokeOnCancellation {
            currentContinuation = null
        }
    }

    private fun readFile(uri: Uri): PickedFile? {
        return try {
            val contentResolver = activity.contentResolver
            val cursor = contentResolver.query(uri, null, null, null, null)
            val fileName = cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) it.getString(nameIndex) else "unknown"
                } else "unknown"
            } ?: "unknown"

            val content = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return null

            val mimeType = contentResolver.getType(uri)

            PickedFile(
                name = fileName,
                content = content,
                mimeType = mimeType
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
