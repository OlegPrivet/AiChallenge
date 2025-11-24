package org.oleg.ai.challenge.domain.rag.embedding

import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

object EmbeddingMath {
    fun l2Normalize(values: List<Float>): List<Float> {
        val norm = sqrt(values.sumOf { it.toDouble().pow(2.0) })
        if (norm == 0.0) return values
        return values.map { (it / norm).toFloat() }
    }

    /**
     * Lightweight FP16-ish quantization by clamping mantissa precision.
     */
    fun quantizeFp16(values: List<Float>): List<Float> {
        return values.map { value ->
            val scaled = (value * 1024.0f).roundToInt() / 1024.0f
            scaled
        }
    }
}
