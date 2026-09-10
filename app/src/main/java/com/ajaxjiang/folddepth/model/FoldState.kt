package com.ajaxjiang.folddepth.model

import kotlin.math.pow

/**
 * Represents the current physical or simulated fold state of the device.
 *
 * @property angle Hinge angle in degrees (0° = closed, 180° = flat/open).
 * @property progress Normalized fold progress between 0f (closed) and 1f (flat).
 * @property isHardwareAvailable True if Sensor.TYPE_HINGE_ANGLE is present on the hardware.
 * @property isSimulated True if developer debug simulation is currently overriding the sensor.
 */
data class FoldState(
    val angle: Float,
    val progress: Float = (angle / 180f).coerceIn(0f, 1f),
    val isHardwareAvailable: Boolean = false,
    val isSimulated: Boolean = false,
)

/**
 * Derived visual effect parameters mapped directly from the physical fold state:
 *
 * 1. Inner Left Half: Gradient blur from 0 at hinge to [innerLeftMaxBlurPx] at far left edge.
 *    As angle decreases (180° -> 0°), blur increases with heavy dramatic intensity.
 *    Perspective angle [rotationYLeft] exactly matches the physical hinge rotation: -(180° - angle).
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

    return FoldVisualParams(
        innerLeftMaxBlurPx = innerLeftMaxBlurPx,
        innerRightBlurPx = 0f,
        isOuterScreenActive = isOuterScreenActive,
        outerBlurPx = outerBlurPx,
        rotationYLeft = rotationYLeft,
    )
}

/**
 * Standard cubic smoothstep easing function: S(x) = 3x^2 - 2x^3.
 */
fun smoothstep(x: Float): Float {
    val t = x.coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}
