package com.ajaxjiang.folddepth

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ajaxjiang.folddepth.display.OuterDisplayManager
import com.ajaxjiang.folddepth.sensor.HingeAngleSource
import com.ajaxjiang.folddepth.ui.FoldDepthDemo

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val hingeSource = remember { HingeAngleSource(applicationContext) }
            val outerDisplayManager = remember { OuterDisplayManager(this) }
            val foldState by hingeSource.foldState
            val lifecycleOwner = LocalLifecycleOwner.current

            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_START -> hingeSource.start()
                        Lifecycle.Event.ON_STOP -> {
                            hingeSource.stop()
                            outerDisplayManager.release()
                        }
                        else -> Unit
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                    hingeSource.stop()
                    outerDisplayManager.release()
                }
            }

            LaunchedEffect(foldState) {
                outerDisplayManager.update(foldState, null)
            }

            FoldDepthDemo(
                foldState = foldState,
                onSimulateAngleChange = { angle -> hingeSource.setSimulatedAngle(angle) },
                onToggleSimulation = { enabled -> hingeSource.setSimulated(enabled) },
            )
        }
    }
}
