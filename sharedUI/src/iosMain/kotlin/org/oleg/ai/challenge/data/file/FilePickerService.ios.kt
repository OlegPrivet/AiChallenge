package org.oleg.ai.challenge.data.file

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UniformTypeIdentifiers.UTTypeItem
import platform.UniformTypeIdentifiers.UTTypePDF
import platform.UniformTypeIdentifiers.UTTypePlainText
import platform.darwin.NSObject
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
actual class FilePickerService {
    actual suspend fun pickFiles(
        allowedExtensions: List<String>,
        allowMultiple: Boolean
    ): List<PickedFile> = suspendCancellableCoroutine { continuation ->

        val documentTypes = allowedExtensions.mapNotNull { ext ->
            when (ext.lowercase()) {
                "txt" -> UTTypePlainText
                "pdf" -> UTTypePDF
                "md" -> UTTypePlainText // Markdown files are treated as plain text
                else -> null
            }
        }.ifEmpty { listOf(UTTypeItem) }

        val picker = UIDocumentPickerViewController(
            forOpeningContentTypes = documentTypes,
            asCopy = true
        )

        picker.allowsMultipleSelection = allowMultiple

        val delegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
            override fun documentPicker(
                controller: UIDocumentPickerViewController,
                didPickDocumentsAtURLs: List<*>
            ) {
                @Suppress("UNCHECKED_CAST")
                val urls = didPickDocumentsAtURLs as List<NSURL>
                val files = urls.mapNotNull { url -> readFile(url) }
                continuation.resume(files)
            }

            override fun documentPickerWasCancelled(
                controller: UIDocumentPickerViewController
            ) {
                continuation.resume(emptyList())
            }
        }

        picker.delegate = delegate

        val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
        rootViewController?.presentViewController(picker, animated = true, completion = null)

        continuation.invokeOnCancellation {
            picker.dismissViewControllerAnimated(true, completion = null)
        }
    }

    private fun readFile(url: NSURL): PickedFile? {
        return try {
            val data = NSData.dataWithContentsOfURL(url) ?: return null
            val bytes = ByteArray(data.length.toInt())
            data.getBytes(bytes.refTo(0).getPointer(null), data.length)

            val fileName = url.lastPathComponent ?: "unknown"
            val pathExtension = url.pathExtension?.lowercase()
            val mimeType = when (pathExtension) {
                "txt" -> "text/plain"
                "pdf" -> "application/pdf"
                "md" -> "text/markdown"
                else -> null
            }

            PickedFile(
                name = fileName,
                content = bytes,
                mimeType = mimeType
            )
        } catch (e: Exception) {
            println("Error reading file: ${e.message}")
            null
        }
    }
}
