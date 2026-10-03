# waterminder

A small, offline Android app for remembering to drink water. Warm cream, sage,
peach, and lavender; original rounded, charcoal-outline artwork inspired by the
Crayon icon aesthetic and shmemplay. A plant, a reminder, and one button.

## Try the prototype

- **Interactive desktop preview:** run `node preview/server.cjs`, then open
  http://127.0.0.1:4173. You can also open `preview/index.html` directly.
- **Installable Android app:** the locally built APK is
  `artifacts/waterminder-0.1.8.apk`. Copy it to your phone and open it to install.
  Android may ask you to allow installation from the app opening the APK.
- Open waterminder, turn on the reminder switch, and allow notifications.
  The Android app starts with reminders off; the desktop preview starts with
  them on to demonstrate the design.

## What it does

- Light and dark appearances follow Android's system theme automatically,
  including the startup background, system bar icons, and time-picker dialogs.
  Dark mode keeps the same pastel plant and accents on a soft charcoal background.
  The browser preview follows the computer's theme; `?theme=dark` or `?theme=light`
  provides a temporary preview-only override for inspecting either appearance.
- A soft blue waterdrop launcher icon with a transparent background.
- Reminders every 1, 2, 3, or 4 hours, or a custom interval from 30 minutes to
  8 hours in 15-minute steps. Default: 2 hours.
- Optional quiet hours, defaulting to 10 PM–7 AM. Overnight and daytime ranges
  work; matching start/end times are rejected.
- A gentle, steady 3.2-second plant-watering animation and haptic when you tap
  **I drank water**. This dismisses the notification and keeps the next reminder unchanged.
  Each delivered reminder automatically schedules the next interval; dismissing
  or ignoring it never stops the reminders.
- A lock-screen notification with an **I drank water** action that opens the
  app and waters the plant.
- Optional screen wake, default on, and a **Send a test nudge** button. The test
  waits 10 seconds so you can lock your screen first. Tests also respect quiet
  hours. The desktop notification is explicitly a simulation.
- Pause/resume with the home-screen switch. Interval and quiet-hour edits start
  a fresh interval. Changing screen wake preserves the pending reminder time.
- Settings survive app closure and phone restart. Reminders restore after a
  reboot, app update, or clock/timezone change. Missed reminders never accumulate
  into a burst. Quiet hours follow the phone's local time.

No accounts, internet permission, drink quantities, history, streaks, ads, or
analytics. Only reminder preferences and the next scheduled reminder time are
stored locally. The plant is a little moment of encouragement, not a progress
meter.

## Android behavior and prototype limits

Reminders use one-shot `AlarmManager` alarms and work without a running app or
persistent service. With **Alarms & reminders** access, they use
`setExactAndAllowWhileIdle`; Android 12+ exposes an **Allow on-time reminders**
button in app settings when access is missing. Without access they fall back to
`setAndAllowWhileIdle`, which Android can delay by an hour or more. Granting access
restores the existing schedule without restarting the interval. The app checks
quiet hours again at delivery. Samsung sleep restrictions can still defer work.
Android cancels alarms when an app is force-stopped; reopen waterminder to resume.
If notification permission is restored after a canceled reminder, reopening the
app restores scheduling.

Screen wake is **best-effort**, not guaranteed to match Snapchat on every phone.
An optional, brief legacy screen wake lock is used when the display is off,
notifications are allowed, and Do Not Disturb is off. It never launches a
full-screen activity or bypasses the lock screen. Some Android versions and
manufacturers restrict this API. Enable lock-screen notifications in the phone's
settings and use the delayed test to check your device. The test briefly keeps
the CPU awake; it can be lost if the app process is killed during those 10 seconds.

The APK has been compiled, linted, and tested with Robolectric, including app
startup and notification-action handling. The interactive preview has been
visually checked at normal and 320-pixel widths. The dark appearance was visually
checked on a connected Samsung SM-S928U. Version 0.1.7 delivered a real exact
reminder after its background process was terminated, then automatically
scheduled the next reminder at quiet-hours end. All 30 unit/UI tests passed.
Haptics, extended Doze behavior, and screen wake still need a phone check.

`ReminderBackgroundTest` is an optional device smoke test that schedules one
real nudge 20 seconds ahead while retaining the user's interval and quiet hours.
After instrumentation finishes, close the app's background process to verify
notification delivery. The test skips when reminders or permissions are off,
or when its delivery would fall inside quiet hours.

## Build

Requires JDK 17, Android SDK Platform 36 and Build Tools 36.0.0. Android 8.0+
(API 26); targets API 36. Kotlin and Jetpack Compose, using the same toolchain as
shmemplay. Create `local.properties` with your SDK path or set `ANDROID_HOME`.

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

The build writes `app/build/outputs/apk/debug/app-debug.apk`. GitHub Actions runs
the same checks and uploads the APK. On Linux/macOS use `./gradlew`.

The 23 tests cover scheduling replacement/cancellation, blocked notifications,
permission denial, reminder delivery, quiet-hour delivery delays, reboot restore,
drink-action resets, a delayed test notification, startup, quiet-hour boundaries,
local timezone behavior, and both daylight-saving transitions.

## Files

- `app/` — the native Android app and reminder tests.
- `preview/` — a dependency-free interactive browser companion; its settings
  are stored separately from the Android app.
- `preview/screenshots/` — screenshots of the verified preview.
- `.github/workflows/android.yml` — tests, lint, and APK build.

![Home screen](preview/screenshots/home.jpg)
