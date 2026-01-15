package io.sensify.sensor.ui.pages.helivibe

import android.app.Application
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.sensify.sensor.domains.helivibe.HeliVibeEngine
import io.sensify.sensor.domains.helivibe.HeliVibeState
import io.sensify.sensor.domains.sensors.packets.SensorPacketConfig
import io.sensify.sensor.domains.sensors.packets.SensorPacketsProvider
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class HeliVibeViewModel : ViewModel() {

    private val engine = HeliVibeEngine()
    val uiState: StateFlow<HeliVibeState> = engine.state

    init {
        // Register Linear Acceleration
        val config = SensorPacketConfig(
            sensorType = Sensor.TYPE_LINEAR_ACCELERATION,
            sensorDelay = SensorManager.SENSOR_DELAY_FASTEST
        )
        SensorPacketsProvider.getInstance().attachSensor(config)

        SensorPacketsProvider.getInstance().mSensorPacketFlow
            .onEach { packet ->
                val values = packet.values
                if (packet.type == Sensor.TYPE_LINEAR_ACCELERATION && values != null && values.size >= 3) {
                    engine.processSample(
                        values[0],
                        values[1],
                        values[2]
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun onTare() {
        engine.tare()
    }

    override fun onCleared() {
        super.onCleared()
        SensorPacketsProvider.getInstance().detachSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    }

    @Suppress("UNCHECKED_CAST")
    class Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HeliVibeViewModel::class.java)) {
                return HeliVibeViewModel() as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
