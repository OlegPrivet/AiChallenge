package org.oleg.ai.challenge.data.audio

/**
 * Service for speech recognition from audio input.
 * Platform-specific implementations handle audio recording and transcription.
 */
expect class SpeechRecognizer {
    /**
     * Starts recording audio from the microphone.
     * Audio is kept in memory only.
     * @throws UnsupportedOperationException if not supported on this platform
     * @throws IllegalStateException if already recording
     */
    suspend fun startRecording()

    /**
     * Stops recording and returns the transcribed text.
     * Audio data is immediately deleted after transcription.
     * @return Transcribed text, or empty string if no speech detected
     * @throws UnsupportedOperationException if not supported on this platform
     * @throws IllegalStateException if not currently recording
     */
    suspend fun stopRecordingAndTranscribe(): String

    /**
     * Checks if this platform supports speech recognition.
     */
    fun isSupported(): Boolean

    /**
     * Cancels recording without transcribing.
     * Immediately deletes any recorded audio data.
     */
    fun cancelRecording()
}
