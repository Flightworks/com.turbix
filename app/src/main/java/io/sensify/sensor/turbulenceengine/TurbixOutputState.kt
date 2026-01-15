package io.sensify.sensor.turbulenceengine

data class TurbixOutputState(
    val absoluteTurbulence: Float = 0f,
    val relativeTurbulence: Float = 0f,
    val samplingRate: Float = 0f
)
