package io.sensify.sensor.turbulenceengine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt

class TurbulenceEngine {

    private val _outputState = MutableStateFlow(TurbixOutputState())
    val outputState: StateFlow<TurbixOutputState> = _outputState.asStateFlow()

    private var tareOffset: Float = 0f
    private val buffer = ArrayDeque<Float>()
    private var maxWindowSize = 100 // Default window size, can be adjusted

    // Configuration
    private var samplingDurationSeconds: Float = 1.0f
    private var currentFs: Float = 100.0f

    fun setRotorConfiguration(rpm: Float, numBlades: Int, fs: Float) {
        currentFs = fs
        updateWindowSize()
    }

    fun setSamplingDuration(durationSeconds: Float) {
        samplingDurationSeconds = durationSeconds
        updateWindowSize()
    }

    private fun updateWindowSize() {
        val targetSize = (currentFs * samplingDurationSeconds).toInt()
        if (targetSize > 0) {
             maxWindowSize = targetSize
             // Trim buffer immediately if needed
             while (buffer.size > maxWindowSize) {
                 buffer.removeFirst()
             }
        }
    }

    fun processAccelerometerData(timestamp: Long, x: Float, y: Float, z: Float, fs: Float) {
        currentFs = fs

        // Calculate magnitude of Linear Acceleration
        val magnitude = sqrt(x*x + y*y + z*z)

        buffer.addLast(magnitude)

        updateWindowSize()

        while (buffer.size > maxWindowSize) {
            buffer.removeFirst()
        }

        // Calculate RMS
        if (buffer.isNotEmpty()) {
            val sumSq = buffer.sumOf { (it * it).toDouble() }
            val rms = sqrt(sumSq / buffer.size).toFloat()

            val relative = rms - tareOffset

            _outputState.value = TurbixOutputState(
                absoluteTurbulence = rms,
                relativeTurbulence = relative,
                samplingRate = fs
            )
        }
    }

    fun performTare() {
        // Set current absolute as offset
        tareOffset = _outputState.value.absoluteTurbulence
    }

    fun reset() {
        tareOffset = 0f
        buffer.clear()
        _outputState.value = TurbixOutputState()
    }
}
