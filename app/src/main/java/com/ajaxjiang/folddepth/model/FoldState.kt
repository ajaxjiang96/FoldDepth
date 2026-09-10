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
 * 3. Outer Screen (Cover Display): Uncalibrated placeholder stub (WIP / not calibrated).
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

    // 3D folding angle: doubled perspective factor (0.36f)
    val rotationYLeft = -((180f - angle) * 0.36f)

    // Dynamic horizontal elastic stretch calibrated to keyframes:
    // 180° -> 1.0x, 130° -> 1.5x, 115° -> 2.0x, 100° -> 3.0x
    val scaleXLeft = calculateStretchScale(angle)

    // Seam continuity: zero crease shadow at the hinge seam so there is no color difference
    val creaseShadowAlpha = 0f

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
 * Keyframed horizontal stretch ratio (scaleXLeft) for the folding left half:
 *
 * User Keyframes:
 * - 180° (flat):  1.0x (normal width)
 * - 130°:         1.5x
 * - 115°:         2.0x
 * - 100°:         3.0x
 * - Below 100°:   Smoothly continues to 3.6x at 0° (closed)
 *
 * Implemented using Monotonic Cubic Hermite Spline (PCHIP):
 * - Guarantees exact hit at all keyframes
 * - Continuous C1 derivative (no velocity discontinuity)
 * - Zero heap allocation on UI thread
 */
fun calculateStretchScale(angle: Float): Float {
    val clampedAngle = angle.coerceIn(0f, 180f)
    val u = 180f - clampedAngle // Fold inward degrees: 0° (flat) to 180° (closed)

    val u0: Float
    val u1: Float
    val y0: Float
    val y1: Float
    val d0: Float
    val d1: Float

    if (u <= 50f) {
        // [0°, 50°] -> [180°, 130°]: 1.0x -> 1.5x
        u0 = 0f; u1 = 50f; y0 = 1.0f; y1 = 1.5f; d0 = 0.0f; d1 = 0.015384615f
    } else if (u <= 65f) {
        // [50°, 65°] -> [130°, 115°]: 1.5x -> 2.0x
        u0 = 50f; u1 = 65f; y0 = 1.5f; y1 = 2.0f; d0 = 0.015384615f; d1 = 0.044444446f
    } else if (u <= 80f) {
        // [65°, 80°] -> [115°, 100°]: 2.0x -> 3.0x
        u0 = 65f; u1 = 80f; y0 = 2.0f; y1 = 3.0f; d0 = 0.044444446f; d1 = 0.011009174f
    } else {
        // [80°, 180°] -> [100°, 0°]: 3.0x -> 3.6x
        u0 = 80f; u1 = 180f; y0 = 3.0f; y1 = 3.6f; d0 = 0.011009174f; d1 = 0.0f
    }

    val h = u1 - u0
    val t = ((u - u0) / h).coerceIn(0f, 1f)
    val t2 = t * t
    val t3 = t2 * t

    val h00 = 2f * t3 - 3f * t2 + 1f
    val h10 = t3 - 2f * t2 + t
    val h01 = -2f * t3 + 3f * t2
    val h11 = t3 - t2

    return h00 * y0 + h10 * h * d0 + h01 * y1 + h11 * h * d1
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
