package com.squig.equalizer.parser

import com.squig.equalizer.models.FreqPoint
import java.io.File
import java.net.URL

class SquigParser {

    /**
     * Parses raw Squig.link text content into a list of FreqPoint items.
     */
    fun parseRawText(rawContent: String): List<FreqPoint> {
        return rawContent.lineSequence()
            .map { it.trim() }
            .filter { line -> line.isNotEmpty() && !line.startsWith("#") && !line.startsWith("Frequency") }
            .mapNotNull { line ->
                val tokens = line.split("\\s+".toRegex())
                if (tokens.size >= 2) {
                    val freq = tokens[0].toFloatOrNull()
                    val db = tokens[1].toFloatOrNull()
                    if (freq != null && db != null) {
                        FreqPoint(freq, db)
                    } else null
                } else null
            }
            .toList()
    }

    /**
     * Calculates the delta curve needed to auto-eq an IEM/headphone to a target curve.
     * Delta = Target SPL - Source SPL
     */
    fun calculateEqDelta(source: List<FreqPoint>, target: List<FreqPoint>): List<FreqPoint> {
        val targetMap = target.associateBy { it.frequency }
        return source.mapNotNull { src ->
            val targetPt = targetMap[src.frequency]
            if (targetPt != null) {
                FreqPoint(src.frequency, targetPt.rawDb - src.rawDb)
            } else null
        }
    }
}
