package com.squig.equalizer

import com.squig.equalizer.dsp.EqEngine
import com.squig.equalizer.parser.SquigParser

fun main() {
    println("==========================================")
    println("      Squig.link Equalizer Engine         ")
    println("==========================================")

    // Sample KZ Castor Frequency Response
    val iemResponseText = """
        20.0    65.0
        31.25   66.5
        62.5    68.0
        125.0   64.0
        250.0   60.0
        500.0   58.0
        1000.0  58.5
        2000.0  62.0
        4000.0  65.0
        8000.0  60.0
        16000.0 50.0
    """.trimIndent()

    // Harman 2019 In-Ear Target Curve
    val targetResponseText = """
        20.0    69.0
        31.25   70.0
        62.5    69.5
        125.0   65.0
        250.0   60.0
        500.0   58.0
        1000.0  59.0
        2000.0  63.5
        4000.0  63.0
        8000.0  57.0
        16000.0 48.0
    """.trimIndent()

    val parser = SquigParser()
    val iemPoints = parser.parseRawText(iemResponseText)
    val targetPoints = parser.parseRawText(targetResponseText)

    val deltaCurve = parser.calculateEqDelta(iemPoints, targetPoints)
    val engine = EqEngine()
    val profile = engine.generateProfile("Auto-EQ Harman Target", deltaCurve)

    println("\nGenerated Profile: ${profile.name}")
    println("Auto Preamp Gain: ${"%.2f".format(profile.preampGainDb)} dB")
    println("\n10-Band EQ Configurations:")
    println("------------------------------------------")
    println("%-12s | %-12s | %-8s".format("Freq (Hz)", "Gain (dB)", "Q Factor"))
    println("------------------------------------------")
    profile.bands.forEach { band ->
        println("%-12.2f | %-12.2f | %-8.3f".format(band.frequency, band.gainDb, band.qFactor))
    }
    println("------------------------------------------")
    println("\nDSP Engine ready for system-wide audio service!")
}
