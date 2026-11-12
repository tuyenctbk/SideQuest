package com.example.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.model.CharacterStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

class VoiceNarrator(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isReady = false

    private data class PendingSpeech(
        val text: String,
        val style: CharacterStyle,
        val title: String
    )

    private var pendingSpeech: PendingSpeech? = null

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _currentTitle = MutableStateFlow("")
    val currentTitle: StateFlow<String> = _currentTitle.asStateFlow()

    private val _autoNarrate = MutableStateFlow(true)
    val autoNarrate: StateFlow<Boolean> = _autoNarrate.asStateFlow()

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.w("VoiceNarrator", "TextToSpeech unavailable in this environment: ${e.message}")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            isReady = result != TextToSpeech.LANG_MISSING_DATA &&
                result != TextToSpeech.LANG_NOT_SUPPORTED
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                override fun onStop(utteranceId: String?, interrupted: Boolean) {
                    _isSpeaking.value = false
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isSpeaking.value = false
                }
            })

            val queued = pendingSpeech
            if (isReady && queued != null) {
                pendingSpeech = null
                speak(queued.text, queued.style, queued.title)
            }
        }
    }

    fun toggleAutoNarrate() {
        val next = !_autoNarrate.value
        _autoNarrate.value = next
        if (!next) {
            stop()
        }
    }

    fun speak(text: String, style: CharacterStyle, title: String = style.narratorTitle) {
        if (text.isBlank()) return
        val cleanText = text
            .replace("*", "")
            .replace("#", "")
            .replace("_", "")
            .trim()
        _currentTitle.value = title
        if (!isReady) {
            pendingSpeech = PendingSpeech(cleanText, style, title)
            return
        }
        try {
            tts?.setPitch(style.voicePitch)
            tts?.setSpeechRate(style.voiceRate)
            val utteranceId = UUID.randomUUID().toString()
            _isSpeaking.value = true
            val speakResult = tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            if (speakResult == TextToSpeech.ERROR) {
                _isSpeaking.value = false
            }
        } catch (e: Exception) {
            Log.w("VoiceNarrator", "Failed speaking utterance: ${e.message}")
            _isSpeaking.value = false
        }
    }

    fun stop() {
        pendingSpeech = null
        try {
            tts?.stop()
        } catch (_: Exception) {
        }
        _isSpeaking.value = false
    }

    fun shutdown() {
        pendingSpeech = null
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {
        }
        _isSpeaking.value = false
    }
}
