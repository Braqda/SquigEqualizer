package com.squig.equalizer.dsp

import com.squig.equalizer.models.EqBand
import com.squig.equalizer.models.EqProfile
import com.squig.equalizer.models.FreqPoint
import kotlin.math.max

class EqEngine {

    // Standard 10-Band ISO Center Frequencies (Hz)
    val standardFrequencies = listOf(31.25f, 62.5f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f)

    /**
     * Maps an arbitrary delta curve to fixed EQ bands and auto-calculates Preamp Gain.
     */
    fun generateProfile(profileName: String, deltaCurve: List<FreqPoint>): EqProfile {
        val bands = mutableListOf<EqBand>()
        var maxGain = 0.0f

        for (centerFreq in standardFrequencies) {
            // Find closest frequency in curve
            val closest = deltaCurve.minByOrNull { Math.abs(it.frequency - centerFreq) }
            val gain = closest?.rawDb ?: 0.0f
            
            bands.add(EqBand(frequency = centerFreq, gainDb = gain))
            maxGain = max(maxGain, gain)
        }

        // Automatic Preamp Gain reduction to prevent digital audio clipping
        val preampGain = if (maxGain > 0f) -maxGain else 0.0f

        return EqProfile(
            name = profileName,
            preampGainDb = preampGain,
            bands = bands
        )
    }
}
