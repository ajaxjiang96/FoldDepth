package com.ajaxjiang.folddepth

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.ajaxjiang.folddepth.sensor.HingeAngleSource
import com.ajaxjiang.folddepth.ui.FoldDepthDemo

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val hinge = remember { HingeAngleSource(this) }
            val angle by hinge.angle

            DisposableEffect(Unit) {
                hinge.start()
                onDispose { hinge.stop() }
            }

            FoldDepthDemo(
                hingeAngle = angle,
                hardwareHingeAvailable = hinge.isAvailable,
            )
        }
    }
}
