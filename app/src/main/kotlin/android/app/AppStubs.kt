package android.app

import android.content.Context
import android.content.Intent
import android.os.IBinder

open class Notification {
    class Builder(context: Any?, channelId: String = "") {
        fun setContentTitle(title: CharSequence): Builder = this
        fun setContentText(text: CharSequence): Builder = this
        fun setSmallIcon(icon: Int): Builder = this
        fun build(): Notification = Notification()
    }
}

open class NotificationChannel(val id: String, val name: CharSequence, val importance: Int)

open class NotificationManager {
    fun createNotificationChannel(channel: NotificationChannel) {}

    companion object {
        const val IMPORTANCE_LOW = 3
    }
}

abstract class Service : Context() {
    open fun onCreate() {}
    open fun onDestroy() {}
    abstract fun onBind(intent: Intent?): IBinder?
    fun startForeground(id: Int, notification: Any?) {}
    fun getSystemService(name: String): Any? = null
}
