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
