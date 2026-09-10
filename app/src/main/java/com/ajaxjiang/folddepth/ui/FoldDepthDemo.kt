package com.ajaxjiang.folddepth.ui

import android.graphics.BitmapFactory
import android.graphics.RenderEffect
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
    val visualParams = remember(foldState.angle) {
        calculateFoldVisualParams(foldState)
    }

    var customBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var showHardwareOverrideSlider by remember { mutableStateOf(false) }
    var showOuterPreviewExpanded by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream)
                    if (bmp != null) {
                        customBitmap = bmp.asImageBitmap()
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        // ====================================================================
        // INNER SCREEN SURFACE: Split down the center hinge
        // Left Half: Horizontal gradient blur (blurrier to the left)
        // Right Half: Unchanged & 100% crisp
        // ====================================================================
        Row(
            modifier = Modifier.fillMaxSize(),
        ) {
            // --- LEFT HALF (Folds & Blurs horizontally from right to left) ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .graphicsLayer {
                        rotationY = visualParams.rotationYLeft
                        cameraDistance = 16f * density
                        transformOrigin = TransformOrigin(1f, 0.5f) // pivot along center hinge
                    },
            ) {
                // Base sharp layer
                WallpaperHalfView(
                    isLeftHalf = true,
                    customBitmap = customBitmap,
                    modifier = Modifier.fillMaxSize(),
                )

                // Hardware-accelerated horizontal gradient blur overlay
                if (Build.VERSION.SDK_INT >= 31 && visualParams.innerLeftMaxBlurPx > 0.3f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                            .graphicsLayer {
                                renderEffect = RenderEffect
                                    .createBlurEffect(
                                        visualParams.innerLeftMaxBlurPx,
                                        visualParams.innerLeftMaxBlurPx,
                                        Shader.TileMode.CLAMP,
                                    )
                                    .asComposeRenderEffect()
                            }
                            .drawWithContent {
                                drawContent()
                                // Alpha mask: 1.0 at far left (max blur), 0.0 at center hinge (zero blur)
                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(Color.Black, Color.Transparent),
                                        startX = 0f,
                                        endX = size.width,
                                    ),
                                    blendMode = BlendMode.DstIn,
                                )
                            },
                    ) {
                        WallpaperHalfView(
                            isLeftHalf = true,
                            customBitmap = customBitmap,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }

            // --- CENTER HINGE CREASE SEPARATOR LINE ---
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(Color.White.copy(alpha = 0.12f)),
            )

            // --- RIGHT HALF (Unchanged, 100% Crisp) ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                WallpaperHalfView(
                    isLeftHalf = false,
                    customBitmap = customBitmap,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        // ====================================================================
        // TOP STATUS BAR & CONTROLS HUD
        // ====================================================================
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "FOLDDEPTH",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                )
                Text(
                    text = "Inner Left: Gradient Blur | Right: Crisp",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Screenshot / Wallpaper Picker Chip
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White.copy(alpha = 0.12f),
                    modifier = Modifier
                        .clickable {
                            if (customBitmap != null) {
                                customBitmap = null
                            } else {
                                imagePickerLauncher.launch("image/*")
                            }
                        }
                        .padding(end = 8.dp),
                ) {
                    Text(
                        text = if (customBitmap != null) "Reset Preset" else "Import Screenshot",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }

                // Sensor Status Chip
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (foldState.isHardwareAvailable && !foldState.isSimulated) {
                        Color(0xFF166534)
                    } else {
                        Color(0xFF854D0E)
                    },
                ) {
                    Text(
                        text = when {
                            foldState.isHardwareAvailable && !foldState.isSimulated -> "LIVE HINGE SENSOR"
                            foldState.isHardwareAvailable && foldState.isSimulated -> "SIMULATED"
                            else -> "NO SENSOR (DEBUG)"
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp,
                    )
                }
            }
        }

        // ====================================================================
        // DUAL-SCREEN HUD / COVER DISPLAY PREVIEW (Activated when angle <= 90°)
        // ====================================================================
        AnimatedVisibility(
            visible = visualParams.isOuterScreenActive,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 54.dp, end = 24.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF10141E).copy(alpha = 0.95f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                shadowElevation = 12.dp,
                modifier = Modifier
                    .clickable { showOuterPreviewExpanded = !showOuterPreviewExpanded }
                    .width(if (showOuterPreviewExpanded) 240.dp else 150.dp)
                    .height(if (showOuterPreviewExpanded) 340.dp else 210.dp),
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "COVER SCREEN (OUTER)",
                            color = Color(0xFF38BDF8),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                        )
                        Text(
                            text = "blur: %.1f".format(visualParams.outerBlurPx),
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    // Outer Screen Content: Shows Right Half with inverted gradient blur
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(10.dp))
                            .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                    ) {
                        OuterScreenView(
                            foldState = foldState,
                            customBitmap = customBitmap,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }

        // ====================================================================
        // BOTTOM TECHNICAL HUD & DEBUG ANGLE SLIDER
        // ====================================================================
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(24.dp),
        ) {
            // Numeric HUD readout
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
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Inner Left Blur: %.1fpx (gradient)".format(visualParams.innerLeftMaxBlurPx),
                        color = Color(0xFF67E8F9),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                    Text(
                        text = "Inner Right Blur: 0.0px (crisp)",
                        color = Color(0xFF4ADE80),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                    Text(
                        text = if (visualParams.isOuterScreenActive) {
                            "Outer Display: ON (blur %.1fpx)".format(visualParams.outerBlurPx)
                        } else {
                            "Outer Display: OFF (angle > 90°)"
                        },
                        color = if (visualParams.isOuterScreenActive) Color(0xFFFBBF24) else Color.White.copy(alpha = 0.4f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Slider visibility
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
                            color = Color.White.copy(alpha = 0.75f),
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
                        .background(Color(0xFF141414).copy(alpha = 0.92f), RoundedCornerShape(14.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "0° (Closed)",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                        )
                        Text(
                            text = "Hinge Angle Simulation Slider",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = "180° (Flat)",
                            color = Color.White.copy(alpha = 0.5f),
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
                            activeTrackColor = Color(0xFF38BDF8),
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f),
                        ),
                    )
                }
            }
        }
    }
}

/**
 * Renders the Cover Screen (Outer Display):
 * 1. Displays the RIGHT HALF of the original wallpaper / screenshot.
 * 2. Inverted gradient blur: "越右越模糊" (clear near hinge on the left, blurred on the right).
 * 3. Angle-dependent sharpness: "折角越小整体越清晰" (blur reduces as angle goes from 90° down to 0°).
 */
@Composable
fun OuterScreenView(
    foldState: FoldState,
    customBitmap: ImageBitmap? = null,
    modifier: Modifier = Modifier,
) {
    val visualParams = remember(foldState.angle) {
        calculateFoldVisualParams(foldState)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D0E15)),
    ) {
        // Base sharp Right Half
        WallpaperHalfView(
            isLeftHalf = false,
            customBitmap = customBitmap,
            modifier = Modifier.fillMaxSize(),
        )

        // Inverted gradient blur overlay (more blur on right edge, clear near left hinge)
        if (Build.VERSION.SDK_INT >= 31 && visualParams.outerBlurPx > 0.3f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                    .graphicsLayer {
                        renderEffect = RenderEffect
                            .createBlurEffect(
                                visualParams.outerBlurPx,
                                visualParams.outerBlurPx,
                                Shader.TileMode.CLAMP,
                            )
                            .asComposeRenderEffect()
                    }
                    .drawWithContent {
                        drawContent()
                        // Alpha mask: 0.0 at left hinge (clear), 1.0 at right edge (max blur)
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, Color.Black),
                                startX = 0f,
                                endX = size.width,
                            ),
                            blendMode = BlendMode.DstIn,
                        )
                    },
            ) {
                WallpaperHalfView(
                    isLeftHalf = false,
                    customBitmap = customBitmap,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
