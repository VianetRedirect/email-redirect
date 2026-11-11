#!/usr/bin/env bash
# One-time setup of a headless build environment on Linux x86_64.
# Downloads Godot 4.4.1, its Android export templates and an APK signer,
# all from GitHub. Needs: curl, unzip, java (11 or newer).
#
#   tools/setup_build_env.sh            # installs into ~/.cache/pinball-build
#   BUILD_DIR=/some/dir tools/setup_build_env.sh
#
# Why not the normal Android SDK: cloud build containers used for this
# project cannot reach dl.google.com, so there is no apksigner. Godot
# exports an unsigned APK and uber-apk-signer signs it.
set -euo pipefail

BUILD_DIR="${BUILD_DIR:-$HOME/.cache/pinball-build}"
GODOT_VERSION="4.4.1-stable"
GODOT_BIN="Godot_v${GODOT_VERSION}_linux.x86_64"
BASE="https://github.com/godotengine/godot/releases/download/${GODOT_VERSION}"
TEMPLATES_DIR="$HOME/.local/share/godot/export_templates/4.4.1.stable"

mkdir -p "$BUILD_DIR/godot" "$BUILD_DIR/signer" "$BUILD_DIR/fake-android-sdk/platform-tools" \
	"$BUILD_DIR/fake-android-sdk/build-tools/35.0.0" "$TEMPLATES_DIR"

if [ ! -x "$BUILD_DIR/godot/$GODOT_BIN" ]; then
	echo "Downloading Godot editor (60 MB)"
	curl -fsSL -o "$BUILD_DIR/godot/godot.zip" "$BASE/${GODOT_BIN}.zip"
	unzip -o -q "$BUILD_DIR/godot/godot.zip" -d "$BUILD_DIR/godot"
	chmod +x "$BUILD_DIR/godot/$GODOT_BIN"
	rm "$BUILD_DIR/godot/godot.zip"
fi

if [ ! -f "$TEMPLATES_DIR/android_debug.apk" ]; then
	echo "Downloading export templates (1.2 GB, only the Android ones are kept)"
	curl -fsSL -o "$BUILD_DIR/templates.tpz" "$BASE/Godot_v${GODOT_VERSION}_export_templates.tpz"
	unzip -o -q -j "$BUILD_DIR/templates.tpz" templates/android_debug.apk templates/android_release.apk templates/version.txt -d "$TEMPLATES_DIR"
	rm "$BUILD_DIR/templates.tpz"
fi

if [ ! -f "$BUILD_DIR/signer/uber-apk-signer.jar" ]; then
	echo "Downloading uber-apk-signer"
	curl -fsSL -o "$BUILD_DIR/signer/uber-apk-signer.jar" \
		"https://github.com/patrickfav/uber-apk-signer/releases/download/v1.3.0/uber-apk-signer-1.3.0.jar"
fi

# Stub SDK. Godot only checks these files exist; signing happens outside it.
for f in platform-tools/adb build-tools/35.0.0/apksigner; do
	printf '#!/bin/sh\nexit 0\n' > "$BUILD_DIR/fake-android-sdk/$f"
	chmod +x "$BUILD_DIR/fake-android-sdk/$f"
done

echo "Build environment ready in $BUILD_DIR"
"$BUILD_DIR/godot/$GODOT_BIN" --version
