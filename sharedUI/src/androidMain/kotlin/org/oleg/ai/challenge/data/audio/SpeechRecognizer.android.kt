package org.oleg.ai.challenge.data.audio

actual class SpeechRecognizer {
    actual suspend fun startRecording() {
        throw UnsupportedOperationException("Speech recognition not available on Android")
    }

    actual suspend fun stopRecordingAndTranscribe(): String {
        throw UnsupportedOperationException("Speech recognition not available on Android")
    }

    actual fun isSupported(): Boolean = false

    actual fun cancelRecording() {
        // No-op
    }
}
