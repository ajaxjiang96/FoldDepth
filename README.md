# FoldDepth

An experimental Android foldable interaction prototype where the physical hinge angle directly drives asymmetrical depth-of-field.

<div align="center">
  <a href="https://youtu.be/vEq8UMa20Ag">
    <img src="https://img.youtube.com/vi/vEq8UMa20Ag/maxresdefault.jpg" alt="FoldDepth - iPhone Duo flip animation recreated on Android foldable" width="850">
  </a>
  <p>
    <a href="https://youtu.be/vEq8UMa20Ag"><b>▶ Watch Real-Device Demo on YouTube: <i>"I’ve recreated the iPhone Duo flip animation on android foldable"</i></b></a>
  </p>
</div>

---

## Philosophy & Core Interaction Model

$$\text{UI} = f(\text{hingeAngle})$$

FoldDepth rejects fixed-duration animations (e.g. canned 500ms timelines) in favor of **direct physical manipulation**. The visual state is a continuous, reversible, scrubbable transfer function of the device hinge angle:
- If the physical hinge stops midway, the visual transition halts at that exact fractional state.
- Unfolding to 180° smoothly reveals the full, flat, crystal-sharp interface.
- Folding closed continuously deepens spatial blur and applies perspective depth.

---

## Spatial Fold Interaction Rules (Inner Screen)

The unfolded display is split down the center crease:
- **Right Half (Stationary Anchor)**:
  - Remains **100% crisp and unblurred** at all times (`blur = 0px`, `scaleX = 1.0x`).
- **Left Half (Pivoting Depth Surface)**:
  - **Native AGSL Progressive Spatial Blur (Android 13+ / API 33+)**:
    - Powered by `android.graphics.RuntimeShader` and `RenderEffect.createRuntimeShaderEffect`.
    - **Center Seam**: Blur radius is strictly $0\text{px}$ (single-sample fast path, 100% bit-identical to the right half).
    - **Outer Left Edge**: Radius expands dynamically using a non-linear diffusion curve.
    - Sampled via a **16-Tap Vogel's Golden Angle Spiral** with per-pixel Interleaved Gradient Noise (IGN) jitter to eliminate banding.
  - **Apple Ambient Exposure Darkening**:
    - Rather than washed-out grey fog, heavily blurred pixels sink naturally into the pure black background void.
  - **Keyframed Elastic Horizontal Stretch**:
    - **$180^\circ$ (Flat)**: $1.0\times$ (normal 1:1 aspect ratio)
    - **$130^\circ$**: $1.5\times$
    - **$115^\circ$**: $2.0\times$
    - **$100^\circ$**: $3.0\times$
    - **$0^\circ$ (Closed)**: $3.6\times$
    - Smoothly interpolated via a Monotonic Cubic Hermite Spline (PCHIP) anchored at `TransformOrigin(1f, 0.5f)` so the center seam never moves.
  - **Subtle 3D Perspective Rotation**:
    - Left half rotates inward along the center hinge axis (`rotationYLeft = -(180° - angle) * 0.36f`).

> [!NOTE]
> **Outer Screen Status**:
> The current prototype focuses exclusively on the **inner screen** interaction and spatial depth calibration. The outer screen (cover display) rendering logic in the codebase is an uncalibrated placeholder/WIP stub and has not been tuned.

---

## High-Performance Hardware Polling

- **Sensor**: `Sensor.TYPE_HINGE_ANGLE` registered with `SENSOR_DELAY_FASTEST` and zero batching (`maxReportLatencyUs = 0`).
- **Dedicated Worker Thread**: Polled on an independent `HandlerThread` with `THREAD_PRIORITY_URGENT_DISPLAY`.
- **Live Real-time Hz Readout**: Real-time sampling frequency (Hz) monitored and displayed live in the diagnostic HUD.

---

## Direct Download APK

Ready-to-install signed APKs are available directly in [GitHub Releases](https://github.com/ajaxjiang96/FoldDepth/releases):

- [**Download FoldDepth-v0.1.0.apk**](https://github.com/ajaxjiang96/FoldDepth/releases/download/v0.1.0/FoldDepth-v0.1.0.apk)
  - Signed with debug keystore: directly installable on any Android 13+ device (`adb install` or tap to install).

---

## Running in Android Studio

1. **Open the project**:
   - Open Android Studio (Ladybug / Meerkat / Quail 2024–2026+).
   - Let Gradle sync complete using Gradle `9.5.0` with Android Gradle Plugin `9.3.2`.
2. **Select Run Configuration**:
   - Choose `app` and select your target device (OPPO Find N, Samsung Galaxy Z Fold, Pixel Fold, or standard emulator).
3. **Press Run (▶)**:
   - The app compiles and installs directly.

Or build from the terminal:
```bash
# Debug APK:
./gradlew assembleDebug

# Signed Release APK:
./gradlew assembleRelease
```

---

## Interactive Controls & Gestures

- **Tap Screen Anywhere**: Toggle all HUD elements on/off (defaults to clean 100% fullscreen wallpaper mode).
- **Auto-Load Wallpaper**: Automatically queries the latest photo from your device's photo gallery on startup (`MediaStoreHelper`).
- **0°–180° Debug Slider**: Available in HUD to scrub the entire folding transition smoothly without a physical foldable device.
