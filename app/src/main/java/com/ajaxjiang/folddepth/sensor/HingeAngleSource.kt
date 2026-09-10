package com.ajaxjiang.folddepth.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import com.ajaxjiang.folddepth.model.FoldState

/**
 * Manages the physical hinge angle sensor (`Sensor.TYPE_HINGE_ANGLE`) and provides
 * fallback developer simulation support when running on non-foldables or emulators.
 */
class HingeAngleSource(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val hingeSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_HINGE_ANGLE)

    val isHardwareAvailable: Boolean = hingeSensor != null

    private val _hardwareAngle = mutableFloatStateOf(180f)
    private val _simulatedAngle = mutableFloatStateOf(180f)
    private val _isSimulated = mutableStateOf(!isHardwareAvailable)

    val isSimulated: State<Boolean> = _isSimulated
    val simulatedAngle: State<Float> = _simulatedAngle
    val hardwareAngle: State<Float> = _hardwareAngle

    val foldState: State<FoldState> = derivedStateOf {
        val simulated = _isSimulated.value
        val angle = if (simulated || !isHardwareAvailable) {
            _simulatedAngle.floatValue
        } else {
            _hardwareAngle.floatValue
        }
        FoldState(
            angle = angle,
            isHardwareAvailable = isHardwareAvailable,
            isSimulated = simulated,
        )
    }

    fun setSimulatedAngle(angle: Float) {
        _simulatedAngle.floatValue = angle.coerceIn(0f, 180f)
    }

    fun setSimulated(enabled: Boolean) {
        _isSimulated.value = enabled
    }

    fun start() {
        hingeSensor?.let { sensor ->
            sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_HINGE_ANGLE) {
            val raw = event.values.firstOrNull() ?: return
            _hardwareAngle.floatValue = raw.coerceIn(0f, 180f)
            // If live sensor is active and receiving events, switch out of simulation
            if (!_isSimulated.value) {
                // Live sensor is actively driving
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
