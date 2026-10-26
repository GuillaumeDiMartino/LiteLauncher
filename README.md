# Lite Launcher

A minimal home screen for Chromecast with Google TV: a clock, the date, and a grid of your apps.
No recommendations, no ads, no background services, no network access, no libraries.

## Remote control

- Arrows to move, OK to open an app.
- Hold OK on an app to move it to the front, move it left or right, hide it, or open its app info.
- The "Apps masquées" tile shows hidden apps again.
- Home jumps back to the top of the grid.

## Install

1. On the Chromecast, open Settings > System > About and press "Android TV OS build" 7 times.
2. Go to Settings > System > Developer options and turn on "USB debugging".
3. Find the Chromecast IP address in Settings > Network & Internet.
4. On the PC (same Wi-Fi network):

```bash
adb connect <IP>:5555
```

Accept the prompt on the TV, then:

```bash
adb install -r app-release.apk
```

```bash
adb shell cmd package set-home-activity fr.lite.launcher/.MainActivity
```

```bash
adb shell pm disable-user --user 0 com.google.android.apps.tv.launcherx
```

The last command disables the stock Google TV launcher. That is what frees most of the RAM.

## Undo

```bash
adb shell pm enable com.google.android.apps.tv.launcherx
```

```bash
adb uninstall fr.lite.launcher
```

A Google system update can turn the stock launcher back on. If that happens, run the disable command again.

## Build

Requires JDK 17+ and the Android SDK (platform 34, build-tools 34).

```bash
gradle assembleRelease
```

The APK is at `app/build/outputs/apk/release/app-release.apk`.
