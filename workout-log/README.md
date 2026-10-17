# Workout Log

An Android app for logging gym sessions: build workout templates, log sets as weight and reps, run a rest timer between sets, and look back over history and personal records. Everything is stored on the phone. Nothing is uploaded.

## Features

- **Templates**: build a workout once (exercises, target sets and reps, rest time) and start it with one tap. Four starter templates ship with the app: Push, Pull, Legs, Full Body.
- **Active workout**: each set shows what you did last time, so you know what to beat. Tick a set to log it and start the rest timer. Blank fields take last session's numbers when ticked.
- **Rest timer**: counts down in a notification with vibration and sound when it ends, even with the screen off. Adjust by 15 seconds or skip from the app or the notification.
- **History**: every finished workout with duration, sets, volume and the best set per exercise.
- **Exercise library**: around 60 common exercises, plus your own. Each exercise shows its heaviest set, best estimated one rep max, a progress chart and the full set history.
- **Personal records**: a PR badge appears on a set that beats your previous best estimated one rep max.
- **Awards**: cumulative milestones with a confetti moment when you cross one. Total weight lifted is compared with real things, from a grizzly bear at 250 kg up to the Eiffel Tower. Distance from cardio sets runs from a parkrun to once around the Earth. Workout counts and streak lengths unlock too.
- **Streaks**: a weekly streak in the Duolingo style. Pick a goal of one to seven workouts a week. Each week you hit it extends the streak. The Awards tab shows this week's progress and how many sessions are still needed.
- **Reminders**: a daily check that sends a notification after a chosen number of quiet days, with a sharper warning when the streak is about to end.
- **Cardio**: treadmill, rowing, bike and outdoor walks or runs log distance and time instead of weight and reps.
- **Share**: Settings has a Send app button that shares the installed app file phone to phone over Nearby Share, Bluetooth, WhatsApp or email. There is also a link to the latest GitHub release.
- **Settings**: kg or lb, default rest length, whether the timer starts automatically, weekly goal and reminder time.

## Getting the APK

Every push that touches `workout-log/` runs the **Workout Log APK** workflow. It builds the app, runs the unit tests, and publishes a GitHub Release named "Workout Log build N" with `WorkoutLog.apk` attached. Open https://github.com/VianetRedirect/email-redirect/releases on the phone, download the APK from the newest Workout Log build, and open it. Android asks once to allow installs from the browser.

Once the app is on one phone, **Settings > Send app** passes the installed file to another phone directly.

### Signing

Add these repository secrets so every build is signed with the same key and installs over the previous version. They are shared with the Fire Remote workflow, so if they already exist nothing more is needed.

- `ANDROID_KEYSTORE_BASE64`: a keystore file encoded with `base64 -w0 release.jks`
- `ANDROID_KEYSTORE_PASSWORD`: its password
- `ANDROID_KEY_ALIAS`: the key alias (optional, defaults to `workoutlog`)

Without them each build uses a throwaway key, and a new build has to be uninstalled and reinstalled rather than updated in place.

## Building locally

Requires Android Studio (Ladybug or newer) with the Android 15 (API 35) SDK, or a JDK 17 and the Android SDK on the command line. From the `workout-log` folder:

```
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

## Project layout

```
app/src/main/java/com/vianet/workoutlog/
  data/db/        Room entities, DAOs, database, seed data
  data/           Repository and settings store
  domain/         Pure Kotlin: 1RM estimate, volume, PR detection, formatting
  timer/          Rest timer state and foreground service
  reminders/      Daily WorkManager check and nudge notification
  share/          Sending the APK or a download link to others
  ui/             Jetpack Compose screens (start, workout, history, exercises, settings)
app/src/test/     Unit tests for the domain layer
../.github/workflows/workout-log.yml   Build, test and publish the APK
```

## Stack

Kotlin, Jetpack Compose with Material 3, Room, Navigation Compose, WorkManager, coroutines. Minimum Android 8.0 (API 26), target Android 15.
