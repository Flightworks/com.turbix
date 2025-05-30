package io.sensify.sensor.ui.pages.turbix

import android.app.Application
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.sensify.sensor.turbulenceengine.TurbulenceEngine
import io.sensify.sensor.turbulenceengine.TurbixOutputState
import io.sensify.sensor.domains.sensors.packets.ModelSensorPacket
import io.sensify.sensor.domains.sensors.packets.SensorPacketConfig
import io.sensify.sensor.domains.sensors.packets.SensorPacketsProvider
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.math.abs // Ensure abs is imported

class TurbixViewModel(application: Application) : AndroidViewModel(application) {

    private val turbulenceEngine = TurbulenceEngine()
    val uiState: StateFlow<TurbixOutputState> = turbulenceEngine.outputState

    private var lastTimestampNanos: Long = 0L
    private var sampleCount: Int = 0
    private var firstTimestampNanos: Long = 0L
    private var estimatedFs: Float = 100.0f // Default to 100Hz, PRD suggests 100Hz default

    // Default values for rotor configuration
    private var currentRpm: Float = 0f // Default to 0 RPM (notch bypassed)
    private var currentNumBlades: Int = 2 // Default to 2 blades

    init {
        val accelerometerConfig = SensorPacketConfig(
            sensorType = Sensor.TYPE_ACCELEROMETER,
            sensorDelay = SensorManager.SENSOR_DELAY_GAME
        )
        SensorPacketsProvider.getInstance().attachSensor(accelerometerConfig)

        // Initial configuration for the engine with default/initial estimatedFs
        turbulenceEngine.setRotorConfiguration(currentRpm, currentNumBlades, estimatedFs)

        SensorPacketsProvider.getInstance().mSensorPacketFlow
            .onEach { packet ->
                if (packet.type == Sensor.TYPE_ACCELEROMETER) {
                    processSensorPacket(packet)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun processSensorPacket(packet: ModelSensorPacket) {
        val currentTimestampNanos = packet.sensorEvent.timestamp
        var fsJustUpdated = false

        if (lastTimestampNanos != 0L && currentTimestampNanos > lastTimestampNanos) {
            sampleCount++
            if (sampleCount == 1) {
                firstTimestampNanos = currentTimestampNanos
            }

            if (sampleCount >= 100) { // Windowed estimation
                val elapsedTimeNanos = currentTimestampNanos - firstTimestampNanos
                if (elapsedTimeNanos > 0) {
                    val newFs = (sampleCount.toFloat() * 1_000_000_000L) / elapsedTimeNanos
                    if (abs(newFs - estimatedFs) > 1.0f) { // Update if change > 1Hz
                        estimatedFs = newFs
                        fsJustUpdated = true
                    }
                }
                sampleCount = 0
            } else { // Instantaneous estimation
                val dtNanos = currentTimestampNanos - lastTimestampNanos
                if (dtNanos > 0) {
                    val newFs = 1_000_000_000.0f / dtNanos
                     if (abs(newFs - estimatedFs) > 1.0f) {
                        estimatedFs = newFs
                        fsJustUpdated = true
                    }
                }
            }
        }
        lastTimestampNanos = currentTimestampNanos

        if (fsJustUpdated) {
            turbulenceEngine.setRotorConfiguration(currentRpm, currentNumBlades, estimatedFs)
        }

        if (packet.values.size >= 3) {
            turbulenceEngine.processAccelerometerData(
                timestamp = packet.timestamp,
                x = packet.values[0],
                y = packet.values[1],
                z = packet.values[2],
                fs = estimatedFs
            )
        }
    }

    fun setRotorRpm(rpm: Float) {
        currentRpm = rpm
        turbulenceEngine.setRotorConfiguration(currentRpm, currentNumBlades, estimatedFs)
    }

    fun setNumBlades(blades: Int) {
        currentNumBlades = blades
        turbulenceEngine.setRotorConfiguration(currentRpm, currentNumBlades, estimatedFs)
    }

    fun performTare() {
        turbulenceEngine.performTare()
    }

    override fun onCleared() {
        super.onCleared()
        SensorPacketsProvider.getInstance().detachSensor(Sensor.TYPE_ACCELEROMETER)
        turbulenceEngine.reset()
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TurbixViewModel::class.java)) {
                return TurbixViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
