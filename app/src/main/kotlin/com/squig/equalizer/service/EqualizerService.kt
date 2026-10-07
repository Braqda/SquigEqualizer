package com.squig.equalizer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import com.squig.equalizer.dsp.AudioDspManager

class EqualizerService : Service() {

    private val binder = LocalBinder()
    val dspManager = AudioDspManager()

    inner class LocalBinder : Binder() {
        fun getService(): EqualizerService = this@EqualizerService
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceNotification()
        dspManager.initEffect(audioSessionId = 0)
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        dspManager.release()
        super.onDestroy()
    }

    private fun startForegroundServiceNotification() {
        val channelId = "squig_eq_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Squig Equalizer Engine",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        val notification: Notification = builder
            .setContentTitle("Squig Equalizer Running")
            .setContentText("System-wide Audio DSP active")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()

        startForeground(1, notification)
    }
}
