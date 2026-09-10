package com.ajaxjiang.folddepth.ui

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.pow

@Composable
fun FoldDepthDemo(
    hingeAngle: Float,
    hardwareHingeAvailable: Boolean,
) {
    val progress = (hingeAngle / 180f).coerceIn(0f, 1f)
    val eased = smoothstep(progress)

    // First-pass prototype: uniform RenderEffect blur driven by the real hinge angle.
    // Next step is a spatial blur mask / RuntimeShader so blur is strongest near the
    // perceived far plane rather than uniform across the layer.
    val blurPx = 34f * (1f - eased).pow(1.35f)
    val scale = 0.86f + 0.14f * eased
    val alpha = 0.18f + 0.82f * eased
    val translationY = 72f * (1f - eased)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                    this.translationY = translationY
                    if (Build.VERSION.SDK_INT >= 31 && blurPx > 0.25f) {
                        renderEffect = RenderEffect
                            .createBlurEffect(blurPx, blurPx, Shader.TileMode.CLAMP)
                            .asComposeRenderEffect()
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

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp),
        ) {
            Text(
                text = "%3.0f°".format(hingeAngle),
                color = Color.White,
                fontSize = 22.sp,
            )
            Text(
                text = if (hardwareHingeAvailable) "hinge sensor · live" else "hinge sensor unavailable · fallback 180°",
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 12.sp,
            )
        }
    }
}

private fun smoothstep(x: Float): Float = x * x * (3f - 2f * x)
