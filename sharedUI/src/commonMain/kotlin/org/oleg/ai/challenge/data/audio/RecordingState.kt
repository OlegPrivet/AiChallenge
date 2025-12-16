package org.oleg.ai.challenge.data.audio

/**
 * Represents the current state of audio recording.
 */
sealed interface RecordingState {
    /**
     * No recording in progress
     */
    data object Idle : RecordingState

    /**
     * Currently recording audio
     */
    data object Recording : RecordingState

    /**
     * Processing recorded audio (transcribing)
     */
    data object Processing : RecordingState

    /**
     * Recording completed with transcription result
     */
    data class Completed(val transcription: String) : RecordingState

    /**
     * Error occurred during recording or transcription
     */
    data class Error(val message: String) : RecordingState
}
