# FoldDepth

An experimental Android foldable interaction prototype where the physical hinge angle directly drives asymmetrical depth-of-field and dual-display continuity.

---

## Philosophy & Core Interaction Model

$$\text{UI} = f(\text{hingeAngle})$$

FoldDepth rejects fixed-duration animations (e.g. canned 500ms timelines) in favor of **direct physical manipulation**. The visual state is a continuous, reversible, scrubbable transfer function of the device hinge angle:
- If the physical hinge stops midway, the visual transition halts at that exact fractional state.
- Unfolding to 180° smoothly reveals the full, flat, crystal-sharp interface.
- Folding closed continuously deepens spatial blur and triggers cover-screen continuity.

---

## Spatial Fold Interaction Rules

### 1. Inner Screen (Dual-Pane Split)
The unfolded display is split down the center crease:
- **Right Half (Stationary Anchor)**:
  - Remains **100% crisp and unblurred** at all times (`blur = 0px`).
- **Left Half (Pivoting Depth Surface)**:
  - Applies a hardware-accelerated **horizontal gradient blur** ("从右往左，越左边越模糊"):
    - **Center Crease / Right Edge**: Blur is $0\text{px}$ (seamlessly matches the crisp right half).
    - **Far Left Edge**: Reaches maximum blur intensity.
  - As the hinge angle decreases from $180^\circ \to 0^\circ$:
    - The maximum blur on the left edge increases monotonically ($0\text{px} \to 48\text{px}$).
    - The left surface subtly tilts with 3D perspective along the hinge axis.

### 2. Outer Screen (Cover Display at $\le 90^\circ$)
- When the hinge angle closes to **$90^\circ$ or less**, the outer screen activates.
- **Content**: Displays the **Right Half** of the original wallpaper / screenshot.
- **Inverted Gradient Blur ("越右越模糊")**:
  - **Left Edge (near hinge)**: Crisp and clear ($0\text{px}$ blur).
  - **Right Edge**: Reaches maximum blur.
- **Clarification Dynamics ("折角越小整体越清晰")**:
  - At $90^\circ$: The outer screen starts with its peak blur ($\sim 40\text{px}$).
  - As the angle drops from $90^\circ \to 0^\circ$: The blur smoothly decreases to $0\text{px}$.
  - At $0^\circ$ (fully closed): The outer screen is 100% flat and crystal clear.

---

## Technical Architecture

```
[Physical Hinge: Sensor.TYPE_HINGE_ANGLE]   or   [0°–180° Debug Simulation Slider]
                                       │
                                       ▼
                   HingeAngleSource (SensorEventListener)
                                       │
                                       ▼
                       FoldState (angle, progress, mode)
                                       │
                                       ▼
                         calculateFoldVisualParams
                      • innerLeftMaxBlurPx = 48 * (1 - p)^1.2
                      • innerRightBlurPx = 0 (crisp)
                      • isOuterScreenActive = (angle <= 90°)
                      • outerBlurPx = 40 * (angle / 90°)
                                       │
                                       ▼
    ┌──────────────────────────────────┴──────────────────────────────────┐
    ▼                                                                     ▼
[Inner Screen View]                                            [Outer Screen View]
• Left 50%: Gradient Blur (DstIn Alpha Mask)                    • Physical Presentation API
• Right 50%: 100% Crisp                                         • In-App Cover Display HUD
• Built-in Wallpaper & Custom Screenshot                        • Inverted Gradient Blur
```

### Key Modules:
- **`com.ajaxjiang.folddepth.model.FoldState`**: Immutable models and pure transfer functions calculating asymmetric inner and outer visual parameters.
- **`com.ajaxjiang.folddepth.ui.FoldableWallpaperSurface`**: Renders high-resolution foldable desktop scenes (widgets, clock, search bar, dock) and supports importing custom screenshots.
- **`com.ajaxjiang.folddepth.ui.FoldDepthDemo`**: Split-screen Compose interface applying GPU offscreen compositing (`RenderEffect` + `BlendMode.DstIn` linear gradient mask) for artifact-free horizontal gradient blur.
- **`com.ajaxjiang.folddepth.display.OuterDisplayManager`**: Manages the physical secondary cover display via Android's `Presentation` API.

---

## Native Foldable Support (OPPO ColorOS, Samsung, Xiaomi)

FoldDepth is configured natively for foldables:
- Declares `android:resizeableActivity="true"` and `configChanges` to prevent ColorOS / OneUI candy-bar letterboxing.
- Supports display cutout mode `shortEdges` for full-bleed edge-to-edge rendering.

---

## Running in Android Studio

1. **Open the project**:
   - Open Android Studio (Ladybug / Quail 2024–2026+).
   - Let Gradle sync complete using the included Gradle Wrapper (`gradle-9.5.0`).
2. **Select Run Configuration**:
   - Choose `app` and select your target device (OPPO foldable, Pixel Fold, or standard emulator).
3. **Press Run (▶)**:
   - The app compiles and installs directly.

Or build from the terminal:
```bash
./gradlew assembleDebug
```

---

## Developer Simulation & Screenshot Testing

- **0°–180° Debug Slider**: Available at the bottom of the screen to scrub the entire folding transition smoothly without needing a physical foldable.
- **Import Screenshot**: Tap **"Import Screenshot"** at the top right to pick any screenshot from your device (e.g. your phone's real home screen) to test spatial folding directly on your own wallpaper.
- **Cover Screen Preview HUD**: When the angle is $\le 90^\circ$, a live Cover Screen preview card appears in the upper right. Tap it to expand and inspect the outer screen's inverted gradient blur.
