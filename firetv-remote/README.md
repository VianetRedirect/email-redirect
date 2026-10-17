# Fire Remote

A small, fast Android remote for Amazon Fire TV sticks and boxes. No adverts, no accounts, no
libraries: it talks straight to the Fire TV over your home Wi‑Fi using the same local service the
official Amazon app uses, so it works even though the physical remote is gone.

- Big navigation ring with press-and-hold scrolling, plus Back, Home and Menu
- Play/Pause, Rewind and Fast forward
- Keyboard: type a search term on the phone and send it to the TV
- Apps: lists what is installed on the Fire TV and opens any app with one tap
- Finds the Fire TV automatically, or takes an IP address
- Remembers more than one Fire TV
- Haptic tick on every key, opens instantly (the APK is under 1 MB)

## Install it on your phone

1. Open the latest **Fire Remote build** under this repository's *Releases* on your phone and
   download `FireRemote.apk`.
2. Open the downloaded file. Android will ask you to allow installs from your browser the first
   time. Allow it, then tap **Install**.
3. Open *Fire Remote*.

## First run: pairing

1. Make sure the phone is on the same Wi‑Fi as the Fire TV and the Fire TV is switched on at the
   wall (it does not need to be awake).
2. The app searches the network and lists any Fire TV it finds. Tap yours. If nothing shows up,
   type the Fire TV's IP address and tap **Connect**. The address is under
   *Settings › My Fire TV › About › Network* on the TV, or in your router's device list.
3. A four-digit PIN appears on the TV screen. Type it into the app.

That's it. The Fire TV hands the app a token, and every key press after that is authorised with it.
To pair a second Fire TV, tap the device name at the top and choose **Add another Fire TV**.

## Using it

| Control | What it does |
|---|---|
| Ring | Up / Down / Left / Right. Hold to keep scrolling. |
| Centre (OK) | Select |
| ← / ⌂ / ≡ | Back, Home, Menu |
| ⏪ ▶⏸ ⏩ | Rewind, Play/Pause, Fast forward (hold to keep scanning) |
| Keyboard | Type text on the phone and send it to whatever box has focus on the TV |
| Apps | Open any installed app |
| ⋮ menu | Wake up, Sleep, add / re-pair / forget a Fire TV |

The dot next to the device name is green when the Fire TV answers, red when it does not.

### Things it cannot do

- **TV volume.** The Fire TV Stick's own remote sends volume to the television over infrared, not
  over the network, so there is no way for a phone app to change it. Use the TV's remote or the TV
  buttons for volume.
- **Sleep** uses a command that the official app appears to use but that is not documented. If your
  Fire TV ignores it, the app says so. **Wake up** uses the documented wake call and is reliable.

## If something does not work

- *Can't reach the Fire TV*: the phone has dropped to mobile data or a guest network, or the Fire
  TV has lost Wi‑Fi. Open the ⋮ menu and choose **Wake up Fire TV**, then try again.
- *The Fire TV no longer accepts this phone*: the token was revoked (for example after a factory
  reset). Choose **Pair again** and enter the new PIN.
- *Nothing is found during setup*: some routers block device discovery between Wi‑Fi clients.
  Enter the IP address by hand.

## For developers

Everything lives in `firetv-remote/`. The app is plain Kotlin on the Android framework (no
AndroidX, no third-party dependencies), minimum Android 8.0 (API 26).

```
app/src/main/java/com/vianetredirect/firetvremote/
  FireTvClient.kt    HTTPS calls to the Fire TV (pairing, keys, media, text, apps, wake)
  Discovery.kt       mDNS (_amzn-wplay._tcp) + subnet sweep for port 8080
  DpadView.kt        the navigation ring, drawn by hand
  HoldTouchListener  tap = keyDownUp, hold = keyDown … keyUp
  MainActivity.kt    the remote
  SetupActivity.kt   find + PIN pairing
  AppsActivity.kt    app list and launch
```

### How it talks to the Fire TV

The Fire TV runs a local HTTPS service on port 8080 with a self-signed certificate. Every request
carries `X-Api-Key: 0987654321`. Pairing is `POST /v1/FireTV/pin/display` (shows the PIN) followed by
`POST /v1/FireTV/pin/verify` with `{"pin":"1234"}`, which returns the client token used as
`X-Client-Token` from then on. Keys are `POST /v1/FireTV?action=dpad_up` (and `dpad_down`,
`dpad_left`, `dpad_right`, `select`, `back`, `home`, `menu`) with `{"keyActionType":"keyDownUp"}`,
or `keyDown`/`keyUp` for holds. Play/pause is `POST /v1/media?action=play`; scanning is
`POST /v1/media?action=scan` with `{"direction":"forward"|"back","keyAction":{...}}`. Text goes to
`POST /v1/FireTV/text`, app launch to `POST /v1/FireTV/app/<package>`, and the app list comes from
`GET /v1/FireTV/appsV2`. Waking a sleeping device is a plain `POST http://<ip>:8009/apps/FireTVRemote`.

### Building

**With Android Studio / Gradle** (the normal way): open `firetv-remote/` or run
`./gradlew assembleRelease`. Signing for release builds comes from the `KEYSTORE_PATH`,
`KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD` environment variables; without them the debug key
is used.

**Without the Android SDK**: `scripts/build-apk.sh` compiles with `kotlinc`, packages with `aapt`,
dexes with `dx` and signs with `apksigner` (all available as Ubuntu packages), and writes
`build-local/FireRemote.apk`. It is how the APK was first built in an environment with no SDK access.

**GitHub Actions**: `.github/workflows/firetv-remote.yml` builds the APK on every push that touches
`firetv-remote/` and attaches it to a GitHub release named *Fire Remote build N*. It signs with a
throwaway key unless these repository secrets exist, in which case every build is signed the same
way and updates install over the previous version:

- `ANDROID_KEYSTORE_BASE64` – the keystore file, base64-encoded
- `ANDROID_KEYSTORE_PASSWORD` – its password (used for the key too)
- `ANDROID_KEY_ALIAS` – the key alias (defaults to `fireremote`)

To create one: `keytool -genkeypair -keystore fireremote.jks -alias fireremote -keyalg RSA -keysize 2048 -validity 10000`
then `base64 -w0 fireremote.jks`. If builds are signed with different keys, uninstall the old
version before installing the new one.
