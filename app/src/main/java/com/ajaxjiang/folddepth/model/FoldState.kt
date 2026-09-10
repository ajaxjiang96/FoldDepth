package com.ajaxjiang.folddepth.model

import kotlin.math.pow

/**
 * Represents the current physical or simulated fold state of the device.
 *
 * @property angle Hinge angle in degrees (0° = closed, 180° = flat/open).
 * @property progress Normalized fold progress between 0f (closed) and 1f (flat).
 * @property isHardwareAvailable True if Sensor.TYPE_HINGE_ANGLE is present on the hardware.
 * @property isSimulated True if developer debug simulation is currently overriding the sensor.
 * @property sensorRateHz Live sampling frequency (Hz) measured from the hinge angle sensor.
 */
data class FoldState(
    val angle: Float,
    val progress: Float = (angle / 180f).coerceIn(0f, 1f),
    val isHardwareAvailable: Boolean = false,
    val isSimulated: Boolean = false,
    val sensorRateHz: Int = 0,
)

/**
 * Derived visual effect parameters mapped directly from the physical fold state:
 *
 * 1. Inner Left Half:
 *    - Gradient blur from 0 at hinge to [innerLeftMaxBlurPx] at far left edge.
 *    - Softened 3D fold angle [rotationYLeft] along center crease.
 *    - Dynamic horizontal elastic stretch [scaleXLeft] (Apple Duo-style wipe & stretch).
 *    - Subtle ambient occlusion crease shadow [creaseShadowAlpha] near the hinge.
 * 2. Inner Right Half: Stays 100% crisp at all times (blur = 0).
 * 3. Outer Screen (Cover Display): Activates when angle <= 90°.
 *    Displays right half of the original wallpaper with inverted gradient blur.
 *    As angle decreases (90° -> 0°), outer blur decreases to 0px (100% sharp at 0°).
 */
data class FoldVisualParams(
    val innerLeftMaxBlurPx: Float,
    val innerRightBlurPx: Float = 0f,
    val isOuterScreenActive: Boolean,
    val outerBlurPx: Float,
    val rotationYLeft: Float,
    val scaleXLeft: Float = 1.0f,
    val creaseShadowAlpha: Float = 0f,
    val wipeAmount: Float = 0f,
)

/**
 * Pure calculation function mapping [FoldState] to [FoldVisualParams].
 */
fun calculateFoldVisualParams(state: FoldState): FoldVisualParams {
    val angle = state.angle.coerceIn(0f, 180f)
    val progress = (angle / 180f).coerceIn(0f, 1f)

    // Inner left blur: greatly enhanced intensity (up to 140px at 0°)
    val closedFraction = 1f - progress
    val easedClosed = smoothstep(closedFraction)
    val innerLeftMaxBlurPx = 140f * easedClosed.pow(1.15f)

    // Outer screen is active when hinge angle is 90° or less
    val isOuterScreenActive = angle <= 90f

    // Outer screen blur: starts at 100px at 90°, reducing to 0px at 0° (clarification)
    val outerBlurPx = if (isOuterScreenActive) {
        val outerProgress = (angle / 90f).coerceIn(0f, 1f)
        100f * smoothstep(outerProgress)
    } else {
        0f
    }

    // 3D folding angle: softened to half of physical fold: -((180° - angle) * 0.5f)
    val rotationYLeft = -((180f - angle) * 0.5f)

    // Dynamic horizontal elastic stretch (Apple Duo-style wipe & stretch):
    // As the panel rotates inward, stretching outward from the hinge counteracts
    // perspective foreshortening compression and creates an organic, elastic feel.
    val stretchIntensity = 0.22f // Up to 22% horizontal stretch at 0°
    val scaleXLeft = 1.0f + stretchIntensity * easedClosed

    // Subtle crease ambient occlusion shadow near the hinge as the angle deepens
    val creaseShadowAlpha = 0.35f * easedClosed

    return FoldVisualParams(
        innerLeftMaxBlurPx = innerLeftMaxBlurPx,
        innerRightBlurPx = 0f,
        isOuterScreenActive = isOuterScreenActive,
        outerBlurPx = outerBlurPx,
        rotationYLeft = rotationYLeft,
        scaleXLeft = scaleXLeft,
        creaseShadowAlpha = creaseShadowAlpha,
        wipeAmount = closedFraction,
    )
}

/**
 * Standard cubic smoothstep easing function: S(x) = 3x^2 - 2x^3.
 */
fun smoothstep(x: Float): Float {
    val t = x.coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

/**
 * Decreasing cubic smoothstep: 1.0 when x <= edge1, smoothly decreasing to 0.0 when x >= edge0.
 * In GLSL: clamp((x - edge0) / (edge1 - edge0), 0.0, 1.0) with cubic hermite interpolation.
 */
fun smoothstepDecreasing(edge0: Float, edge1: Float, x: Float): Float {
    if (x <= edge1) return 1.0f
    if (x >= edge0) return 0.0f
    val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

/**
 * Apple iPhone Duo non-linear blur distribution formula (from WipeFragment):
 * remap(-0.25, 1.0, distance) * wipeAmount * 2.5
 *
 * Guarantees zero blur and absolute clarity near the center hinge (distance = 0),
 * while creating an explosive, dynamic burst of heavy blur towards the outer left edge.
 *
 * @param u Normalized horizontal coordinate across left panel: 0.0 (left edge) to 1.0 (hinge).
 * @param wipeAmount Fold closure amount: 0.0 (180° flat) to 1.0 (0° closed).
 * @return Blur area factor, ranging from 0.0 at hinge up to ~1.333f at outer edge.
 */
fun calculateAppleBlurArea(u: Float, wipeAmount: Float): Float {
    val distance = (1.0f - u).coerceIn(0f, 1f) // 0 at hinge, 1 at left edge
    // remap(-0.25, 1.0, distance) = (distance + 0.25) / 1.25
    // Ensure deadband near crease:
    val normDist = ((distance - 0.06f) / 0.94f).coerceIn(0f, 1f)
    val scaled = (normDist * wipeAmount * 2.5f).coerceIn(0f, 1f)
    return scaled / 0.75f // reaches up to 1.3333f
}

/**
 * Apple iPhone Duo ambient exposure shading & darkening formula (from WipeFragment):
 * smoothstep(1.3, 0.9, blurArea)
 *
 * Instead of displaying washed-out grey/white fog, pixels with maximum blur
 * are deeply exposure-darkened into the background pure black void.
 */
fun calculateAppleShade(blurArea: Float): Float {
    return smoothstepDecreasing(1.3f, 0.9f, blurArea)
}
