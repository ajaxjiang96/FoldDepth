package com.ajaxjiang.folddepth.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf

class HingeAngleSource(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val hingeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HINGE_ANGLE)

    private val _angle = mutableFloatStateOf(180f)
    val angle: State<Float> = _angle

    val isAvailable: Boolean
        get() = hingeSensor != null

    fun start() {
        hingeSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_HINGE_ANGLE) {
            _angle.floatValue = event.values.firstOrNull()?.coerceIn(0f, 180f) ?: return
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
