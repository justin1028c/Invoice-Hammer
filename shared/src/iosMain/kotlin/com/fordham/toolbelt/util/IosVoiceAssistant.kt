package com.fordham.toolbelt.util

import platform.AVFoundation.*
import platform.Foundation.*
import platform.Speech.*
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_async

class IosVoiceAssistant : VoiceAssistant {
    private val synthesizer = AVSpeechSynthesizer()
    private val speechRecognizer = SFSpeechRecognizer(NSLocale.localeWithLocaleIdentifier("en-US"))
    private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest? = null
    private var recognitionTask: SFSpeechRecognitionTask? = null
    private val audioEngine = AVAudioEngine()

    private fun runOnMain(block: () -> Unit) {
        dispatch_async(dispatch_get_main_queue()) {
            block()
        }
    }

    override fun speak(text: String) {
        val utterance = AVSpeechUtterance.speechUtteranceWithString(text)
        utterance.voice = AVSpeechSynthesisVoice.voiceWithLanguage("en-US")
        
        // Ensure session is set for playback
        val audioSession = AVAudioSession.sharedInstance()
        audioSession.setCategory(AVAudioSessionCategoryPlayback, error = null)
        audioSession.setActive(true, error = null)
        
        synthesizer.speakUtterance(utterance)
    }

    override fun startListening(onResult: (String) -> Unit, onEnd: () -> Unit) {
        if (speechRecognizer?.isAvailable() == false) {
            SFSpeechRecognizer.requestAuthorization { status ->
                if (status == SFSpeechRecognizerAuthorizationStatusAuthorized) {
                    runOnMain {
                        startListening(onResult, onEnd)
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

        recognitionRequest = SFSpeechAudioBufferRecognitionRequest()
        val inputNode = audioEngine.inputNode
        
        recognitionTask = speechRecognizer?.recognitionTaskWithRequest(recognitionRequest!!) { result, error ->
            if (result != null) {
                runOnMain {
                    onResult(result.bestTranscription.formattedString)
                }
            }
            
            if (error != null || (result?.isFinal() == true)) {
                stopListening()
                runOnMain(onEnd)
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
        if (audioEngine.isRunning()) {
            audioEngine.stop()
            audioEngine.inputNode.removeTapOnBus(0u)
        }
        recognitionRequest?.endAudio()
        recognitionTask?.cancel()
        recognitionTask = null
        recognitionRequest = null
    }

    override fun destroy() {
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundaryImmediate)
        stopListening()
    }
}
