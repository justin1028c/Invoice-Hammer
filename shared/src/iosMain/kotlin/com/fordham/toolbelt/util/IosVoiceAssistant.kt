package com.fordham.toolbelt.util

import platform.AVFoundation.*
import platform.Foundation.*
import platform.Speech.*
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_async

class IosVoiceAssistant : VoiceAssistant {
    private val synthesizer = AVSpeechSynthesizer()
    private val preferredVoice = IosTtsVoiceSelector.bestUsVoice()
    private val speechRecognizer = SFSpeechRecognizer(locale = NSLocale.localeWithLocaleIdentifier("en-US"))
    private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest? = null
    private var recognitionTask: SFSpeechRecognitionTask? = null
    private val audioEngine = AVAudioEngine()

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
        if (speechRecognizer?.isAvailable == false) {
            SFSpeechRecognizer.requestAuthorization { status ->
                if (status == SFSpeechRecognizerAuthorizationStatusAuthorized) {
                    runOnMain {
                        startListeningWithMeta(onResult, onEnd)
                    }
                } else {
                    runOnMain(onEnd)
                }
            }
            return
        }

        if (recognitionTask != null) {
            recognitionTask?.cancel()
            recognitionTask = null
        }

        val audioSession = AVAudioSession.sharedInstance()
        try {
            audioSession.setCategory(AVAudioSessionCategoryPlayAndRecord, withOptions = AVAudioSessionCategoryOptionDefaultToSpeaker, error = null)
            audioSession.setMode(AVAudioSessionModeMeasurement, error = null)
            audioSession.setActive(true, withOptions = AVAudioSessionCategoryOptionNotifyOthersOnDeactivation, error = null)
        } catch (e: Exception) {
            onEnd()
            return
        }

        recognitionRequest = SFSpeechAudioBufferRecognitionRequest().apply {
            shouldReportPartialResults = false
        }
        val inputNode = audioEngine.inputNode
        
        recognitionTask = speechRecognizer?.recognitionTaskWithRequest(recognitionRequest!!) { result, error ->
            if (result != null) {
                val transcriptions = result.transcriptions
                val alternatives = if (transcriptions.size > 1) {
                    transcriptions.drop(1).map { (it as SFTranscription).formattedString }
                } else {
                    emptyList()
                }
                if (result.isFinal) {
                    runOnMain {
                        onResult(VoiceTranscriptMeta(result.bestTranscription.formattedString, null, alternatives))
                    }
                }
            }
            
            if (error != null || (result?.isFinal == true)) {
                runOnMain {
                    if (audioEngine.isRunning) {
                        audioEngine.stop()
                        audioEngine.inputNode.removeTapOnBus(0u)
                    }
                    recognitionTask = null
                    recognitionRequest = null
                    onEnd()
                }
            }
        }

        val recordingFormat = inputNode.outputFormatForBus(0u)
        inputNode.removeTapOnBus(0u) // Defensive
        inputNode.installTapOnBus(0u, bufferSize = 1024u, format = recordingFormat) { buffer, _ ->
            recognitionRequest?.appendAudioPCMBuffer(buffer!!)
        }

        audioEngine.prepare()
        try {
            audioEngine.startAndReturnError(null)
        } catch (e: Exception) {
            onEnd()
        }
    }

    override fun stopListening() {
        runOnMain {
            if (audioEngine.isRunning) {
                audioEngine.stop()
                audioEngine.inputNode.removeTapOnBus(0u)
            }
            recognitionRequest?.endAudio()
        }
    }

    override fun destroy() {
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundaryImmediate)
        runOnMain {
            if (audioEngine.isRunning) {
                audioEngine.stop()
                audioEngine.inputNode.removeTapOnBus(0u)
            }
            recognitionRequest?.endAudio()
            recognitionTask?.cancel()
            recognitionTask = null
            recognitionRequest = null
        }
    }
}

