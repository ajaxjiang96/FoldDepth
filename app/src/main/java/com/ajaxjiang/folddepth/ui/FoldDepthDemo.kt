package com.ajaxjiang.folddepth.ui

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
import com.ajaxjiang.folddepth.model.calculateAppleBlurArea
import com.ajaxjiang.folddepth.model.calculateAppleShade
import com.ajaxjiang.folddepth.model.calculateFoldVisualParams
import com.ajaxjiang.folddepth.model.smoothstep
import com.ajaxjiang.folddepth.util.MediaStoreHelper
/**
 * Calibrated AGSL Optical Perspective Shader (v0.2.0).
 *
 * Optical perspective projection formulas and shader parameters adapted from Envl (@SesamPicr):
 * - Project: SoloTilt (https://solotilt.com/ , formerly https://solo.gnimoay.com)
 * - Creator: Envl (@SesamPicr on X, https://x.com/SesamPicr)
 *
 * Exact formulation adapted for Android AGSL:
 * - blurAngle: pow(smoothstep(0.0, 1.570796327, tilt), 0.5) [anglePower: 0.5, fullAngle: 90°]
 * - blurSpread: pow(smoothstep(0.0, 0.70, fromHinge), 1.45)  [distancePower: 1.45, fullDistance: 0.70]
 * - defocus: blurAngle * blurSpread
 * - sigma: size.x * 0.038 * defocus
 * - eyeDistance: 2.4 * max(aspect, 1.0)
 * - depth: fromHinge * aspect * sine
 * - perspective: eyeDistance / (eyeDistance - depth)
 * - imageUv.x: hinge + (uv.x - hinge) * cosine * perspective
 * - imageUv.y: 0.5 + (uv.y - 0.5) * perspective
 * - glass shading: 1.0 - 0.28 * sine * pow(fromHinge, 1.6)
 * - specular reflection: exp(-pow((fromHinge - 0.70) / 0.30, 2.0)) * sine * 0.025
 * - blackFade: clamp((fromHinge - 0.26) / 0.74, 0.0, 1.0) * 0.70 * blurAngle
 */
private const val AGSL_PROGRESSIVE_BLUR = """
    uniform shader content;
    uniform float2 size;
    uniform float hingeAngle;
    uniform float useShaderPerspective;

    float ign(float2 p) {
        return fract(sin(dot(p, float2(12.9898, 78.233))) * 43758.5453);
    }

    vec4 main(float2 fragCoord) {
        float turn = clamp((180.0 - hingeAngle) / 90.0, 0.0, 1.0);
        if (turn <= 0.0001) {
            return content.eval(fragCoord);
        }

        vec2 uv = fragCoord / size;
        float hinge = 1.0;
        float fromHinge = clamp(1.0 - uv.x, 0.0, 1.0);

        // Physical fold tilt: 0 to PI/2 (90°)
        float tilt = turn * 1.570796327;
        float cosine = max(0.0, cos(tilt));
        float sine = sin(tilt);

        // Camera perspective parameters (Solo):
        float aspect = size.x / size.y;
        float eyeDistance = 2.4 * max(aspect, 1.0);
        float depth = fromHinge * aspect * sine;
        float perspective = eyeDistance / max(eyeDistance - depth, 0.001);

        vec2 imageUv;
        if (useShaderPerspective > 0.5) {
            imageUv.x = hinge + (uv.x - hinge) * cosine * perspective;
            imageUv.y = 0.5 + (uv.y - 0.5) * perspective;
        } else {
            imageUv = uv;
        }

        // Solo blur parameters:
        // Angle response: pow(..., 0.5)
        // Spread response: pow(..., 1.45) with fullDistance = 0.70
        float blurAngle = pow(smoothstep(0.0, 1.570796327, tilt), 0.5);
        float blurSpread = pow(smoothstep(0.0, 0.70, fromHinge), 1.45);
        float defocus = blurAngle * blurSpread;
        float sigma = size.x * 0.038 * defocus;

        // Vertical margin softness and mask (Solo trapezoid margins):
        float verticalMask = 1.0;
        if (useShaderPerspective > 0.5) {
            float pixelY = 1.0 / size.y;
            float marginSoftness = pixelY + 2.0 * sigma / size.y;
            verticalMask = 1.0 - smoothstep(0.5 - marginSoftness, 0.5 + marginSoftness, abs(imageUv.y - 0.5));
            if (verticalMask <= 0.0) {
                return vec4(0.0, 0.0, 0.0, 1.0);
            }
        }

        // Sample content with Vogel golden spiral:
        vec4 baseSample;
        vec2 centerSampleCoord = clamp(imageUv, 0.0, 1.0) * size;

        if (sigma < 0.5) {
            baseSample = content.eval(centerSampleCoord);
        } else {
            const int SAMPLES = 16;
            const float GOLDEN_ANGLE = 2.39996323;
            float jitter = ign(fragCoord) * 6.283185;

            vec4 sum = vec4(0.0);
            float totalWeight = 0.0;

            for (int i = 0; i < 16; i++) {
                float fi = float(i);
                float r = sqrt((fi + 0.5) / 16.0) * sigma;
                float theta = fi * GOLDEN_ANGLE + jitter;
                vec2 offset = vec2(cos(theta), sin(theta)) * r;

                float weight = 1.0 - (r / (sigma + 0.01)) * 0.5;
                vec2 samplePos = clamp((centerSampleCoord + offset) / size, 0.0, 1.0) * size;
                sum += content.eval(samplePos) * weight;
                totalWeight += weight;
            }
            baseSample = sum / totalWeight;
        }

        // Solo glass ambient shading:
        float glass = sine * pow(fromHinge, 1.6);
        baseSample.rgb *= 1.0 - 0.28 * glass;

        // Solo specular glass reflection sheen:
        float reflection = exp(-pow((fromHinge - 0.70) / 0.30, 2.0)) * sine;
        baseSample.rgb += vec3(0.82, 0.85, 0.86) * reflection * 0.025;

        // Solo black gradient fade (starts at 0.26 from hinge, max 0.70 opacity):
        float blackFade = clamp((fromHinge - 0.26) / 0.74, 0.0, 1.0);
        baseSample.rgb *= 1.0 - 0.70 * blurAngle * blackFade;

        vec3 dark = vec3(0.0, 0.0, 0.0);
        return vec4(mix(dark, baseSample.rgb, verticalMask), 1.0);
    }
"""

@Composable
fun FoldDepthDemo(
    foldState: FoldState,
    initialCustomBitmap: ImageBitmap?,
    onSimulateAngleChange: (Float) -> Unit,
    onToggleSimulation: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val visualParams = remember(foldState.angle) {
        calculateFoldVisualParams(foldState)
    }

    var customBitmap by remember(initialCustomBitmap) {
        mutableStateOf(initialCustomBitmap)
    }

    // Default: Hide all app UI elements; tap screen anywhere to toggle UI visibility
    var showUi by remember { mutableStateOf(false) }
    var showHardwareOverrideSlider by remember { mutableStateOf(false) }
    var showOuterPreviewExpanded by remember { mutableStateOf(false) }
    var useSoloPerspective by remember { mutableStateOf(true) }

    val agslShader = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            RuntimeShader(AGSL_PROGRESSIVE_BLUR)
        } else null
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val bytes = stream.readBytes()
                    val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
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
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                showUi = !showUi
            },
    ) {
        // ====================================================================
        // INNER SCREEN SURFACE: Seamless dual-pane layout (NO separation line)
        // Left Half: Physically matches 3D fold angle -(180° - angle),
        //            vacated screen space is pure black, and heavy gradient blur is applied.
        // Right Half: Stationary, unblurred & 100% crisp.
        // ====================================================================
        Row(
            modifier = Modifier.fillMaxSize(),
        ) {
            // --- LEFT HALF CONTAINER ---
            // Pure black background fills any screen space vacated when the panel rotates inward
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color.Black)
                    // Solo (solo.gnimoay.com) Calibrated AGSL Shader:
                    // Stationed on display glass (in screen space)
                    .graphicsLayer {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && agslShader != null && foldState.angle < 179.9f) {
                            agslShader.setFloatUniform("size", size.width, size.height)
                            agslShader.setFloatUniform("hingeAngle", foldState.angle)
                            agslShader.setFloatUniform("useShaderPerspective", if (useSoloPerspective) 1.0f else 0.0f)
                            renderEffect = RenderEffect
                                .createRuntimeShaderEffect(agslShader, "content")
                                .asComposeRenderEffect()
                        }
                    },
            ) {
                // Inner content panel:
                // When useSoloPerspective is true, the shader computes exact ray perspective & stretch!
                // When false, Compose 3D rotation & stretch are applied.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            if (!useSoloPerspective) {
                                rotationY = visualParams.rotationYLeft
                                scaleX = visualParams.scaleXLeft
                                cameraDistance = 14f * density
                                transformOrigin = TransformOrigin(1f, 0.5f) // pivot along center hinge
                            }
                        },
                ) {
                    WallpaperHalfView(
                        isLeftHalf = true,
                        customBitmap = customBitmap,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            // (No crease divider line: Left and Right touch seamlessly)

            // --- RIGHT HALF (Unchanged & 100% Crisp) ---
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
        // OVERLAY UI (Hidden by default, toggled on/off by tapping screen)
        // ====================================================================

        // --- TOP STATUS BAR & CONTROLS ---
        AnimatedVisibility(
            visible = showUi,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopStart),
        ) {
            Row(
                modifier = Modifier
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
                        text = if (customBitmap != null) "Latest Photo / Screenshot Active" else "Built-in Wallpaper Preset",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Reload Latest Gallery Photo
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier
                            .clickable {
                                val latest = MediaStoreHelper.loadLatestGalleryImage(context)
                                if (latest != null) {
                                    customBitmap = latest
                                } else {
                                    imagePickerLauncher.launch("image/*")
                                }
                            }
                            .padding(end = 8.dp),
                    ) {
                        Text(
                            text = "Reload Latest",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }

                    // Choose Screenshot File
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier
                            .clickable { imagePickerLauncher.launch("image/*") }
                            .padding(end = 8.dp),
                    ) {
                        Text(
                            text = "Pick Image",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }

                    // Sensor Status Badge
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
                                foldState.isHardwareAvailable && !foldState.isSimulated -> {
                                    if (foldState.sensorRateHz > 0) {
                                        "LIVE SENSOR · ${foldState.sensorRateHz} Hz"
                                    } else {
                                        "LIVE SENSOR · FASTEST"
                                    }
                                }
                                foldState.isHardwareAvailable && foldState.isSimulated -> "SIMULATED"
                                else -> "DEBUG SLIDER"
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
        }

        // --- DUAL-SCREEN HUD / COVER DISPLAY PREVIEW (When angle <= 90° and UI is visible) ---
        AnimatedVisibility(
            visible = showUi && visualParams.isOuterScreenActive,
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
                border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
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

        // --- BOTTOM TECHNICAL HUD & DEBUG SLIDER (Visible when showUi == true) ---
        AnimatedVisibility(
            visible = showUi,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(24.dp),
            ) {
                // Numeric readouts
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
                            text = "progress: %.2f (%d%%) · rotY: %.1f° · stretch: %.2fx".format(
                                foldState.progress,
                                (foldState.progress * 100f).toInt(),
                                visualParams.rotationYLeft,
                                visualParams.scaleXLeft,
                            ),
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                        Text(
                            text = "AGSL Progressive Blur · Ambient Shading: ON",
                            color = Color(0xFFF472B6),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                        Spacer(Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (useSoloPerspective) Color(0xFF2563EB).copy(alpha = 0.7f) else Color.White.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, if (useSoloPerspective) Color(0xFF60A5FA) else Color.White.copy(alpha = 0.2f)),
                            modifier = Modifier.clickable { useSoloPerspective = !useSoloPerspective },
                        ) {
                            Text(
                                text = if (useSoloPerspective) "Mode: Solo Ray-Traced (Active)" else "Mode: Compose 3D Matrix",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Left Blur: %.1fpx (gradient)".format(visualParams.innerLeftMaxBlurPx),
                            color = Color(0xFF67E8F9),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                        Text(
                            text = if (foldState.isHardwareAvailable && !foldState.isSimulated) {
                                "Sampling: %d Hz (0µs FASTEST)".format(foldState.sensorRateHz)
                            } else {
                                "Right Blur: 0.0px (crisp)"
                            },
                            color = if (foldState.isHardwareAvailable && !foldState.isSimulated) Color(0xFFA78BFA) else Color(0xFF4ADE80),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                        Text(
                            text = if (visualParams.isOuterScreenActive) {
                                "Outer: ON (blur %.1fpx)".format(visualParams.outerBlurPx)
                            } else {
                                "Outer: OFF (> 90°)"
                            },
                            color = if (visualParams.isOuterScreenActive) Color(0xFFFBBF24) else Color.White.copy(alpha = 0.4f),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

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
}

/**
 * Renders the Cover Screen (Outer Display):
 * 1. Displays the RIGHT HALF of the wallpaper / screenshot.
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
