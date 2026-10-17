#!/usr/bin/env bash
# Builds a signed, installable APK *without* the Android SDK or Gradle.
#
# Needs on PATH (or via env vars):
#   kotlinc            https://github.com/JetBrains/kotlin/releases  (KOTLINC)
#   javac + keytool    any JDK 17+
#   aapt, dx, zipalign, apksigner   Ubuntu/Debian: apt install aapt dalvik-exchange zipalign apksigner
#   zip, curl
# ANDROID_JAR may point at platforms/android-34/android.jar; if unset a copy is downloaded.
#
# Output: build-local/FireRemote.apk
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC="$ROOT/app/src/main"
OUT="${OUT:-$ROOT/build-local}"
KOTLINC="${KOTLINC:-kotlinc}"
AAPT="${AAPT:-aapt}"
DX="${DX:-dx}"
ZIPALIGN="${ZIPALIGN:-zipalign}"
APKSIGNER="${APKSIGNER:-apksigner}"
KEYSTORE="${KEYSTORE:-$OUT/local-signing.jks}"
KEYSTORE_PASS="${KEYSTORE_PASS:-fireremote}"
KEY_ALIAS="${KEY_ALIAS:-fireremote}"
# dx can only translate the Kotlin stdlib's invokedynamic sites for Android 8+, so this script
# produces an Android 8.0+ APK. The Gradle/CI build (R8) goes down to Android 7.0.
MIN_SDK=26
TARGET_SDK=34
PACKAGE=com.vianetredirect.firetvremote
VERSION_CODE="${VERSION_CODE:-2}"
VERSION_NAME="${VERSION_NAME:-1.1}"

mkdir -p "$OUT"
ANDROID_JAR="${ANDROID_JAR:-$OUT/android-34.jar}"
if [ ! -f "$ANDROID_JAR" ]; then
  echo ">> downloading android.jar (API 34 stubs)"
  curl -sSL -o "$ANDROID_JAR" https://raw.githubusercontent.com/Sable/android-platforms/master/android-34/android.jar
fi

KOTLIN_HOME="$(cd "$(dirname "$(readlink -f "$(command -v "$KOTLINC")")")/.." && pwd)"
STDLIB="$KOTLIN_HOME/lib/kotlin-stdlib.jar"
[ -f "$STDLIB" ] || { echo "kotlin-stdlib.jar not found next to kotlinc ($STDLIB)"; exit 1; }

rm -rf "$OUT/gen" "$OUT/classes" "$OUT/dex"
mkdir -p "$OUT/gen" "$OUT/classes" "$OUT/dex"

# Gradle supplies the package name and SDK levels; aapt needs them in the manifest itself.
MANIFEST="$OUT/AndroidManifest.xml"
sed "s|<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">|<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\" package=\"$PACKAGE\">|" \
  "$SRC/AndroidManifest.xml" > "$MANIFEST"
grep -q "package=\"$PACKAGE\"" "$MANIFEST" || { echo "could not inject package name into manifest"; exit 1; }
AAPT_FLAGS=(--min-sdk-version "$MIN_SDK" --target-sdk-version "$TARGET_SDK" --version-code "$VERSION_CODE" --version-name "$VERSION_NAME")

echo ">> generating R.java"
"$AAPT" package -f -m -J "$OUT/gen" -M "$MANIFEST" -S "$SRC/res" -I "$ANDROID_JAR" "${AAPT_FLAGS[@]}"

echo ">> compiling Kotlin"
# Lambdas and SAM conversions are compiled to classes because dx cannot read invokedynamic.
"$KOTLINC" -no-jdk -no-stdlib -no-reflect \
  -jvm-target 1.8 -Xlambdas=class -Xsam-conversions=class \
  -classpath "$ANDROID_JAR:$STDLIB" \
  -d "$OUT/classes" \
  "$SRC/java" "$OUT/gen"

echo ">> compiling R.java"
javac --release 8 -nowarn -cp "$ANDROID_JAR" -d "$OUT/classes" $(find "$OUT/gen" -name '*.java')

echo ">> preparing kotlin-stdlib for dx"
cp "$STDLIB" "$OUT/dex/stdlib.jar"
zip -q -d "$OUT/dex/stdlib.jar" 'META-INF/versions/*' 'META-INF/*.kotlin_module' >/dev/null 2>&1 || true

echo ">> dexing"
"$DX" --dex --min-sdk-version=$MIN_SDK --output="$OUT/dex/classes.dex" "$OUT/classes" "$OUT/dex/stdlib.jar"

echo ">> packaging resources"
rm -f "$OUT/unsigned.apk" "$OUT/aligned.apk"
"$AAPT" package -f -M "$MANIFEST" -S "$SRC/res" -I "$ANDROID_JAR" "${AAPT_FLAGS[@]}" -F "$OUT/unsigned.apk"
(cd "$OUT/dex" && "$AAPT" add "$OUT/unsigned.apk" classes.dex >/dev/null)

echo ">> aligning"
"$ZIPALIGN" -f 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"

if [ ! -f "$KEYSTORE" ]; then
  echo ">> creating signing key $KEYSTORE"
  keytool -genkeypair -keystore "$KEYSTORE" -storepass "$KEYSTORE_PASS" -keypass "$KEYSTORE_PASS" \
    -alias "$KEY_ALIAS" -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=Fire Remote, O=Personal, C=GB" >/dev/null 2>&1
fi

echo ">> signing"
"$APKSIGNER" sign --ks "$KEYSTORE" --ks-pass "pass:$KEYSTORE_PASS" --key-pass "pass:$KEYSTORE_PASS" \
  --ks-key-alias "$KEY_ALIAS" --out "$OUT/FireRemote.apk" "$OUT/aligned.apk"
"$APKSIGNER" verify "$OUT/FireRemote.apk"
rm -f "$OUT/unsigned.apk" "$OUT/aligned.apk"

echo ">> done: $OUT/FireRemote.apk ($(du -h "$OUT/FireRemote.apk" | cut -f1))"
