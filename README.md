# FoldDepth

An Android foldable interaction experiment where physical hinge angle directly drives UI depth.

## Philosophy & Core Interaction Model

$$\text{UI} = f(\text{hingeAngle})$$

FoldDepth rejects fixed-duration animations (e.g. 500ms or 800ms canned timelines) in favor of **direct physical manipulation**. The visual state is a continuous, reversible, scrubbable transfer function of the device hinge angle:
- If the hinge stops midway, the visual transition halts at that exact fractional state.
- Unfolding to 180° smoothly reveals the flat, sharp, unblurred interface.
- Folding back down continuously restores depth, blur, and scale transformations.

---

## Technical Architecture

The signal pipeline is strictly decoupled into distinct functional layers:

```
[Hardware Sensor: Sensor.TYPE_HINGE_ANGLE]   or   [0°–180° Debug Simulation Slider]
                                       │
                                       ▼
                     HingeAngleSource (SensorEventListener)
                                       │
                                       ▼
                       FoldState (angle, progress, mode)
                        progress = (angle / 180f)
                                       │
                                       ▼
                    FoldVisualParams & Easing (smoothstep)
                      • blurPx = 34f * (1f - eased)^1.35
                      • scale = 0.86f + 0.14f * eased
                      • alpha = 0.18f + 0.82f * eased
                      • translationY = 72f * (1f - eased)
                                       │
                                       ▼
                       FoldDepthDemo (Jetpack Compose)
                      • RenderEffect (API 31+ hardware blur)
                      • GraphicsLayer transformation
                      • Live Technical HUD & Debug Controls
```

### Key Modules:
- **`com.ajaxjiang.folddepth.sensor.HingeAngleSource`**: Manages hardware `Sensor.TYPE_HINGE_ANGLE` registration/unregistration safely via Android lifecycle events. Supports seamless fallback and simulation overrides.
- **`com.ajaxjiang.folddepth.model.FoldState`**: Immutable state modeling hinge angle, normalized progress $[0.0, 1.0]$, sensor availability, and simulation flags.
- **`com.ajaxjiang.folddepth.model.FoldVisualParams`**: Pure calculation functions that translate fold progress into rendering parameters via cubic smoothstep easing ($S(x) = 3x^2 - 2x^3$).
- **`com.ajaxjiang.folddepth.ui.FoldDepthDemo`**: Fullscreen Compose surface rendering real-time hardware `RenderEffect` blur, scale, translation, and opacity, alongside a live diagnostics HUD and interactive simulation slider.

---

## Android Studio Setup & How to Run

### Prerequisites
- **Android Studio**: Android Studio Hedgehog / Iguana / Koala / Ladybug / Quail (2024–2026+)
- **JDK**: Java 21+ (bundled with modern Android Studio releases)
- **Android SDK**: Compile SDK 36, Min SDK 33

### Running the App
1. **Clone the repository**:
   ```bash
   git clone https://github.com/ajaxjiang96/FoldDepth.git
   ```
2. **Open in Android Studio**:
   - Open Android Studio, choose **Open**, and select the `FoldDepth` project root.
   - Let Gradle sync complete automatically via the included Gradle Wrapper (`gradle-9.5.0`).
3. **Select Run Configuration**:
   - In the top toolbar, ensure the **`app`** configuration is selected.
   - Choose a target device (physical foldable, regular phone, or any Android emulator).
4. **Press Run (▶)**:
   - The app compiles and installs immediately.

Alternatively, build from the command line:
```bash
./gradlew assembleDebug
```
The output APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

---

## Real-Device Hinge Sensor Requirements

- **Supported Hardware**: Foldable devices supporting Android's standard `Sensor.TYPE_HINGE_ANGLE` (value `36`), such as Pixel Fold / Pixel 9 Pro Fold, Samsung Galaxy Z Fold series (running Android 13+), and other foldables adhering to standard Android sensor HAL.
- **Angle Range**: $0^\circ$ (fully closed) to $180^\circ$ (fully open / flat).
- **Manifest Declaration**: Declares `<uses-feature android:name="android.hardware.sensor.hinge_angle" android:required="false" />` so it installs on any Android 13+ device or emulator without restriction.

---

## Developer Debug Simulation Slider

Normal emulators and non-foldable phones do not expose `Sensor.TYPE_HINGE_ANGLE`. FoldDepth includes a built-in fallback / debug mode:

- **Automatic Prominence on Standard Devices**: If no hardware hinge sensor is detected, the HUD displays `DEBUG SIMULATION (NO SENSOR)` and a `0° → 180°` slider appears at the bottom.
- **Scrubbable Real-Time Feedback**: Dragging the slider continuously drives blur, scale, translation, and opacity with zero latency.
- **Hardware Override on Real Foldables**: When a physical hinge sensor is detected, the live sensor drives the UI by default. A **"Simulate 0°–180°"** action allows developers to temporarily decouple from the physical hinge to test specific edge angles directly.

---

## Roadmap

- [x] Runnable, self-contained Android Studio project with modern AGP and Compose BOM
- [x] Live hardware hinge angle tracking via `Sensor.TYPE_HINGE_ANGLE`
- [x] 0°–180° interactive developer fallback slider
- [x] Angle-driven blur, scale, translation, and alpha
- [ ] Non-uniform spatial / gradient blur field
- [ ] AGSL / `RuntimeShader` integration for distance-based depth-of-field masking
- [ ] 3D perspective transformation and fold crease axis projection
- [ ] Hinge angular velocity tracking for inertia and kinetic depth response
