# Spirit Level

Android spirit level that works on every side of the phone.

- **Stood on any of the four edges**: a tube level along the edge that is down. The screen draws
  itself the right way up for that edge, so it reads correctly upright, upside down or on either side.
- **Lying on its back**: a bullseye level with X and Y tilt.
- Angle in degrees and slope in percent. Turns green within 0.2 degrees, amber within 1 degree.
- **HOLD** freezes the reading. **BUZZ** gives a short vibration on reaching level.
- **CALIBRATE** is a two-step reversal calibration: measure, turn the phone 180 degrees on the same
  spot, measure again. The surface's own slope cancels out, leaving the phone's error, such as the
  camera bump lifting one end when it lies on its back. Works on any surface, with or without a case.
  Each edge and lying flat are calibrated separately; long-press CALIBRATE to clear one.
- Foldables (built for the OnePlus Open): folded and open keep separate calibrations, sensor axes are
  matched to the inner screen's rotation, and a warning shows when the phone is half folded.
- Screen stays on while the app is open.

No libraries beyond the Android framework. Level maths is in `LevelMath.kt` and covered by unit tests.

Build: `./gradlew testDebugUnitTest assembleRelease`. The `Spirit Level APK` GitHub workflow builds and
publishes a release on every push that touches this folder.
