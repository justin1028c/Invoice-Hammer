package com.fordham.toolbelt.util

import platform.AVFoundation.*
import platform.Foundation.*
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_async
import platform.Speech.*
import com.fordham.toolbelt.domain.repository.GeminiRepository
import com.fordham.toolbelt.domain.model.GeminiOutcome
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.posix.memcpy
import platform.CoreAudio.kAudioFormatMPEG4AAC

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = this.length.toInt()
    val bytes = ByteArray(size)
    if (size > 0) {
        bytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), this.bytes, this.length)
        }
    }
    return bytes
}

class IosVoiceAssistant(
    private val geminiRepository: GeminiRepository
) : VoiceAssistant {
    private val synthesizer = AVSpeechSynthesizer()
    private var audioRecorder: AVAudioRecorder? = null
    private var onResultCallback: ((VoiceTranscriptMeta) -> Unit)? = null
    private var onEndCallback: (() -> Unit)? = null
    private val scope = CoroutineScope(Dispatchers.Default)
    private var currentSessionId = 0
    private var silenceJob: kotlinx.coroutines.Job? = null

    private var speechRecognizer: SFSpeechRecognizer? = null
    private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest? = null
    private var recognitionTask: SFSpeechRecognitionTask? = null
    private val audioEngine = AVAudioEngine()
    private var isSpeechRecognizerListening = false

    private fun runOnMain(block: () -> Unit) {
        dispatch_async(dispatch_get_main_queue()) {
            block()
        }
    }

    override fun speak(text: String) {
        val spoken = ForemanVoiceSpeak.prepareSpokenText(text) ?: return
        val locale = if (AppLocale.fromSystem() == AppLocale.Spanish) "es-ES" else "en-US"
        val voice = AVSpeechSynthesisVoice.voiceWithLanguage(locale)
            ?: AVSpeechSynthesisVoice.speechVoices().firstOrNull {
                (it as? AVSpeechSynthesisVoice)?.language?.startsWith("es") == true
            } as? AVSpeechSynthesisVoice
            ?: AVSpeechSynthesisVoice.voiceWithLanguage("en-US")
        val utterance = AVSpeechUtterance.speechUtteranceWithString(spoken)
        utterance.voice = voice
        utterance.rate = 0.52f
        utterance.pitchMultiplier = 1.0
        utterance.preUtteranceDelay = 0.05
        utterance.postUtteranceDelay = 0.05

        val audioSession = AVAudioSession.sharedInstance()
        audioSession.setCategory(AVAudioSessionCategoryPlayback, error = null)
        audioSession.setActive(true, error = null)

        synthesizer.speakUtterance(utterance)
    }

    override fun stopSpeaking() {
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundaryImmediate)
    }

    override fun startListening(
        onResult: (String) -> Unit,
        onEnd: () -> Unit,
        onPartialResult: (String) -> Unit
    ) {
        startListeningWithMeta(
            onResult = { meta -> onResult(meta.text) },
            onEnd = onEnd,
            onPartialResult = onPartialResult
        )
    }

    override fun startListeningWithMeta(
        onResult: (VoiceTranscriptMeta) -> Unit,
        onEnd: () -> Unit,
        onPartialResult: (String) -> Unit
    ) {
        AVAudioSession.sharedInstance().requestRecordPermission { microphoneGranted ->
            if (microphoneGranted) {
                SFSpeechRecognizer.requestAuthorization { authStatus ->
                    runOnMain {
                        onResultCallback = null
                        onEndCallback = null
                        stopListening(discard = true)

                        currentSessionId++
                        onResultCallback = onResult
                        onEndCallback = onEnd

                        val sessionId = currentSessionId
                        if (authStatus == SFSpeechRecognizerAuthorizationStatusAuthorized) {
                            startSpeechRecognizer(
                                onResult = { meta ->
                                    if (sessionId == currentSessionId) {
                                        onResult(meta)
                                    }
                                },
                                onEnd = onEnd,
                                onPartialResult = onPartialResult
                            )
                        } else {
                            startRecordingFlow(onResult, onEnd)
                        }
                    }
                }
            } else {
                runOnMain(onEnd)
            }
        }
    }

    private fun startSpeechRecognizer(
        onResult: (VoiceTranscriptMeta) -> Unit,
        onEnd: () -> Unit,
        onPartialResult: (String) -> Unit
    ) {
        val locale = if (AppLocale.fromSystem() == AppLocale.Spanish) "es-ES" else "en-US"
        val recognizer = SFSpeechRecognizer(locale = NSLocale(localeIdentifier = locale))
        if (recognizer == null || !recognizer.isAvailable()) {
            startRecordingFlow(onResult, onEnd)
            return
        }
        speechRecognizer = recognizer

        stopSpeechRecognizerListening(discard = true)

        val audioSession = AVAudioSession.sharedInstance()
        try {
            audioSession.setCategory(AVAudioSessionCategoryPlayAndRecord, withOptions = AVAudioSessionCategoryOptionDefaultToSpeaker, error = null)
            audioSession.setMode(AVAudioSessionModeMeasurement, error = null)
            audioSession.setActive(true, withOptions = AVAudioSessionCategoryOptionNotifyOthersOnDeactivation, error = null)
        } catch (e: Exception) {
            startRecordingFlow(onResult, onEnd)
            return
        }

        val request = SFSpeechAudioBufferRecognitionRequest()
        recognitionRequest = request
        request.shouldReportPartialResults = true

        val inputNode = audioEngine.inputNode
        val recordingFormat = inputNode.outputFormatForBus(0.toULong())

        inputNode.removeTapOnBus(0.toULong())
        inputNode.installTapOnBus(
            bus = 0.toULong(),
            bufferSize = 1024.toUInt(),
            format = recordingFormat
        ) { buffer, _ ->
            if (buffer != null) {
                recognitionRequest?.appendAudioPCMBuffer(buffer)
            }
        }

        audioEngine.prepare()
        try {
            audioEngine.startAndReturnError(null)
        } catch (e: Exception) {
            audioEngine.stop()
            inputNode.removeTapOnBus(0.toULong())
            startRecordingFlow(onResult, onEnd)
            return
        }

        isSpeechRecognizerListening = true

        val sessionId = currentSessionId
        recognitionTask = recognizer.recognitionTaskWithRequest(request) { result, error ->
            var isFinal = false
            if (result != null) {
                val transcript = result.bestTranscription.formattedString
                runOnMain {
                    if (sessionId == currentSessionId) {
                        onPartialResult(transcript)
                    }
                }
                isFinal = result.isFinal()
            }

            if (error != null || isFinal) {
                runOnMain {
                    if (sessionId == currentSessionId) {
                        if (result != null) {
                            val transcript = result.bestTranscription.formattedString
                            onResult(VoiceTranscriptMeta(transcript, 1.0f))
                        }
                        onEnd()
                        stopSpeechRecognizerListening(discard = false)
                    }
                }
            }
        }
    }

    private fun stopSpeechRecognizerListening(discard: Boolean) {
        if (!isSpeechRecognizerListening) return
        isSpeechRecognizerListening = false

        audioEngine.stop()
        audioEngine.inputNode.removeTapOnBus(0.toULong())

        recognitionRequest?.endAudio()
        recognitionRequest = null

        if (discard) {
            recognitionTask?.cancel()
        }
        recognitionTask = null
    }

    private fun startRecordingFlow(
        onResult: (VoiceTranscriptMeta) -> Unit,
        onEnd: () -> Unit
    ) {
        onResultCallback = null
        onEndCallback = null
        stopListening(discard = true)

        currentSessionId++
        onResultCallback = onResult
        onEndCallback = onEnd

        val audioSession = AVAudioSession.sharedInstance()
        try {
            audioSession.setCategory(AVAudioSessionCategoryPlayAndRecord, withOptions = AVAudioSessionCategoryOptionDefaultToSpeaker, error = null)
            audioSession.setMode(AVAudioSessionModeVoiceChat, error = null)
            audioSession.setActive(true, withOptions = AVAudioSessionCategoryOptionNotifyOthersOnDeactivation, error = null)
        } catch (e: Exception) {
            onEnd()
            return
        }

        val tempDir = NSTemporaryDirectory()
        val filePath = tempDir + "voice_command.m4a"
        val url = NSURL.fileURLWithPath(filePath)

        val fileManager = NSFileManager.defaultManager
        if (fileManager.fileExistsAtPath(filePath)) {
            fileManager.removeItemAtPath(filePath, error = null)
        }

        val settings = mapOf<Any?, Any?>(
            AVFormatIDKey to kAudioFormatMPEG4AAC.toLong(),
            AVSampleRateKey to 44100.0,
            AVNumberOfChannelsKey to 1,
            AVEncoderAudioQualityKey to AVAudioQualityHigh.toLong()
        )

        try {
            val recorder = AVAudioRecorder(URL = url, settings = settings as Map<Any?, *>, error = null)
            recorder.meteringEnabled = true
            audioRecorder = recorder
            recorder.prepareToRecord()
            recorder.record()

            silenceJob = scope.launch(Dispatchers.Main) {
                val checkIntervalMs = 200L
                val requiredSilenceDurationMs = 3000L
                var consecutiveSilenceMs = 0L

                delay(1000)

                while (audioRecorder != null) {
                    delay(checkIntervalMs)
                    val currentRecorder = audioRecorder ?: break
                    currentRecorder.updateMeters()
                    val power = currentRecorder.averagePowerForChannel(0.toULong())
                    if (power < -40.0f) {
                        consecutiveSilenceMs += checkIntervalMs
                        if (consecutiveSilenceMs >= requiredSilenceDurationMs) {
                            stopListening(discard = false)
                            break
                        }
                    } else {
                        consecutiveSilenceMs = 0L
                    }
                }
            }
        } catch (e: Exception) {
            audioRecorder = null
            onEnd()
        }
    }

    override fun stopListening(discard: Boolean) {
        silenceJob?.cancel()
        silenceJob = null

        if (isSpeechRecognizerListening) {
            stopSpeechRecognizerListening(discard)
            if (discard) {
                onResultCallback = null
                onEndCallback = null
            }
            return
        }

        if (discard) {
            currentSessionId++
        }
        val recorder = audioRecorder
        if (recorder == null) {
            if (discard) {
                onResultCallback = null
                onEndCallback = null
            }
            return
        }
        audioRecorder = null

        val sessionId = currentSessionId
        val localResultCallback = onResultCallback
        val localEndCallback = onEndCallback

        onResultCallback = null
        onEndCallback = null

        try {
            recorder.stop()
        } catch (e: Exception) {
            // Ignore
        }

        if (discard) {
            val tempDir = NSTemporaryDirectory()
            val filePath = tempDir + "voice_command.m4a"
            NSFileManager.defaultManager.removeItemAtPath(filePath, error = null)
            runOnMain {
                localEndCallback?.invoke()
            }
            return
        }

        val tempDir = NSTemporaryDirectory()
        val filePath = tempDir + "voice_command.m4a"
        val fileData = NSData.dataWithContentsOfFile(filePath)
        if (fileData != null) {
            scope.launch {
                val bytes = fileData.toByteArray()
                when (val outcome = geminiRepository.transcribeAudio(bytes, "audio/mp4")) {
                    is GeminiOutcome.Success -> {
                        val transcript = outcome.text
                        runOnMain {
                            if (sessionId == currentSessionId) {
                                localResultCallback?.invoke(VoiceTranscriptMeta(transcript, 1.0f))
                            }
                        }
                    }
                    is GeminiOutcome.Failure -> {
                        // Fail silently or log
                    }
                }
                runOnMain {
                    if (sessionId == currentSessionId) {
                        localEndCallback?.invoke()
                    }
                }
            }
        } else {
            runOnMain {
                if (sessionId == currentSessionId) {
                    localEndCallback?.invoke()
                }
            }
        }
    }

    override fun destroy() {
        silenceJob?.cancel()
        silenceJob = null
        stopListening(discard = true)
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundaryImmediate)
        val recorder = audioRecorder
        audioRecorder = null
        if (recorder != null) {
            try {
                recorder.stop()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
