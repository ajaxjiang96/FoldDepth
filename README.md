# FoldDepth

An experimental Android foldable interaction prototype where the physical hinge angle directly drives asymmetrical depth-of-field.

<div align="center">
  <a href="https://youtu.be/Hr8peOP33ko">
    <img src="https://img.youtube.com/vi/Hr8peOP33ko/maxresdefault.jpg" alt="FoldDepth - iPhone Duo flip animation recreated on Android foldable" width="850">
  </a>
  <p>
    <a href="https://youtu.be/Hr8peOP33ko"><b>▶ Watch Real-Device Demo on YouTube: <i>"I’ve recreated the iPhone Duo flip animation on android foldable"</i></b></a>
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
  - **Ray-Traced Optical Perspective (v0.2.0 - Native AGSL RuntimeShader)**:
    - **Camera Ray Projection**: Computes analytic camera ray intersection per pixel ($uv_{\text{proj}}.x = 1.0 + (uv.x - 1.0) \cdot \cos\theta \cdot \text{perspective}$), producing physics-based horizontal elastic stretching and 3D trapezoid vertical perspective compression without mesh distortions.
    - **Defocus Diffusion**: Powered by a square-root angular response ($\text{tilt}^{0.5}$) and power-$1.45$ distance spread reaching full diffusion at $70\%$ screen width.
    - **Center Seam Invariant**: Blur radius and darkening are strictly $0$ at the center crease ($26\%$ brightness deadband), ensuring the left panel joins the right panel seamlessly without seam boundaries or color steps.
    - **Glass Shading & Specular Sheen**: Simulates curved glass depth with ambient falloff and a subtle Gaussian specular reflection band at $70\%$ distance.
    - Sampled via a **16-Tap Vogel's Golden Angle Spiral** with per-pixel Interleaved Gradient Noise (IGN) jitter.
  - **Live Mode Toggle in Diagnostic HUD**:
    - Tap anywhere on screen to toggle the diagnostic HUD, allowing instant side-by-side comparison between **`Mode: Solo Ray-Traced (Active)`** and **`Mode: Compose 3D Matrix`**.

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

- [**Download FoldDepth-v0.2.0.apk** (Latest)](https://github.com/ajaxjiang96/FoldDepth/releases/download/v0.2.0/FoldDepth-v0.2.0.apk)
  - **v0.2.0**: Integrated ray-projected optical perspective shader, square-root angular blur, glass ambient shading & specular reflection.
- [**Download FoldDepth-v0.1.0.apk**](https://github.com/ajaxjiang96/FoldDepth/releases/download/v0.1.0/FoldDepth-v0.1.0.apk)
  - Initial working release with hardware hinge polling and monotonic spline stretch.

---

## Credits & Acknowledgments

Special thanks to **Envl** ([@SesamPicr](https://x.com/SesamPicr)) for the brilliant optical perspective formulas and shader calibration demonstrated in [SoloTilt](https://solotilt.com/) (formerly `solo.gnimoay.com`). 

Starting in **v0.2.0**, FoldDepth adapts Envl's ray-projected camera perspective equations, angle/distance diffusion curves, and glass shading models into native Android AGSL (`RuntimeShader`), driving genuine hardware-sensor-driven depth rendering on physical foldable devices.

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
