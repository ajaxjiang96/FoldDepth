package com.ajaxjiang.folddepth.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import com.ajaxjiang.folddepth.model.FoldState

/**
 * Manages the physical hinge angle sensor (`Sensor.TYPE_HINGE_ANGLE`) and provides
 * fallback developer simulation support when running on non-foldables or emulators.
 *
 * Maximizes event sampling rate and responsiveness by:
 * 1. Requesting `SensorManager.SENSOR_DELAY_FASTEST` (0 µs delay).
 * 2. Setting `maxReportLatencyUs = 0` (zero FIFO batching latency).
 * 3. Dispatching sensor callbacks on a dedicated high-priority `HandlerThread`
 *    (`THREAD_PRIORITY_URGENT_DISPLAY`) to avoid main UI thread scheduling contention.
 * 4. Tracking real-time event sampling frequency (Hz).
 */
class HingeAngleSource(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val hingeSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_HINGE_ANGLE)

    val isHardwareAvailable: Boolean = hingeSensor != null

    private val _hardwareAngle = mutableFloatStateOf(180f)
    private val _simulatedAngle = mutableFloatStateOf(180f)
    private val _isSimulated = mutableStateOf(!isHardwareAvailable)
    private val _sensorRateHz = mutableIntStateOf(0)

    val isSimulated: State<Boolean> = _isSimulated
    val simulatedAngle: State<Float> = _simulatedAngle
    val hardwareAngle: State<Float> = _hardwareAngle
    val sensorRateHz: State<Int> = _sensorRateHz

    // Sampling rate calculation state
    private var eventCount = 0
    private var lastRateCalcTimeNs = 0L

    @Volatile
    private var isRunning = false
    private var sensorThread: HandlerThread? = null
    private var sensorHandler: Handler? = null

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
            sensorRateHz = if (!simulated && isHardwareAvailable) _sensorRateHz.intValue else 0,
        )
    }

    fun setSimulatedAngle(angle: Float) {
        _simulatedAngle.floatValue = angle.coerceIn(0f, 180f)
    }

    fun setSimulated(enabled: Boolean) {
        _isSimulated.value = enabled
    }

    @Synchronized
    fun start() {
        if (isRunning) return
        hingeSensor?.let { sensor ->
            // Dedicated high-priority thread for immediate sensor interrupt processing
            val thread = HandlerThread("FoldHingeSensorThread", Process.THREAD_PRIORITY_URGENT_DISPLAY).apply {
                start()
            }
            sensorThread = thread
            val handler = Handler(thread.looper)
            sensorHandler = handler

            // SENSOR_DELAY_FASTEST (0 µs) + maxReportLatencyUs = 0:
            // Instructs the kernel HAL to deliver events at the absolute maximum hardware rate without batching.
            val registered = sensorManager?.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_FASTEST,
                0,
                handler,
            ) ?: false

            if (!registered) {
                // Fallback overload if 5-parameter registration is rejected by OEM HAL
                sensorManager?.registerListener(
                    this,
                    sensor,
                    SensorManager.SENSOR_DELAY_FASTEST,
                    handler,
                )
            }
            isRunning = true
        }
    }

    @Synchronized
    fun stop() {
        if (!isRunning) return
        sensorManager?.unregisterListener(this)
        sensorThread?.quitSafely()
        sensorThread = null
        sensorHandler = null
        isRunning = false
        _sensorRateHz.intValue = 0
        eventCount = 0
        lastRateCalcTimeNs = 0L
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_HINGE_ANGLE) {
            val raw = event.values.firstOrNull() ?: return
            _hardwareAngle.floatValue = raw.coerceIn(0f, 180f)

            // Calculate live sampling rate (Hz)
            val now = System.nanoTime()
            eventCount++
            if (lastRateCalcTimeNs == 0L) {
                lastRateCalcTimeNs = now
            } else {
                val elapsed = now - lastRateCalcTimeNs
                if (elapsed >= 500_000_000L) { // update every 0.5s for snappy responsiveness
                    val hz = ((eventCount.toDouble() / elapsed.toDouble()) * 1_000_000_000L).toInt()
                    _sensorRateHz.intValue = hz
                    eventCount = 0
                    lastRateCalcTimeNs = now
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
