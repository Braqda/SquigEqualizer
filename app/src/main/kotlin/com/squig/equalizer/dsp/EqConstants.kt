package com.squig.equalizer.dsp

import kotlin.math.sqrt

/** The 32 band centres used by the web UI (must match `isoFreqs` in WebServer.kt). */
object EqConstants {
    val CENTERS = floatArrayOf(
        20f, 25f, 31.5f, 40f, 50f, 63f, 80f, 100f, 125f, 160f, 200f, 250f, 315f, 400f, 500f, 630f,
        800f, 1000f, 1250f, 1600f, 2000f, 2500f, 3150f, 4000f, 5000f, 6300f, 8000f, 10000f,
        12500f, 16000f, 18000f, 20000f
    )
    const val BAND_COUNT = 32

    /**
     * DynamicsProcessing bands are defined by their UPPER cutoff, not their centre,
     * so use the geometric midpoint between neighbouring centres.
     */
    val CUTOFFS = FloatArray(BAND_COUNT) { i ->
        if (i < BAND_COUNT - 1) sqrt(CENTERS[i] * CENTERS[i + 1]) else 21000f
    }
}
