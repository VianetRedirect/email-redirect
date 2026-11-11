#!/usr/bin/env bash
# Runs the tests, then builds a signed debug APK.
#   tools/build_apk.sh            # tests + build
#   SKIP_TESTS=1 tools/build_apk.sh
# Output: export/pinball-roguelike-debug.apk (arm64 only).
# Run tools/setup_build_env.sh once first.
set -euo pipefail

BUILD_DIR="${BUILD_DIR:-$HOME/.cache/pinball-build}"
PROJECT="$(cd "$(dirname "$0")/.." && pwd)"
GODOT="$BUILD_DIR/godot/Godot_v4.4.1-stable_linux.x86_64"
SIGNER="$BUILD_DIR/signer/uber-apk-signer.jar"
OUT="$PROJECT/export"

[ -x "$GODOT" ] || { echo "Godot not found. Run tools/setup_build_env.sh first."; exit 1; }

echo "== import (builds the class cache, required before any -s script)"
"$GODOT" --headless --path "$PROJECT" --import > /dev/null 2>&1 || true

if [ -z "${SKIP_TESTS:-}" ]; then
	echo "== flipper power test"
	"$GODOT" --headless --fixed-fps 60 --path "$PROJECT" -s res://tests/flipper_power_test.gd 2>&1 | grep -E "RESULT|SCRIPT ERROR" | tee /dev/stderr | grep -q "PASS"
	echo "== drone top test"
	"$GODOT" --headless --fixed-fps 60 --path "$PROJECT" -s res://tests/drone_top_test.gd 2>&1 | grep -E "RESULT|SCRIPT ERROR" | tee /dev/stderr | grep -q "PASS"
	echo "== items test"
	"$GODOT" --headless --fixed-fps 60 --path "$PROJECT" -s res://tests/items_test.gd 2>&1 | grep -E "CHECK FAIL|RESULT|SCRIPT ERROR" | tee /dev/stderr | grep -q "RESULT.*PASS"
	echo "== table elements test"
	"$GODOT" --headless --fixed-fps 60 --path "$PROJECT" -s res://tests/elements_test.gd 2>&1 | grep -E "CHECK FAIL|RESULT|SCRIPT ERROR" | tee /dev/stderr | grep -q "RESULT.*PASS"
	echo "== shot map test"
	"$GODOT" --headless --fixed-fps 60 --path "$PROJECT" -s res://tests/shot_map_test.gd 2>&1 | grep -E "CHECK FAIL|RESULT|SCRIPT ERROR" | tee /dev/stderr | grep -q "RESULT.*PASS"
	echo "== sump bot smoke test (3 sectors, up to 15 simulated minutes)"
	"$GODOT" --headless --fixed-fps 60 --path "$PROJECT" -s res://tests/sump_bot_test.gd 2>&1 | grep -E "RESULT|SCRIPT ERROR" || true
fi

echo "== export unsigned APK"
mkdir -p "$OUT"
rm -f "$OUT"/*.apk "$OUT"/*.idsig
"$GODOT" --headless --editor -s res://tools/export_android.gd --path "$PROJECT" 2>&1 | grep -E "export_project|export message" || true
[ -f "$OUT/pinball-roguelike-unsigned.apk" ] || { echo "Export failed"; exit 1; }

echo "== sign"
java -jar "$SIGNER" --apks "$OUT/pinball-roguelike-unsigned.apk" \
	--ks "$PROJECT/tools/debug.keystore" --ksAlias androiddebugkey --ksPass android --ksKeyPass android \
	--out "$OUT" 2>&1 | grep -E "verified|success|error" || true
mv "$OUT/pinball-roguelike-aligned-signed.apk" "$OUT/pinball-roguelike-debug.apk"
rm -f "$OUT/pinball-roguelike-unsigned.apk" "$OUT"/*.idsig
ls -la "$OUT/pinball-roguelike-debug.apk"
