package com.squig.equalizer.dsp

import android.media.audiofx.DynamicsProcessing
import android.util.Log
import com.squig.equalizer.models.EqSettings
import kotlin.math.max

/** Owns one DynamicsProcessing effect per audio session (session 0 = global output mix). */
class AudioDspManager {

    private val effects = HashMap<Int, DynamicsProcessing>()

    @Synchronized
    fun attach(sessionId: Int, settings: EqSettings): Boolean {
        detach(sessionId)
        return try {
            val config = DynamicsProcessing.Config.Builder(
                DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                2,                      // stereo
                true, EqConstants.BAND_COUNT, // Pre-EQ
                false, 0,               // no MBC
                false, 0,               // no Post-EQ
                true                    // limiter
            ).build()
            val dp = DynamicsProcessing(0, sessionId, config)
            configure(dp, settings)
            dp.enabled = settings.enabled
            effects[sessionId] = dp
            true
        } catch (e: Exception) {
            Log.w(TAG, "Could not attach to session $sessionId", e)
            false
        }
    }

    @Synchronized
    fun detach(sessionId: Int) {
        effects.remove(sessionId)?.let {
            try { it.enabled = false; it.release() } catch (_: Exception) {}
        }
    }

    @Synchronized
    fun applyToAll(settings: EqSettings) {
        val dead = mutableListOf<Int>()
        for ((id, dp) in effects) {
            try {
                configure(dp, settings)
                dp.enabled = settings.enabled
            } catch (e: Exception) {
                Log.w(TAG, "Effect for session $id failed, dropping it", e)
                dead += id
            }
        }
        dead.forEach { detach(it) }
    }

    @Synchronized
    fun releaseAll() = effects.keys.toList().forEach { detach(it) }

    @Synchronized
    fun sessions(): Set<Int> = effects.keys.toSet()

    private fun configure(dp: DynamicsProcessing, s: EqSettings) {
        val gains = s.gains.map { it.coerceIn(-24f, 24f) }

        // Preamp: user's post gain, minus headroom for the biggest boost
        val headroom = if (s.autoPreamp) -max(0f, gains.maxOrNull() ?: 0f) else 0f
        dp.setInputGainAllChannelsTo(s.postGainDb + headroom)

        for (i in 0 until EqConstants.BAND_COUNT) {
            dp.setPreEqBandAllChannelsTo(
                i, DynamicsProcessing.EqBand(true, EqConstants.CUTOFFS[i], gains[i])
            )
        }

        // Safety net against clipping
        dp.setLimiterAllChannelsTo(
            DynamicsProcessing.Limiter(true, true, 0, 1f, 60f, 10f, -2f, 0f)
        )
    }

    private companion object { const val TAG = "AudioDspManager" }
}
