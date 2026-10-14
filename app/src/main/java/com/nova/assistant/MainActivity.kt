package com.nova.assistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var tvStatus: TextView
    private lateinit var tvHeard: TextView
    private lateinit var tvReply: TextView
    private lateinit var btnMic: Button

    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    private lateinit var brain: NovaBrain
    private val micRequestCode = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        tvHeard = findViewById(R.id.tvHeard)
        tvReply = findViewById(R.id.tvReply)
        btnMic = findViewById(R.id.btnMic)

        brain = NovaBrain(AppLauncher(this))
        tts = TextToSpeech(this, this)

        btnMic.setOnClickListener { checkPermissionAndListen() }
    }

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            tvStatus.text = "Voice engine start nahi hua. Phone mein Google Text-to-speech check karein."
            return
        }
        val result = tts?.setLanguage(Locale.forLanguageTag("hi-IN"))
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            tvStatus.text = "Hindi voice installed nahi hai. Settings > Text-to-speech > Google > Install voice data > Hindi."
        }
        ttsReady = true
    }

    private fun checkPermissionAndListen() {
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            startListening()
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.RECORD_AUDIO), micRequestCode
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == micRequestCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startListening()
            } else {
                tvStatus.text = "Mic ki permission nahi mili. Phone Settings > Apps > Sia AI > Permissions mein Microphone allow karein."
            }
        }
    }

    private fun speak(text: String) {
        tvReply.text = "Sia: $text"
        if (ttsReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "nova_reply")
        }
    }

    override fun onDestroy() {
        recognizer?.destroy()
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            tvStatus.text = "Is phone mein speech recognition available nahi hai. Google app update/install karein."
            return
        }

        tts?.stop()
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this)

        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                tvStatus.text = "Sun raha hoon... boliye"
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                tvStatus.text = "Samajh raha hoon..."
            }

            override fun onError(error: Int) {
                tvStatus.text = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Kuch sunai nahi diya. Dobara try karein."
                    SpeechRecognizer.ERROR_NETWORK,
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Internet problem hai. Connection check karein."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Mic permission chahiye."
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy hai. Thodi der baad try karein."
                    else -> "Kuch gadbad hui (code $error). Dobara try karein."
                }
            }

            override fun onResults(results: Bundle?) {
                val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val heard = list?.firstOrNull()
                if (heard.isNullOrBlank()) {
                    tvStatus.text = "Kuch sunai nahi diya. Dobara try karein."
                    return
                }
                tvStatus.text = "Mic dabaiye aur boliye"
                tvHeard.text = "Aapne kaha: $heard"
                val reply = brain.reply(heard)
                speak(reply)
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        recognizer?.startListening(intent)
    }
}
