package com.squig.equalizer.dsp

import android.content.Context
import android.content.SharedPreferences
import com.squig.equalizer.models.EqSettings

/**
 * Process-wide brain of the equalizer. The service, the web server and the
 * audio-session receiver all talk to this one object.
 *
 * GLOBAL   = one effect on the output mix (session 0) -> affects every app, if the device allows it.
 * SESSIONS = one effect per app audio session, learned from OPEN/CLOSE_AUDIO_EFFECT_CONTROL_SESSION broadcasts.
 */
object EqController {

    enum class Mode { GLOBAL, SESSIONS }

    private val dsp = AudioDspManager()
    private var prefs: SharedPreferences? = null
    private var settings = EqSettings.FLAT
    private var mode = Mode.GLOBAL
    private var globalOk = false
    private val knownSessions = LinkedHashSet<Int>()

    /** Called whenever status text changes (service refreshes its notification). */
    @Volatile var onChanged: (() -> Unit)? = null

    @Synchronized
    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences("eq", Context.MODE_PRIVATE)
        prefs = p
        val gains = p.getString("gains", null)?.split(",")?.mapNotNull { it.toFloatOrNull() }
            ?.takeIf { it.size == EqConstants.BAND_COUNT } ?: EqSettings.FLAT.gains
        settings = EqSettings(p.getBoolean("enabled", false), p.getFloat("post", 0f), gains)
        mode = if (p.getString("mode", "global") == "sessions") Mode.SESSIONS else Mode.GLOBAL
    }

    private fun usesSessions() = mode == Mode.SESSIONS || !globalOk

    @Synchronized
    fun start() {
        dsp.releaseAll()
        globalOk = false
        if (mode == Mode.GLOBAL) globalOk = dsp.attach(0, settings)
        if (usesSessions()) knownSessions.forEach { dsp.attach(it, settings) }
        onChanged?.invoke()
    }

    @Synchronized
    fun shutdown() {
        dsp.releaseAll()
        globalOk = false
        onChanged = null
    }

    @Synchronized
    fun apply(s: EqSettings) {
        settings = s
        prefs?.edit()
            ?.putBoolean("enabled", s.enabled)
            ?.putFloat("post", s.postGainDb)
            ?.putString("gains", s.gains.joinToString(","))
            ?.apply()
        dsp.applyToAll(s)
    }

    @Synchronized
    fun setMode(newMode: Mode) {
        mode = newMode
        prefs?.edit()?.putString("mode", if (newMode == Mode.SESSIONS) "sessions" else "global")?.apply()
        start()
    }

    @Synchronized
    fun onSessionOpened(id: Int) {
        knownSessions += id
        if (usesSessions()) dsp.attach(id, settings)
        onChanged?.invoke()
    }

    @Synchronized
    fun onSessionClosed(id: Int) {
        knownSessions -= id
        dsp.detach(id)
        onChanged?.invoke()
    }

    @Synchronized
    fun modeName() = if (mode == Mode.SESSIONS) "sessions" else "global"

    @Synchronized
    fun statusText(): String {
        val n = dsp.sessions().count { it != 0 }
        return when {
            mode == Mode.GLOBAL && globalOk -> "System-wide effect active"
            mode == Mode.GLOBAL -> "System-wide effect not available on this device - using per-app mode ($n apps attached)"
            else -> "Per-app mode: $n audio sessions attached. If nothing changes, restart playback in your music app."
        }
    }
}
