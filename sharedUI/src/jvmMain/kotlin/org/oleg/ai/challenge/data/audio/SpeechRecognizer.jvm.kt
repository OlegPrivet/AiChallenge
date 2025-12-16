package org.oleg.ai.challenge.data.audio

import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine

actual class SpeechRecognizer {
    private val logger = Logger.withTag("SpeechRecognizer")

    // Audio format: 16kHz, 16-bit, mono (Vosk requirement)
    private val audioFormat = AudioFormat(
        16000f,  // 16kHz sample rate
        16,      // 16 bits per sample
        1,       // mono
        true,    // signed
        false    // little endian
    )

    // In-memory audio buffer (no disk writes)
    private var audioBuffer: ByteArrayOutputStream? = null
    private var targetDataLine: TargetDataLine? = null
    private var isRecording = false

    // Vosk model (lazy-loaded)
    private var model: VoskLite.ModelHandle? = null

    actual suspend fun startRecording() {
        check(!isRecording) { "Already recording" }

        withContext(Dispatchers.IO) {
            try {
                // Open microphone line
                val dataLineInfo = DataLine.Info(TargetDataLine::class.java, audioFormat)

                if (!AudioSystem.isLineSupported(dataLineInfo)) {
                    throw UnsupportedOperationException("Microphone not available")
                }

                targetDataLine = AudioSystem.getLine(dataLineInfo) as TargetDataLine
                targetDataLine?.open(audioFormat)
                targetDataLine?.start()

                // Initialize in-memory buffer
                audioBuffer = ByteArrayOutputStream()
                isRecording = true

                logger.d { "Started recording audio" }

                // Capture audio in background
                val buffer = ByteArray(4096)
                while (isRecording) {
                    val bytesRead = targetDataLine?.read(buffer, 0, buffer.size) ?: 0
                    if (bytesRead > 0) {
                        audioBuffer?.write(buffer, 0, bytesRead)
                    }
                }
            } catch (e: Exception) {
                logger.e(e) { "Failed to start recording" }
                cleanup()
                throw e
            }
        }
    }

    actual suspend fun stopRecordingAndTranscribe(): String {
        check(isRecording) { "Not currently recording" }

        return withContext(Dispatchers.IO) {
            try {
                isRecording = false
                targetDataLine?.stop()
                targetDataLine?.close()

                val audioData = audioBuffer?.toByteArray() ?: byteArrayOf()
                logger.d { "Stopped recording, audio size: ${audioData.size} bytes" }

                // Transcribe using Vosk
                val transcription = transcribeAudio(audioData)

                logger.d { "Transcription completed: $transcription" }

                transcription
            } catch (e: Exception) {
                logger.e(e) { "Failed to transcribe audio" }
                ""
            } finally {
                // Always clean up memory
                cleanup()
            }
        }
    }

    actual fun isSupported(): Boolean = true

    actual fun cancelRecording() {
        isRecording = false
        targetDataLine?.stop()
        targetDataLine?.close()
        cleanup()
        logger.d { "Recording cancelled" }
    }

    private fun cleanup() {
        audioBuffer?.reset()
        audioBuffer = null
        targetDataLine = null
        isRecording = false
    }

    /**
     * Find Vosk model in different possible locations:
     * 1. compose.application.resources.dir (packaged app)
     * 2. app.dir/Resources (macOS .app bundle)
     * 3. user.dir/desktopApp/appResources (development mode)
     * 4. Search up directory tree from user.dir
     */
    private fun findVoskModel(): String {
        val modelName = "vosk-model-ru-0.42"
        val searchedPaths = mutableListOf<String>()
        val userDir = System.getProperty("user.dir")

        // Strategy 1: compose.application.resources.dir (for packaged apps)
        val composeResourcesDir = System.getProperty("compose.application.resources.dir")
        if (composeResourcesDir != null) {
            val modelPath = File(composeResourcesDir, modelName)
            searchedPaths.add(modelPath.absolutePath)
            if (modelPath.exists() && modelPath.isDirectory) {
                logger.d { "Found model at: ${modelPath.absolutePath}" }
                return modelPath.absolutePath
            }
        }

        // Strategy 1.5: Check for macOS .app bundle Resources directory
        // When running from .app, user.dir is typically the .app/Contents/Resources
        val appBundleModel = File(userDir, modelName)
        searchedPaths.add(appBundleModel.absolutePath)
        if (appBundleModel.exists() && appBundleModel.isDirectory) {
            logger.d { "Found model at: ${appBundleModel.absolutePath}" }
            return appBundleModel.absolutePath
        }

        // Strategy 2: app.dir (alternative packaged app location)
        val appDir = System.getProperty("app.dir")
        if (appDir != null) {
            val modelPath = File(appDir, modelName)
            searchedPaths.add(modelPath.absolutePath)
            if (modelPath.exists() && modelPath.isDirectory) {
                logger.d { "Found model at: ${modelPath.absolutePath}" }
                return modelPath.absolutePath
            }
        }

        // Strategy 3: Development mode - desktopApp/appResources
        val devModelPath = File(userDir, "desktopApp/appResources/$modelName")
        searchedPaths.add(devModelPath.absolutePath)
        if (devModelPath.exists() && devModelPath.isDirectory) {
            logger.d { "Found model at: ${devModelPath.absolutePath}" }
            return devModelPath.absolutePath
        }

        // Strategy 4: Search from user.dir up the directory tree
        var currentDir: File? = File(userDir)
        var levelsSearched = 0

        while (currentDir != null && levelsSearched < 5) {
            // Check desktopApp/appResources subdirectory
            val desktopAppModel = File(currentDir, "desktopApp/appResources/$modelName")
            searchedPaths.add(desktopAppModel.absolutePath)
            if (desktopAppModel.exists() && desktopAppModel.isDirectory) {
                logger.d { "Found model at: ${desktopAppModel.absolutePath}" }
                return desktopAppModel.absolutePath
            }

            // Check model directly in current dir
            val modelPath = File(currentDir, modelName)
            searchedPaths.add(modelPath.absolutePath)
            if (modelPath.exists() && modelPath.isDirectory) {
                logger.d { "Found model at: ${modelPath.absolutePath}" }
                return modelPath.absolutePath
            }

            currentDir = currentDir.parentFile
            levelsSearched++
        }

        throw IllegalStateException(
            "Vosk model '$modelName' not found. Searched in:\n" +
            searchedPaths.joinToString("\n") { "  - $it" } +
            "\n\nSystem properties:\n" +
            "  compose.application.resources.dir = $composeResourcesDir\n" +
            "  app.dir = $appDir\n" +
            "  user.dir = $userDir"
        )
    }

    /**
     * Transcribe audio using Vosk model.
     * Model path: vosk-model-ru-0.42 in app resources or project root
     */
    private fun transcribeAudio(audioData: ByteArray): String {
        try {
            // Set log level to errors only (0 = errors only, -1 = no logs)
            VoskLite.setLogLevel(0)

            // Load model if not already loaded
            if (model == null) {
                val modelPath = findVoskModel()
                logger.d { "Loading Vosk model from: $modelPath" }
                model = VoskLite.loadModel(modelPath)
            }

            // Create recognizer
            val recognizer = VoskLite.newRecognizer(model!!, 16000f)

            try {
                // Process audio data
                if (audioData.isNotEmpty()) {
                    VoskLite.acceptWaveform(recognizer, audioData, audioData.size)
                }

                // Get final result
                val resultJson: String = VoskLite.finalResult(recognizer)

                logger.e { "resultJson: $resultJson" }

                // Parse JSON result (simple extraction)
                // Format: {"text":"transcribed text here"}
                val textMatch: MatchResult? = Regex(""""text"\s*:\s*"([^"]*)"""").find(resultJson)
                val transcription: String = textMatch?.groupValues?.getOrNull(1)?.trim() ?: ""

                return transcription
            } finally {
                // Always close recognizer
                recognizer.close()
            }

        } catch (e: Exception) {
            logger.e(e) { "Vosk transcription failed" }
            throw IllegalStateException("Speech recognition failed: ${e.message}", e)
        }
    }
}
