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
        if (result == TextToSpeech.LANG_MISSING_DATA || result ==
