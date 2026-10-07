package com.squig.equalizer.dsp

import android.media.audiofx.DynamicsProcessing
import com.squig.equalizer.models.EqProfile

class AudioDspManager {

    private var dpEffect: DynamicsProcessing? = null

    /**
     * Initializes DynamicsProcessing for system-wide audio (Session 0).
     */
    fun initEffect(audioSessionId: Int = 0, bandCount: Int = 10) {
        release()

        val config = DynamicsProcessing.Config.Builder(
            DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
            2, // Stereo (Channel count)
            true, // Enable PreEQ stage
            bandCount, // Number of EQ bands
            false, // No MBC (Multi-Band Compressor) needed
            0,
            false, // No PostEQ stage needed
            0,
            true  // Enable Limiter (protects against accidental clipping)
        ).build()

        dpEffect = DynamicsProcessing(0, audioSessionId, config).apply {
            enabled = true
        }
    }

    /**
     * Applies the custom EqProfile (Preamp Gain + Band Gains) to the DSP effect.
     */
    fun applyProfile(profile: EqProfile) {
        val effect = dpEffect ?: return

        // 1. Apply Preamp Gain (in dB) across both channels (0 = Left, 1 = Right)
        effect.setInputGainByChannelIndex(0, profile.preampGainDb)
        effect.setInputGainByChannelIndex(1, profile.preampGainDb)

        // 2. Configure each EQ band frequency and gain
        profile.bands.forEachIndexed { index, band ->
            val eqBand = DynamicsProcessing.EqBand(true, band.frequency, band.gainDb)
            effect.setPreEqBandAllChannelsTo(index, eqBand)
        }
    }

    /**
     * Toggle equalizer effect on or off.
     */
    fun setEnabled(enabled: Boolean) {
        dpEffect?.enabled = enabled
    }

    /**
     * Release DSP resources when stopping.
     */
    fun release() {
        dpEffect?.enabled = false
        dpEffect?.release()
        dpEffect = null
    }
}
