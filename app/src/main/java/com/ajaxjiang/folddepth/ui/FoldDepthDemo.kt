package com.ajaxjiang.folddepth.ui

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ajaxjiang.folddepth.model.FoldState
import com.ajaxjiang.folddepth.model.calculateFoldVisualParams

@Composable
fun FoldDepthDemo(
    foldState: FoldState,
    onSimulateAngleChange: (Float) -> Unit,
    onToggleSimulation: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val visualParams = remember(foldState.progress) {
        calculateFoldVisualParams(foldState.progress)
    }

    var showHardwareOverrideSlider by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        // --- Visual Depth Layer: UI = f(hingeAngle) ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = visualParams.scale
                    scaleY = visualParams.scale
                    alpha = visualParams.alpha
                    translationY = visualParams.translationY
                    if (Build.VERSION.SDK_INT >= 31 && visualParams.blurPx > 0.25f) {
                        renderEffect = RenderEffect
                            .createBlurEffect(
                                visualParams.blurPx,
                                visualParams.blurPx,
                                Shader.TileMode.CLAMP,
                            )
                            .asComposeRenderEffect()
                    } else if (Build.VERSION.SDK_INT >= 31) {
                        renderEffect = null
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "FOLD",
                    color = Color.White,
                    fontSize = 58.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 12.sp,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "DEPTH",
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 18.sp,
                    letterSpacing = 8.sp,
                )
            }
        }

        // --- Top Status Header: Technical parameters readout ---
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "FOLDDEPTH",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp,
                )
                Text(
                    text = "UI = f(hingeAngle)",
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (foldState.isHardwareAvailable && !foldState.isSimulated) {
                    Color(0xFF1E3A2F)
                } else {
                    Color(0xFF2C2416)
                },
            ) {
                Text(
                    text = when {
                        foldState.isHardwareAvailable && !foldState.isSimulated -> "LIVE HINGE SENSOR"
                        foldState.isHardwareAvailable && foldState.isSimulated -> "SIMULATION OVERRIDE"
                        else -> "DEBUG SIMULATION (NO SENSOR)"
                    },
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    color = if (foldState.isHardwareAvailable && !foldState.isSimulated) {
                        Color(0xFF4ADE80)
                    } else {
                        Color(0xFFFBBF24)
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp,
                )
            }
        }

        // --- Bottom Controls & Technical HUD ---
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(24.dp),
        ) {
            // HUD readouts: Angle, Progress, Visual metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column {
                    Text(
                        text = "%3.1f°".format(foldState.angle),
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    )
                    Text(
                        text = "progress: %.2f (%d%%)".format(
                            foldState.progress,
                            (foldState.progress * 100f).toInt(),
                        ),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "blur: %.1fpx".format(visualParams.blurPx),
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                    Text(
                        text = "scale: %.2f | alpha: %.2f".format(visualParams.scale, visualParams.alpha),
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                    Text(
                        text = "transY: %.1fpx".format(visualParams.translationY),
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Debug slider visibility logic:
            // When real sensor is NOT available: ALWAYS show slider prominently
            // When real sensor IS available: show secondary toggle to reveal override slider
            val isSliderVisible = !foldState.isHardwareAvailable || showHardwareOverrideSlider

            if (foldState.isHardwareAvailable) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = {
                            val newMode = !showHardwareOverrideSlider
                            showHardwareOverrideSlider = newMode
                            onToggleSimulation(newMode)
                        },
                    ) {
                        Text(
                            text = if (showHardwareOverrideSlider) "Switch to Live Sensor" else "Simulate 0°–180°",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isSliderVisible,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF141414), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "0° (Closed)",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 11.sp,
                        )
                        Text(
                            text = "Debug Hinge Angle Slider",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = "180° (Open)",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 11.sp,
                        )
                    }

                    Slider(
                        value = foldState.angle,
                        onValueChange = { newAngle ->
                            if (foldState.isHardwareAvailable && !foldState.isSimulated) {
                                onToggleSimulation(true)
                            }
                            onSimulateAngleChange(newAngle)
                        },
                        valueRange = 0f..180f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White.copy(alpha = 0.85f),
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f),
                        ),
                    )
                }
            }
        }
    }
}
