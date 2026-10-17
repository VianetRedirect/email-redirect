# Fire Remote

A small, fast Android remote for Amazon Fire TV sticks and boxes, laid out like the Alexa Voice
Remote that comes in the box. No adverts, no accounts, no libraries: it talks straight to the Fire TV
over your home Wi‑Fi using the same local service the official Amazon app uses, so it works even
when the physical remote is lost or broken.

- The familiar layout: power and Alexa button up top, the navigation ring, Back / Home / Menu,
  Rewind / Play‑Pause / Fast forward, Mute, volume rocker, Guide, and four app buttons
- Press-and-hold on the ring and keys keeps scrolling, just like the real thing
- Alexa button opens a keyboard so you can type a search instead of talking
- Guide button lists every app on the Fire TV; the four app buttons open one directly, and a
  long-press lets you point any of them at a different app
- Finds the Fire TV automatically, or takes an IP address, and remembers more than one
- Share the app to another phone straight from the menu
- Opens instantly: the APK is about 70 KB

## Install it on your phone

1. On your phone, open the latest *Fire Remote build* under this repository's **Releases** and
   download `FireRemote.apk`. The newest one is always at
   `https://github.com/VianetRedirect/email-redirect/releases/latest/download/FireRemote.apk`.
2. Open the downloaded file. Android will ask you to allow installs from your browser the first
   time. Allow it, then tap **Install**.
3. Open *Fire Remote*.

Works on Android 7.0 and newer, phones and tablets.

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

| Button | What it does |
|---|---|
| Power | Tap to wake the Fire TV. Hold to put it to sleep. |
| Alexa (blue) | Opens a keyboard. What you type is sent to whatever box has focus on the TV. |
| Ring | Up / Down / Left / Right. Hold to keep scrolling. Centre is Select. |
| ↶ / ⌂ / ≡ | Back, Home, Menu |
| ⏪ ▶⏸ ⏩ | Rewind, Play/Pause, Fast forward. Hold to keep scanning. |
| Mute, + / − | Volume. Only works on Fire TV models that support volume over the network (see below). |
| Guide | Lists every app installed on the Fire TV. Tap one to open it. |
| App buttons | Open that app. Long-press to choose a different app for the button. |
| Device name | Switch between paired Fire TVs. |
| ⋮ menu | Share this app, copy the download link, add / re-pair / forget a Fire TV. |

The dot next to the device name is green when the Fire TV answers, red when it does not.

### Sharing it with someone else

Open the ⋮ menu and choose **Share this app**. The share sheet sends the APK itself, so Quick Share,
Bluetooth or a file transfer app will put it straight on the other phone, and messaging apps get the
download link. **Copy download link** copies the link to the newest build.

### Things it cannot do

- **TV volume on a Fire TV Stick.** The Stick's own remote sends volume to the television over
  infrared, which no network app can imitate. The volume buttons try the network command anyway,
  and the app tells you if the Fire TV refuses. Fire TV Cube and Fire TV Edition televisions are the
  models most likely to accept it.
- **Voice.** The Alexa button types instead of listening; the voice stream protocol is not
  documented.
- **Sleep** uses a command that the official app appears to use but that is not documented. If your
  Fire TV ignores it, the app says so. Wake-up uses the documented wake call and is reliable.

## If something does not work

- *Can't reach the Fire TV*: the phone has dropped to mobile data or a guest network, or the Fire
  TV has lost Wi‑Fi. Tap the power button to send a wake-up, then try again.
- *The Fire TV no longer accepts this phone*: the token was revoked (for example after a factory
  reset). Choose **Pair again** and enter the new PIN.
- *Nothing is found during setup*: some routers block device discovery between Wi‑Fi clients.
  Enter the IP address by hand.
- *"App not installed" when updating*: the new APK was signed with a different key than the one
  on the phone. Uninstall the old copy first, or set up the signing key below so every build
  matches.

## For developers

Everything lives in `firetv-remote/`. The app is plain Kotlin on the Android framework (no
AndroidX, no third-party dependencies), minimum Android 7.0 (API 24).

```
app/src/main/java/com/vianetredirect/firetvremote/
  FireTvClient.kt    HTTPS calls to the Fire TV (pairing, keys, media, text, apps, wake)
  Discovery.kt       mDNS (_amzn-wplay._tcp) + subnet sweep for port 8080
  DpadView.kt        the navigation ring, drawn by hand
  HoldTouchListener  tap = keyDownUp, hold = keyDown … keyUp
  Shortcuts.kt       the four app buttons and their brand labels
  ApkProvider.kt     serves the installed APK to the share sheet
  MainActivity.kt    the remote
  SetupActivity.kt   find + PIN pairing
  AppsActivity.kt    app list, launch, and "choose an app for this button"
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
Port 8080 is closed while the device sleeps, so a failed key press sends that wake call and retries.

### Building

**With Android Studio / Gradle** (the normal way): open `firetv-remote/` or run
`./gradlew assembleRelease`. Signing for release builds comes from the `KEYSTORE_PATH`,
`KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD` environment variables; without them the debug key
is used.

**Without the Android SDK**: `scripts/build-apk.sh` compiles with `kotlinc`, packages with `aapt`,
dexes with `dx` and signs with `apksigner` (all available as Ubuntu packages), and writes
`build-local/FireRemote.apk`. It is how the APK was first built in an environment with no SDK access.

**GitHub Actions**: `.github/workflows/firetv-remote.yml` builds the APK on every push that touches
`firetv-remote/` and attaches it to a GitHub release named *Fire Remote build N*.

### Signing: make every build install over the last one

Android only lets a new build replace an installed one if both are signed with the same key. The
workflow signs with a throwaway key unless these repository secrets exist
(*Settings › Secrets and variables › Actions › New repository secret*):

- `ANDROID_KEYSTORE_BASE64` – the keystore file, base64-encoded
- `ANDROID_KEYSTORE_PASSWORD` – its password (used for the key too)
- `ANDROID_KEY_ALIAS` – the key alias (defaults to `fireremote`)

To create a keystore yourself:
`keytool -genkeypair -keystore fireremote.jks -alias fireremote -keyalg RSA -keysize 2048 -validity 10000`
then `base64 -w0 fireremote.jks` for the secret value. Keep the keystore somewhere safe; losing it
means everyone has to uninstall and reinstall once.
