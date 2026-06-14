package com.fordham.toolbelt.util

import platform.AVFoundation.*
import platform.Foundation.*
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_async
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
    private val preferredVoice = IosTtsVoiceSelector.bestUsVoice()
    private var audioRecorder: AVAudioRecorder? = null
    private var onResultCallback: ((VoiceTranscriptMeta) -> Unit)? = null
    private var onEndCallback: (() -> Unit)? = null
    private val scope = CoroutineScope(Dispatchers.Default)
    private var currentSessionId = 0
    private var silenceJob: kotlinx.coroutines.Job? = null

    private fun runOnMain(block: () -> Unit) {
        dispatch_async(dispatch_get_main_queue()) {
            block()
        }
    }

    override fun speak(text: String) {
        val spoken = ForemanVoiceSpeak.prepareSpokenText(text) ?: return
        val utterance = AVSpeechUtterance.speechUtteranceWithString(spoken)
        utterance.voice = preferredVoice ?: AVSpeechSynthesisVoice.voiceWithLanguage("en-US")
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

    override fun startListening(onResult: (String) -> Unit, onEnd: () -> Unit) {
        startListeningWithMeta(
            onResult = { meta -> onResult(meta.text) },
            onEnd = onEnd
        )
    }

    override fun startListeningWithMeta(
        onResult: (VoiceTranscriptMeta) -> Unit,
        onEnd: () -> Unit
    ) {
        AVAudioSession.sharedInstance().requestRecordPermission { granted ->
            if (granted) {
                runOnMain {
                    startRecordingFlow(onResult, onEnd)
                }
            } else {
                runOnMain(onEnd)
            }
        }
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
            // Strategy B: Hardware noise-suppression and voice pre-processing
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
                val requiredSilenceDurationMs = 2500L
                var consecutiveSilenceMs = 0L

                delay(1500)

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
            // Ignore stop issues
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
                // Strategy C: Multi-modal cloud audio processing
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
