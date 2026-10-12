package com.squig.equalizer.models

// Single frequency point from Squig.link raw file (Frequency, SPL/dB)
data class FreqPoint(
    val frequency: Float,
    val rawDb: Float
)

// Single EQ Band configuration
data class EqBand(
    val frequency: Float,
    val gainDb: Float,
    val qFactor: Float = 1.414f
)

// Complete Equalizer Profile including Preamp Gain
data class EqProfile(
    val name: String,
    val preampGainDb: Float,
    val bands: List<EqBand>
)

// What the web UI sends to the audio engine
data class EqSettings(
    val enabled: Boolean,
    val postGainDb: Float,
    val gains: List<Float>,        // one gain (dB) per EqConstants.CENTERS entry
    val autoPreamp: Boolean = true // lower level by the largest boost to avoid clipping
) {
    companion object {
        val FLAT = EqSettings(false, 0f, List(32) { 0f })
    }
}
