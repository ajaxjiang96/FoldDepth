package com.ajaxjiang.folddepth.model

import kotlin.math.pow

/**
 * Represents the current physical or simulated fold state of the device.
 *
 * @property angle Hinge angle in degrees (0° = closed, 180° = fully flat/open).
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
 *    As angle decreases (180° -> 0°), blur increases monotonically.
 * 2. Inner Right Half: Stays 100% crisp at all times (blur = 0).
 * 3. Outer Screen (Cover Display): Activates when angle <= 90°.
 *    Displays right half of the original wallpaper with inverted gradient blur (clear near hinge,
 *    blurred on the right). As angle decreases (90° -> 0°), the outer screen clarifies until
 *    reaching 100% sharpness at 0° (fully closed).
 */
data class FoldVisualParams(
    val innerLeftMaxBlurPx: Float,
    val innerRightBlurPx: Float = 0f,
    val isOuterScreenActive: Boolean,
    val outerBlurPx: Float,
    val rotationYLeft: Float = 0f,
)

/**
 * Pure calculation function mapping [FoldState] to [FoldVisualParams].
 */
fun calculateFoldVisualParams(state: FoldState): FoldVisualParams {
    val angle = state.angle.coerceIn(0f, 180f)
    val progress = (angle / 180f).coerceIn(0f, 1f)

    // Inner left blur increases as angle drops: 180° -> 0px, 0° -> 48px
    val closedFraction = 1f - progress
    val easedClosed = smoothstep(closedFraction)
    val innerLeftMaxBlurPx = 48f * easedClosed.pow(1.2f)

    // Outer screen is active when hinge angle is 90° or less
    val isOuterScreenActive = angle <= 90f

    // Outer screen blur: At 90° it starts with peak blur (~40px),
    // and as angle drops from 90° -> 0°, it gets clearer and clearer (0px at 0°)
    val outerBlurPx = if (isOuterScreenActive) {
        val outerProgress = (angle / 90f).coerceIn(0f, 1f)
        40f * smoothstep(outerProgress)
    } else {
        0f
    }

    // Subtle 3D perspective fold on the left half (hinge at right edge of left half)
    val rotationYLeft = -((180f - angle) * 0.18f)

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
