package com.squig.equalizer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.squig.equalizer.MainActivity
import com.squig.equalizer.dsp.EqController
import com.squig.equalizer.web.WebServer

/** Foreground service that keeps the effects and the local web UI alive. */
class AudioEqualizerService : Service() {

    private var server: WebServer? = null

    override fun onCreate() {
        super.onCreate()
        EqController.init(this)

        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Equalizer", NotificationManager.IMPORTANCE_LOW)
        )
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, notification)
        }

        EqController.onChanged = { nm.notify(NOTIF_ID, buildNotification()) }
        EqController.start()

        try {
            server = WebServer(port = 8080).also { it.start() }
        } catch (e: java.io.IOException) {
            android.util.Log.e("AudioEqualizerService", "Web UI could not start (port 8080 busy?)", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_headset)
            .setContentTitle("SquigEqualizer")
            .setContentText(EqController.statusText())
            .setContentIntent(open)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        server?.stop()
        EqController.shutdown()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private companion object {
        const val CHANNEL = "equalizer"
        const val NOTIF_ID = 1
    }
}
