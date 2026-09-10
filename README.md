# FoldDepth

An Android foldable interaction experiment where the physical hinge directly drives UI depth.

The prototype explores:

- continuous hinge-angle input via `Sensor.TYPE_HINGE_ANGLE`
- angle-driven, reversible UI transitions rather than timeline animations
- blur, scale, translation, opacity and perspective as functions of the fold angle
- spatial / progressive blur to create perceived depth while unfolding

## Status

Private prototype. Intended to be open-sourced once the demo is convincing.

## Current prototype

The first pass wires the hinge sensor directly into Compose and drives a uniform `RenderEffect` blur plus scale/translation/opacity. This deliberately keeps the signal path simple so it can be tested on real foldable hardware immediately.

The next rendering step is a non-uniform blur field using Android graphics APIs / shader-driven masking so the blur gradient moves with the perceived depth plane.

## Interaction model

`UI = f(hingeAngle)`

No fixed-duration opening animation. If the physical hinge stops, the visual transition stops. Folding backward reverses it naturally.

## Requirements

- Android foldable exposing `TYPE_HINGE_ANGLE`
- Android 13+ for this experimental build
- Kotlin + Jetpack Compose

## License

No license yet while the repository is private. Add an open-source license immediately before making the repository public.
