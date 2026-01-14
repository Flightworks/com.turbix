package io.sensify.sensor.domains.helivibe

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt
import kotlin.math.max
import kotlin.math.roundToInt

data class HeliVibeState(
    val liveRms: Float = 0f,
    val baselineRms: Float = 0f,
    val deltaRms: Float = 0f,
    val score: Int = 0,
    val colorState: Int = 0 // 0: Green, 1: Yellow, 2: Amber, 3: Orange, 4: Red
)

class HeliVibeEngine {
    private val _state = MutableStateFlow(HeliVibeState())
    val state = _state.asStateFlow()

    private val bufferSize = 200 // 2 seconds at ~100Hz
    private val buffer = ArrayDeque<Float>(bufferSize)

    // ISO 2631-1 Thresholds
    // < 0.1 -> 0 (Green)
    // 0.1 - 0.3 -> 1-2 (Green)
    // 0.3 - 0.5 -> 3-4 (Yellow)
    // 0.5 - 0.8 -> 5-6 (Amber)
    // 0.8 - 1.6 -> 7-8 (Orange)
    // > 1.6 -> 9-10 (Red)

    fun processSample(x: Float, y: Float, z: Float) {
        val magnitude = sqrt(x * x + y * y + z * z)

        // Add to buffer
        if (buffer.size >= bufferSize) {
            buffer.removeFirst()
        }
        buffer.addLast(magnitude)

        // Calculate RMS of buffer
        // optimization: maintain sum of squares if needed, but 200 loop is fine
        var sumSq = 0.0
        for (v in buffer) {
            sumSq += v * v
        }

        val rms = if (buffer.isNotEmpty()) sqrt(sumSq / buffer.size).toFloat() else 0f

        val baseline = _state.value.baselineRms
        val delta = max(0f, rms - baseline)

        val score = calculateScore(delta)
        val color = calculateColor(score)

        _state.value = _state.value.copy(
            liveRms = rms,
            deltaRms = delta,
            score = score,
            colorState = color
        )
    }

    fun tare() {
        val currentRms = _state.value.liveRms
        _state.value = _state.value.copy(baselineRms = currentRms)
    }

    private fun calculateScore(delta: Float): Int {
        val s = when {
            delta < 0.1 -> 0.0 // 0
            delta < 0.3 -> interpolate(delta, 0.1, 0.3, 1.0, 2.0)
            delta < 0.5 -> interpolate(delta, 0.3, 0.5, 3.0, 4.0)
            delta < 0.8 -> interpolate(delta, 0.5, 0.8, 5.0, 6.0)
            delta < 1.6 -> interpolate(delta, 0.8, 1.6, 7.0, 8.0)
            else -> interpolate(delta, 1.6, 3.2, 9.0, 10.0) // Cap at 10 logic approximately
        }
        return s.roundToInt().coerceIn(0, 10)
    }

    private fun interpolate(v: Float, x0: Double, x1: Double, y0: Double, y1: Double): Double {
        return y0 + (v - x0) * (y1 - y0) / (x1 - x0)
    }

    private fun calculateColor(score: Int): Int {
        return when (score) {
            0, 1, 2 -> 0 // Green
            3, 4 -> 1 // Yellow
            5, 6 -> 2 // Amber
            7, 8 -> 3 // Orange
            else -> 4 // Red
        }
    }
}
