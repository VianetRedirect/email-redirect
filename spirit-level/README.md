# Spirit Level

Android spirit level that works on every side of the phone.

- **Stood on any of the four edges**: a tube level along the edge that is down. The screen draws
  itself the right way up for that edge, so it reads correctly upright, upside down or on either side.
- **Lying on its back**: a bullseye level with X and Y tilt.
- Angle in degrees and slope in percent. Turns green within 0.2 degrees, amber within 1 degree.
- **HOLD** freezes the reading. **ZERO** calibrates the side the phone is on (each edge and flat are
  zeroed separately); long-press ZERO to clear it. **BUZZ** gives a short vibration on reaching level.
- Screen stays on while the app is open.

No libraries beyond the Android framework. Level maths is in `LevelMath.kt` and covered by unit tests.

Build: `./gradlew testDebugUnitTest assembleRelease`. The `Spirit Level APK` GitHub workflow builds and
publishes a release on every push that touches this folder.
