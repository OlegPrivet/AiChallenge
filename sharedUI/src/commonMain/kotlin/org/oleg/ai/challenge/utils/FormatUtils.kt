package org.oleg.ai.challenge.utils

import kotlin.math.round

/**
 * Formats a double/float to a string with specified decimal places
 * Multiplatform-compatible alternative to String.format()
 */
fun Double.format(decimals: Int): String {
    val multiplier = when (decimals) {
        1 -> 10.0
        2 -> 100.0
        3 -> 1000.0
        4 -> 10000.0
        else -> 100.0
    }
    return "${round(this * multiplier) / multiplier}"
}

fun Float.format(decimals: Int): String = this.toDouble().format(decimals)
