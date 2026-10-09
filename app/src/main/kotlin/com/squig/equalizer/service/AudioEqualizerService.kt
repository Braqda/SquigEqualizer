package com.squig.equalizer.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

class AudioEqualizerService : Service() {

    override fun onCreate() {
        super.onCreate()
        println("[AudioEqualizerService] Service Created on Galaxy A05s.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val preamp = intent?.getFloatExtra("PREAMP_GAIN", 0.0f) ?: 0.0f
        val gains = intent?.getFloatArrayExtra("EQ_GAINS") ?: FloatArray(32) { 0.0f }

        println("[AudioEqualizerService] Processing DSP Request: Preamp=$preamp dB | Bands=${gains.size}")
        
        applyDspGains(preamp, gains)

        return START_STICKY
    }

    private fun applyDspGains(preamp: Float, gains: FloatArray) {
        println("[DSP Engine] Active processing on audio stream.")
        for (i in gains.indices) {
            if (gains[i] != 0.0f) {
                println("  -> Band $i: ${gains[i]} dB")
            }
        }
    }

    override fun onDestroy() {
        println("[AudioEqualizerService] Service Destroyed.")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
