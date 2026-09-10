package com.ajaxjiang.folddepth.model

import kotlin.math.pow

/**
 * Represents the current fold state of the device or active simulation.
 *
 * @property angle Hinge angle in degrees (0° = completely folded/closed, 180° = flat/open).
 * @property progress Normalized progress between 0f (closed) and 1f (open).
 * @property isHardwareAvailable Whether the physical device exposes Sensor.TYPE_HINGE_ANGLE.
 * @property isSimulated Whether the angle is currently driven by developer debug simulation.
 */
data class FoldState(
    val angle: Float,
    val progress: Float = (angle / 180f).coerceIn(0f, 1f),
    val isHardwareAvailable: Boolean = false,
    val isSimulated: Boolean = false,
)

/**
 * Derived visual effect parameters mapped directly from normalized fold progress: UI = f(hingeAngle).
 *
 * @property blurPx Blur radius in pixels applied via RenderEffect.
 * @property scale Layer scale factor (e.g., 0.86f to 1.0f).
 * @property alpha Layer opacity (e.g., 0.18f to 1.0f).
 * @property translationY Layer vertical translation offset in pixels.
 */
data class FoldVisualParams(
    val blurPx: Float,
    val scale: Float,
    val alpha: Float,
    val translationY: Float,
)

/**
 * Pure function mapping normalized fold progress [0f, 1f] to visual rendering parameters.
 * Keeping this decoupled allows future expansion into spatial blur fields, AGSL RuntimeShaders,
 * perspective transforms, and velocity tracking.
 */
fun calculateFoldVisualParams(progress: Float): FoldVisualParams {
    val clamped = progress.coerceIn(0f, 1f)
    val eased = smoothstep(clamped)

    val blurPx = 34f * (1f - eased).pow(1.35f)
    val scale = 0.86f + 0.14f * eased
    val alpha = 0.18f + 0.82f * eased
    val translationY = 72f * (1f - eased)

    return FoldVisualParams(
        blurPx = blurPx,
        scale = scale,
        alpha = alpha,
        translationY = translationY,
    )
}

/**
 * Standard cubic smoothstep easing function: S(x) = 3x^2 - 2x^3.
 */
fun smoothstep(x: Float): Float {
    val t = x.coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}
